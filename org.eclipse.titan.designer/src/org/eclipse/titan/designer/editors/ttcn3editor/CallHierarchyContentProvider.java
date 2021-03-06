/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcn3editor;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for the {@link CallHierarchyView}'s TreeViewer.<br>
 * The content provider work on {@link CallHierarchyNode}s.
 * 
 * @see CallHierarchyView
 * @see CallHierarchyNode
 * @author Sandor Balazs
 */
public class CallHierarchyContentProvider implements ITreeContentProvider {
	/**
	 * The <code>callHierarchy</code> object is needed because of the graph processing algorithms.
	 * @see CallHierarchy
	 */
	private final CallHierarchy callHierarchy;

	/**
	 * The content provider's constructor.<br>
	 * The <code>callHierarchy</code> object is needed because of the graph processing algorithms.
	 *
	 * @param callHierarchy
	 * 			The previous created <code>callHierarchy</code> object.
	 * @see CallHierarchy
	 */
	public CallHierarchyContentProvider(final CallHierarchy callHierarchy) {
		this.callHierarchy = callHierarchy;
	}

	/**
	 * Return array of the parent node's children as Object.<br>
	 *
	 * @see CallHierarchyNode#getChildren()
	 * @see ITreeContentProvider
	 * @param parentElement
	 *                The parent node as an Object which the method get the
	 *                children.
	 * @return Array of the parent node's children as Object.
	 */
	@Override
	public Object[] getChildren(final Object parentElement) {
		if (!(parentElement instanceof CallHierarchyNode)) {
			return new Object[] {};
		}

		final CallHierarchyNode parentNode = (CallHierarchyNode) parentElement;
		final CallHierarchyNode updatedParentNode = callHierarchy.functionCallFinder(parentNode);
		return updatedParentNode.getChildren();
	}

	/**
	 * Return array of the parent node's children as Object.<br>
	 * Use for the {@link CallHierarchyView}'s treeWiever.
	 *
	 * @see CallHierarchyNode#getChildren()
	 * @see ITreeContentProvider
	 * @param parentElement
	 * 			The parent node as an Object which the method get the children.
	 * @return
	 * 			Array of the parent node's children as Object.
	 */
	@Override
	public Object[] getElements(final Object inputElement) {
		if (!(inputElement instanceof CallHierarchyNode)) {
			return null;
		}
		return getChildren(inputElement);
	}

	/**
	 * Unused method in this implementation.
	 */
	@Override
	public Object getParent(final Object element) {
		return null;
	}

	/**
	 * Always return true, because the child searching run when the user get the children on the TreeViewer.
	 * @see #getChildren()
	 */
	@Override
	public boolean hasChildren(final Object element) {
		return true;
	}

	/**
	 * Unused method in this implementation.
	 */
	@Override
	public void dispose() {
		//Do nothing
	}

	/**
	 * Unused method in this implementation.
	 */
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		//Do nothing
	}
}