/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.Object;

import java.text.MessageFormat;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IReferenceChainElement;
import org.eclipse.titan.designer.AST.ISetting;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.ASN1Object;
import org.eclipse.titan.designer.AST.ASN1.ObjectClass;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Class to represent a ReferencedObject. It is a DefinedObject or
 * ObjectFromObject.
 *
 * @author Kristof Szabados
 */
public final class ReferencedObject extends ASN1Object implements IReferenceChainElement {

	private static final String OBJECTEXPECTED = "Object reference expected";
	private static final String CIRCULAROBJECTREFERENCE = "Circular object reference chain: `{0}''";

	private final Reference reference;
	/** cache. */
	private ASN1Object objectReferenced;
	private Object_Definition referencedLast;

	public ReferencedObject(final Reference reference) {
		this.reference = reference;

		if (null != reference) {
			reference.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public ReferencedObject newInstance() {
		return new ReferencedObject(reference.newInstance());
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (null != reference) {
			reference.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		if (null != reference && null != reference.getLocation()) {
			return reference.getLocation();
		}

		return location;
	}

	@Override
	/** {@inheritDoc} */
	public String chainedDescription() {
		return "object reference: " + reference;
	}

	@Override
	/** {@inheritDoc} */
	public Location getChainLocation() {
		return getLocation();
	}

	/**
	 * Find and return the ASN.1 object referenced.
	 *
	 * @param timestamp
	 *                the timestamp of the actual build cycle
	 * @param refChain
	 *                the reference chain used to detect circular
	 *                references.
	 *
	 * @return the referenced ASN.1 object, or the actual type in case of an
	 *         error
	 * */
	public ASN1Object getRefd(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (reference == null) {
			return null;
		}

		if (referenceChain.add(this)) {
			if (objectReferenced != null && lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
				return objectReferenced;
			}
			final Assignment assignment = reference.getRefdAssignment(timestamp, true, referenceChain);
			if (null != assignment) {
				final ISetting setting = reference.getRefdSetting(timestamp);
				if (null != setting && !Setting_type.S_ERROR.equals(setting.getSettingtype())) {
					if (Setting_type.S_O.equals(setting.getSettingtype())) {
						objectReferenced = (ASN1Object) setting;
						return objectReferenced;
					}

					location.reportSemanticError(OBJECTEXPECTED);
				}
			}
		}

		objectReferenced = new Object_Definition(null);
		objectReferenced.setMyGovernor(myGovernor);
		return objectReferenced;
	}

	/**
	 * Returns the ASN.1 object referred last on the chain of references.
	 *
	 * @param timestamp
	 *                the time stamp of the actual semantic check cycle.
	 *
	 * @return the ASN.1 object referred last on the chain of references.
	 * */
	public Object_Definition getRefdLast(final CompilationTimeStamp timestamp) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(CIRCULAROBJECTREFERENCE, true);

		ASN1Object object = this;
		while (object instanceof ReferencedObject && !object.getIsErroneous(timestamp)) {
			object = ((ReferencedObject) object).getRefd(timestamp, referenceChain);
		}

		referenceChain.release();
		return (Object_Definition) object;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		if (null == myGovernor) {
			return;
		}

		final ObjectClass_Definition myClass = myGovernor.getRefdLast(timestamp, null);
		final Object_Definition refdLast = getRefdLast(timestamp, null);
		final ObjectClass refdLastGovernor = refdLast.getMyGovernor();
		final ObjectClass_Definition refdClass = refdLastGovernor.getRefdLast(timestamp, null);
		if (myClass != refdClass) {
			location.reportSemanticError(MessageFormat.format(Referenced_ObjectSet.MISMATCH, myClass.getFullName(),
					refdClass.getFullName()));
			objectReferenced = new Object_Definition(null);
			objectReferenced.setIsErroneous(true);
			objectReferenced.setMyGovernor(myGovernor);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Object_Definition getRefdLast(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		final boolean newChain = null == referenceChain;
		IReferenceChain temporalReferenceChain;
		if (newChain) {
			temporalReferenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		} else {
			temporalReferenceChain = referenceChain;
		}

		referencedLast = getRefd(timestamp, temporalReferenceChain).getRefdLast(timestamp, temporalReferenceChain);

		if (newChain) {
			temporalReferenceChain.release();
		}

		return referencedLast;
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int index) {
		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		if (null != referencedLast) {
			referencedLast.addProposal(propCollector, index);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int index) {
		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		if (null != referencedLast) {
			referencedLast.addDeclaration(declarationCollector, index);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean memberAccept(final ASTVisitor v) {
		if (reference != null && !reference.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData) {
		final Object_Definition last = getRefdLast(CompilationTimeStamp.getBaseTimestamp());
		if(myScope.getModuleScopeGen() == last.getMyScope().getModuleScopeGen()) {
			last.generateCode(aData);
		}
	}
}
