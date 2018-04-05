/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcnppeditor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.titan.designer.editors.ttcnppeditor.PairMatcher;
import org.eclipse.titan.designer.editors.ttcnppeditor.TTCNPPEditor;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * @author Kristof Szabados
 * */
public final class GotoMatchingBracketAction extends AbstractHandler implements IEditorActionDelegate {
	private TTCNPPEditor targetEditor = null;
	private ISelection selection = TextSelection.emptySelection();

	@Override
	public void run(final IAction action) {
		if (targetEditor == null) {
			return;
		}

		if (!selection.isEmpty()) {
			if (selection instanceof TextSelection) {
				TextSelection tSelection = (TextSelection) selection;
				if (tSelection.getLength() != 0) {
					return;
				}
			}
		}

		IDocument document = targetEditor.getDocument();
		int carretOffset = targetEditor.getCarretOffset();
		PairMatcher pairMatcher = new PairMatcher();

		IRegion region = pairMatcher.match(document, carretOffset);
		if (region == null) {
			return;
		}

		int targetOffset;
		if (region.getOffset() + 1 == carretOffset) {
			targetOffset = region.getOffset() + region.getLength();
		} else {
			targetOffset = region.getOffset() + 1;
		}

		targetEditor.setCarretOffset(targetOffset);
		targetEditor.selectAndReveal(targetOffset, 0);
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		if (targetEditor instanceof TTCNPPEditor) {
			this.targetEditor = (TTCNPPEditor) targetEditor;
		} else {
			this.targetEditor = null;
		}
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (activeEditor instanceof TTCNPPEditor) {
			this.targetEditor = (TTCNPPEditor) activeEditor;
		} else {
			this.targetEditor = null;
		}

		if (activeEditor == null) {
			return null;
		}

		if (!selection.isEmpty()) {
			if (selection instanceof TextSelection) {
				TextSelection tSelection = (TextSelection) selection;
				if (tSelection.getLength() != 0) {
					return null;
				}
			}
		}

		IDocument document = this.targetEditor.getDocument();
		int carretOffset = this.targetEditor.getCarretOffset();
		PairMatcher pairMatcher = new PairMatcher();

		IRegion region = pairMatcher.match(document, carretOffset);
		if (region == null) {
			return null;
		}

		int targetOffset;
		if (region.getOffset() + 1 == carretOffset) {
			targetOffset = region.getOffset() + region.getLength();
		} else {
			targetOffset = region.getOffset() + 1;
		}

		this.targetEditor.setCarretOffset(targetOffset);
		this.targetEditor.selectAndReveal(targetOffset, 0);
		return null;
	}
}
