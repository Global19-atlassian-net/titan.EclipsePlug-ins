/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.pages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;
import org.eclipse.titan.designer.properties.data.TTCN3PreprocessorOptionsData;

/**
 * @author Kristof Szabados
 * */
public final class TTCN3PreprocessorOptionsPage implements IOptionsPage {
	private Composite mainComposite;

	private StringFieldEditor tool;

	private final boolean CBuild;

	public TTCN3PreprocessorOptionsPage(final boolean CBuild) {
		this.CBuild = CBuild;
	}

	@Override
	public void dispose() {
		if (mainComposite != null) {
			mainComposite.dispose();
			mainComposite = null;

			if (CBuild) {
				tool.dispose();
			}
		}
	}

	@Override
	public Composite createContents(final Composite parent) {
		if (mainComposite != null) {
			return mainComposite;
		}

		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		if (CBuild) {
			tool = new StringFieldEditor(TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY, "TTCN-3 preprocessor:", mainComposite);
			tool.setStringValue(TTCN3PreprocessorOptionsData.DEFAULT_PREPROCESSOR);
		}

		return mainComposite;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		if (mainComposite == null) {
			return;
		}

		if (CBuild) {
			tool.setEnabled(enabled, mainComposite);
		}
	}

	@Override
	public void copyPropertyStore(final IProject project, final PreferenceStore tempStorage) {
		String temp = null;
		try {
			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY));
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}
		if (temp != null) {
			tempStorage.setValue(TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY, temp);
		}
	}

	@Override
	public boolean evaluatePropertyStore(final IProject project, final PreferenceStore tempStorage) {
		String actualValue = null;
		String copyValue = null;
		try {
			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY));
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}
		copyValue = tempStorage.getString(TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY);
		return ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

	}

	@Override
	public void performDefaults() {
		if (mainComposite == null) {
			return;
		}

		if (CBuild) {
			tool.setEnabled(true, mainComposite);
			tool.setStringValue("cpp");
		}
	}

	@Override
	public boolean checkProperties(final ProjectBuildPropertyPage page) {
		if (CBuild) {
			final String temp = tool.getStringValue();
			if (temp == null || "".equals(temp)) {
				page.setErrorMessage("The TTCN-3 preprocessor must be set.");
				return false;
			}
		}

		return true;
	}

	@Override
	public void loadProperties(final IProject project) {
		if (!CBuild) {
			return;
		}

		try {
			String temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY));
			if (temp == null || temp.length() == 0) {
				temp = "cpp";
			}
			tool.setStringValue(temp);
		} catch (CoreException e) {
			tool.setStringValue("cpp");
		}
	}

	@Override
	public boolean saveProperties(final IProject project) {
		if (CBuild) {
			try {
				final QualifiedName qualifiedName = new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY);
				final String newValue = tool.getStringValue();
				final String oldValue = project.getPersistentProperty(qualifiedName);
				if (newValue != null && !newValue.equals(oldValue)) {
					project.setPersistentProperty(qualifiedName, newValue);
				}
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace(e);
				return false;
			}
		}

		return true;
	}
}
