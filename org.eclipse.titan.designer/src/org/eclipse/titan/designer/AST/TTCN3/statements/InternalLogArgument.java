/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * @author Kristof Szabados
 * */
public abstract class InternalLogArgument {
	public enum ArgumentType {
		TemplateInstance, Value, Match, Macro, Reference, String
	}

	private final ArgumentType argumentType;

	protected InternalLogArgument(final ArgumentType argumentType) {
		this.argumentType = argumentType;
	}

	public final ArgumentType getArgumentType() {
		return argumentType;
	}

	/**
	 * Sets the code_section attribute of this log argument to the provided value.
	 *
	 * @param codeSection the code section where this log argument should be generated.
	 * */
	public abstract void setCodeSection(final CodeSectionType codeSection);

	/**
	 * Checks whether this log argument is defining itself in a recursive
	 * way. This can happen for example if a constant is using itself to
	 * determine its initial value.
	 *
	 * @param timestamp
	 *                the time stamp of the actual semantic check cycle.
	 * @param referenceChain
	 *                the ReferenceChain used to detect circular references.
	 * */
	public abstract void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain);

	/**
	 * Add generated java code on this level.
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param expression the expression code generated
	 */
	public abstract void generateCode( final JavaGenData aData, final ExpressionStruct expression );
}
