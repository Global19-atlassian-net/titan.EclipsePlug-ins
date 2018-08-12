/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;

import org.eclipse.titan.runtime.core.Optional.optional_sel;

/**
 * Part of the representation of the ASN.1 EXTERNAL type
 *
 * @author Kristof Szabados
 */
public class TitanExternal extends Base_Type {
	TitanExternal_identification identification; //ASN1_Choice_Type
	Optional<TitanUniversalCharString> data__value__descriptor; //ObjectDescriptor_Type
	TitanOctetString data__value; //OctetString_Type

	public TitanExternal() {
		identification = new TitanExternal_identification();
		data__value__descriptor = new Optional<TitanUniversalCharString>(TitanUniversalCharString.class);
		data__value = new TitanOctetString();
	}

	public TitanExternal( final TitanExternal_identification aIdentification, final Optional<TitanUniversalCharString> aData__value__descriptor, final TitanOctetString aData__value ) {
		identification = new TitanExternal_identification( aIdentification );
		data__value__descriptor = new Optional<TitanUniversalCharString>(TitanUniversalCharString.class);
		this.data__value__descriptor.assign( aData__value__descriptor );
		data__value = new TitanOctetString( aData__value );
	}

	public TitanExternal( final TitanExternal aOtherValue ) {
		this();
		assign( aOtherValue );
	}

	public TitanExternal assign( final TitanExternal aOtherValue ) {
		if ( !aOtherValue.isBound() ) {
			throw new TtcnError( "Assignment of an unbound value of type EXTERNAL" );
		}

		if (aOtherValue != this) {
			if ( aOtherValue.getIdentification().isBound() ) {
				this.identification.assign( aOtherValue.getIdentification() );
			} else {
				this.identification.cleanUp();
			}
			if ( aOtherValue.getData__value__descriptor().isBound() ) {
				this.data__value__descriptor.assign( aOtherValue.getData__value__descriptor() );
			} else {
				this.data__value__descriptor.cleanUp();
			}
			if ( aOtherValue.getData__value().isBound() ) {
				this.data__value.assign( aOtherValue.getData__value() );
			} else {
				this.data__value.cleanUp();
			}
		}

		return this;
	}

	@Override
	public TitanExternal assign(final Base_Type otherValue) {
		if (otherValue instanceof TitanExternal ) {
			return assign((TitanExternal) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to EXTERNAL", otherValue));
	}

	public void cleanUp() {
		identification.cleanUp();
		data__value__descriptor.cleanUp();
		data__value.cleanUp();
	}

	public boolean isBound() {
		if ( identification.isBound() ) { return true; }
		if ( optional_sel.OPTIONAL_OMIT.equals(data__value__descriptor.get_selection()) || data__value__descriptor.isBound() ) { return true; }
		if ( data__value.isBound() ) { return true; }
		return false;
	}

	public boolean isPresent() {
		return isBound();
	}

	public boolean isValue() {
		if ( !identification.isValue() ) { return false; }
		if ( !optional_sel.OPTIONAL_OMIT.equals(data__value__descriptor.get_selection()) && !data__value__descriptor.isValue() ) { return false; }
		if ( !data__value.isValue() ) { return false; }
		return true;
	}

	public boolean operatorEquals( final TitanExternal aOtherValue ) {
		if ( !this.identification.operatorEquals( aOtherValue.identification ) ) { return false; }
		if ( !this.data__value__descriptor.operatorEquals( aOtherValue.data__value__descriptor ) ) { return false; }
		if ( !this.data__value.operatorEquals( aOtherValue.data__value ) ) { return false; }
		return true;
	}

	@Override
	public boolean operatorEquals(final Base_Type otherValue) {
		if (otherValue instanceof TitanExternal ) {
			return operatorEquals((TitanExternal) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to EXTERNAL", otherValue));		}

	public TitanExternal_identification getIdentification() {
		return identification;
	}

	public TitanExternal_identification constGetIdentification() {
		return identification;
	}

	public Optional<TitanUniversalCharString> getData__value__descriptor() {
		return data__value__descriptor;
	}

	public Optional<TitanUniversalCharString> constGetData__value__descriptor() {
		return data__value__descriptor;
	}

	public TitanOctetString getData__value() {
		return data__value;
	}

	public TitanOctetString constGetData__value() {
		return data__value;
	}

	public TitanInteger sizeOf() {
		int sizeof = 0;
		if (data__value__descriptor.isPresent()) {
			sizeof++;
		}
		sizeof += 2;
		return new TitanInteger(sizeof);
	}

	public void log() {
		if (!isBound()) {
			TTCN_Logger.log_event_unbound();
			return;
		}
		TTCN_Logger.log_char('{');
		TTCN_Logger.log_event_str(" identification := ");
		identification.log();
		TTCN_Logger.log_char(',');
		TTCN_Logger.log_event_str(" data-value-descriptor := ");
		data__value__descriptor.log();
		TTCN_Logger.log_char(',');
		TTCN_Logger.log_event_str(" data-value := ");
		data__value.log();
		TTCN_Logger.log_event_str(" }");
	}

	@Override
	public void encode_text(final Text_Buf text_buf) {
		identification.encode_text(text_buf);
		data__value__descriptor.encode_text(text_buf);
		data__value.encode_text(text_buf);
	}

	@Override
	public void decode_text(final Text_Buf text_buf) {
		identification.decode_text(text_buf);
		data__value__descriptor.decode_text(text_buf);
		data__value.decode_text(text_buf);
	}
}