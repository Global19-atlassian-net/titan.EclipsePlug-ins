/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.asn1editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.titan.designer.editors.asn1editor.ASN1Editor;
import org.eclipse.titan.designer.refactoring.RenameRefactoring;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * @author Adam Delic
 * */
public final class RenameRefactoringAction extends AbstractHandler implements IEditorActionDelegate {
	private IEditorPart targetEditor = null;
	private ISelection selection = TextSelection.emptySelection();

	@Override
	public void run(final IAction action) {
		if (targetEditor == null || !(targetEditor instanceof ASN1Editor)) {
			return;
		}
		RenameRefactoring.runAction(targetEditor, selection);
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		targetEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (targetEditor == null || !(targetEditor instanceof ASN1Editor)) {
			return null;
		}
		RenameRefactoring.runAction(targetEditor, selection);

		return null;
	}

}
