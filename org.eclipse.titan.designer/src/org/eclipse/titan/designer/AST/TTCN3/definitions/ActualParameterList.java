/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.util.ArrayList;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a list of actual parameters.
 *
 * @author Kristof Szabados
 * */
public final class ActualParameterList extends ASTNode implements IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".<parameter";

	private ArrayList<ActualParameter> parameters;

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);

		if (parameters == null) {
			return;
		}

		parameters.trimToSize();
		for (int i = 0; i < parameters.size(); i++) {
			parameters.get(i).setMyScope(scope);
		}
	}

	/**
	 * Sets the code_section attribute of these parameters to the provided value.
	 *
	 * @param codeSection the code section where these parameters should be generated.
	 * */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (parameters == null) {
			return;
		}

		for (int i = 0; i < parameters.size(); i++) {
			parameters.get(i).setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (parameters == null) {
			return builder;
		}

		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i) == child) {
				return builder.append(FULLNAMEPART).append(String.valueOf(i + 1)).append(INamedNode.MORETHAN);
			}
		}

		return builder;
	}

	public void addParameter(final ActualParameter parameter) {
		if (parameter != null) {
			if (parameters == null) {
				parameters = new ArrayList<ActualParameter>(1);
			}

			parameters.add(parameter);
			parameter.setFullNameParent(this);
		}
	}

	public int getNofParameters() {
		if (parameters == null) {
			return 0;
		}

		return parameters.size();
	}

	public ActualParameter getParameter(final int index) {
		if (parameters == null) {
			return null;
		}

		return parameters.get(index);
	}

	/**
	 * Checks for circular references within the actual parameter list.
	 *
	 * @param timestamp
	 *                the time stamp of the actual semantic check cycle.
	 * @param referenceChain
	 *                the ReferenceChain used to detect circular references,
	 *                must not be null.
	 **/
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (parameters == null) {
			return;
		}

		for (int i = 0; i < parameters.size(); i++) {
			parameters.get(i).checkRecursions(timestamp, referenceChain);
		}
	}

	/**
	 * Handles the incremental parsing of this list of actual parameters.
	 *
	 * @param reparser
	 *                the parser doing the incremental parsing.
	 * @param isDamaged
	 *                true if the location contains the damaged area, false
	 *                if only its' location needs to be updated.
	 * */
	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (parameters == null) {
			return;
		}

		ActualParameter parameter;
		for (int i = 0, size = parameters.size(); i < size; i++) {
			parameter = parameters.get(i);

			parameter.updateSyntax(reparser, false);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (parameters != null) {
			for (ActualParameter ap : parameters) {
				if (!ap.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Generates the Java equivalent of the actual parameter list without
	 * considering any aliasing between variables and 'in' parameters.
	 *
	 * generate_code_noalias in the compiler
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param expression the expression used for code generation
	 */
	public void generateCodeNoAlias( final JavaGenData aData, final ExpressionStruct expression ) {
		if ( parameters == null ) {
			return;
		}
		final int size = parameters.size();
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				expression.expression.append(", ");
			}
			parameters.get(i).generateCode(aData, expression);
		}
	}

	//TODO re-implement
	public void generateCodeAlias( final JavaGenData aData, final ExpressionStruct expression) {
		generateCodeNoAlias(aData, expression);
	}

	/**
	 * Appends the initialization sequence of all (directly or indirectly)
	 * referred non-parameterized templates and the default values of all
	 * parameterized templates to source and returns the resulting string.
	 * Only objects belonging to module usageModule are initialized.
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source where the could should be added
	 * @param usageModule where the parameters are to be used
	 * */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		for (int i = 0, size = parameters.size(); i < size; i++) {
			parameters.get(i).reArrangeInitCode(aData, source, usageModule);
		}
	}
}
