/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents a generic extension attribute. All real attributes are sub-classed
 * from this.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public abstract class ExtensionAttribute implements ILocateableNode, INamedNode {
	public enum ExtensionAttribute_type {
		PROTOTYPE, ENCODE, DECODE, ERRORBEHAVIOR, PRINTING, TRANSPARENT,
		ANYTYPE, ENCDECVALUE, PORTTYPE, DONE, EXTENDS, VERSION, REQUIRES, TITANVERSION
	}

	/** the time when this attribute was checked the last time. */
	protected CompilationTimeStamp lastTimeChecked;
	/** the naming parent of the node. */
	private INamedNode nameParent;

	/**
	 * The location of the whole attribute. This location encloses the
	 * attribute fully, as it is used to report errors to.
	 **/
	private Location location = NULL_Location.INSTANCE;

	/**
	 * @return the kind of the extension attribute represented by this attribute instance.
	 * */
	public abstract ExtensionAttribute_type getAttributeType();

	@Override
	/** {@inheritDoc} */
	public final void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public final Location getLocation() {
		return location;
	}

	@Override
	/** {@inheritDoc} */
	public String getFullName() {
		return getFullName(this).toString();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		if (nameParent != null) {
			return nameParent.getFullName(this);
		}

		return new StringBuilder();
	}

	@Override
	/** {@inheritDoc} */
	public final void setFullNameParent(final INamedNode nameParent) {
		this.nameParent = nameParent;
	}

	@Override
	/** {@inheritDoc} */
	public INamedNode getNameParent() {
		return nameParent;
	}
}
