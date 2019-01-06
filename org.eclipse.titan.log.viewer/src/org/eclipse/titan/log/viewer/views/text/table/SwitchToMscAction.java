/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.views.text.table;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.log.viewer.Activator;
import org.eclipse.titan.log.viewer.actions.OpenMSCViewAction;
import org.eclipse.titan.log.viewer.extractors.TestCaseExtractor;
import org.eclipse.titan.log.viewer.models.LogFileMetaData;
import org.eclipse.titan.log.viewer.parsers.data.TestCase;
import org.eclipse.titan.log.viewer.utils.Constants;
import org.eclipse.titan.log.viewer.utils.LogFileCacheHandler;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

class SwitchToMscAction extends Action {
	private final TextTableView textTableView;

	public SwitchToMscAction(final TextTableView textTableView) {
		super("", ImageDescriptor.createFromImage(Activator.getDefault().getIcon(Constants.ICONS_MSC_VIEW)));
		this.textTableView = textTableView;
		setId("switchToMSC");
		setToolTipText("Switch to MSC view");
	}

	@Override
	public void run() {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceRoot root = workspace.getRoot();
		final LogFileMetaData logFileMetaData = textTableView.getLogFileMetaData();
		final IProject project = root.getProject(logFileMetaData.getProjectName());
		final IFile logFile = project.getFile(logFileMetaData.getProjectRelativePath().substring(logFileMetaData.getProjectName().length() + 1));

		if (LogFileCacheHandler.hasLogFileChanged(logFile)) {
			LogFileCacheHandler.handleLogFileChange(logFile);
			return;
		}

		final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		final TestCaseExtractor extractor = new TestCaseExtractor();
		try {
			extractor.extractTestCasesFromIndexedLogFile(logFile);
		} catch (IOException e) {
			ErrorReporter.logExceptionStackTrace(e);
			final MessageBox mb = new MessageBox(activePage.getActivePart().getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
			mb.setText("Test case extraction failed.");
			mb.setMessage("Error while extracting the test cases.");
			return;
		} catch (ClassNotFoundException e) {
			ErrorReporter.logExceptionStackTrace(e);
			final MessageBox mb = new MessageBox(activePage.getActivePart().getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
			mb.setText("Test case extraction failed.");
			mb.setMessage("Error while extracting the test cases.");
			return;
		}

		if (textTableView.getSelectedRecord() == null) {
			final MessageBox mb = new MessageBox(activePage.getActivePart().getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
			mb.setText("Invalid selection.");
			mb.setMessage("Please select a record to open the MSC view.");
			return;
		}

		final int recordNumber = textTableView.getSelectedRecord().getRecordNumber();
		final List<TestCase> testCases = extractor.getTestCases();
		final int testCaseNumber = findContainingTestCase(testCases, recordNumber);

		if (testCaseNumber == -1) {
			final MessageBox mb = new MessageBox(activePage.getActivePart().getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
			mb.setText("Testcase can not be found.");
			mb.setMessage("The testcase containing the selected log record can not be found.");
			return;
		}

		final OpenMSCViewAction openMSCAction = new OpenMSCViewAction();
		openMSCAction.selectionChanged(null, new StructuredSelection(testCases.get(testCaseNumber)));
		openMSCAction.setFirstRow(recordNumber);
		openMSCAction.run();
	}

	private int findContainingTestCase(final List<TestCase> testCases, final int recordNumber) {
		int testCaseNumber = -1;
		for (int min = 0, max = testCases.size() - 1, mid = (min + max) / 2;
				min <= max;
				mid = (min + max) / 2) {

			if (recordNumber > testCases.get(mid).getEndRecordNumber()) {
				min = mid + 1;
			} else if (recordNumber < testCases.get(mid).getStartRecordNumber()) {
				max = mid - 1;
			} else {
				testCaseNumber = mid;
				break;
			}
		}
		return testCaseNumber;
	}
}
