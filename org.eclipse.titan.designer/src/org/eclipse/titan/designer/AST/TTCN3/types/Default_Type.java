/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * default type (TTCN-3).
 *
 * @author Kristof Szabados
 * */
public final class Default_Type extends Type {
	private static final String DEFAULTVALUEEXPECTED = "Default value was expected";
	private static final String TEMPLATENOTALLOWED = "{0} cannot be used for type `default''";
	private static final String LENGTHRESTRICTIONNOTALLOWED = "Length restriction is not allowed for type `default''";

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetype() {
		return Type_type.TYPE_DEFAULT;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isCompatible(final CompilationTimeStamp timestamp, final IType otherType, final TypeCompatibilityInfo info,
			final TypeCompatibilityInfo.Chain leftChain, final TypeCompatibilityInfo.Chain rightChain) {
		check(timestamp);
		otherType.check(timestamp);
		final IType temp = otherType.getTypeRefdLast(timestamp);

		if (getIsErroneous(timestamp) || temp.getIsErroneous(timestamp)) {
			return true;
		}

		return Type_type.TYPE_DEFAULT.equals(temp.getTypetype());
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetypeTtcn3() {
		if (isErroneous) {
			return Type_type.TYPE_UNDEFINED;
		}

		return getTypetype();
	}

	@Override
	/** {@inheritDoc} */
	public String getTypename() {
		return "default";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "default.gif";
	}

	@Override
	/** {@inheritDoc} */
	public boolean isComponentInternal(final CompilationTimeStamp timestamp) {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void checkComponentInternal(final CompilationTimeStamp timestamp, final Set<IType> typeSet, final String operation) {
		location.reportSemanticError(MessageFormat.format("Default type cannot be {0}", operation));
	}

	@Override
	/** {@inheritDoc} */
	public void checkThisValue(final CompilationTimeStamp timestamp, final IValue value, final ValueCheckingOptions valueCheckingOptions) {
		super.checkThisValue(timestamp, value, valueCheckingOptions);

		final IValue last = value.getValueRefdLast(timestamp, valueCheckingOptions.expected_value, null);
		if (last == null || last.getIsErroneous(timestamp)) {
			return;
		}

		// already handled ones
		switch (value.getValuetype()) {
		case OMIT_VALUE:
		case REFERENCED_VALUE:
			return;
		case UNDEFINED_LOWERIDENTIFIER_VALUE:
			if (Value_type.REFERENCED_VALUE.equals(last.getValuetype())) {
				return;
			}
			break;
		default:
			break;
		}

		switch (last.getValuetype()) {
		case TTCN3_NULL_VALUE:
			value.setValuetype(timestamp, Value_type.DEFAULT_NULL_VALUE);
			break;
		case EXPRESSION_VALUE:
		case MACRO_VALUE:
			// already checked
			break;
		default:
			value.getLocation().reportSemanticError(DEFAULTVALUEEXPECTED);
			value.setIsErroneous(true);
		}

		value.setLastTimeChecked(timestamp);
	}

	@Override
	public void checkEmbedded(final CompilationTimeStamp timestamp, final Location errorLocation,
			final boolean defaultAllowed, final String errorMessage) {
		if (!defaultAllowed) {
			errorLocation.reportSemanticError(MessageFormat.format("Default type `{0}'' cannot be {1}", getTypename(), errorMessage));
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkThisTemplate(final CompilationTimeStamp timestamp, final ITTCN3Template template,
			final boolean isModified, final boolean implicitOmit) {
		registerUsage(template);
		template.setMyGovernor(this);

		template.getLocation().reportSemanticError(MessageFormat.format(TEMPLATENOTALLOWED, template.getTemplateTypeName()));

		if (template.getLengthRestriction() != null) {
			template.getLocation().reportSemanticError(LENGTHRESTRICTIONNOTALLOWED);
		}
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
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((ParameterisedSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getProposalDescription(final StringBuilder builder) {
		return builder.append("default");
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if(needsAlias()) {
			final String ownName = getGenNameOwn();
			source.append(MessageFormat.format("\tpublic static class {0} extends {1} '{' '}'\n", ownName, getGenNameValue(aData, source, myScope)));
			source.append(MessageFormat.format("\tpublic static class {0}_template extends {1} '{' '}'\n", ownName, getGenNameTemplate(aData, source, myScope)));
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameValue(JavaGenData aData, final StringBuilder source , final Scope scope) {
		aData.addBuiltinTypeImport( "TitanDefault" );
		return "TitanDefault";
	}

	@Override
	public String getGenNameTemplate(JavaGenData aData, StringBuilder source, Scope scope) {
		aData.addBuiltinTypeImport( "TitanDefault_template" );
		return "TitanDefault_template";
	}
}
