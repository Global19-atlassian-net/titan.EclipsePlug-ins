/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.titan.runtime.core.Param_Types.Module_Parameter;
import org.eclipse.titan.runtime.core.Param_Types.Module_Parameter.basic_check_bits_t;
import org.eclipse.titan.runtime.core.Param_Types.Module_Parameter.type_t;

/**
 * TTCN-3 integer template
 *
 * Not yet complete rewrite
 */
public class TitanInteger_template extends Base_Template {
	// int_val part
	// TODO maybe should be renamed in core
	private TitanInteger single_value;

	// value_list part
	private ArrayList<TitanInteger_template> value_list;

	// value range part
	private boolean min_is_present, max_is_present;
	private boolean min_is_exclusive, max_is_exclusive;
	private TitanInteger min_value, max_value;


	public TitanInteger_template() {
		// do nothing
	}

	public TitanInteger_template(final template_sel otherValue) {
		super(otherValue);
		checkSingleSelection(otherValue);
	}

	public TitanInteger_template(final int otherValue) {
		super(template_sel.SPECIFIC_VALUE);
		single_value = new TitanInteger(otherValue);
	}

	public TitanInteger_template(final BigInteger otherValue) {
		super(template_sel.SPECIFIC_VALUE);
		single_value = new TitanInteger(otherValue);
	}

	public TitanInteger_template(final TitanInteger otherValue) {
		super(template_sel.SPECIFIC_VALUE);
		otherValue.mustBound("Creating a template from an unbound integer value.");

		single_value = new TitanInteger(otherValue);
	}

	public TitanInteger_template(final Optional<TitanInteger> otherValue) {
		switch (otherValue.get_selection()) {
		case OPTIONAL_PRESENT:
			set_selection(template_sel.SPECIFIC_VALUE);
			single_value = new TitanInteger(otherValue.constGet());
			break;
		case OPTIONAL_OMIT:
			set_selection(template_sel.OMIT_VALUE);
			break;
		case OPTIONAL_UNBOUND:
			throw new TtcnError("Creating an integer template from an unbound optional field.");
		}
	}

	public TitanInteger_template(final TitanInteger_template otherValue) {
		copyTemplate(otherValue);
	}

	//originally clean_up
	public void cleanUp() {
		switch (templateSelection) {
		case SPECIFIC_VALUE:
			single_value = null;
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			value_list.clear();
			value_list = null;
			break;
		case VALUE_RANGE:
			min_value = null;
			max_value = null;
			break;
		default:
			break;
		}
		templateSelection = template_sel.UNINITIALIZED_TEMPLATE;
	}

