/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;

import org.eclipse.titan.runtime.core.RAW.RAW_enc_tr_pos;
import org.eclipse.titan.runtime.core.RAW.RAW_enc_tree;
import org.eclipse.titan.runtime.core.TTCN_EncDec.coding_type;
import org.eclipse.titan.runtime.core.TTCN_EncDec.error_type;
import org.eclipse.titan.runtime.core.TTCN_EncDec.raw_order_t;

/**
 * Part of the representation of the ASN.1 unrestricted string (CHARACTER STRING) type.
 *
 * @author Kristof Szabados
 */
public class TitanCharacter_String_identification_context__negotiation extends Base_Type {
	final TitanInteger presentation__context__id; //ASN1_Integer_Type
	final TitanObjectid transfer__syntax; //ObjectID_Type

	public TitanCharacter_String_identification_context__negotiation() {
		presentation__context__id = new TitanInteger();
		transfer__syntax = new TitanObjectid();
	}

	public TitanCharacter_String_identification_context__negotiation( final TitanInteger aPresentation__context__id, final TitanObjectid aTransfer__syntax ) {
		presentation__context__id = new TitanInteger( aPresentation__context__id );
		transfer__syntax = new TitanObjectid( aTransfer__syntax );
	}

	public TitanCharacter_String_identification_context__negotiation( final TitanCharacter_String_identification_context__negotiation aOtherValue ) {
		if(!aOtherValue.isBound()) {
			throw new TtcnError("Copying of an unbound value of type CHARACTER STRING.identification.context-negotiation.");
		}
		presentation__context__id = new TitanInteger();
		transfer__syntax = new TitanObjectid();
		assign( aOtherValue );
	}

	public TitanCharacter_String_identification_context__negotiation assign(final TitanCharacter_String_identification_context__negotiation aOtherValue ) {
		if ( !aOtherValue.isBound() ) {
			throw new TtcnError( "Assignment of an unbound value of type CHARACTER STRING.identification.context-negotiation");
		}

		if (aOtherValue != this) {
			if ( aOtherValue.getPresentation__context__id().isBound() ) {
				this.presentation__context__id.assign( aOtherValue.getPresentation__context__id() );
			} else {
				this.presentation__context__id.cleanUp();
			}
			if ( aOtherValue.getTransfer__syntax().isBound() ) {
				this.transfer__syntax.assign( aOtherValue.getTransfer__syntax() );
			} else {
				this.transfer__syntax.cleanUp();
			}
		}

		return this;
	}

