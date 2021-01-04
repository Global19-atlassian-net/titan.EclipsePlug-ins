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
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.AST.TTCN3.statements.Stop_Execution_Statement;
import org.eclipse.titanium.markers.spotters.BaseModuleCodeSmellSpotter;
import org.eclipse.titanium.markers.types.CodeSmellType;

public class StopInFunction extends BaseModuleCodeSmellSpotter {
	private static final String ERROR_MESSAGE = "The stop execution statement should not be used in functions";

	public StopInFunction() {
		super(CodeSmellType.STOP_IN_FUNCTION);
	}

	@Override
	public void process(final IVisitableNode node, final Problems problems) {
		if (node instanceof Stop_Execution_Statement) {
			final Stop_Execution_Statement s = (Stop_Execution_Statement) node;
			final StatementBlock sb = s.getMyStatementBlock();
			final Definition d = sb.getMyDefinition();
			if (d instanceof Def_Function) {
				problems.report(s.getLocation(), ERROR_MESSAGE);
			}
		}
	}

	@Override
	public List<Class<? extends IVisitableNode>> getStartNode() {
		final List<Class<? extends IVisitableNode>> ret = new ArrayList<Class<? extends IVisitableNode>>(1);
		ret.add(Stop_Execution_Statement.class);
		return ret;
	}
}
