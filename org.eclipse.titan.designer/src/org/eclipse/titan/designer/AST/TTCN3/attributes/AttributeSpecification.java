/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import java.util.List;

import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * A single attribute specification as read from the TTCN-3 by the TTCN-3
 * parser.
 *
 * It needs to be parsed later by an attribute parser, to extract the semantic
 * data from it.
 *
 * @author Kristof Szabados
 * */
public final class AttributeSpecification implements ILocateableNode, IIncrementallyUpdateable {
	private final String specification;

	final List<String> encodings;

	/**
	 * The location of the whole specification. This location encloses the
	 * specification fully, as it is used to report errors to.
	 **/
	private Location location = NULL_Location.INSTANCE;

	public AttributeSpecification(final String specification) {
		this.specification = specification;
		encodings = null;
	}

	public AttributeSpecification(final String specification, final List<String> encodings) {
		this.specification = specification;
		this.encodings = encodings;
	}

	/**
	 * @return the specification text of this attribute specification.
	 * */
	public String getSpecification() {
		return specification;
	}

	/**
	 * @return the encodings set for this attribute specification
	 * */
	public List<String> getEncodings() {
		return encodings;
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return location;
	}

	/**
	 * Handles the incremental parsing of this attribute specification.
	 *
	 * @param reparser
	 *                the parser doing the incremental parsing.
	 * @param isDamaged
	 *                true if the location contains the damaged area, false
	 *                if only its' location needs to be updated.
	 * */
	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}
	}
}
