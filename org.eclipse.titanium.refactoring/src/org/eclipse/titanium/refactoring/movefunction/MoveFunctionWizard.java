/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.movefunction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Wizard for the 'Slicing' refactoring operation.
 *
 * @author Bianka Bekefi
 */
public class MoveFunctionWizard extends RefactoringWizard implements
		IExecutableExtension {

	private static final String WIZ_WINDOWTITLE1 = "Slicing";
	private static final String WIZ_WINDOWTITLE2 = "Slicing - function selection";
	private static final String WIZ_WINDOWTITLE3 = "Slicing - destination options";

	private final MoveFunctionRefactoring refactoring;

	MoveFunctionWizard(final Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		this.refactoring = (MoveFunctionRefactoring)refactoring;
	}

	@Override
	public void setInitializationData(final IConfigurationElement config,
			final String propertyName, final Object data) throws CoreException {

	}

	@Override
	protected void addUserInputPages() {
		setDefaultPageTitle(WIZ_WINDOWTITLE1);
		final MoveFunctionWizardFunctionsPage functionsPage = new MoveFunctionWizardFunctionsPage(WIZ_WINDOWTITLE2, refactoring);
		addPage(functionsPage);
		final MoveFunctionWizardDestinationsPage destinationsPage = new MoveFunctionWizardDestinationsPage(WIZ_WINDOWTITLE3, refactoring);
		addPage(destinationsPage);
	}
}
