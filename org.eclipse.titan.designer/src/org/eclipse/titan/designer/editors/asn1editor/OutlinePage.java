/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.asn1editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.AST.IOutlineElement;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.editors.OutlineViewSorter;
import org.eclipse.titan.designer.graphics.ImageCache;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * @author Kristof Szabados
 * */
public final class OutlinePage extends ContentOutlinePage {
	private final ASN1Editor editor;
	private TreeViewer viewer;

	public OutlinePage(final ASN1Editor editor) {
		this.editor = editor;
	}

	@Override
	public void createControl(final Composite parent) {
		super.createControl(parent);

		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		viewer = getTreeViewer();
		viewer.setContentProvider(new OutlineContentProvider());
		viewer.setLabelProvider(new OutlineLabelProvider());

		final OutlineViewSorter comperator = new OutlineViewSorter();
		comperator.setSortByName(store.getBoolean(PreferenceConstants.OUTLINE_SORTED));
		viewer.setComparator(comperator);

		viewer.setAutoExpandLevel(2);
		viewer.setInput(getModule());
		viewer.addSelectionChangedListener(this);

		final IActionBars bars = getSite().getActionBars();
		final Action sortToggler = new Action("Sort") {
			@Override
			public void run() {
				ViewerComparator comperator = viewer.getComparator();
				if (comperator == null) {
					comperator = new OutlineViewSorter();
					viewer.setComparator(comperator);
				}

				if (comperator instanceof OutlineViewSorter) {
					store.setValue(PreferenceConstants.OUTLINE_SORTED, isChecked());
					((OutlineViewSorter) comperator).setSortByName(isChecked());
				}

				viewer.refresh();
			}
		};
		sortToggler.setImageDescriptor(ImageCache.getImageDescriptor("sort_alphabetically.gif"));
		sortToggler.setChecked(store.getBoolean(PreferenceConstants.OUTLINE_SORTED));
		bars.getToolBarManager().add(sortToggler);
	}

	public void update() {
		final Control control = getControl();
		if (!control.isDisposed()) {
			control.setRedraw(false);
			getTreeViewer().setInput(getModule());
			control.setRedraw(true);
		}
	}

	public void refresh() {
		final Control control = getControl();
		if (control.isDisposed()) {
			return;
		}

		control.setRedraw(false);
		final Module module = getModule();
		if (getTreeViewer().getInput() == module) {
			getTreeViewer().refresh();
			getTreeViewer().expandToLevel(2);
		} else {
			getTreeViewer().setInput(getModule());
		}
		control.setRedraw(true);
	}

	@Override
	public void selectionChanged(final SelectionChangedEvent event) {
		super.selectionChanged(event);

		final ISelection selection = event.getSelection();
		if (selection.isEmpty()) {
			return;
		}

		final Object selectedElement = ((IStructuredSelection) selection).getFirstElement();
		Identifier identifier = null;
		if (selectedElement instanceof IOutlineElement) {
			identifier = ((IOutlineElement) selectedElement).getIdentifier();
		}

		if (identifier == null || identifier.getLocation() == null) {
			return;
		}

		final Location location = identifier.getLocation();

		editor.selectAndReveal(location.getOffset(), location.getEndOffset() - location.getOffset());
	}

	private Module getModule() {
		final IFile file = (IFile) editor.getEditorInput().getAdapter(IFile.class);

		if (file == null) {
			return null;
		}

		final ProjectSourceParser sourceParser = GlobalParser.getProjectSourceParser(file.getProject());
		final Module module = sourceParser.containedModule(file);
		if (module == null || module.getLastCompilationTimeStamp() == null) {
			return null;
		}

		return module;
	}
}
