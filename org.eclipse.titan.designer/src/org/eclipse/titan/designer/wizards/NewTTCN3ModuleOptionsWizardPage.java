/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.wizards;

/**
 * @author Kristof Szabados, Jeno Balasko
 * */
public final class NewTTCN3ModuleOptionsWizardPage extends NewModuleOptionsWizardPage {
	private static final String TITLE = "TTCN3 Module creation options";
	private static final String DESCRIPTION = "Create the new TTCN3 module according to these options";

	NewTTCN3ModuleOptionsWizardPage() {
		super(TITLE);
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getTitle() {
		return TITLE;
	}

}

