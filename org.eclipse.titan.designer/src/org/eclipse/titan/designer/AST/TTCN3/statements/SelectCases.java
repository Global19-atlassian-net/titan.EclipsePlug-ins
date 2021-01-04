/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * The SelectCases class is helper class for the SelectCase_Statement class.
 * Holds a list of the select cases that were parsed from the source code.
 *
 * @see SelectCase_Statement {@link SelectCase_Statement}
 * @see SelectCase
 *
 * @author Kristof Szabados
 * */
public final class SelectCases extends ASTNode implements IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".select_case_";

	private final List<SelectCase> select_cases;

	public SelectCases() {
		select_cases = new ArrayList<SelectCase>();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0, size = select_cases.size(); i < size; i++) {
			if (select_cases.get(i) == child) {
				return builder.append(FULLNAMEPART).append(Integer.toString(i + 1));
			}
		}

		return builder;
	}

	/**
	 * Adds a select case branch.
	 * <p>
	 * The parameter can not be null, that case is handled in the parser.
	 *
	 * @param selectCase
	 *                the select case to be added.
	 * */
	public void addSelectCase(final SelectCase selectCase) {
		select_cases.add(selectCase);
		selectCase.setFullNameParent(this);
	}

	/**
	 * Sets the scope of the contained select case branches.
	 *
	 * @param scope
	 *                the scope to be set.
	 * */
	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		for (final SelectCase select_case : select_cases) {
			select_case.setMyScope(scope);
		}
	}

	/**
	 * Sets the code_section attribute for the statements in this select case list to the provided value.
	 *
	 * @param codeSection the code section where these statements should be generated.
	 * */
	public void setCodeSection(final CodeSectionType codeSection) {
		for (final SelectCase select_case : select_cases) {
			select_case.setCodeSection(codeSection);
		}
	}

	public void setMyStatementBlock(final StatementBlock statementBlock, final int index) {
		for (int i = 0, size = select_cases.size(); i < size; i++) {
			select_cases.get(i).setMyStatementBlock(statementBlock, index);
		}
	}

	public void setMyDefinition(final Definition definition) {
		for (final SelectCase select_case : select_cases) {
			select_case.setMyDefinition(definition);
		}
	}

	public void setMyAltguards(final AltGuards altGuards) {
		for (final SelectCase select_case : select_cases) {
			select_case.setMyAltguards(altGuards);
		}
	}

	/**
	 * Used to tell break and continue statements if they are located with an altstep, a loop or none.
	 *
	 * @param pAltGuards the altguards set only within altguards
	 * @param pLoopStmt the loop statement, set only within loops.
	 * */
	public void setMyLaicStmt(final AltGuards pAltGuards, final Statement pLoopStmt) {
		for (final SelectCase selectCase : select_cases) {
			selectCase.getStatementBlock().setMyLaicStmt(pAltGuards, pLoopStmt);
		}
	}

	/**
	 * Checks whether the select cases have a return statement, either
	 * directly or embedded.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 *
	 * @return the return status of the select cases.
	 * */
	public StatementBlock.ReturnStatus_type hasReturn(final CompilationTimeStamp timestamp) {
		StatementBlock.ReturnStatus_type result = StatementBlock.ReturnStatus_type.RS_MAYBE;
		boolean hasElse = false;
		for (final SelectCase select_case : select_cases) {
			switch (select_case.hasReturn(timestamp)) {
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

			if (select_case.hasElse()) {
				hasElse = true;
				break;
			}
		}

		if (!hasElse && result == StatementBlock.ReturnStatus_type.RS_YES) {
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
		for (int i = 0; i < select_cases.size(); i++) {
			if (select_cases.get(i).hasReceivingStatement()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Does the semantic checking of the select case list.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param governor
	 *                the governor of the select expression, to check the
	 *                cases against.
	 * */
	public void check(final CompilationTimeStamp timestamp, final IType governor) {
		boolean unrechable = false;
		for (final SelectCase select_case : select_cases) {
			unrechable = select_case.check(timestamp, governor, unrechable);
		}
	}

	/**
	 * Checks if some statements are allowed in an interleave or not
	 * */
	public void checkAllowedInterleave() {
		for (final SelectCase select_case : select_cases) {
			select_case.checkAllowedInterleave();
		}
	}

	/**
	 * Checks the properties of the statement, that can only be checked
	 * after the semantic check was completely run.
	 */
	public void postCheck() {
		for (final SelectCase select_case : select_cases) {
			select_case.postCheck();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		for (final SelectCase select_case : select_cases) {
			select_case.updateSyntax(reparser, false);
			reparser.updateLocation(select_case.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (select_cases == null) {
			return;
		}

		for (final SelectCase sc : select_cases) {
			sc.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (select_cases != null) {
			for (final SelectCase sc : select_cases) {
				if (!sc.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}

	public List<SelectCase> getSelectCaseArray() {
		return select_cases;
	}

	/**
	 * Add generated java code for the list of select cases.
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 * @param name the name to compare the branch variables to (expression or temporary name)
	 */
	public void generateCode(final JavaGenData aData, final StringBuilder source, final String name) {
		final StringBuilder init = new StringBuilder();
		final AtomicBoolean unreach = new AtomicBoolean(false);

		for (int i = 0; i < select_cases.size(); i++) {
			if (unreach.get()) {
				break;
			}
			if (i > 0) {
				source.append("else { \n");
				init.append("}\n");
			}
			select_cases.get(i).generateCode(aData, source, name, unreach);
		}

		source.append(init);
	}
}
