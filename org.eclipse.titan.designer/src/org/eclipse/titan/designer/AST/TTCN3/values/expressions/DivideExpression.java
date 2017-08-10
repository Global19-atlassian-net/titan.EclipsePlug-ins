/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Real_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class DivideExpression extends Expression_Value {
	private static final String FIRSTOPERANDERROR = "The first operand of the `/' operation should be an integer or float value";
	private static final String FIRSTOPERANDERROR2 = "{0} can not be used as the first operand of the `/'' operation";
	private static final String SECONDOPERANDERROR = "The second operand of the `/' operation should be an integer or float value";
	private static final String SECONDOPERANDERROR2 = "{0} can not be used as the second operand of the `/'' operation";
	private static final String SAMEOPERANDERROR = "The operands of operation `/' should be of the same type";
	private static final String ZEROOPERANDERROR = "The second operand of operation `/' should not be zero";

	private final Value value1;
	private final Value value2;

	public DivideExpression(final Value value1, final Value value2) {
		this.value1 = value1;
		this.value2 = value2;

		if (value1 != null) {
			value1.setFullNameParent(this);
		}
		if (value2 != null) {
			value2.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.DIVIDE_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (value1.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}
		if (value2.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append('(').append(value1.createStringRepresentation());
		builder.append(" / ");
		builder.append(value2.createStringRepresentation()).append(')');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (value1 != null) {
			value1.setMyScope(scope);
		}
		if (value2 != null) {
			value2.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (value1 == child) {
			return builder.append(OPERAND1);
		} else if (value2 == child) {
			return builder.append(OPERAND2);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (value1 == null) {
			return Type_type.TYPE_UNDEFINED;
		}

		value1.setLoweridToReference(timestamp);
		final Type_type tempType = value1.getExpressionReturntype(timestamp, expectedValue);
		switch (tempType) {
		case TYPE_INTEGER:
		case TYPE_REAL:
			return tempType;
		case TYPE_UNDEFINED:
			return tempType;
		default:
			getValueRefdLast(timestamp, expectedValue, null);
			setIsErroneous(true);
			return Type_type.TYPE_UNDEFINED;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (value1 == null || value2 == null) {
			return true;
		}

		return value1.isUnfoldable(timestamp, expectedValue, referenceChain) || value2.isUnfoldable(timestamp, expectedValue, referenceChain);
	}

	/**
	 * Checks the parameters of the expression and if they are valid in
	 * their position in the expression or not.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param expectedValue
	 *                the kind of value expected.
	 * @param referenceChain
	 *                a reference chain to detect cyclic references.
	 * */
	private void checkExpressionOperands(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		Type_type tempType1 = null;
		Type_type tempType2 = null;

		if (value1 != null) {
			value1.setLoweridToReference(timestamp);
			tempType1 = value1.getExpressionReturntype(timestamp, expectedValue);

			switch (tempType1) {
			case TYPE_INTEGER:
				value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				break;
			case TYPE_REAL: {
				final IValue last = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (!last.isUnfoldable(timestamp)) {
					final Real_Value real = (Real_Value) last;
					if (real.isSpecialFloat()) {
						value1.getLocation().reportSemanticError(
								MessageFormat.format(FIRSTOPERANDERROR2, real.createStringRepresentation()));
						setIsErroneous(true);
					}
				}
				break;
			}
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				value1.getLocation().reportSemanticError(FIRSTOPERANDERROR);
				setIsErroneous(true);
				break;
			}
		}

		if (value2 != null) {
			value2.setLoweridToReference(timestamp);
			tempType2 = value2.getExpressionReturntype(timestamp, expectedValue);

			switch (tempType2) {
			case TYPE_INTEGER: {
				final IValue lastValue = value2.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (!lastValue.getIsErroneous(timestamp) && !lastValue.isUnfoldable(timestamp)) {
					if (((Integer_Value) lastValue).equals(new Integer_Value(0L))) {
						value2.getLocation().reportSemanticError(ZEROOPERANDERROR);
						setIsErroneous(true);
					}
				}
				break;
			}
			case TYPE_REAL: {
				final IValue lastValue = value2.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (!lastValue.getIsErroneous(timestamp) && !lastValue.isUnfoldable(timestamp)) {
					final Real_Value real = (Real_Value) lastValue;
					if (Double.compare(real.getValue(), 0.0) == 0) {
						value2.getLocation().reportSemanticError(ZEROOPERANDERROR);
						setIsErroneous(true);
					} else if (real.isSpecialFloat()) {
						value2.getLocation().reportSemanticError(
								MessageFormat.format(SECONDOPERANDERROR2, real.createStringRepresentation()));
						setIsErroneous(true);
					}
				}
				break;
			}
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				value2.getLocation().reportSemanticError(SECONDOPERANDERROR);
				setIsErroneous(true);
				break;
			}
		}

		if (value1 != null && value2 != null && !getIsErroneous(timestamp)) {
			if (value1.getIsErroneous(timestamp) || value2.getIsErroneous(timestamp)) {
				setIsErroneous(true);
				return;
			}

			if (tempType1 != tempType2) {
				location.reportSemanticError(SAMEOPERANDERROR);
				setIsErroneous(true);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public IValue evaluateValue(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return lastValue;
		}

		isErroneous = false;
		lastTimeChecked = timestamp;
		lastValue = this;

		if (value1 == null || value2 == null) {
			return lastValue;
		}

		checkExpressionOperands(timestamp, expectedValue, referenceChain);

		if (getIsErroneous(timestamp) || isUnfoldable(timestamp, referenceChain)) {
			return lastValue;
		}

		final IValue last1 = value1.getValueRefdLast(timestamp, referenceChain);
		final IValue last2 = value2.getValueRefdLast(timestamp, referenceChain);

		if (last1.getIsErroneous(timestamp) || last2.getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return lastValue;
		}

		switch (last1.getValuetype()) {
		case INTEGER_VALUE:
			// If the operands exist they must be of the same type.
			lastValue = ((Integer_Value) last1).divide((Integer_Value) last2);
			lastValue.copyGeneralProperties(this);
			break;
		case REAL_VALUE:
			final double f1 = ((Real_Value) last1).getValue();
			final double f2 = ((Real_Value) last2).getValue();
			lastValue = new Real_Value(f1 / f2);
			lastValue.copyGeneralProperties(this);
			break;
		default:
			setIsErroneous(true);
			break;
		}

		return lastValue;
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this)) {
			if (value1 != null) {
				referenceChain.markState();
				value1.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
			if (value2 != null) {
				referenceChain.markState();
				value2.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (value1 != null) {
			value1.updateSyntax(reparser, false);
			reparser.updateLocation(value1.getLocation());
		}

		if (value2 != null) {
			value2.updateSyntax(reparser, false);
			reparser.updateLocation(value2.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (value1 != null) {
			value1.findReferences(referenceFinder, foundIdentifiers);
		}
		if (value2 != null) {
			value2.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (value1 != null && !value1.accept(v)) {
			return false;
		}
		if (value2 != null && !value2.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		value1.generateCodeExpressionMandatory(aData, expression);
		expression.expression.append(".div(");
		value2.generateCodeExpressionMandatory(aData, expression);
		expression.expression.append(" )");
	}

}
