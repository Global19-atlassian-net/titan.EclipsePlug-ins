/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * @author Kristof Szabados
 * */
public class TITANProjectImportRecursivelyPage extends WizardPage {
	private Composite pageComposite;
	private Button recursively;
	private boolean importRecursively = true;

	public TITANProjectImportRecursivelyPage(final String name) {
		super(name);
	}

	public boolean getRecursiveImport() {
		return importRecursively;
	}

	@Override
	public void createControl(final Composite parent) {
		pageComposite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		pageComposite.setLayout(layout);
		final GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		pageComposite.setLayoutData(data);

		createProjectFileEditor(pageComposite);

		setControl(pageComposite);
	}

	protected void createProjectFileEditor(final Composite parent) {
		final Font font = parent.getFont();
		final Group group = new Group(parent, SWT.NONE);
		group.setText("Included projects:");
		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setFont(font);
		recursively = new Button(group, SWT.CHECK);
		recursively.setText("Import included projects automatically");
		recursively.setLayoutData(new GridData());
		recursively.setSelection(true);
		recursively.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				importRecursively = recursively.getSelection();
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				importRecursively = recursively.getSelection();
			}
		});
	}
}
