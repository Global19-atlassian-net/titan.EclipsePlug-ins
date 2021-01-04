/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types.subtypes;

/**
 * RangeListConstraint with added NaN value (NaN is unordered so it cannot be a
 * limit value) this is canonical only if two different Real values are never
 * considered to be adjacent which means that in theory for two different Real
 * values there are always infinite number of Real values that are between them
 *
 * @author Adam Delic
 */
public final class RealRangeListConstraint extends SubtypeConstraint {
	private final boolean hasNan;
	private final RangeListConstraint rlc;

	/** empty set constructor */
	public RealRangeListConstraint() {
		hasNan = false;
		rlc = new RangeListConstraint(LimitType.Type.REAL);
	}

	/** single value set */
	public RealRangeListConstraint(final Double d) {
		if (Double.isNaN(d)) {
			hasNan = true;
			rlc = new RangeListConstraint(LimitType.Type.REAL);
			return;
		}
		hasNan = false;
		rlc = new RangeListConstraint(new RealLimit(d));
	}

	/** value range set */
	public RealRangeListConstraint(final RealLimit rlBegin, final RealLimit rlEnd) {
		hasNan = false;
		rlc = new RangeListConstraint(rlBegin, rlEnd);
	}

	private RealRangeListConstraint(final boolean hasNan, final RangeListConstraint rlc) {
		this.hasNan = hasNan;
		this.rlc = rlc;
	}

	@Override
	public RealRangeListConstraint complement() {
		return new RealRangeListConstraint(!hasNan, rlc.complement());
	}

	@Override
	/** {@inheritDoc} */
	public RealRangeListConstraint intersection(final SubtypeConstraint other) {
		final RealRangeListConstraint o = (RealRangeListConstraint) other;
		return new RealRangeListConstraint(hasNan && o.hasNan, rlc.intersection(o.rlc));
	}

	@Override
	/** {@inheritDoc} */
	public boolean isElement(final Object o) {
		final Double d = (Double) o;
		if (d.isNaN()) {
			return hasNan;
		}

		return rlc.isElement(new RealLimit(d));
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isEmpty() {
		return rlc.isEmpty().and(TernaryBool.fromBool(!hasNan));
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isEqual(final SubtypeConstraint other) {
		final RealRangeListConstraint o = (RealRangeListConstraint) other;
		return rlc.isEqual(o.rlc).and(TernaryBool.fromBool(hasNan == o.hasNan));
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isFull() {
		return rlc.isFull().and(TernaryBool.fromBool(hasNan));
	}

	@Override
	/** {@inheritDoc} */
	public void toString(final StringBuilder sb) {
		sb.append('(');
		rlc.toString(sb, false);
		if (hasNan) {
			if (rlc.isEmpty() != TernaryBool.TTRUE) {
				sb.append(", ");
			}
			sb.append("NaN");
		}
		sb.append(')');
	}

	@Override
	/** {@inheritDoc} */
	public RealRangeListConstraint union(final SubtypeConstraint other) {
		final RealRangeListConstraint o = (RealRangeListConstraint) other;
		return new RealRangeListConstraint(hasNan || o.hasNan, rlc.union(o.rlc));
	}

}
