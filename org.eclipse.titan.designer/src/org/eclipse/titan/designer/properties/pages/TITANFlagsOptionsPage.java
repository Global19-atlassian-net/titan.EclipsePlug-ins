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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;
import org.eclipse.titan.designer.properties.data.TITANFlagsOptionsData;

/**
 * @author Kristof Szabados
 * */
public final class TITANFlagsOptionsPage implements IOptionsPage {
	private Composite mainComposite;

	private Button disableBER;
	private Button disableRAW;
	private Button disableTEXT;
	private Button disableXER;
	private Button disableJSON;
	private Button disableOER;
	private Button forceXER;
	private Button disableSubtypeChecking;
	private Button disableAttributeValidation;
	private Button defaultAsOmit;
	//private Button enumHack;
	private Button forceOldFuncOutPar;
	private Button gccMessageFormat;
	private Button lineNumbersOnlyInMessages;
	private Button includeSourceInfo;
	private Button addSourceLineInfo;
	private Button suppressWarnings;
	private Button quietly;
	private Button omitInValueList;
	private Button warningsForBadVariants;
	private Button ignoreUntaggedOnTopLevelUnion;
	private Button enableLegacyEncoding;
	private Button disableUserInformation;
	private Button enableRealtimeFeature;
	private Button forceGenSeof;
	private Button activateDebugger;

	//private Composite namingRuleComposite; //TODO: check: is this obsolete?
	//private ComboFieldEditor namingRules;  //TODO: check: is this obsolete?

	private final boolean CBuilder;

	public TITANFlagsOptionsPage(final boolean CBuilder) {
		this.CBuilder = CBuilder;
	}

