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

import org.eclipse.core.runtime.Platform;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.values.Bitstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Hexstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Octetstring_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * @author Kristof Szabados
 * */
public final class ShiftRightExpression extends Expression_Value {
	private static final String FIRSTOPERANDERROR = "The first operand of the `>>' operation should be a binary string value";
	private static final String SECONDOPERANDERROR = "The second operand of the `>>' operation should be an integer value";
	private static final String NEGATIVESHIFTPROBLEM = "Shifting to the left should be used"
			+ " instead of shifting to the right with a negative value";
	private static final String ZEROSHIFTPROBLEM = "Shifting to the right with 0 will not change the original value";
	private static final String TOOBIGSHIFTPROBLEM = "Shifting a {0} long string to the right with {1}"
			+ " will always result in a string of same size, but filled with '0' -s.";
	private static final String LARGEINTEGERSECONDOPERANDERROR = "Using a large integer value ({0})"
			+ " as the second operand of the `>>'' operation is not supported";

	private final Value value1;
	private final Value value2;

	public ShiftRightExpression(final Value value1, final Value value2) {
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
		return Operation_type.SHIFTRIGHT_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append('(').append(value1.createStringRepresentation());
		builder.append(" >> ");
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
		final IValue last = getValueRefdLast(timestamp, expectedValue, null);

		if (last == null || value1 == null) {
			return Type_type.TYPE_UNDEFINED;
		}

		if (last.getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return Type_type.TYPE_UNDEFINED;
		}

		value1.setLoweridToReference(timestamp);
		final Type_type tempType = value1.getExpressionReturntype(timestamp, expectedValue);
		switch (tempType) {
		case TYPE_BITSTRING:
		case TYPE_HEXSTRING:
		case TYPE_OCTETSTRING:
			return tempType;
		case TYPE_UNDEFINED:
			return tempType;
		default:
			setIsErroneous(true);
			return Type_type.TYPE_UNDEFINED;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (value1 == null || value2 == null || getIsErroneous(timestamp)) {
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
		long stringSize = 0;
		long shiftSize = 0;
		IValue tempValue;

		if (value1 != null) {
			value1.setLoweridToReference(timestamp);
			tempType1 = value1.getExpressionReturntype(timestamp, expectedValue);

			switch (tempType1) {
			case TYPE_BITSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.BITSTRING_VALUE.equals(tempValue.getValuetype())) {
					stringSize = ((Bitstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_HEXSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.HEXSTRING_VALUE.equals(tempValue.getValuetype())) {
					stringSize = ((Hexstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_OCTETSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.OCTETSTRING_VALUE.equals(tempValue.getValuetype())) {
					stringSize = ((Octetstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				location.reportSemanticError(FIRSTOPERANDERROR);
				setIsErroneous(true);
				break;
			}
		}

		if (value2 != null) {
			value2.setLoweridToReference(timestamp);
			tempType2 = value2.getExpressionReturntype(timestamp, expectedValue);

			switch (tempType2) {
			case TYPE_INTEGER:
				tempValue = value2.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.INTEGER_VALUE.equals(tempValue.getValuetype()) && !getIsErroneous(timestamp)) {
					if (!((Integer_Value) tempValue).isNative()) {
						value2.getLocation().reportSemanticError(
								MessageFormat.format(LARGEINTEGERSECONDOPERANDERROR, ((Integer_Value) tempValue).getValueValue()));
						setIsErroneous(true);
						break;
					}
					shiftSize = ((Integer_Value) tempValue).getValue();
					if (value1 != null && !value1.isUnfoldable(timestamp)) {
						final String severity = Platform.getPreferencesService().getString(
								ProductConstants.PRODUCT_ID_DESIGNER,
								PreferenceConstants.REPORTINCORRECTSHIFTROTATESIZE, GeneralConstants.WARNING, null);
						if (shiftSize < 0) {
							location.reportConfigurableSemanticProblem(severity, NEGATIVESHIFTPROBLEM);
						} else if (shiftSize == 0) {
							location.reportConfigurableSemanticProblem(severity, ZEROSHIFTPROBLEM);
						} else if (shiftSize > stringSize) {
							location.reportConfigurableSemanticProblem(severity,
									MessageFormat.format(TOOBIGSHIFTPROBLEM, stringSize, shiftSize));
						}
					}
				}
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				location.reportSemanticError(SECONDOPERANDERROR);
				setIsErroneous(true);
				break;
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

		String string;
		long shiftSize;

		switch (last1.getValuetype()) {
		case BITSTRING_VALUE:
			string = ((Bitstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).getValue();
			lastValue = new Bitstring_Value(shiftRight(string, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		case HEXSTRING_VALUE:
			string = ((Hexstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).getValue();
			lastValue = new Hexstring_Value(shiftRight(string, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		case OCTETSTRING_VALUE:
			string = ((Octetstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).getValue() * 2;
			lastValue = new Octetstring_Value(shiftRight(string, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		default:
			setIsErroneous(true);
			break;
		}

		return lastValue;
	}

	/**
	 * Shifts the contents of the string right by the provided amount.
	 *
	 * @param string
	 *                the string to be shifted
	 * @param shiftSize
	 *                the amount with which the shifting should be done
	 *
	 * @return the resulting shifted value.
	 * */
	public static String shiftRight(final String string, final long shiftSize) {
		if (shiftSize > 0) {
			final StringBuilder result = new StringBuilder();
			if (shiftSize < string.length()) {
				while (result.length() < shiftSize) {
					result.append('0');
				}
				result.append(string.substring(0, string.length() - (int) shiftSize));
			} else {
				while (result.length() < string.length()) {
					result.append('0');
				}
			}

			return result.toString();
		}

		if (shiftSize < 0) {
			return ShiftLeftExpression.shiftLeft(string, -shiftSize);
		}

		return string;
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

	public Value getValue1() {
		return value1;
	}

	public Value getValue2() {
		return value2;
	}
	
	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		value1.generateCodeExpressionMandatory(aData, expression);
		expression.expression.append(".shiftRight( ");
		value2.generateCodeExpressionMandatory(aData, expression);
		expression.expression.append(" )");
	}
}
