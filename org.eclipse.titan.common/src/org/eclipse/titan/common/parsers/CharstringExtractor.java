/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.common.parsers;

/**
 * Extracts TTCN-3 charstring
 * 
 * @author Arpad Lovassy
 */
public class CharstringExtractor {

	// Error messages
	private static final String INVALID_ESCAPE_SEQUENCE = "Invalid escape sequence: ";

	/** true, if TTCN-3 string contains error */
	private boolean mErroneous = false;

	/** the value string of the TTCN-3 string */
	private final String mExtractedString;

	/** the error message (if any) */
	private String mErrorMessage;

	/**
	 * Constructor
	 * 
	 * @param aTtcnCharstring
	 *                the TTCN-3 string with escapes to extract
	 */
	public CharstringExtractor(final String aTtcnCharstring) {
		if (aTtcnCharstring != null) {
			//remove the beginning and ending '"' characters
			final String escaped = aTtcnCharstring.replaceAll("^\"|\"$", "");
			mExtractedString = extractString(escaped);
		} else {
			mExtractedString = null;
		}
	}

	/** @return the value string of the TTCN-3 string */
	public String getExtractedString() {
		return mExtractedString;
	}

	/**
	 * @return if TTCN-3 string contains error
	 */
	public boolean isErroneous() {
		return mErroneous;
	}

	/**
	 * @return the error message (if any)
	 */
	public String getErrorMessage() {
		return mErrorMessage;
	}

	/**
	 * Converts string with special characters to normal displayable string
	 * Special characters:
	 *   "" -> "
	 *   \['"?\abfnrtv\u000a]
	 *   \x[0-9a-fA-F][0-9a-fA-F]?
	 *   \[0-3]?[0-7][0-7]?
	 * 
	 * @param aTtcnCharstring
	 *                TTCN-3 charstring representation, it can contain
	 *                escape characters, NOT NULL
	 * @return extracted string value
	 */
	private String extractString(final String aTtcnCharstring) {
		final int slength = aTtcnCharstring.length();
		int pointer = 0;
		final StringBuilder sb = new StringBuilder();
		while (pointer < slength) {
			// Special characters:
			// Special characters by the TTCNv3 standard:
			// The 2 double-quotes: "" -> it is one double-quote
			if (pointer + 1 < slength && aTtcnCharstring.substring(pointer, pointer + 2).equals("\"\"")) {
				sb.append('"');
				pointer += 2;
			}

			// TITAN specific special characters:
			// backslash-escaped character sequences:
			else if (pointer + 1 < slength) {
				final char c1 = aTtcnCharstring.charAt(pointer);
				if (c1 == '\\') {
					pointer++;
					final char c2 = aTtcnCharstring.charAt(pointer);
					// backslash-escaped single-quote,
					// double-quote, question mark or
					// backslash:
					if (c2 == '\'' || c2 == '"' || c2 == '?' || c2 == '\\') {
						sb.append(aTtcnCharstring.charAt(pointer));
						pointer++;
					} else if (c2 == 'a') { // Audible bell
						sb.append((char) 0x07);
						pointer++;
					} else if (c2 == 'b') { // Backspace
						sb.append((char) 0x08);
						pointer++;
					} else if (c2 == 'f') { // Form feed
						sb.append((char) 0x0c);
						pointer++;
					} else if (c2 == 'n') { // New line
						sb.append((char) 0x0a);
						pointer++;
					} else if (c2 == 'r') { // Carriage return
						sb.append((char) 0x0d);
						pointer++;
					} else if (c2 == 't') { // Horizontal tab
						sb.append((char) 0x09);
						pointer++;
					} else if (c2 == 'v') { // Vertical tab
						sb.append((char) 0x0b);
						pointer++;
					} else if (c2 == 10) { // New line escaped
						sb.append((char) 0x0a);
						pointer++;
					} else if (c2 == 'x') { // hex-notation: \xHH?
						pointer++;
						if (pointer >= slength) {
							// end of string reached
							mErrorMessage = INVALID_ESCAPE_SEQUENCE + "'\\x'";
							mErroneous = true;
							return null;
						}
						final int hexStart = pointer;
						if (!isHexDigit(aTtcnCharstring.charAt(pointer))) {
							// invalid char after \x
							mErrorMessage = INVALID_ESCAPE_SEQUENCE + "'\\x" + aTtcnCharstring.charAt(hexStart) + "'";
							mErroneous = true;
							return null;
						}
						pointer++;
						if (pointer < slength && isHexDigit(aTtcnCharstring.charAt(pointer))) {
							// 2nd hex digit is optional
							pointer++;
						}
						sb.append((char) Integer.parseInt(aTtcnCharstring.substring(hexStart, pointer), 16));
					} else if (isOctDigit(c2)) { // [0..7] // octal notation: \[0-3]?[0-7][0-7]?
						final int octStart = pointer;
						pointer++;
						while (pointer < slength && pointer - octStart < 3 && isOctDigit(aTtcnCharstring.charAt(pointer))) {
							pointer++;
						}
						final int octInt = Integer.parseInt(aTtcnCharstring.substring(octStart, pointer), 8);
						if (octInt > 255) { // oct 377
							mErrorMessage = INVALID_ESCAPE_SEQUENCE + "'\\"
									+ aTtcnCharstring.substring(octStart, pointer) + "'";
							mErroneous = true;
							return null;
						} else {
							sb.append((char) octInt);
						}
					} else {
						//TODO: remove or use one of them
						/*
						mErrorMessage = INVALID_ESCAPE_SEQUENCE + "'\\" + c2 + "'";
						mErroneous = true;
						return null;
						/*/
						sb.append('\\');
						sb.append(c2);
						pointer++;
						//*/
					}
				} else { // End of backslash-escape
					sb.append(aTtcnCharstring.charAt(pointer));
					pointer++;
				}
			} else {
				sb.append(aTtcnCharstring.charAt(pointer));
				pointer++;
			}
		} // End of While

		return sb.toString();
	}

	/**
	 * @param aChar
	 *                character to check
	 * @return true if aChar is hexadecimal digit
	 */
	private static boolean isHexDigit(final char aChar) {
		return (aChar >= '0' && aChar <= '9') || (aChar >= 'a' && aChar <= 'f') || (aChar >= 'A' && aChar <= 'F');
	}

	/**
	 * @param aChar
	 *                character to check
	 * @return true if aChar is octal digit
	 */
	private static boolean isOctDigit(final char aChar) {
		return aChar >= '0' && aChar <= '7';
	}
}
