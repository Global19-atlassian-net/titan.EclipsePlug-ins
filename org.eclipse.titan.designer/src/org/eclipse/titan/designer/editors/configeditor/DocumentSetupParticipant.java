/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.titan.designer.editors.GlobalIntervalHandler;

/**
 * @author Kristof Szabados
 * */
public final class DocumentSetupParticipant implements IDocumentSetupParticipant {

	@Override
	public void setup(final IDocument document) {
		final IDocumentPartitioner partitioner = new FastPartitioner(new PartitionScanner(), PartitionScanner.PARTITION_TYPES);
		if (document instanceof IDocumentExtension3) {
			final IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			extension3.setDocumentPartitioner(PartitionScanner.CONFIG_PARTITIONING, partitioner);
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
				//Do nothing
			}

		});
	}
}
