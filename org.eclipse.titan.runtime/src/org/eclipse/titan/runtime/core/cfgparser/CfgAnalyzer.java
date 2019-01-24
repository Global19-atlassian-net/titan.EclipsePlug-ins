/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core.cfgparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.ListIterator;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.UnbufferedCharStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.titan.runtime.core.TtcnError;

/**
 * Syntactic analyzer for CFG files
 * @author Arpad Lovassy
 */
public final class CfgAnalyzer {

	private static final int RECURSION_LIMIT = 20;
	final private static String TEMP_CFG_FILENAME = "temp.cfg";
	
	private ExecuteSectionHandler executeSectionHandler = null;
	private IncludeSectionHandler orderedIncludeSectionHandler = new IncludeSectionHandler();
	private DefineSectionHandler defineSectionHandler = new DefineSectionHandler();
	
	public ExecuteSectionHandler getExecuteSectionHandler() {
		return executeSectionHandler;
	}

	/**
	 * Parses a file.
	 *
	 * @param file the file to parse.
	 * @return {@code true} if there were errors in the file, {@code false} otherwise
	 */
	public boolean parse(final File file) {
		String fileName = "<unknown file>";
		if ( file != null ) {
			fileName = file.getName();
		}
		return directParse(file, fileName, null);
	}

	/**
	 * Parses a string.
	 *
	 * @param code the source code to parse.
	 * @return {@code true} if there were errors in the file, {@code false} otherwise
	 */
	public boolean parse(final String code) {
		final String fileName = "<unknown file>";
		return directParse(null, fileName, code);
	}

