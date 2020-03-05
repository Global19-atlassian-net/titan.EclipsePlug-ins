/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.Object;

import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Error_Setting;
import org.eclipse.titan.designer.AST.ISetting;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * @author Kristof Szabados
 * */
public final class Erroneous_FieldSpecification extends FieldSpecification {

	private ISetting settingError;
	private final boolean hasDefaultFlag;

	public Erroneous_FieldSpecification(final Identifier identifier, final boolean isOptional, final boolean hasDefault) {
		super(identifier, isOptional);
		hasDefaultFlag = hasDefault;
	}

	@Override
	/** {@inheritDoc} */
	public Fieldspecification_types getFieldSpecificationType() {
		return Fieldspecification_types.FS_ERROR;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasDefault() {
		return hasDefaultFlag;
	}

	@Override
	/** {@inheritDoc} */
	public ISetting getDefault() {
		if (null == settingError && hasDefaultFlag) {
			settingError = new Error_Setting();
		}
		return settingError;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		//Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		final List<ISubReference> subreferences = declarationCollector.getReference().getSubreferences();
		if (subreferences.size() <= i) {
			return;
		}

		final ISubReference subreference = subreferences.get(i);
		if (Subreference_type.fieldSubReference.equals(subreference.getReferenceType())) {
			if (subreferences.size() == i + 1) {
				declarationCollector.addDeclaration(identifier.getDisplayName(), getLocation(), this);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int i) {
		final List<ISubReference> subreferences = propCollector.getReference().getSubreferences();
		if (subreferences.size() <= i) {
			return;
		}

		final ISubReference subreference = subreferences.get(i);
		if (Subreference_type.fieldSubReference.equals(subreference.getReferenceType())) {
			if (subreferences.size() == i + 1) {
				propCollector.addProposal(identifier, " - unknown fieldspeciication", null, "unknown fieldspeciication");
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (identifier != null && !identifier.accept(v)) {
			return false;
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData) {
		//there is intentionally no code generated for erroneous field specifications.
	}
}
