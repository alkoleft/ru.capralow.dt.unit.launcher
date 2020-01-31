/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *   Julien Ruaux: jruaux@octo.com
 * 	 Vincent Massol: vmassol@octo.com
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package ru.capralow.dt.internal.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

import ru.capralow.dt.internal.junit.model.JUnitModel;
import ru.capralow.dt.junit.TestRunListener;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * The plug-in runtime class for the JUnit core plug-in.
 */
public class JUnitCorePlugin extends Plugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitCorePlugin fgPlugin= null;

	public static final String CORE_PLUGIN_ID= "org.eclipse.jdt.junit.core"; //$NON-NLS-1$
	
	/**
	 * Plug-in ID of the <b>UI</b> plug-in ("org.eclipse.jdt.junit").
	 * @see #CORE_PLUGIN_ID
	 */
	public static final String PLUGIN_ID= "org.eclipse.jdt.junit"; //$NON-NLS-1$
	public static final String ID_EXTENSION_POINT_TESTRUN_LISTENERS= PLUGIN_ID + "." + "testRunListeners"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String ID_EXTENSION_POINT_TEST_KINDS= PLUGIN_ID + "." + "internal_testKinds"; //$NON-NLS-1$ //$NON-NLS-2$

	public final static String TEST_SUPERCLASS_NAME= "junit.framework.TestCase"; //$NON-NLS-1$
	public final static String TEST_INTERFACE_NAME= "junit.framework.Test"; //$NON-NLS-1$

	public final static String JUNIT5_TESTABLE_ANNOTATION_NAME= "org.junit.platform.commons.annotation.Testable"; //$NON-NLS-1$
	public final static String JUNIT5_JUPITER_TEST_ANNOTATION_NAME= "org.junit.jupiter.api.Test"; //$NON-NLS-1$
	public final static String JUNIT5_JUPITER_NESTED_ANNOTATION_NAME= "org.junit.jupiter.api.Nested"; //$NON-NLS-1$

	public final static String JUNIT4_ANNOTATION_NAME= "org.junit.Test"; //$NON-NLS-1$
	public static final String SIMPLE_TEST_INTERFACE_NAME= "Test"; //$NON-NLS-1$

	/**
	 * The class path variable referring to the junit home location
	 */
	public final static String JUNIT_HOME= "JUNIT_HOME"; //$NON-NLS-1$

	/**
	 * The class path variable referring to the junit source location
     * @since 3.2
	 */
	public static final String JUNIT_SRC_HOME= "JUNIT_SRC_HOME";  //$NON-NLS-1$

	private static final String HISTORY_DIR_NAME= "history"; //$NON-NLS-1$

	private final JUnitModel fJUnitModel= new JUnitModel();


	/**
	 * List storing the registered test run listeners
	 * @deprecated to avoid deprecation warnings
	 */
	@Deprecated
	private List<ru.capralow.dt.junit.ITestRunListener> fLegacyTestRunListeners;

	/**
	 * List storing the registered test run listeners
	 */
	private ListenerList<TestRunListener> fNewTestRunListeners;

	private BundleContext fBundleContext;

	private static boolean fIsStopped= false;


	public JUnitCorePlugin() {
		fgPlugin= this;
		fNewTestRunListeners= new ListenerList<>();
	}

	public static JUnitCorePlugin getDefault() {
		return fgPlugin;
	}

	public static String getPluginId() {
		return CORE_PLUGIN_ID;
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * @see Plugin#start(BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext= context;
		fJUnitModel.start();
	}

	/**
	 * @see Plugin#stop(BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		fIsStopped= true;
		try {
			InstanceScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID).flush();
			fJUnitModel.stop();
		} finally {
			super.stop(context);
		}
		fBundleContext= null;
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 * 
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 * @since 3.5
	 */
	public Object getService(String serviceName) {
		ServiceReference<?> reference= fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return fBundleContext.getService(reference);
	}

	public static JUnitModel getModel() {
		return getDefault().fJUnitModel;
	}

	/**
	 * Initializes TestRun Listener extensions
	 * @deprecated to avoid deprecation warning
	 */
	@Deprecated
	private synchronized void loadTestRunListeners() {
		if (fLegacyTestRunListeners != null)
			return;

		fLegacyTestRunListeners= new ArrayList<>();
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_TESTRUN_LISTENERS);
		if (extensionPoint == null) {
			return;
		}
		IConfigurationElement[] configs= extensionPoint.getConfigurationElements();
		MultiStatus status= new MultiStatus(CORE_PLUGIN_ID, IStatus.OK, "Could not load some testRunner extension points", null); //$NON-NLS-1$
		for (IConfigurationElement config : configs) {
			try {
				Object testRunListener= config.createExecutableExtension("class"); //$NON-NLS-1$
				if (testRunListener instanceof TestRunListener) {
					fNewTestRunListeners.add((TestRunListener) testRunListener);
				} else if (testRunListener instanceof ru.capralow.dt.junit.ITestRunListener) {
					fLegacyTestRunListeners.add((ru.capralow.dt.junit.ITestRunListener) testRunListener);
				}
			}catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		if (!status.isOK()) {
			JUnitCorePlugin.log(status);
		}
	}

	/**
	 * @return an array of all TestRun listeners
	 * @deprecated to avoid deprecation warnings
	 */
	@Deprecated
	public ru.capralow.dt.junit.ITestRunListener[] getTestRunListeners() {
		loadTestRunListeners();
		return fLegacyTestRunListeners.toArray(new ru.capralow.dt.junit.ITestRunListener[fLegacyTestRunListeners.size()]);
	}

	/**
	 * Returns the bundle for a given bundle name,
	 * regardless whether the bundle is resolved or not.
	 *
	 * @param bundleName the bundle name
	 * @return the bundle
	 * @since 3.2
	 */
	public Bundle getBundle(String bundleName) {
		Bundle[] bundles= getBundles(bundleName, null);
		if (bundles != null && bundles.length > 0)
			return bundles[0];
		return null;
	}

	/**
	 * Returns the bundles for a given bundle name,
	 *
	 * @param bundleName the bundle name
	 * @param version the version of the bundle
	 * @return the bundles of the given name
	 */
	public Bundle[] getBundles(String bundleName, String version) {
		Bundle[] bundles= Platform.getBundles(bundleName, version);
		if (bundles != null)
			return bundles;

		// Accessing unresolved bundle
		ServiceReference<PackageAdmin> serviceRef= fBundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin admin= fBundleContext.getService(serviceRef);
		bundles= admin.getBundles(bundleName, version);
		if (bundles != null && bundles.length > 0)
			return bundles;
		return null;
	}

	/**
	 * Adds a TestRun listener to the collection of listeners
	 * @param newListener the listener to add
	 * @deprecated to avoid deprecation warnings
	 */
	@Deprecated
	public void addTestRunListener(ru.capralow.dt.junit.ITestRunListener newListener) {
		loadTestRunListeners();

		for (ru.capralow.dt.junit.ITestRunListener o : fLegacyTestRunListeners) {
			if (o == newListener)
				return;
		}
		fLegacyTestRunListeners.add(newListener);
	}

	/**
	 * Removes a TestRun listener to the collection of listeners
	 * @param newListener the listener to remove
	 * @deprecated to avoid deprecation warnings
	 */
	@Deprecated
	public void removeTestRunListener(ru.capralow.dt.junit.ITestRunListener newListener) {
		if (fLegacyTestRunListeners != null)
			fLegacyTestRunListeners.remove(newListener);
	}

	/**
	 * @return a <code>ListenerList</code> of all <code>TestRunListener</code>s
	 */
	public ListenerList<TestRunListener> getNewTestRunListeners() {
		loadTestRunListeners();
		
		return fNewTestRunListeners;
	}

	public static boolean isStopped() {
		return fIsStopped;
	}

	public static File getHistoryDirectory() throws IllegalStateException {
		File historyDir= getDefault().getStateLocation().append(HISTORY_DIR_NAME).toFile();
		if (! historyDir.isDirectory()) {
			historyDir.mkdir();
		}
		return historyDir;
	}

}
