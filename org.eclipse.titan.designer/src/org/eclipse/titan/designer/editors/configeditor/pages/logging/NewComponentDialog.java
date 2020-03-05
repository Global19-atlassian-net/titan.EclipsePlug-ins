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
 * @author Kristof Szabados
 * */
public class NewComponentDialog extends Dialog {
	static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[0-9a-zA-Z_]+");

	private final Set<String> namesTaken;
	private Text nameText;
	private Label errorLabel;
	private String name;

	private final ModifyListener modifyListener = new ModifyListener() {

		@Override
		public void modifyText(final ModifyEvent e) {
			validate();
		}
	};

	public NewComponentDialog(final Shell shell, final Set<String> namesTaken) {
		super(shell);
		setShellStyle(shell.getStyle() | SWT.RESIZE | SWT.PRIMARY_MODAL);
		this.namesTaken = namesTaken;
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create new component configuration.");
	}

	public String getName() {
		return name;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		errorLabel = new Label(container, SWT.NONE);
		errorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		errorLabel.setText("The name of the component must be a valid identifier or '*' for all valid components.");
		errorLabel.setVisible(false);

		final Composite nameContainer = new Composite(parent, SWT.NONE);
		nameContainer.setLayout(new GridLayout(2, false));
		nameContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		int i = 0;
		while (namesTaken.contains("new_component_" + i)) {
			i++;
		}
		name = "new_component_" + i;

		final Label label = new Label(nameContainer, SWT.NONE);
		label.setText("The name of the new component: ");
		nameText = new Text(nameContainer, SWT.SINGLE | SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		nameText.setText(name);
		nameText.addModifyListener(modifyListener);

		Dialog.applyDialogFont(container);

		return container;
	}

	private void validate() {
		final String temp = nameText.getText();
		if (namesTaken.contains(temp)) {
			getButton(OK).setEnabled(false);
			errorLabel.setText("A component with this name already exists.");
			errorLabel.setVisible(true);
			return;
		}

		if ("*".equals(temp) || IDENTIFIER_PATTERN.matcher(temp).matches()) {
			getButton(OK).setEnabled(true);
			errorLabel.setVisible(false);
			name = temp;
			return;
		}

		getButton(OK).setEnabled(false);
		errorLabel.setText("The name of the component must be a valid identifier or '*' for all valid components.");
		errorLabel.setVisible(true);
	}
}
