/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.parsers.asn1parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;

public class TokenWithIndexAndSubTokensFactory implements	TokenFactory<TokenWithIndexAndSubTokens> {
	public static final TokenFactory<TokenWithIndexAndSubTokens> DEFAULT = new TokenWithIndexAndSubTokensFactory();
	protected final boolean copyText;

	public TokenWithIndexAndSubTokensFactory() {
		this(false);
	}

	public TokenWithIndexAndSubTokensFactory(final boolean copyText) {
		this.copyText = copyText;
	}

	@Override
	public TokenWithIndexAndSubTokens create(final Pair<TokenSource, CharStream> source, final int type, final String text,
			final int channel, final int start, final int stop,
							  final int line, final int charPositionInLine)
	{
		final TokenWithIndexAndSubTokens t = new TokenWithIndexAndSubTokens(source, type, channel, start, stop);
		t.setLine(line);
		t.setCharPositionInLine(charPositionInLine);
		if ( text!=null ) {
			t.setText(text);
		} else if ( copyText && source.b != null ) {
			t.setText(source.b.getText(Interval.of(start,stop)));
		}

		return t;
	}

	@Override
	public TokenWithIndexAndSubTokens create(final int type, final String text) {
		return new TokenWithIndexAndSubTokens(type, text);
	}


}
