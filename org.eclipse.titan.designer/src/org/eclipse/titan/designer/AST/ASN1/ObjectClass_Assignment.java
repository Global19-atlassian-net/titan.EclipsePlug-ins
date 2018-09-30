/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.graphics.ImageCache;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * ObjectClass assignment.
 *
 * @author Kristof Szabados
 */
public final class ObjectClass_Assignment extends ASN1Assignment {
	public static final String PARAMETERISEDOBJECTCLASS = "`{0}'' is a parameterized objectclass assignment";

	/** right. */
	private final ObjectClass objectClass;

	public ObjectClass_Assignment(final Identifier id, final Ass_pard assPard, final ObjectClass objectClass) {
		super(id, assPard);
		this.objectClass = objectClass;

		if (null != objectClass) {
			objectClass.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment_type getAssignmentType() {
		return Assignment_type.A_OC;
	}

	@Override
	protected ASN1Assignment internalNewInstance(final Identifier identifier) {
		return new ObjectClass_Assignment(identifier, null, objectClass.newInstance());
	}

	@Override
	/** {@inheritDoc} */
	public void setRightScope(final Scope rightScope) {
		if (null != objectClass) {
			objectClass.setMyScope(rightScope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (null != objectClass) {
			objectClass.setMyScope(scope);
		}
	}

	/**
	 * Checks the object class and returns it.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 *
	 * @return the ObjectClass of this ObjectClass assignment
	 */
	public ObjectClass getObjectClass(final CompilationTimeStamp timestamp) {
		if (null != assPard) {
			location.reportSemanticError(MessageFormat.format(PARAMETERISEDOBJECTCLASS, getFullName()));
			return null;
		}

		check(timestamp);

		return objectClass;
	}

	@Override
	/** {@inheritDoc} */
	public ObjectClass getSetting(final CompilationTimeStamp timestamp) {
		return getObjectClass(timestamp);
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		check(timestamp, null);
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final IReferenceChain refChain) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		if (null != assPard) {
			assPard.check(timestamp);
			// lastTimeChecked = timestamp;
			return;
		}

		if (null != objectClass) {
			objectClass.setGenName(getGenName());
			objectClass.check(timestamp);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() >= i + 1 && identifier.getName().equals(subrefs.get(i).getId().getName())) {
			if (subrefs.size() > i + 1 && null != objectClass) {
				objectClass.addDeclaration(declarationCollector, i + 1);
			} else if (subrefs.size() == i + 1 && Subreference_type.fieldSubReference.equals(subrefs.get(i).getReferenceType())) {
				declarationCollector.addDeclaration(this);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int i) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= i) {
			return;
		}

		if (subrefs.size() == i + 1 && identifier.getName().toLowerCase().startsWith(subrefs.get(i).getId().getName().toLowerCase())) {
			propCollector.addProposal(identifier, " - " + "ObjectClass assignment", ImageCache.getImage(getOutlineIcon()),
					"ObjectClass assignment");
		} else if (subrefs.size() > i + 1 && null != objectClass && identifier.getName().equals(subrefs.get(i).getId().getName())) {
			// perfect match
			objectClass.addProposal(propCollector, i + 1);
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getAssignmentName() {
		return "information object class";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "titan.gif";
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		// TODO
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (objectClass != null && !objectClass.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final boolean cleanUp ) {
		if (assPard != null) {
			return;
		}

		objectClass.generateCode(aData);
	}
}
