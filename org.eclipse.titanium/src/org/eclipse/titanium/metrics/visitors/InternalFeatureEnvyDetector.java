/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.visitors;

import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Helper visitor class, used by the metrics.
 * <p>
 * Counts the number of references to entities that are defined inside a given
 * module.
 *
 * @author poroszd
 *
 */
public class InternalFeatureEnvyDetector extends CounterVisitor {
	private final Module ownModule;

	public InternalFeatureEnvyDetector(final Module ownModule, final Counter n) {
		super(n);
		this.ownModule = ownModule;
	}

	@Override
	public int visit(final IVisitableNode node) {
		if (node instanceof Reference) {
			final Reference reference = (Reference) node;
			final Assignment assignment = reference.getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false);
			if (assignment != null) {
				final Module module = assignment.getMyScope().getModuleScope();
				if (ownModule == module) {
					count.inc();
				}
			}
		}
		return V_CONTINUE;
	}
}
