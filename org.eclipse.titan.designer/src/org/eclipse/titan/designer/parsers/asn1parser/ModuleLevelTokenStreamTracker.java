/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/

package org.eclipse.titan.designer.parsers.asn1parser;

/**
 * @author Laszlo Baji
 * */
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.WritableToken;
import org.antlr.v4.runtime.misc.Pair;
import org.eclipse.core.resources.IFile;

public class ModuleLevelTokenStreamTracker extends CommonTokenStream {
	private HashSet<Integer> discardMask = new HashSet<Integer>();
	private IFile sourceFile;

	public ModuleLevelTokenStreamTracker(final TokenSource source) {
		super(source);
		this.sourceFile = null;
	}

	public void setActualFile(final IFile sourceFile) {
		this.sourceFile = sourceFile;
	}

	public void discard(final int ttype) {
		discardMask.add(Integer.valueOf(ttype));
	}

	@Override
	public int fetch(int n) {
		if (fetchedEOF) {
			return 0;
		}
		Token t;
		Token first;

		int i = 0;

		do {
			t = getTokenSource().nextToken();
	        if ( t instanceof WritableToken ) {
	        	((WritableToken)t).setTokenIndex(tokens.size());
	        }
 			first = t;
			if (t.getType() == Token.EOF) {
				fetchedEOF = true;
		        tokens.add(new TokenWithIndexAndSubTokens(t));
				return ++i;
			} else if (discardMask.contains(Integer.valueOf(t.getType()))) {
				// discard this Token
				assert(true);
			} else if (t.getType() == Asn1Lexer.BEGINCHAR) {
				fetchedEOF = getBlock(first);
				if (fetchedEOF) {
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

	private boolean getBlock(final Token first) { // return true if EOF hit
		Token t;
		TokenWithIndexAndSubTokens result;

		t = getTokenSource().nextToken();
		if ( t instanceof WritableToken ) {
			((WritableToken)t).setTokenIndex(tokens.size());
		}

		List<Token> tokenList = new ArrayList<Token>();
		int nofUnclosedParanthesis = 1;

		while(t != null && t.getType() != Token.EOF) {
			if(t.getType() == Asn1Lexer.BEGINCHAR) {
				nofUnclosedParanthesis++;
			} else if(t.getType() == Asn1Lexer.ENDCHAR) {
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
				tokenList.add(new TokenWithIndexAndSubTokens(t));
			}
			t = getTokenSource().nextToken();
		}

		result = new TokenWithIndexAndSubTokens(new Pair<TokenSource, CharStream>(getTokenSource(), getTokenSource().getInputStream()), Asn1Lexer.BLOCK, 0, ((TokenWithIndexAndSubTokens) first).getStopIndex(), t == null ? 0 : ((TokenWithIndexAndSubTokens) t).getStopIndex(), tokenList, sourceFile);
		result.setCharPositionInLine(first.getCharPositionInLine());
		result.setLine(first.getLine());
		tokens.add(result);
		return true;
	}

	private String makeString(final List<Token> list) {
		StringBuilder text = new StringBuilder();
		for (Token t : list) {
			text.append(t.getText());
		}
		return text.toString();
	}
}
