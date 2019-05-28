/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcn3editor;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.titan.designer.editors.BaseTextHover;
import org.eclipse.titan.designer.editors.IReferenceParser;
import org.eclipse.ui.IEditorPart;

/**
 * @author Kristof Szabados
 * */
public final class TextHover extends BaseTextHover {
	private final ISourceViewer sourceViewer;
	private final IEditorPart editor;

	public TextHover(final ISourceViewer sourceViewer, final IEditorPart editor) {
		this.sourceViewer = sourceViewer;
		this.editor = editor;
	}

	@Override
	protected ISourceViewer getSourceViewer() {
		return sourceViewer;
	}

	@Override
	protected IEditorPart getTargetEditor() {
		return editor;
	}

	@Override
	protected IReferenceParser getReferenceParser() {
		return new TTCN3ReferenceParser(false);
	}
}
