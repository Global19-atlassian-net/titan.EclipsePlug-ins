/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.parsers.asn1parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.parsers.Interval;
import org.eclipse.titan.common.parsers.SyntacticErrorStorage;
import org.eclipse.titan.common.parsers.TITANMarker;
import org.eclipse.titan.designer.AST.ASN1.definitions.ASN1Module;
import org.eclipse.titan.designer.parsers.ISourceAnalyzer;

/**
 * ASN1 Analyzer
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public class ASN1Analyzer implements ISourceAnalyzer {

	private List<TITANMarker> warnings;
	private List<TITANMarker> unsupportedConstructs;
	private Interval rootInterval;
	private ASN1Module actualAsn1Module = null;
	private ASN1Listener lexerListener = null;
	private ASN1Listener parserListener = null;

	@Override
	public List<TITANMarker> getWarnings() {
		return warnings;
	}

	@Override
	public List<TITANMarker> getUnsupportedConstructs() {
		return unsupportedConstructs;
	}

	@Override
	public ASN1Module getModule() {
		return actualAsn1Module;
	}

	@Override
	public Interval getRootInterval() {
		return rootInterval;
	}

	@Override
	public List<SyntacticErrorStorage> getErrorStorage() {
		if (lexerListener == null || parserListener == null) {
			// the parser was not yet run for some reason.
			return new ArrayList<SyntacticErrorStorage>();
		}

		if (!lexerListener.getErrorsStored().isEmpty() && parserListener.getErrorsStored().isEmpty()) {
			return lexerListener.getErrorsStored();
		} else if (lexerListener.getErrorsStored().isEmpty() && !parserListener.getErrorsStored().isEmpty()) {
			return parserListener.getErrorsStored();
		} else if (!lexerListener.getErrorsStored().isEmpty() && !parserListener.getErrorsStored().isEmpty()) {
			if (lexerListener.addAll(parserListener.getErrorsStored())) {
				return lexerListener.getErrorsStored();
			}
		}
		return new ArrayList<SyntacticErrorStorage>();
	}

	@Override
	public void parse(final IFile file, final String code) throws FileNotFoundException {
		Reader reader = null;

		if (code != null) {
			reader = new StringReader(code);
		} else if (file != null) {
			try {
				reader = new BufferedReader(new InputStreamReader(file.getContents(), file.getCharset()));
			} catch (Exception e) {
				ErrorReporter.logExceptionStackTrace("Could not get the contents of `" + file.getName() + "'", e);
				return;
			}
		} else {
			return;
		}

		CharStream charStream;
		try {
			charStream = new ANTLRInputStream(reader);
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e2) {
			}
			return;
		}
		final Asn1Lexer lexer = new Asn1Lexer(charStream);
		lexer.setTokenFactory(new TokenWithIndexAndSubTokensFactory(true));
		lexer.setActualFile(file);
		lexerListener = new ASN1Listener();
		lexer.removeErrorListeners(); // remove ConsoleErrorListener
		lexer.addErrorListener(lexerListener);

		final ModuleLevelTokenStreamTracker tracker = new ModuleLevelTokenStreamTracker(lexer);
		tracker.discard(Asn1Lexer.WS);
		tracker.discard(Asn1Lexer.MULTILINECOMMENT);
		tracker.discard(Asn1Lexer.SINGLELINECOMMENT);
		tracker.setActualFile(file);
		final Asn1Parser parser = new Asn1Parser(tracker);
		parser.setProject(file.getProject());
		parser.setActualFile(file);
		parser.setBuildParseTree(false);
		parserListener = new ASN1Listener(parser);
		parser.removeErrorListeners(); // remove ConsoleErrorListener
		parser.addErrorListener(parserListener);
		parser.pr_ASN1ModuleDefinition();
		actualAsn1Module = parser.getModule();
		warnings = parser.getWarnings();
		unsupportedConstructs = parser.getUnsupportedConstructs();

		try {
			reader.close();
		} catch (IOException e) {
		}
	}
}
