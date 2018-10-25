/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.pages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This dialog is used by the MyListControl class to add new or edit existing
 * elements.
 * 
 * @author Kristof Szabados
 * */
public final class ListItemDialog extends Dialog {

	private final String title;
	private final String itemKind;
	private String item;
	private Text itemText;

	public ListItemDialog(final Shell shell, final String title, final String itemKind, final String item) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.title = title;
		this.itemKind = itemKind;
		this.item = item;
	}

	public String getItem() {
		return item;
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		if (title != null) {
			newShell.setText(title);
		}
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setText(itemKind + ":");
		nameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		itemText = new Text(container, SWT.SINGLE | SWT.BORDER);
		itemText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		itemText.setText(item);

		return container;
	}

	@Override
	protected void okPressed() {
		item = itemText.getText();
		super.okPressed();
	}
}
