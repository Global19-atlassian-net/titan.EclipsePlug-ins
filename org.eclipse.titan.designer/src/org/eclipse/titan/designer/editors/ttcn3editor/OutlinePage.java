/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcn3editor;

import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.AST.IOutlineElement;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Extfunction;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Template;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Var_Template;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Group;
import org.eclipse.titan.designer.AST.TTCN3.definitions.ImportModule;
import org.eclipse.titan.designer.editors.OutlineViewSorter;
import org.eclipse.titan.designer.graphics.ImageCache;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * @author Kristof Szabados
 * */
public final class OutlinePage extends ContentOutlinePage {
	private final AbstractTextEditor editor;
	private TreeViewer viewer;
	private ViewerFilter hideFunctionsFilter;
	private ViewerFilter hideTemplatesFilter;
	private ViewerFilter hideTypesFilter;
	private ViewerFilter groupFilter;
	private ViewerFilter underGroupFilter;
	private static final String ACTION_SORT = "Sort";
	private static final String USE_CATEGORIES = "Categorise";
	private static final String ACTION_TOGGLE_GROUP_MODE = "Toggle Group Mode";
	private static final String ACTION_HIDE_FUNCTIONS = "Hide Functions";
	private static final String ACTION_HIDE_TEMPLATES = "Hide Templates";
	private static final String ACTION_HIDE_TYPES = "Hide Types";

	public OutlinePage(final AbstractTextEditor editor) {
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
		comperator.setCategorizing(store.getBoolean(PreferenceConstants.OUTLINE_CATEGORISED));
		viewer.setComparator(comperator);

		final ViewerFilter filterGroup = getGroupFilter();
		final ViewerFilter filterUnderGroup = getUnderGroupFilter();
		if (store.getBoolean(PreferenceConstants.OUTLINE_GROUPED)) {
			viewer.removeFilter(filterGroup);
			viewer.addFilter(filterUnderGroup);
		} else {
			viewer.removeFilter(filterUnderGroup);
			viewer.addFilter(filterGroup);
		}
		ViewerFilter filter = getHideFunctionsFilter();
		if (store.getBoolean(PreferenceConstants.OUTLINE_HIDE_FUNCTIONS)) {
			viewer.addFilter(filter);
		} else {
			viewer.removeFilter(filter);
		}

		filter = getHideTemplatesFilter();
		if (store.getBoolean(PreferenceConstants.OUTLINE_HIDE_TEMPLATES)) {
			viewer.addFilter(filter);
		} else {
			viewer.removeFilter(filter);
		}

		filter = getHideTypesFilter();
		if (store.getBoolean(PreferenceConstants.OUTLINE_HIDE_TYPES)) {
			viewer.addFilter(filter);
		} else {
			viewer.removeFilter(filter);
		}

		viewer.setAutoExpandLevel(2);
		viewer.setInput(getModule());
		viewer.addSelectionChangedListener(this);

		final IActionBars bars = getSite().getActionBars();
		final Action sortToggler = new Action(ACTION_SORT) {
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

				viewer.refresh(false);
			}
		};
		sortToggler.setImageDescriptor(ImageCache.getImageDescriptor("sort_alphabetically.gif"));
		sortToggler.setChecked(store.getBoolean(PreferenceConstants.OUTLINE_SORTED));
		bars.getToolBarManager().add(sortToggler);

		final Action categorise = new Action(USE_CATEGORIES) {
			@Override
			public void run() {
				ViewerComparator comperator = viewer.getComparator();

				if (comperator == null) {
					comperator = new OutlineViewSorter();
					viewer.setComparator(comperator);
				}

				if (comperator instanceof OutlineViewSorter) {
					store.setValue(PreferenceConstants.OUTLINE_CATEGORISED, isChecked());
					((OutlineViewSorter) comperator).setCategorizing(isChecked());
				}

				viewer.refresh(false);
			}
		};
		categorise.setImageDescriptor(ImageCache.getImageDescriptor("categorize.gif"));
		categorise.setChecked(store.getBoolean(PreferenceConstants.OUTLINE_CATEGORISED));
		bars.getToolBarManager().add(categorise);

		final Action toggleGroupMode = new Action(ACTION_TOGGLE_GROUP_MODE) {
			@Override
			public void run() {
				ViewerFilter filterGroup = getGroupFilter();
				ViewerFilter filterUnderGroup = getUnderGroupFilter();
				store.setValue(PreferenceConstants.OUTLINE_GROUPED, isChecked());
				if (isChecked()) {
					viewer.removeFilter(filterGroup);
					viewer.addFilter(filterUnderGroup);
				} else {
					viewer.removeFilter(filterUnderGroup);
					viewer.addFilter(filterGroup);
				}
				viewer.refresh(false);
			}
		};
		toggleGroupMode.setImageDescriptor(ImageCache.getImageDescriptor("outline_group.gif"));
		toggleGroupMode.setChecked(store.getBoolean(PreferenceConstants.OUTLINE_GROUPED));
		bars.getToolBarManager().add(toggleGroupMode);

