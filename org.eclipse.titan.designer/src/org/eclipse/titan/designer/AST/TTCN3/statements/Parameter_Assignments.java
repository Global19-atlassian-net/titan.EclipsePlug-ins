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

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents the parameter assignments of a parameter redirection.
 *
 * @author Kristof Szabados
 * */
public final class Parameter_Assignments extends ASTNode implements IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".parameterassignment_";

	private final List<Parameter_Assignment> parameterAssignments;

	public Parameter_Assignments() {
		parameterAssignments = new ArrayList<Parameter_Assignment>();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0; i < parameterAssignments.size(); i++) {
			if (parameterAssignments.get(i) == child) {
				return builder.append(FULLNAMEPART).append(Integer.toString(i + 1));
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);

		for (int i = 0; i < parameterAssignments.size(); i++) {
			parameterAssignments.get(i).setMyScope(scope);
		}
	}

	/**
	 * Sets the code_section attribute for the statements in these parameter assignments to the provided value.
	 *
	 * @param codeSection the code section where these statements should be generated.
	 * */
	public void setCodeSection(final CodeSectionType codeSection) {
		for (int i = 0; i < parameterAssignments.size(); i++) {
			parameterAssignments.get(i).setCodeSection(codeSection);
		}
	}

	public void add(final Parameter_Assignment parameterAssignment) {
		parameterAssignments.add(parameterAssignment);
		parameterAssignment.setFullNameParent(this);
	}

	public int getNofParameterAssignments() {
		return parameterAssignments.size();
	}

	public Parameter_Assignment getParameterAssignmentByIndex(final int index) {
		return parameterAssignments.get(index);
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		for (final Parameter_Assignment assignment : parameterAssignments) {
			assignment.updateSyntax(reparser, isDamaged);
			reparser.updateLocation(assignment.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (parameterAssignments == null) {
			return;
		}

		for (final Parameter_Assignment pa : parameterAssignments) {
			pa.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (parameterAssignments != null) {
			for (final Parameter_Assignment pa : parameterAssignments) {
				if (!pa.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}
}
