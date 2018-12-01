/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.parsers.ttcn3parser;

import java.io.Reader;
import java.io.StringReader;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.UnbufferedCharStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.parsers.ParserUtilities;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Parser.Pr_UnifiedReferenceParserContext;

/**
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class TTCN3ReferenceAnalyzer {

	public TTCN3ReferenceAnalyzer() {
	}

	/**
	 * @return the parsed reference or null if the text can not form a reference
	 */
	public Reference parse(final IFile file, final String code, final boolean reportErrors, final int aLine, final int aOffset) {
		final Reader reader = new StringReader( code );
		final CharStream charStream = new UnbufferedCharStream( reader );
		final Ttcn3Lexer lexer = new Ttcn3Lexer( charStream );
		lexer.setTokenFactory( new CommonTokenFactory( true ) );
		lexer.initRootInterval( code.length() );
		lexer.removeErrorListeners();

		final CommonTokenStream tokenStream = new CommonTokenStream( lexer );
		final Ttcn3Parser parser = new Ttcn3Parser( tokenStream );
		ParserUtilities.setBuildParseTree( parser );

		lexer.setActualFile(file);
		parser.setActualFile(file);
		parser.setProject(file.getProject());
		parser.setLine(aLine);
		parser.setOffset(aOffset);
		parser.removeErrorListeners();

		final Pr_UnifiedReferenceParserContext root = parser.pr_UnifiedReferenceParser();
		ParserUtilities.logParseTree( root, parser );

		return root.reference;
	}

	/**
	 * Parses the provided elements. If the contents of an editor are to be parsed,
	 *  than the file parameter is only used to report the errors to.
	 *
	 * @param file the file to parse, and report the errors to
	 * @param code the contents of an editor, or null.
	 * */
	public Reference parseForCompletion(final IFile file, final String code) {
		final Reader reader = new StringReader( code );
		final CharStream charStream = new UnbufferedCharStream( reader );
		final Ttcn3KeywordlessLexer lexer = new Ttcn3KeywordlessLexer( charStream );
		lexer.setTokenFactory( new CommonTokenFactory( true ) );
		lexer.initRootInterval( code.length() );
		lexer.removeErrorListeners();

		final CommonTokenStream tokenStream = new CommonTokenStream( lexer );
		final Ttcn3Parser parser = new Ttcn3Parser( tokenStream );
		ParserUtilities.setBuildParseTree( parser );

		lexer.setActualFile(file);
		parser.setActualFile(file);
		parser.setProject(file.getProject());
		parser.removeErrorListeners();

		final Pr_UnifiedReferenceParserContext root = parser.pr_UnifiedReferenceParser();
		ParserUtilities.logParseTree( root, parser );

		return root.reference;
	}
}
