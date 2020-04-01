/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
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
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.RawAST;
import org.eclipse.titan.designer.AST.TTCN3.templates.DecodeMatch_template;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.ValueRange;
import org.eclipse.titan.designer.AST.TTCN3.templates.Value_Range_Template;
import org.eclipse.titan.designer.AST.TTCN3.types.subtypes.SubType;
import org.eclipse.titan.designer.AST.TTCN3.values.Charstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value.Operation_type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.BuildTimestamp;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * character string type (TTCN-3).
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class CharString_Type extends Type {
	private static final String CHARSTRING = "charstring";
	public static final String CHARSTRINGVALUEEXPECTED = "Character string value was expected";
	private static final String TEMPLATENOTALLOWED = "{0} cannot be used for type `{1}''";
	private static final String INCORRECTBOUNDARIES = "The lower boundary is higher than the upper boundary";
	private static final String INFINITEBOUNDARYERROR = "The {0} boundary must be a charstring value";
	private static final String TOOLONGBOUNDARYERROR = "The {0} boundary must be a charstring value containing a single character.";

	public static enum CharCoding {
		UNKNOWN("<unknown>"),
		ASCII("ASCII"),
		UTF_8("UTF-8"),
		UTF16("UTF-16"),
		UTF16BE("UTF-16BE"),
		UTF16LE("UTF-16LE"),
		UTF32("UTF-32"),
		UTF32BE("UTF-32BE"),
		UTF32LE("UTF-32LE");

		final String name;

		CharCoding(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetype() {
		return Type_type.TYPE_CHARSTRING;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getProposalDescription(final StringBuilder builder) {
		return builder.append(CHARSTRING);
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "charstring.gif";
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

		switch (temp.getTypetype()) {
		case TYPE_CHARSTRING:
		case TYPE_NUMERICSTRING:
		case TYPE_PRINTABLESTRING:
		case TYPE_IA5STRING:
		case TYPE_VISIBLESTRING:
		case TYPE_UTCTIME:
		case TYPE_GENERALIZEDTIME:
			return true;
		default:
			return false;
		}
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
		return CHARSTRING;
	}

	@Override
	/** {@inheritDoc} */
	public SubType.SubType_type getSubtypeType() {
		return SubType.SubType_type.ST_CHARSTRING;
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

		switch (last.getValuetype()) {
		case UNDEFINED_BLOCK:
			last = last.setValuetype(timestamp, Value_type.CHARSYMBOLS_VALUE);
			if (last.getIsErroneous(timestamp)) {
				return selfReference;
			}

			last.setValuetype(timestamp, Value_type.CHARSTRING_VALUE);
			break;
		case CHARSYMBOLS_VALUE:
		case UNIVERSALCHARSTRING_VALUE:
			last.setValuetype(timestamp, Value_type.CHARSTRING_VALUE);
			break;
		case CHARSTRING_VALUE:
		case ISO2022STRING_VALUE:
			break;
		case EXPRESSION_VALUE:
		case MACRO_VALUE:
			// already checked
			break;
		default:
			value.getLocation().reportSemanticError(CHARSTRINGVALUEEXPECTED);
			value.setIsErroneous(true);
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
		checkThisTemplateString(timestamp, this, template, isModified, implicitOmit, lhs);

		return false;
	}

	/**
	 * Checks if the provided template is valid for the provided type.
	 * <p>
	 * The type must be equivalent with the TTCN-3 charstring type
	 *
	 * @param timestamp the time stamp of the actual semantic check cycle.
	 * @param type the charstring type used for the check.
	 * @param template the template to be checked by the type.
	 * @param isModified true if the template is a modified template.
	 * @param implicitOmit true if the implicit omit optional attribute was set for the template, false otherwise.
	 * @param lhs the assignment to check against.
	 *
	 * @return true if the value contains a reference to lhs
	 * */
	public static boolean checkThisTemplateString(final CompilationTimeStamp timestamp, final Type type,
			final ITTCN3Template template, final boolean isModified, final boolean implicitOmit, final Assignment lhs) {
		template.setMyGovernor(type);

		boolean selfReference = false;
		switch (template.getTemplatetype()) {
		case VALUE_RANGE: {
			final ValueRange range = ((Value_Range_Template) template).getValueRange();
			final IValue lower = checkBoundary(timestamp, type, range.getMin(), template, "lower");
			final IValue upper = checkBoundary(timestamp, type, range.getMax(), template, "upper");
			range.setTypeType(type.getTypetypeTtcn3());

			if (lower != null && upper != null) {
				if (((Charstring_Value) lower).getValue().compareTo(((Charstring_Value) upper).getValue()) > 0) {
					template.getLocation().reportSemanticError(INCORRECTBOUNDARIES);
				}
			}
			break;
		}
		case CSTR_PATTERN:
			// TODO implement later once patterns become supported
			break;
		case DECODE_MATCH:
			selfReference = ((DecodeMatch_template)template).checkThisTemplateString(timestamp, type, implicitOmit, lhs);
			break;
		default:
			template.getLocation().reportSemanticError(
					MessageFormat.format(TEMPLATENOTALLOWED, template.getTemplateTypeName(), type.getTypename()));
			break;
		}

		return selfReference;
	}

	private static IValue checkBoundary(final CompilationTimeStamp timestamp, final Type type, final Value value,
			final ITTCN3Template template, final String which) {
		if (value == null) {
			template.getLocation().reportSemanticError(MessageFormat.format(INFINITEBOUNDARYERROR, which));
			return null;
		}

		value.setMyGovernor(type);
		IValue temp = type.checkThisValueRef(timestamp, value);
		type.checkThisValue(timestamp, temp, null, new ValueCheckingOptions(Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false, false, true, false, false));
		temp = temp.getValueRefdLast(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, null);
		switch (temp.getValuetype()) {
		case CHARSTRING_VALUE:
			if (((Charstring_Value) temp).getValueLength() != 1) {
				value.getLocation().reportSemanticError(MessageFormat.format(TOOLONGBOUNDARYERROR, which));
			}
			break;
		default:
			temp = null;
			break;
		}

		return temp;
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
				if (rawAttribute.fieldlength == 0) {
					rawAttribute.fieldlength = restrictionLength * 8;
					rawAttribute.length_restriction = -1;
				} else {
					rawAttribute.length_restriction = restrictionLength;
				}
			}
		}

		checkJson(timestamp);
		//TODO add checks for other encodings.
	}

	@Override
	/** {@inheritDoc} */
	public void checkJsonDefault() {
		final String defaultValue = jsonAttribute.default_value;
		final int length = defaultValue.length();
		int i = 0;
		while (i < length) {
			final char value = defaultValue.charAt(i);
			if ((byte)value < 0) {
				getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
				return;
			}
			if (value == '\\') {
				if (i == length-1) {
					getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
					return;
				}
				
				switch (value) {
				case '\\':
				case '\"':
				case 'n':
				case 't':
				case 'r':
				case 'f':
				case 'b':
				case '/':
					break;
				case 'u':
				{
					if (i + 4 >= length) {
						getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
						return;
					}
					if (defaultValue.charAt(i+1) != '0' || defaultValue.charAt(i+2) != '0' ||
							defaultValue.charAt(i+3) < '0' || defaultValue.charAt(i+3) > '7') {
						getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
						return;
					}
					final char nextChar = defaultValue.charAt(i+4);
					if ((nextChar < '0' || nextChar > '9') &&
							(nextChar < 'a' || nextChar > 'f') &&
							(nextChar < 'A' || nextChar > 'F')) {
						getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
						return;
					}
					i += 4;
					break;
				}
				default:
					getLocation().reportSemanticError(MessageFormat.format("Invalid {0} JSON default value", getTypename()));
					return;
				}
			}
			i++;
		}//while
	}

	@Override
	/** {@inheritDoc} */
	public boolean canHaveCoding(final CompilationTimeStamp timestamp, final MessageEncoding_type coding) {
		if (coding == MessageEncoding_type.BER) {
			return hasEncoding(timestamp, MessageEncoding_type.BER, null);
		}

		switch (coding) {
		case RAW:
		case TEXT:
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
			if (subreferences.size() > actualSubReference + 1) {
				subreference.getLocation().reportSemanticError(ArraySubReference.INVALIDSTRINGELEMENTINDEX);
				return null;
			} else if (subreferences.size() == actualSubReference + 1) {
				reference.setStringElementReferencing();
			}

			final Value indexValue = ((ArraySubReference) subreference).getValue();
			checkStringIndex(timestamp, indexValue, expectedIndex, refChain);

			return this;
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
	public int getLengthMultiplier() {
		return 8;
	}

	@Override
	/** {@inheritDoc} */
	public int getRawLength(final BuildTimestamp timestamp) {
		if (rawAttribute != null && rawAttribute.fieldlength > 0) {
			return rawAttribute.fieldlength;
		}

		return -1;
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		final List<ISubReference> subreferences = declarationCollector.getReference().getSubreferences();
		if (subreferences.size() <= i) {
			return;
		}

		final ISubReference subreference = subreferences.get(i);
		if (Subreference_type.arraySubReference.equals(subreference.getReferenceType())
				&& subreferences.size() == i + 1) {
			declarationCollector.addDeclaration(CHARSTRING, location, this);

		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean generatesOwnClass(JavaGenData aData, StringBuilder source) {
		return needsAlias();
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return;
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		if(needsAlias()) {
			final String ownName = getGenNameOwn();
			source.append(MessageFormat.format("\tpublic static class {0} extends {1} '{'\n", ownName, getGenNameValue(aData, source)));

			final StringBuilder descriptor = new StringBuilder();
			generateCodeTypedescriptor(aData, source, descriptor);
			source.append(descriptor);

			source.append("\t}\n");

			source.append(MessageFormat.format("\tpublic static class {0}_template extends {1} '{' '}'\n", ownName, getGenNameTemplate(aData, source)));
		} else {
			generateCodeTypedescriptor(aData, source, null);
		}

		if (hasDoneAttribute()) {
			generateCodeDone(aData, source);
		}
		if (subType != null) {
			subType.generateCode(aData, source);
		}

		generateCodeForCodingHandlers(aData, source);
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameValue(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanCharString" );

		return "TitanCharString";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTemplate(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanCharString_template" );

		return "TitanCharString_template";
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeIsPresentBoundChosen(final JavaGenData aData, final ExpressionStruct expression, final List<ISubReference> subreferences,
			final int subReferenceIndex, final String globalId, final String externalId, final boolean isTemplate, final Operation_type optype, final String field, final Scope targetScope) {
		generateCodeIspresentBound_forStrings(aData, expression, subreferences, subReferenceIndex, globalId, externalId, isTemplate, optype, field, targetScope);
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTypeDescriptor(final JavaGenData aData, final StringBuilder source) {
		if (rawAttribute != null || jsonAttribute != null ||
				hasVariantAttributes(CompilationTimeStamp.getBaseTimestamp())
				|| hasEncodeAttribute("JSON")) {
			if (needsAlias()) {
				String baseName = getGenNameOwn(aData);
				return baseName + "." + getGenNameOwn();
			} else if (getParentType() != null) {
				final IType parentType = getParentType();
				if (parentType.generatesOwnClass(aData, source)) {
					return parentType.getGenNameOwn(aData) + "." + getGenNameOwn();
				}

				return getGenNameOwn(aData);
			}

			return getGenNameOwn(aData);
		}

		if (needsAlias()) {
			String baseName = getGenNameOwn(aData);
			return baseName + "." + getGenNameOwn();
		}

		aData.addBuiltinTypeImport( "Base_Type" );
		return "Base_Type.TitanCharString";
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

			return "RAW.TitanCharString_raw_";
		} else if (needsAlias()) {
			return getGenNameOwn(aData) + "." + getGenNameOwn() + "_raw_";
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
		if (needsOwnJsonDescriptor(aData)) {
			return getGenNameOwn(aData) + "_json_";
		}

		aData.addBuiltinTypeImport( "JSON" );

		return "JSON.TitanCharString_json_";
	}


	@Override
	/** {@inheritDoc} */
	public String generateConversion(final JavaGenData aData, final IType fromType, final String fromName, final boolean forValue, final ExpressionStruct expression) {
		aData.addBuiltinTypeImport( "TitanCharString" );

		return MessageFormat.format("TitanCharString.convert_to_CharString({0})", fromName);
	}
}
