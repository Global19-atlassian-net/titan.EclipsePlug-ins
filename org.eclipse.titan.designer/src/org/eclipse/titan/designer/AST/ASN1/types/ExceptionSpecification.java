/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.types;

import java.util.List;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IType.TypeOwner_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.ASN1.ASN1Type;
import org.eclipse.titan.designer.AST.ASN1.IASN1Type;

/**
 * ExceptionSpecification.
 *
 * @author Kristof Szabados
 */
public final class ExceptionSpecification extends ASTNode {

	private final ASN1Type type;
	private final Value value;

	public ExceptionSpecification(final ASN1Type type, final Value value) {
		if (null == type) {
			this.type = new ASN1_Integer_Type();
		} else {
			type.setOwnertype(TypeOwner_type.OT_EXC_SPEC, this);
			this.type = type;
		}
		this.value = value;

		this.type.setFullNameParent(this);
		this.value.setFullNameParent(this);
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		type.setMyScope(scope);
		value.setMyScope(scope);
	}

	public IASN1Type getType() {
		return type;
	}

	public IValue getValue() {
		return value;
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (type != null) {
			type.findReferences(referenceFinder, foundIdentifiers);
		}
		if (value != null) {
			value.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (type != null && !type.accept(v)) {
			return false;
		}
		if (value != null && !value.accept(v)) {
			return false;
		}
		return true;
	}
}