	@Override
	public void dispose() {
		if (mainComposite != null) {
			mainComposite.dispose();
			mainComposite = null;

			if (CBuilder) {
				disableBER.dispose();
			}

			disableRAW.dispose();

			if (CBuilder) {
				disableTEXT.dispose();
				disableXER.dispose();
				disableJSON.dispose();
				disableOER.dispose();
				forceXER.dispose();
				disableSubtypeChecking.dispose();
			}

			disableAttributeValidation.dispose();
			if (CBuilder) {
				defaultAsOmit.dispose();
				forceOldFuncOutPar.dispose();
				gccMessageFormat.dispose();
				lineNumbersOnlyInMessages.dispose();
				includeSourceInfo.dispose();
			}

			addSourceLineInfo.dispose();

			if (CBuilder) {
				suppressWarnings.dispose();
				quietly.dispose();
			}

			omitInValueList.dispose();

			if (CBuilder) {
				warningsForBadVariants.dispose();
				ignoreUntaggedOnTopLevelUnion.dispose();
				enableLegacyEncoding.dispose();
				disableUserInformation.dispose();
				enableRealtimeFeature.dispose();
			}

			forceGenSeof.dispose();

			if (CBuilder) {
				activateDebugger.dispose();
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
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (CBuilder) {
			disableBER = new Button(mainComposite, SWT.CHECK);
			disableBER.setText("Disable BER encoding (-b)");
		}

		disableRAW = new Button(mainComposite, SWT.CHECK);
		disableRAW.setText("Disable RAW encoding (-r)");

		if (CBuilder) {
			disableTEXT = new Button(mainComposite, SWT.CHECK);
			disableTEXT.setText("Disable TEXT encoding (-x)");

			disableXER = new Button(mainComposite, SWT.CHECK);
			disableXER.setText("Disable XER encoding (-X)");

			disableJSON = new Button(mainComposite, SWT.CHECK);
			disableJSON.setText("Disable JSON encoder (-j)");

			disableOER = new Button(mainComposite, SWT.CHECK);
			disableOER.setText("Disable OER encoder (-O)");

			forceXER = new Button(mainComposite, SWT.CHECK);
			forceXER.setText("Force XER in ASN.1 files (-a)");

			disableSubtypeChecking = new Button(mainComposite, SWT.CHECK);
			disableSubtypeChecking.setText("Disable subtype checking (-y)");
		}

		disableAttributeValidation = new Button(mainComposite,  SWT.CHECK);
		disableAttributeValidation.setText("Disable attribute validation (-0)");
		if (CBuilder) {	
			defaultAsOmit = new Button(mainComposite, SWT.CHECK);
			defaultAsOmit.setText("Treat default fields as omit (-d)");
		
			forceOldFuncOutPar = new Button(mainComposite, SWT.CHECK);
			forceOldFuncOutPar.setText("Force old function out par handling (-Y)");
		
			gccMessageFormat = new Button(mainComposite, SWT.CHECK);
			gccMessageFormat.setText("Emulate gcc error/warning message format (-g)");

			lineNumbersOnlyInMessages = new Button(mainComposite, SWT.CHECK);
			lineNumbersOnlyInMessages.setText("Use only line numbers in error/warning messages (-i)");

			includeSourceInfo = new Button(mainComposite, SWT.CHECK);
			includeSourceInfo.setText("Include source line info in C++ code (-l)");
		}

		addSourceLineInfo = new Button(mainComposite, SWT.CHECK);
		addSourceLineInfo.setText("Add source line info for logging (-L)");

		if (CBuilder) {
			suppressWarnings = new Button(mainComposite, SWT.CHECK);
			suppressWarnings.setText("Suppress warnings (-w)");

			quietly = new Button(mainComposite, SWT.CHECK);
			quietly.setText("Suppress all messages (quiet mode) (-q)");
		}

		omitInValueList = new Button(mainComposite, SWT.CHECK);
		omitInValueList.setText("Allow 'omit' in template value lists (legacy behavior) (-M)");

		if (CBuilder) {
			warningsForBadVariants = new Button(mainComposite, SWT.CHECK);
			warningsForBadVariants.setText("Display warnings instead of errors for invalid variants (-E)");

			ignoreUntaggedOnTopLevelUnion = new Button(mainComposite, SWT.CHECK);
			ignoreUntaggedOnTopLevelUnion.setText("Ignore UNTAGGED enc. instr. on top level unions (legacy behaviour) (-N)");

			enableLegacyEncoding = new Button(mainComposite, SWT.CHECK);
			enableLegacyEncoding.setText("Enable legacy encoding (-e)");

			disableUserInformation = new Button(mainComposite, SWT.CHECK);
			disableUserInformation.setText("Disable user information and timestamp in headers (-D)");

			enableRealtimeFeature = new Button(mainComposite, SWT.CHECK);
			enableRealtimeFeature.setText("Enable Realtime testing feature (-I)");
		}

		forceGenSeof = new Button(mainComposite, SWT.CHECK);
		forceGenSeof.setText("Force the generation of Seof types (-F)");

		if (CBuilder) {
			activateDebugger = new Button(mainComposite, SWT.CHECK);
			activateDebugger.setText("Activate debugger (generates extra code for debugging) (-n)");
		}

		return mainComposite;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		if (mainComposite == null) {
			return;
		}

		if (CBuilder) {
			disableBER.setEnabled(enabled);
		}

		disableRAW.setEnabled(enabled);

		if (CBuilder) {
			disableTEXT.setEnabled(enabled);
			disableXER.setEnabled(enabled);
			disableJSON.setEnabled(enabled);
			disableOER.setEnabled(enabled);
			forceXER.setEnabled(enabled);
			disableSubtypeChecking.setEnabled(enabled);
		}

		disableAttributeValidation.setEnabled(enabled);

		if (CBuilder) {
			defaultAsOmit.setEnabled(enabled);
			forceOldFuncOutPar.setEnabled(enabled);
			gccMessageFormat.setEnabled(enabled);
			lineNumbersOnlyInMessages.setEnabled(enabled);
			includeSourceInfo.setEnabled(enabled);
		}

		addSourceLineInfo.setEnabled(enabled);

		if (CBuilder) {
			suppressWarnings.setEnabled(enabled);
			quietly.setEnabled(enabled);
		}

		omitInValueList.setEnabled(enabled);

		if (CBuilder) {
			warningsForBadVariants.setEnabled(enabled);
			ignoreUntaggedOnTopLevelUnion.setEnabled(enabled);
			enableLegacyEncoding.setEnabled(enabled);
			disableUserInformation.setEnabled(enabled);
			enableRealtimeFeature.setEnabled(enabled);
		}

		forceGenSeof.setEnabled(enabled);

		if (CBuilder) {
			activateDebugger.setEnabled(enabled);
		}
	}

	@Override
	public void copyPropertyStore(final IProject project, final PreferenceStore tempStorage) {
		String temp = null;
		for (int i = 0; i < TITANFlagsOptionsData.PROPERTIES.length; i++) {
			try {
				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.PROPERTIES[i]));
				if (temp != null) {
					tempStorage.setValue(TITANFlagsOptionsData.PROPERTIES[i], temp);
				}
			} catch (CoreException ce) {
				ErrorReporter.logExceptionStackTrace(ce);
			}
		}
	}

