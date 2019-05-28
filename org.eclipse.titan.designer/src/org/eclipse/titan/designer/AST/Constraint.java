/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST;

import java.util.List;

import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents a constraint.
 *
 * @author Kristof Szabados
 * */
public abstract class Constraint extends ASTNode {

	//FIXME plenty more constraint kinds are not yet supported.
	public enum Constraint_type {
		/** table constraint */
		CT_TABLE,
		/**< PermittedAlphabetConstraint */
		CT_PERMITTEDALPHABET
	}

	private final Constraint_type constraintType;
	protected Type myType;

	/** the time when this constraint was check the last time. */
	protected CompilationTimeStamp lastTimeChecked;

	public Constraint(final Constraint_type constraintType) {
		this.constraintType = constraintType;
	}

	public Constraint_type getConstraintType() {
		return constraintType;
	}

	public void setMyType(final Type myType) {
		this.myType = myType;
	}

	/** @return a new instance of this constraint */
	public abstract Constraint newInstance();

	/**
	 * Does the semantic checking of the constraint.
	 *
	 * @param timestamp the time stamp of the actual semantic check cycle.
	 * */
	public abstract void check(final CompilationTimeStamp timestamp);

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		// TODO: ASN.1 subtypes are not implemented yet
	}
}
