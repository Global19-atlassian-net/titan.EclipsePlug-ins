/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import org.eclipse.titan.designer.AST.Governed;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.Object.Object_Definition;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Class to represent an ASN.1 Object.
 *
 * @author Kristof Szabados
 */
public abstract class ASN1Object extends Governed implements IObjectSet_Element {

	protected ObjectClass myGovernor;

	@Override
	/** {@inheritDoc} */
	public final Setting_type getSettingtype() {
		return Setting_type.S_O;
	}

	/** @return a new instance of this AST node */
	public abstract ASN1Object newInstance();

	@Override
	/** {@inheritDoc} */
	public final IObjectSet_Element newOseInstance() {
		return newInstance();
	}

	@Override
	/** {@inheritDoc} */
	public final ObjectClass getMyGovernor() {
		return myGovernor;
	}

	public abstract Object_Definition getRefdLast(final CompilationTimeStamp timestamp, IReferenceChain referenceChain);

	/**
	 * Sets the governing ObjectClass of the Object.
	 *
	 * @param governor
	 *                the governor of the Object.
	 * */
	public final void setMyGovernor(final ObjectClass governor) {
		myGovernor = governor;
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNameOse(final String prefix, final String suffix) {
		setGenName(prefix, suffix);
	}

	/**
	 * @return the number of elements.
	 * */
	public final int getNofElems() {
		return 1;
	}

	@Override
	/** {@inheritDoc} */
	public final void accept(final ObjectSetElement_Visitor visitor) {
		visitor.visitObject(this);
	}

	@Override
	/** {@inheritDoc} */
	public final void setMyScopeOse(final Scope scope) {
		setMyScope(scope);
	}

	/**
	 * Does the semantic checking of the ASN.1 Object.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle.
	 * */
	public abstract void check(final CompilationTimeStamp timestamp);

	/**
	 * Adds the object to the list completion proposals, with some
	 * description.
	 * <p>
	 * Extending class only need to implement their
	 * {@link #getProposalKind()} function
	 *
	 * @param propCollector
	 *                the proposal collector.
	 * @param index
	 *                the index of a part of the full reference, for which
	 *                we wish to find completions.
	 * */
	public abstract void addProposal(final ProposalCollector propCollector, final int index);

	/**
	 * Adds the object to the list declaration proposals.
	 *
	 * @param declarationCollector
	 *                the declaration collector.
	 * @param index
	 *                the index of a part of the full reference, for which
	 *                we wish to find the object, or an object which might
	 *                point us a step forward to the declaration.
	 * */
	public abstract void addDeclaration(final DeclarationCollector declarationCollector, final int index);

	/**
	 * Generate Java code for this object.
	 *
	 * generate_code in the compiler
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 */
	public abstract void generateCode( final JavaGenData aData);
}