		final Action hideFunctions = new Action(ACTION_HIDE_FUNCTIONS) {
			@Override
			public void run() {
				ViewerFilter filter = getHideFunctionsFilter();
				store.setValue(PreferenceConstants.OUTLINE_HIDE_FUNCTIONS, isChecked());
				if (isChecked()) {
					viewer.addFilter(filter);
				} else {
					viewer.removeFilter(filter);
				}
				viewer.refresh(false);
			}
		};
		hideFunctions.setImageDescriptor(ImageCache.getImageDescriptor("filter_functions.gif"));
		hideFunctions.setChecked(store.getBoolean(PreferenceConstants.OUTLINE_HIDE_FUNCTIONS));
		bars.getToolBarManager().add(hideFunctions);

		final Action hideTemplates = new Action(ACTION_HIDE_TEMPLATES) {
			@Override
			public void run() {
				ViewerFilter filter = getHideTemplatesFilter();
				store.setValue(PreferenceConstants.OUTLINE_HIDE_TEMPLATES, isChecked());
				if (isChecked()) {
					viewer.addFilter(filter);
				} else {
					viewer.removeFilter(filter);
				}
				viewer.refresh(false);
			}
		};
		hideTemplates.setImageDescriptor(ImageCache.getImageDescriptor("filter_templates.gif"));
		hideTemplates.setChecked(store.getBoolean(PreferenceConstants.OUTLINE_HIDE_TEMPLATES));
		bars.getToolBarManager().add(hideTemplates);

		final Action hideTypes = new Action(ACTION_HIDE_TYPES) {
			@Override
			public void run() {
				ViewerFilter filter = getHideTypesFilter();
				store.setValue(PreferenceConstants.OUTLINE_HIDE_TYPES, isChecked());
				if (isChecked()) {
					viewer.addFilter(filter);
				} else {
					viewer.removeFilter(filter);
				}
				viewer.refresh(false);
			}
		};
		hideTypes.setImageDescriptor(ImageCache.getImageDescriptor("filter_types.gif"));
		hideTypes.setChecked(store.getBoolean(PreferenceConstants.OUTLINE_HIDE_TYPES));
		bars.getToolBarManager().add(hideTypes);

	}

	private ViewerFilter getGroupFilter() {
		if (groupFilter == null) {
			groupFilter = new ViewerFilter() {
				@Override
				public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
					if (element instanceof Group) {
						return false;
					}
					return true;
				}
			};
		}
		return groupFilter;
	}

	private ViewerFilter getUnderGroupFilter() {
		if (underGroupFilter == null) {
			underGroupFilter = new ViewerFilter() {
				@Override
				public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
					if (element instanceof Definition && ((Definition) element).getParentGroup() != null && !(parentElement instanceof Group)) {
						return false;
					}
					if (element instanceof ImportModule && ((ImportModule) element).getParentGroup() != null
							&& !(parentElement instanceof Vector<?>)) {
						return false;
					}
					return true;
				}
			};
		}
		return underGroupFilter;
	}

	private ViewerFilter getHideFunctionsFilter() {
		if (hideFunctionsFilter == null) {
			hideFunctionsFilter = new ViewerFilter() {
				@Override
				public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
					if (element instanceof Def_Function || element instanceof Def_Extfunction) {
						return false;
					}
					return true;
				}
			};
		}
		return hideFunctionsFilter;
	}

	private ViewerFilter getHideTemplatesFilter() {
		if (hideTemplatesFilter == null) {
			hideTemplatesFilter = new ViewerFilter() {
				@Override
				public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
					if (element instanceof Def_Template || element instanceof Def_Var_Template) {
						return false;
					}
					return true;
				}
			};
		}
		return hideTemplatesFilter;
	}

	private ViewerFilter getHideTypesFilter() {
		if (hideTypesFilter == null) {
			hideTypesFilter = new ViewerFilter() {
				@Override
				public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
					if (element instanceof Def_Type) {
						return false;
					}
					return true;
				}
			};
		}
		return hideTypesFilter;
	}

	public void update() {
		final Control control = getControl();
		if (control != null && !control.isDisposed()) {
			control.setRedraw(false);
			getTreeViewer().setInput(getModule());
			control.setRedraw(true);
		}
	}

	public void refresh() {
		final Control control = getControl();
		if (control == null || control.isDisposed()) {
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
