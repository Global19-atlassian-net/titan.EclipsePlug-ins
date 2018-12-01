/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.parsers.asn1parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;
import org.eclipse.core.resources.IFile;
import org.eclipse.titan.designer.AST.ASN1.Block;

public class SyntaxLevelTokenStreamTracker extends CommonTokenStream {
	private HashSet<Integer> discardMask = new HashSet<Integer>();
	private IFile sourceFile;
	private int index;
	private List<Token> oldList;

	protected SyntaxLevelTokenStreamTracker(final Block aBlock, final int startIndex) {
		super(aBlock);
		this.index = startIndex;
		this.oldList = aBlock.getTokenList();
	}
	public void setActualFile(final IFile sourceFile) {
		this.sourceFile = sourceFile;
	}

	public void discard(final int ttype) {
		discardMask.add(Integer.valueOf(ttype));
	}

	public int getActualIndex() {
		return index;
	}

	@Override
	public int fetch (int n) {
		if (fetchedEOF) {
			return 0;
		}

		Token first;
		int i = 0;

		if (oldList == null || index >= oldList.size()) {
			tokens.add(new TokenWithIndexAndSubTokens(Token.EOF));
			return ++i;
		}

		do {
			final Token t = oldList.get(index++);
			//t = tokens.get(index++);
			first = t;
			if (t == null) {
				return 0;
			} else if (discardMask.contains(Integer.valueOf(t.getType()))) {
				// discard this Token
			} else if (t.getType() == Asn1Lexer.SQUAREOPEN) {
				final boolean exit = getBlock(first);
				if (exit) {
					return ++i;
				}
				++i;
				--n;
			} else {
				tokens.add(t);
				++i;
				--n;
			}
		} while (0 < n);
		return i;
	}

	public static Asn1Parser getASN1ParserForBlock(final Block aBlock) {
		return getASN1ParserForBlock(aBlock, 0);
	}

	public static Asn1Parser getASN1ParserForBlock(final Block aBlock, final int startIndex) {
		if(aBlock == null || aBlock.getLocation() == null) {
			return null;
		}

		final SyntaxLevelTokenStreamTracker tracker = new SyntaxLevelTokenStreamTracker(aBlock, startIndex);
		tracker.discard(Asn1Lexer.WS);
		tracker.discard(Asn1Lexer.MULTILINECOMMENT);
		tracker.discard(Asn1Lexer.SINGLELINECOMMENT);

		final Asn1Parser parser = new Asn1Parser(tracker);
		tracker.setActualFile((IFile) aBlock.getLocation().getFile());
		parser.setActualFile((IFile) aBlock.getLocation().getFile());
		parser.setBuildParseTree(false);
		final ASN1Listener parserListener = new ASN1Listener(parser);
		parser.removeErrorListeners(); // remove ConsoleErrorListener
		parser.addErrorListener(parserListener);

		return parser;
	}

	private boolean getBlock(final Token first) {
		if(index >= oldList.size()) {
			tokens.add(first);
			return true;
		}

		TokenWithIndexAndSubTokens result;
		Token t = oldList.get(index++);
		final List<Token> tokenList = new ArrayList<Token>();
		int nofUnclosedParanthesis = 1;
		while(t != null && t.getType() != Token.EOF && index < oldList.size()) {
			if(t.getType() == Asn1Lexer.SQUAREOPEN) {
				nofUnclosedParanthesis++;
			} else if(t.getType() == Asn1Lexer.SQUARECLOSE) {
				nofUnclosedParanthesis--;
				if(nofUnclosedParanthesis == 0) {
					result = new TokenWithIndexAndSubTokens(new Pair<TokenSource, CharStream>(getTokenSource(), getTokenSource().getInputStream()), Asn1Lexer.BLOCK, 0, ((TokenWithIndexAndSubTokens) first).getStopIndex(), ((TokenWithIndexAndSubTokens) t).getStopIndex(), tokenList, sourceFile);
					result.setCharPositionInLine(first.getCharPositionInLine());
					result.setLine(first.getLine());
					result.setText(makeString(tokenList));
					tokens.add(result);
					return false;
				}
			}
			if(!discardMask.contains(Integer.valueOf(t.getType()))) {
				tokenList.add(t);
			}
			t = oldList.get(index++);
		}

		result = new TokenWithIndexAndSubTokens(new Pair<TokenSource, CharStream>(getTokenSource(), getTokenSource().getInputStream()), Asn1Lexer.BLOCK, 0, ((TokenWithIndexAndSubTokens) first).getStopIndex(), t == null ? 0: ((TokenWithIndexAndSubTokens) t).getStopIndex(), tokenList, sourceFile);
		result.setCharPositionInLine(first.getCharPositionInLine());
		result.setLine(first.getLine());
		tokens.add(result);
		return true;
	}

	private String makeString(final List<Token> list) {
		final StringBuilder text = new StringBuilder();
		for (final Token t : list) {
			text.append(t.getText());
		}

		return text.toString();
	}
}
