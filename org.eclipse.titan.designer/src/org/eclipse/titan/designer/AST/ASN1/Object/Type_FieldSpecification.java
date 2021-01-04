/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.Object;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ISetting;
import org.eclipse.titan.designer.AST.IType.TypeOwner_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Class to represent a TypeFieldSpec.
 *
 * @author Kristof Szabados
 */
public final class Type_FieldSpecification extends FieldSpecification {

	private final Type definedType;

	public Type_FieldSpecification(final Identifier identifier, final boolean isOptional, final Type definedType) {
		super(identifier, isOptional);
		this.definedType = definedType;

		if (null != definedType) {
			definedType.setOwnertype(TypeOwner_type.OT_TYPE_FLD, this);
			definedType.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Fieldspecification_types getFieldSpecificationType() {
		return Fieldspecification_types.FS_T;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyObjectClass(final ObjectClass_Definition objectClass) {
		super.setMyObjectClass(objectClass);
		if (null != definedType) {
			definedType.setMyScope(myObjectClass.getMyScope());
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasDefault() {
		return null != definedType;
	}

	@Override
	/** {@inheritDoc} */
	public ISetting getDefault() {
		return definedType;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		if (null != definedType) {
			definedType.setGenName(myObjectClass.getGenNameOwn(), identifier.getName());
			definedType.check(timestamp);
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		if (null != definedType) {
			definedType.addDeclaration(declarationCollector, i);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int i) {
		if (null != definedType) {
			definedType.addProposal(propCollector, i);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (identifier != null && !identifier.accept(v)) {
			return false;
		}
		if (definedType != null && !definedType.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData) {
		if (definedType != null) {
			final String genName = definedType.getGenNameOwn();
			final StringBuilder sb = aData.getCodeForType(genName);
			final StringBuilder source = new StringBuilder();

			definedType.generateCode( aData, source );
			sb.append(source);
		}
	}
}
