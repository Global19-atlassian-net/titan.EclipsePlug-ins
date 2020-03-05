/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.implementation;

import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titanium.metrics.MetricData;
import org.eclipse.titanium.metrics.ModuleMetric;
import org.eclipse.titanium.metrics.visitors.Counter;
import org.eclipse.titanium.metrics.visitors.CounterVisitor;


public class MMNofStatements extends BaseModuleMetric {

	private static class StatementCounterVisitor extends CounterVisitor {

		public StatementCounterVisitor(final Counter n) {
			super(n);
		}

		@Override
		public int visit(final IVisitableNode node) {
			if (node instanceof StatementBlock) {
				count.increase(((StatementBlock) node).getSize());
			}
			return V_CONTINUE;
		}
	}

	public MMNofStatements() {
		super(ModuleMetric.NOF_STATEMENTS);
	}

	@Override
	public Number measure(final MetricData data, final Module module) {
		final Counter statements = new Counter(0);
		final StatementCounterVisitor visitor = new StatementCounterVisitor(statements);
		module.accept(visitor);
		return statements.val();
	}
}
