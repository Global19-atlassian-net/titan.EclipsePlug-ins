/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for handling TTCN-3 pattern
 * @author Arpad Lovassy
 */
public class TTCN_Pattern {

	/**
	 * Pattern for TTCN-3 dynamic reference
	 * "{IDENTIFIER}"
	 */
	private static final Pattern PATTERN_DYNAMIC_REFERENCE = Pattern.compile( "(.*?)\\{([A-Za-z][A-Za-z0-9_]*)\\}(.*)" );

	/**
	 * Pattern for TTCN-3 static reference
	 * "{\IDENTIFIER}"
	 * NOTE: {\ is already parsed
	 */
	private static final Pattern PATTERN_STATIC_REFERENCE = Pattern.compile( "([A-Za-z][A-Za-z0-9_]*)\\}(.*)" );

	/**
	 * Pattern for a TTCN-3 referenced character set
	 * "\N{IDENTIFIER}"
	 * NOTE: \N is already parsed
	 */
	private static final Pattern PATTERN_CHARSET_REFERENCE = Pattern.compile( "\\\\N\\{([A-Za-z][A-Za-z0-9_]*)\\}(.*)" );

	/**
	 * Pattern for universal char quadruple
	 * "\q{i,i,i,i}" where i is non-negative integer and i < 256
	 * NOTE: \q is already parsed
	 */
	private static final Pattern PATTERN_UNICHAR_QUADRUPLE = Pattern.compile( "\\{\\s*(0|[1-9][0-9]*)\\s*\\,\\s*(0|[1-9][0-9]*)\\s*\\,\\s*(0|[1-9][0-9]*)\\s*\\,\\s*(0|[1-9][0-9]*)\\s*\\}(.*)" );

	/**
	 * Pattern for universal char USI-like notation
	 * example: "\q{U0171}" or "\q{U171, U+172}"
	 * NOTE: \q is already parsed
	 */
	private static final Pattern PATTERN_UNICHAR_USI_LIST = Pattern.compile( "\\{\\s*(U\\+?[0-9A-Fa-f]+(\\s*\\,\\s*U\\+?[0-9A-Fa-f]+)*)\\s*\\}(.*)" );

	/**
	 * Pattern for single value repetition
	 * example: "#5"
	 * NOTE: # is already parsed
	 */
	private static final Pattern PATTERN_REPETITION_SINGLE = Pattern.compile( "\\s*(\\d+)(.*)" );

	/**
	 * Pattern for range repetition.
	 * Single value repetition with parenthesis is handled here
	 * example: range: "#(,)", "#(5,)", "#(,5)", "#(3,5)",
	 *          single value: "#(5)"
	 * NOTE: # is already parsed
	 */
	private static final Pattern PATTERN_REPETITION_RANGE = Pattern.compile( "\\(\\s*(\\d*)\\s*(,\\s*(\\d*))?\\s*\\)(.*)" );

	/**
	 * List of possible metacharacters
	 */
	private static final String METACHARS = "#()*+-?[\\]^{|}";

	/**
	 * converts TTCN-3 pattern to java pattern
	 * @param ttcnPattern TTCN-3 pattern
	 * @param nocase true for case insensitive matching
	 * @return converted java pattern
	 */
	public static Pattern convert_pattern( final String ttcnPattern, final boolean nocase ) {
		return convert_pattern( ttcnPattern, nocase, null );
	}

	/**
	 * converts TTCN-3 pattern to java pattern
	 * @param ttcnPattern TTCN-3 pattern
	 * @param nocase true for case insensitive matching
	 * @param refs references in a map, where key is reference, value is reference value
	 * @return converted java pattern
	 */
	public static Pattern convert_pattern( final String ttcnPattern, final boolean nocase, final Map< String, String > refs ) {
		Pattern javaPattern = null;
		try {
			String javaPatternString = convert_pattern(ttcnPattern, refs);
			if ( nocase ) {
				//NOTE: it is made this way because Pattern.compile( javaPatternString, Pattern.CASE_INSENSITIVE ) does NOT work
				javaPatternString = javaPatternString.toLowerCase();
			}
			javaPattern = Pattern.compile( javaPatternString );
		} catch (Exception e) {
			throw new TtcnError( MessageFormat.format( "Cannot convert pattern \"{0}\" to POSIX-equivalent.", ttcnPattern ) );
		}
		return javaPattern;
	}

