/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;

/**
 * Part of the representation of the ASN.1 EMBEDDED PDV type
 *
 * @author Kristof Szabados
 */
public class TitanEmbedded_PDV_identification_syntaxes extends Base_Type {
	TitanObjectid abstract_; //ObjectID_Type
	TitanObjectid transfer; //ObjectID_Type

	public TitanEmbedded_PDV_identification_syntaxes() {
		abstract_ = new TitanObjectid();
		transfer = new TitanObjectid();
	}

	public TitanEmbedded_PDV_identification_syntaxes( final TitanObjectid aAbstract_, final TitanObjectid aTransfer ) {
		abstract_ = new TitanObjectid( aAbstract_ );
		transfer = new TitanObjectid( aTransfer );
	}

	public TitanEmbedded_PDV_identification_syntaxes( final TitanEmbedded_PDV_identification_syntaxes aOtherValue ) {
		this();
		assign( aOtherValue );
	}

	public TitanEmbedded_PDV_identification_syntaxes assign( final TitanEmbedded_PDV_identification_syntaxes aOtherValue ) {
		if ( !aOtherValue.isBound() ) {
			throw new TtcnError( "Assignment of an unbound value of type EMBEDDED PDV.identification.syntaxes" );
		}

		if (aOtherValue != this) {
			if ( aOtherValue.getAbstract_().isBound() ) {
				this.abstract_.assign( aOtherValue.getAbstract_() );
			} else {
				this.abstract_.cleanUp();
			}
			if ( aOtherValue.getTransfer().isBound() ) {
				this.transfer.assign( aOtherValue.getTransfer() );
			} else {
				this.transfer.cleanUp();
			}
		}


		return this;
	}

	@Override
	public TitanEmbedded_PDV_identification_syntaxes assign(final Base_Type otherValue) {
		if (otherValue instanceof TitanEmbedded_PDV_identification_syntaxes ) {
			return assign((TitanEmbedded_PDV_identification_syntaxes) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to EMBEDDED PDV.identification.syntaxes", otherValue));
	}

	public void cleanUp() {
		abstract_.cleanUp();
		transfer.cleanUp();
	}

	public boolean isBound() {
		if ( abstract_.isBound() ) { return true; }
		if ( transfer.isBound() ) { return true; }
		return false;
	}

	public boolean isPresent() {
		return isBound();
	}

	public boolean isValue() {
		if ( !abstract_.isValue() ) { return false; }
		if ( !transfer.isValue() ) { return false; }
		return true;
	}

	public boolean operatorEquals( final TitanEmbedded_PDV_identification_syntaxes aOtherValue ) {
		if ( !this.abstract_.operatorEquals( aOtherValue.abstract_ ) ) { return false; }
		if ( !this.transfer.operatorEquals( aOtherValue.transfer ) ) { return false; }
		return true;
	}

	@Override
	public boolean operatorEquals(final Base_Type otherValue) {
		if (otherValue instanceof TitanEmbedded_PDV_identification_syntaxes ) {
			return operatorEquals((TitanEmbedded_PDV_identification_syntaxes) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to EMBEDDED PDV.identification.syntaxes", otherValue));
	}

	public TitanObjectid getAbstract_() {
		return abstract_;
	}

	public TitanObjectid constGetAbstract_() {
		return abstract_;
	}

	public TitanObjectid getTransfer() {
		return transfer;
	}

	public TitanObjectid constGetTransfer() {
		return transfer;
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
		TTCN_Logger.log_event_str(" abstract := ");
		abstract_.log();
		TTCN_Logger.log_char(',');
		TTCN_Logger.log_event_str(" transfer := ");
		transfer.log();
		TTCN_Logger.log_event_str(" }");
	}

	@Override
	public void encode_text(final Text_Buf text_buf) {
		abstract_.encode_text(text_buf);
		transfer.encode_text(text_buf);
	}

	@Override
	public void decode_text(final Text_Buf text_buf) {
		abstract_.decode_text(text_buf);
		transfer.decode_text(text_buf);
	}
}