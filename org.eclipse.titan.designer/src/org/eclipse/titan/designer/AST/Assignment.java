/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASN1.definitions.SpecialASN1Module;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter.parameterEvaluationType;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Group;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.declarationsearch.Declaration;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * The assignment class represents TTCN3 or ASN.1 assignments.
 * <p>
 * All TTCN3 definitions extend this class.
 *
 * @author Kristof Szabados
 * */
public abstract class Assignment extends ASTNode implements IOutlineElement, ILocateableNode, IReferenceChainElement, IReferencingElement {
	protected static final String GLOBALLY_UNUSED = "The {0} seems to be never used globally";
	protected static final String LOCALLY_UNUSED = "The {0} seems to be never used locally";

	public enum Assignment_type {
		/** type assignment. */																A_TYPE,

		/** altstep assignment (TTCN-3). */											A_ALTSTEP,
		/** constant assignment (TTCN-3). */										A_CONST,
		/**  external constant (TTCN-3). */											A_EXT_CONST,
		/** function without return statement (TTCN-3).*/					A_FUNCTION ,
		/** function returning a value (TTCN-3). */								A_FUNCTION_RVAL,
		/** function returning a template (TTCN-3). */							A_FUNCTION_RTEMP,
		/** external function without return statement  (TTCN-3). */	A_EXT_FUNCTION,
		/** external function returning a value (TTCN-3). */					A_EXT_FUNCTION_RVAL,
		/** external function returning a template (TTCN-3). */			A_EXT_FUNCTION_RTEMP,
		/** a module parameter (TTCN-3). */											A_MODULEPAR,
		/** a template module parameter (TTCN-3). */								A_MODULEPAR_TEMPLATE,
		/** port (TTCN-3). */																	A_PORT ,
		/** variable  (TTCN-3). */															A_VAR,
		/** template  (TTCN-3). */															A_TEMPLATE,
		/** template variable, dynamic template (TTCN-3). */				A_VAR_TEMPLATE,
		/** timer  (TTCN-3). */																A_TIMER,
		/** testcase  (TTCN-3). */															A_TESTCASE,
		/** formal parameter (value) (TTCN3). */									A_PAR_VAL,
		/** formal parameter (in value) (TTCN3). */								A_PAR_VAL_IN,
		/** formal parameter (out value) (TTCN3). */							A_PAR_VAL_OUT,
		/** formal parameter (inout value) (TTCN3). */							A_PAR_VAL_INOUT,
		/** formal parameter (in template) (TTCN3). */							A_PAR_TEMP_IN,
		/** formal parameter (out template) (TTCN3). */						A_PAR_TEMP_OUT,
		/** formal parameter (inout template) (TTCN3). */					A_PAR_TEMP_INOUT,
		/** formal parameter (timer) (TTCN3). */									A_PAR_TIMER,
		/** formal parameter (port) (TTCN3). */									A_PAR_PORT,

		/**< undefined/undecided (ASN.1). */			A_UNDEF,
		/**< value set (ASN.1). */								A_VS,
		/**< information object class (ASN.1). */		A_OC,
		/**< information object (ASN.1). */				A_OBJECT,
		/**< information object set (ASN.1). */			A_OS;

		public final boolean semanticallyEquals(final Assignment_type other) {
			if(this == A_PAR_VAL || this == A_PAR_VAL_IN) {
				return other == A_PAR_VAL || other == A_PAR_VAL_IN;
			}

			return this == other;
		}
	}

	/** The identifier of the assignment. */
	protected Identifier identifier;

	/**
	 * The location of the whole assignment.
	 * This location encloses the assignment fully, as it is used to report errors to.
	 **/
	protected Location location;

	/** The enclosing group of the whole assignment. */
	protected Group parentGroup;

	/** the time when this assignment was checked the last time. */
	protected CompilationTimeStamp lastTimeChecked;

	protected boolean isErroneous;

	/** Stores whether this assignment was found to be used in this semantic check cycle. */
	//TODO this should be removed and the same functionality implemented in Titanium as part of already existing codesmells.
	@Deprecated
	protected boolean isUsed;

	/** used by the incremental processing to signal if the assignment can eb the root of a change */
	private boolean canBeCheckRoot = true;

	public Assignment(final Identifier identifier) {
		this.identifier = identifier;
		isErroneous = false;
		isUsed = false;
		location = NULL_Location.INSTANCE;
	}

	public final CompilationTimeStamp getLastTimeChecked() {
		return lastTimeChecked;
	}

	/**
	 * returns true if the assignment is the root of a change.
	 * */
	public final boolean isCheckRoot() {
		return canBeCheckRoot;
	}

	/**
	 * Signals that the assignment can serve as a change root for the incremental analysis.
	 * */
	public final void checkRoot() {
		canBeCheckRoot = true;
	}

	/**
	 * Signals that the assignment can not serve as a change root for the incremental analysis.
	 * */
	public final void notCheckRoot() {
		canBeCheckRoot = false;
	}

	public final boolean getIsErroneous() {
		return isErroneous;
	}

	/**
	 * Returns a string containing the Java reference pointing to this assignment.
	 * */
	public abstract String getGenName();

	@Override
	/** {@inheritDoc} */
	public final void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public final Location getLocation() {
		return location;
	}

	@Override
	/** {@inheritDoc} */
	public final Location getChainLocation() {
		return location;
	}

	@Override
	/** {@inheritDoc} */
	public final String chainedDescription() {
		return getFullName();
	}

	public final void setUsed() {
		isUsed = true;
	}

	public boolean isUsed() {
		return isUsed;
	}

	/** @return whether the assignment is local or not */
	public boolean isLocal() {
		return false;
	}