	/**
	 * Match charstring with a Java regex pattern
	 * @param s search string
	 * @param javaPattern regex pattern in Java format
	 * @param nocase true for case insensitive matching
	 * @return match result
	 */
	public static boolean match( final String s, final Pattern javaPattern, final boolean nocase ) {
		boolean result = false;
		try {
			final Matcher m = javaPattern.matcher( nocase ? s.toLowerCase() : s );
			result = m.matches();
		} catch (Exception e) {
			throw new TtcnError( MessageFormat.format( "Pattern matching error: {0}", e.toString() ) );
		}
		return result;
	}

	/**
	 * Originally regexp() function
	 * @param s search string
	 * @param javaPattern regexp pattern in Java format
	 * @param groupno pattern group number, 0 for the whole pattern
	 * @param nocase true for case insensitive matching
	 * @return matching substring, or empty string in case of no match
	 */
	public static String regexp( final String s, final Pattern javaPattern, final int groupno, final boolean nocase ) {
		String result = "";
		String regexpPatternString = javaPattern.pattern();
		//nocase pattern
		if (nocase) {
			regexpPatternString = "(?i)" + regexpPatternString;
		}
		//multiline string
		if (s.contains("\n") || s.contains("\r")) {
			regexpPatternString = "(?s)" + regexpPatternString;
		}

		final Pattern tempPattern = Pattern.compile(regexpPatternString);
		try {
			final Matcher m = tempPattern.matcher(s);
			if ( m.matches() ) {
				result = m.group( groupno + 1 );
			}
		} catch (Exception e) {
			throw new TtcnError( MessageFormat.format( "Pattern matching error: {0}", e.toString() ) );
		}

		return result;
	}

	/**
	 * Originally regexp() function
	 * @param s search string
	 * @param ttcnPattern TTCN-3 pattern
	 * @param groupno pattern group number, 0 for the whole pattern
	 * @param nocase true for case insensitive matching
	 * @return matching substring, or empty string in case of no match
	 */
	public static String regexp( final String s, final String ttcnPattern, final int groupno, final boolean nocase ) {
		return regexp( s, convert_pattern( ttcnPattern, nocase ), groupno, nocase );
	}

	/**
	 * converts TTCN-3 dynamic pattern to java pattern
	 * (Dynamic pattern: contains dynamic references)
	 * @param ttcnPattern TTCN-3 pattern
	 * @param refs references in a map, where key is reference, value is reference value
	 * @return converted java pattern
	 */
	private static String convert_pattern( final String ttcnPattern, final Map< String, String > refs ) {
		final StringBuilder javaPattern = new StringBuilder();
		final String ttcnStaticPattern = convert_dynamic_references( ttcnPattern, refs );
		convert_static_pattern( ttcnStaticPattern, new AtomicInteger(0), javaPattern, refs );
		return javaPattern.toString();
	}

	/**
	 * converts TTCN-3 dynamic pattern to TTCN-3 static pattern, resolves all the dynamic patterns recursively.
	 * @param ttcnPattern TTCN-3 pattern
	 * @param refs references in a map, where key is reference, value is reference value
	 * @return converted TTCN-3 static pattern
	 */
	private static String convert_dynamic_references( String ttcnPattern, final Map< String, String > refs ) {
		if ( refs == null || refs.isEmpty() ) {
			return ttcnPattern;
		}

		Matcher m = PATTERN_DYNAMIC_REFERENCE.matcher( ttcnPattern );
		while ( m.matches() ) {
			final String ref = m.group(2);
			final String refValue = refs.get(ref);
			ttcnPattern = m.group(1) + refValue + m.group(3);

			m = PATTERN_DYNAMIC_REFERENCE.matcher( ttcnPattern );
		}

		return ttcnPattern;
	}

