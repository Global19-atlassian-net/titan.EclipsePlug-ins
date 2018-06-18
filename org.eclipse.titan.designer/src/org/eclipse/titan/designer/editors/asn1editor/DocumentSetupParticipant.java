/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.asn1editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.titan.designer.commonFilters.ResourceExclusionHelper;
import org.eclipse.titan.designer.editors.DocumentTracker;
import org.eclipse.titan.designer.editors.EditorTracker;
import org.eclipse.titan.designer.editors.GlobalIntervalHandler;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectConfigurationParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * @author Kristof Szabados
 * */
public final class DocumentSetupParticipant implements IDocumentSetupParticipant {
	private final ASN1Editor editor;

	public DocumentSetupParticipant(final ASN1Editor editor) {
		this.editor = editor;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.core.filebuffers.IDocumentSetupParticipant#setup(org.
	 * eclipse.jface.text.IDocument)
	 */
	@Override
	public void setup(final IDocument document) {
		EditorTracker.remove(editor);
		EditorTracker.put((IFile) editor.getEditorInput().getAdapter(IFile.class), editor);
		DocumentTracker.put((IFile) editor.getEditorInput().getAdapter(IFile.class), document);

		IDocumentPartitioner partitioner = new FastPartitioner(new PartitionScanner(), PartitionScanner.PARTITION_TYPES);
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			extension3.setDocumentPartitioner(PartitionScanner.ASN1_PARTITIONING, partitioner);
		} else {
			document.setDocumentPartitioner(partitioner);
		}
		partitioner.connect(document);

		document.addDocumentListener(new IDocumentListener() {

			@Override
			public void documentAboutToBeChanged(final DocumentEvent event) {
				GlobalIntervalHandler.putInterval(event.getDocument(), null);
			}

			@Override
			public void documentChanged(final DocumentEvent event) {
				IPreferencesService prefs = Platform.getPreferencesService();
				if (prefs.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.USEONTHEFLYPARSING, true, null)) {
					analyze(document, false);
				}
			}

		});

		analyze(document, true);
	}

	void analyze(final IDocument document, final boolean isInitial) {
		final IFile editedFile = (IFile) editor.getEditorInput().getAdapter(IFile.class);
		if (editedFile == null || ResourceExclusionHelper.isExcluded(editedFile)) {
			return;
		}

		IProject project = editedFile.getProject();
		if (project == null) {
			return;
		}

		ProjectSourceParser projectSourceParser = GlobalParser.getProjectSourceParser(project);
		projectSourceParser.reportOutdating(editedFile);

		if (isInitial || !editor.isSemanticCheckingDelayed()) {
			projectSourceParser.analyzeAll();
		} else {
			projectSourceParser.reportSyntacticOutdatingOnly(editedFile);
			projectSourceParser.analyzeAllOnlySyntactically();
		}
	}

}
