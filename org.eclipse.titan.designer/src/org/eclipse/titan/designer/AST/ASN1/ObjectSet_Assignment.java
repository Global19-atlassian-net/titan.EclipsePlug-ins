/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

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
 * ObjectSet assignment.
 *
 * @author Kristof Szabados
 */
public final class ObjectSet_Assignment extends ASN1Assignment {
	public static final String PARAMETERISEDOBJECTSET = "`{0}'' is a parameterized objectset assignment";

	/** left. */
	private final ObjectClass objectClass;
	/** right. */
	private final ObjectSet objectSet;

	public ObjectSet_Assignment(final Identifier id, final Ass_pard assPard, final ObjectClass objectClass, final ObjectSet objectSet) {
		super(id, assPard);
		this.objectClass = objectClass;
		this.objectSet = objectSet;

		if (null != objectClass) {
			objectClass.setFullNameParent(this);
		}
		if (null != objectSet) {
			objectSet.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment_type getAssignmentType() {
		return Assignment_type.A_OS;
	}

	@Override
	/** {@inheritDoc} */
	protected ASN1Assignment internalNewInstance(final Identifier identifier) {
		return new ObjectSet_Assignment(identifier, null, objectClass.newInstance(), objectSet.newInstance());
	}

	@Override
	/** {@inheritDoc} */
	public void setRightScope(final Scope rightScope) {
		if (null != objectSet) {
			objectSet.setMyScope(rightScope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (null != objectClass) {
			objectClass.setMyScope(scope);
		}
		if (null != objectSet) {
			objectSet.setMyScope(scope);
		}
	}

	/**
	 * Checks the object set.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 *
	 * @return the object set of this object set assignment,
	 * */
	public ObjectSet getObjectSet(final CompilationTimeStamp timestamp) {
		if (null != assPard) {
			location.reportSemanticError(MessageFormat.format(PARAMETERISEDOBJECTSET, getFullName()));
			return null;
		}

		check(timestamp);

		return objectSet;
	}

	@Override
	/** {@inheritDoc} */
	public ObjectSet getSetting(final CompilationTimeStamp timestamp) {
		return getObjectSet(timestamp);
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
			// lastTimeChecked = timestamp;;
			return;
		}

		if (null != objectClass) {
			objectClass.check(timestamp);
		}

		if (null != objectSet) {
			objectSet.setMyGovernor(objectClass);
			objectSet.setGenName(getGenName());
			objectSet.check(timestamp);
		}

		// lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int index) {
		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() > index && identifier.getName().equals(subrefs.get(index).getId().getName())) {
			if (subrefs.size() > index + 1 && null != objectSet) {
				objectSet.addDeclaration(declarationCollector, index + 1);
			} else if (subrefs.size() == index + 1 && Subreference_type.fieldSubReference.equals(subrefs.get(index).getReferenceType())) {
				declarationCollector.addDeclaration(this);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int index) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= index) {
			return;
		}

		if (subrefs.size() == index + 1 && identifier.getName().toLowerCase(Locale.ENGLISH).startsWith(subrefs.get(index).getId().getName().toLowerCase(Locale.ENGLISH))) {
			propCollector.addProposal(identifier, " - " + "ObjectSet assignment", ImageCache.getImage(getOutlineIcon()),
					"ObjectSet assignment");
		} else if (subrefs.size() > index + 1 && null != objectSet && identifier.getName().equals(subrefs.get(index).getId().getName())) {
			// perfect match
			objectSet.addProposal(propCollector, index + 1);
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getAssignmentName() {
		return "information object set";
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
		if (objectSet != null) {
			objectSet.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}

		if (assPard != null) {
			// if parameterised the rest was not checked.
			return true;
		}

		if (objectClass != null && !objectClass.accept(v)) {
			return false;
		}
		if (objectSet != null && !objectSet.accept(v)) {
			return false;
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final boolean cleanUp ) {
		if (assPard != null || dontGenerate) {
			return;
		}

		objectClass.generateCode(aData);
		objectSet.generateCode(aData);
	}
}
