/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.preferences.pages;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.titan.log.viewer.Activator;
import org.eclipse.titan.log.viewer.preferences.PreferenceConstants;
import org.eclipse.titan.log.viewer.preferences.fieldeditors.StringListEditor;
import org.eclipse.titan.log.viewer.utils.ImportExportUtils;
import org.eclipse.titan.log.viewer.utils.Messages;
import org.eclipse.ui.IWorkbench;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog.
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */
public class FilteredFunctionsPrefPage extends LogViewerPreferenceRootPage {

	private StringListEditor filterFuntionsEditor;

	/**
	 * Constructor
	 */
	public FilteredFunctionsPrefPage() {
		super(GRID, false);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.getString("FilteredFunctionsPrefPage.0")); //$NON-NLS-1$
	}

	@Override
	public void createFieldEditors() {
		this.filterFuntionsEditor = new StringListEditor(PreferenceConstants.PREF_FILTER_FUNCTION_ID,
				"", //$NON-NLS-1$
				getFieldEditorParent(),
				false);
		this.filterFuntionsEditor.setPreferenceStore(getPreferenceStore());
		addField(this.filterFuntionsEditor);
	}

	@Override
	public void init(final IWorkbench workbench) {
		// Do nothing
	}

	@Override
	protected String getPageId() {
		return PreferenceConstants.PAGE_ID_FILTERED_FUNCTIONS_PAGE;
	}

	/**
	 * Method for getting the current preferences in the preference store
	 * @return map of the preferences
	 */
	private Map<String, String> getCurrentPreferences(final String separator) {
		final Map<String, String> currentPrefs = new HashMap<String, String>();
		String functionFilter = ""; //$NON-NLS-1$
		final String[] elements = this.filterFuntionsEditor.getElements();
		for (final String element : elements) {
			functionFilter = functionFilter.concat(element + separator);
		}
		currentPrefs.put(PreferenceConstants.PREF_FILTER_FUNCTION_ID, functionFilter);
		return currentPrefs;
	}

	/**
	 * Method for getting the current preferences in the preference store
	 * @return map of the preferences
	 */
	private Map<String, String[]> getCurrentPreferencesSeparated() {
		final Map<String, String[]> currentPrefs = new HashMap<String, String[]>();
		currentPrefs.put(this.filterFuntionsEditor.getPreferenceName(), this.filterFuntionsEditor.getElements());
		return currentPrefs;
	}

	@Override
	protected void exportPreferences() {
		ImportExportUtils.exportSettings(getPageId(), getCurrentPreferencesSeparated(), true);
	}

	@Override
	protected void importPreferences() {
		final Map<String, String> prop = ImportExportUtils.importSettings(PreferenceConstants.PAGE_ID_FILTERED_FUNCTIONS_PAGE);
		//if cancel
		if (prop == null) {
			return;
		}
		setOldPreferences(getCurrentPreferences(File.pathSeparator));
		setProperties(prop);
		// Settings changed -> Enable apply button
		getApplyButton().setEnabled(true);
	}

	@Override
	protected void updatePage() {
		this.filterFuntionsEditor.loadDefault();
		this.filterFuntionsEditor.load();
	}
}
