/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.runtime.core.Base_Type.TTCN_Typedescriptor;
import org.eclipse.titan.runtime.core.Param_Types.Module_Parameter;
import org.eclipse.titan.runtime.core.Param_Types.Module_Parameter.basic_check_bits_t;
import org.eclipse.titan.runtime.core.Param_Types.Module_Parameter.expression_operand_t;
import org.eclipse.titan.runtime.core.Param_Types.Module_Parameter.type_t;
import org.eclipse.titan.runtime.core.TTCN_EncDec.error_behavior_type;
import org.eclipse.titan.runtime.core.TTCN_EncDec.error_type;

/**
 * TTCN-3 octetstring template
 *
 * @author Arpad Lovassy
 * @author Andrea Palfi
 */
public class TitanOctetString_template extends Restricted_Length_Template {

	protected TitanOctetString single_value;

	// value_list part
	private ArrayList<TitanOctetString_template> value_list;

	/**
	 * octetstring pattern value
	 *
	 * Each element is represented as an unsigned short. Meaning of values:
	 * 0 .. 255 -> 00 .. FF, 256 -> ?, 257 -> *
	 */
	private char pattern_value[];

	/** reference counter for pattern_value */
	private int pattern_value_ref_count;

	private IDecode_Match dec_match;

	public TitanOctetString_template() {
		// do nothing
	}

	public TitanOctetString_template(final template_sel otherValue) {
		super(otherValue);
		checkSingleSelection(otherValue);
	}

	public TitanOctetString_template(final TitanOctetString otherValue) {
		super(template_sel.SPECIFIC_VALUE);
		otherValue.mustBound("Creating a template from an unbound octetstring value.");

		single_value = new TitanOctetString(otherValue);
	}

	public TitanOctetString_template(final TitanOctetString_Element otherValue) {
		super(template_sel.SPECIFIC_VALUE);
		otherValue.mustBound("Creating a template from an unbound octetstring value.");

		single_value = new TitanOctetString(otherValue.get_nibble());
	}

	public TitanOctetString_template(final Optional<TitanOctetString> otherValue) {
		switch (otherValue.get_selection()) {
		case OPTIONAL_PRESENT:
			set_selection(template_sel.SPECIFIC_VALUE);
			single_value = new TitanOctetString(otherValue.constGet());
			break;
		case OPTIONAL_OMIT:
			set_selection(template_sel.OMIT_VALUE);
			break;
		case OPTIONAL_UNBOUND:
			throw new TtcnError("Creating a octetstring template from an unbound optional field.");
		}
	}

	public TitanOctetString_template(final TitanOctetString_template otherValue) {
		copyTemplate(otherValue);
	}

	public TitanOctetString_template(final char pattern_elements[]) {
		super(template_sel.STRING_PATTERN);
		pattern_value = TitanStringUtils.copyCharList(pattern_elements);
	}

	public TitanOctetString_template(final String patternString) {
		super(template_sel.STRING_PATTERN);
		pattern_value = patternString2List(patternString);
	}

	private static char[] patternString2List(final String patternString) {
		if (patternString == null) {
			throw new TtcnError("Internal error: octetstring pattern is null.");
		}

		final int patternLength = patternString.length();
		final List<Character> tmp_result = new ArrayList<Character>(patternLength);
		for (int i = 0; i < patternLength; i++) {
			int patternValue = octetDigit1(patternString.charAt(i));
			if (patternValue < 16) {
				// there is an other digit, which is not ? or *
				++i;
				if (i == patternLength) {
					throw new TtcnError("Internal error: last octet is incomplete.");
				}
				patternValue *= 16;
				patternValue += octetDigit2(patternString.charAt(i));

			}
			tmp_result.add((char) patternValue);
		}
		char result[] = new char[tmp_result.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = tmp_result.get(i);
		}
		return result;
	}

