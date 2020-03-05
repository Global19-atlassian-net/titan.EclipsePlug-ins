/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Coding_Type;
import org.eclipse.titan.designer.AST.IType.MessageEncoding_type;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.ASN1.values.ISO2022String_Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.CharString_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class Charstring_Value extends Value {
	private static final String QUADRUPLEPROBLEM =
			"This string value cannot contain multiple-byte characters, but it has quadruple char({0},{1},{2},{3}) at index {4}";
	private static final String CHARACTERCODEPROBLEM =
			"This string value may not contain characters with code higher than 127, but it has character with code {0} at index {1}";

	private final String value;

	public Charstring_Value(final String value) {
		this.value = value;
	}

	/**
	 * function used to convert a universal charstring value into a charstring value if possible.
	 *
	 * @param timestamp
	 *                the timestamp of the actual build cycle
	 * @param original
	 *                the value to be converted
	 * */
	public static Charstring_Value convert(final CompilationTimeStamp timestamp, final UniversalCharstring_Value original) {
		final UniversalCharstring oldString = original.getValue();
		Charstring_Value target;

		if (oldString == null) {
			original.setIsErroneous(true);
			target = new Charstring_Value(null);
			target.copyGeneralProperties(original);
			return target;
		}

		for (int i = 0; i < oldString.length() && !original.getIsErroneous(timestamp); i++) {
			final UniversalChar uchar = oldString.get(i);
			if (uchar.group() != 0 || uchar.plane() != 0 || uchar.row() != 0) {
				original.getLocation().reportSemanticError(MessageFormat.format(QUADRUPLEPROBLEM, uchar.group(), uchar.plane(), uchar.row(), uchar.cell(), i));
				original.setIsErroneous(true);
			} else if (uchar.cell() > 127) {
				original.getLocation().reportSemanticWarning(MessageFormat.format(CHARACTERCODEPROBLEM, uchar.cell(), i));
			}
		}

		if (original.getIsErroneous(timestamp)) {
			target = new Charstring_Value(null);
		} else {
			target = new Charstring_Value(oldString.getString());
		}

		target.copyGeneralProperties(original);

		return target;
	}

	@Override
	/** {@inheritDoc} */
	public Value_type getValuetype() {
		return Value_type.CHARSTRING_VALUE;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_CHARSTRING;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append('\"').append(value).append('"');

		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public IType getExpressionGovernor(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (myGovernor != null) {
			return myGovernor;
		}

		return new CharString_Type();
	}

	@Override
	/** {@inheritDoc} */
	public IValue getReferencedSubValue(final CompilationTimeStamp timestamp, final Reference reference,
			final int actualSubReference, final IReferenceChain refChain) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return this;
		}

		final IType type = myGovernor.getTypeRefdLast(timestamp);
		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference: {
			final Value arrayIndex = ((ArraySubReference) subreference).getValue();
			final IValue valueIndex = arrayIndex.getValueRefdLast(timestamp, refChain);
			if (!valueIndex.isUnfoldable(timestamp)) {
				if (Value_type.INTEGER_VALUE.equals(valueIndex.getValuetype())) {
					final int index = ((Integer_Value) valueIndex).intValue();
					return getStringElement(index, arrayIndex.getLocation());
				}

				arrayIndex.getLocation().reportSemanticError(ArraySubReference.INTEGERINDEXEXPECTED);
				return null;
			}
			return null;
		}
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(
					FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId().getDisplayName(), type.getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(ParameterisedSubReference.INVALIDVALUESUBREFERENCE);
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		return false;
	}

	public String getValue() {
		return value;
	}

	public int getValueLength() {
		if (value == null || isErroneous) {
			return 0;
		}

		return value.length();
	}

	public Charstring_Value getStringElement(final int index, final Location location) {
		if (value == null) {
			return null;
		}

		if (index < 0) {
			location.reportSemanticError(MessageFormat.format(Bitstring_Value.NEGATIVEINDEX, index));
			return null;
		} else if (index >= value.length()) {
			location.reportSemanticError(MessageFormat.format(Bitstring_Value.INDEXOWERFLOW, index, value.length()));
			return null;
		}

		final Charstring_Value result = new Charstring_Value(value.substring(index, index + 1));
		result.setMyGovernor(myGovernor);
		result.setMyScope(myScope);
		return result;
	}

	@Override
	/** {@inheritDoc} */
	public Value setValuetype(final CompilationTimeStamp timestamp, final Value_type newType) {
		switch (newType) {
		case UNIVERSALCHARSTRING_VALUE:
			return new UniversalCharstring_Value(this);
		case ISO2022STRING_VALUE:
			return new ISO2022String_Value(this);
		default:
			return super.setValuetype(timestamp, newType);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = other.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		switch (last.getValuetype()) {
		case CHARSTRING_VALUE:
			return value.equals(((Charstring_Value) last).getValue());
		case UNIVERSALCHARSTRING_VALUE:
			return value.equals(((UniversalCharstring_Value) last).getValue().getString());
		case ISO2022STRING_VALUE:
			return value.equals(((ISO2022String_Value) last).getValueISO2022String());
		default:
			return false;
		}
	}

	/**
	 * Checks that the value is a valid dynamic encoding string for the specified type.
	 *
	 * @param timestamp
	 *                the time stamp of the actual semantic check cycle.
	 * @param type
	 *                the type to check against
	 *
	 * @return true if the provided type does not have a coding with this
	 *         name.
	 * */
	public boolean checkDynamicEncodingString(final CompilationTimeStamp timestamp, final IType type) {
		final IType ct = type.getTypeWithCodingTable(timestamp, false);
		if (ct == null) {
			return false;
		}

		boolean errorFound = true;
		final MessageEncoding_type coding = Type.getEncodingType(value);
		final boolean builtIn = !coding.equals(MessageEncoding_type.PER) && !coding.equals(MessageEncoding_type.CUSTOM);
		final List<Coding_Type> codingTable = ct.getCodingTable();
		for (int i = 0; i < codingTable.size(); i++) {
			final Coding_Type temp = codingTable.get(i);
			if (builtIn == temp.builtIn && ((builtIn && coding == temp.builtInCoding) ||
					(!builtIn && value.equals(temp.customCoding.name)))) {
				errorFound = false;
				break;
			}
		}

		return errorFound;
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIsvalue(final boolean fromSequence) {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		// no members
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean returnsNative() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateSingleExpression(final JavaGenData aData) {
		//TODO register as module level charstring literal and return the literal's name
		final StringBuilder result = new StringBuilder();
		result.append(MessageFormat.format("\"{0}\"", get_stringRepr()));

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		source.append(MessageFormat.format("{0}.operator_assign(\"{1}\");\n", name, get_stringRepr()));

		lastTimeGenerated = aData.getBuildTimstamp();

		return source;
	}

	/**
	 * Converts charstring content to its equivalent escaped java string.
	 * The result will be a valid java string literal, which can be inserted to the generated java code.
	 * It is typically used for charstring patterns.
	 *
	 * @return escaped java string
	 */
	private String get_stringRepr() {
		return get_stringRepr(value);
	}

	private static boolean is_printable(final char c) {
		// originally if (isprint(c))
		if (!Character.isISOControl(c)) {
			return true;
		} else {
			switch (c) {
			case 0x07: //'\a' Audible bell
			case '\b':
			case '\t':
			case '\n':
			case 0x0b: // '\v' Vertical tab
			case '\f':
			case '\r':
				return true;
			default:
				return false;
			}
		}
	}

	/**
	 * Converts character to escaped string in the format, which is used in generated java code.
	 * Originally string::append_stringRepr(),
	 * but changed to create escaped string for generated java code
	 * @param str output converted (escaped) string
	 * @param c input character to convert
	 */
	private static void append_stringRepr(final StringBuilder str, final char c) {
		switch (c) {
		case 0x07: //'\a' Audible bell
			str.append("\\u0007");
			break;
		case '\b':
			str.append("\\b");
			break;
		case '\t':
			str.append("\\t");
			break;
		case '\n':
			str.append("\\n");
			break;
		case 0x0b: // '\v' Vertical tab
			str.append("\\u000b");
			break;
		case '\f':
			str.append("\\f");
			break;
		case '\r':
			str.append("\\r");
			break;
		case '\\':
			str.append("\\\\");
			break;
		case '"':
			str.append("\\\"");
			break;
		default:
			//NOTE: c is printable, it was checked in get_stringRepr()
			str.append(c);
			break;
		}
	}

	/**
	 * Converts string to escaped string in the format, which is used in generated java code.
	 * Originally string::get_stringRepr(),
	 * but changed to create escaped string for generated java code
	 * NOTE: beginning and closing quotes are not added
	 * @param src unescaped string to convert
	 * @return converted (escaped) string in java format
	 */
	public static String get_stringRepr(final String src)	{
		final StringBuilder ret_val = new StringBuilder();
		for (int i = 0; i < src.length(); i++) {
			final char c = src.charAt(i);
			if (is_printable(c)) {
				// the actual character is printable
				append_stringRepr(ret_val, c);
			} else {
				ret_val.append(String.format("\\u%04x", (int)c));
			}
		}
		return ret_val.toString();
	}
}