	/**
	 * converts TTCN-3 static pattern to java pattern
	 * (Static pattern: does NOT contain dynamic references)
	 * @param ttcnPattern TTCN-3 pattern
	 * @param pos character position in ttcnPattern
	 * @param javaPattern converted java pattern
	 * @param refs references in a map, where key is reference, value is reference value
	 */
	private static void convert_static_pattern( final String ttcnPattern, final AtomicInteger pos,
			final StringBuilder javaPattern, final Map< String, String > refs ) {
		while ( pos.get() < ttcnPattern.length() ) {
			final char c = ttcnPattern.charAt(pos.getAndIncrement());
			switch ( c ) {
			case '?':
				javaPattern.append('.');
				break;
			case '*':
				javaPattern.append(".*");
				break;
			case '+':
				javaPattern.append('+');
				break;
			case '\\': {
				convert_escaped( ttcnPattern, pos, javaPattern, false, refs );
				break;
			}
			case '"': {
				final char c2 = ttcnPattern.charAt(pos.getAndIncrement());
				if ( c2 == '"' ) {
					javaPattern.append('"');
				} else {
					pos.decrementAndGet();
					javaPattern.append('\"');
				}
				// else is not needed, because single '"' is the end of the string, which is handled by the parser
				break;
			}
			case '$':
				// $ is not escaped in TTCN-3 but escaped in Java, but not in set
				javaPattern.append("\\$");
				break;
			case '[': {
				javaPattern.append('[');
				convert_set( ttcnPattern, pos, javaPattern, refs );
				break;
			}
			case '#': {
				convert_repetition( ttcnPattern, pos, javaPattern );
				break;
			}
			//FIXME:don't need any converts because references are already resolved
			case '{': {
				javaPattern.append("\\{");
				//final char c2 = ttcnPattern.charAt(pos.getAndIncrement());
				//if ( c2 == '\\' ) {
				//	convert_static_reference( ttcnPattern, pos, javaPattern, refs );
				//}
				// else is not needed, because dynamic references are already resolved
				break;
			}
			//NOTE: special cases: ()|
			//      these work in the same way in TTCN-3 and java pattern, so no conversion is needed
			default:
				javaPattern.append(c);
				break;
			}
		}
	}

	/**
	 * @return {@code true} if character can be metacharacter, {@code false} otherwise.
	 * Quote from TTCN-3 standard B.1.5.0:
	 * The symbols that can appear as lexical marks in metacharacter definitions are called metacharacter symbols. They
	 * include the following characters: "#", "(", ")", "*", "+", "-", "?", "[", "\", "]", "^", "{","|","}". When any of the
	 * metacharacter symbols are present in a pattern, but do not form a valid metacharacter, they retain their literal value.
	 */
	private static boolean is_meta( final char c ) {
		return METACHARS.indexOf(c) >= 0;
	}

