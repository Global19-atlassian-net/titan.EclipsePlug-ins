/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.ttcn3editor;

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * @author Kristof Szabados
 * */
public class TitanSpecificKeywordDetector implements IWordDetector {

	@Override
	public boolean isWordStart(final char aChar) {
		return aChar == '@';
	}

	@Override
	public boolean isWordPart(final char aChar) {
		return Character.isLetterOrDigit(aChar) || '_' == aChar;
	}
}
