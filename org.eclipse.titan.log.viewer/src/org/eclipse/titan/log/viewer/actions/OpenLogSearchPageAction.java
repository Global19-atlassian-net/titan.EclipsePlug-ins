/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.titan.log.viewer.exceptions.TechnicalException;
import org.eclipse.titan.log.viewer.exceptions.TitanLogExceptionHandler;
import org.eclipse.titan.log.viewer.utils.Constants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class OpenLogSearchPageAction extends Action implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	@Override
	public void run(final IAction action) {
		if (window == null || window.getActivePage() == null) {
			TitanLogExceptionHandler.handleException(new TechnicalException("Could not open the search page."));  //$NON-NLS-1$
			return;
		}
		NewSearchUI.openSearchDialog(window, Constants.LOG_SEARCH_PAGE_ID);
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		// Do nothing
	}

	@Override
	public void dispose() {
		window = null;
	}

	@Override
	public void init(final IWorkbenchWindow window) {
		this.window = window;
	}
}
