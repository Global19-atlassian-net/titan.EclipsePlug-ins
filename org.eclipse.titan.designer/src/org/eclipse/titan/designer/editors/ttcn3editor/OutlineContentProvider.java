/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcn3editor;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.titan.designer.AST.IOutlineElement;

/**
 * @author Kristof Szabados
 *
 * TODO: instead of getOutlineChildren we should use visitors
 * */
public final class OutlineContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(final Object parentElement) {
		if (parentElement instanceof IOutlineElement) {
			return ((IOutlineElement) parentElement).getOutlineChildren();
			// The following two branches are for module importations.
			// The top level module importations are stored in an ArrayList,
			// but module importations in groups are stored in a Vector.
		} else if (parentElement instanceof List<?>) {
			return ((List<?>) parentElement).toArray();
		}

		return new Object[] {};
	}

	@Override
	public Object getParent(final Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof IOutlineElement) {
			final Object[] children = ((IOutlineElement) element).getOutlineChildren();
			return children != null && children.length > 0;
		} else if (element instanceof List<?>) {
			return true;
		}

		return false;
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		return getChildren(inputElement);
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
