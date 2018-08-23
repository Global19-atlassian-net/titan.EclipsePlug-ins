/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;


public class TitanBitString_Element {
	private boolean bound_flag;
	private TitanBitString str_val;
	private int bit_pos;

	public TitanBitString_Element(final boolean par_bound_flag, final TitanBitString par_str_val, final int par_bit_pos) {
		bound_flag = par_bound_flag;
		str_val = par_str_val;
		bit_pos = par_bit_pos;
	}

	public boolean isBound() {
		return bound_flag;
	}

	public boolean isValue() {
		return isBound();
	}

	public void mustBound(final String aErrorMessage) {
		if (!bound_flag) {
			throw new TtcnError(aErrorMessage);
		}
	}

	// originally operator=
	public TitanBitString_Element assign(final TitanBitString_Element otherValue) {
		otherValue.mustBound("Assignment of an unbound bitstring element.");

		bound_flag = true;
		str_val.setBit(bit_pos, otherValue.str_val.getBit(otherValue.bit_pos));
		return this;
	}

	// originally operator=
	public TitanBitString_Element assign(final TitanBitString otherValue) {
		otherValue.mustBound("Assignment of unbound bitstring value.");

		if (otherValue.lengthOf().getInt() != 1) {
			throw new TtcnError("Assignment of a bitstring value with length other than 1 to a bitstring element.");
		}

		bound_flag = true;
		str_val.setBit(bit_pos, otherValue.getBit(0));
		return this;
	}

	// originally operator==
	public boolean operatorEquals(final TitanBitString_Element otherValue) {
		mustBound("Unbound left operand of bitstring element comparison.");
		otherValue.mustBound("Unbound right operand of bitstring comparison.");

		return str_val.getBit(bit_pos) == otherValue.str_val.getBit(otherValue.bit_pos);
	}

	// originally operator==
	public boolean operatorEquals(final TitanBitString otherValue) {
		mustBound("Unbound left operand of bitstring element comparison.");
		otherValue.mustBound("Unbound right operand of bitstring element comparison.");

		if (otherValue.lengthOf().getInt() != 1) {
			return false;
		}

		return str_val.getBit(bit_pos) == otherValue.getBit(0);
	}

	//originally operator!=
	public boolean operatorNotEquals(final TitanBitString_Element otherValue) {
		return !operatorEquals(otherValue);
	}

	//originally operator!=
	public boolean operatorNotEquals(final TitanBitString otherValue) {
		return !operatorEquals(otherValue);
	}

	//originally operator+
	public TitanBitString concatenate(final TitanBitString otherValue) {
		mustBound("Unbound left operand of bitstring element concatenation.");
		otherValue.mustBound("Unbound right operand of bitstring concatenation.");

		final int n_bits = otherValue.lengthOf().getInt();
		final int n_bytes = (n_bits + 7) / 8;
		final int result[] = new int[n_bytes];
		final int temp[] = otherValue.getValue();

		result[0] = get_bit() ? 1 : 0;
		for (int byte_count = 0; byte_count < n_bytes; byte_count++) {
			result[byte_count] = (result[byte_count] | temp[byte_count] << 1) & 0xFF;
			if (n_bits > byte_count * 8 + 7) {
				result[byte_count + 1] = (temp[byte_count] & 128) >> 7;
			}
		}

		return new TitanBitString(result, n_bits + 1);
	}

	// originally operator+
	public TitanBitString concatenate(final TitanBitString_Element otherValue) {
		mustBound("Unbound left operand of bitstring element concatenation.");
		otherValue.mustBound("Unbound right operand of bitstring element concatenation.");

		int result = str_val.getBit(bit_pos) ? 1 : 2;
		if (otherValue.get_bit()) {
			result = result | 2;
		}
		final int temp_ptr[] = new int[1];
		temp_ptr[0] = result;
		return new TitanBitString(temp_ptr, 2);
	}

	// originally operator~
	public TitanBitString not4b() {
		mustBound("Unbound bitstring element operand of operator not4b.");

		final int result = str_val.getBit(bit_pos) ? 0 : 1;
		final int dest_ptr[] = new int[1];
		dest_ptr[0] = result;
		return new TitanBitString(dest_ptr, 1);
	}

