/*******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/

package org.eclipse.titan.log.viewer.views.text.table;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.titan.log.viewer.Activator;
import org.eclipse.titan.log.viewer.utils.Constants;
import org.eclipse.titan.log.viewer.utils.Messages;
import org.eclipse.titan.log.viewer.views.DetailsView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

class CloseAllAction extends Action {

	public CloseAllAction() {
		super("", ImageDescriptor.createFromImage(Activator.getDefault().getIcon(Constants.ICONS_MSC_DELETE)));
		setId("closeTextTable");
		setToolTipText(Messages.getString("TextTableView.6"));
	}

	@Override
	public void run() {

		final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		final IViewReference[] viewReferences = activePage.getViewReferences();

		for (final IViewReference reference : viewReferences) {
			final IViewPart view = reference.getView(false);

			// memento restored views that never have had focus are null!!!
			if (view == null) {
				activePage.hideView(reference);
			} else if (view instanceof TextTableView) {
				activePage.hideView(reference);
			}
		}

		// Clear Details View if needed
		final DetailsView detailsView = (DetailsView) activePage.findView(Constants.DETAILS_VIEW_ID);
		if (detailsView != null
				&& "".equals(detailsView.getTestCaseName())) {
			detailsView.setData(null, false);
		}
	}
}
