/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Identifier.Identifier_type;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.ASN1.ASN1Type;
import org.eclipse.titan.designer.AST.ASN1.IASN1Type;
import org.eclipse.titan.designer.AST.ASN1.Type_Assignment;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.RawAST;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template.Template_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ValueRange;
import org.eclipse.titan.designer.AST.TTCN3.templates.Value_Range_Template;
import org.eclipse.titan.designer.AST.TTCN3.types.subtypes.SubType;
import org.eclipse.titan.designer.AST.TTCN3.values.Real_Value;
import org.eclipse.titan.designer.compiler.BuildTimestamp;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * @author Kristof Szabados
 * */
public final class Float_Type extends ASN1Type {
	private static final String REALVALUEEXPECTED = "REAL value was expected";
	private static final String FLOATVALUEEXPECTED = "float value was expected";
	private static final String TEMPLATENOTALLOWED = "{0} cannot be used for type `float''";
	private static final String LENGTHRESTRICTIONNOTALLOWED = "Length restriction is not allowed for type `float''";
	private static final String INCORRECTBOUNDARIES = "The lower boundary is higher than the upper boundary";

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetype() {
		return Type_type.TYPE_REAL;
	}

	@Override
	/** {@inheritDoc} */
	public IASN1Type newInstance() {
		return new Float_Type();
	}

