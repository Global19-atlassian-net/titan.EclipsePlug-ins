/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.parsers.asn1parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;
import org.eclipse.core.resources.IFile;

/**
 * @author Laszlo Baji
 */

public class TokenWithIndexAndSubTokens extends CommonToken {
	private static final long serialVersionUID = 3906412166039744425L;
	List<Token> tokenList = null;
	IFile sourceFile;

	public TokenWithIndexAndSubTokens(final Pair<TokenSource, CharStream> source, final int type, final int channel, final int start, final int stop) {
		super(source, type, channel, start, stop);
		this.tokenList = new ArrayList<Token>();
	}

	public TokenWithIndexAndSubTokens(final Pair<TokenSource, CharStream> source, final int type, final int channel, final int start, final int stop, final List<Token> tokenList, final IFile sourceFile) {
		super(source, type, channel, start, stop);
		this.tokenList = tokenList;
		this.sourceFile = sourceFile;
	}

	public TokenWithIndexAndSubTokens(final int t) {
		super(t);
		this.tokenList = new ArrayList<Token>();
	}

	public TokenWithIndexAndSubTokens(final int t, final String text) {
		super(t, text);
		this.tokenList = new ArrayList<Token>();
	}

	public TokenWithIndexAndSubTokens(final Token tok) {
		super(tok);
		tokenList = new ArrayList<Token>();
		super.setStartIndex(tok.getStartIndex());
		super.setStopIndex(tok.getStopIndex());
	}

	public IFile getSourceFile() {
		return sourceFile;
	}

	@Override
	public void setText(final String s) {
		super.setText(s);
	}

	public List<Token> getSubTokens() {
		return tokenList;
	}

	@Override
	public String toString() {
		return"[:\"" + getText() + "\",<" + getType() + ">,line=" + line + ",col=" + charPositionInLine + ",start=" + start + ",stop=" + stop +"]\n";
	}

	public TokenWithIndexAndSubTokens copy() {
		final TokenWithIndexAndSubTokens token = new TokenWithIndexAndSubTokens(source, type, channel, start, stop, tokenList, sourceFile);
		token.line = line;
		token.charPositionInLine = charPositionInLine;
		token.setText(getText());

		return token;
	}

}

