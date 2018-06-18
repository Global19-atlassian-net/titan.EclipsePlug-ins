/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.markers.spotters.implementation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.statements.AltGuard;
import org.eclipse.titan.designer.AST.TTCN3.statements.If_Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titanium.markers.spotters.BaseModuleCodeSmellSpotter;
import org.eclipse.titanium.markers.types.CodeSmellType;

public class IfInsteadAltguard extends BaseModuleCodeSmellSpotter {
	private static final String MIGHT_ALTGUARD = "Consider whether the condition could be transformed to an alt guard";

	public IfInsteadAltguard() {
		super(CodeSmellType.IF_INSTEAD_ALTGUARD);
	}

	@Override
	public void process(final IVisitableNode node, final Problems problems) {
		if (node instanceof AltGuard) {
			final AltGuard ag = (AltGuard) node;
			final StatementBlock statements = ag.getStatementBlock();
			if (statements != null && !statements.isEmpty()) {
				final Statement firstStatement = statements.getStatementByIndex(0);
				if (firstStatement instanceof If_Statement) {
					final Value condition = ((If_Statement) firstStatement).getIfClauses().getClauses().get(0).getExpression();
					Location reportAt;
					if (condition != null) {
						reportAt = condition.getLocation();
					} else {
						reportAt = firstStatement.getLocation();
					}
					problems.report(reportAt, MIGHT_ALTGUARD);
				}
			}
		}
	}

	@Override
	public List<Class<? extends IVisitableNode>> getStartNode() {
		final List<Class<? extends IVisitableNode>> ret = new ArrayList<Class<? extends IVisitableNode>>(1);
		ret.add(AltGuard.class);
		return ret;
	}
}
