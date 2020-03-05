/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.tabpages.testset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;

/**
 * @author Kristof Szabados
 * */
public final class TestsetTreeDragSourceListener implements DragSourceListener {
	private final TreeViewer testsetViewer;

	public TestsetTreeDragSourceListener(final TreeViewer testsetViewer) {
		this.testsetViewer = testsetViewer;
	}

	@Override
	public void dragFinished(final DragSourceEvent event) {
		final IStructuredSelection selection = (IStructuredSelection) testsetViewer.getSelection();
		if (!selection.isEmpty()) {
			testsetViewer.getTree().setRedraw(false);
			if (event.detail == DND.DROP_MOVE) {
				for (final Iterator<?> it = selection.iterator(); it.hasNext();) {
					final Object element = it.next();
					if (element instanceof TestCaseTreeElement) {
						((TestsetTreeElement) ((TestCaseTreeElement) element).parent()).remove((TestCaseTreeElement) element);
						((TestCaseTreeElement) element).dispose();
					}
				}
			}
			testsetViewer.getTree().setRedraw(true);
			testsetViewer.refresh();
		}
	}

	@Override
	public void dragSetData(final DragSourceEvent event) {
		if (TestcaseTransfer.getInstance().isSupportedType(event.dataType)) {
			final IStructuredSelection selection = (IStructuredSelection) testsetViewer.getSelection();
			final List<TestCaseTreeElement> testcases = new ArrayList<TestCaseTreeElement>();
			if (!selection.isEmpty()) {
				for (final Iterator<?> it = selection.iterator(); it.hasNext();) {
					final Object element = it.next();
					if (element instanceof TestCaseTreeElement) {
						testcases.add((TestCaseTreeElement) element);
					}
				}
				event.data = testcases.toArray(new TestCaseTreeElement[testcases.size()]);
			}
		}
	}

	@Override
	public void dragStart(final DragSourceEvent event) {
		final IStructuredSelection selection = (IStructuredSelection) testsetViewer.getSelection();
		event.doit = !selection.isEmpty() && !(selection.getFirstElement() instanceof TestsetTreeElement);
	}
}
