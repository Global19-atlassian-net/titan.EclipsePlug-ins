/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.HexString_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a hexstring value.
 *
 * @author Kristof Szabados
 */
public final class Hexstring_Value extends Value {

	private final String value;

	public Hexstring_Value(final String value) {
		this.value = value;
	}

	@Override
	/** {@inheritDoc} */
	public Value_type getValuetype() {
		return Value_type.HEXSTRING_VALUE;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_HEXSTRING;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append('\'').append(value).append("\'H");

		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public IType getExpressionGovernor(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (myGovernor != null) {
			return myGovernor;
		}

		return new HexString_Type();
	}

	@Override
	/** {@inheritDoc} */
	public IValue getReferencedSubValue(final CompilationTimeStamp timestamp, final Reference reference,
			final int actualSubReference, final IReferenceChain refChain) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return this;
		}

		final IType type = myGovernor.getTypeRefdLast(timestamp);
		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			final Value arrayIndex = ((ArraySubReference) subreference).getValue();
			final IValue valueIndex = arrayIndex.getValueRefdLast(timestamp, refChain);
			if (!valueIndex.isUnfoldable(timestamp)) {
				if (Value_type.INTEGER_VALUE.equals(valueIndex.getValuetype())) {
					final int index = ((Integer_Value) valueIndex).intValue();
					return getStringElement(index, arrayIndex.getLocation());
				}

				arrayIndex.getLocation().reportSemanticError(ArraySubReference.INTEGERINDEXEXPECTED);
				return null;
			}
			return null;
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(
					FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId().getDisplayName(), type.getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(ParameterisedSubReference.INVALIDVALUESUBREFERENCE);
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		return false;
	}

	public String getValue() {
		return value;
	}

	public int getValueLength() {
		if (value == null || isErroneous) {
			return 0;
		}

		return value.length();
	}

	public Hexstring_Value getStringElement(final int index, final Location location) {
		if (value == null) {
			return null;
		}

		if (index < 0) {
			location.reportSemanticError(MessageFormat.format(Bitstring_Value.NEGATIVEINDEX, index));
			return null;
		} else if (index >= value.length()) {
			location.reportSemanticError(MessageFormat.format(Bitstring_Value.INDEXOWERFLOW, index, value.length()));
			return null;
		}

		final Hexstring_Value result = new Hexstring_Value(value.substring(index, index + 1));
		result.setMyGovernor(myGovernor);
		result.setMyScope(myScope);
		return result;
	}

	@Override
	/** {@inheritDoc} */
	public Value setValuetype(final CompilationTimeStamp timestamp, final Value_type newType) {
		switch (newType) {
		case BITSTRING_VALUE:
			return new Bitstring_Value(this);
		case OCTETSTRING_VALUE:
			return new Octetstring_Value(this);
		default:
			return super.setValuetype(timestamp, newType);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = other.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		return Value_type.HEXSTRING_VALUE.equals(last.getValuetype()) && value.equals(((Hexstring_Value) last).getValue());
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIsvalue(final boolean fromSequence) {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		// no members
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		if (myGovernor != null) {
			switch (myGovernor.getTypetype()) {
			case TYPE_BITSTRING:
				aData.addBuiltinTypeImport( "TitanBitString" );
				source.append(MessageFormat.format("{0}.assign(new TitanBitString(\"{1}\"));\n", name, value));
				return source;
			case TYPE_OCTETSTRING:
				aData.addBuiltinTypeImport( "TitanOctetString" );
				source.append(MessageFormat.format("{0}.assign(new TitanOctetString(\"{1}\"));\n", name, value));
				return source;
			default:
				break;
			}
		}

		aData.addBuiltinTypeImport( "TitanHexString" );

		source.append(MessageFormat.format("{0}.assign(new TitanHexString(\"{1}\"));\n", name, value));
		return source;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateSingleExpression(final JavaGenData aData) {
		final StringBuilder result = new StringBuilder();

		if (myGovernor != null) {
			switch (myGovernor.getTypetype()) {
			case TYPE_BITSTRING:
				aData.addBuiltinTypeImport( "TitanBitString" );
				result.append(MessageFormat.format("new TitanBitString(\"{0}\")\n", value));
				return result;
			case TYPE_OCTETSTRING:
				aData.addBuiltinTypeImport( "TitanOctetString" );
				result.append(MessageFormat.format("new TitanOctetString(\"{0}\")", value));
				return result;
			default:
				break;
			}
		}

		aData.addBuiltinTypeImport( "TitanHexString" );
		result.append(MessageFormat.format("new TitanHexString(\"{0}\")", value));

		return result;
	}
}
