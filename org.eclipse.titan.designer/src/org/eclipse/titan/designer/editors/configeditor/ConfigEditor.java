/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.parsers.cfg.CfgAnalyzer;
import org.eclipse.titan.common.parsers.cfg.CfgParseTreePrinter;
import org.eclipse.titan.designer.editors.configeditor.pages.compgroupmc.ComponentsGroupsMCPage;
import org.eclipse.titan.designer.editors.configeditor.pages.execute.ExecuteExternalcommandsPage;
import org.eclipse.titan.designer.editors.configeditor.pages.include.IncludeDefinePage;
import org.eclipse.titan.designer.editors.configeditor.pages.logging.LoggingPage;
import org.eclipse.titan.designer.editors.configeditor.pages.modulepar.ModuleParameterSectionPage;
import org.eclipse.titan.designer.editors.configeditor.pages.testportpar.TestportParametersSectionPage;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

/**
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class ConfigEditor extends FormEditor implements IResourceChangeListener {

	private static final boolean CONFIG_EDITOR_TABS_VISIBLE = true;

	private final ConfigTextEditor editor;

	private int editorPageIndex;
	private boolean isDirty = false;
	private String errorMessage;

	private ModuleParameterSectionPage mModuleParameterSectionEditor;
	private TestportParametersSectionPage mTestportParameterSectionEditor;
	private ComponentsGroupsMCPage mComponentGroupMCSectionEditor;
	private ExecuteExternalcommandsPage mExecuteExternalCommandsEditor;
	private IncludeDefinePage mIncludeDefineEditor;
	private LoggingPage mLoggingEditor;

	private ParserRuleContext mParseTreeRoot;
	private List<Token> mTokens;

	public ConfigEditor() {
		editor = new ConfigTextEditor(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	protected FormToolkit createToolkit(final Display display) {
		return new FormToolkit(FormColorCache.getFormColors(display));
	}

	void createTextEditorPage() {
		try {
			editorPageIndex = addPage(editor, getEditorInput());
			setPageText(editorPageIndex, editor.getTitle());
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
		}
	}

	public void setDirty() {
		isDirty = true;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public boolean isDirty() {
		return isDirty || editor.isDirty();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	public void gotoMarker(final IMarker marker) {
		setActivePage(editorPageIndex);
		IDE.gotoMarker(getEditor(editorPageIndex), marker);
	}

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			final IEditorInput input = editor.getEditorInput();
			if (input instanceof FileEditorInput && ((FileEditorInput) input).getFile().getProject().equals(event.getResource())) {
				final IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
				if (workbenchWindow == null) {
					return;
				}
				final IWorkbenchPage[] pages = workbenchWindow.getPages();

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < pages.length; i++) {
							final IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart, true);
						}
					}
				});
			}
		}
	}

	public ConfigTextEditor getEditor() {
		return editor;
	}

	public void refresh(final CfgAnalyzer cfgAnalyzer) {
		if ( !CONFIG_EDITOR_TABS_VISIBLE ) {
			return;
		}
		final Shell shell = getSite().getShell();
		if (shell == null) {
			return;
		}
		shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				mModuleParameterSectionEditor.refreshData( cfgAnalyzer.getModuleParametersHandler() );
				mTestportParameterSectionEditor.refreshData( cfgAnalyzer.getTestportParametersHandler() );
				mComponentGroupMCSectionEditor.refreshData( cfgAnalyzer.getComponentSectionHandler(),
						cfgAnalyzer.getGroupSectionHandler(),
						cfgAnalyzer.getMcSectionHandler() );
				mExecuteExternalCommandsEditor.refreshData( cfgAnalyzer.getExternalCommandsSectionHandler(),
						cfgAnalyzer.getExecuteSectionHandler() );
				mIncludeDefineEditor.refreshData( cfgAnalyzer.getIncludeSectionHandler(), cfgAnalyzer.getDefineSectionHandler() );
				mLoggingEditor.refreshData( cfgAnalyzer.getLoggingSectionHandler() );
			}
		});
	}

	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;

		if ( !CONFIG_EDITOR_TABS_VISIBLE ) {
			return;
		}
		final Shell shell = getSite().getShell();
		if (shell == null) {
			return;
		}
		shell.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				mModuleParameterSectionEditor.setErrorMessage();
				mTestportParameterSectionEditor.setErrorMessage();
				mComponentGroupMCSectionEditor.setErrorMessage();
				mExecuteExternalCommandsEditor.setErrorMessage();
				mIncludeDefineEditor.setErrorMessage();
				mLoggingEditor.setErrorMessage();
			}
		});
	}

	@Override
	protected void addPages() {
		if ( CONFIG_EDITOR_TABS_VISIBLE ) {
			try {
				createTextEditorPage();
				mModuleParameterSectionEditor = new ModuleParameterSectionPage(this);
				addPage(mModuleParameterSectionEditor);
				mTestportParameterSectionEditor = new TestportParametersSectionPage(this);
				addPage(mTestportParameterSectionEditor);
				mComponentGroupMCSectionEditor = new ComponentsGroupsMCPage(this);
				addPage(mComponentGroupMCSectionEditor);
				mExecuteExternalCommandsEditor = new ExecuteExternalcommandsPage(this);
				addPage(mExecuteExternalCommandsEditor);
				mIncludeDefineEditor = new IncludeDefinePage(this);
				addPage(mIncludeDefineEditor);
				mLoggingEditor = new LoggingPage(this);
				addPage(mLoggingEditor);
			} catch (PartInitException e) {
				ErrorReporter.logExceptionStackTrace(e);
			}
		} else {
			createTextEditorPage();
		}

		setPartName(getEditorInput().getName());
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
		if (isDirty) {
			updateTextualPage();
		}

		editor.doSave(monitor);
		isDirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {
		if (isDirty) {
			updateTextualPage();
		}

		editor.doSaveAs();
		setPageText(editorPageIndex, editor.getTitle());
		setInput(editor.getEditorInput());

		isDirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	public ParserRuleContext getParseTreeRoot() {
		return mParseTreeRoot;
	}

	public void setParseTreeRoot(final ParserRuleContext aParseTreeRoot) {
		mParseTreeRoot = aParseTreeRoot;
	}

	public List<Token> getTokens() {
		return mTokens;
	}

	public void setTokens(final List<Token> aTokens) {
		mTokens = aTokens;
	}

	private void updateTextualPage() {
		if (mParseTreeRoot != null && mTokens != null) {
			final String original = editor.getDocument().get();
			final String content = CfgParseTreePrinter.toStringWithHidden( mParseTreeRoot, mTokens, true );

			if (!content.equals(original)) {
				editor.getDocument().set(content);
			}
		}
	}

	@Override
	protected void pageChange(final int newPageIndex) {
		if (newPageIndex == editorPageIndex) {
			updateTextualPage();
		}

		super.pageChange(newPageIndex);
	}

}
