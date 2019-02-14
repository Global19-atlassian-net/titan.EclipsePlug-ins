/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template.Template_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.SpecificValue_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.templates.Template_List;
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

/**
 * @author Kristof Szabados
 * */
public final class ReplaceExpression extends Expression_Value {
	private static final String OPERANDERROR1 = "The second operand of operation `replace' should be an integer value";
	private static final String OPERANDERROR2 = "The third operand of operation `replace' should be an integer value";
	private static final String OPERANDERROR3 = "The second operand of operation `replace' should not be negative";
	private static final String OPERANDERROR4 = "The third operand of operation `replace' should not be negative";
	private static final String OPERANDERROR5 = "The first operand of operation `replace' should be a string, `record of', or a `set of' value";
	private static final String OPERANDERROR6 = "The fourth operand of operation `replace' should be a string, `record of', or a `set of' value";
	private static final String OPERANDERROR7 = "The first and fourth operands of operation `replace' should be of the same type";
	private static final String OPERANDERROR8 = "The third operand of operation `replace'' ({0})"
			+ " is greater than the length of the first operand ({1})";
	private static final String OPERANDERROR9 = "The second operand of operation `replace'' ({0})"
			+ " is greater than the length of the first operand ({1})";
	private static final String OPERANDERROR10 = "The sum of the second operand ({0}) and the third operand ({1}) of operation `replace''"
			+ " is greater than the length of the first operand ({2})";
	private static final String OPERANDERROR11 = "Using a large integer value ({0}) as the second operand of operation `replace'' is not allowed";
	private static final String OPERANDERROR12 = "Using a large integer value ({0}) as the third operand of operation `replace'' is not allowed'";
	private static final String OPERANDERROR13 = "Cannot determine the type of the operand in the `replace()' operation";
	private final TemplateInstance templateInstance1;
	private final Value value2;
	private final Value value3;
	private final TemplateInstance templateInstance4;

