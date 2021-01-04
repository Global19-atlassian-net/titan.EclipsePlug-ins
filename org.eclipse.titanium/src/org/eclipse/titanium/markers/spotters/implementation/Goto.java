/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.markers.spotters.implementation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.TTCN3.statements.Goto_statement;
import org.eclipse.titanium.markers.spotters.BaseModuleCodeSmellSpotter;
import org.eclipse.titanium.markers.types.CodeSmellType;

public class Goto extends BaseModuleCodeSmellSpotter {
	private static final String ERROR_MESSAGE = "Usage of goto and label statements is not recommended "
			+ "as they usually break the structure of the code";

	public Goto() {
		super(CodeSmellType.GOTO);
	}

	@Override
	public void process(final IVisitableNode node, final Problems problems) {
		if (node instanceof Goto_statement) {
			final Goto_statement s = (Goto_statement) node;
			problems.report(s.getLocation(), ERROR_MESSAGE);
		}
	}

	@Override
	public List<Class<? extends IVisitableNode>> getStartNode() {
		final List<Class<? extends IVisitableNode>> ret = new ArrayList<Class<? extends IVisitableNode>>(1);
		ret.add(Goto_statement.class);
		return ret;
	}
}
