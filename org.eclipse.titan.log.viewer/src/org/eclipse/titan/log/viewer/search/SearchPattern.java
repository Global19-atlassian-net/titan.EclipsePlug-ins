/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.search;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.titan.log.viewer.models.FilterPattern;
import org.eclipse.titan.log.viewer.models.FilterPattern.Field;
import org.eclipse.titan.log.viewer.parsers.data.LogRecord;
import org.eclipse.titan.log.viewer.utils.Constants;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

public class SearchPattern {

	private final FilterPattern filterPattern;
	private final IWorkingSet[] workingSets;
	private final int scope;

	public SearchPattern(final String searchString, final boolean isCaseSensitive, final boolean isRegularExpression,
			final Map<String, Boolean> events, final Map<Field, Boolean> limitTo,
			final int scope, final IWorkingSet[] workingsets) {
		this.filterPattern = new FilterPattern(searchString, limitTo, isCaseSensitive, isRegularExpression);
		this.filterPattern.setEventsToFilter(events, true, false);
		this.scope = scope;
		this.workingSets = workingsets;
	}

	public SearchPattern(final FilterPattern filterPattern, final int scope, final IWorkingSet[] workingsets) {
		this.filterPattern = filterPattern;
		this.scope = scope;
		this.workingSets = workingsets;
	}

	public SortedMap<String, Boolean> getEvents() {
		return filterPattern.getEventsToFilter();
	}

	public String getSearchString() {
		return filterPattern.getFilterExpression();
	}

	public Map<Field, Boolean> getLimitTo() {
		return filterPattern.getFieldsToFilter();
	}

	public boolean isCaseSensitive() {
		return filterPattern.isCaseSensitive();
	}

	public boolean isRegularExpression() {
		return filterPattern.isRegularExpression();
	}

	public IWorkingSet[] getWorkingSets() {
		return workingSets;
	}

	public int getScope() {
		return scope;
	}

	public boolean match(final LogRecord record) {
		return filterPattern.match(record);
	}

	public void store(final IDialogSettings settings) {
		settings.put("searchString", filterPattern.getFilterExpression());
		settings.put("isCaseSensitive", filterPattern.isCaseSensitive());
		settings.put("isRegularExpression", filterPattern.isRegularExpression());

		for (final Entry<String, Boolean> entry : filterPattern.getEventsToFilter().entrySet()) {
			settings.put(entry.getKey(), entry.getValue());
		}

		for (final Entry<Field, Boolean> entry : filterPattern.getFieldsToFilter().entrySet()) {
			settings.put(entry.getKey().toString(), entry.getValue());
		}

		settings.put("scope", this.scope);

		if (workingSets != null) {
			String[] wsIds = new String[workingSets.length];
			for (int i = 0; i < workingSets.length; i++) {
				wsIds[i] = workingSets[i].getName();
			}
			settings.put("workingSets", wsIds);
		} else {
			settings.put("workingSets", new String[0]);
		}
	}

	public static SearchPattern create(final IDialogSettings settings) {
		final String loadedSearchString = settings.get("searchString");
		final boolean loadedIsCaseSensitive = settings.getBoolean("isCaseSensitive");
		final boolean loadedIsRegularExpression = settings.getBoolean("isRegularExpression");

		final SortedMap<String, Boolean> loadedEvents = new TreeMap<String, Boolean>();
		for (final String entry : Constants.EVENT_CATEGORIES.keySet()) {
			loadedEvents.put(entry, settings.getBoolean(entry));
		}

		final Map<Field, Boolean> loadedLimitTo = new HashMap<Field, Boolean>();
		loadedLimitTo.put(Field.SOURCE_INFO, settings.getBoolean(Field.SOURCE_INFO.toString()));
		loadedLimitTo.put(Field.MESSAGE, settings.getBoolean(Field.MESSAGE.toString()));

		final FilterPattern loadedFilterPattern = new FilterPattern(loadedSearchString, loadedLimitTo,
				loadedIsCaseSensitive, loadedIsRegularExpression);
		loadedFilterPattern.setEventsToFilter(loadedEvents, true, false);

		final String[] wsIds = settings.getArray("workingSets"); //$NON-NLS-1$
		IWorkingSet[] workingSets = null;
		if (wsIds != null && wsIds.length > 0) {
			final IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
			workingSets = new IWorkingSet[wsIds.length];
			for (int i = 0; workingSets != null && i < wsIds.length; i++) {
				workingSets[i] = workingSetManager.getWorkingSet(wsIds[i]);
				if (workingSets[i] == null) {
					workingSets = null;
				}
			}
		}

		return new SearchPattern(loadedFilterPattern, settings.getInt("scope"), workingSets);
	}
}
