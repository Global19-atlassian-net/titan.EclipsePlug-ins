/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.logging.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.TTCN3.statements.If_Clause;
import org.eclipse.titan.designer.AST.TTCN3.statements.If_Clauses;
import org.eclipse.titan.designer.AST.TTCN3.statements.If_Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titanium.refactoring.logging.ContextLoggingRefactoring.Settings;

/**
 * Context class representing {@link If_Statement} nodes.
 *
 * @author Viktor Varga
 */
class IfContext extends Context {

	private final Set<String> varNamesInConditions;

	IfContext(final If_Statement st, final Settings settings) {
		super(st, settings);
		varNamesInConditions = new HashSet<String>();
	}

	@Override
	public If_Statement getNode() {
		return (If_Statement)super.getNode();
	}

	protected void process_internal() {
		final If_Statement st = getNode();
		final If_Clauses ics = st.getIfClauses();
		final Context child = getChild();
		if (child != null && child.getNode().equals(ics)) {
			//the log statement is in one of the conditional clauses
			final List<If_Clause> icl = ics.getClauses();
			final Context clauseContext = child.getChild();
			if (clauseContext != null && icl.contains(clauseContext.getNode())) {
				final IVisitableNode ic = clauseContext.getNode();
				final ClauseVisitor vis = new ClauseVisitor();
				ic.accept(vis);
				final List<Reference> refs = vis.getResult();
				for (Reference ref: refs) {
					varNamesInConditions.add(ref.getDisplayName());
				}
			}
		} else {
			//the log statement is in the else block
			final List<Reference> refs = extractAllIdsFromClauses(ics);
			for (Reference ref: refs) {
				varNamesInConditions.add(ref.getDisplayName());
			}
		}
	}

	private static List<Reference> extractAllIdsFromClauses(final If_Clauses ics) {
		final List<If_Clause> icl = ics.getClauses();
		final List<Reference> ret = new ArrayList<Reference>();
		for (If_Clause ic: icl) {
			final ClauseVisitor vis = new ClauseVisitor();
			ic.accept(vis);
			ret.addAll(vis.getResult());
		}
		return ret;
	}

	@Override
	protected List<String> createLogParts_internal(final Set<String> idsAlreadyHandled) {
		final List<String> ret = new ArrayList<String>();
		if (varNamesInConditions == null) {
			return ret;
		}
		for (String s: varNamesInConditions) {
			if (idsAlreadyHandled.contains(s)) {
				continue;
			}
			idsAlreadyHandled.add(s);
			ret.add(formatLogPart(s));
		}
		return ret;
	}

	/**
	 * Collects all the references from an {@link If_Clause}.
	 * <p>
	 * Call on {@link If_Clause}
	 * */
	private static class ClauseVisitor extends ASTVisitor {

		private final List<Reference> result = new ArrayList<Reference>();

		private List<Reference> getResult() {
			return result;
		}

		@Override
		public int visit(final IVisitableNode node) {
			if (node instanceof Reference) {
				final Reference ref = (Reference)node;

				final Assignment refdAssignment = ref.getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false);
				switch(refdAssignment.getAssignmentType()) {
				case A_ALTSTEP:
				case A_EXT_FUNCTION:
				case A_EXT_FUNCTION_RTEMP:
				case A_EXT_FUNCTION_RVAL:
				case A_FUNCTION:
				case A_FUNCTION_RTEMP:
				case A_FUNCTION_RVAL:
				case A_TESTCASE:
					break;
				default:
					result.add(ref);
				}
				//result.add(ref);
				return V_SKIP;
			}
			if (node instanceof StatementBlock) {
				return V_SKIP;
			}
			return V_CONTINUE;
		}

	}

}
