/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.IType.Encoding_type;

/**
 * Represents a single encode attribute on an external function, used to
 * automatically generate the encoding function, according to the encoding type
 * and options passed as parameters..
 * 
 * @author Kristof Szabados
 * */
public final class EncodeAttribute extends ExtensionAttribute implements IVisitableNode {

	private final Encoding_type encodingType;
	private final String options;

	public EncodeAttribute(final Encoding_type encodingType, final String options) {
		this.encodingType = encodingType;
		this.options = options;
	}

	@Override
	/** {@inheritDoc} */
	public ExtensionAttribute_type getAttributeType() {
		return ExtensionAttribute_type.ENCODE;
	}

	public Encoding_type getEncodingType() {
		return encodingType;
	}

	public String getOptions() {
		return options;
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
		// no members
		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}
}
