/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.pages;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.MarkerHandler;
import org.eclipse.titan.designer.core.TITANBuilder;
import org.eclipse.titan.designer.properties.PropertyNotificationManager;
import org.eclipse.titan.designer.properties.data.FileBuildPropertyData;
import org.eclipse.titan.designer.properties.data.ProjectDocumentHandlingUtility;
import org.eclipse.titan.designer.wizards.projectFormat.TITANAutomaticProjectExporter;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * @author Kristof Szabados
 * */
public final class FileBuildPropertyPage extends PropertyPage {
	private Composite pageComposite = null;
	private Label headLabel = null;
	private ConfigurationManagerControl configurationManager;
	private String firstConfiguration;

	private Button excludeFromBuildButton = null;
	private static final String EXCLUDE_DISPLAY_TEXT = "Excluded from build.";

	private IFile fileResource;
	private final PreferenceStore tempStore;

	public FileBuildPropertyPage() {
		super();
		tempStore = new PreferenceStore();
	}

	@Override
	public void dispose() {
		pageComposite.dispose();
		headLabel.dispose();

		super.dispose();
	}

	/**
	 * Handles the change of the active configuration. Sets the new
	 * configuration to be the active one, and loads its settings.
	 * 
	 * @param configuration
	 *                the name of the new configuration.
	 * */
	public void changeConfiguration(final String configuration) {
		configurationManager.changeActualConfiguration();

		loadProperties();

		PropertyNotificationManager.firePropertyChange(fileResource.getProject());
	}

	@Override
	protected Control createContents(final Composite parent) {
		fileResource = (IFile) getElement();
		try {
			final String temp = fileResource.getPersistentProperty(new QualifiedName(FileBuildPropertyData.QUALIFIER,
					FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY));
			tempStore.setValue(FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY,
					FileBuildPropertyData.TRUE_STRING.equals(temp) ? FileBuildPropertyData.TRUE_STRING
							: FileBuildPropertyData.FALSE_STRING);
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}

		pageComposite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		pageComposite.setLayout(layout);
		final GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		pageComposite.setLayoutData(data);
		if (TITANBuilder.isBuilderEnabled(fileResource.getProject())) {
			headLabel = new Label(pageComposite, SWT.NONE);
			headLabel.setText(ProjectBuildPropertyPage.BUILDER_IS_ENABLED);
		} else {
			headLabel = new Label(pageComposite, SWT.NONE);
			headLabel.setText(ProjectBuildPropertyPage.BUILDER_IS_NOT_ENABLED);
		}

		configurationManager = new ConfigurationManagerControl(pageComposite, fileResource.getProject());
		configurationManager.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (configurationManager.hasConfigurationChanged()) {
					changeConfiguration(configurationManager.getActualSelection());
				}
			}
		});
		firstConfiguration = configurationManager.getActualSelection();

		excludeFromBuildButton = new Button(pageComposite, SWT.CHECK);
		excludeFromBuildButton.setText(EXCLUDE_DISPLAY_TEXT);
		excludeFromBuildButton.setEnabled(true);
		try {
			final String mode = fileResource.getPersistentProperty(new QualifiedName(FileBuildPropertyData.QUALIFIER,
					FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY));
			excludeFromBuildButton.setSelection(FileBuildPropertyData.TRUE_STRING.equals(mode));
			if (mode == null) {
				fileResource.setPersistentProperty(new QualifiedName(FileBuildPropertyData.QUALIFIER,
						FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY), FileBuildPropertyData.FALSE_STRING);
			}
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}

		return null;
	}

	@Override
	public void setVisible(final boolean visible) {
		if (!visible) {
			return;
		}

		if (configurationManager != null) {
			configurationManager.refresh();
		}

		super.setVisible(visible);
	}

	@Override
	protected void performDefaults() {
		excludeFromBuildButton.setSelection(false);

		configurationManager.saveActualConfiguration();
	}

	@Override
	public boolean performCancel() {
		configurationManager.clearActualConfiguration();
		loadProperties();

		return super.performCancel();
	}

	@Override
	public boolean performOk() {
		final IProject project = fileResource.getProject();

		try {
			final String tempString = excludeFromBuildButton.getSelection() ? FileBuildPropertyData.TRUE_STRING
					: FileBuildPropertyData.FALSE_STRING;
			fileResource.setPersistentProperty(new QualifiedName(FileBuildPropertyData.QUALIFIER,
					FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY), tempString);
			final boolean configurationChanged = !firstConfiguration.equals(configurationManager.getActualSelection());
			if (configurationChanged || !tempString.equals(tempStore.getString(FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY))) {
				tempStore.setValue(FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY, FileBuildPropertyData.TRUE_STRING
						.equals(tempString) ? FileBuildPropertyData.TRUE_STRING : FileBuildPropertyData.FALSE_STRING);

				configurationManager.saveActualConfiguration();
				ProjectDocumentHandlingUtility.saveDocument(project);
				TITANAutomaticProjectExporter.saveAllAutomatically(project);

				MarkerHandler.markAllMarkersForRemoval(fileResource);

				PropertyNotificationManager.firePropertyChange(fileResource);
			}
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
			return false;
		}

		return true;
	}

	/**
	 * Loads the properties from the resource into the user interface
	 * elements.
	 * */
	private void loadProperties() {
		try {
			final String temp = fileResource.getPersistentProperty(new QualifiedName(FileBuildPropertyData.QUALIFIER,
					FileBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY));
			excludeFromBuildButton.setSelection(FileBuildPropertyData.TRUE_STRING.equals(temp));
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}
	}
}
