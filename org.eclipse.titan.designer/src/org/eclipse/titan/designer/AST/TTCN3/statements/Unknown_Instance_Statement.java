/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
/**
 * @author Kristof Szabados
 * */
public final class Unknown_Instance_Statement extends Statement {
	private static final String FUNCTIONORALTSTEPEXPECTED = "Reference to a function or altstep was expected instead of {0}, which cannot be invoked";

	private static final String FULLNAMEPART = ".reference";
	private static final String STATEMENT_NAME = "function or altstep instance";

	private final Reference reference;

	private Statement realStatement;

	/** The index of this statement in its parent statement block. */
	private int statementIndex;

	public Unknown_Instance_Statement(final Reference reference) {
		this.reference = reference;

		if (reference != null) {
			reference.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_UNKNOWN_INSTANCE;
	}

	@Override
	/** {@inheritDoc} */
	public String getStatementName() {
		return STATEMENT_NAME;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (reference == child) {
			return builder.append(FULLNAMEPART);
		}

		return builder;
	}

	public Statement getRealStatement() {
		return realStatement;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (reference != null) {
			reference.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyStatementBlock(final StatementBlock statementBlock, final int index) {
		super.setMyStatementBlock(statementBlock, index);
		statementIndex = index;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasReceivingStatement() {
		if (realStatement != null) {
			return realStatement.hasReceivingStatement();
		}
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		final Assignment assignment = reference.getRefdAssignment(timestamp, true);
		if (assignment == null) {
			return;
		}

		switch (assignment.getAssignmentType()) {
		case A_FUNCTION:
		case A_FUNCTION_RVAL:
		case A_FUNCTION_RTEMP:
		case A_EXT_FUNCTION:
		case A_EXT_FUNCTION_RVAL:
		case A_EXT_FUNCTION_RTEMP:
			if (realStatement == null || !Statement_type.S_FUNCTION_INSTANCE.equals(realStatement.getType())) {
				realStatement = new Function_Instance_Statement(reference);
				realStatement.setFullNameParent(this);
				realStatement.setLocation(location);
				realStatement.setMyStatementBlock(getMyStatementBlock(), statementIndex);
			}
			realStatement.check(timestamp);
			break;
		case A_ALTSTEP:
			if (realStatement == null || !Statement_type.S_ALTSTEP_INSTANCE.equals(realStatement.getType())) {
				realStatement = new Altstep_Instance_Statement(reference);
				realStatement.setFullNameParent(this);
				realStatement.setLocation(location);
				realStatement.setMyStatementBlock(getMyStatementBlock(), statementIndex);
			}
			realStatement.check(timestamp);
			break;
		default:
			reference.getLocation().reportSemanticError(MessageFormat.format(FUNCTIONORALTSTEPEXPECTED, assignment.getFullName()));
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkAllowedInterleave() {
		if (realStatement != null) {
			realStatement.checkAllowedInterleave();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (reference != null) {
			reference.updateSyntax(reparser, false);
			reparser.updateLocation(reference.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (realStatement != null) {
			realStatement.findReferences(referenceFinder, foundIdentifiers);
		} else {
			if (reference != null) {
				reference.findReferences(referenceFinder, foundIdentifiers);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (realStatement != null) {
			return realStatement.accept(v);
		} else {
			if (reference != null && !reference.accept(v)) {
				return false;
			}
			return true;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if (realStatement != null) {
			realStatement.generateCode(aData, source);
		}
	}
}
