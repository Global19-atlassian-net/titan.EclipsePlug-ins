/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.runsonscopereduction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3Editor;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titanium.refactoring.Utils;

/**
 * 
 * @author Farkas Izabella Ingrid
 *
 */
public class RunsOnScopeReductionFromEditor extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		//update AST
		Utils.updateASTForProjectActiveInEditor("RunsOnScopeReduction");
		Activator.getDefault().pauseHandlingResourceChanges();

		// getting the active editor
		final TTCN3Editor targetEditor = Utils.getActiveEditor();
		if (targetEditor == null) {
			return null;
		}

		//getting selected file
		final IFile selectedFile = Utils.getSelectedFileInEditor("RunsOnScopeReduction");
		if (selectedFile == null) {
			return null;
		}
		
		final IStructuredSelection structSelection = new StructuredSelection(selectedFile);
		final RunsOnScopeReductionRefactoring refactoring = new RunsOnScopeReductionRefactoring(structSelection);
		
		//open wizard
		final RunsOnScopeReductionWizard wiz = new RunsOnScopeReductionWizard(refactoring);
		final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wiz);

		try {
			operation.run(targetEditor.getEditorSite().getShell(), "");
		} catch (InterruptedException irex) {
			// operation was cancelled
		} catch (Exception e) {
			ErrorReporter.logError("RunsOnScopeReductionFromEditor: Error while performing refactoring change! ");
			ErrorReporter.logExceptionStackTrace(e);
		}

		//update AST again
		Activator.getDefault().resumeHandlingResourceChanges();

		final IProject project = selectedFile.getProject();
		GlobalParser.getProjectSourceParser(project).reportOutdating(selectedFile);
		GlobalParser.getProjectSourceParser(project).analyzeAll();

		return null;
	}

}
