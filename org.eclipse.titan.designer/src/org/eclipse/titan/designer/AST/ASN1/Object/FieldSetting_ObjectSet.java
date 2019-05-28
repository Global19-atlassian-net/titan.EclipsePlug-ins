/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.Object;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.ASN1.ObjectClass;
import org.eclipse.titan.designer.AST.ASN1.ObjectSet;
import org.eclipse.titan.designer.AST.ASN1.Object.FieldSpecification.Fieldspecification_types;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Class to represent ObjectSet FieldSettings.
 *
 * @author Kristof Szabados
 */
public final class FieldSetting_ObjectSet extends FieldSetting {

	private static final String OBJECTSETEXPECTED = "ObjectSet setting was expected";

	private ObjectSet objectSet;

	public FieldSetting_ObjectSet(final Identifier identifier, final ObjectSet objectSet) {
		super(identifier);
		this.objectSet = objectSet;

		if (null != objectSet) {
			objectSet.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public FieldSetting_ObjectSet newInstance() {
		return new FieldSetting_ObjectSet(name.newInstance(), objectSet.newInstance());
	}

	@Override
	/** {@inheritDoc} */
	public ObjectSet getSetting() {
		return objectSet;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final FieldSpecification fieldSpecification) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		if (!Fieldspecification_types.FS_OS.equals(fieldSpecification.getFieldSpecificationType())) {
			location.reportSemanticError(OBJECTSETEXPECTED);
			objectSet = new ObjectSet_definition();
			objectSet.setFullNameParent(this);
			objectSet.setLocation(fieldSpecification.getLocation());
		}

		final ObjectSet_FieldSpecification fs = (ObjectSet_FieldSpecification) fieldSpecification;
		final ObjectClass oc = fs.getObjectClass();
		objectSet.setMyGovernor(oc);

		lastTimeChecked = timestamp;

		objectSet.check(timestamp);
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		if (null != objectSet) {
			objectSet.addDeclaration(declarationCollector, i);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int i) {
		if (null != objectSet) {
			objectSet.addProposal(propCollector, i);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (name != null && !name.accept(v)) {
			return false;
		}
		if (objectSet != null && !objectSet.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData) {
		objectSet.generateCode(aData);
	}
}
