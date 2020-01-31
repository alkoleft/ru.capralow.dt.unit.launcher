/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package ru.capralow.dt.internal.junit.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import ru.capralow.dt.internal.junit.util.CoreTestSearchEngine;


public class TypeRenameParticipant extends JUnitRenameParticipant {

	private IType fType;

	@Override
	protected boolean initialize(Object element) {
		fType= (IType) element;
		return isTestOrTestSuite();
	}

	protected boolean isTestOrTestSuite() {
		try {
			return CoreTestSearchEngine.isTestOrTestSuite(fType);
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public void createChangeForConfig(ChangeList list, LaunchConfigurationContainer config) throws CoreException {
		String typeName= fType.getFullyQualifiedName('.');
		String mainType= config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
		if (typeName.equals(mainType)) {
			int index= mainType.lastIndexOf('.');
			String prefix;
			if (index == -1)
				prefix= ""; //$NON-NLS-1$
			prefix= mainType.substring(0, index + 1);
			String newValue= prefix + getNewName();
			list.addAttributeChange(config, IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, newValue);

			list.addRenameChangeIfNeeded(config, fType.getElementName());
		}
	}
}
