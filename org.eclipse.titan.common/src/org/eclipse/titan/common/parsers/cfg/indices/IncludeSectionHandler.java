/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.common.parsers.cfg.indices;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Stores temporary config editor data of the include section
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class IncludeSectionHandler extends ConfigSectionHandlerBase {

	/** list of include files, which are stored as ParseTree nodes */
	private List<ParseTree> mFiles = new ArrayList<ParseTree>();

	public List<ParseTree> getFiles() {
		return mFiles;
	}

	public void addFile( final ParserRuleContext aIncludeFile ) {
		mFiles.add( aIncludeFile );
	}
}
