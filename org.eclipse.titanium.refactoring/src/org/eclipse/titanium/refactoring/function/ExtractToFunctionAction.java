/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3Editor;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titanium.refactoring.Utils;

/**
 * This class handles the <code>ExtractToFunctionRefactoring</code> class.
 * <code>execute()</code> is called by the UI (see plugin.xml).
 *
 * @author Viktor Varga
 */
public class ExtractToFunctionAction extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		//update AST
		//TODO: force saving before refactoring (like in RenameRefactoring)
		Utils.updateASTForProjectActiveInEditor("ExtractToFunction");

		//get selected statements
		final ExtractToFunctionRefactoring refactoring = new ExtractToFunctionRefactoring();
		refactoring.findSelection();
		if (!refactoring.isSelectionValid()) {
			ErrorReporter.logError("ExtractToFunctionAction: Invalid selection! ");
			return null;
		}

		final IFile selectedFile = refactoring.getSelectedFile();
		final ProjectSourceParser projectSourceParser = GlobalParser.getProjectSourceParser(selectedFile.getProject());

		Activator.getDefault().pauseHandlingResourceChanges();

		//find params & create function from string builders
		try {
			final WorkspaceJob job1 = refactoring.createFunction();
			job1.join();
			if (!job1.getResult().isOK()) {
				return null;
			}
		} catch (InterruptedException ie) {
			ErrorReporter.logExceptionStackTrace(ie);
		}
		//getting active editor
		final TTCN3Editor targetEditor = Utils.getActiveEditor();
		if (targetEditor == null) {
			return null;
		}
		//open wizard and modify function name, param names if necessary
		final ExtractToFunctionWizard wiz = new ExtractToFunctionWizard(refactoring);
		final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wiz);
		try {
			operation.run(targetEditor.getEditorSite().getShell(), "");
		} catch (InterruptedException irex) {
			// operation was cancelled
		} catch (Exception e) {
			ErrorReporter.logError("ExtractToFunctionAction: Error while performing refactoring change! ");
			ErrorReporter.logExceptionStackTrace(e);
		}

		//report outdating
		projectSourceParser.reportOutdating(selectedFile);
		projectSourceParser.analyzeAll();

		Activator.getDefault().resumeHandlingResourceChanges();
		return null;
	}



}
