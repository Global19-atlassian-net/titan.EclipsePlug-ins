/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.preferences.pages;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.titanium.preferences.PreferenceConstants;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class GraphClusterAutoPage extends GraphClusterPage implements IWorkbenchPreferencePage {

	private static final String DESCRIPTION = "Settings for the automatic clustering tool";

	@Override
	public void init(final IWorkbench workbench) {
		setDescription(DESCRIPTION);
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite page = new Composite(parent, 0);
		page.setLayout(new GridLayout(1, false));
		Composite inner;

		inner = setupLabel(page, "Clustering tools",
				"List of clustering tools, whose clustering will be improved by the automatic clustering tool.\n"
						+ "If more than one is selected, the 'best' one will be displayed.");

		setupBooleanEditor(inner, PreferenceConstants.CLUSTER_AUTO_FOLDER, "Improve clustering by folder name",
				"The automatic clustering tool will improve the clustering created from the folders.");

		setupBooleanEditor(inner, PreferenceConstants.CLUSTER_AUTO_REGEXP, "Improve clustering by the regular expressions",
				"The automatic clustering tool will improve the clustering created from the regular expressions.");

		setupBooleanEditor(inner, PreferenceConstants.CLUSTER_AUTO_NAME, "Improve clustering by module name",
				"The automatic clustering tool will improve the clustering created from the module names.");

		inner = setupLabel(page, "Inner settings", "Settings for the algorithm.");

		setupIntegerEditor(inner, PreferenceConstants.CLUSTER_ITERATION, "Maximum number of iterations",
				"The number of iterations used to improve a clustering. If set too high, the algorithm will be slow.");

		setupIntegerEditor(
				inner,
				PreferenceConstants.CLUSTER_SIZE_LIMIT,
				"Maximum number of clusters",
				"If the initial number of clusters is higher than the given value,"
						+ "the algorithm will eliminate some by merging them and running the algorithm again. "
						+ "The value should be around the number of clusters created by other clustering tools.");

		initialize();
		return page;
	}
}
