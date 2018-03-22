/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.TTCN3.types.CharString_Type.CharCoding;
import org.eclipse.titan.designer.AST.TTCN3.values.PredefFunc.DecodeException;
import org.eclipse.titan.designer.compiler.JavaGenData;

/**
 * Internal representation of a universal charstring.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class UniversalCharstring implements Comparable<UniversalCharstring> {

	private List<UniversalChar> value;

	/** true, if TTCN-3 string contains error */
	private boolean mErrorneous = false;

	/** the error message (if any) */
	private String mErrorMessage;

	public UniversalCharstring() {
		value = new ArrayList<UniversalChar>();
	}

	public UniversalCharstring(final UniversalChar character) {
		value = new ArrayList<UniversalChar>(1);
		value.add(character);
	}

	/**
	 * Constructor
	 * @param aValue TTCN-3 charstring value, it can contain escape characters
	 */
	public UniversalCharstring(final String string) {
		//TODO: make UTF-8 checking faster, converting each string to octetstring is not very effective (also on the C++ side)
		// Check for UTF8 encoding and decode it
		// in case the editor encoded the TTCN-3 file with UTF-8
		final StringBuilder octet_str = new StringBuilder();
		int len = string.length();
		for (int i = 0; i < len; ++i) {
			try {
				octet_str.append(PredefFunc.hexdigit_to_char((char) (string.charAt(i) / 16)));
				octet_str.append(PredefFunc.hexdigit_to_char((char) (string.charAt(i) % 16)));
			} catch (DecodeException e) {
				//cannot happen
			}
		}
		final CharCoding coding = PredefFunc.getCharCoding(octet_str.toString());
		if (CharCoding.UTF_8 == coding) {
			try {
				UniversalCharstring ucs = PredefFunc.decode_utf8(octet_str.toString(), CharCoding.UTF_8);
				value = ucs.value;
			} catch (Exception e) {
				mErrorneous = true;
				mErrorMessage = e.getMessage();
			}
			return;
		}

		final CharstringExtractor cs = new CharstringExtractor(string);
		if ( cs.isErrorneous() ) {
			mErrorneous = true;
			mErrorMessage = cs.getErrorMessage();
		}
		final String extracted = cs.getExtractedString();
		if ( extracted != null ) {
			value = new ArrayList<UniversalChar>(extracted.length());
			for (int i = 0; i < extracted.length(); i++) {
				value.add(new UniversalChar(extracted.charAt(i)));
			}
		}
	}

	/**
	 * @return if TTCN-3 string contains error
	 * NOTE: it can only be true if String constructor was used
	 */
	public boolean isErrorneous() {
		return mErrorneous;
	}

	/**
	 * @return the error message (if any)
	 */
	public String getErrorMessage() {
		return mErrorMessage;
	}

	public UniversalCharstring(final UniversalCharstring other) {
		value = new ArrayList<UniversalChar>(other.value.size());
		for (int i = 0; i < other.value.size(); i++) {
			value.add(other.value.get(i));
		}
	}

	private UniversalCharstring(final List<UniversalChar> value) {
		this.value = value;
	}

	public int length() {
		return value.size();
	}

	public UniversalChar get(final int index) {
		return value.get(index);
	}

	public String getString() {
		final StringBuilder builder = new StringBuilder(value.size());
		for (int i = 0; i < value.size(); i++) {
			builder.append((char)value.get(i).cell());
		}

		return builder.toString();
	}

	/**
	 * Creates and returns a string representation if the universal charstring.
	 *
	 * @return the string representation of the universal charstring.
	 * */
	public String getStringRepresentation() {
		final StringBuilder builder = new StringBuilder(value.size());
		for (int i = 0; i < value.size(); i++) {
			final UniversalChar tempChar = value.get(i);
			if (tempChar.group() == 0 && tempChar.plane() == 0 && tempChar.row() == 0) {
				builder.append((char) tempChar.cell());
			} else {
				builder.append("char(").append(tempChar.group()).append(',').append(tempChar.plane()).append(',')
				.append(tempChar.row()).append(',').append(tempChar.cell()).append(')');
			}
		}

		return builder.toString();
	}

	public UniversalCharstring substring(final int beginIndex) {
		return substring(beginIndex, value.size());
	}

	public UniversalCharstring substring(final int beginIndex, final int endIndex) {
		final List<UniversalChar> newList = new ArrayList<UniversalChar>(value.subList(beginIndex, endIndex));
		return new UniversalCharstring(newList);
	}

	public UniversalCharstring append(final String other) {
		for (int i = 0; i < other.length(); i++) {
			value.add(new UniversalChar(0, 0, 0, other.charAt(i)));
		}

		return this;
	}

	public UniversalCharstring append(final UniversalCharstring other) {
		if( other != null) {
			for (int i = 0; i < other.value.size(); i++) {
				value.add(other.value.get(i));
			}
		}

		return this;
	}

	public UniversalCharstring append(final UniversalChar uc) {
		if( uc != null) {
			value.add(uc);
		}

		return this;
	}

	public static boolean isCharstring(final String string) {
		if (string == null) {
			return true;
		}

		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) > 127) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Three way lexicographical comparison of universal character strings.
	 *
	 * @param other the string to be compared.
	 * @return the value 0 if the argument string is equal to this string; a
	 *         value less than 0 if this string is lexicographically less than
	 *         the string argument; and a value greater than 0 if this string is
	 *         lexicographically greater than the string argument.
	 */
	public int compareWith(final UniversalCharstring other) {
		if (this == other) {
			return 0;
		}

		UniversalChar actual;
		UniversalChar otherActual;
		for (int i = 0;; i++) {
			if (i == value.size()) {
				if (i == other.value.size()) {
					return 0;
				}

				return -1;
			} else if (i == other.value.size()) {
				return +1;
			}

			actual = value.get(i);
			otherActual = other.value.get(i);
			if (actual.group() > otherActual.group()) {
				return +1;
			} else if (actual.group() < otherActual.group()) {
				return -1;
			} else if (actual.plane() > otherActual.plane()) {
				return +1;
			} else if (actual.plane() < otherActual.plane()) {
				return -1;
			} else if (actual.row() > otherActual.row()) {
				return +1;
			} else if (actual.row() < otherActual.row()) {
				return -1;
			} else if (actual.cell() > otherActual.cell()) {
				return +1;
			} else if (actual.cell() < otherActual.cell()) {
				return -1;
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public int compareTo(final UniversalCharstring other) {
		return compareWith(other);
	}

	/**
	 * Checks if this universal character string equals in meaning with the one provided.
	 *
	 * @param other the one to compare against.
	 *
	 * @return true if they mean the same symbol list.
	 * */
	public boolean checkEquality(final UniversalCharstring other) {
		if (this == other) {
			return true;
		}

		if (value.size() != other.value.size()) {
			return false;
		}

		for (int i = 0; i < value.size(); i++) {
			if (!value.get(i).checkEquality(other.value.get(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if this universal character string equals in meaning with the one provided.
	 *
	 * @param other the one to compare against.
	 *
	 * @return true if they mean the same symbol list.
	 * */
	public boolean checkEquality(final String other) {
		if (value.size() != other.length()) {
			return false;
		}

		for (int i = 0; i < value.size(); i++) {
			if (!value.get(i).checkEquality(other.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the hashcode for the Universal character string.
	 * Useful in case it is stored in a hashmap.
	 *
	 * @return  a hash code value for this universal character  string.
	 */
	@Override
	public int hashCode() {
		int h = 0;
		UniversalChar temp;
		for (int i = 0, size = value.size(); i < size; i++) {
			temp = value.get(i);
			h = 31 * h + temp.group();
			h = 31 * h + temp.plane();
			h = 31 * h + temp.row();
			h = 31 * h + temp.cell();
		}

		return h;
	}

	@Override
	/** {@inheritDoc} */
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof UniversalCharstring) {
			return checkEquality((UniversalCharstring) obj);
		}

		return false;
	}

	/**
	 * Returns the Java expression to be used in the generated code.
	 *
	 * get_single_expr in the compiler
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * */
	public StringBuilder generateSingleExpression(final JavaGenData aData) {
		final StringBuilder result = new StringBuilder();
		result.append("new TitanUniversalChar[]{");

		for (int i = 0, size = value.size(); i < size; i++) {
			final UniversalChar temp = value.get(i);

			result.append(temp.generateSingleExpression(aData));
			if(i != size - 1) {
				result.append(", ");
			}
		}

		result.append("}");

		return result;
	}
}
