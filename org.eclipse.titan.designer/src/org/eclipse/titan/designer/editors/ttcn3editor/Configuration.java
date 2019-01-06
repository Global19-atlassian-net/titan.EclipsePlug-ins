/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcn3editor;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.titan.designer.editors.AnnotationHover;
import org.eclipse.titan.designer.editors.BracketCompletionAutoEditStrategy;
import org.eclipse.titan.designer.editors.ClosingBracketIndentationAutoEditStrategy;
import org.eclipse.titan.designer.editors.ColorManager;
import org.eclipse.titan.designer.editors.ContentAssitant;
import org.eclipse.titan.designer.editors.GeneralTITANAutoEditStrategy;
import org.eclipse.titan.designer.editors.HeuristicalIntervalDetector;
import org.eclipse.titan.designer.editors.IntervallBasedDamagerRepairer;
import org.eclipse.titan.designer.editors.actions.IndentationSupport;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * @author Kristof Szabados
 * */
public final class Configuration extends TextSourceViewerConfiguration {
	private final ColorManager colorManager;
	private DoubleClickStrategy doubleClickStrategy;
	private final TTCN3Editor editor;
	private Reconciler reconciler;
	private PresentationReconciler presentationReconciler = null;

	public Configuration(final ColorManager colorManager, final TTCN3Editor editor) {
		this.colorManager = colorManager;
		this.editor = editor;
	}

	@Override
	public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
		return PartitionScanner.PARTITION_TYPES;
	}

	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(final ISourceViewer sourceViewer, final String contentType) {
		if (doubleClickStrategy == null) {
			doubleClickStrategy = new DoubleClickStrategy();
		}

		return doubleClickStrategy;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
		if (presentationReconciler == null) {
			presentationReconciler = new PresentationReconciler();
			presentationReconciler.setDocumentPartitioning(PartitionScanner.TTCN3_PARTITIONING);

			final IntervallBasedDamagerRepairer dr = new IntervallBasedDamagerRepairer(new CodeScanner(colorManager));
			presentationReconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
			presentationReconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		}

		return presentationReconciler;
	}

	@Override
	public IContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {
		final ContentAssitant assistant = new ContentAssitant();
		final IContentAssistProcessor pr = new ContentAssistProcessor(editor);

		assistant.setContentAssistProcessor(pr, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.setDocumentPartitioning(PartitionScanner.TTCN3_PARTITIONING);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		assistant.setProposalSelectorBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		return assistant;
	}

	@Override
	public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType, final int stateMask) {
		return new TextHover(sourceViewer, editor);
	}

	@Override
	public IAnnotationHover getAnnotationHover(final ISourceViewer sourceViewer) {
		return new AnnotationHover();
	}

	@Override
	public IReconciler getReconciler(final ISourceViewer sourceViewer) {
		if (reconciler == null) {
			final ReconcilingStrategy strategy = new ReconcilingStrategy(editor);
			reconciler = new Reconciler(strategy);
			editor.setReconciler(reconciler);
		}

		return reconciler;
	}

	/**
	 * Returns the auto indentation strategy ready to be used with the given
	 * source viewer when manipulating text of the given content type..
	 *
	 * @param sourceViewer
	 *                the source viewer to be configured by this
	 *                configuration
	 * @param contentType
	 *                the content type for which the strategy is applicable
	 * @return the auto indent strategy or <code>null</code> if automatic
	 *         indentation is not to be enabled
	 */
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(final ISourceViewer sourceViewer, final String contentType) {
		final HeuristicalIntervalDetector detector = new HeuristicalIntervalDetector();
		final GeneralTITANAutoEditStrategy strategy2 = new ClosingBracketIndentationAutoEditStrategy();
		strategy2.setHeuristicIntervalDetector(detector);
		final GeneralTITANAutoEditStrategy strategy3 = new SmartIndentAfterNewLineAutoEditStrategy();
		strategy3.setHeuristicIntervalDetector(detector);

		return new IAutoEditStrategy[] { new BracketCompletionAutoEditStrategy(), strategy2, strategy3 };
	}

	/**
	 * Returns the prefixes to be used by the line-shift operation.
	 *
	 * @param sourceViewer
	 *                the source viewer to be configured by this
	 *                configuration
	 * @param contentType
	 *                the content type for which the prefix is applicable
	 * @return the prefixes or <code>null</code> if the prefix operation
	 *         should not be supported
	 */
	@Override
	public String[] getIndentPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		return new String[] { IndentationSupport.getIndentString() };
	}

	/**
	 * Returns the default prefixes to be used by the line-prefix operation
	 * in the given source viewer for text of the given content type. This
	 * implementation always returns the prefix of the single line
	 * commenting ("//").
	 *
	 * @param sourceViewer
	 *                the source viewer to be configured by this
	 *                configuration
	 * @param contentType
	 *                the content type for which the prefix is applicable
	 * @return the prefix of the single line commenting ("//")
	 */
	@Override
	public String[] getDefaultPrefixes(final ISourceViewer sourceViewer, final String contentType) {
		return new String[] { "//" };
	}

	/**
	 * Returns the information control creator. The creator is a factory
	 * creating information controls for the given source viewer. This
	 * implementation always returns a creator for
	 * <code>JavaInformationControl</code> instances.
	 *
	 * @param sourceViewer
	 *                the source viewer to be configured by this
	 *                configuration
	 * @return the information control creator or <code>null</code> if no
	 *         information support should be installed
	 * @since 2.0
	 */
	@Override
	public IInformationControlCreator getInformationControlCreator(final ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			@Override
			public IInformationControl createInformationControl(final Shell parent) {
				// return new TextHoverControl(parent);
				return new DefaultInformationControl(parent, false);
			}
		};
	}
}
