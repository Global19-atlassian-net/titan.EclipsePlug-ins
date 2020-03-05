/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.actions;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.titan.common.parsers.cfg.CfgLocation;
import org.eclipse.titan.designer.AST.Assignment;

/**
 * @author Kristof Szabados
 * */
public final class OpenDeclarationLabelProvider implements ILabelProvider {

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse
	 * .jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(final ILabelProviderListener listener) {
		//Do nothing
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		//Do nothing
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(final Object element) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(final Object element) {
		if (element instanceof ConfigDeclarationCollectionHelper) {
			final CfgLocation location = ((ConfigDeclarationCollectionHelper) element).location;
			return ((ConfigDeclarationCollectionHelper) element).description + " - " + location.getFile().getName();
		} else if (element instanceof Assignment) {
			final Assignment assignment = (Assignment) element;
			return assignment.getDescription() + " - " + assignment.getLocation().getFile().getName();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java
	 * .lang.Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(final Object element, final String property) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse
	 * .jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(final ILabelProviderListener listener) {
		//Do nothing
	}

}
