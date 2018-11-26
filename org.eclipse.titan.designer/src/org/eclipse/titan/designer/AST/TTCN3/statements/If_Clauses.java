/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * The If_Clauses class represents the clauses (branches) of a TTCN3 if
 * statement.
 *
 * @see If_Clause
 * @see If_Statement
 *
 * @author Kristof Szabados
 * */
public final class If_Clauses extends ASTNode implements IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".if_clause_";

	private final List<If_Clause> ifclauses;

	public If_Clauses() {
		ifclauses = new ArrayList<If_Clause>();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			if (ifclauses.get(i) == child) {
				return builder.append(FULLNAMEPART).append(Integer.toString(i + 1));
			}
		}

		return builder;
	}

	public void setMyStatementBlock(final StatementBlock statementBlock, final int index) {
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			ifclauses.get(i).setMyStatementBlock(statementBlock, index);
		}
	}

	public void setMyDefinition(final Definition definition) {
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			ifclauses.get(i).setMyDefinition(definition);
		}
	}

	public void addIfClause(final If_Clause ifClause) {
		ifclauses.add(ifClause);
		ifClause.setFullNameParent(this);
	}

	public void addFrontIfClause(final If_Clause ifClause) {
		ifclauses.add(0, ifClause);
		ifClause.setFullNameParent(this);
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			ifclauses.get(i).setMyScope(scope);
		}
	}

	/**
	 * Sets the code_section attribute for the statements in this if clause list to the provided value.
	 *
	 * @param codeSection the code section where these statements should be generated.
	 * */
	public void setCodeSection(final CodeSectionType codeSection) {
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			ifclauses.get(i).setCodeSection(codeSection);
		}
	}

	public void setMyAltguards(final AltGuards altGuards) {
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			ifclauses.get(i).setMyAltguards(altGuards);
		}
	}

	/**
	 * Used to tell break and continue statements if they are located with an altstep, a loop or none.
	 *
	 * @param pAltGuards the altguards set only within altguards
	 * @param pLoopStmt the loop statement, set only within loops.
	 * */
	public void setMyLaicStmt(final AltGuards pAgs, final Statement pLoopStmt) {
		for (final If_Clause ifClause : ifclauses) {
			ifClause.getStatementBlock().setMyLaicStmt(pAgs, pLoopStmt);
		}
	}

	/**
	 * Checks whether the if clauses have a return statement, either
	 * directly or embedded.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param elseBlock
	 *                the statement block of the if statement's else branch.
	 *
	 * @return the return status of the if clauses.
	 * */
	public StatementBlock.ReturnStatus_type hasReturn(final CompilationTimeStamp timestamp, final StatementBlock elseBlock) {
		StatementBlock.ReturnStatus_type result = StatementBlock.ReturnStatus_type.RS_MAYBE;

		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			switch (ifclauses.get(i).hasReturn(timestamp)) {
			case RS_NO:
				if (result == StatementBlock.ReturnStatus_type.RS_YES) {
					return StatementBlock.ReturnStatus_type.RS_MAYBE;
				}

				result = StatementBlock.ReturnStatus_type.RS_NO;
				break;
			case RS_YES:
				if (result == StatementBlock.ReturnStatus_type.RS_NO) {
					return StatementBlock.ReturnStatus_type.RS_MAYBE;
				}

				result = StatementBlock.ReturnStatus_type.RS_YES;
				break;
			default:
				return StatementBlock.ReturnStatus_type.RS_MAYBE;
			}
		}

		StatementBlock.ReturnStatus_type elseStatus;
		if (elseBlock != null) {
			elseStatus = elseBlock.hasReturn(timestamp);
		} else {
			elseStatus = StatementBlock.ReturnStatus_type.RS_NO;
		}

		switch (elseStatus) {
		case RS_NO:
			if (result == StatementBlock.ReturnStatus_type.RS_YES) {
				return StatementBlock.ReturnStatus_type.RS_MAYBE;
			}

			result = StatementBlock.ReturnStatus_type.RS_NO;
			break;
		case RS_YES:
			if (result == StatementBlock.ReturnStatus_type.RS_NO) {
				return StatementBlock.ReturnStatus_type.RS_MAYBE;
			}

			result = StatementBlock.ReturnStatus_type.RS_YES;
			break;
		default:
			return StatementBlock.ReturnStatus_type.RS_MAYBE;
		}

		return result;
	}

	/**
	 * Used when generating code for interleaved statement.
	 * If the block has no receiving statements, then the general code generation can be used
	 *  (which may use blocks).
	 * */
	public boolean hasReceivingStatement() {
		for (int i = 0; i < ifclauses.size(); i++) {
			if (ifclauses.get(i).hasReceivingStatement()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Does the semantic checking of the alt guard list.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param unreachable
	 *                boolean parameter telling if this if statement was
	 *                already found unreachable by previous clauses or not
	 *
	 * @return true if further branches should are no longer reachable.
	 * */
	public boolean check(final CompilationTimeStamp timestamp, final boolean unreachable) {
		boolean temporalUnreachable = unreachable;
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			temporalUnreachable = ifclauses.get(i).check(timestamp, temporalUnreachable);
		}
		return temporalUnreachable;
	}

	/**
	 * Checks if some statements are allowed in an interleave or not
	 * */
	public void checkAllowedInterleave() {
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			ifclauses.get(i).checkAllowedInterleave();
		}
	}

	/**
	 * Checks the properties of the statement, that can only be checked
	 * after the semantic check was completely run.
	 */
	public void postCheck() {
		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			ifclauses.get(i).postCheck();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		for (int i = 0, size = ifclauses.size(); i < size; i++) {
			final If_Clause clause = ifclauses.get(i);

			clause.updateSyntax(reparser, false);
			reparser.updateLocation(clause.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (ifclauses == null) {
			return;
		}

		for (final If_Clause ic : ifclauses) {
			ic.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	/**
	 * A check to aid detection of single if without else code smell.
	 *
	 * @return true if there is exactly one if clause.
	 */
	public boolean isExactlyOne() {
		return ifclauses != null && ifclauses.size() == 1;
	}

	/**
	 * A check to aid detection of NegatedConditionInIf code smell.
	 *
	 * @return true if there is exactly one if clause, and that has a
	 *         negated expression as condition.
	 */
	public boolean isExactlyOneNegated() {
		return (ifclauses != null && ifclauses.size() == 1 && ifclauses.get(0).isNegatedCondition());
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (ifclauses != null) {
			for (final If_Clause ic : ifclauses) {
				if (!ic.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}

	public List<If_Clause> getClauses() {
		return ifclauses;
	}

	/**
	 * Add generated java code on this level.
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 * @param blockCount the number of block already created
	 * @param unReachable tells whether this branch is already unreachable because of previous conditions
	 * @param eachFalse true if the branches so far all evaluated to a false condition in compile time.
	 */
	public void generateCode( final JavaGenData aData, final StringBuilder source, final AtomicInteger blockCount, final AtomicBoolean unReachable, final AtomicBoolean eachFalse) {
		for (int i = 0; i < ifclauses.size(); i++) {
			if (unReachable.get()) {
				return;
			}

			ifclauses.get(i).generateCode(aData, source, blockCount, unReachable, eachFalse);
		}
	}
}
