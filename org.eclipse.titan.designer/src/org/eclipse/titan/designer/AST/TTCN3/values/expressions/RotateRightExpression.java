/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.TypeFactory;
import org.eclipse.titan.designer.AST.TTCN3.values.Array_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Bitstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Charstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Hexstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Octetstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SequenceOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SetOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.UniversalCharstring;
import org.eclipse.titan.designer.AST.TTCN3.values.UniversalCharstring_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * @author Kristof Szabados
 * */
public final class RotateRightExpression extends Expression_Value {
	private static final String FIRSTOPERANDERROR = "The first operand of the `@>' operation should be a string,"
			+ " `record of', `set of' or an array  value";
	private static final String SECONDOPERANDERROR = "The second operand of the `@>' operation should be an integer value";
	private static final String EFFECTLESSROTATION = "Rotating will not change the value";
	private static final String NEGATIVEROTATEPROBLEM = "Rotating to the left should be used instead of rotating to the right"
			+ " with a negative value";
	private static final String ZEROROTATEPROBLEM = "Rotating to the right with 0 will not change the original value";
	private static final String TOOBIGROTATEPROBLEM = "Rotating a {0} long value to the right with {1}"
			+ " will have the same effect as rotating by {2}";
	private static final String LARGEINTEGERSECONDOPERANDERROR = "Using a large integer value ({0})"
			+ " as the second operand of the `@>'' operation is not supported";

	private final Value value1;
	private final Value value2;

	private boolean needsConversion = false;