	/**
	 * Converts the 1st digit of an octet in an octetstring template to pattern value.
	 * Possible values: [0-9A-Fa-f?*]
	 * If digit is ? or *, there is no 2nd digit
	 *
	 * Each element occupies one byte. Meaning of values:
	 * 0 .. 15 -> 0 .. F, 256 -> ?, 257 -> *
	 * @param digit hexadecimal digit or ? or *
	 * @return int value
	 */
	private static int octetDigit1(final char digit) {
		if ('0' <= digit && '9' >= digit) {
			return digit - '0';
		}

		if ('A' <= digit && 'F' >= digit) {
			return digit - 'A' + 10;
		}

		if ('a' <= digit && 'f' >= digit) {
			return digit - 'a' + 10;
		}

		if ('?' == digit) {
			return 256;
		}

		if ('*' == digit) {
			return 257;
		}

		throw new TtcnError("Internal error: invalid element in octetstring pattern.");
	}

	/**
	 * Converts the 2nd digit of an octet in an octetstring template to pattern value.
	 * Possible values: [0-9A-Fa-f]
	 * @param digit hexadecimal digit.
	 * @return int value
	 */
	private static int octetDigit2(final char digit) {
		if ('0' <= digit && '9' >= digit) {
			return digit - '0';
		}

		if ('A' <= digit && 'F' >= digit) {
			return digit - 'A' + 10;
		}

		if ('a' <= digit && 'f' >= digit) {
			return digit - 'a' + 10;
		}

		throw new TtcnError("Internal error: invalid element in octetstring pattern.");
	}

	@Override
	public void cleanUp() {
		switch (templateSelection) {
		case SPECIFIC_VALUE:
			single_value = null;
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			value_list.clear();
			value_list = null;
		case STRING_PATTERN:
			if (pattern_value_ref_count > 1) {
				pattern_value_ref_count--;
			} else if (pattern_value_ref_count == 1) {
				pattern_value = null;
			} else {
				throw new TtcnError("Internal error: Invalid reference counter in a octetstring pattern.");
			}
			break;
		case DECODE_MATCH:
			dec_match = null;
			break;
		default:
			break;
		}
		templateSelection = template_sel.UNINITIALIZED_TEMPLATE;
	}