	@Override
	/** {@inheritDoc} */
	public boolean isCompatible(final CompilationTimeStamp timestamp, final IType otherType, final TypeCompatibilityInfo info,
			final TypeCompatibilityInfo.Chain leftChain, final TypeCompatibilityInfo.Chain rightChain) {
		check(timestamp);
		otherType.check(timestamp);
		final IType temp = otherType.getTypeRefdLast(timestamp);
		if (getIsErroneous(timestamp) || temp.getIsErroneous(timestamp)) {
			return true;
		}

		return Type_type.TYPE_REAL.equals(temp.getTypetype());
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetypeTtcn3() {
		if (isErroneous) {
			return Type_type.TYPE_UNDEFINED;
		}

		return getTypetype();
	}

	@Override
	/** {@inheritDoc} */
	public String getTypename() {
		return "float";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "float.gif";
	}

	@Override
	/** {@inheritDoc} */
	public SubType.SubType_type getSubtypeType() {
		return SubType.SubType_type.ST_FLOAT;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		initAttributes(timestamp);

		if (constraints != null) {
			constraints.check(timestamp);
		}

		checkSubtypeRestrictions(timestamp);

		if (myScope != null) {
			checkEncode(timestamp);
			checkVariants(timestamp);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisValue(final CompilationTimeStamp timestamp, final IValue value, final Assignment lhs, final ValueCheckingOptions valueCheckingOptions) {
		final boolean selfReference = super.checkThisValue(timestamp, value, lhs, valueCheckingOptions);

		IValue last = value.getValueRefdLast(timestamp, valueCheckingOptions.expected_value, null);
		if (last == null || last.getIsErroneous(timestamp)) {
			return selfReference;
		}

		// already handled ones
		switch (value.getValuetype()) {
		case OMIT_VALUE:
		case REFERENCED_VALUE:
			return selfReference;
		case UNDEFINED_LOWERIDENTIFIER_VALUE:
			if (Value_type.REFERENCED_VALUE.equals(last.getValuetype())) {
				return selfReference;
			}
			break;
		default:
			break;
		}

		if (value.isAsn()) {
			if (Value_type.REFERENCED_VALUE.equals(value.getValuetype())) {
				final IType lastType = last.getMyGovernor().getTypeRefdLast(timestamp);
				if (!lastType.getIsErroneous(timestamp) && !Type_type.TYPE_REAL.equals(lastType.getTypetype())) {
					value.getLocation().reportSemanticError(REALVALUEEXPECTED);
					value.setIsErroneous(true);
					return selfReference;
				}
			}
			switch (last.getValuetype()) {
			case REAL_VALUE:
				break;
			case UNDEFINED_BLOCK: {
				last = last.setValuetype(timestamp, Value_type.SEQUENCE_VALUE);
				final Identifier identifier = new Identifier(Identifier_type.ID_ASN, "REAL");
				final Assignment assignment = getMyScope().getAssignmentsScope().getLocalAssignmentByID(timestamp, identifier);
				((Type_Assignment) assignment).getType(timestamp).checkThisValue(
						timestamp, last, null, new ValueCheckingOptions(Expected_Value_type.EXPECTED_CONSTANT,
								false, false, true, false, valueCheckingOptions.str_elem));
				last = last.setValuetype(timestamp, Value_type.REAL_VALUE);
				break; }
			case INTEGER_VALUE:
				last.setValuetype(timestamp, Value_type.REAL_VALUE);
				break;
			case EXPRESSION_VALUE:
			case MACRO_VALUE:
				// already checked
				break;
			default:
				last.getLocation().reportSemanticError(REALVALUEEXPECTED);
				last.setIsErroneous(true);
				break;
			}
		} else {
			switch (last.getValuetype()) {
			case REAL_VALUE:
				break;
			case EXPRESSION_VALUE:
			case MACRO_VALUE:
				// already checked
				break;
			default:
				value.getLocation().reportSemanticError(FLOATVALUEEXPECTED);
				value.setIsErroneous(true);
			}
		}

		if (valueCheckingOptions.sub_check) {
			//there is no parent type to check
			if (subType != null) {
				subType.checkThisValue(timestamp, last);
			}
		}

		value.setLastTimeChecked(timestamp);

		return selfReference;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisTemplate(final CompilationTimeStamp timestamp, final ITTCN3Template template,
			final boolean isModified, final boolean implicitOmit, final Assignment lhs) {
		registerUsage(template);
		template.setMyGovernor(this);

		if (getIsErroneous(timestamp)) {
			return false;
		}

		if (Template_type.VALUE_RANGE.equals(template.getTemplatetype())) {
			final ValueRange range = ((Value_Range_Template) template).getValueRange();
			final IValue lower = checkBoundary(timestamp, range.getMin());
			final IValue upper = checkBoundary(timestamp, range.getMax());
			range.setTypeType(getTypetypeTtcn3());

			if (lower != null && upper != null) {
				if (((Real_Value) lower).getValue() > ((Real_Value) upper).getValue()) {
					template.getLocation().reportSemanticError(INCORRECTBOUNDARIES);
				}
			}
			if (lower != null && range.getMax() == null) {
				checkBoundaryInfinity(timestamp, lower, true);
			}
			if (range.getMin() == null && upper != null) {
				checkBoundaryInfinity(timestamp, upper, false);
			}
		} else {
			template.getLocation().reportSemanticError(MessageFormat.format(TEMPLATENOTALLOWED, template.getTemplateTypeName()));
		}

		if (template.getLengthRestriction() != null) {
			template.getLocation().reportSemanticError(LENGTHRESTRICTIONNOTALLOWED);
		}

		return false;
	}

	private IValue checkBoundary(final CompilationTimeStamp timestamp, final Value value) {
		if (value == null) {
			return null;
		}

		value.setMyGovernor(this);
		IValue temp = checkThisValueRef(timestamp, value);
		checkThisValue(timestamp, temp, null, new ValueCheckingOptions(Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false, false, true, false, false));
		temp = temp.getValueRefdLast(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, null);
		switch (temp.getValuetype()) {
		case REAL_VALUE:
			break;
		default:
			temp = null;
			break;
		}

		return temp;
	}

	private void checkBoundaryInfinity(final CompilationTimeStamp timestamp, final IValue value, final boolean isUpper) {
		if (value == null) {
			return;
		}

		value.setMyGovernor(this);
		IValue temp = checkThisValueRef(timestamp, value);
		checkThisValue(timestamp, temp, null, new ValueCheckingOptions(Expected_Value_type.EXPECTED_STATIC_VALUE, false, false, true, false, false));
		temp = temp.getValueRefdLast(timestamp, Expected_Value_type.EXPECTED_STATIC_VALUE, null);
		if (temp.getValuetype() == Value_type.OMIT_VALUE) {
			value.getLocation().reportSemanticError("`omit' value is not allowed in this context");
			value.setIsErroneous(true);
			return;
		}
		if (subType != null) {
			//FIXME implement subtype check
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkCodingAttributes(final CompilationTimeStamp timestamp, final IReferenceChain refChain) {
		//check raw attributes
		if (subType != null) {
			final int restrictionLength = subType.get_length_restriction();
			if (restrictionLength != -1) {
				if (rawAttribute == null) {
					rawAttribute = new RawAST(getDefaultRawFieldLength());
				}

				rawAttribute.length_restriction = restrictionLength;
			}
		}
		if (rawAttribute != null) {
			if (rawAttribute.fieldlength != 64 && rawAttribute.fieldlength != 32) {
				getLocation().reportSemanticError(MessageFormat.format("Invalid length ({0}) specified in parameter FIELDLENGTH for float type `{1}''. The FIELDLENGTH must be single (32) or double (64)", rawAttribute.fieldlength, getFullName()));
			}
		}

		checkJson(timestamp);
		//TODO add checks for other encodings.
	}

	@Override
	/** {@inheritDoc} */
	public void checkJsonDefault() {
		final String defaultValue = jsonAttribute.default_value;
		if (defaultValue.matches("-?infinity|not_a_number") || defaultValue.length() < 1) {
			// special float values => skip the rest of the check
			return;
		}

		boolean first_digit = false; // first non-zero digit reached
		boolean zero = false; // first zero digit reached
		boolean decimal_point = false; // decimal point (.) reached
		boolean exponent_mark = false; // exponential mark (e or E) reached
		boolean exponent_sign = false; // sign of the exponential (- or +) reached
		boolean error = false;

		int i = (defaultValue.charAt(0) == '-') ? 1 : 0;
		while(!error && i < defaultValue.length()) {
			final char value = defaultValue.charAt(i);
			switch (value) {
			case '.':
				if (decimal_point || exponent_mark || (!first_digit && !zero)) {
					error = true;
				}
				decimal_point = true;
				first_digit = false;
				zero = false;
				break;
			case 'e':
			case 'E':
				if (exponent_mark || (!first_digit && !zero)) {
					error = true;
				}
				exponent_mark = true;
				first_digit = false;
				zero = false;
				break;
			case '0':
				if (!first_digit && (exponent_mark || (!decimal_point && zero))) {
					error = true;
				}
				zero = true;
				break;
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				if (!first_digit && zero && (!decimal_point || exponent_mark)) {
					error = true;
				}
				first_digit = true;
				break;
			case '-':
			case '+':
				if (exponent_sign || !exponent_mark || zero || first_digit) {
					error = true;
				}
				exponent_sign = true;
				break;
			default:
				error = true;
			}
			++i;
		}

		if (!first_digit && !zero) {
			getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean canHaveCoding(final CompilationTimeStamp timestamp, final MessageEncoding_type coding) {
		if (coding == MessageEncoding_type.BER) {
			return hasEncoding(timestamp, MessageEncoding_type.BER, null);
		}

		switch (coding) {
		case RAW:
		case JSON:
		case XER:
			return true;
		default:
			return false;
		}
	}

	@Override
	/** {@inheritDoc} */
	public IType getFieldType(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference,
			final Expected_Value_type expectedIndex, final IReferenceChain refChain, final boolean interruptIfOptional) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (subreferences.size() <= actualSubReference) {
			return this;
		}

		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(ArraySubReference.INVALIDSUBREFERENCE, getTypename()));
			return null;
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((ParameterisedSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public int getDefaultRawFieldLength() {
		return 64;
	}

	@Override
	/** {@inheritDoc} */
	public int getRawLength(final BuildTimestamp timestamp) {
		if (rawAttribute != null) {
			return rawAttribute.fieldlength;
		}

		return getDefaultRawFieldLength();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getProposalDescription(final StringBuilder builder) {
		return builder.append("float");
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return;
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		generateCodeTypedescriptor(aData, source);
		if(needsAlias()) {
			final String ownName = getGenNameOwn();
			source.append(MessageFormat.format("\tpublic static class {0} extends {1} '{' '}'\n", ownName, getGenNameValue(aData, source)));
			source.append(MessageFormat.format("\tpublic static class {0}_template extends {1} '{' '}'\n", ownName, getGenNameTemplate(aData, source)));
		}
		if (!isAsn()) {
			if (hasDoneAttribute()) {
				generateCodeDone(aData, source);
			}
			if (subType != null) {
				subType.generateCode(aData, source);
			}
		}

		generateCodeForCodingHandlers(aData, source);
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameValue(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanFloat" );
		return "TitanFloat";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTemplate(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanFloat_template" );
		return "TitanFloat_template";
	}

	@Override
	/** {@inheritDoc} */
	public String internalGetGenNameTypeDescriptor(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "Base_Type" );
		return "Base_Type.TitanFloat";
	}

	@Override
	/** {@inheritDoc} */
	public boolean needsOwnRawDescriptor(final JavaGenData aData) {
		return rawAttribute != null;
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameRawDescriptor(final JavaGenData aData, final StringBuilder source) {
		if (rawAttribute == null) {
			aData.addBuiltinTypeImport( "RAW" );

			return "RAW.TitanFloat_raw_";
		} else {
			return getGenNameOwn(aData) + "_raw_";
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean needsOwnJsonDescriptor(final JavaGenData aData) {
		return !((jsonAttribute == null || jsonAttribute.empty()) && (getOwnertype() != TypeOwner_type.OT_RECORD_OF || getParentType().getJsonAttribute() == null
				|| !getParentType().getJsonAttribute().as_map));
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameJsonDescriptor(final JavaGenData aData, final StringBuilder source) {
		if (!needsOwnJsonDescriptor(aData)) {
			aData.addBuiltinTypeImport( "JSON" );

			return "JSON.TitanFloat_json_";
		} else {
			return getGenNameOwn(aData) + "_json_";
		}
	}

}
