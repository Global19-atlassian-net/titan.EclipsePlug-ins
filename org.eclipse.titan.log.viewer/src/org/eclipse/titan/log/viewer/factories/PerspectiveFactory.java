/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.factories;

import org.eclipse.titan.log.viewer.utils.Constants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Class responsible for creating the perspective layout
 *
 */
public class PerspectiveFactory implements IPerspectiveFactory {
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	@Override
	public void createInitialLayout(final IPageLayout layout) {

		final String editorArea = layout.getEditorArea();

		final IFolderLayout left = layout.createFolder(Constants.LAYOUT_LEFT, IPageLayout.LEFT, (float) 0.7, editorArea);
		left.addView("org.eclipse.ui.navigator.ProjectExplorer");

		final IFolderLayout top = layout.createFolder(Constants.LAYOUT_TOP, IPageLayout.RIGHT, (float) 0.25, Constants.LAYOUT_LEFT);
		top.addPlaceholder(org.eclipse.titan.log.viewer.utils.Constants.MSC_VIEW_ID);
		top.addPlaceholder(org.eclipse.titan.log.viewer.utils.Constants.MSC_VIEW_ID + ":*"); //$NON-NLS-1$
		top.addPlaceholder(org.eclipse.titan.log.viewer.utils.Constants.TEXT_TABLE_VIEW_ID);
		top.addPlaceholder(org.eclipse.titan.log.viewer.utils.Constants.TEXT_TABLE_VIEW_ID + ":*"); //$NON-NLS-1$
		top.addPlaceholder(org.eclipse.titan.log.viewer.utils.Constants.STATISTICAL_VIEW_ID);
		top.addPlaceholder(org.eclipse.titan.log.viewer.utils.Constants.STATISTICAL_VIEW_ID + ":*"); //$NON-NLS-1$

		final IFolderLayout bottom = layout.createFolder(Constants.LAYOUT_BOTTOM, IPageLayout.BOTTOM, (float) 0.75, Constants.LAYOUT_TOP);
		bottom.addView(Constants.CONSOLE_ID);
		layout.setEditorAreaVisible(false);
	}

}
