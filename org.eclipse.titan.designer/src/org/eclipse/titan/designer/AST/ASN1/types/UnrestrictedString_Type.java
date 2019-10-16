/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.types;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignments;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IReferencingType;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Identifier.Identifier_type;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.ASN1.ASN1Type;
import org.eclipse.titan.designer.AST.ASN1.IASN1Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * @author Kristof Szabados
 * */
public final class UnrestrictedString_Type extends ASN1Type implements IReferencingType {
	private static final String TEMPLATENOTALLOWED = "{0} cannot be used for type `CHARACTER STRING''";
	private static final String LENGTHRESTRICTIONNOTALLOWED = "Length restriction is not allowed for type `CHARACTER STRING''";

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetype() {
		return Type_type.TYPE_UNRESTRICTEDSTRING;
	}

	@Override
	/** {@inheritDoc} */
	public IASN1Type newInstance() {
		return new UnrestrictedString_Type();
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetypeTtcn3() {
		return Type_type.TYPE_TTCN3_SEQUENCE;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isCompatible(final CompilationTimeStamp timestamp, final IType otherType, final TypeCompatibilityInfo info,
			final TypeCompatibilityInfo.Chain leftChain, final TypeCompatibilityInfo.Chain rightChain) {
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String getTypename() {
		return "CHARACTER STRING";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "record.gif";
	}

	@Override
	/** {@inheritDoc} */
	public IType getTypeRefd(final CompilationTimeStamp timestamp, final IReferenceChain refChain) {
		if (null == myScope) {
			setIsErroneous(true);
			return this;
		}

		final Identifier identifier = new Identifier(Identifier_type.ID_ASN, "CHARACTER STRING");
		final Assignments assignments = myScope.getAssignmentsScope();
		if (!assignments.hasAssignmentWithId(timestamp, identifier)) {
			setIsErroneous(true);
			return this;
		}

		final Assignment assignment = assignments.getLocalAssignmentByID(timestamp, identifier);
		if (null == assignment || null == assignment.getType(timestamp)) {
			setIsErroneous(true);
			return this;
		}

		return assignment.getType(timestamp);
	}

	@Override
	/** {@inheritDoc} */
	public IType getTypeRefdLast(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		final boolean newChain = null == referenceChain;
		IReferenceChain tempReferenceChain;
		if (newChain) {
			tempReferenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		} else {
			tempReferenceChain = referenceChain;
		}

		IType t = this;
		if (!getIsErroneous(timestamp)) {
			t = getTypeRefd(timestamp, tempReferenceChain);
		}

		if (newChain) {
			tempReferenceChain.release();
		}

		if (t != null && t.getIsErroneous(timestamp)) {
			setIsErroneous(true);
		}

		return t;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (null != constraints) {
			constraints.check(timestamp);
		}

		if (myScope != null) {
			checkEncode(timestamp);
			checkVariants(timestamp);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisValue(final CompilationTimeStamp timestamp, final IValue value, final Assignment lhs, final ValueCheckingOptions valueCheckingOptions) {
		final IType last = getTypeRefd(timestamp, null);

		boolean selfReference = false;
		if (null != last && last != this) {
			selfReference = last.checkThisValue(timestamp, value, lhs, valueCheckingOptions);
		}

		value.setLastTimeChecked(timestamp);

		return selfReference;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisTemplate(final CompilationTimeStamp timestamp, final ITTCN3Template template, final boolean isModified,
			final boolean implicitOmit, final Assignment lhs) {
		registerUsage(template);
		template.setMyGovernor(this);

		template.getLocation().reportSemanticError(MessageFormat.format(TEMPLATENOTALLOWED, template.getTemplateTypeName()));

		if (null != template.getLengthRestriction()) {
			template.getLocation().reportSemanticError(LENGTHRESTRICTIONNOTALLOWED);
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public IType getFieldType(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference,
			final Expected_Value_type expectedIndex, final IReferenceChain refChain, final boolean interruptIfOptional) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (subreferences.size() <= actualSubReference) {
			return this;
		}

		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			if (subreferences.size() > actualSubReference + 1) {
				subreference.getLocation().reportSemanticError(ArraySubReference.INVALIDSTRINGELEMENTINDEX);
				return null;
			} else if (subreferences.size() == actualSubReference + 1) {
				reference.setStringElementReferencing();
			}
			return this;
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId()
							.getDisplayName(), getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((ParameterisedSubReference) subreference)
							.getId().getDisplayName(), getTypename()));
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkMapParameter(final CompilationTimeStamp timestamp, final IReferenceChain refChain, final Location errorLocation) {
		if (refChain.contains(this)) {
			return;
		}

		refChain.add(this);
		final IType referencedLast = getTypeRefdLast(timestamp);
		if (referencedLast != null) {
			referencedLast.checkMapParameter(timestamp, refChain, errorLocation);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getProposalDescription(final StringBuilder builder) {
		return builder.append("unrestricted string");
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return;
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		generateCodeTypedescriptor(aData, source);
		if(needsAlias()) {
			final String ownName = getGenNameOwn();
			source.append(MessageFormat.format("\tpublic static class {0} extends {1} '{' '}'\n", ownName, getGenNameValue(aData, source)));
			source.append(MessageFormat.format("\tpublic static class {0}_template extends {1} '{' '}'\n", ownName, getGenNameTemplate(aData, source)));
		}

		generateCodeForCodingHandlers(aData, source);
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameValue(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanCharacter_String" );

		return "TitanCharacter_String";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTemplate(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanCharacter_String_template" );

		return "TitanCharacter_String_template";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTypeDescriptor(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "Base_Type" );
		return "Base_Type.TitanCharacter_String";
	}

	@Override
	/** {@inheritDoc} */
	public String generateConversion(final JavaGenData aData, final IType fromType, final String fromName, final boolean forValue, final ExpressionStruct expression) {
		aData.addBuiltinTypeImport( "TitanCharString" );

		return MessageFormat.format("TitanCharString.convert_to_CharString({0})", fromName);
	}
}
