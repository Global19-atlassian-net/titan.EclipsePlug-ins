/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.pages;

import java.net.URI;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.titan.common.fieldeditors.TITANResourceLocatorFieldEditor;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.path.PathUtil;
import org.eclipse.titan.common.path.TITANPathUtilities;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.preferences.pages.ComboFieldEditor;
import org.eclipse.titan.designer.productUtilities.ProductConstants;
import org.eclipse.titan.designer.properties.data.MakefileCreationData;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * @author Kristof Szabados
 * */
public final class MakefileCreationTab {
	private static final String CREATIONATTRIBUTES_TEXT = "Makefile creation attributes";
	private static final String INVALID_CODESPLITTING_NUMBER = "Invalid code splitting number in makefile creation tab: ";
	private boolean makeFileGenerationEnabled = true; //Is it not stored elsewhere???
	private Composite automaticBuildPropertiesComposite;
	private Button useAbsolutePathButton;
	private Button gnuMakeButton;
	private Button incrementalDependecyRefresh;
	private Button dynamicLinking;
	private Button functionTestRuntimeButton;
	private Button singleModeButton;
	private Group codeSplittingGroup;
	private Composite codeSplittingComposite;
	private ComboFieldEditor codeSplitting; //code splitting mode
	private Composite codeSplittingNumberComposite;
	private IntegerFieldEditor codeSplittingNumber; //code splitting number if codeSpitting == "number"
	private Composite defaultTargetComposite;
	private ComboFieldEditor defaultTarget;

	private Composite targetExecutableComposite;
	public static final String TEMPORAL_TARGET_EXECUTABLE = ProductConstants.PRODUCT_ID_DESIGNER + ".temporalTargetExecutable";
	private TITANResourceLocatorFieldEditor temporalTargetExecutableFileFieldEditor;

	private TabItem creationAttributesTabItem;
	private final IProject project;
	private final PropertyPage page;

	public MakefileCreationTab(final IProject project, final PropertyPage page) {
		this.project = project;
		this.page = page;
	}

	/**
	 * Disposes the SWT resources allocated by this tab page.
	 */
	public void dispose() {
		useAbsolutePathButton.dispose();
		gnuMakeButton.dispose();
		incrementalDependecyRefresh.dispose();
		dynamicLinking.dispose();
		functionTestRuntimeButton.dispose();
		singleModeButton.dispose();

		codeSplittingComposite.dispose();
		codeSplitting.dispose();
		codeSplittingNumberComposite.dispose();
		codeSplittingNumber.dispose();

		defaultTarget.dispose();
		defaultTargetComposite.dispose();

		temporalTargetExecutableFileFieldEditor.dispose();
		targetExecutableComposite.dispose();

		automaticBuildPropertiesComposite.dispose();
		creationAttributesTabItem.dispose();

	}