	// originally operator&
	public TitanBitString and4b(final TitanBitString otherValue) {
		mustBound("Left operand of operator and4b is an unbound bitstring element.");
		otherValue.mustBound("Right operand of operator and4b is an unbound bitstring value.");

		if (otherValue.lengthOf().getInt() != 1) {
			throw new TtcnError("The bitstring operands of operator and4b must have the same length.");
		}

		final boolean temp = str_val.getBit(bit_pos) & otherValue.getBit(0);
		final int result = temp ? 1 : 0;
		final int dest_ptr[] = new int[1];
		dest_ptr[0] = result;
		return new TitanBitString(dest_ptr, 1);
	}

	// originally operator&
	public TitanBitString and4b(final TitanBitString_Element otherValue) {
		mustBound("Left operand of operator and4b is an unbound bitstring element.");
		otherValue.mustBound("Right operand of operator and4b is an unbound bitstring element.");

		final boolean temp = str_val.getBit(bit_pos) & otherValue.get_bit();
		final int result = temp ? 1 : 0;
		final int dest_ptr[] = new int[1];
		dest_ptr[0] = result;
		return new TitanBitString(dest_ptr, 1);
	}

	// originally operator|
	public TitanBitString or4b(final TitanBitString otherValue) {
		mustBound("Left operand of operator or4b is an unbound bitstring element.");
		otherValue.mustBound("Right operand of operator or4b is an unbound bitstring value.");

		if (otherValue.lengthOf().getInt() != 1) {
			throw new TtcnError("The bitstring operands of operator or4b must have the same length.");
		}

		final boolean temp = str_val.getBit(bit_pos) | otherValue.getBit(0);
		final int result = temp ? 1 : 0;
		final int dest_ptr[] = new int[1];
		dest_ptr[0] = result;
		return new TitanBitString(dest_ptr, 1);
	}

	// originally operator|
	public TitanBitString or4b(final TitanBitString_Element otherValue) {
		mustBound("Left operand of operator or4b is an unbound bitstring element.");
		otherValue.mustBound("Right operand of operator or4b is an unbound bitstring element.");

		final boolean temp = str_val.getBit(bit_pos) | otherValue.get_bit();
		final int result = temp ? 1 : 0;
		final int dest_ptr[] = new int[1];
		dest_ptr[0] = result;
		return new TitanBitString(dest_ptr, 1);
	}

	//originally operator^
	public TitanBitString xor4b(final TitanBitString otherValue) {
		mustBound("Left operand of operator xor4b is an unbound bitstring element.");
		otherValue.mustBound("Right operand of operator xor4b is an unbound bitstring value.");

		if (otherValue.lengthOf().getInt() != 1) {
			throw new TtcnError("The bitstring operands of operator xor4b must have the same length.");
		}

		final boolean temp = str_val.getBit(bit_pos) ^ otherValue.getBit(0);
		final int result = temp ? 1 : 0;
		final int dest_ptr[] = new int[1];
		dest_ptr[0] = result;
		return new TitanBitString(dest_ptr, 1);
	}

	//originally operator^
	public TitanBitString xor4b(final TitanBitString_Element otherValue) {
		mustBound("Left operand of operator xor4b is an unbound bitstring element.");
		otherValue.mustBound("Right operand of operator xor4b is an unbound bitstring element.");

		final boolean temp = str_val.getBit(bit_pos) ^ otherValue.get_bit();
		final int result = temp ? 1 : 0;
		final int dest_ptr[] = new int[1];
		dest_ptr[0] = result;
		return new TitanBitString(dest_ptr, 1);
	}

	public boolean get_bit() {
		return str_val.getBit(bit_pos);
	}

	public void log() {
		if (bound_flag) {
			TTCN_Logger.log_char('\'');
			TTCN_Logger.log_char(str_val.getBit(bit_pos) ? '1' : '0');
			TTCN_Logger.log_event_str("'B");
		} else {
			TTCN_Logger.log_event_unbound();
		}
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append('\'');
		result.append(str_val.getBit(bit_pos) ? '1' : '0');
		result.append("\'B");

		return result.toString();
	}
}
