/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;


/**
 * @author Farkas Izabella Ingrid
 * */

public class TitanOctetString_Element {
	private boolean bound_flag;
	private final TitanOctetString str_val;
	private final int nibble_pos;

	public TitanOctetString_Element(final boolean par_bound_flag, final TitanOctetString par_str_val, final int par_nibble_pos) {
		bound_flag = par_bound_flag;
		str_val = par_str_val;
		nibble_pos = par_nibble_pos;
	}

	public boolean isBound() {
		return bound_flag;
	}

	public boolean isValue() {
		return bound_flag;
	}

	public void mustBound(final String aErrorMessage) {
		if (!bound_flag) {
			throw new TtcnError(aErrorMessage);
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
		final StringBuilder sb = new StringBuilder();
		final int digit = get_nibble();
		sb.append(TitanHexString.HEX_DIGITS.charAt(digit / 16));
		sb.append(TitanHexString.HEX_DIGITS.charAt(digit % 16));
		return sb.toString();
	}

	/**
	 * Assigns the other value to this value.
	 * Overwriting the current content in the process.
	 *<p>
	 * operator= in the core.
	 *
	 * @param otherValue
	 *                the other value to assign.
	 * @return the new value object.
	 */
	public TitanOctetString_Element assign(final TitanOctetString_Element otherValue) {
		otherValue.mustBound("Assignment of an unbound octetstring element.");

		bound_flag = true;
		str_val.set_nibble(nibble_pos, otherValue.str_val.get_nibble(otherValue.nibble_pos));

		return this;
	}

	/**
	 * Assigns the other value to this value.
	 * Overwriting the current content in the process.
	 *<p>
	 * operator= in the core.
	 *
	 * @param otherValue
	 *                the other value to assign.
	 * @return the new value object.
	 */
	public TitanOctetString_Element assign(final TitanOctetString otherValue) {
		otherValue.mustBound("Assignment of unbound octetstring value.");

		if (otherValue.getValue().length != 1) {
			throw new TtcnError("Assignment of a octetstring value with length other than 1 to a octetstring element.");
		}

		bound_flag = true;
		str_val.set_nibble(nibble_pos, otherValue.get_nibble(0));
		return this;
	}

	/**
	 * Checks if the current value is equivalent to the provided one.
	 *
	 * operator== in the core
	 *
	 * @param otherValue
	 *                the other value to check against.
	 * @return {@code true} if the values are equivalent.
	 */
	public boolean operatorEquals(final TitanOctetString_Element otherValue) {
		mustBound("Unbound left operand of octetstring element comparison.");
		otherValue.mustBound("Unbound right operand of octetstring comparison.");

		return str_val.get_nibble(nibble_pos) == otherValue.str_val.get_nibble(otherValue.nibble_pos);
	}

	/**
	 * Checks if the current value is equivalent to the provided one.
	 *
	 * operator== in the core
	 *
	 * @param otherValue
	 *                the other value to check against.
	 * @return {@code true} if the values are equivalent.
	 */
	public boolean operatorEquals(final TitanOctetString otherValue) {
		mustBound("Unbound left operand of octetstring element comparison.");
		otherValue.mustBound("Unbound right operand of octetstring element comparison.");

		if (otherValue.getValue().length != 1) {
			return false;
		}

		return str_val.get_nibble(nibble_pos) == otherValue.get_nibble(0);
	}

	/**
	 * Checks if the current value is not equivalent to the provided one.
	 *
	 * operator!= in the core
	 *
	 * @param otherValue
	 *                the other value to check against.
	 * @return {@code true} if the values are not equivalent.
	 */
	public boolean operatorNotEquals(final TitanOctetString_Element otherValue) {
		return !operatorEquals(otherValue);
	}

	/**
	 * Checks if the current value is not equivalent to the provided one.
	 *
	 * operator!= in the core
	 *
	 * @param otherValue
	 *                the other value to check against.
	 * @return {@code true} if the values are not equivalent.
	 */
	public boolean operatorNotEquals(final TitanOctetString otherValue) {
		return !operatorEquals(otherValue);
	}

	// originally operator+
	public TitanOctetString concatenate(final TitanOctetString other_value) {
		mustBound("Unbound left operand of octetstring element concatenation.");
		other_value.mustBound("Unbound right operand of octetstring concatenation.");

		final char src_ptr[] = other_value.getValue();
		final int n_nibbles = src_ptr.length;
		final char dest_ptr[] = new char[1 + n_nibbles];
		dest_ptr[0] = str_val.get_nibble(nibble_pos);
		// chars in the result minus 1
		System.arraycopy(src_ptr, 0, dest_ptr, 1, n_nibbles);

		return new TitanOctetString(dest_ptr);
	}

	// originally operator+
	public TitanOctetString concatenate(final TitanOctetString_Element other_value) {
		mustBound("Unbound left operand of octetstring element concatenation.");
		other_value.mustBound("Unbound right operand of octetstring element concatenation.");

		final char dest_ptr[] = new char[2];
		dest_ptr[0] = str_val.get_nibble(nibble_pos);
		dest_ptr[1] = other_value.get_nibble();

		return new TitanOctetString(dest_ptr);
	}

	// originally operator~
	public TitanOctetString not4b() {
		mustBound("Unbound octetstring element operand of operator not4b.");

		final int temp = str_val.get_nibble(nibble_pos);
		final int digit1 = temp >> 4;
		final int digit2 = temp & 0x0F;
		final int negDigit1 = ~digit1 & 0x0F;
		final int negDigit2 = ~digit2 & 0x0F;
		return new TitanOctetString((char) ((negDigit1 << 4) + negDigit2));
	}

	// originally operator&
	public TitanOctetString and4b(final TitanOctetString other_value) {
		mustBound("Left operand of operator and4b is an unbound octetstring element.");
		other_value.mustBound("Right operand of operator and4b is an unbound octetstring value.");

		if (other_value.getValue().length != 1) {
			throw new TtcnError("The octetstring operands of operator and4b must have the same length.");
		}

		final char result = (char) (str_val.get_nibble(nibble_pos) & other_value.get_nibble(0));
		return new TitanOctetString(result);
	}

	// originally operator&
	public TitanOctetString and4b(final TitanOctetString_Element other_value) {
		mustBound("Left operand of operator and4b is an unbound octetstring element.");
		other_value.mustBound("Right operand of operator and4b is an unbound octetstring element.");

		final char result = (char) (str_val.get_nibble(nibble_pos) & other_value.str_val.get_nibble(other_value.nibble_pos));
		return new TitanOctetString(result);
	}

	// originally operator|
	public TitanOctetString or4b(final TitanOctetString other_value) {
		mustBound("Left operand of operator or4b is an unbound octetstring element.");
		other_value.mustBound("Right operand of operator or4b is an unbound octetstring value.");

		if (other_value.getValue().length != 1) {
			throw new TtcnError("The octetstring operands of operator or4b must have the same length.");
		}

		final char result = (char) (str_val.get_nibble(nibble_pos) | other_value.get_nibble(0));
		return new TitanOctetString(result);
	}

	//originally operator|
	public TitanOctetString or4b(final TitanOctetString_Element other_value) {
		mustBound("Left operand of operator or4b is an unbound octetstring element.");
		other_value.mustBound("Right operand of operator or4b is an unbound octetstring element.");

		final char result = (char) (str_val.get_nibble(nibble_pos) | other_value.str_val.get_nibble(other_value.nibble_pos));
		return new TitanOctetString(result);
	}

	//originally operator^
	public TitanOctetString xor4b(final TitanOctetString other_value) {
		mustBound("Left operand of operator xor4b is an unbound octetstring element.");
		other_value.mustBound("Right operand of operator xor4b is an unbound octetstring value.");

		if (other_value.getValue().length != 1) {
			throw new TtcnError("The octetstring operands of operator xor4b must have the same length.");
		}

		final char result = (char) (str_val.get_nibble(nibble_pos) ^ other_value.get_nibble(0));
		return new TitanOctetString(result);
	}

	//originally operator^
	public TitanOctetString xor4b(final TitanOctetString_Element other_value) {
		mustBound("Left operand of operator xor4b is an unbound octetstring element.");
		other_value.mustBound("Right operand of operator xor4b is an unbound octetstring element.");

		final char result = (char) (str_val.get_nibble(nibble_pos) ^ other_value.str_val.get_nibble(other_value.nibble_pos));
		return new TitanOctetString(result);
	}

	public char get_nibble() {
		return (char) str_val.get_nibble(nibble_pos);
	}

	public void log() {
		if (bound_flag) {
			TTCN_Logger.log_char('\'');
			TTCN_Logger.log_octet(str_val.get_nibble(nibble_pos));
			TTCN_Logger.log_event_str("'O");
		} else {
			TTCN_Logger.log_event_unbound();
		}
	}
}
