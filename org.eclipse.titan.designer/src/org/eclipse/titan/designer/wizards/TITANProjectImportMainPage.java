/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.path.PathConverter;

/**
 * @author Kristof Szabados
 * */
public class TITANProjectImportMainPage extends WizardPage {
	private TITANProjectImportPage newProjectPage;

	private Composite pageComposite;
	private Text projectFileText;
	private Button projectFileSelectionButton;

	private String projectFile;
	private GUIProjectImporter.ProjectInformation projectInformation = null;

	protected class BasicProjectSelectorListener implements ModifyListener, SelectionListener {

		@Override
		public void modifyText(final ModifyEvent e) {
			final Object source = e.getSource();
			if (source == projectFileText) {
				handleProjectFileModified();
			}
		}

		@Override
		public void widgetDefaultSelected(final SelectionEvent e) {
			//Do nothing
		}

		@Override
		public void widgetSelected(final SelectionEvent e) {
			final Object source = e.getSource();
			if (source == projectFileSelectionButton) {
				handleProjectFileButtonSelected();
			}
		}
	}

	private final BasicProjectSelectorListener generalListener = new BasicProjectSelectorListener();

	public TITANProjectImportMainPage(final String name) {
		super(name);
	}

	public GUIProjectImporter.ProjectInformation getInformation() {
		return projectInformation;
	}

	public void setNewProjectPage(final TITANProjectImportPage newProjectPage) {
		this.newProjectPage = newProjectPage;
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
		group.setText("Original project file:");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setFont(font);
		projectFileText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		projectFileText.setLayoutData(gd);
		projectFileText.setFont(font);
		projectFileText.addModifyListener(generalListener);
		projectFileSelectionButton = new Button(group, SWT.PUSH);
		projectFileSelectionButton.setText("Browse..");
		projectFileSelectionButton.setLayoutData(new GridData());
		projectFileSelectionButton.addSelectionListener(generalListener);
	}

	protected void handleProjectFileButtonSelected() {
		final FileDialog dialog = new FileDialog(getShell());
		dialog.setFilterPath(projectFileText.getText());
		dialog.setFilterExtensions(new String[] { "*.prj" });
		final String file = dialog.open();
		if (file != null && !file.equals(projectFileText.getText())) {
			projectFileText.setText(file);
		}
	}

	protected void handleProjectFileModified() {
		projectFile = projectFileText.getText();
		try {
			new ProgressMonitorDialog(null).run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					final SubMonitor progress = SubMonitor.convert(monitor, 1);
					progress.setTaskName("Importing the data of the project");

					checkProjectFile(progress.newChild(1));
				}
			});
		} catch (Exception e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
	}

	/**
	 * Check the validity of the file set as project file. And also extracts
	 * all data from the file if it is a correct project file.
	 * 
	 * @param monitor
	 *                the monitor used to report progress.
	 * */
	private void checkProjectFile(final SubMonitor monitor) {
		final GUIProjectImporter importer = new GUIProjectImporter();
		projectInformation = importer.loadProjectFile(projectFile, monitor, false); //false: not headless

		if (newProjectPage != null && projectInformation != null) {
			newProjectPage.setInitialProjectName(projectInformation.getName());

			final String absolutePath = PathConverter.getAbsolutePath(projectInformation.getSourceFile(), projectInformation.getWorkingDir());
			newProjectPage.setWorkingDirectory(absolutePath);
		}

	}

}
