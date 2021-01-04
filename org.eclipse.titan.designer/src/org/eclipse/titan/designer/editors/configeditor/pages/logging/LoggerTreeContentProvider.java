/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.logging;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.parsers.cfg.indices.LoggingSectionHandler;

/**
 * @author Adam Delic
 * */
public final class LoggerTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(final Object inputElement) {
		if (inputElement instanceof LoggingSectionHandler) {
			final LoggingSectionHandler lsh = (LoggingSectionHandler) inputElement;
			return lsh.getComponentsTreeElementArray();
		}
		return new Object[] {};
	}

	@Override
	public void dispose() {
		//Do nothing
	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		//Do nothing
	}

	@Override
	public Object[] getChildren(final Object parentElement) {
		if (parentElement instanceof LoggingSectionHandler.LoggerTreeElement) {
			final LoggingSectionHandler.LoggerTreeElement compLTE = (LoggingSectionHandler.LoggerTreeElement) parentElement;
			if (compLTE.getPluginName() != null) {
				ErrorReporter.INTERNAL_ERROR("plugin has children");
			}
			return compLTE.getLsh().getPluginsTreeElementArray(compLTE.getComponentName());
		}
		return new Object[] {};
	}

	@Override
	public Object getParent(final Object element) {
		if (element instanceof LoggingSectionHandler.LoggerTreeElement) {
			final LoggingSectionHandler.LoggerTreeElement lte = (LoggingSectionHandler.LoggerTreeElement) element;
			if (lte.getPluginName() == null) {
				// this is a component element
				return null;
			}

			// this is a plugin element
			return new LoggingSectionHandler.LoggerTreeElement(lte.getLsh(), lte.getComponentName());
		}
		return null;
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof LoggingSectionHandler.LoggerTreeElement) {
			final LoggingSectionHandler.LoggerTreeElement lte = (LoggingSectionHandler.LoggerTreeElement) element;
			return lte.getPluginName() == null;
		}

		return false;
	}

}
