/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.logging;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.titan.common.parsers.cfg.indices.LoggingBit;
import org.eclipse.titan.common.parsers.cfg.indices.LoggingBitHelper;

/**
 * @author Kristof Szabados
 * */
public final class LoggingBitsContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(final Object parentElement) {
		if (parentElement == null || !(parentElement instanceof LoggingBit)) {
			return new Object[] {};
		}

		final LoggingBit bit = (LoggingBit) parentElement;
		return LoggingBitHelper.getChildren(bit);
	}

	@Override
	public Object getParent(final Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element == null || !(element instanceof LoggingBit)) {
			return false;
		}

		final LoggingBit bit = (LoggingBit) element;
		return LoggingBitHelper.hasChildren(bit);
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		return LoggingBitHelper.getFirstLevelNodes();
	}

	@Override
	public void dispose() {
		//Do nothing
	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		//Do nothing
	}
}
