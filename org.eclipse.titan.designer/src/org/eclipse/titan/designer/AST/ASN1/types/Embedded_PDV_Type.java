/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
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
import org.eclipse.titan.designer.AST.Identifier.Identifier_type;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.ASN1.ASN1Type;
import org.eclipse.titan.designer.AST.ASN1.IASN1Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * @author Kristof Szabados
 * */
public final class Embedded_PDV_Type extends ASN1Type implements IReferencingType {
	private static final String EMBEDDED_PDV = "EMBEDDED PDV";
	private static final String TEMPLATENOTALLOWED = "{0} cannot be used for type `EMBEDDED PDV''";
	private static final String LENGTHRESTRICTIONNOTALLOWED = "Length restriction is not allowed for type `EMBEDDED PDV''";

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetype() {
		return Type_type.TYPE_EMBEDDED_PDV;
	}

	@Override
	/** {@inheritDoc} */
	public IASN1Type newInstance() {
		return new Embedded_PDV_Type();
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
		return EMBEDDED_PDV;
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

		final Identifier identifier = new Identifier(Identifier_type.ID_ASN, EMBEDDED_PDV);
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
		while (t != null && t instanceof IReferencingType && !t.getIsErroneous(timestamp)) {
			t = ((IReferencingType) t).getTypeRefd(timestamp, tempReferenceChain);
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
			subreference.getLocation().reportSemanticError(MessageFormat.format(ArraySubReference.INVALIDSUBREFERENCE, getTypename()));
			return null;
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
		return builder.append("embedded PDV");
	}

	@Override
	/** {@inheritDoc} */
	public boolean generatesOwnClass(JavaGenData aData, StringBuilder source) {
		return needsAlias();
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return;
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		if(needsAlias()) {
			final String ownName = getGenNameOwn();

			source.append(MessageFormat.format("\tpublic static class {0} extends {1} '{'\n", ownName, getGenNameValue(aData, source)));

			final StringBuilder descriptor = new StringBuilder();
			generateCodeTypedescriptor(aData, source, descriptor, null);
			generateCodeDefaultCoding(aData, source, descriptor);
			generateCodeForCodingHandlers(aData, source, descriptor);
			source.append(descriptor);

			source.append("\t}\n");

			source.append(MessageFormat.format("\tpublic static class {0}_template extends {1} '{' '}'\n", ownName, getGenNameTemplate(aData, source)));
		} else if (getParentType() == null || !getParentType().generatesOwnClass(aData, source)) {
			generateCodeTypedescriptor(aData, source, null, aData.attibute_registry);
			generateCodeDefaultCoding(aData, source, null);
			generateCodeForCodingHandlers(aData, source, null);
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameValue(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanEmbedded_PDV" );

		return "TitanEmbedded_PDV";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTemplate(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanEmbedded_PDV_template" );

		return "TitanEmbedded_PDV_template";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTypeDescriptor(final JavaGenData aData, final StringBuilder source) {
		if (rawAttribute != null || jsonAttribute != null ||
				hasVariantAttributes(CompilationTimeStamp.getBaseTimestamp())) {
			if (needsAlias()) {
				String baseName = getGenNameOwn(aData);
				return baseName + "." + getGenNameOwn();
			} else if (getParentType() != null) {
				final IType parentType = getParentType();
				if (parentType.generatesOwnClass(aData, source)) {
					return parentType.getGenNameOwn(aData) + "." + getGenNameOwn();
				}

				return getGenNameOwn(aData);
			}

			return getGenNameOwn(aData);
		}

		if (needsAlias()) {
			String baseName = getGenNameOwn(aData);
			return baseName + "." + getGenNameOwn();
		}

		aData.addBuiltinTypeImport( "Base_Type" );
		return "Base_Type.TitanEmbedded_PDV";
	}
}
