/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types.subtypes;

import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a ranged sub-type restriction as it was parsed.
 *
 * @author Adam Delic
 * */
public final class Range_ParsedSubType extends ParsedSubType {
	private final Value min;
	private final boolean minExclusive;
	private final Value max;
	private final boolean maxExclusive;

	public Range_ParsedSubType(final Value min, final boolean minExclusive, final Value max, final boolean maxExclusive) {
		this.min = min;
		this.minExclusive = minExclusive;
		this.max = max;
		this.maxExclusive = maxExclusive;
	}

	@Override
	/** {@inheritDoc} */
	public ParsedSubType_type getSubTypetype() {
		return ParsedSubType_type.RANGE_PARSEDSUBTYPE;
	}

	public Value getMin() {
		return min;
	}

	public boolean getMinExclusive() {
		return minExclusive;
	}

	public Value getMax() {
		return max;
	}

	public boolean getMaxExclusive() {
		return maxExclusive;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (min != null) {
			min.updateSyntax(reparser, false);
			reparser.updateLocation(min.getLocation());
		}

		if (max != null) {
			max.updateSyntax(reparser, false);
			reparser.updateLocation(max.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		// FIXME: this object should always know it's own location, but
		// currently infinity is null
		if ((min != null) && (max != null)) {
			return Location.interval(min.getLocation(), max.getLocation());
		}

		return null;
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (min != null) {
			min.findReferences(referenceFinder, foundIdentifiers);
		}
		if (max != null) {
			max.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean accept(final ASTVisitor v) {
		switch (v.visit(this)) {
		case ASTVisitor.V_ABORT:
			return false;
		case ASTVisitor.V_SKIP:
			return true;
		}
		if (min != null) {
			if (!min.accept(v)) {
				return false;
			}
		}
		if (max != null) {
			if (!max.accept(v)) {
				return false;
			}
		}
		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}
}
