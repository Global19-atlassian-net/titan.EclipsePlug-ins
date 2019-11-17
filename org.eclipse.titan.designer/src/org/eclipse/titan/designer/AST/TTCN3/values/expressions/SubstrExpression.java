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

import org.eclipse.titan.common.logging.ErrorReporter;
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
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template.Template_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.SpecificValue_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.values.Bitstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Charstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Hexstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Octetstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SequenceOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SetOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.UniversalCharstring_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class SubstrExpression extends Expression_Value {
	private static final String OPERANDERROR1 = "The first operand of operation `substr' should be a string, `record of', or a `set of' value";
	private static final String OPERANDERROR2 = "The second operand of operation `substr' should be an integer value";
	private static final String OPERANDERROR3 = "The second operand of operation `substr' should not be negative";
	private static final String OPERANDERROR4 = "The third operand of operation `substr' should be an integer value";
	private static final String OPERANDERROR5 = "The third operand of operation `substr' should not be negative";
	private static final String OPERANDERROR6 = "The third operand of operation `substr'' ({0})"
			+ " is greater than the length of the first operand ({1})";
	private static final String OPERANDERROR7 = "The second operand of operation `substr'' ({0})"
			+ " is greater than the length of the first operand ({1})";
	private static final String OPERANDERROR8 = "The sum of second operand ({0}) and third operand ({1}) of operation `substr''"
			+ " is greater than the length of the first operand ({2})";
	private static final String OPERANDERROR9 = "Using a large integer value ({0}) as the second operand of operation `substr'' is not allowed";
	private static final String OPERANDERROR10 = "Using a large integer value ({0}) as the third operand of operation `substr'' is not allowed";
	private static final String OPERANDERROR11 = "The first operand of operation `substr' cannot be an empty list";

	private final TemplateInstance templateInstance1;
	private final Value value2;
	private final Value value3;

	public SubstrExpression(final TemplateInstance templateInstance1, final Value value2, final Value value3) {
		this.templateInstance1 = templateInstance1;
		this.value2 = value2;
		this.value3 = value3;

		if (templateInstance1 != null) {
			templateInstance1.setFullNameParent(this);
		}
		if (value2 != null) {
			value2.setFullNameParent(this);
		}
		if (value3 != null) {
			value3.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.SUBSTR_OPERATION;
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

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("substr");
		builder.append('(').append(templateInstance1.createStringRepresentation());
		builder.append(", ");
		builder.append(value2.createStringRepresentation());
		builder.append(", ");
		builder.append(value3.createStringRepresentation());
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
		if (templateInstance1 == null || value2 == null || value3 == null || getIsErroneous(timestamp)) {
			return true;
		}

		ITTCN3Template template = templateInstance1.getTemplateBody();
		if (template == null || !Template_type.SPECIFIC_VALUE.equals(template.getTemplatetype())) {
			return true;
		}

		final IValue value1 = ((SpecificValue_Template) template).getSpecificValue();
		if (value1 == null) {
			return true;
		}

		template = template.setLoweridToReference(timestamp);
		final Type_type tempType = template.getExpressionReturntype(timestamp, Expected_Value_type.EXPECTED_TEMPLATE);
		switch (tempType) {
		case TYPE_BITSTRING:
		case TYPE_HEXSTRING:
		case TYPE_OCTETSTRING:
		case TYPE_CHARSTRING:
		case TYPE_UCHARSTRING:
			break;
		default:
			return true;
		}

		return value1.isUnfoldable(timestamp, expectedValue, referenceChain) || value2.isUnfoldable(timestamp, expectedValue, referenceChain)
				|| value3.isUnfoldable(timestamp, expectedValue, referenceChain);
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

		IValue value1 = null;

		if (templateInstance1 != null) {
			final ITTCN3Template temp = templateInstance1.getTemplateBody();
			if (!Template_type.SPECIFIC_VALUE.equals(temp.getTemplatetype())) {
				templateInstance1.getLocation().reportSemanticError(OPERANDERROR1);
				setIsErroneous(true);
			}
			value1 = ((SpecificValue_Template) temp).getSpecificValue();
			value1.setLoweridToReference(timestamp);
			final IType templateGovernor = templateInstance1.getExpressionGovernor(timestamp, expectedValue);
			final IType lastTemplateGovernor = templateGovernor.getTypeRefdLast(timestamp);

			switch (lastTemplateGovernor.getTypetype()) {
			case TYPE_BITSTRING:
			case TYPE_HEXSTRING:
			case TYPE_OCTETSTRING:
			case TYPE_CHARSTRING:
			case TYPE_UCHARSTRING:
				value1.getValueRefdLast(timestamp, internalExpectation, referenceChain);
				break;
			case TYPE_SET_OF:
			case TYPE_SEQUENCE_OF: {
				IsValueExpression.checkExpressionTemplateInstance(timestamp, this, templateInstance1, lastTemplateGovernor, referenceChain, expectedValue);
				if (getIsErroneous(timestamp)) {
					return;
				}

				templateInstance1.getTemplateBody().checkSpecificValue(timestamp, false);

				final TypeCompatibilityInfo info = new TypeCompatibilityInfo(myGovernor, lastTemplateGovernor, true);
				if (myGovernor != null && !myGovernor.isCompatible(timestamp, lastTemplateGovernor , info, null, null)) {
					if (info.getSubtypeError() == null) {
						final String errorString = info.getErrorStringString();
						if (errorString == null) {
							getLocation().reportSemanticError(MessageFormat.format("First operand of operation `substr'' is of type `{0}'', but a value of type `{1}'' was expected here", lastTemplateGovernor.getTypename(), myGovernor.getTypename()));
						} else {
							getLocation().reportSemanticError(errorString);
						}
					} else {
						// this is ok.
						if (info.getNeedsConversion()) {
							set_needs_conversion();
						}
					}
				} else if (info.getNeedsConversion()) {
					set_needs_conversion();
				}
				break;
			}
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				templateInstance1.getLocation().reportSemanticError(OPERANDERROR1);
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
						value2.getLocation().reportSemanticError(MessageFormat.format(OPERANDERROR9, ((Integer_Value) last2).getValueValue()));
						setIsErroneous(true);
					}
				}
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				location.reportSemanticError(OPERANDERROR2);
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
							value3.getLocation().reportSemanticError(OPERANDERROR5);
							setIsErroneous(true);
						}
					} else {
						value3.getLocation().reportSemanticError(MessageFormat.format(OPERANDERROR10, ((Integer_Value) last3).getValueValue()));
						setIsErroneous(true);
					}
				}
				break;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				break;
			default:
				location.reportSemanticError(OPERANDERROR4);
				setIsErroneous(true);
				break;
			}
		}

		checkExpressionOperandsHelper(timestamp, value1, expectedValue, referenceChain);
	}

	private void checkExpressionOperandsHelper(final CompilationTimeStamp timestamp, final IValue value1,
			final Expected_Value_type expectedValue, final IReferenceChain referenceChain) {
		if (value1 == null || value2 == null || value3 == null || getIsErroneous(timestamp)) {
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
			case SEQUENCEOF_VALUE:
				valueSize = ((SequenceOf_Value) temp).getNofComponents();
				if(valueSize==0){
					templateInstance1.getLocation().reportSemanticError(OPERANDERROR11);
				}
				break;
			case SETOF_VALUE:
				valueSize = ((SetOf_Value) temp).getNofComponents();
				if(valueSize==0){
					templateInstance1.getLocation().reportSemanticError(OPERANDERROR11);
				}
				break;
			default:
				break;
			}
		}

		if (valueSize < 0) {
			return;
		}

		if (value2.isUnfoldable(timestamp)) {
			if (!value3.isUnfoldable(timestamp)) {
				final IValue last3 = value3.getValueRefdLast(timestamp, expectedValue, referenceChain);
				final long last3Value = ((Integer_Value) last3).getValue();
				if (last3Value > valueSize) {
					location.reportSemanticError(MessageFormat.format(OPERANDERROR6, last3Value, valueSize));
					setIsErroneous(true);
				}
			}
		} else {
			final IValue last2 = value2.getValueRefdLast(timestamp, expectedValue, referenceChain);
			final long last2Value = ((Integer_Value) last2).getValue();
			if (value3.isUnfoldable(timestamp)) {
				if (last2Value > valueSize) {
					location.reportSemanticError(MessageFormat.format(OPERANDERROR7, last2Value, valueSize));
					setIsErroneous(true);
				}
			} else {
				final IValue last3 = value3.getValueRefdLast(timestamp, expectedValue, referenceChain);
				final long last3Value = ((Integer_Value) last3).getValue();
				if (last2Value + last3Value > valueSize) {
					location.reportSemanticError(MessageFormat.format(OPERANDERROR8, last2Value, last3Value, valueSize));
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

		if (templateInstance1 == null || value2 == null || value3 == null) {
			return lastValue;
		}

		checkExpressionOperands(timestamp, expectedValue, referenceChain);

		if (getIsErroneous(timestamp) || isUnfoldable(timestamp, referenceChain)) {
			return lastValue;
		}

		final ITTCN3Template temp = templateInstance1.getTemplateBody();
		final IValue value1 = ((SpecificValue_Template) temp).getSpecificValue();
		final IValue v1 = value1.getValueRefdLast(timestamp, referenceChain);
		final IValue v2 = value2.getValueRefdLast(timestamp, referenceChain);
		final IValue v3 = value3.getValueRefdLast(timestamp, referenceChain);

		final int index = (int) ((Integer_Value) v2).getValue();
		final int len = (int) ((Integer_Value) v3).getValue();

		switch (v1.getValuetype()) {
		case BITSTRING_VALUE:
			lastValue = new Bitstring_Value(((Bitstring_Value) v1).getValue().substring(index, index + len));
			lastValue.copyGeneralProperties(this);
			break;
		case HEXSTRING_VALUE:
			lastValue = new Hexstring_Value(((Hexstring_Value) v1).getValue().substring(index, index + len));
			lastValue.copyGeneralProperties(this);
			break;
		case OCTETSTRING_VALUE:
			lastValue = new Octetstring_Value(((Octetstring_Value) v1).getValue().substring(index * 2, (index + len) * 2));
			lastValue.copyGeneralProperties(this);
			break;
		case CHARSTRING_VALUE:
			lastValue = new Charstring_Value(((Charstring_Value) v1).getValue().substring(index, index + len));
			lastValue.copyGeneralProperties(this);
			break;
		case UNIVERSALCHARSTRING_VALUE:
			lastValue = new UniversalCharstring_Value(((UniversalCharstring_Value) v1).getValue().substring(index, index + len));
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
		return true;
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
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return !get_needs_conversion() && templateInstance1.hasSingleExpression() && value2.canGenerateSingleExpression()
				&& value3.canGenerateSingleExpression();
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		if (lastValue != null && lastValue != this) {
			lastValue.generateCodeExpression(aData, expression, true);
			return;
		}

		if (get_needs_conversion()) {
			ExpressionStruct tempExpression = new ExpressionStruct();
			generateCodeExpressionSubstring(aData, tempExpression);
			final IType templateGovernor = templateInstance1.getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
			final IType lastTemplateGovernor = templateGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());

			final String tempId1 = aData.getTemporaryVariableName();
			if (tempExpression.preamble.length() > 0) {
				expression.preamble.append(tempExpression.preamble);
			}

			expression.preamble.append(MessageFormat.format("{0} {1} = {2};\n", lastTemplateGovernor.getGenNameValue(aData, expression.preamble), tempId1, tempExpression.expression));
			if (tempExpression.postamble.length() > 0) {
				expression.postamble.append(tempExpression.postamble);
			}

			final String tempId2 = myGovernor.generateConversion(aData, lastTemplateGovernor, tempId1, true, expression);
			expression.expression.append(tempId2);
		} else {
			generateCodeExpressionSubstring(aData, expression);
		}
	}

	private void generateCodeExpressionSubstring(final JavaGenData aData, final ExpressionStruct expression) {
		final IValue lastValue2 = value2.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE, null);
		final IValue lastValue3 = value3.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE, null);

		final Type_type expressionType = templateInstance1.getExpressionReturntype(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
		switch (expressionType) {
		case TYPE_BITSTRING:
		case TYPE_HEXSTRING:
		case TYPE_OCTETSTRING:
		case TYPE_CHARSTRING:
		case TYPE_UCHARSTRING: {
			aData.addCommonLibraryImport("AdditionalFunctions");

			expression.expression.append("AdditionalFunctions.substr( ");
			final ITTCN3Template temp = templateInstance1.getTemplateBody();
			if (temp.isValue(CompilationTimeStamp.getBaseTimestamp())) {
				final IValue value = temp.getValue();
				value.generateCodeExpressionMandatory(aData, expression, true);
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
			expression.expression.append(')');
			break;
		}
		case TYPE_SEQUENCE_OF:
		case TYPE_SET_OF: {
			templateInstance1.generateCode(aData, expression, Restriction_type.TR_NONE);
			expression.expression.append(".substr( ");
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
			expression.expression.append(')');
			break;
		}
		default:
			ErrorReporter.INTERNAL_ERROR("FATAL ERROR while generating code for expression `" + getFullName() + "''");
			break;
		}
	}
}