	public RotateRightExpression(final Value value1, final Value value2) {
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
		return Operation_type.ROTATERIGHT_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (value1 != null && value1.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}
		if (value2 != null && value2.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append('(').append(value1.createStringRepresentation());
		builder.append(" @> ");
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
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);

		if (value1 != null) {
			value1.setCodeSection(codeSection);
		}
		if (value2 != null) {
			value2.setCodeSection(codeSection);
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
		case TYPE_CHARSTRING:
		case TYPE_UCHARSTRING:
		case TYPE_SET_OF:
		case TYPE_SEQUENCE_OF:
		case TYPE_ARRAY:
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
	public IType getExpressionGovernor(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		IType type = getMyGovernor();
		if (type != null) {
			return type;
		}

		final IValue last = getValueRefdLast(timestamp, expectedValue, null);

		if (last == null || value1 == null) {
			return null;
		}

		if (last.getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return null;
		}

		value1.setLoweridToReference(timestamp);
		final Type_type tempType = value1.getExpressionReturntype(timestamp, expectedValue);
		switch (tempType) {
		case TYPE_BITSTRING:
		case TYPE_HEXSTRING:
		case TYPE_OCTETSTRING:
		case TYPE_CHARSTRING:
		case TYPE_UCHARSTRING:
			return TypeFactory.createType(tempType);
		case TYPE_SET_OF:
		case TYPE_SEQUENCE_OF:
		case TYPE_ARRAY:
			return value1.getExpressionGovernor(timestamp, expectedValue);
		case TYPE_UNDEFINED:
			return null;
		default:
			setIsErroneous(true);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (value1 == null || value2 == null || getIsErroneous(timestamp)) {
			return true;
		}

		if (value1.isUnfoldable(timestamp, expectedValue, referenceChain) || value2.isUnfoldable(timestamp, expectedValue, referenceChain)) {
			return true;
		}

		value1.setLoweridToReference(timestamp);
		final Type_type tempType = value1.getExpressionReturntype(timestamp, expectedValue);

		switch (tempType) {
		case TYPE_BITSTRING:
		case TYPE_HEXSTRING:
		case TYPE_OCTETSTRING:
		case TYPE_CHARSTRING:
		case TYPE_UCHARSTRING:
			return false;
		default:
			return true;
		}
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
		long valueSize = 0;
		long rotationSize = 0;
		IValue tempValue;

		if (value1 != null) {
			value1.setLoweridToReference(timestamp);
			tempType1 = value1.getExpressionReturntype(timestamp, expectedValue);

			switch (tempType1) {
			case TYPE_BITSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.BITSTRING_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((Bitstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_HEXSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.HEXSTRING_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((Hexstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_OCTETSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.OCTETSTRING_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((Octetstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_CHARSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.CHARSTRING_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((Charstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_UCHARSTRING:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.UNIVERSALCHARSTRING_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((UniversalCharstring_Value) tempValue).getValueLength();
				}
				break;
			case TYPE_SET_OF: {
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.SEQUENCEOF_VALUE.equals(tempValue.getValuetype())) {
					tempValue = tempValue.setValuetype(timestamp, Value_type.SETOF_VALUE);
				}
				if (Value_type.SETOF_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((SetOf_Value) tempValue).getNofComponents();
				}

				final IType v1_governor = tempValue.getExpressionGovernor(timestamp, expectedValue);
				final IValue temp = v1_governor.checkThisValueRef(timestamp, tempValue);
				v1_governor.checkThisValue(timestamp, temp, null, new IType.ValueCheckingOptions(expectedValue, false, false, true, false, false));
				final TypeCompatibilityInfo info = new TypeCompatibilityInfo(getMyGovernor(), v1_governor, true);
				if (myGovernor != null && !myGovernor.isCompatible(timestamp, v1_governor , info, null, null)) {
					if (info.getSubtypeError() == null) {
						if (info.getErrorStr() == null) {
							getLocation().reportSemanticError(MessageFormat.format("First operand of operation `@>'' is of type `{0}'', but a value of type `{1}'' was expected here", v1_governor.getTypename(), myGovernor.getTypename()));
						} else {
							getLocation().reportSemanticError(info.getErrorStr());
						}
					} else {
						// this is ok.
						if (info.getNeedsConversion()) {
							needsConversion = true;
						}
					}
				} else if (info.getNeedsConversion()) {
					needsConversion = true;
				}
				break;
			}
			case TYPE_SEQUENCE_OF: {
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.SEQUENCEOF_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((SequenceOf_Value) tempValue).getNofComponents();
				} else if (Value_type.SETOF_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((SetOf_Value) tempValue).getNofComponents();
				}

				final IType v1_governor = value1.getExpressionGovernor(timestamp, expectedValue);
				final IValue temp = v1_governor.checkThisValueRef(timestamp, value1);
				v1_governor.checkThisValue(timestamp, temp, null, new IType.ValueCheckingOptions(expectedValue, false, false, true, false, false));
				final TypeCompatibilityInfo info = new TypeCompatibilityInfo(getMyGovernor(), v1_governor, true);
				if (myGovernor != null && !myGovernor.isCompatible(timestamp, v1_governor , info, null, null)) {
					if (info.getSubtypeError() == null) {
						final String errorString = info.getErrorStringString();
						if (errorString == null) {
							getLocation().reportSemanticError(MessageFormat.format("First operand of operation `@>'' is of type `{0}'', but a value of type `{1}'' was expected here", v1_governor.getTypename(), myGovernor.getTypename()));
						} else {
							getLocation().reportSemanticError(errorString);
						}
					} else {
						// this is ok.
						if (info.getNeedsConversion()) {
							needsConversion = true;
						}
					}
				} else if (info.getNeedsConversion()) {
					needsConversion = true;
				}
				break;
			}
			case TYPE_ARRAY:
				tempValue = value1.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (Value_type.SEQUENCEOF_VALUE.equals(tempValue.getValuetype())) {
					tempValue = tempValue.setValuetype(timestamp, Value_type.ARRAY_VALUE);
				}
				if (Value_type.ARRAY_VALUE.equals(tempValue.getValuetype())) {
					valueSize = ((Array_Value) tempValue).getNofComponents();
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
					rotationSize = ((Integer_Value) tempValue).getValue();
					if (value1 != null && !value1.isUnfoldable(timestamp)) {
						final String severity = Platform.getPreferencesService().getString(
								ProductConstants.PRODUCT_ID_DESIGNER,
								PreferenceConstants.REPORTINCORRECTSHIFTROTATESIZE, GeneralConstants.WARNING, null);
						if (valueSize == 0 || valueSize == 1) {
							location.reportConfigurableSemanticProblem(severity, EFFECTLESSROTATION);
						} else if (rotationSize < 0) {
							location.reportConfigurableSemanticProblem(severity, NEGATIVEROTATEPROBLEM);
						} else if (rotationSize == 0) {
							location.reportConfigurableSemanticProblem(severity, ZEROROTATEPROBLEM);
						} else if (rotationSize > valueSize) {
							location.reportConfigurableSemanticProblem(severity, MessageFormat.format(
									TOOBIGROTATEPROBLEM, valueSize, rotationSize, rotationSize % valueSize));
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
		needsConversion = false;

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
		int shiftSize;

		switch (last1.getValuetype()) {
		case BITSTRING_VALUE:
			string = ((Bitstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).intValue();
			lastValue = new Bitstring_Value(rotateRight(string, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		case HEXSTRING_VALUE:
			string = ((Hexstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).intValue();
			lastValue = new Hexstring_Value(rotateRight(string, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		case OCTETSTRING_VALUE:
			string = ((Octetstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).intValue() * 2;
			lastValue = new Octetstring_Value(rotateRight(string, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		case CHARSTRING_VALUE:
			string = ((Charstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).intValue();
			lastValue = new Charstring_Value(rotateRight(string, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		case UNIVERSALCHARSTRING_VALUE: {
			final UniversalCharstring string2 = ((UniversalCharstring_Value) last1).getValue();
			shiftSize = ((Integer_Value) last2).intValue();
			lastValue = new UniversalCharstring_Value(rotateRight(string2, shiftSize));
			lastValue.copyGeneralProperties(this);
			break;
		}
		default:
			setIsErroneous(true);
			break;
		}

		return lastValue;
	}

	/**
	 * Rotates the contents of the string right by the provided amount.
	 *
	 * @param string
	 *                the string to be rotated
	 * @param rotateSize
	 *                the amount with which the rotation should be done
	 *
	 * @return the resulting rotated value.
	 * */
	public static String rotateRight(final String string, final int rotateSize) {
		if (string.length() == 0) {
			return "";
		}

		if (rotateSize < 0) {
			return RotateLeftExpression.rotateLeft(string, -rotateSize);
		}

		final int realAmmount = rotateSize % string.length();
		if (realAmmount == 0) {
			return string;
		}

		return string.substring(string.length() - realAmmount) + string.substring(0, string.length() - realAmmount);
	}

	/**
	 * Rotates the contents of the string right by the provided amount.
	 *
	 * @param string
	 *                the string to be rotated
	 * @param rotateSize
	 *                the amount with which the rotation should be done
	 *
	 * @return the resulting rotated value.
	 * */
	public static UniversalCharstring rotateRight(final UniversalCharstring string, final int rotateSize) {
		if (string.length() == 0) {
			return new UniversalCharstring();
		}

		if (rotateSize < 0) {
			return RotateLeftExpression.rotateLeft(string, -rotateSize);
		}

		final int realAmmount = rotateSize % string.length();
		if (realAmmount == 0) {
			return new UniversalCharstring(string);
		}

		return string.substring(string.length() - realAmmount).append(string.substring(0, string.length() - realAmmount));
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
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (value1 != null) {
			value1.reArrangeInitCode(aData, source, usageModule);
		}
		if (value2 != null) {
			value2.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return !needsConversion && value1.canGenerateSingleExpression() && value2.canGenerateSingleExpression();
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		if (needsConversion) {
			final ExpressionStruct tempExpr = new ExpressionStruct();
			final String tempId1 = aData.getTemporaryVariableName();
			final IType myGovernor = getMyGovernor();
			final IType v1Governor = value1.getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
			expression.preamble.append(MessageFormat.format("{0} {1} = ", v1Governor.getGenNameValue(aData, expression.preamble), tempId1));
			value1.generateCodeExpressionMandatory(aData, tempExpr, true);
			tempExpr.expression.append(".rotate_right( ");
			value2.generateCodeExpressionMandatory(aData, tempExpr, false);
			tempExpr.expression.append(" );\n");
			tempExpr.mergeExpression(expression.preamble);

			final String tempId2 = myGovernor.generateConversion(aData, v1Governor, tempId1, true, expression);
			expression.expression.append(tempId2);
		} else {
			value1.generateCodeExpressionMandatory(aData, expression, true);
			expression.expression.append(".rotate_right( ");
			value2.generateCodeExpressionMandatory(aData, expression, false);
			expression.expression.append(" )");
		}
	}
}