	/**
	 * Creates and returns the SWT control for the customized body of this
	 * TabItem under the given parent TabFolder.
	 * <p>
	 * 
	 * @param tabFolder
	 *                the parent TabFolder
	 * @return the new TabItem
	 */
	protected TabItem createContents(final TabFolder tabFolder) {
		creationAttributesTabItem = new TabItem(tabFolder, SWT.BORDER);
		creationAttributesTabItem.setText(CREATIONATTRIBUTES_TEXT);
		creationAttributesTabItem.setToolTipText("Settings controlling the generation of the makefile.");

		automaticBuildPropertiesComposite = new Composite(tabFolder, SWT.MULTI);
		automaticBuildPropertiesComposite.setEnabled(true);
		automaticBuildPropertiesComposite.setLayout(new GridLayout());

		useAbsolutePathButton = new Button(automaticBuildPropertiesComposite, SWT.CHECK);
		useAbsolutePathButton.setText("use absolute pathnames in the Makefile");
		gnuMakeButton = new Button(automaticBuildPropertiesComposite, SWT.CHECK);
		gnuMakeButton.setText("generate Makefile for use with GNU make");
		incrementalDependecyRefresh = new Button(automaticBuildPropertiesComposite, SWT.CHECK);
		incrementalDependecyRefresh.setText("generate Makefile with incrementally refreshing dependency");
		dynamicLinking = new Button(automaticBuildPropertiesComposite, SWT.CHECK);
		dynamicLinking.setText("link dynamically");
		functionTestRuntimeButton = new Button(automaticBuildPropertiesComposite, SWT.CHECK);
		functionTestRuntimeButton.setText("generate Makefile for use with the function test runtime");
		singleModeButton = new Button(automaticBuildPropertiesComposite, SWT.CHECK);
		singleModeButton.setText("generate Makefile for single mode");

		//codeSplitting
		codeSplittingGroup = new Group(automaticBuildPropertiesComposite, SWT.NONE);
		codeSplittingGroup.setText("Code Splitting (-U)");
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		codeSplittingGroup.setLayout(layout);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
		codeSplittingGroup.setLayoutData(gridData);

		codeSplittingComposite = new Composite(codeSplittingGroup, SWT.NONE);
		codeSplitting = new ComboFieldEditor(MakefileCreationData.CODE_SPLITTING_PROPERTY, "Splitting mode:",
			new String[][] {
				{ "none", GeneralConstants.NONE },
				{ "type", GeneralConstants.TYPE },
				{ "number", GeneralConstants.NUMBER} },
			codeSplittingComposite);

		codeSplitting.setPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				updateCodeSplittingNumberEnabled();
			}
		});

		codeSplittingNumberComposite = new Composite(codeSplittingGroup, SWT.NONE);
		final GridLayout codeSplittingNumberLayout = new GridLayout();
		codeSplittingNumberComposite.setLayout(codeSplittingNumberLayout);
		final GridData codeSplittingNumberData = new GridData(GridData.FILL);
		codeSplittingNumberData.grabExcessHorizontalSpace = true;
		codeSplittingNumberData.horizontalAlignment = SWT.FILL;
		codeSplittingNumberComposite.setLayoutData(codeSplittingNumberData);
		codeSplittingNumber = new IntegerFieldEditor(
				GeneralConstants.NUMBER_DEFAULT,
				"Number (1-9999):",
				codeSplittingNumberComposite,
				5);
		codeSplittingNumber.setValidRange(1, 9999);
		codeSplittingNumber.setPropertyChangeListener (new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				checkCodeSplittingNumber();
			}
		});

		//defaultTarget
		defaultTargetComposite = new Composite(automaticBuildPropertiesComposite, SWT.NONE);
		defaultTarget = new ComboFieldEditor(MakefileCreationData.DEFAULT_TARGET_PROPERTY, "Default target:",
				MakefileCreationData.DefaultTarget.getDisplayNamesAndValues(), defaultTargetComposite);

		//target exe
		targetExecutableComposite = new Composite(automaticBuildPropertiesComposite, SWT.NONE);
		final GridLayout targetExecutableLayout = new GridLayout();
		targetExecutableComposite.setLayout(targetExecutableLayout);
		final GridData targetExecutableData = new GridData(GridData.FILL);
		targetExecutableData.grabExcessHorizontalSpace = true;
		targetExecutableData.horizontalAlignment = SWT.FILL;
		targetExecutableComposite.setLayoutData(targetExecutableData);
		temporalTargetExecutableFileFieldEditor = new TITANResourceLocatorFieldEditor(TEMPORAL_TARGET_EXECUTABLE, "Target executable:",
				targetExecutableComposite, IResource.FILE, project.getLocation().toOSString());
		temporalTargetExecutableFileFieldEditor
				.getLabelControl(targetExecutableComposite)
				.setToolTipText("The \"final\" executable file that will be built. (Also called Executable Test Suite) \n"
						+ "This field is optional.\n"
						+ " If it is not set, the executable will be generated into the working directory, with the name of the project.");
		temporalTargetExecutableFileFieldEditor.setPage(page);

		creationAttributesTabItem.setControl(automaticBuildPropertiesComposite);
		return creationAttributesTabItem;
	}

	/**
	 * Handles the enabling/disabling of controls when the automatic
	 * Makefile generation is enabled/disabled.
	 * 
	 * @param value
	 *                the actual state of automatic Makefile generation.
	 * */
	protected void setMakefileGenerationEnabled(final boolean value) {
		makeFileGenerationEnabled = value;
		useAbsolutePathButton.setEnabled(value);
		gnuMakeButton.setEnabled(value);
		incrementalDependecyRefresh.setEnabled(value);
		dynamicLinking.setEnabled(value);
		functionTestRuntimeButton.setEnabled(value);
		singleModeButton.setEnabled(value);
		codeSplittingComposite.setEnabled(value);
		codeSplitting.setEnabled(value, codeSplittingComposite);
		updateCodeSplittingNumberEnabled();
		defaultTargetComposite.setEnabled(value);
		defaultTarget.setEnabled(value, defaultTargetComposite);
		temporalTargetExecutableFileFieldEditor.setEnabled(value, targetExecutableComposite);
	}

	/**
	 * Copies the actual values into the provided preference storage.
	 * 
	 * @param project
	 *                the actual project (the real preference store).
	 * @param tempStorage
	 *                the temporal store to copy the values to.
	 * */
	public void copyPropertyStore(final IProject project, final PreferenceStore tempStorage) {
		String temp = null;
		try {
			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.USE_ABSOLUTEPATH_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.USE_ABSOLUTEPATH_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.GNU_MAKE_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.GNU_MAKE_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.INCREMENTAL_DEPENDENCY_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.INCREMENTAL_DEPENDENCY_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.DYNAMIC_LINKING_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.DYNAMIC_LINKING_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.FUNCTIONTESTRUNTIME_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.FUNCTIONTESTRUNTIME_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.SINGLEMODE_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.SINGLEMODE_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.CODE_SPLITTING_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.CODE_SPLITTING_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.DEFAULT_TARGET_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.DEFAULT_TARGET_PROPERTY, temp);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.TARGET_EXECUTABLE_PROPERTY));
			if (temp != null) {
				tempStorage.setValue(MakefileCreationData.TARGET_EXECUTABLE_PROPERTY, temp);
			}
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}
	}

	/**
	 * Evaluates the properties on the option page, and compares them with
	 * the saved values.
	 * 
	 * @param project
	 *                the actual project (the real preference store).
	 * @param tempStorage
	 *                the temporal store to copy the values to.
	 * 
	 * @return true if the values in the real and the temporal storage are
	 *         different (they have changed), false otherwise.
	 * */
	public boolean evaluatePropertyStore(final IProject project, final PreferenceStore tempStorage) {
		boolean result = false;

		String actualValue = null;
		String copyValue = null;
		try {
			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.USE_ABSOLUTEPATH_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.USE_ABSOLUTEPATH_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.GNU_MAKE_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.GNU_MAKE_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.INCREMENTAL_DEPENDENCY_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.INCREMENTAL_DEPENDENCY_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.DYNAMIC_LINKING_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.DYNAMIC_LINKING_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.FUNCTIONTESTRUNTIME_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.FUNCTIONTESTRUNTIME_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.SINGLEMODE_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.SINGLEMODE_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.CODE_SPLITTING_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.CODE_SPLITTING_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.DEFAULT_TARGET_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.DEFAULT_TARGET_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));

			actualValue = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.TARGET_EXECUTABLE_PROPERTY));
			copyValue = tempStorage.getString(MakefileCreationData.TARGET_EXECUTABLE_PROPERTY);
			result |= ((actualValue != null && !actualValue.equals(copyValue)) || (actualValue == null && copyValue == null));
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}

		return result;
	}

	/**
	 * Performs special processing when the ProjectBuildProperty page's
	 * Defaults button has been pressed.
	 */
	protected void performDefaults() {
		useAbsolutePathButton.setSelection(MakefileCreationData.USE_ABSOLUTEPATH_DEFAULT_VALUE);
		gnuMakeButton.setSelection(MakefileCreationData.GNU_MAKE_DEFAULT_VALUE);
		incrementalDependecyRefresh.setSelection(MakefileCreationData.INCREMENTAL_DEPENDENCY_DEFAULT_VALUE);
		dynamicLinking.setSelection(MakefileCreationData.DYNAMIC_LINKING_DEFAULT_VALUE);
		functionTestRuntimeButton.setSelection(MakefileCreationData.FUNCTIONTESTRUNTIME_DEFAULT_VALUE);
		singleModeButton.setSelection(MakefileCreationData.SINGLEMODE_DEFAULT_VALUE);
		codeSplitting.setSelectedValue(MakefileCreationData.CODE_SPLITTING_DEFAULT_VALUE);
		codeSplittingNumber.setStringValue(GeneralConstants.NUMBER_DEFAULT);
		defaultTarget.setSelectedValue(MakefileCreationData.DefaultTarget.getDefault().toString());
		temporalTargetExecutableFileFieldEditor.setStringValue(MakefileCreationData.getDefaultTargetExecutableName(project));
		setMakefileGenerationEnabled(true); // everything is enabled in the list just listed
	}

	/**
	 * Checks the properties of this page for errors.
	 * 
	 * @param page
	 *                the property page to report the errors to.
	 * @return true if no error was found, false otherwise.
	 * */
	public boolean checkProperties(final ProjectBuildPropertyPage page) {
		if (dynamicLinking.getSelection()) {
			if (!gnuMakeButton.getSelection()) {
				page.setErrorMessage("Dynamic linking is only supported with GNU makefile right now.");
				return false;
			}

			if (Platform.OS_WIN32.equals(Platform.getOS())) {
				page.setErrorMessage("Dynamic linking is not supported on windows right now.");
				return false;
			}
		}

		if (incrementalDependecyRefresh.getSelection()) {
			if (!gnuMakeButton.getSelection()) {
				page.setErrorMessage("Incremental dependency generation is only available when the Makefile is generated for use with GNU make");
				return false;
			}
		}

		if ("".equals(temporalTargetExecutableFileFieldEditor.getStringValue()) || !temporalTargetExecutableFileFieldEditor.isValid()) {
			String errorMessage = temporalTargetExecutableFileFieldEditor.getErrorMessage();
			if (errorMessage == null) {
				errorMessage = "The target executable is not set";
			}
			page.setErrorMessage(errorMessage);
			return false;
		}

		if ( GeneralConstants.NUMBER.equals(codeSplitting.getActualValue())){
			try {
				codeSplittingNumber.getIntValue();
			} catch (NumberFormatException e) {
				page.setErrorMessage(INVALID_CODESPLITTING_NUMBER + codeSplittingNumber.getStringValue());
				return false;
			}
		}

		return true;
	}

	/**
	 * Loads the properties from the property storage, into the user
	 * interface elements.
	 * 
	 * @param project
	 *                the project to load the properties from.
	 * */
	public void loadProperties(final IProject project) {
		String temp;
		boolean useAbsolutePath = false;
		try {
			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.USE_ABSOLUTEPATH_PROPERTY));
			useAbsolutePath = "true".equals(temp) ? true : false;
			useAbsolutePathButton.setSelection(useAbsolutePath);
			 

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.GNU_MAKE_PROPERTY));
			gnuMakeButton.setSelection("true".equals(temp) ? true : false);

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.INCREMENTAL_DEPENDENCY_PROPERTY));
			incrementalDependecyRefresh.setSelection("true".equals(temp) ? true : false);

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.DYNAMIC_LINKING_PROPERTY));
			dynamicLinking.setSelection("true".equals(temp) ? true : false);

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.FUNCTIONTESTRUNTIME_PROPERTY));
			functionTestRuntimeButton.setSelection("true".equals(temp) ? true : false);

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.SINGLEMODE_PROPERTY));
			singleModeButton.setSelection("true".equals(temp) ? true : false);

			//Code Splitting:
			String codeSplittingType = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.CODE_SPLITTING_PROPERTY));
			if (codeSplittingType == null || codeSplittingType.length() == 0) {
				codeSplitting.setSelectedValue(GeneralConstants.NONE);
				codeSplittingNumber.setStringValue(GeneralConstants.NUMBER_DEFAULT);
			} else if (codeSplittingType.equals(GeneralConstants.NONE) || codeSplittingType.equals(GeneralConstants.TYPE)){
				codeSplitting.setSelectedValue(codeSplittingType);
				codeSplittingNumber.setStringValue(GeneralConstants.NUMBER_DEFAULT);
			} else {
				//number supposed:
				codeSplittingNumber.setStringValue(codeSplittingType);
				try {
					codeSplittingNumber.getIntValue();
					codeSplitting.setSelectedValue(GeneralConstants.NUMBER);
				} catch (final NumberFormatException e) {
					//not number is set:
					codeSplitting.setSelectedValue(GeneralConstants.NONE);
					codeSplittingNumber.setStringValue(GeneralConstants.NUMBER_DEFAULT);
					ErrorReporter.INTERNAL_ERROR(INVALID_CODESPLITTING_NUMBER + codeSplittingNumber.getStringValue());
				}
			}
			updateCodeSplittingNumberEnabled();

			//Default target:
			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.DEFAULT_TARGET_PROPERTY));
			if (temp == null || temp.length() == 0) {
				defaultTarget.setSelectedValue(MakefileCreationData.DefaultTarget.getDefault().toString());
			} else {
				try {
					defaultTarget.setSelectedValue(MakefileCreationData.DefaultTarget.createInstance(temp).toString());
				} catch (final IllegalArgumentException e) {
					ErrorReporter.INTERNAL_ERROR("Unknown default target in makefile creation tab: " + temp);
				}
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.TARGET_EXECUTABLE_PROPERTY));
			if (temp == null) {
				temporalTargetExecutableFileFieldEditor.setStringValue(MakefileCreationData.getDefaultTargetExecutableName(project,useAbsolutePath));
			} else {
				temporalTargetExecutableFileFieldEditor.setStringValue(temp);
			}
		} catch (CoreException e) {
			codeSplitting.setSelectedValue(MakefileCreationData.CODE_SPLITTING_DEFAULT_VALUE);
			codeSplittingNumber.setStringValue(GeneralConstants.NUMBER_DEFAULT);
			defaultTarget.setSelectedValue(MakefileCreationData.DefaultTarget.getDefault().toString());
			temporalTargetExecutableFileFieldEditor.setStringValue(MakefileCreationData.getDefaultTargetExecutableName(project,useAbsolutePath));
			setMakefileGenerationEnabled(false);
		}
	}

	/**
	 * Saves the properties to the property storage, from the user interface
	 * elements.
	 * 
	 * @param project
	 *                the project to save the properties to.
	 * @return true if the save was successful, false otherwise.
	 * */
	public boolean saveProperties(final IProject project) {
		try {
			setProperty(project, MakefileCreationData.USE_ABSOLUTEPATH_PROPERTY, useAbsolutePathButton.getSelection() ? "true" : "false");
			setProperty(project, MakefileCreationData.GNU_MAKE_PROPERTY, gnuMakeButton.getSelection() ? "true" : "false");
			setProperty(project, MakefileCreationData.INCREMENTAL_DEPENDENCY_PROPERTY,
					incrementalDependecyRefresh.getSelection() ? "true" : "false");
			setProperty(project, MakefileCreationData.DYNAMIC_LINKING_PROPERTY, dynamicLinking.getSelection() ? "true" : "false");
			setProperty(project, MakefileCreationData.FUNCTIONTESTRUNTIME_PROPERTY, functionTestRuntimeButton.getSelection() ? "true"
					: "false");
			setProperty(project, MakefileCreationData.SINGLEMODE_PROPERTY, singleModeButton.getSelection() ? "true" : "false");

			String codeSplittingActualValue = codeSplitting.getActualValue();

			if (codeSplittingActualValue.equals(GeneralConstants.NONE) || codeSplittingActualValue.equals(GeneralConstants.TYPE)) {
				setProperty(project, MakefileCreationData.CODE_SPLITTING_PROPERTY, codeSplittingActualValue);
			} else if ( codeSplittingActualValue.equals(GeneralConstants.NUMBER)) {
				try {
					codeSplittingNumber.getIntValue();
					setProperty(project, MakefileCreationData.CODE_SPLITTING_PROPERTY, codeSplittingNumber.getStringValue());
				} catch( NumberFormatException e)  {
					ErrorReporter.logError(INVALID_CODESPLITTING_NUMBER + codeSplittingNumber.getStringValue());
				}
			} else {
				setProperty(project, MakefileCreationData.CODE_SPLITTING_PROPERTY, GeneralConstants.NONE);
				ErrorReporter.INTERNAL_ERROR("Invalid code splitting value in makefile creation tab:" +  codeSplittingActualValue);
			}

			setProperty(project, MakefileCreationData.DEFAULT_TARGET_PROPERTY, defaultTarget.getActualValue());
			String temp = temporalTargetExecutableFileFieldEditor.getStringValue();
			URI path = URIUtil.toURI(temp);
			URI resolvedPath = TITANPathUtilities.resolvePathURI(temp, project.getLocation().toOSString());
			if (path.equals(resolvedPath)) {
				temp = PathUtil.getRelativePath(project.getLocation().toOSString(), temp);
			}

			setProperty(project, MakefileCreationData.TARGET_EXECUTABLE_PROPERTY, temp);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
			return false;
		}
		return true;
	}

	/**
	 * Sets the provided value, on the provided project, for the provided
	 * property.
	 * 
	 * @param project
	 *                the project to work on.
	 * @param name
	 *                the name of the property to change.
	 * @param value
	 *                the value to set.
	 * 
	 * @exception CoreException
	 *                    if this method fails. Reasons include:
	 *                    <ul>
	 *                    <li>This project does not exist.</li>
	 *                    <li>This project is not local.</li>
	 *                    <li>This project is a project that is not open.</li>
	 *                    <li>Resource changes are disallowed during certain
	 *                    types of resource change event notification. See
	 *                    <code>IResourceChangeEvent</code> for more
	 *                    details.</li>
	 *                    </ul>
	 * */
	private void setProperty(final IProject project, final String name, final String value) throws CoreException {
		final QualifiedName qualifiedName = new QualifiedName(ProjectBuildPropertyData.QUALIFIER, name);
		final String oldValue = project.getPersistentProperty(qualifiedName);
		if (value != null && !value.equals(oldValue)) {
			project.setPersistentProperty(qualifiedName, value);
		}
	}

	private void checkCodeSplittingNumber() {
		if(!codeSplittingNumber.isValid()) {
			page.setErrorMessage(INVALID_CODESPLITTING_NUMBER+codeSplittingNumber.getStringValue());
		} else {
			page.setErrorMessage(null);
		}
	}

	private void updateCodeSplittingNumberEnabled() {
		if ( makeFileGenerationEnabled && GeneralConstants.NUMBER.equals(codeSplitting.getActualValue())) {
			codeSplittingNumber.setEnabled(true, codeSplittingNumberComposite);
			checkCodeSplittingNumber();
		} else {
			codeSplittingNumber.setEnabled(false, codeSplittingNumberComposite);
			page.setErrorMessage(null);
		}
	}
}
