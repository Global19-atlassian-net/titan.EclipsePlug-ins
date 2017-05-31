/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.text.MessageFormat;

import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.values.Omit_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents a template that matches the omit value only.
 *
 * @author Kristof Szabados
 * */
public final class OmitValue_Template extends TTCN3Template {
	public static final String SPECIFICVALUEEXPECTED = "A specific value was expected instead of omit value";
	private static final String OMITNOTALLOWED = "`omit'' value is not allowed in this context";
	private static final String SIGNATUREERROR = "Generic wildcard `omit'' cannot be used for signature `{0}''";

	// cache storing the value form of this if already created, or null
	private Omit_Value asValue = null;

	@Override
	/** {@inheritDoc} */
	public Template_type getTemplatetype() {
		return Template_type.OMIT_VALUE;
	}

	@Override
	/** {@inheritDoc} */
	public String getTemplateTypeName() {
		if (isErroneous) {
			return "erroneous omit value";
		}

		return "omit value";
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("omit");
		if (lengthRestriction != null) {
			builder.append(lengthRestriction.createStringRepresentation());
		}
		if (isIfpresent) {
			builder.append("ifpresent");
		}
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void checkSpecificValue(final CompilationTimeStamp timestamp, final boolean allowOmit) {
		if (!allowOmit) {
			getLocation().reportSemanticError(SPECIFICVALUEEXPECTED);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected void checkTemplateSpecificLengthRestriction(final CompilationTimeStamp timestamp, final Type_type typeType) {
		lengthRestriction.getLocation().reportSemanticError("Length restriction cannot be used with omit value");
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		// nothing to be done
	}

	@Override
	/** {@inheritDoc} */
	public void checkThisTemplateGeneric(final CompilationTimeStamp timestamp, final IType type, final boolean isModified,
			final boolean allowOmit, final boolean allowAnyOrOmit, final boolean subCheck, final boolean implicitOmit) {
		if (!allowOmit) {
			location.reportSemanticError(OMITNOTALLOWED);
			setIsErroneous(true);
		}

		if (!getIsErroneous(timestamp)) {
			final IType last = type.getTypeRefdLast(timestamp);
			if (Type_type.TYPE_SIGNATURE.equals(last.getTypetype())) {
				location.reportSemanticError(MessageFormat.format(SIGNATUREERROR, last.getFullName()));
				setIsErroneous(true);
			}
		}

		checkLengthRestriction(timestamp, type);
		if (!allowOmit && isIfpresent) {
			location.reportSemanticError("`ifpresent' is not allowed here");
		}
		if (subCheck) {
			type.checkThisTemplateSubtype(timestamp, this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isValue(final CompilationTimeStamp timestamp) {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public IValue getValue() {
		if (asValue != null) {
			return asValue;
		}

		asValue = new Omit_Value();
		asValue.setLocation(getLocation());
		asValue.setMyScope(getMyScope());
		asValue.setFullNameParent(getNameParent());
		return asValue;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkValueomitRestriction(final CompilationTimeStamp timestamp, final String definitionName, final boolean omitAllowed, final Location usageLocation) {
		if (omitAllowed) {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_OMIT, usageLocation);
		} else {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_VALUE, usageLocation);
			usageLocation.reportSemanticError(MessageFormat.format(RESTRICTIONERROR, definitionName, getTemplateTypeName()));
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkPresentRestriction(final CompilationTimeStamp timestamp, final String definitionName, final Location usageLocation) {
		checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_PRESENT, usageLocation);
		usageLocation.reportSemanticError(MessageFormat.format(RESTRICTIONERROR, definitionName, getTemplateTypeName()));
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression() {
		if (lengthRestriction != null || isIfpresent /* TODO:  || get_needs_conversion()*/) {
			return false;
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getSingleExpression(JavaGenData aData, boolean castIsNeeded) {
		StringBuilder result = new StringBuilder();

		if (castIsNeeded && (lengthRestriction != null || isIfpresent)) {
			result.append( "\t//TODO: fatal error while generating " );
			result.append( getClass().getSimpleName() );
			result.append( ".getSingleExpression() !\n" );
			//fatal error
			return result;
		}

		aData.addBuiltinTypeImport( "Base_Template.template_sel" );
		result.append( "template_sel.OMIT_VALUE" );

		//TODO handle cast needed

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInit(JavaGenData aData, StringBuilder source, String name) {
		aData.addBuiltinTypeImport( "Base_Template.template_sel" );
		source.append(name);
		source.append(".assign( ");
		source.append(getSingleExpression(aData, false));
		source.append( " );\n" );
		//TODO missing parts need to be completed

		if (isIfpresent) {
			source.append(name);
			source.append(".set_ifPresent();\n");
		}
	}
}