	/**
	 * RECURSIVE
	 * Preparse the [INCLUDE] and [ORDERED_INCLUDE] sections of a CFG file, which means that the include file name is replaced
	 * by the content of the include file recursively.
	 * After a successful include preparsing we get one CFG file that will not contain any [INCLUDE] or [ORDERED_INCLUDE] sections.
	 * @param file actual file to preparse
	 * @param sb output string buffer, where the resolved content is written
	 * @param lexerListener listener for ANTLR lexer/parser errors
	 * @param recursionDepth counter of the recursion depth
	 * @return true, if CFG file was changed during preparsing
	 *         false, otherwise, so the CFG file did not contain any [INCLUDE] or [ORDERED_INCLUDE] sections
	 */
	private boolean preparseInclude(final File file, final StringBuilder sb, final CFGListener lexerListener, final int recursionDepth) {
		if (recursionDepth > RECURSION_LIMIT) {
			// dumb but safe defense against infinite recursion, default value from gcc
			throw new TtcnError("Maximum include recursion depth reached in file: " + file.getName());
		}
		final String dir = file.getParent();
		boolean modified = false;
		final Reader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF8));
		} catch (FileNotFoundException e) {
			throw new TtcnError(e);
		}
		if ( lexerListener != null ) {
			lexerListener.setFilename(file.getName());
		}
		final RuntimeCfgLexer lexer = createLexer(reader, lexerListener);
		final CommonTokenStream tokenStream = new CommonTokenStream( lexer );
		tokenStream.fill();
		List<Token> tokens = tokenStream.getTokens();
		ListIterator<Token> iter = tokens.listIterator();
		while (iter.hasNext()) {
			final Token token = iter.next();
			final int tokenType = token.getType();
			final String tokenText = token.getText();
			switch (tokenType) {
			case RuntimeCfgLexer.INCLUDE_SECTION:
			case RuntimeCfgLexer.ORDERED_INCLUDE_SECTION:
				modified = true;
				break;
			case RuntimeCfgLexer.INCLUDE_FILENAME:
			case RuntimeCfgLexer.ORDERED_INCLUDE_FILENAME:
				final String orderedIncludeFilename = tokenText.substring( 1, tokenText.length() - 1 );
				if ( !orderedIncludeSectionHandler.isFileAdded( orderedIncludeFilename ) ) {
					orderedIncludeSectionHandler.addFile( orderedIncludeFilename );
					final File orderedIncludeFile = new File(dir, orderedIncludeFilename);
					preparseInclude(orderedIncludeFile, sb, lexerListener, recursionDepth + 1);
					modified = true;
				}
				break;

			default:
				sb.append(tokenText);
				break;
			}
		}

		IOUtils.closeQuietly(reader);
		return modified;
	}

	/**
	 * Preparse a CFG file.
	 * It effects the [INCLUDE], [ORDERED_INCLUDE] and [DEFINE] sections.
	 * After a successful include preparsing we get one CFG file that will not contain
	 * any [INCLUDE], [ORDERED_INCLUDE] or [DEFINE] sections.
	 * @param file actual file to preparse
	 * @param lexerListener listener for ANTLR lexer/parser errors
	 * @return true, if CFG file was changed during preparsing
	 *         false, otherwise, so the CFG file did not contain any [INCLUDE] or [ORDERED_INCLUDE] sections
	 */
	private boolean preparse(final File file, final CFGListener lexerListener) {
		final StringBuilder sb = new StringBuilder();
		final boolean modified = preparseInclude(file, sb, lexerListener, 0);
		//TODO: preparseDefine()
		writeTempCfg(file.getParent(), sb);
		return modified;
	}

	/**
	 * Create and initialize a new CFG Lexer object
	 * @param reader file reader
	 * @param lexerListener listener for ANTLR lexer/parser errors, it can be null
	 * @return the created lexer object
	 */
	private RuntimeCfgLexer createLexer(final Reader reader, final CFGListener lexerListener) {
		final CharStream charStream = new UnbufferedCharStream(reader);
		final RuntimeCfgLexer lexer = new RuntimeCfgLexer(charStream);
		lexer.setTokenFactory(new CommonTokenFactory(true));
		lexer.removeErrorListeners(); // remove ConsoleErrorListener
		if ( lexerListener != null ) {
			lexer.addErrorListener(lexerListener);
		}
		return lexer;
	}

	private void writeTempCfg( final String dir, final StringBuilder sb ) {
		final File out = new File(dir, TEMP_CFG_FILENAME);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(out);
			pw.append(sb);
		} catch (FileNotFoundException e) {
			throw new TtcnError(e);
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	/**
	 * Parses the provided elements.
	 * If the contents of an editor are to be parsed, than the file parameter is only used to report the errors to.
	 *
	 * @param file the file to parse
	 * @param fileName the name of the file, to refer to.
	 * @param code the contents of an editor, or null.
	 *
	 * @return {@code true} if there were errors in the file, {@code false} otherwise
	 */
	private boolean directParse(File file, final String fileName, final String code) {
		final Reader reader;
		final CFGListener lexerListener = new CFGListener(fileName);
		if (null != code) {
			// preparsing is not needed
			reader = new StringReader(code);
		} else if (null != file) {
			try {
				// if the cfg file is modified during the preparsing process, file is updated
				if ( preparse( file, lexerListener ) ) {
					// preparsing modified the cfg file, so use the temp.cfg instead
					file = new File(file.getParent(), TEMP_CFG_FILENAME);
				}
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF8));
			} catch (FileNotFoundException e) {
				throw new TtcnError(e);
			}
		} else {
			throw new TtcnError("CfgAnalyzer.directParse(): nothing to parse");
		}

		if ( lexerListener != null ) {
			lexerListener.setFilename(fileName);
		}
		final RuntimeCfgLexer lexer = createLexer(reader, lexerListener);

		// 1. Previously it was UnbufferedTokenStream(lexer), but it was changed to BufferedTokenStream, because UnbufferedTokenStream seems to be unusable. It is an ANTLR 4 bug.
		// Read this: https://groups.google.com/forum/#!topic/antlr-discussion/gsAu-6d3pKU
		// pr_PatternChunk[StringBuilder builder, boolean[] uni]:
		//   $builder.append($v.text); <-- exception is thrown here: java.lang.UnsupportedOperationException: interval 85..85 not in token buffer window: 86..341
		// 2. Changed from BufferedTokenStream to CommonTokenStream, otherwise tokens with "-> channel(HIDDEN)" are not filtered out in lexer.
		final CommonTokenStream tokenStream = new CommonTokenStream( lexer );
		final RuntimeCfgParser parser = new RuntimeCfgParser( tokenStream );
		parser.setActualFile( file );

		// remove ConsoleErrorListener
		parser.removeErrorListeners();
		final CFGListener parserListener = new CFGListener(fileName);
		parser.addErrorListener(parserListener);

		// parse tree is built by default
		parser.setBuildParseTree(false);

		parser.pr_ConfigFile();

		executeSectionHandler = parser.getExecuteSectionHandler();
		IOUtils.closeQuietly(reader);

		return lexerListener.encounteredError() || parserListener.encounteredError();
	}
}
