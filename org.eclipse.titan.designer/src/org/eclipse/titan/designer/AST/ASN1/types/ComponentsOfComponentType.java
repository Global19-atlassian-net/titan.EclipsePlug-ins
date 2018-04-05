/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.types;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.TypeOwner_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.ASN1Type;
import org.eclipse.titan.designer.AST.TTCN3.types.CompField;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * ComponentsOf.
 * <p>
 * originally CT_CompsOf
 *
 * @author Kristof Szabados
 */
public final class ComponentsOfComponentType extends ComponentType {
	private static final String FULLNAMEPART = ".<ComponentsOfType>";

	private static final String SEQUENCEEXPECTED = "COMPONENTS OF in a SEQUENCE type shall refer to another SEQUENCE type instead of `{0}''";
	private static final String SETEXPECTED = "COMPONENTS OF in a SET type shall refer to another SET type instead of `{0}''";

	private final ASN1Type componentsOfType;
	private CompilationTimeStamp trCompsofTimestamp;
	// cache
	private ComponentTypeList componentTypes;

	public ComponentsOfComponentType(final ASN1Type componentsOfType) {
		this.componentsOfType = componentsOfType;

		if (null != componentsOfType) {
			componentsOfType.setOwnertype(TypeOwner_type.OT_COMPS_OF, this);
			componentsOfType.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (componentsOfType == child) {
			return builder.append(FULLNAMEPART);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (null != componentsOfType) {
			componentsOfType.setMyScope(scope);
		}
		if (null != componentTypes) {
			componentTypes.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public int getNofComps() {
		if (null != componentTypes) {
			return componentTypes.getNofComps();
		}
		return 0;
	}

	@Override
	/** {@inheritDoc} */
	public CompField getCompByIndex(final int index) {
		if (null != componentTypes) {
			return componentTypes.getCompByIndex(index);
		}

		// FATAL_ERROR
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasCompWithName(final Identifier identifier) {
		if (null != componentTypes) {
			return componentTypes.hasCompWithName(identifier);
		}

		// FATAL_ERROR
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public CompField getCompByName(final Identifier identifier) {
		if (null != componentTypes) {
			return componentTypes.getCompByName(identifier);
		}

		// FATAL_ERROR
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public void trCompsof(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain, final boolean isSet) {
		if (null != trCompsofTimestamp && !trCompsofTimestamp.isLess(timestamp)) {
			return;
		}

		if (null == componentsOfType) {
			return;
		}

		final IType type = componentsOfType.getTypeRefdLast(timestamp);

		if (type.getIsErroneous(timestamp)) {
			return;
		}

		CTs_EE_CTs tempCtss;
		switch (type.getTypetype()) {
		case TYPE_ASN1_SET:
			if (!isSet) {
				getLocation().reportSemanticError(MessageFormat.format(SEQUENCEEXPECTED, type.getFullName()));

				trCompsofTimestamp = timestamp;
				return;
			}

			referenceChain.markState();
			if (referenceChain.add(componentsOfType)) {
				((ASN1_Set_Type) type).trCompsof(timestamp, referenceChain);
			}
			referenceChain.previousState();

			tempCtss = ((ASN1_Set_Type) type).components;
			break;
		case TYPE_ASN1_SEQUENCE:
			if (isSet) {
				getLocation().reportSemanticError(MessageFormat.format(SETEXPECTED, type.getFullName()));

				trCompsofTimestamp = timestamp;
				return;
			}

			referenceChain.markState();
			if (referenceChain.add(componentsOfType)) {
				((ASN1_Sequence_Type) type).trCompsof(timestamp, referenceChain);
			}
			referenceChain.previousState();

			tempCtss = ((ASN1_Sequence_Type) type).components;
			break;
		default:
			trCompsofTimestamp = timestamp;
			return;
		}

		// emergency exit for the case of infinite recursion
		if (timestamp.equals(trCompsofTimestamp)) {
			return;
		}

		componentTypes = new ComponentTypeList();
		type.check(timestamp);
		for (int i = 0; i < tempCtss.getNofRootComps(); i++) {
			final CompField compfield = tempCtss.getRootCompByIndex(i).newInstance();
			compfield.setLocation(location);
			final RegularComponentType componentType = new RegularComponentType(compfield);
			componentType.setLocation(location);
			componentTypes.addComponentType(componentType);
		}
		componentTypes.setMyScope(componentsOfType.getMyScope());
		componentTypes.setFullNameParent(this);

		trCompsofTimestamp = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (componentsOfType != null) {
			componentsOfType.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (componentsOfType != null && !componentsOfType.accept(v)) {
			return false;
		}
		return true;
	}
}
