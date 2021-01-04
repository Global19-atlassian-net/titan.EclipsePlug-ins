/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
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
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectClass_refd;
import org.eclipse.titan.designer.AST.ASN1.Object.Object_Definition;
import org.eclipse.titan.designer.AST.ASN1.Object.ReferencedObject;
import org.eclipse.titan.designer.AST.ASN1.values.Undefined_Block_Value;
import org.eclipse.titan.designer.AST.TTCN3.types.Referenced_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.Undefined_LowerIdentifier_Value;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * An undefined assignment.
 * <p>
 * Can only be Object or Value assignment because of the syntax
 *
 * @author Kristof Szabados
 * */
public final class Undefined_Assignment_O_or_V extends Undefined_Assignment {

	private final Reference reference;
	private final Block mBlock;
	private final Reference objectReference;

	public Undefined_Assignment_O_or_V(final Identifier id, final Ass_pard assPard, final Reference reference, final Block aBlock) {
		super(id, assPard);
		this.reference = reference;
		this.mBlock = aBlock;
		objectReference = null;

		if (null != reference) {
			reference.setFullNameParent(this);
		}
		if (null != aBlock) {
			aBlock.setFullNameParent(this);
		}
	}

	public Undefined_Assignment_O_or_V(final Identifier id, final Ass_pard assPard, final Reference reference, final Reference reference2) {
		super(id, assPard);
		this.reference = reference;
		mBlock = null;
		objectReference = reference2;

		if (null != reference) {
			reference.setFullNameParent(this);
		}
		if (null != objectReference) {
			objectReference.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (null != reference) {
			reference.setMyScope(scope);
		}
		if (null != objectReference) {
			objectReference.setMyScope(scope);
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
			if (null != reference && !reference.refersToSettingType(timestamp, Setting_type.S_ERROR, temporalReferenceChain)) {
				reference.setMyScope(myScope);
				if (null != objectReference) {
					objectReference.setMyScope(rightScope);
				}

				if (identifier.isvalidAsnObjectReference()
						&& reference.refersToSettingType(timestamp, Setting_type.S_OC, temporalReferenceChain)) {
					if (oldRealAssignment != null && oldRealAssignment.getAssignmentType() == Assignment_type.A_OBJECT) {
						//did not change since the last time.
						realAssignment = oldRealAssignment;
					} else {
						final ObjectClass_refd oc = new ObjectClass_refd(reference);
						oc.setLocation(reference.getLocation());
						if (null != mBlock) {
							final Object_Definition obj = new Object_Definition(mBlock);
							// obj.setLocation(right1);
							realAssignment = new Object_Assignment(identifier, assPard, oc, obj);
						} else if (null != objectReference) {
							final ReferencedObject obj = new ReferencedObject(objectReference);
							obj.setLocation(objectReference.getLocation());
							realAssignment = new Object_Assignment(identifier, assPard, oc, obj);
						}
					}
				} else if (identifier.isvalidAsnValueReference()
						&& (reference.refersToSettingType(timestamp, Setting_type.S_T, temporalReferenceChain) || reference
								.refersToSettingType(timestamp, Setting_type.S_VS, temporalReferenceChain))) {
					if (oldRealAssignment != null && oldRealAssignment.getAssignmentType() == Assignment_type.A_CONST) {
						//did not change since the last time.
						realAssignment = oldRealAssignment;
					} else {
						final Referenced_Type type = new Referenced_Type(reference);
						if (null != mBlock) {
							final Value value = new Undefined_Block_Value(mBlock);
							value.setLocation(mBlock.getLocation());
							realAssignment = new Value_Assignment(identifier, assPard, type, value);
						} else if (null != objectReference) {
							final Value value = new Undefined_LowerIdentifier_Value(objectReference.getId().newInstance());
							value.setLocation(objectReference.getLocation());
							realAssignment = new Value_Assignment(identifier, assPard, type, value);
						}
					}
				}
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
		if (null != mBlock) {
			return new Undefined_Assignment_O_or_V(identifier, null, reference.newInstance(), mBlock);
		}

		return new Undefined_Assignment_O_or_V(identifier, null, reference.newInstance(), objectReference.newInstance());
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
		if (objectReference != null && !objectReference.accept(v)) {
			return false;
		}
		return true;
	}
}
