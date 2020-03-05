/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types.subtypes;

import org.eclipse.titan.designer.AST.TTCN3.templates.PatternString;

/**
 * @author Adam Delic
 * */
public final class StringPatternConstraint extends SubtypeConstraint {
	private final PatternString pattern;

	public StringPatternConstraint(final PatternString pattern) {
		this.pattern = pattern;
	}

	@Override
	/** {@inheritDoc} */
	public StringPatternConstraint complement() {
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public StringPatternConstraint intersection(final SubtypeConstraint other) {
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isElement(final Object o) {
		// TODO
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isEmpty() {
		return TernaryBool.TUNKNOWN;
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isEqual(final SubtypeConstraint other) {
		if (this == other) {
			return TernaryBool.TTRUE;
		}

		return TernaryBool.TUNKNOWN;
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isFull() {
		return TernaryBool.TUNKNOWN;
	}

	@Override
	/** {@inheritDoc} */
	public void toString(final StringBuilder sb) {
		// TODO
		sb.append("<patterns not implemented yet>");
	}

	@Override
	/** {@inheritDoc} */
	public StringPatternConstraint union(final SubtypeConstraint other) {
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isSubset(final SubtypeConstraint other) {
		return TernaryBool.TUNKNOWN;
	}

}
