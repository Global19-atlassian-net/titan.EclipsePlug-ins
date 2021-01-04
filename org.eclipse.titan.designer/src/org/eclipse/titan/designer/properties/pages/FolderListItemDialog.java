/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.pages;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.titan.common.fieldeditors.TITANResourceLocator;

/**
 * @author Kristof Szabados
 * */
public class FolderListItemDialog extends Dialog {

	private final String title;
	private final String itemKind;
	private String item;
	private final String basePath;
	private TITANResourceLocator itemText;

	public FolderListItemDialog(final Shell shell, final String basePath, final String title, final String itemKind, final String item) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.basePath = basePath;
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
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true, true));

		itemText = new TITANResourceLocator(itemKind + ":", container, IResource.FOLDER, basePath);
		itemText.setStringValue(item);

		return container;
	}

	@Override
	protected void okPressed() {
		item = itemText.getStringValue();
		super.okPressed();
	}
}