	public ReplaceExpression(final TemplateInstance templateInstance1, final Value value2, final Value value3,
			final TemplateInstance templateInstance4) {
		this.templateInstance1 = templateInstance1;
		this.value2 = value2;
		this.value3 = value3;
		this.templateInstance4 = templateInstance4;

		if (templateInstance1 != null) {
			templateInstance1.setFullNameParent(this);
		}
		if (value2 != null) {
			value2.setFullNameParent(this);
		}
		if (value3 != null) {
			value3.setFullNameParent(this);
		}
		if (templateInstance4 != null) {
			templateInstance4.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.REPLACE_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (templateInstance1 != null && templateInstance1.getTemplateBody().checkExpressionSelfReferenceTemplate(timestamp, lhs)) {
			return true;
		}
		if (value2 != null && value2.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}
		if (value3 != null && value3.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}
		if (templateInstance4 != null && templateInstance4.getTemplateBody().checkExpressionSelfReferenceTemplate(timestamp, lhs)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("replace(");
		builder.append(templateInstance1.createStringRepresentation());
		builder.append(", ");
		builder.append(value2.createStringRepresentation());
		builder.append(", ");
		builder.append(value3.createStringRepresentation());
		builder.append(", ");
		builder.append(templateInstance4.createStringRepresentation());
		builder.append(')');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (templateInstance1 != null) {
			templateInstance1.setMyScope(scope);
		}
		if (value2 != null) {
			value2.setMyScope(scope);
		}
		if (value3 != null) {
			value3.setMyScope(scope);
		}
		if (templateInstance4 != null) {
			templateInstance4.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);

		if (templateInstance1 != null) {
			templateInstance1.setCodeSection(codeSection);
		}
		if (value2 != null) {
			value2.setCodeSection(codeSection);
		}
		if (value3 != null) {
			value3.setCodeSection(codeSection);
		}
		if (templateInstance4 != null) {
			templateInstance4.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (templateInstance1 == child) {
			return builder.append(OPERAND1);
		} else if (value2 == child) {
			return builder.append(OPERAND2);
		} else if (value3 == child) {
			return builder.append(OPERAND3);
		} else if (templateInstance4 == child) {
			return builder.append(OPERAND4);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		final IValue last = getValueRefdLast(timestamp, expectedValue, null);

		if (last == null || templateInstance1 == null) {
			return Type_type.TYPE_UNDEFINED;
		}

		if (last.getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return Type_type.TYPE_UNDEFINED;
		}

		final ITTCN3Template template = templateInstance1.getTemplateBody().setLoweridToReference(timestamp);
		final Type_type tempType = template.getExpressionReturntype(timestamp, Expected_Value_type.EXPECTED_TEMPLATE);
		switch (tempType) {
		case TYPE_BITSTRING:
		case TYPE_HEXSTRING:
		case TYPE_OCTETSTRING:
		case TYPE_CHARSTRING:
		case TYPE_UCHARSTRING:
		case TYPE_SET_OF:
		case TYPE_SEQUENCE_OF:
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
		final IType governor = super.getMyGovernor();

		if (governor != null) {
			return governor;
		}

		if (templateInstance1 == null) {
			return null;
		}

		IType tempType;
		if (Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue)) {
			tempType = templateInstance1.getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_TEMPLATE);
		} else {
			tempType = templateInstance1.getExpressionGovernor(timestamp, expectedValue);
		}

		if (tempType != null) {
			tempType = tempType.getTypeRefdLast(timestamp);
		}
		return tempType;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (templateInstance1 == null || templateInstance4 == null || getIsErroneous(timestamp)) {
			return true;
		}

		final ITTCN3Template template1 = templateInstance1.getTemplateBody();
		if (template1 == null || !Template_type.SPECIFIC_VALUE.equals(template1.getTemplatetype())) {
			return true;
		}

		final ITTCN3Template template4 = templateInstance4.getTemplateBody();
		if (template4 == null || !Template_type.SPECIFIC_VALUE.equals(template4.getTemplatetype())) {
			return true;
		}

		final IValue value1 = ((SpecificValue_Template) template1).getSpecificValue();
		final IValue value4 = ((SpecificValue_Template) template4).getSpecificValue();
		if (value1 == null || value4 == null) {
			return true;
		}

		if (value2 == null || value3 == null) {
			return true;
		}

		if (value1.isUnfoldable(timestamp, expectedValue, referenceChain) || value2.isUnfoldable(timestamp, expectedValue, referenceChain)
				|| value3.isUnfoldable(timestamp, expectedValue, referenceChain)
				|| value4.isUnfoldable(timestamp, expectedValue, referenceChain)) {
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
		final Expected_Value_type internalExpectation = Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue) ? Expected_Value_type.EXPECTED_TEMPLATE
				: expectedValue;

		Type_type tempType1 = null;
		IValue value1 = null;

		if (templateInstance1 != null) {
			IType governor = templateInstance1.getExpressionGovernor(timestamp, expectedValue);
			if (governor == null) {
				templateInstance1.getTemplateBody().setLoweridToReference(timestamp);
				governor = templateInstance1.getExpressionGovernor(timestamp, expectedValue);
			}
			if (governor != null) {
				templateInstance1.check(timestamp, governor);
			}

			final ITTCN3Template temp = templateInstance1.getTemplateBody();
			switch( temp.getTemplatetype() ) {
			case SPECIFIC_VALUE:
				break;
			case TEMPLATE_LIST:
			case NAMED_TEMPLATE_LIST:
			case INDEXED_TEMPLATE_LIST:
				templateInstance1.getLocation().reportSemanticError(OPERANDERROR13);
				setIsErroneous(true);
				return;
			default:
				templateInstance1.getLocation().reportSemanticError(OPERANDERROR5);
				setIsErroneous(true);
				return;
			}
			// temp is specific value:
			value1 = ((SpecificValue_Template) temp).getSpecificValue();
			value1.setLoweridToReference(timestamp);
			tempType1 = value1.getExpressionReturntype(timestamp, internalExpectation);

			switch (tempType1) {
			case TYPE_BITSTRING:
			case TYPE_HEXSTRING:
			case TYPE_OCTETSTRING:
			case TYPE_CHARSTRING:
			case TYPE_UCHARSTRING:
			case TYPE_SET_OF:
			case TYPE_SEQUENCE_OF:
				value1.getValueRefdLast(timestamp, internalExpectation, referenceChain);
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				templateInstance1.getLocation().reportSemanticError(OPERANDERROR5);
				setIsErroneous(true);
				break;
			}
		}

		if (value2 != null) {
			value2.setLoweridToReference(timestamp);
			final Type_type tempType2 = value2.getExpressionReturntype(timestamp, expectedValue);

			switch (tempType2) {
			case TYPE_INTEGER:
				final IValue last2 = value2.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (!last2.isUnfoldable(timestamp) && Value.Value_type.INTEGER_VALUE.equals(last2.getValuetype())) {
					if (((Integer_Value) last2).isNative()) {
						final long i = ((Integer_Value) last2).getValue();
						if (i < 0) {
							value2.getLocation().reportSemanticError(OPERANDERROR3);
							setIsErroneous(true);
						}
					} else {
						value2.getLocation().reportSemanticError(MessageFormat.format(OPERANDERROR11, ((Integer_Value) last2).getValueValue()));
						setIsErroneous(true);
					}
				}
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				value2.getLocation().reportSemanticError(OPERANDERROR1);
				setIsErroneous(true);
				break;
			}
		}

		if (value3 != null) {
			value3.setLoweridToReference(timestamp);
			final Type_type tempType3 = value3.getExpressionReturntype(timestamp, expectedValue);

			switch (tempType3) {
			case TYPE_INTEGER:
				final IValue last3 = value3.getValueRefdLast(timestamp, expectedValue, referenceChain);
				if (!last3.isUnfoldable(timestamp) && Value.Value_type.INTEGER_VALUE.equals(last3.getValuetype())) {
					if (((Integer_Value) last3).isNative()) {
						final long i = ((Integer_Value) last3).getValue();
						if (i < 0) {
							value3.getLocation().reportSemanticError(OPERANDERROR4);
							setIsErroneous(true);
						}
					} else {
						value3.getLocation().reportSemanticError(MessageFormat.format(OPERANDERROR12, ((Integer_Value) last3).getValueValue()));
						setIsErroneous(true);
					}
				}
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				value3.getLocation().reportSemanticError(OPERANDERROR2);
				setIsErroneous(true);
				break;
			}
		}

		Type_type tempType4 = null;
		IValue value4 = null;
		if (templateInstance4 != null) {
			IType governor = templateInstance4.getExpressionGovernor(timestamp, expectedValue);
			if (governor == null) {
				templateInstance4.getTemplateBody().setLoweridToReference(timestamp);
				governor = templateInstance4.getExpressionGovernor(timestamp, expectedValue);
			}
			if (governor != null) {
				templateInstance4.check(timestamp, governor);
			}

			final ITTCN3Template temp = templateInstance4.getTemplateBody();

			switch( temp.getTemplatetype() ) {
			case SPECIFIC_VALUE:
				value4 = ((SpecificValue_Template) temp).getSpecificValue();
				break;
			case TEMPLATE_LIST:
				if( !((Template_List) temp).isValue(timestamp) ) {
					templateInstance4.getLocation().reportSemanticError(OPERANDERROR6);
					setIsErroneous(true);
					return;
				}
			case NAMED_TEMPLATE_LIST:
			case INDEXED_TEMPLATE_LIST:
				tempType4 = templateInstance4.getExpressionReturntype(timestamp, expectedValue);
				if (Type_type.TYPE_UNDEFINED.equals(tempType4)){
					templateInstance4.getLocation().reportSemanticError(OPERANDERROR13);
				}
				return;
			default:
				templateInstance4.getLocation().reportSemanticError(OPERANDERROR6);
				setIsErroneous(true);
				return;
			}

			value4.setLoweridToReference(timestamp);
			tempType4 = value4.getExpressionReturntype(timestamp, internalExpectation);

			switch (tempType4) {
			case TYPE_BITSTRING:
			case TYPE_HEXSTRING:
			case TYPE_OCTETSTRING:
			case TYPE_CHARSTRING:
			case TYPE_UCHARSTRING:
			case TYPE_SET_OF:
			case TYPE_SEQUENCE_OF:
				value4.getValueRefdLast(timestamp, internalExpectation, referenceChain);
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				location.reportSemanticError(OPERANDERROR6);
				setIsErroneous(true);
				break;
			}
		}

		if (tempType1 != null && tempType4 != null && !tempType1.equals(tempType4) && !getIsErroneous(timestamp)) {
			location.reportSemanticError(OPERANDERROR7);
			setIsErroneous(true);
		}

		checkExpressionOperandsHelper(timestamp, value1, expectedValue, referenceChain);
	}

	private void checkExpressionOperandsHelper(final CompilationTimeStamp timestamp, final IValue value1,
			final Expected_Value_type expectedValue, final IReferenceChain referenceChain) {
		if (templateInstance1 == null || getIsErroneous(timestamp)) {
			return;
		}

		long valueSize = -1;

		if (!value1.isUnfoldable(timestamp)) {
			IValue temp = value1.setLoweridToReference(timestamp);
			temp = temp.getValueRefdLast(timestamp, referenceChain);
			switch (temp.getValuetype()) {
			case BITSTRING_VALUE:
				valueSize = ((Bitstring_Value) temp).getValueLength();
				break;
			case HEXSTRING_VALUE:
				valueSize = ((Hexstring_Value) temp).getValueLength();
				break;
			case OCTETSTRING_VALUE:
				valueSize = ((Octetstring_Value) temp).getValueLength();
				break;
			case CHARSTRING_VALUE:
				valueSize = ((Charstring_Value) temp).getValueLength();
				break;
			case UNIVERSALCHARSTRING_VALUE:
				valueSize = ((UniversalCharstring_Value) temp).getValueLength();
				break;
			case SETOF_VALUE:
				valueSize = ((SetOf_Value) temp).getNofComponents();
				break;
			case SEQUENCEOF_VALUE:
				valueSize = ((SequenceOf_Value) temp).getNofComponents();
				break;
			default:
				break;
			}
		}

		if (valueSize < 0) {
			return;
		}

		if (value2 == null || value3 == null || templateInstance4 == null) {
			return;
		}

		if (value2.isUnfoldable(timestamp)) {
			if (!value3.isUnfoldable(timestamp)) {
				final IValue last3 = value3.getValueRefdLast(timestamp, expectedValue, referenceChain);
				final long last3Value = ((Integer_Value) last3).getValue();
				if (last3Value > valueSize) {
					location.reportSemanticError(MessageFormat.format(OPERANDERROR8, last3Value, valueSize));
					setIsErroneous(true);
				}
			}
		} else {
			final IValue last2 = value2.getValueRefdLast(timestamp, expectedValue, referenceChain);
			final long last2Value = ((Integer_Value) last2).getValue();
			if (value3.isUnfoldable(timestamp)) {
				if (last2Value > valueSize) {
					location.reportSemanticError(MessageFormat.format(OPERANDERROR9, last2Value, valueSize));
					setIsErroneous(true);
				}
			} else {
				final IValue last3 = value3.getValueRefdLast(timestamp, expectedValue, referenceChain);
				final long last3Value = ((Integer_Value) last3).getValue();
				if (last2Value + last3Value > valueSize) {
					location.reportSemanticError(MessageFormat.format(OPERANDERROR10, last2Value, last3Value, valueSize));
					setIsErroneous(true);
				}
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

		if (templateInstance1 == null || value2 == null || value3 == null || templateInstance4 == null) {
			setIsErroneous(true);
			return lastValue;
		}

		checkExpressionOperands(timestamp, expectedValue, referenceChain);

		if (getIsErroneous(timestamp)) {
			return lastValue;
		}

		if (isUnfoldable(timestamp, referenceChain)) {
			return lastValue;
		}

		ITTCN3Template temp = templateInstance1.getTemplateBody();
		final IValue value1 = ((SpecificValue_Template) temp).getSpecificValue();
		final IValue v1 = value1.getValueRefdLast(timestamp, referenceChain);
		final IValue v2 = value2.getValueRefdLast(timestamp, referenceChain);
		final IValue v3 = value3.getValueRefdLast(timestamp, referenceChain);

		temp = templateInstance4.getTemplateBody();
		final IValue value4 = ((SpecificValue_Template) temp).getSpecificValue();
		final IValue v4 = value4.getValueRefdLast(timestamp, referenceChain);

		final Value_type vt = v1.getValuetype();

		final int index = ((Integer_Value) v2).intValue();
		final int len = ((Integer_Value) v3).intValue();

		switch (vt) {
		case BITSTRING_VALUE: {
			final String v1Str = ((Bitstring_Value) v1).getValue();
			final String v4Str = ((Bitstring_Value) v4).getValue();
			String result = v1Str.substring(0, index);
			result = result.concat(v4Str);
			result = result.concat(v1Str.substring(index + len));
			lastValue = new Bitstring_Value(result);
			lastValue.copyGeneralProperties(this);
			break;
		}
		case HEXSTRING_VALUE: {
			final String v1Str = ((Hexstring_Value) v1).getValue();
			final String v4Str = ((Hexstring_Value) v4).getValue();
			String result = v1Str.substring(0, index);
			result = result.concat(v4Str);
			result = result.concat(v1Str.substring(index + len));
			lastValue = new Hexstring_Value(result);
			lastValue.copyGeneralProperties(this);
			break;
		}
		case OCTETSTRING_VALUE: {
			final String v1Str = ((Octetstring_Value) v1).getValue();
			final String v4Str = ((Octetstring_Value) v4).getValue();
			String result = v1Str.substring(0, index * 2);
			result = result.concat(v4Str);
			result = result.concat(v1Str.substring((index + len) * 2));
			lastValue = new Octetstring_Value(result);
			lastValue.copyGeneralProperties(this);
			break;
		}
		case CHARSTRING_VALUE: {
			final String v1Str = ((Charstring_Value) v1).getValue();
			final String v4Str = ((Charstring_Value) v4).getValue();
			String result = v1Str.substring(0, index);
			result = result.concat(v4Str);
			result = result.concat(v1Str.substring(index + len));
			lastValue = new Charstring_Value(result);
			lastValue.copyGeneralProperties(this);
			break;
		}
		case UNIVERSALCHARSTRING_VALUE: {
			final UniversalCharstring v1Str = ((UniversalCharstring_Value) v1).getValue();
			final UniversalCharstring v4Str = ((UniversalCharstring_Value) v4).getValue();
			final UniversalCharstring result = v1Str.substring(0, index);
			// This append() is not like concat().
			result.append(v4Str);
			result.append(v1Str.substring(index + len));
			lastValue = new UniversalCharstring_Value(result);
			lastValue.copyGeneralProperties(this);
			break;
		}
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
			if (templateInstance1 != null) {
				referenceChain.markState();
				templateInstance1.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
			if (value2 != null) {
				referenceChain.markState();
				value2.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
			if (value3 != null) {
				referenceChain.markState();
				value3.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
			if (templateInstance4 != null) {
				referenceChain.markState();
				templateInstance4.checkRecursions(timestamp, referenceChain);
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

		if (templateInstance1 != null) {
			templateInstance1.updateSyntax(reparser, false);
			reparser.updateLocation(templateInstance1.getLocation());
		}

		if (value2 != null) {
			value2.updateSyntax(reparser, false);
			reparser.updateLocation(value2.getLocation());
		}

		if (value3 != null) {
			value3.updateSyntax(reparser, false);
			reparser.updateLocation(value3.getLocation());
		}

		if (templateInstance4 != null) {
			templateInstance4.updateSyntax(reparser, false);
			reparser.updateLocation(templateInstance4.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (templateInstance1 != null) {
			templateInstance1.findReferences(referenceFinder, foundIdentifiers);
		}
		if (value2 != null) {
			value2.findReferences(referenceFinder, foundIdentifiers);
		}
		if (value3 != null) {
			value3.findReferences(referenceFinder, foundIdentifiers);
		}
		if (templateInstance4 != null) {
			templateInstance4.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (templateInstance1 != null && !templateInstance1.accept(v)) {
			return false;
		}
		if (value2 != null && !value2.accept(v)) {
			return false;
		}
		if (value3 != null && !value3.accept(v)) {
			return false;
		}
		if (templateInstance4 != null && !templateInstance4.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return templateInstance1.hasSingleExpression() && value2.canGenerateSingleExpression()
				&& value3.canGenerateSingleExpression() && templateInstance4.hasSingleExpression();
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (templateInstance1 != null) {
			templateInstance1.reArrangeInitCode(aData, source, usageModule);
		}
		if (value2 != null) {
			value2.reArrangeInitCode(aData, source, usageModule);
		}
		if (value3 != null) {
			value3.reArrangeInitCode(aData, source, usageModule);
		}
		if (templateInstance4 != null) {
			templateInstance4.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		if (lastValue != null && lastValue != this) {
			lastValue.generateCodeExpression(aData, expression, true);
			return;
		}

		final IValue lastValue2 = value2.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE, null);
		final IValue lastValue3 = value3.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE, null);

		//TODO extract to template instance
		final boolean isValue1 = templateInstance1.getDerivedReference() == null && templateInstance1.getTemplateBody().isValue(CompilationTimeStamp.getBaseTimestamp());
		final boolean isValue4 = templateInstance4.getDerivedReference() == null && templateInstance4.getTemplateBody() instanceof SpecificValue_Template;
		// TODO handle the needs conversion case
		final Type_type expressionType = templateInstance1.getExpressionReturntype(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
		switch (expressionType) {
		case TYPE_BITSTRING:
		case TYPE_HEXSTRING:
		case TYPE_OCTETSTRING:
		case TYPE_CHARSTRING:
		case TYPE_UCHARSTRING:
			aData.addCommonLibraryImport("AdditionalFunctions");

			expression.expression.append("AdditionalFunctions.replace( ");
			if (isValue1) {
				final IValue value1 = ((SpecificValue_Template) templateInstance1.getTemplateBody()).getValue();
				value1.generateCodeExpressionMandatory(aData, expression, true);
			} else {
				templateInstance1.generateCode(aData, expression, Restriction_type.TR_NONE);
			}
			expression.expression.append(", ");
			if (lastValue2.isUnfoldable(CompilationTimeStamp.getBaseTimestamp()) || !((Integer_Value) lastValue2).isNative()) {
				lastValue2.generateCodeExpressionMandatory(aData, expression, true);
			} else {
				final long tempNative = ((Integer_Value) lastValue2).getValue();
				expression.expression.append(tempNative);
			}
			expression.expression.append(", ");
			if (lastValue3.isUnfoldable(CompilationTimeStamp.getBaseTimestamp()) || !((Integer_Value) lastValue3).isNative()) {
				lastValue3.generateCodeExpressionMandatory(aData, expression, true);
			} else {
				final long tempNative = ((Integer_Value) lastValue3).getValue();
				expression.expression.append(tempNative);
			}
			expression.expression.append(", ");
			if (isValue4) {
				final IValue value4 = ((SpecificValue_Template)templateInstance4.getTemplateBody()).getValue();
				value4.generateCodeExpressionMandatory(aData, expression, true);
			} else {
				templateInstance4.generateCode(aData, expression, Restriction_type.TR_NONE);
			}
			expression.expression.append(')');
			break;
		case TYPE_SEQUENCE_OF:
		case TYPE_SET_OF:
			if (isValue1) {
				final IValue value1 = ((SpecificValue_Template) templateInstance1.getTemplateBody()).getValue();
				value1.generateCodeExpressionMandatory(aData, expression, true);
			} else {
				templateInstance1.generateCode(aData, expression, Restriction_type.TR_NONE);
			}
			expression.expression.append(".replace( ");
			if (lastValue2 instanceof Integer_Value) {
				final long tempNative = ((Integer_Value) lastValue2).getValue();
				expression.expression.append(tempNative);
			} else if (lastValue2.returnsNative()) {
				lastValue2.generateCodeExpressionMandatory(aData, expression, false);
			} else {
				lastValue2.generateCodeExpressionMandatory(aData, expression, true);
				expression.expression.append(".get_int()");
			}
			expression.expression.append(", ");
			if (lastValue3 instanceof Integer_Value) {
				final long tempNative = ((Integer_Value) lastValue3).getValue();
				expression.expression.append(tempNative);
			} else if (lastValue3.returnsNative()) {
				lastValue3.generateCodeExpressionMandatory(aData, expression, false);
			} else {
				lastValue3.generateCodeExpressionMandatory(aData, expression, true);
				expression.expression.append(".get_int()");
			}
			expression.expression.append(", ");
			if (isValue4) {
				final IValue value4 = templateInstance4.getTemplateBody().getValue();
				value4.generateCodeExpressionMandatory(aData, expression, true);
			} else {
				templateInstance4.generateCode(aData, expression, Restriction_type.TR_NONE);
			}
			expression.expression.append(')');
			break;
		default:
			ErrorReporter.INTERNAL_ERROR("FATAL ERROR while generating code for expression `" + getFullName() + "''");
			break;
		}
	}
}
