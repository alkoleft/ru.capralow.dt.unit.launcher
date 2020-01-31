/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import ru.capralow.dt.internal.junit.refactoring.JUnitRenameParticipant.ChangeList;

public interface IChangeAdder {

	public abstract void createChangeForConfig(ChangeList list, LaunchConfigurationContainer config) throws CoreException;

}