	@Override
	public boolean evaluatePropertyStore(final IProject project, final PreferenceStore tempStorage) {
		String actualValue = null;
		String copyValue = null;
		boolean result = false;
		for (int i = 0; i < TITANFlagsOptionsData.PROPERTIES.length; i++) {
			try {
				actualValue = project.getPersistentProperty(
						new QualifiedName(ProjectBuildPropertyData.QUALIFIER, TITANFlagsOptionsData.PROPERTIES[i]));
				copyValue = tempStorage.getString(TITANFlagsOptionsData.PROPERTIES[i]);
				result |= ((actualValue != null && !actualValue.equals(copyValue))
						|| (actualValue == null && copyValue == null));
				if (result) {
					return true;
				}
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace(e);
				result = true;
			}
		}

		return result;
	}

	@Override
	public void performDefaults() {
		if (mainComposite == null) {
			return;
		}

		setEnabled(true);

		if (CBuilder) {
			disableBER.setSelection(false);
		}

		disableRAW.setSelection(false);

		if (CBuilder) {
			disableTEXT.setSelection(false);
			disableXER.setSelection(false);
			disableJSON.setSelection(false);
			disableOER.setSelection(false);
			forceXER.setSelection(false);
			disableSubtypeChecking.setSelection(false);
		}

		disableAttributeValidation.setSelection(false);
		if (CBuilder) {
			defaultAsOmit.setSelection(false);
			forceOldFuncOutPar.setSelection(false);
			gccMessageFormat.setSelection(false);
			lineNumbersOnlyInMessages.setSelection(false);
			includeSourceInfo.setSelection(true);
		}

		addSourceLineInfo.setSelection(true);

		if (CBuilder) {
			suppressWarnings.setSelection(false);
			quietly.setSelection(false);
		}

		omitInValueList.setSelection(false);

		if (CBuilder) {
			warningsForBadVariants.setSelection(false);
			ignoreUntaggedOnTopLevelUnion.setSelection(false);
			enableLegacyEncoding.setSelection(false);
			disableUserInformation.setSelection(false);
			enableRealtimeFeature.setSelection(false);
		}

		forceGenSeof.setSelection(false);

		if (CBuilder) {
			activateDebugger.setSelection(false);
		}
	}

	@Override
	public boolean checkProperties(final ProjectBuildPropertyPage page) {
		final boolean xerDisabled = disableXER.getSelection();
		final boolean xerOnASN1Forced = forceXER.getSelection();

		if (xerDisabled && xerOnASN1Forced) {
			page.setErrorMessage("Forcing XER in ASN.1 files and disabling XER are incompatible options.");
			return false;
		}

		return true;
	}

