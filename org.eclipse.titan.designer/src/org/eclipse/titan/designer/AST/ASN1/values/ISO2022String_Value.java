/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.values;

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
import org.eclipse.titan.designer.AST.TTCN3.values.Charstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.Char2OctExpression;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class ISO2022String_Value extends Value {
	public static final String NEGATIVEINDEX = "A non-negative integer value was expected instead of {0} for indexing a string element";
	public static final String INDEXOWERFLOW = "Index overflow when accessing a string element: the index is {0}, but the string has only {1} elements";

	private final String value;

	public ISO2022String_Value(final String value) {
		this.value = value;
	}

	public ISO2022String_Value(final Charstring_Value original) {
		value = original.getValue();
		copyGeneralProperties(original);
	}

	@Override
	/** {@inheritDoc} */
	public Value_type getValuetype() {
		return Value_type.ISO2022STRING_VALUE;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_GENERALSTRING;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append('\"').append(value).append('"');

		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public IValue getReferencedSubValue(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference,
			final IReferenceChain refChain) {
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
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId()
							.getDisplayName(), type.getTypename()));
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

	public String getValueISO2022String() {
		return value;
	}

	public IValue getStringElement(final int index, final Location location) {
		if (null == value) {
			return null;
		}

		if (index < 0) {
			location.reportSemanticError(MessageFormat.format(NEGATIVEINDEX, index));
			return null;
		} else if (index >= value.length()) {
			location.reportSemanticError(MessageFormat.format(INDEXOWERFLOW, index, value.length()));
		}

		return new ISO2022String_Value(value.substring(index, index + 1));
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = other.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		switch (last.getValuetype()) {
		case ISO2022STRING_VALUE:
			return value.equals(((ISO2022String_Value) last).getValueISO2022String());
		default:
			return false;
		}
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
	public boolean canGenerateSingleExpression() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return source;
		}

		source.append(name);
		source.append(".assign( ");
		source.append(generateSingleExpression(aData));
		source.append( " );\n" );

		lastTimeGenerated = aData.getBuildTimstamp();

		return source;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateSingleExpression(final JavaGenData aData) {
		final StringBuilder result = new StringBuilder();

		if (myGovernor != null) {
			switch (myGovernor.getTypetype()) {
			case TYPE_TELETEXSTRING:
				aData.addBuiltinTypeImport( "TitanTeletexString" );
				result.append(MessageFormat.format("TitanTeletexString.TTCN_ISO2022_2_TeletexString(new TitanOctetString(\"{0}\"))\n", Char2OctExpression.char2oct(value)));
				return result;
			case TYPE_VIDEOTEXSTRING:
				aData.addBuiltinTypeImport( "TitanVideotexString" );
				result.append(MessageFormat.format("TitanVideotexString.TTCN_ISO2022_2_VideotexString(new TitanOctetString(\"{0}\"))", Char2OctExpression.char2oct(value)));
				return result;
			case TYPE_GRAPHICSTRING:
			case TYPE_OBJECTDESCRIPTOR:
				aData.addBuiltinTypeImport( "TitanGraphicString" );
				result.append(MessageFormat.format("TitanGraphicString.TTCN_ISO2022_2_GraphicString(new TitanOctetString(\"{0}\"))", Char2OctExpression.char2oct(value)));
				return result;
			case TYPE_GENERALSTRING:
				aData.addBuiltinTypeImport( "TitanGeneralString" );
				result.append(MessageFormat.format("TitanGeneralString.TTCN_ISO2022_2_GeneralString(new TitanOctetString(\"{0}\"))", Char2OctExpression.char2oct(value)));
				return result;
			default:
				//FATAL error
				break;
			}
		}

		return result;
	}
}