	/**
	 * Sets the parent group of the assignment.
	 *
	 * @param parentGroup the group to be set as the parent.
	 * */
	public final void setParentGroup(final Group parentGroup) {
		this.parentGroup = parentGroup;
	}

	/** @return the parent group of the assignment. */
	public final Group getParentGroup() {
		return parentGroup;
	}

	/**
	 * @return the kind of the assignment.
	 * */
	public abstract Assignment_type getAssignmentType();

	/**
	 * @return the identifier of the assignment.
	 * */
	@Override
	public final Identifier getIdentifier() {
		return identifier;
	}

	@Override
	/** {@inheritDoc} */
	public Object[] getOutlineChildren() {
		return new Object[]{};
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineText() {
		return "";
	}

	@Override
	/** {@inheritDoc} */
	public int category() {
		return getAssignmentType().ordinal() * Type.Type_type.values().length;
	}

	/**
	 * @return the name of the assignment
	 * */
	public abstract String getAssignmentName();

	/**
	 * @see DeclarationCollector#addDeclaration(org.eclipse.titan.designer.AST.TTCN3.definitions.Definition)
	 * @return a short description of the assignment, used by the declaration collector.
	 * */
	public String getProposalDescription() {
		return "basic assignment";
	}

	/**
	 * Returns a short description of the assignment to be used in error reports.
	 *
	 * @return a short description of the assignment, used by error reports.
	 * */
	public String getDescription() {
		final StringBuilder builder = new StringBuilder(getAssignmentName());
		builder.append(" `").append(getFullName()).append('\'');
		return builder.toString();
	}

	/**
	 * Calculates the setting of this assignment.
	 *
	 * @param timestamp the time stamp of the actual semantic check cycle.
	 *
	 * @return the setting of this assignment
	 * */
	public ISetting getSetting(final CompilationTimeStamp timestamp) {
		return null;
	}

	/**
	 * Calculates the type of this assignment.
	 *
	 * @param timestamp the time stamp of the actual semantic check cycle.
	 *
	 *  @return the type of the assignment if it has one, otherwise null
	 * */
	public IType getType(final CompilationTimeStamp timestamp) {
		return null;
	}

	/**
	 * Does the semantic checking of the assignment.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle.
	 * */
	public abstract void check(final CompilationTimeStamp timestamp);

	/**
	 * Does the semantic checking of the assignment.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle.
	 * @param refChain the reference chain to detect circular references.
	 * */
	public abstract void check(final CompilationTimeStamp timestamp, final IReferenceChain refChain);

	/** Checks the properties of the assignment,
	 * that can only be checked after the semantic check was completely run. */
	public void postCheck() {
	}

	/**
	 * Adds the assignment to the list completion proposals, with some
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
	 * Adds the assignment to the list declaration proposals.
	 *
	 * @param declarationCollector
	 *                the declaration collector.
	 * @param index
	 *                the index of a part of the full reference, for which
	 *                we wish to find the assignment, or a assignment which
	 *                might point us a step forward to the declaration.
	 * */
	public abstract void addDeclaration(final DeclarationCollector declarationCollector, final int index);

	/**
	 * Returns true if the assignment should be marked according to the
	 * preference options.
	 *
	 * @return true if the highlighting of this assignment is turned on
	 */
	public abstract boolean shouldMarkOccurrences();

	@Override
	/** {@inheritDoc} */
	public Declaration getDeclaration() {
		return Declaration.createInstance(this);
	}

	/**
	 * Returns a string containing the Java reference pointing to this
	 * definition from the Java equivalent of scope \a p_scope.
	 * The reference is a simple identifier qualified with a namespace when necessary.
	 * If \a p_prefix is not NULL it is inserted before the string returned by
	 * function \a get_genname().
	 *
	 * get_genname_from_scope in titan.core
	 *
	 * @param aData only used to update imports if needed
	 * @param source the source code generated
	 * @return The name of the Java value class in the generated code.
	 */
	public String getGenNameFromScope(final JavaGenData aData, final StringBuilder source, final String prefix) {
		if(myScope == null) {
			ErrorReporter.INTERNAL_ERROR("Code generator reached erroneous setting `" + getFullName() + "''");
			return "FATAL_ERROR encountered while processing `" + getFullName() + "''\n";
		}

		final StringBuilder returnValue = new StringBuilder();
		final Module myModule = myScope.getModuleScopeGen();//get_scope_mod_gen
		final Module generatedModule = aData.getModuleScope();
		if(!myModule.equals(aData.getModuleScope()) && !SpecialASN1Module.isSpecAsss(myModule)) {
			//when the definition is referred from another module
			// the reference shall be qualified with the namespace of my module
			returnValue.append(myModule.getName()).append('.');

			if (myModule.getProject() != generatedModule.getProject()) {
				aData.addInterModuleImport(myModule.getName());
			}
		}

		if (prefix != null) {
			returnValue.append(prefix);
		}

		returnValue.append(getGenName());
		switch (getAssignmentType()) {
		case A_PAR_VAL:
		case A_PAR_VAL_IN:
		case A_PAR_TEMP_IN: {
			final FormalParameter formalParameter = (FormalParameter) this;
			if (formalParameter.getEvaluationType() != parameterEvaluationType.NORMAL_EVAL && prefix == null) {
				returnValue.append(".evaluate()");
			}
			break;
		}
		default:
			break;
		}

		return returnValue.toString();
	}

	/**
	 * Generate Java code for module and component level definitions/assignments.
	 *
	 * generate_code in the compiler
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param cleanUp generate cleanup call for the object generated
	 */
	public abstract void generateCode( final JavaGenData aData, final boolean cleanUp );
}
