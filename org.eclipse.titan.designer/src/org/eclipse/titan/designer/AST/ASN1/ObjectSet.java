/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import org.eclipse.titan.designer.AST.GovernedSet;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectSetElementVisitor_objectCollector;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectSet_definition;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Class to represent ObjectSet.
 *
 * @author Kristof Szabados
 */
public abstract class ObjectSet extends GovernedSet {
	protected ObjectClass myGovernor;

	@Override
	/** {@inheritDoc} */
	public final Setting_type getSettingtype() {
		return Setting_type.S_OS;
	}

	public abstract ObjectSet newInstance();

	@Override
	/** {@inheritDoc} */
	public final ObjectClass getMyGovernor() {
		return myGovernor;
	}

	/**
	 * Sets the ObjectClass of the object set.
	 *
	 * @param governor
	 *                the object class to set as governor.
	 * */
	public final void setMyGovernor(final ObjectClass governor) {
		myGovernor = governor;
	}

	public abstract ObjectSet_definition getRefdLast(final CompilationTimeStamp timestamp, IReferenceChain referenceChain);

	public abstract int getNofObjects();

	public abstract ASN1Object getObjectByIndex(int index);

	public abstract void accept(ObjectSetElementVisitor_objectCollector v);

	public abstract void check(final CompilationTimeStamp timestamp);

	/**
	 * Adds the object set to the list completion proposals, with some
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
	public abstract void addProposal(ProposalCollector propCollector, int index);

	/**
	 * Adds the object set to the list declaration proposals.
	 *
	 * @param declarationCollector
	 *                the declaration collector.
	 * @param index
	 *                the index of a part of the full reference, for which
	 *                we wish to find the object set, or a object set which
	 *                might point us a step forward to the declaration.
	 * */
	public abstract void addDeclaration(DeclarationCollector declarationCollector, int index);

	/**
	 * Generate Java code for this object set.
	 *
	 * generate_code in the compiler
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 */
	public abstract void generateCode( final JavaGenData aData);
}