	@Override
	public void loadProperties(final IProject project) {
		String temp;
		try {
			if (CBuilder) {
				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DISABLE_BER_PROPERTY));
				disableBER.setSelection("true".equals(temp) ? true : false);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TITANFlagsOptionsData.DISABLE_RAW_PROPERTY));
			disableRAW.setSelection("true".equals(temp) ? true : false);

			if (CBuilder) {
				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DISABLE_TEXT_PROPERTY));
				disableTEXT.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DISABLE_XER_PROPERTY));
				disableXER.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DISABLE_JSON_PROPERTY));
				disableJSON.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DISABLE_OER_PROPERTY));
				disableOER.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.FORCE_XER_IN_ASN1_PROPERTY));
				forceXER.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DISABLE_SUBTYPE_CHECKING_PROPERTY));
				disableSubtypeChecking.setSelection("true".equals(temp) ? true : false);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TITANFlagsOptionsData.DISABLE_ATTRIBUTE_VALIDATION_PROPERTY));
			disableAttributeValidation.setSelection("true".equals(temp) ? true : false);
			if (CBuilder) {
				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DEFAULT_AS_OMIT_PROPERTY));
				defaultAsOmit.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.FORCE_OLD_FUNC_OUT_PAR_PROPERTY));
				forceOldFuncOutPar.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.GCC_MESSAGE_FORMAT_PROPERTY));
				gccMessageFormat.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.LINE_NUMBERS_ONLY_IN_MESSAGES_PROPERTY));
				lineNumbersOnlyInMessages.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.INCLUDE_SOURCEINFO_PROPERTY));
				includeSourceInfo.setSelection("true".equals(temp) ? true : false);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TITANFlagsOptionsData.ADD_SOURCELINEINFO_PROPERTY));
			if (temp == null) {
				addSourceLineInfo.setSelection(false);
			} else {
				addSourceLineInfo.setSelection("true".equals(temp) ? true : false);
			}

			if (CBuilder) {
				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.SUPPRESS_WARNINGS_PROPERTY));
				suppressWarnings.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.QUIETLY_PROPERTY));
				quietly.setSelection("true".equals(temp) ? true : false);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TITANFlagsOptionsData.ALLOW_OMIT_IN_VALUELIST_TEMPLATE_PROPERTY));
			omitInValueList.setSelection("true".equals(temp) ? true : false);

			if (CBuilder) {
				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.WARNINGS_FOR_BAD_VARIANTS_PROPERTY));
				warningsForBadVariants.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.IGNORE_UNTAGGED_ON_TOP_LEVEL_UNION_PROPERTY));
				ignoreUntaggedOnTopLevelUnion.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.ENABLE_LEGACY_ENCODING_PROPERTY));
				enableLegacyEncoding.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.DISABLE_USER_INFORMATION_PROPERTY));
				disableUserInformation.setSelection("true".equals(temp) ? true : false);

				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.ENABLE_REALTIME));
				enableRealtimeFeature.setSelection("true".equals(temp) ? true : false);
			}

			temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TITANFlagsOptionsData.FORCE_GEN_SEOF));
			forceGenSeof.setSelection("true".equals(temp) ? true : false);

			if (CBuilder) {
				temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TITANFlagsOptionsData.ACTIVATE_DEBUGGER_PROPERTY));
				activateDebugger.setSelection("true".equals(temp) ? true : false);

			}
		} catch (CoreException e) {
			performDefaults();
		}
	}

	@Override
	public boolean saveProperties(final IProject project) {
		try {
			if (CBuilder) {
				setProperty(project, TITANFlagsOptionsData.DISABLE_BER_PROPERTY, disableBER.getSelection() ? "true" : "false");
			}

			setProperty(project, TITANFlagsOptionsData.DISABLE_RAW_PROPERTY, disableRAW.getSelection() ? "true" : "false");

			if (CBuilder) {
				setProperty(project, TITANFlagsOptionsData.DISABLE_TEXT_PROPERTY, disableTEXT.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.DISABLE_XER_PROPERTY, disableXER.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.DISABLE_JSON_PROPERTY, disableJSON.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.DISABLE_OER_PROPERTY, disableOER.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.FORCE_XER_IN_ASN1_PROPERTY, forceXER.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.DISABLE_SUBTYPE_CHECKING_PROPERTY, disableSubtypeChecking.getSelection() ? "true"
						: "false");
			}

			setProperty(project, TITANFlagsOptionsData.DISABLE_ATTRIBUTE_VALIDATION_PROPERTY, disableAttributeValidation.getSelection() ? "true"
					: "false");
			if (CBuilder) {
				setProperty(project, TITANFlagsOptionsData.DEFAULT_AS_OMIT_PROPERTY, defaultAsOmit.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.FORCE_OLD_FUNC_OUT_PAR_PROPERTY, forceOldFuncOutPar.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.GCC_MESSAGE_FORMAT_PROPERTY, gccMessageFormat.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.LINE_NUMBERS_ONLY_IN_MESSAGES_PROPERTY,
						lineNumbersOnlyInMessages.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.INCLUDE_SOURCEINFO_PROPERTY, includeSourceInfo.getSelection() ? "true" : "false");
			}

			setProperty(project, TITANFlagsOptionsData.ADD_SOURCELINEINFO_PROPERTY, addSourceLineInfo.getSelection() ? "true" : "false");

			if (CBuilder) {
				setProperty(project, TITANFlagsOptionsData.SUPPRESS_WARNINGS_PROPERTY, suppressWarnings.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.QUIETLY_PROPERTY, quietly.getSelection() ? "true" : "false");
			}

			setProperty(project, TITANFlagsOptionsData.ALLOW_OMIT_IN_VALUELIST_TEMPLATE_PROPERTY ,  omitInValueList.getSelection() ? "true" : "false");

			if (CBuilder) {
				setProperty(project, TITANFlagsOptionsData.WARNINGS_FOR_BAD_VARIANTS_PROPERTY,  warningsForBadVariants.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.IGNORE_UNTAGGED_ON_TOP_LEVEL_UNION_PROPERTY,  ignoreUntaggedOnTopLevelUnion.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.ENABLE_LEGACY_ENCODING_PROPERTY, enableLegacyEncoding.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.DISABLE_USER_INFORMATION_PROPERTY, disableUserInformation.getSelection() ? "true" : "false");
				setProperty(project, TITANFlagsOptionsData.ENABLE_REALTIME, enableRealtimeFeature.getSelection() ? "true" : "false");
			}

			setProperty(project, TITANFlagsOptionsData.FORCE_GEN_SEOF, forceGenSeof.getSelection() ? "true" : "false");

			if (CBuilder) {
				setProperty(project, TITANFlagsOptionsData.ACTIVATE_DEBUGGER_PROPERTY, activateDebugger.getSelection() ? "true" : "false");
			}

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
}
