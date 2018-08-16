/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter.parameterEvaluationType;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents an actual parameter that has a Value as its actual value.
 *
 * @author Kristof Szabados
 * */
public final class Value_ActualParameter extends ActualParameter {

	private final IValue value;

	public Value_ActualParameter(final IValue value) {
		this.value = value;
	}

	public IValue getValue() {
		return value;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);

		if (value != null) {
			value.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression() {
		if (value != null) {
			return value.canGenerateSingleExpression();
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (value != null) {
			value.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (value != null) {
			referenceChain.markState();
			value.checkRecursions(timestamp, referenceChain);
			referenceChain.previousState();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (value instanceof IIncrementallyUpdateable) {
			((IIncrementallyUpdateable) value).updateSyntax(reparser, false);
			reparser.updateLocation(value.getLocation());
		} else if (value != null) {
			throw new ReParseException();
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (value != null) {
			if (!value.accept(v)) {
				return false;
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeDefaultValue(final JavaGenData aData, final StringBuilder source) {
		//FIXME handle the needs conversion case
		if (value != null) {
			// TODO check if needed at all, right now this is intentionally not active
//			value.generateCodeInit(aData, source, value.get_lhs_name());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final ExpressionStruct expression, final FormalParameter formalParameter) {
		//TODO not complete implementation pl. copy_needed
		if (value != null ) {
			final parameterEvaluationType eval = formalParameter == null ? parameterEvaluationType.NORMAL_EVAL : formalParameter.getEvaluationType();
			if (eval == parameterEvaluationType.NORMAL_EVAL) {
				final StringBuilder expressionExpression = new StringBuilder();
				final ExpressionStruct valueExpression = new ExpressionStruct();
				value.generateCodeExpression(aData, valueExpression, true);
				if(valueExpression.preamble.length() > 0) {
					expression.preamble.append(valueExpression.preamble);
				}
				if(valueExpression.postamble.length() == 0) {
					expressionExpression.append(valueExpression.expression);
				} else {
					// make sure the postambles of the parameters are executed before the
					// function call itself (needed if the value contains function calls
					// with lazy or fuzzy parameters)
					final String tempId = aData.getTemporaryVariableName();
					value.getMyGovernor().getGenNameValue(aData, expression.preamble, myScope);
					expression.preamble.append(" ");
					expression.preamble.append(tempId);
					expression.preamble.append("(");
					expression.preamble.append(valueExpression.expression);
					expression.preamble.append(")");
	
					expression.preamble.append(valueExpression.postamble);
					expressionExpression.append(tempId);
				}
	
				//TODO copy might be needed here
				expression.expression.append(expressionExpression);
			} else {
				final boolean used_as_lvalue = formalParameter == null ? false : formalParameter.getUsedAsLvalue();
				LazyFuzzyParamData.init(used_as_lvalue);
				LazyFuzzyParamData.generateCode(aData, expression, value, myScope, eval == parameterEvaluationType.LAZY_EVAL);
				LazyFuzzyParamData.clean();
				//FIXME implement rest
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (value != null) {
			value.reArrangeInitCode(aData, source, usageModule);
		}
	}
}