	@Override
	public TitanOctetString_template assign(final Base_Type otherValue) {
		if (otherValue instanceof TitanOctetString) {
			return assign((TitanOctetString)otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to octetstring", otherValue));
	}

	@Override
	public TitanOctetString_template assign(final Base_Template otherValue) {
		if (otherValue instanceof TitanOctetString_template) {
			return assign((TitanOctetString_template)otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to octetstring", otherValue));
	}

	// originally operator=
	public TitanOctetString_template assign(final template_sel otherValue) {
		checkSingleSelection(otherValue);
		cleanUp();
		set_selection(otherValue);

		return this;
	}

	// originally operator=
	public TitanOctetString_template assign(final char[] otherValue) {
		cleanUp();
		set_selection(template_sel.SPECIFIC_VALUE);
		single_value = new TitanOctetString(otherValue);

		return this;
	}

	// originally operator=
	public TitanOctetString_template assign(final TitanOctetString otherValue) {
		otherValue.mustBound("Assignment of an unbound octetstring value to a template.");

		cleanUp();
		set_selection(template_sel.SPECIFIC_VALUE);
		single_value = new TitanOctetString(otherValue);

		return this;
	}

	public TitanOctetString_template assign(final TitanOctetString_Element otherValue) {
		otherValue.mustBound("Assignment of an unbound octetstring value to a template.");

		cleanUp();
		set_selection(template_sel.SPECIFIC_VALUE);
		single_value = new TitanOctetString(otherValue.get_nibble());

		return this;
	}

	// originally operator=
	public TitanOctetString_template assign(final TitanOctetString_template otherValue) {
		if (otherValue != this) {
			cleanUp();
			copyTemplate(otherValue);
		}

		return this;
	}

	private void copyTemplate(final TitanOctetString_template otherValue) {
		switch (otherValue.templateSelection) {
		case SPECIFIC_VALUE:
			single_value = new TitanOctetString(otherValue.single_value);
			break;
		case OMIT_VALUE:
		case ANY_VALUE:
		case ANY_OR_OMIT:
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			value_list = new ArrayList<TitanOctetString_template>(otherValue.value_list.size());
			for (int i = 0; i < otherValue.value_list.size(); i++) {
				final TitanOctetString_template temp = new TitanOctetString_template(otherValue.value_list.get(i));
				value_list.add(temp);
			}
			break;
		case STRING_PATTERN:
			pattern_value = otherValue.pattern_value;
			pattern_value_ref_count++;
			break;
		case DECODE_MATCH:
			dec_match = otherValue.dec_match;
			break;
		default:
			throw new TtcnError("Copying an uninitialized/unsupported octetstring template.");
		}

		set_selection(otherValue);
	}

	//originally operator[](int)
	public TitanOctetString_Element getAt(final int index_value) {
		if (templateSelection != template_sel.SPECIFIC_VALUE || is_ifPresent) {
			throw new TtcnError("Accessing a octetstring element of a non-specific octetstring template.");
		}

		return single_value.getAt(index_value);
	}

	// originally operator[](const INTEGER&)
	public TitanOctetString_Element getAt(final TitanInteger index_value) {
		index_value.mustBound("Indexing a octetstring template with an unbound integer value.");

		return getAt(index_value.getInt());
	}

	// originally operator[](int) const
	public TitanOctetString_Element constGetAt(final int index_value) {
		if (templateSelection != template_sel.SPECIFIC_VALUE || is_ifPresent) {
			throw new TtcnError("Accessing a octetstring element of a non-specific octetstring template.");
		}

		return single_value.constGetAt(index_value);
	}

	// originally operator[](const INTEGER&) const
	public TitanOctetString_Element constGetAt(final TitanInteger index_value) {
		index_value.mustBound("Indexing a octetstring template with an unbound integer value.");

		return constGetAt(index_value.getInt());
	}

	@Override
	public boolean match(final Base_Type otherValue, final boolean legacy) {
		if (otherValue instanceof TitanOctetString) {
			return match((TitanOctetString) otherValue, legacy);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to octetstring", otherValue));
	}

	@Override
	public void log_match(final Base_Type match_value, final boolean legacy) {
		if (match_value instanceof TitanOctetString) {
			log_match((TitanOctetString) match_value, legacy);
			return;
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to octetstring", match_value));
	}

	// originally match
	public boolean match(final TitanOctetString otherValue) {
		return match(otherValue, false);
	}

	// originally match
	public boolean match(final TitanOctetString otherValue, final boolean legacy) {
		if (!otherValue.isBound()) {
			return false;
		}

		final TitanInteger value_length = otherValue.lengthOf();
		if (!match_length(value_length.getInt())) {
			return false;
		}

		switch (templateSelection) {
		case SPECIFIC_VALUE:
			return single_value.operatorEquals(otherValue);
		case OMIT_VALUE:
			return false;
		case ANY_VALUE:
		case ANY_OR_OMIT:
			return true;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			for (int i = 0; i < value_list.size(); i++) {
				if (value_list.get(i).match(otherValue, legacy)) {
					return templateSelection == template_sel.VALUE_LIST;
				}
			}
			return templateSelection == template_sel.COMPLEMENTED_LIST;
		case STRING_PATTERN:
			return match_pattern(pattern_value, otherValue);
		case DECODE_MATCH: {
			TTCN_EncDec.set_error_behavior(error_type.ET_ALL, error_behavior_type.EB_WARNING);
			TTCN_EncDec.clear_error();
			final TTCN_Buffer buffer = new TTCN_Buffer(otherValue);
			final boolean ret_val = dec_match.match(buffer);
			TTCN_EncDec.set_error_behavior(error_type.ET_ALL, error_behavior_type.EB_DEFAULT);
			TTCN_EncDec.clear_error();
			return ret_val;
		}
		default:
			throw new TtcnError("Matching with an uninitialized/unsupported octetstring template.");
		}
	}

	/**
	 * This is the same algorithm that match_array uses
	 * to match 'record of' types.
	 * The only differences are: how two elements are matched and
	 * how an asterisk or ? is identified in the template
	 */
	private boolean match_pattern(final char string_pattern[], final TitanOctetString string_value) {
		final int stringPatternSize = string_pattern.length;
		final int stringValueNOctets = string_value.getValue().length;
		// the empty pattern matches the empty octetstring only
		if (stringPatternSize == 0) {
			return stringValueNOctets == 0;
		}

		int value_index = 0;
		int template_index = 0;
		int last_asterisk = -1;
		int last_value_to_asterisk = -1;
		//this variable is used to speed up the function
		char pattern_element;

		for(;;) {
			pattern_element = string_pattern[template_index];
			if (pattern_element < 256) {
				if (string_value.get_nibble(value_index) == pattern_element) {
					value_index++;
					template_index++;
				} else {
					if (last_asterisk == -1) {
						return false;
					}
					template_index = last_asterisk + 1;
					value_index = ++last_value_to_asterisk;
				}
			} else if (pattern_element == 256) {
				// ? found
				value_index++;
				template_index++;
			} else if (pattern_element == 257) {
				// * found
				last_asterisk = template_index++;
				last_value_to_asterisk = value_index;
			} else {
				throw new TtcnError("Internal error: invalid element in an octetstring pattern.");
			}

			if (value_index == stringValueNOctets && template_index == stringPatternSize) {
				return true;
			} else if (template_index == stringPatternSize) {
				if (string_pattern[template_index - 1] == 257) {
					return true;
				} else if (last_asterisk == -1) {
					return false;
				} else {
					template_index = last_asterisk + 1;
					value_index = ++last_value_to_asterisk;
				}
			} else if (value_index == stringValueNOctets) {
				while (template_index < stringPatternSize && string_pattern[template_index] == 257) {
					template_index++;
				}
				return template_index == stringPatternSize;
			}
		}
	}

	/** 
	 * Do not use this function!<br>
	 * It is provided by Java and currently used for debugging.
	 * But it is not part of the intentionally provided interface,
	 *   and so can be changed without notice. 
	 * <p>
	 * JAVA DESCRIPTION:
	 * <p>
	 * {@inheritDoc}
	 *  */
	@Override
	public String toString() {
		switch (templateSelection) {
		case UNINITIALIZED_TEMPLATE:
			return "<unbound>";
		case OMIT_VALUE:
			return "omit";
		case ANY_VALUE:
			return "?";
		case ANY_OR_OMIT:
			return "*";
		case SPECIFIC_VALUE:
			return single_value.toString();
		case COMPLEMENTED_LIST:
		case VALUE_LIST:
 {
			final StringBuilder builder = new StringBuilder();
			if (templateSelection == template_sel.COMPLEMENTED_LIST) {
				builder.append("complement");
			}
			builder.append('(');
			for (int i = 0; i < value_list.size(); i++) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(value_list.get(i).toString());
			}
			builder.append(')');
			return builder.toString();
		}
		case STRING_PATTERN:
		{
			final StringBuilder sb = new StringBuilder();
			sb.append('\'');
			final int size = pattern_value.length;
			for (int i = 0; i < size; i++) {
				final Character digit = pattern_value[i];
				if (digit == 256) {
					sb.append('?');
				} else if (digit == 257) {
					sb.append('*');
				} else {
					sb.append(TitanHexString.HEX_DIGITS.charAt(digit >> 4));
					sb.append(TitanHexString.HEX_DIGITS.charAt(digit % 16));
				}
			}
			sb.append("\'O");
			return sb.toString();
		}
		default:
			return "<unknown template selection>";
		}
	}

	public TitanOctetString valueOf() {
		if (templateSelection != template_sel.SPECIFIC_VALUE || is_ifPresent) {
			throw new TtcnError("Performing a valueof or send operation on a non-specific octetstring template.");
		}

		return single_value;
	}

	public TitanInteger lengthOf() {
		int min_length;
		boolean has_any_or_none;
		if (is_ifPresent) {
			throw new TtcnError("Performing lengthOf() operation on a octetstring template which has an ifpresent attribute.");
		}

		switch (templateSelection)
		{
		case SPECIFIC_VALUE:
			min_length = single_value.lengthOf().getInt();
			has_any_or_none = false;
			break;
		case OMIT_VALUE:
			throw new TtcnError("Performing lengthof() operation on an octetstring template containing omit value.");
		case ANY_VALUE:
		case ANY_OR_OMIT:
			min_length = 0;
			has_any_or_none = true; // max. length is infinity
			break;
		case VALUE_LIST:
		{
			// error if any element does not have length or the lengths differ
			if (value_list.isEmpty()) {
				throw new TtcnError("Internal error: Performing lengthOf() operation on an octetstring template "
						+ "containing an empty list.");
			}

			final int item_length = value_list.get(0).lengthOf().getInt();
			for (int i = 1; i < value_list.size(); i++) {
				if (value_list.get(i).lengthOf().getInt() != item_length) {
					throw new TtcnError("Performing lengthof() operation on an octetstring template "
							+ "containing a value list with different lengths.");
				}
			}
			min_length = item_length;
			has_any_or_none = false;
			break;
		}
		case COMPLEMENTED_LIST:
			throw new TtcnError("Performing lengthof() operation on an octetstring template containing complemented list.");
		case STRING_PATTERN:
			min_length = 0;
			has_any_or_none = false; // true if * chars in the pattern
			for (int i = 0; i < pattern_value.length; i++) {
				if (pattern_value[i] < 257) {
					min_length++;
				} else {
					// case of * character
					has_any_or_none = true;
				}
			}
			break;
		default:
			throw new TtcnError("Performing lengthof() operation on an uninitialized/unsupported octetstring template.");
		}

		return new TitanInteger(check_section_is_single(min_length, has_any_or_none, "length", "an", "octetstring template"));
	}

	public void setType(final template_sel template_type) {
		setType(template_type,0);
	}

	public void setType(final template_sel template_type, final int list_length) {
		if (template_type != template_sel.VALUE_LIST && template_type != template_sel.COMPLEMENTED_LIST &&
				template_type != template_sel.DECODE_MATCH) {
			throw new TtcnError("Setting an invalid type for an octetstring template.");
		}
		cleanUp();
		set_selection(template_type);
		if (template_type != template_sel.DECODE_MATCH) {
			value_list = new ArrayList<TitanOctetString_template>(list_length);
			for (int i = 0; i < list_length; ++i) {
				value_list.add(new TitanOctetString_template());
			}
		}
	}

	public TitanOctetString_template listItem(final int listIndex) {
		if (templateSelection != template_sel.VALUE_LIST &&
				templateSelection != template_sel.COMPLEMENTED_LIST) {
			throw new TtcnError("Accessing a list element of a non-list octetstring template.");
		}

		if (listIndex < 0) {
			throw new TtcnError("Accessing an octetstring value list template using a negative index (" + listIndex + ").");
		}
		if (listIndex >= value_list.size()) {
			throw new TtcnError("Index overflow in an octetstring value list template.");
		}
		return value_list.get(listIndex);
	}

	public void set_decmatch(final IDecode_Match dec_match) {
		if (templateSelection != template_sel.DECODE_MATCH) {
			throw new TtcnError("Setting the decoded content matching mechanism of a non-decmatch octetstring template.");
		}

		this.dec_match = dec_match;
	}

	public Object get_decmatch_dec_res() {
		if (templateSelection != template_sel.DECODE_MATCH) {
			throw new TtcnError("Retrieving the decoding result of a non-decmatch octetstring template.");
		}

		return dec_match.get_dec_res();
	}

	public TTCN_Typedescriptor get_decmatch_type_descr() {
		if (templateSelection != template_sel.DECODE_MATCH) {
			throw new TtcnError("Retrieving the decoded type's descriptor in a non-decmatch octetstring template.");
		}

		return dec_match.get_type_descr();
	}

	@Override
	public void log() {
		switch (templateSelection) {
		case SPECIFIC_VALUE:
			single_value.log();
			break;
		case COMPLEMENTED_LIST:
			TTCN_Logger.log_event_str("complement");
		case VALUE_LIST:
			TTCN_Logger.log_char('(');
			for (int i = 0; i < value_list.size(); i++) {
				if (i > 0) {
					TTCN_Logger.log_event_str(", ");
				}
				value_list.get(i).log();
			}
			TTCN_Logger.log_char(')');
			break;
		case STRING_PATTERN:
			TTCN_Logger.log_char('\'');
			for (int i = 0; i < pattern_value.length; i++) {
				final char pattern = pattern_value[i];
				if (pattern < 256) {
					TTCN_Logger.log_octet(pattern);
				} else if (pattern == 256) {
					TTCN_Logger.log_char('?');
				} else if (pattern == 257) {
					TTCN_Logger.log_char('*');
				} else {
					TTCN_Logger.log_event_str("<unknown>");
				}
			}
			TTCN_Logger.log_event_str("'O");
			break;
		case DECODE_MATCH:
			TTCN_Logger.log_event_str("decmatch ");
			dec_match.log();
			break;
		default:
			log_generic();
			break;
		}
		log_restricted();
		log_ifpresent();
	}

	public void log_match(final TitanOctetString match_value, final boolean legacy) {
		if (TTCN_Logger.matching_verbosity_t.VERBOSITY_COMPACT == TTCN_Logger.get_matching_verbosity()
				&& TTCN_Logger.get_logmatch_buffer_len() != 0) {
			TTCN_Logger.print_logmatch_buffer();
			TTCN_Logger.log_event_str(" := ");
		}
		match_value.log();
		TTCN_Logger.log_event_str(" with ");
		log();
		if (match(match_value)) {
			TTCN_Logger.log_event_str(" matched");
		} else {
			TTCN_Logger.log_event_str(" unmatched");
		}
	}

	@Override
	/** {@inheritDoc} */
	public void set_param(final Module_Parameter param) {
		param.basic_check(basic_check_bits_t.BC_TEMPLATE.getValue()|basic_check_bits_t.BC_LIST.getValue(), "octetstring template");
		switch (param.get_type()) {
		case MP_Omit:
			this.assign(template_sel.OMIT_VALUE);
			break;
		case MP_Any:
			this.assign(template_sel.ANY_VALUE);
			break;
		case MP_AnyOrNone:
			this.assign(template_sel.ANY_OR_OMIT);
			break;
		case MP_List_Template:
		case MP_ComplementList_Template: {
			final TitanOctetString_template temp = new TitanOctetString_template();
			temp.setType(param.get_type() == type_t.MP_List_Template ? template_sel.VALUE_LIST : template_sel.COMPLEMENTED_LIST, param.get_size());
			for (int i = 0; i < param.get_size(); i++) {
				temp.listItem(i).set_param(param.get_elem(i));
			}
			this.assign(temp);
			break;
		}
		case MP_Octetstring:
			this.assign(new TitanOctetString((char[]) param.get_string_data()));
			break;
		case MP_Octetstring_Template:
			this.assign((TitanOctetString_template)param.get_string_data());
			break;
		case MP_Expression:
			if (param.get_expr_type() == expression_operand_t.EXPR_CONCATENATE) {
				final TitanOctetString operand1 = new TitanOctetString();
				final TitanOctetString operand2 = new TitanOctetString();
				operand1.set_param(param.get_operand1());
				operand2.set_param(param.get_operand2());
				this.assign(operand1.concatenate(operand2));
			} else {
				param.expr_type_error("an octetstring");
			}
			break;
		default:
			param.type_error("octetstring template");
		}
		is_ifPresent = param.get_ifpresent();
		if (param.get_length_restriction() != null) {
			set_length_range(param);
		}
	}

	public boolean match_omit(final boolean legacy) {
		if (is_ifPresent) {
			return true;
		}

		switch (templateSelection) {
		case OMIT_VALUE:
		case ANY_OR_OMIT:
			return true;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			if (legacy) {
				// legacy behavior: 'omit' can appear in the value/complement list
				for (int i = 0; i < value_list.size(); i++) {
					if (value_list.get(i).match_omit()) {
						return templateSelection == template_sel.VALUE_LIST;
					}
				}
				return templateSelection == template_sel.COMPLEMENTED_LIST;
			}
			return false;
		default:
			return false;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void encode_text(final Text_Buf text_buf) {
		encode_text_restricted(text_buf);

		switch (templateSelection) {
		case OMIT_VALUE:
		case ANY_VALUE:
		case ANY_OR_OMIT:
			break;
		case SPECIFIC_VALUE:
			single_value.encode_text(text_buf);
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			text_buf.push_int(value_list.size());
			for (int i = 0; i < value_list.size(); i++) {
				value_list.get(i).encode_text(text_buf);
			}
			break;
		case STRING_PATTERN:
			text_buf.push_int(pattern_value.length);
			byte[] temp = new byte[pattern_value.length];
			for (int i = 0; i < pattern_value.length; i++) {
				temp[i] = (byte)pattern_value[i];
			}
			text_buf.push_raw((pattern_value.length + 7) / 8, temp);
			break;
		default:
			throw new TtcnError("Text encoder: Encoding an uninitialized/unsupported octetstring template.");
		}
	}

	@Override
	/** {@inheritDoc} */
	public void decode_text(final Text_Buf text_buf) {
		cleanUp();
		decode_text_restricted(text_buf);

		switch (templateSelection) {
		case OMIT_VALUE:
		case ANY_VALUE:
		case ANY_OR_OMIT:
			break;
		case SPECIFIC_VALUE:
			single_value = new TitanOctetString();
			single_value.decode_text(text_buf);
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST: {
			final int size = text_buf.pull_int().getInt();
			value_list = new ArrayList<TitanOctetString_template>(size);
			for (int i = 0; i < size; i++) {
				final TitanOctetString_template temp = new TitanOctetString_template();
				temp.decode_text(text_buf);
				value_list.add(temp);
			}
			break;
		}
		case STRING_PATTERN: {
			final int n_elements = text_buf.pull_int().getInt();
			pattern_value = new char[n_elements];
			final byte[] temp = new byte[n_elements];
			text_buf.pull_raw(n_elements, temp);
			for (int i = 0; i < n_elements; i++) {
				pattern_value[i] = (char)temp[i];
			}
			break;
		}
		default:
			throw new TtcnError("Text decoder: An unknown/unsupported selection was received for a octetstring template.");
		}
	}

	@Override
	public void check_restriction(final template_res restriction, final String name, final boolean legacy) {
		if (templateSelection == template_sel.UNINITIALIZED_TEMPLATE) {
			return;
		}

		switch ((name != null && restriction == template_res.TR_VALUE) ? template_res.TR_OMIT : restriction) {
		case TR_VALUE:
			if (!is_ifPresent && templateSelection == template_sel.SPECIFIC_VALUE) {
				return;
			}
			break;
		case TR_OMIT:
			if (!is_ifPresent && (templateSelection == template_sel.OMIT_VALUE || templateSelection == template_sel.SPECIFIC_VALUE)) {
				return;
			}
			break;
		case TR_PRESENT:
			if (!match_omit(legacy)) {
				return;
			}
			break;
		default:
			return;
		}

		throw new TtcnError(MessageFormat.format("Restriction `{0}'' on template of type {1} violated.", getResName(restriction), name == null ? "octetstring" : name));
	}
}
