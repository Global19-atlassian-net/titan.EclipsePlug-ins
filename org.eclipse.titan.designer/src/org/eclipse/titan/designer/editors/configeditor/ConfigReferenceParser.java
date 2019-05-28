/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.editors.IReferenceParser;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReferenceAnalyzer;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * @author Ferenc Kovacs
 * */
public final class ConfigReferenceParser implements IReferenceParser {
	private boolean reportErrors;
	private boolean moduleParameter;
	private String exactModuleName;
	private String defineName;

	public ConfigReferenceParser(final boolean reportErrors) {
		this.reportErrors = reportErrors;
		this.moduleParameter = true;
		exactModuleName = null;
		defineName = null;
	}

	@Override
	public void setErrorReporting(final boolean reportErrors) {
		this.reportErrors = reportErrors;
	}

	@Override
	public Reference findReferenceForCompletion(final IFile file, final int offset, final IDocument document) {
		return null;
	}

	public String findIncludedFileForOpening(final int offset, final IDocument document) {
		moduleParameter = false;
		final String includeString = null;
		int ofs = offset - 1;
		int endoffset = offset;
		if (-1 == offset) {
			return includeString;
		}
		try {
			final int tempOfs = includeStartOffset(ofs, document);
			if (-1 == tempOfs) {
				return includeString;
			}
			ofs = tempOfs + 1;
			char currentChar = document.getChar(endoffset);
			while (endoffset < document.getLength()) {
				if (currentChar == '"') {
					break;
				}
				currentChar = document.getChar(++endoffset);
			}
			return document.get(ofs, endoffset - ofs);
		} catch (BadLocationException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		return includeString;
	}

	@Override
	public Reference findReferenceForOpening(final IFile file, final int offset, final IDocument document) {
		Reference reference = null;
		int ofs = offset - 1;
		int endoffset = offset;
		if (-1 == offset) {
			return reference;
		}
		try {
			final int tempOfs = referenceStartOffset(ofs, document);
			if (-1 == tempOfs) {
				return reference;
			}
			ofs = tempOfs + 1;
			char currentChar = document.getChar(endoffset);
			while (endoffset < document.getLength()) {
				if (!Character.isLetterOrDigit(currentChar) && currentChar != '*' && currentChar != '_' && currentChar != '.') {
					break;
				}
				currentChar = document.getChar(++endoffset);
			}
			if (!moduleParameter) {
				defineName = document.get(ofs, endoffset - ofs);
				return null;
			}
			final String selected = document.get(ofs, endoffset - ofs);
			String parameter = selected;
			final int dotIndex = selected.indexOf('.');
			if (dotIndex > 0) {
				final String moduleName = selected.substring(0, dotIndex);
				if (!"*".equals(moduleName)) {
					exactModuleName = moduleName;

					final IPreferencesService prefs = Platform.getPreferencesService();
					if (prefs.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.DISPLAYDEBUGINFORMATION, true,
							null)) {
						TITANDebugConsole.println("Module: " + exactModuleName);

					}
				}
				parameter = selected.substring(dotIndex + 1);
			}
			final TTCN3ReferenceAnalyzer refAnalyzer = new TTCN3ReferenceAnalyzer();
			reference = refAnalyzer.parse(file, parameter, reportErrors, document.getLineOfOffset(ofs) + 1, ofs);
		} catch (BadLocationException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		return reference;
	}

	private int referenceStartOffset(final int offset, final IDocument document) throws BadLocationException {
		int ofs = offset;
		char currentChar = document.getChar(ofs);
		while (ofs > 0) {
			if (!Character.isLetterOrDigit(currentChar) && currentChar != '*' && currentChar != '_' && currentChar != '.'
					&& currentChar != '$' && currentChar != '{') {
				break;
			}

			if (currentChar == '$' || currentChar == '{') {
				moduleParameter = false;
				break;
			}
			currentChar = document.getChar(--ofs);
		}
		return ofs;
	}

	private int includeStartOffset(final int offset, final IDocument document) throws BadLocationException {
		int ofs = offset;
		char currentChar = document.getChar(ofs);
		while (ofs > 0) {
			if (currentChar == '"') {
				break;
			}
			currentChar = document.getChar(--ofs);
		}
		return ofs;
	}

	public String getExactModuleName() {
		return exactModuleName;
	}

	public String getDefinitionName() {
		return defineName;
	}

	public boolean isModuleParameter() {
		return moduleParameter;
	}
}