	@Override
	public TitanInteger_template assign(final Base_Type otherValue) {
		if (otherValue instanceof TitanInteger) {
			return assign((TitanInteger)otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to integer", otherValue));
	}

	@Override
	public TitanInteger_template assign(final Base_Template otherValue) {
		if (otherValue instanceof TitanInteger_template) {
			return assign((TitanInteger_template)otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to integer", otherValue));
	}

	@Override
	public void log_match(final Base_Type match_value, final boolean legacy) {
		if (match_value instanceof TitanInteger) {
			log_match((TitanInteger) match_value, legacy);
			return;
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to integer", match_value));
	}

	// originally operator=
	public TitanInteger_template assign(final template_sel otherValue) {
		checkSingleSelection(otherValue);
		cleanUp();
		set_selection(otherValue);

		return this;
	}

	// originally operator=
	public TitanInteger_template assign(final int otherValue) {
		cleanUp();
		set_selection(template_sel.SPECIFIC_VALUE);
		single_value = new TitanInteger(otherValue);

		return this;
	}

	// originally operator=
	public TitanInteger_template assign(final BigInteger otherValue) {
		cleanUp();
		set_selection(template_sel.SPECIFIC_VALUE);
		single_value = new TitanInteger(otherValue);

		return this;
	}

	// originally operator=
	public TitanInteger_template assign(final TitanInteger otherValue) {
		otherValue.mustBound("Assignment of an unbound integer value to a template.");

		cleanUp();
		set_selection(template_sel.SPECIFIC_VALUE);
		single_value = new TitanInteger(otherValue);

		return this;
	}

	// originally operator=
	public TitanInteger_template assign(final TitanInteger_template otherValue) {
		if (otherValue != this) {
			cleanUp();
			copyTemplate(otherValue);
		}

		return this;
	}

	private void copyTemplate(final TitanInteger_template otherValue) {
		switch (otherValue.templateSelection) {
		case SPECIFIC_VALUE:
			single_value = new TitanInteger(otherValue.single_value);
			break;
		case OMIT_VALUE:
		case ANY_VALUE:
		case ANY_OR_OMIT:
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			value_list = new ArrayList<TitanInteger_template>(otherValue.value_list.size());
			for (int i = 0; i < otherValue.value_list.size(); i++) {
				final TitanInteger_template temp = new TitanInteger_template(otherValue.value_list.get(i));
				value_list.add(temp);
			}
			break;
		case VALUE_RANGE:
			min_is_present = otherValue.min_is_present;
			min_is_exclusive = otherValue.min_is_exclusive;
			if (min_is_present) {
				min_value = new TitanInteger(otherValue.min_value);
			}
			max_is_present = otherValue.max_is_present;
			max_is_exclusive = otherValue.max_is_exclusive;
			if (max_is_present) {
				max_value = new TitanInteger(otherValue.max_value);
			}
			break;
		default:
			throw new TtcnError("Copying an uninitialized/unsupported integer template.");
		}

		set_selection(otherValue);
	}

	@Override
	public boolean match(final Base_Type otherValue, final boolean legacy) {
		if (otherValue instanceof TitanInteger) {
			return match((TitanInteger) otherValue, legacy);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to integer", otherValue));
	}

	// originally match
	public boolean match(final TitanInteger otherValue) {
		return match(otherValue, false);
	}

	// originally match
	public boolean match(final TitanInteger otherValue, final boolean legacy) {
		if (!otherValue.isBound()) {
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
		case VALUE_RANGE: {
			boolean lowerMatch = true;
			boolean upperMatch = true;
			if (min_is_present) {
				if (min_is_exclusive) {
					lowerMatch = min_value.isLessThan(otherValue);
				} else {
					lowerMatch = min_value.isLessThanOrEqual(otherValue);
				}
			}
			if (max_is_present) {
				if (max_is_exclusive) {
					upperMatch = max_value.isGreaterThan(otherValue);
				} else {
					upperMatch = max_value.isGreaterThanOrEqual(otherValue);
				}
			}
			return lowerMatch && upperMatch;
		}
		default:
			throw new TtcnError("Matching with an uninitialized/unsupported integer template.");
		}
	}

	public TitanInteger valueOf() {
		if (templateSelection != template_sel.SPECIFIC_VALUE) {
			throw new TtcnError("Performing a valueof or send operation on a non-specific integer template.");
		}

		return new TitanInteger(single_value);
	}

	// originally set_type
	public void setType(final template_sel templateType) {
		setType(templateType, 0);
	}

	// originally set_type
	public void setType(final template_sel templateType, final int listLength) {
		cleanUp();
		switch (templateType) {
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			set_selection(templateType);
			value_list = new ArrayList<TitanInteger_template>(listLength);
			for (int i = 0; i < listLength; i++) {
				value_list.add(new TitanInteger_template());
			}
			break;
		case VALUE_RANGE:
			set_selection(template_sel.VALUE_RANGE);
			min_is_present = false;
			max_is_present = false;
			min_is_exclusive = false;
			max_is_exclusive = false;
			break;
		default:
			throw new TtcnError("Setting an invalid type for an integer template.");
		}
	}

	// originally list_iem
	public TitanInteger_template listItem(final int listIndex) {
		if (!template_sel.VALUE_LIST.equals(templateSelection) &&
				!template_sel.COMPLEMENTED_LIST.equals(templateSelection)) {
			throw new TtcnError("Accessing a list element of a non-list integer template.");
		}
		if (listIndex < 0) {
			throw new TtcnError("Accessing an integer value list template using a negative index (" + listIndex + ").");
		}
		if (listIndex > value_list.size()) {
			throw new TtcnError("Index overflow in an integer value list template.");
		}

		return value_list.get(listIndex);
	}

	// originally set_min
	public void setMin(final int otherMinValue) {
		if (!template_sel.VALUE_RANGE.equals(templateSelection)) {
			throw new TtcnError("Integer template is not range when setting lower limit.");
		}

		if (max_is_present) {
			if (!max_value.isGreaterThanOrEqual(otherMinValue)) {
				throw new TtcnError("The lower limit of the range is greater than the upper limit in an integer template.");
			}
		}

		min_is_present = true;
		min_is_exclusive = false;
		min_value = new TitanInteger(otherMinValue);
	}

	// originally set_min
	public void setMin(final TitanInteger otherMinValue) {
		otherMinValue.mustBound("Using an unbound value when setting the lower bound in an integer range template.");

		if (!template_sel.VALUE_RANGE.equals(templateSelection)) {
			throw new TtcnError("Integer template is not range when setting lower limit.");
		}

		if (max_is_present) {
			if (!max_value.isGreaterThanOrEqual(otherMinValue)) {
				throw new TtcnError("The lower limit of the range is greater than the upper limit in an integer template.");
			}
		}

		min_is_present = true;
		min_is_exclusive = false;
		min_value = otherMinValue;
	}

	//originally set_min_exclusive
	public void setMinExclusive(final boolean minExclusive) {
		min_is_exclusive = minExclusive;
	}

	// originally set_max
	public void setMax(final int otherMaxValue) {
		if (!template_sel.VALUE_RANGE.equals(templateSelection)) {
			throw new TtcnError("Integer template is not range when setting upper limit.");
		}

		if (min_is_present) {
			if (min_value.isGreaterThan(otherMaxValue)) {
				throw new TtcnError("The upper limit of the range is smaller than the lower limit in an integer template.");
			}
		}

		max_is_present = true;
		max_is_exclusive = false;
		max_value = new TitanInteger(otherMaxValue);
	}

	// originally set_max
	public void setMax(final TitanInteger otherMaxValue) {
		otherMaxValue.mustBound("Using an unbound value when setting the upper bound in an integer range template.");

		if (!template_sel.VALUE_RANGE.equals(templateSelection)) {
			throw new TtcnError("Integer template is not range when setting upper limit.");
		}

		if (max_is_present) {
			if (!max_value.isGreaterThan(otherMaxValue)) {
				throw new TtcnError("TThe upper limit of the range is smaller than the lower limit in an integer template.");
			}
		}

		max_is_present = true;
		max_is_exclusive = false;
		max_value = otherMaxValue;
	}

	//originally set_max_exclusive
	public void setMaxExclusive(final boolean maxExclusive) {
		max_is_exclusive = maxExclusive;
	}

	// originally is_present (with default parameter)
	public boolean isPresent() {
		return isPresent(false);
	}

	// originally is_present
	public boolean isPresent(final boolean legacy) {
		if (templateSelection == template_sel.UNINITIALIZED_TEMPLATE) {
			return false;
		}

		return !match_omit(legacy);
	}

	// originally match_omit (with default parameter)
	public boolean match_omit() {
		return match_omit(false);
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

	public void log() {
		switch (templateSelection) {
		case SPECIFIC_VALUE: {
			String tmp_str;
			if (single_value.isNative()) {
				tmp_str = Integer.toString(single_value.getInt());
			} else {
				tmp_str = single_value.getBigInteger().toString();
			}
			TTCN_Logger.log_event("%s", tmp_str);
			break;
		}
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
		case VALUE_RANGE:
			TTCN_Logger.log_char('(');
			if (min_is_exclusive) {
				TTCN_Logger.log_char('!');
			}
			if (min_is_present) {
				if (min_value.isNative()) {
					TTCN_Logger.log_event("%s", Integer.toString(min_value.getInt()));
				} else {
					TTCN_Logger.log_event("%s", min_value.getBigInteger().toString());
				}
			} else {
				TTCN_Logger.log_event_str("-infinity");
			}
			TTCN_Logger.log_event_str(" .. ");

			if (max_is_exclusive) {
				TTCN_Logger.log_char('!');
			}
			if (max_is_present) {
				if (max_value.isNative()) {
					TTCN_Logger.log_event("%s", Integer.toString(max_value.getInt()));
				} else {
					TTCN_Logger.log_event("%s", max_value.getBigInteger().toString());
				}
			} else {
				TTCN_Logger.log_event_str("infinity");
			}

			TTCN_Logger.log_char(')');
			break;
		default:
			log_generic();
			break;
		}
		log_ifpresent();
	}

	public void log_match(final TitanInteger match_value, final boolean legacy) {
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
	public void encode_text(final Text_Buf text_buf) {
		encode_text_base(text_buf);

		switch (templateSelection) {
		case OMIT_VALUE:
		case ANY_VALUE:
		case ANY_OR_OMIT:
			break;
		case SPECIFIC_VALUE:
			text_buf.push_int(single_value);
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST:
			text_buf.push_int(value_list.size());
			for (int i = 0; i < value_list.size(); i++) {
				value_list.get(i).encode_text(text_buf);
			}
			break;
		case VALUE_RANGE:
			text_buf.push_int(min_is_present ? 1 : 0);
			if (min_is_present) {
				text_buf.push_int(min_value);
			}
			text_buf.push_int(max_is_present ? 1 : 0);
			if (max_is_present) {
				text_buf.push_int(max_value);
			}
			break;
		default:
			throw new TtcnError("Text encoder: Encoding an uninitialized/unsupported integer template.");
		}
	}

	@Override
	/** {@inheritDoc} */
	public void decode_text(final Text_Buf text_buf) {
		cleanUp();
		decode_text_base(text_buf);

		switch (templateSelection) {
		case OMIT_VALUE:
		case ANY_VALUE:
		case ANY_OR_OMIT:
			break;
		case SPECIFIC_VALUE:
			single_value = text_buf.pull_int();
			break;
		case VALUE_LIST:
		case COMPLEMENTED_LIST: {
			final int size = text_buf.pull_int().getInt();
			value_list = new ArrayList<TitanInteger_template>(size);
			for (int i = 0; i < size; i++) {
				final TitanInteger_template temp = new TitanInteger_template();
				temp.decode_text(text_buf);
				value_list.add(temp);
			}
			break;
		}
		case VALUE_RANGE:
			min_is_present = text_buf.pull_int().getInt() != 0;
			if (min_is_present) {
				min_value = text_buf.pull_int();
			}
			max_is_present = text_buf.pull_int().getInt() != 0;
			if (max_is_present) {
				max_value = text_buf.pull_int();
			}
			min_is_exclusive = false;
			max_is_exclusive = false;
			break;
		default:
			throw new TtcnError("Text decoder: An unknown/unsupported selection was received for an integer template.");
		}
	}

	@Override
	/** {@inheritDoc} */
	public void set_param(final Module_Parameter param) {
		param.basic_check(basic_check_bits_t.BC_TEMPLATE.getValue(), "integer template");
		switch (param.get_type()) {
		case MP_Omit:
			assign(template_sel.OMIT_VALUE);
			break;
		case MP_Any:
			assign(template_sel.ANY_VALUE);
			break;
		case MP_AnyOrNone:
			assign(template_sel.ANY_OR_OMIT);
			break;
		case MP_List_Template:
		case MP_ComplementList_Template: {
			final TitanInteger_template temp = new TitanInteger_template();
			temp.setType(param.get_type() == type_t.MP_List_Template ?
					template_sel.VALUE_LIST : template_sel.COMPLEMENTED_LIST, param.get_size());
			for (int i = 0; i < param.get_size(); i++) {
				final TitanInteger_template tempElem = new TitanInteger_template();
				tempElem.set_param(param.get_elem(i));
				temp.value_list.add(tempElem);
			}
			assign(temp);
			break;
		}
		case MP_Integer:
			assign(param.get_integer());
			break;
		case MP_IntRange: {
			setType(template_sel.VALUE_RANGE);
			if (param.get_lower_int() != null) {
				setMin(param.get_lower_int());
			}
			setMinExclusive(param.get_is_min_exclusive());
			if (param.get_upper_int() != null) {
				setMax(param.get_upper_int());
			}
			setMaxExclusive(param.get_is_max_exclusive());
			break;
		}
		case MP_Expression:
			switch (param.get_expr_type()) {
			case EXPR_NEGATE: {
				final TitanInteger operand = new TitanInteger();
				operand.set_param(param.get_operand1());
				assign(operand.sub());
				break;
			}
			case EXPR_ADD: {
				final TitanInteger operand1 = new TitanInteger();
				final TitanInteger operand2 = new TitanInteger();
				operand1.set_param(param.get_operand1());
				operand2.set_param(param.get_operand2());
				assign(operand1.add(operand2));
				break;
			}
			case EXPR_SUBTRACT: {
				final TitanInteger operand1 = new TitanInteger();
				final TitanInteger operand2 = new TitanInteger();
				operand1.set_param(param.get_operand1());
				operand2.set_param(param.get_operand2());
				assign(operand1.sub(operand2));
				break;
			}
			case EXPR_MULTIPLY: {
				final TitanInteger operand1 = new TitanInteger();
				final TitanInteger operand2 = new TitanInteger();
				operand1.set_param(param.get_operand1());
				operand2.set_param(param.get_operand2());
				assign(operand1.mul(operand2));
				break;
			}
			case EXPR_DIVIDE: {
				final TitanInteger operand1 = new TitanInteger();
				final TitanInteger operand2 = new TitanInteger();
				operand1.set_param(param.get_operand1());
				operand2.set_param(param.get_operand2());
				if (operand2.operatorEquals(0)) {
					param.error("Integer division by zero.");
				}
				assign(operand1.div(operand2));
				break;
			}
			default:
				param.expr_type_error("an integer");
				break;
			}
			break;
		default:
			param.type_error("integer template");
		}
		is_ifPresent = param.get_ifpresent() || param.get_ifpresent();
	}

}