	@Override
	public TitanCharacter_String_identification_context__negotiation assign(final Base_Type otherValue) {
		if (otherValue instanceof TitanCharacter_String_identification_context__negotiation ) {
			return assign((TitanCharacter_String_identification_context__negotiation) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to CHARACTER STRING.identification.context-negotiation", otherValue));
	}

	public void cleanUp() {
		presentation__context__id.cleanUp();
		transfer__syntax.cleanUp();
	}

	@Override
	public boolean isBound() {
		if ( presentation__context__id.isBound() ) { return true; }
		if ( transfer__syntax.isBound() ) { return true; }
		return false;
	}

	@Override
	public boolean isPresent() {
		return isBound();
	}

	@Override
	public boolean isValue() {
		if ( !presentation__context__id.isValue() ) { return false; }
		if ( !transfer__syntax.isValue() ) { return false; }
		return true;
	}

	public boolean operatorEquals( final TitanCharacter_String_identification_context__negotiation aOtherValue ) {
		if ( !this.presentation__context__id.operatorEquals( aOtherValue.presentation__context__id ) ) { return false; }
		if ( !this.transfer__syntax.operatorEquals( aOtherValue.transfer__syntax ) ) { return false; }
		return true;
	}

	@Override
	public boolean operatorEquals(final Base_Type otherValue) {
		if (otherValue instanceof TitanCharacter_String_identification_context__negotiation ) {
			return operatorEquals((TitanCharacter_String_identification_context__negotiation) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to CHARACTER STRING.identification.context-negotiation", otherValue));
	}

	public TitanInteger getPresentation__context__id() {
		return presentation__context__id;
	}

	public TitanInteger constGetPresentation__context__id() {
		return presentation__context__id;
	}

	public TitanObjectid getTransfer__syntax() {
		return transfer__syntax;
	}

	public TitanObjectid constGetTransfer__syntax() {
		return transfer__syntax;
	}

	public TitanInteger sizeOf() {
		int sizeof = 0;
		sizeof += 2;
		return new TitanInteger(sizeof);
	}
	public void log() {
		if (!isBound()) {
			TTCN_Logger.log_event_unbound();
			return;
		}
		TTCN_Logger.log_char('{');
		TTCN_Logger.log_event_str(" presentation-context-id := ");
		presentation__context__id.log();
		TTCN_Logger.log_char(',');
		TTCN_Logger.log_event_str(" transfer-syntax := ");
		transfer__syntax.log();
		TTCN_Logger.log_event_str(" }");
	}

	@Override
	public void set_implicit_omit() {
		if (presentation__context__id.isBound()) {
			presentation__context__id.set_implicit_omit();
		}
		if (transfer__syntax.isBound()) {
			transfer__syntax.set_implicit_omit();
		}
	}

	@Override
	public void encode_text(final Text_Buf text_buf) {
		presentation__context__id.encode_text(text_buf);
		transfer__syntax.encode_text(text_buf);
	}

	@Override
	public void decode_text(final Text_Buf text_buf) {
		presentation__context__id.decode_text(text_buf);
		transfer__syntax.decode_text(text_buf);
	}

	@Override
	public void encode(final TTCN_Typedescriptor p_td, final TTCN_Buffer p_buf, final coding_type p_coding, final int flavour) {
		switch (p_coding) {
		case CT_RAW: {
			final TTCN_EncDec_ErrorContext errorContext = new TTCN_EncDec_ErrorContext("While RAW-encoding type '%s': ", p_td.name);
			if (p_td.raw == null) {
				TTCN_EncDec_ErrorContext.error_internal("No RAW descriptor available for type '%s'.", p_td.name);
			}
			final RAW_enc_tr_pos rp = new RAW_enc_tr_pos(0, null);
			final RAW_enc_tree root = new RAW_enc_tree(false, null, rp, 1, p_td.raw);
			RAW_encode(p_td, root);
			root.put_to_buf(p_buf);
			errorContext.leaveContext();
			break;
		}
		default:
			throw new TtcnError(MessageFormat.format("Unknown coding method requested to encode type `{0}''", p_td.name));
		}
	}

	@Override
	public void decode(final TTCN_Typedescriptor p_td, final TTCN_Buffer p_buf, final coding_type p_coding, final int flavour) {
		switch (p_coding) {
		case CT_RAW: {
			final TTCN_EncDec_ErrorContext errorContext = new TTCN_EncDec_ErrorContext("While RAW-decoding type '%s': ", p_td.name);
			if (p_td.raw == null) {
				TTCN_EncDec_ErrorContext.error_internal("No RAW descriptor available for type '%s'.", p_td.name);
			}
			raw_order_t order;
			switch (p_td.raw.top_bit_order) {
			case TOP_BIT_LEFT:
				order = raw_order_t.ORDER_LSB;
				break;
			case TOP_BIT_RIGHT:
			default:
				order = raw_order_t.ORDER_MSB;
				break;
			}
			final int rawr = RAW_decode(p_td, p_buf, p_buf.get_len() * 8, order);
			if (rawr < 0) {
				final error_type temp = error_type.values()[-rawr];
				switch (temp) {
				case ET_INCOMPL_MSG:
				case ET_LEN_ERR:
					TTCN_EncDec_ErrorContext.error(temp, "Can not decode type '%s', because invalid or incomplete message was received", p_td.name);
					break;
				case ET_UNBOUND:
				default:
					TTCN_EncDec_ErrorContext.error(error_type.ET_INVAL_MSG, "Can not decode type '%s', because invalid or incomplete message was received", p_td.name);
					break;
				}
			}
			errorContext.leaveContext();
			break;
		}
		default:
			throw new TtcnError(MessageFormat.format("Unknown coding method requested to decode type `{0}''", p_td.name));
		}
	}
}