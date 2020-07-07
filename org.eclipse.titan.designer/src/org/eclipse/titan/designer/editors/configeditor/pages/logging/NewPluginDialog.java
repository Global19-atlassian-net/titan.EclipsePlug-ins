/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.logging;

import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Adam Delic
 * */
public class NewPluginDialog extends Dialog {

	static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z][0-9A-Za-z_]*");
	static final Pattern PATH_PATTERN = Pattern.compile("[^\"]*");

	private final Set<String> namesTaken;
	private Text nameText;
	private Text pathText;
	private Label errorLabel;
	private String name;
	private String path;

	private final ModifyListener modifyListener = new ModifyListener() {

		@Override
		public void modifyText(final ModifyEvent e) {
			validate();
		}
	};

	public NewPluginDialog(final Shell parentShell, final Set<String> namesTaken) {
		super(parentShell);
		setShellStyle(parentShell.getStyle() | SWT.RESIZE | SWT.PRIMARY_MODAL);
		this.namesTaken = namesTaken;
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create new plugin configuration.");
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return (path.length() > 0) ? path : null;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		errorLabel = new Label(container, SWT.NONE);
		errorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		errorLabel.setText("The name of the plugin is not an identifier");
		errorLabel.setVisible(false);

		final Composite nameContainer = new Composite(parent, SWT.NONE);
		nameContainer.setLayout(new GridLayout(2, false));
		nameContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		int i = 0;
		while (namesTaken.contains("new_plugin_" + i)) {
			i++;
		}
		name = "new_plugin_" + i;
		Label label = new Label(nameContainer, SWT.NONE);
		label.setText("The name of the new plugin: ");
		nameText = new Text(nameContainer, SWT.SINGLE | SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		nameText.setText(name);
		nameText.addModifyListener(modifyListener);

		path = "";
		final Composite pathContainer = new Composite(parent, SWT.NONE);
		pathContainer.setLayout(new GridLayout(2, false));
		pathContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		label = new Label(pathContainer, SWT.NONE);
		label.setText("The path of the new plugin: ");
		pathText = new Text(pathContainer, SWT.SINGLE | SWT.BORDER);
		pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pathText.setText(path);
		pathText.addModifyListener(modifyListener);

		Dialog.applyDialogFont(container);

		return container;
	}

	private void validate() {
		final String nameTemp = nameText.getText();
		if (namesTaken.contains(nameTemp)) {
			getButton(OK).setEnabled(false);
			errorLabel.setText("A plugin with this name already exists.");
			errorLabel.setVisible(true);
			return;
		}
		if (!NAME_PATTERN.matcher(nameTemp).matches()) {
			getButton(OK).setEnabled(false);
			errorLabel.setText("The name of the plugin must be a valid identifier.");
			errorLabel.setVisible(true);
			return;
		}

		final String pathTemp = pathText.getText();
		if (!PATH_PATTERN.matcher(pathTemp).matches()) {
			getButton(OK).setEnabled(false);
			errorLabel.setText("The path of the plugin is not valid.");
			errorLabel.setVisible(true);
			return;
		}

		// success, set the variables
		getButton(OK).setEnabled(true);
		errorLabel.setVisible(false);
		name = nameTemp;
		path = pathTemp;
	}
}
