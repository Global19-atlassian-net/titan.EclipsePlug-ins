/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class LogArguments extends ASTNode implements IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".logargs_";

	private final ArrayList<LogArgument> arguments;

	public LogArguments() {
		super();
		arguments = new ArrayList<LogArgument>();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0, size = arguments.size(); i < size; i++) {
			if (arguments.get(i) == child) {
				return builder.append(FULLNAMEPART).append(Integer.toString(i + 1));
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);

		for (int i = 0, size = arguments.size(); i < size; i++) {
			arguments.get(i).setMyScope(scope);
		}
	}

	/**
	 * Sets the code_section attribute for these arguments to the provided value.
	 *
	 * @param codeSection the code section where these arguments should be generated.
	 * */
	public void setCodeSection(final CodeSectionType codeSection) {
		for (int i = 0, size = arguments.size(); i < size; i++) {
			arguments.get(i).setCodeSection(codeSection);
		}
	}

	public int getNofLogArguments () {
		return arguments.size();
	}

	public void add(final LogArgument logArgument) {
		if (logArgument != null) {
			arguments.add(logArgument);
			logArgument.setFullNameParent(this);
		}
	}

	public LogArgument getLogArgumentByIndex(final int index) {
		return arguments.get(index);
	}

	/**
	 * Does the semantic checking of the log arguments.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * */
	public void check(final CompilationTimeStamp timestamp) {
		arguments.trimToSize();
		for (int i = 0, size = arguments.size(); i < size; i++) {
			arguments.get(i).check(timestamp);
		}
	}

	/**
	 * Checks whether this value is defining itself in a recursive way. This
	 * can happen for example if a constant is using itself to determine its
	 * initial value.
	 *
	 * @param timestamp
	 *                the time stamp of the actual semantic check cycle.
	 * @param referenceChain
	 *                the ReferenceChain used to detect circular references.
	 * */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		for (int i = 0, size = arguments.size(); i < size; i++) {
			arguments.get(i).checkRecursions(timestamp, referenceChain);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		for (int i = 0, size = arguments.size(); i < size; i++) {
			final LogArgument argument = arguments.get(i);

			argument.updateSyntax(reparser, false);
			reparser.updateLocation(argument.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (arguments == null) {
			return;
		}

		final List<LogArgument> tempList = new ArrayList<LogArgument>(arguments);
		for (final LogArgument logArgument : tempList) {
			logArgument.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (arguments != null) {
			for (final LogArgument la : arguments) {
				if (!la.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Add generated java code on this level.
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if ( arguments == null ) {
			return;
		}

		//This will be the final implementation
		for ( int i = 0; i < arguments.size(); i++ ) {
			arguments.get(i).generateCode(aData, source);
		}
	}

	/**
	 * Generates the equivalent Java code for the log argument into an expression
	 *
	 *  generate_code_expr in the compiler
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param expression the expression to generate source code into
	 * */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression) {
		if ( arguments == null ) {
			return;
		}

		aData.addCommonLibraryImport("TTCN_Logger");
		aData.addBuiltinTypeImport("TitanCharString");

		expression.preamble.append( "TTCN_Logger.begin_event_log2str();\n");
		final int size = arguments.size();
		for ( int i = 0; i < size; i++ ) {
			arguments.get( i ).generateCode(aData, expression.preamble);
		}
		String tempId = aData.getTemporaryVariableName();
		expression.preamble.append(MessageFormat.format("final TitanCharString {0} = TTCN_Logger.end_event_log2str();\n", tempId));
		expression.expression.append(tempId);
	}
}
