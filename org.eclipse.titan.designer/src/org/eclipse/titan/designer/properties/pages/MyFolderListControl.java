/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.pages;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Kristof Szabados
 * */
public class MyFolderListControl extends MyListControl {
	private final String basePath;

	public MyFolderListControl(final Composite parent, final String basePath, final String title, final String itemDescription) {
		super(parent, title, itemDescription);
		this.basePath = basePath;
	}

	@Override
	protected void addNewItem() {
		final FolderListItemDialog dialog = new FolderListItemDialog(list.getShell(), basePath, "Add new " + itemDescription, itemDescription, "");
		if (dialog.open() != Window.OK) {
			return;
		}

		final String newItem = dialog.getItem();

		if (newItem != null && newItem.length() > 0) {
			final int index = list.getSelectionIndex();
			if (index >= 0) {
				list.add(newItem, index + 1);
				list.setSelection(index + 1);
			} else {
				final int size = list.getItemCount();
				list.add(newItem, size);
				list.setSelection(size);
			}
		}

		selectionChanged();
	}

	@Override
	protected void editSelectedItem() {
		final int index = list.getSelectionIndex();

		if (index != -1) {
			final String item = list.getItem(index);

			final FolderListItemDialog dialog = new FolderListItemDialog(list.getShell(), basePath, "Edit " + itemDescription, itemDescription,
					item);
			if (dialog.open() != Window.OK) {
				return;
			}

			final String newItem = dialog.getItem();
			if (newItem != null && newItem.length() > 0 && !newItem.equals(item)) {
				list.setItem(index, newItem);
				selectionChanged();
			}
		}
	}
}
