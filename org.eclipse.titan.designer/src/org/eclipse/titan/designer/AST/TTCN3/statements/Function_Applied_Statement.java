/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.ActualParameterList;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.templates.ParsedActualParameters;
import org.eclipse.titan.designer.AST.TTCN3.values.Function_Reference_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
// NOT PARSED
public final class Function_Applied_Statement extends Statement {
	private static final String FULLNAMEPART1 = ".reference";
	private static final String FULLNAMEPART2 = ".<parameters>";
	private static final String STATEMENT_NAME = "function type application";

	private final Value dereferredValue;
	private final ParsedActualParameters actualParameterList;

	private final ActualParameterList actualParameterList2;

	public Function_Applied_Statement(final Value dereferredValue, final ParsedActualParameters actualParameterList, final ActualParameterList actualParameterList2) {
		this.dereferredValue = dereferredValue;
		this.actualParameterList = actualParameterList;
		this.actualParameterList2 = actualParameterList2;

		if (dereferredValue != null) {
			dereferredValue.setFullNameParent(this);
		}
		if (actualParameterList != null) {
			actualParameterList.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_FUNCTION_APPLIED;
	}

	@Override
	/** {@inheritDoc} */
	public String getStatementName() {
		return STATEMENT_NAME;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (dereferredValue == child) {
			return builder.append(FULLNAMEPART1);
		} else if (actualParameterList == child) {
			return builder.append(FULLNAMEPART2);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (dereferredValue != null) {
			dereferredValue.setMyScope(scope);
		}
		if (actualParameterList != null) {
			actualParameterList.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (dereferredValue != null) {
			dereferredValue.setCodeSection(codeSection);
		}
		if (actualParameterList != null) {
			actualParameterList.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (dereferredValue != null) {
			dereferredValue.updateSyntax(reparser, false);
			reparser.updateLocation(dereferredValue.getLocation());
		}

		if (actualParameterList != null) {
			actualParameterList.updateSyntax(reparser, false);
			reparser.updateLocation(actualParameterList.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (dereferredValue != null) {
			dereferredValue.findReferences(referenceFinder, foundIdentifiers);
		}
		if (actualParameterList != null) {
			actualParameterList.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (dereferredValue != null && !dereferredValue.accept(v)) {
			return false;
		}
		if (actualParameterList != null && !actualParameterList.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final StringBuilder source) {
		final ExpressionStruct expression = new ExpressionStruct();
		final IValue last = dereferredValue.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_DYNAMIC_VALUE, null);
		if (last.getValuetype().equals(Value_type.FUNCTION_REFERENCE_VALUE)) {
			final Function_Reference_Value refdValue = (Function_Reference_Value)last;
			final Definition definition = refdValue.getReferredFunction();

			expression.expression.append(definition.getGenNameFromScope(aData, source, ""));
			expression.expression.append("(");
			if (actualParameterList2 != null && actualParameterList2.getNofParameters() > 0) {
				actualParameterList2.generateCodeAlias(aData, expression, null);
			}
			expression.expression.append(')');
			expression.mergeExpression(source);

			return;
		}

		dereferredValue.generateCodeExpressionMandatory(aData, expression, true);
		expression.expression.append(".invoke(");

		if (actualParameterList2 != null && actualParameterList2.getNofParameters() > 0) {
			actualParameterList2.generateCodeAlias(aData, expression, null);
		}

		expression.expression.append(')');
		expression.mergeExpression(source);
	}
}