	/**
	 * converts TTCN-3 pattern escaped character to java pattern character.
	 * NOTE: \ is already parsed
	 * @param ttcnPattern TTCN-3 pattern
	 * @param pos character position in ttcnPattern
	 * @param javaPattern converted java pattern
	 * @param isSet true inside a set
	 * @param refs references in a map, where key is reference, value is reference value
	 */
	private static void convert_escaped( final String ttcnPattern, final AtomicInteger pos, final StringBuilder javaPattern,
			final boolean isSet, final Map< String, String > refs ) {
		final char c = ttcnPattern.charAt(pos.getAndIncrement());
		switch ( c ) {
		case 'd':
			javaPattern.append(isSet ? "0-9" : "[0-9]");
			break;
		case 'w':
			javaPattern.append(isSet ? "0-9A-Za-z" : "[0-9A-Za-z]");
			break;
		case 't':
			javaPattern.append("\\t");
			break;
		case 'n':
			javaPattern.append(isSet ? "\\n-\\r" : "[\\n-\\r]");
			break;
		case 'r':
			javaPattern.append("\\r");
			break;
		case 's':
			javaPattern.append(isSet ? "\\t-\\r " : "[\\t-\\r ]");
			break;
		case 'b':
			//NOTE: it does nothing in pattern_p.y
			javaPattern.append("\\b");
			break;
		case 'f':
			javaPattern.append("\\f");
			break;
		case '\'':
			javaPattern.append("\\\'");
			break;
		case '"':
			javaPattern.append("\\\"");
			break;
		case 'q':
			convert_unichar_list(ttcnPattern, pos, javaPattern);
			break;
		case 'N':
			convert_charset_reference(ttcnPattern, pos, javaPattern, isSet, refs);
			break;
		default:
			if ( is_meta( c ) ) {
				javaPattern.append('\\').append(c);
			} else {
				throw new TtcnError("Escape character \\" + c + " is not supported at position " + pos.get());
			}
		}
	}

	/**
	 * converts TTCN-3 pattern set to java pattern set (from '[' to ']').
	 * NOTE: '[' is already parsed
	 * @param ttcnPattern TTCN-3 pattern
	 * @param pos character position in ttcnPattern
	 * @param javaPattern converted java pattern
	 * @param refs references in a map, where key is reference, value is reference value
	 */
	private static void convert_set( final String ttcnPattern, final AtomicInteger pos,
			final StringBuilder javaPattern, final Map< String, String > refs ) {
		char c = ttcnPattern.charAt(pos.getAndIncrement());
		if ( c == '^' ) {
			javaPattern.append('^');
			c = ttcnPattern.charAt(pos.getAndIncrement());
		}
		while ( c != ']' ) {
			switch ( c ) {
			case '\\':
				convert_escaped( ttcnPattern, pos, javaPattern, true, refs );
				break;
			case '^':
				throw new TtcnError("Character ^ can be only the first character of th set at position " + pos.get());
			case '[':
				// [ within a set defines a literal [
				javaPattern.append("\\[");
				break;
			case '-':
				//special case, needs to be checked
				//it defines a set range, so it can be used after a valid set item and cannot be used again until range is closed
				javaPattern.append('-');
				break;
			default:
				javaPattern.append(c);
				break;
			}
			c = ttcnPattern.charAt(pos.getAndIncrement());
		}
		javaPattern.append(']');
	}

	/**
	 * converts TTCN-3 static reference to java string
	 * NOTE: {\ is already parsed
	 * @param ttcnPattern TTCN-3 pattern
	 * @param pos character position in ttcnPattern
	 * @param javaPattern converted java pattern
	 * @param refs references in a map, where key is reference, value is reference value
	 */
	private static void convert_static_reference( final String ttcnPattern, final AtomicInteger pos,
			final StringBuilder javaPattern, final Map<String, String> refs ) {
		final String input = ttcnPattern.substring(pos.get());
		final Matcher m = PATTERN_STATIC_REFERENCE.matcher(input);
		if ( m.matches() ) {
			final int offset = m.toMatchResult().start(2);
			pos.getAndAdd(offset);
			final String ref = m.group(1);
			final String refValue = refs.get(ref);
			//TODO: escape refValue?
			javaPattern.append( refValue );
		} else {
			throw new TtcnError("Invalid static reference at position " + pos.get());
		}
	}

