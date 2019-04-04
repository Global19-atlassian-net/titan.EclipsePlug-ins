/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISetting.Setting_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectClass_refd;
import org.eclipse.titan.designer.AST.TTCN3.types.Referenced_Type;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * An undefined assignment.
 * <p>
 * Can only be Type or ObjectClass assignment because of the syntax
 *
 * @author Kristof Szabados
 * */
public final class Undefined_Assignment_T_or_OC extends Undefined_Assignment {

	private final Reference reference;

	public Undefined_Assignment_T_or_OC(final Identifier id, final Ass_pard assPard, final Reference reference) {
		super(id, assPard);
		this.reference = reference;

		if (null != reference) {
			reference.setFullNameParent(this);
		}
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
	protected void classifyAssignment(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		final boolean newChain = null == referenceChain;
		IReferenceChain temporalReferenceChain;
		if (newChain) {
			temporalReferenceChain = ReferenceChain.getInstance(CIRCULARASSIGNMENTCHAIN, true);
		} else {
			temporalReferenceChain = referenceChain;
			temporalReferenceChain.markState();
		}

		final ASN1Assignment oldRealAssignment = realAssignment;
		realAssignment = null;

		if (temporalReferenceChain.add(this)) {
			reference.setMyScope(rightScope);
			if (identifier.isvalidAsnObjectClassReference()
					&& reference.refersToSettingType(timestamp, Setting_type.S_OC, temporalReferenceChain)) {
				if (oldRealAssignment != null && oldRealAssignment.getAssignmentType() == Assignment_type.A_OC) {
					//did not change since the last time.
					realAssignment = oldRealAssignment;
				} else {
					final ObjectClass_refd oc = new ObjectClass_refd(reference);
					oc.setLocation(reference.getLocation());
					realAssignment = new ObjectClass_Assignment(identifier, assPard, oc);
				}
				// assPard = null;
				// asstype = Assignment.A_OC;
			} else if (identifier.isvalidAsnTyperef()
					&& (reference.refersToSettingType(timestamp, Setting_type.S_T, temporalReferenceChain) || reference
							.refersToSettingType(timestamp, Setting_type.S_VS, temporalReferenceChain))) {
				if (oldRealAssignment != null && oldRealAssignment.getAssignmentType() == Assignment_type.A_TYPE) {
					//did not change since the last time.
					realAssignment = oldRealAssignment;
				} else {
					final Referenced_Type type = new Referenced_Type(reference);
					type.setLocation(reference.getLocation());

					realAssignment = new Type_Assignment(identifier, assPard, type);
				}
				// assPard = null;
				// asstype = A_TYPE;
			}
		}

		if (null == realAssignment) {
			location.reportSemanticError(UNRECOGNISABLEASSIGNMENT);
			isErroneous = true;
		} else if (oldRealAssignment != realAssignment) {
			realAssignment.setLocation(location);
			realAssignment.setMyScope(myScope);
			realAssignment.setRightScope(rightScope);
			realAssignment.setFullNameParent(this);
		}

		if (newChain) {
			temporalReferenceChain.release();
		} else {
			temporalReferenceChain.previousState();
		}
	}

	@Override
	protected ASN1Assignment internalNewInstance(final Identifier identifier) {
		return new Undefined_Assignment_T_or_OC(identifier, null, reference.newInstance());
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

		if (realAssignment != null) {
			return realAssignment.accept(v);
		}

		if (reference != null && !reference.accept(v)) {
			return false;
		}

		return true;
	}
}