	/**
	 * converts TTCN-3 referenced character set to java string
	 * NOTE: \N is already parsed
	 * @param ttcnPattern TTCN-3 pattern
	 * @param pos character position in ttcnPattern
	 * @param javaPattern converted java pattern
	 * @param isSet true inside a set
	 * @param refs references in a map, where key is reference, value is reference value
	 */
	private static void convert_charset_reference( final String ttcnPattern, final AtomicInteger pos,
			final StringBuilder javaPattern, final boolean isSet,
			final Map<String, String> refs ) {
		final String input = ttcnPattern.substring(pos.get());
		final Matcher m = PATTERN_CHARSET_REFERENCE.matcher(input);
		if ( m.matches() ) {
			final int offset = m.toMatchResult().start(2);
			pos.getAndAdd(offset);
			javaPattern.append( isSet ? m.group(1) : '[' + m.group(1) + ']' );
			pos.getAndAdd(m.group(1).length());
		} else if(input == null) {
			throw new TtcnError("Invalid character set reference at position " + pos.get());
		} else {
			javaPattern.append( isSet ? input : '[' + input + ']' );
			pos.getAndAdd(input.length());
		}
	}

	/**
	 * converts TTCN-3 pattern repetition to java pattern
	 * NOTE: # is already parsed
	 * @param ttcnPattern TTCN-3 pattern
	 * @param pos character position in ttcnPattern
	 * @param javaPattern converted java pattern
	 */
	private static void convert_repetition(final String ttcnPattern, final AtomicInteger pos, final StringBuilder javaPattern) {
		final String input = ttcnPattern.substring(pos.get());
		Matcher m = PATTERN_REPETITION_SINGLE.matcher(input);
		if ( m.matches() ) {
			final int offset = m.toMatchResult().start(2);
			pos.getAndAdd(offset);
			final String valueStr = m.group(1);
			final int value = Integer.parseInt(valueStr);
			javaPattern.append( "{" + value + "}" );
		} else {
			m = PATTERN_REPETITION_RANGE.matcher(input);
			if ( m.matches() ) {
				final int offset = m.toMatchResult().start(4);
				pos.getAndAdd(offset);
				final String minStr = m.group(1);
				final String commaWithMaxStr = m.group(2);
				//maxStr is null if there is no comma
				final String maxStr = m.group(3);
				javaPattern.append( '{' );
				javaPattern.append( minStr.isEmpty() ? '0' : minStr );
				if ( commaWithMaxStr != null && !commaWithMaxStr.isEmpty() ) {
					javaPattern.append( ',' ).append( maxStr );
				}
				javaPattern.append( '}' );
			} else {
				throw new TtcnError("Invalid pattern repetition at position " + pos.get());
			}
		}
	}

	/**
	 * converts unichar list to java pattern characters.
	 * NOTE: \ is already parsed
	 * @param ttcnPattern TTCN-3 pattern
	 * @param pos character position in ttcnPattern
	 * @param javaPattern converted java pattern
	 */
	private static void convert_unichar_list(final String ttcnPattern, final AtomicInteger pos, final StringBuilder javaPattern) {
		final String input = ttcnPattern.substring(pos.get());
		Matcher m = PATTERN_UNICHAR_USI_LIST.matcher(input);
		if ( m.matches() ) {
			final int offset = m.toMatchResult().start(3);
			pos.getAndAdd(offset);
			final String ucharlist = m.group(1);
			final String[] uchars = ucharlist.split("\\s*,\\s*");
			for (final String uchar : uchars) {
				final String hexstr = uchar.substring(1);
				final int hex = Integer.parseInt(hexstr, 16);
				javaPattern.append(String.valueOf(Character.toChars(hex)));
			}
		} else {
			m = PATTERN_UNICHAR_QUADRUPLE.matcher(input);
			if ( m.matches() ) {
				final int offset = m.toMatchResult().start(5);
				pos.getAndAdd(offset);
				final String groupStr = m.group(1);
				final String planeStr = m.group(2);
				final String rowStr = m.group(3);
				final String cellStr = m.group(4);
				final int group = Integer.parseInt(groupStr);
				final int plane = Integer.parseInt(planeStr);
				final int row = Integer.parseInt(rowStr);
				final int cell = Integer.parseInt(cellStr);
				final TitanUniversalChar uc = new TitanUniversalChar( (char)group, (char)plane, (char)row, (char)cell);
				javaPattern.append(uc.to_utf(true));
			} else {
				throw new TtcnError("Invalid unichar list at position " + pos.get());
			}
		}
	}
}
