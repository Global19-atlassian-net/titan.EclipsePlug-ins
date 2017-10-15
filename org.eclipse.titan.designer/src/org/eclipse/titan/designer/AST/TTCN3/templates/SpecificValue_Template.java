/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IType.ValueCheckingOptions;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.types.Array_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Function_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.Bitstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Charstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value.Operation_type;
import org.eclipse.titan.designer.AST.TTCN3.values.Function_Reference_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Hexstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Octetstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Referenced_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SequenceOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SetOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Undefined_LowerIdentifier_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.UniversalCharstring_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ApplyExpression;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a template with a specific value.
 *
 * @author Kristof Szabados
 * */
public final class SpecificValue_Template extends TTCN3Template {

	private final IValue specificValue;

	private TTCN3Template realTemplate;

	public SpecificValue_Template(final IValue specificValue) {
		this.specificValue = specificValue;

		if (specificValue != null) {
			specificValue.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Template_type getTemplatetype() {
		return Template_type.SPECIFIC_VALUE;
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return new Location(specificValue.getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		//Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public String getTemplateTypeName() {
		if (isErroneous) {
			return "erroneous specific value";
		}

		return "specific value";
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		if (specificValue == null) {
			return "<erroneous template>";
		}

		final StringBuilder builder = new StringBuilder();
		builder.append(specificValue.createStringRepresentation());

		if (lengthRestriction != null) {
			builder.append(lengthRestriction.createStringRepresentation());
		}
		if (isIfpresent) {
			builder.append("ifpresent");
		}

		return builder.toString();
	}

	public IValue getSpecificValue() {
		return specificValue;
	}

	public Identifier getIdentifier() {
		if (Value_type.UNDEFINED_LOWERIDENTIFIER_VALUE.equals(specificValue.getValuetype())) {
			return ((Undefined_LowerIdentifier_Value) specificValue).getIdentifier();
		}

		return null;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (specificValue != null) {
			specificValue.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);
		if (specificValue != null) {
			specificValue.setCodeSection(codeSection);
		}
		if (lengthRestriction != null) {
			lengthRestriction.setCodeSection(codeSection);
		}
		if (realTemplate != null && realTemplate != this) {
			realTemplate.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public IType getExpressionGovernor(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			if (myGovernor != null) {
				return myGovernor;
			}
		}

		if (realTemplate != null && realTemplate != this) {
			return realTemplate.getExpressionGovernor(timestamp, expectedValue);
		}

		if (specificValue != null) {
			specificValue.setMyGovernor(null);
			final IValue temp = specificValue.setLoweridToReference(timestamp);
			return temp.getExpressionGovernor(timestamp, expectedValue);
		}

		return null;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (getIsErroneous(timestamp) || specificValue == null) {
			return Type_type.TYPE_UNDEFINED;
		}

		if (realTemplate != null && realTemplate != this) {
			return realTemplate.getExpressionReturntype(timestamp, expectedValue);
		}

		specificValue.setLoweridToReference(timestamp);
		return specificValue.getExpressionReturntype(timestamp, expectedValue);
	}

	@Override
	/** {@inheritDoc} */
	public ITTCN3Template setLoweridToReference(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return realTemplate;
		}

		lastTimeChecked = timestamp;
		realTemplate = this;
		isErroneous = false;

		final IValue temp = specificValue.setLoweridToReference(timestamp);

		if (Value_type.REFERENCED_VALUE.equals(temp.getValuetype())) {
			final Assignment assignment = ((Referenced_Value) temp).getReference().getRefdAssignment(timestamp, false);

			if (assignment != null) {
				switch (assignment.getAssignmentType()) {
				case A_TEMPLATE:
				case A_VAR_TEMPLATE:
				case A_PAR_TEMP_IN:
				case A_PAR_TEMP_OUT:
				case A_PAR_TEMP_INOUT:
				case A_FUNCTION_RTEMP:
				case A_EXT_FUNCTION_RTEMP:
					realTemplate = setTemplatetype(timestamp, Template_type.TEMPLATE_REFD);
					break;
				default:
					break;
				}
			} else {
				isErroneous = true;
			}
		}

		return realTemplate;
	}

	@Override
	/** {@inheritDoc} */
	public TTCN3Template getTemplateReferencedLast(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (realTemplate != null && realTemplate != this) {
			return realTemplate.getTemplateReferencedLast(timestamp, referenceChain);
		}

		return this;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReferenceTemplate(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (realTemplate != null && realTemplate != this) {
			return realTemplate.checkExpressionSelfReferenceTemplate(timestamp, lhs);
		}

		final IType governor = specificValue.getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
		if (governor == null) {
			return false;
		}

		return governor.checkThisValue(timestamp, specificValue, lhs, new ValueCheckingOptions(Expected_Value_type.EXPECTED_DYNAMIC_VALUE, true, true, false, false, false));
	}

	@Override
	/** {@inheritDoc} */
	public void checkSpecificValue(final CompilationTimeStamp timestamp, final boolean allowOmit) {
		if (specificValue == null) {
			return;
		}

		switch (specificValue.getValuetype()) {
		case EXPRESSION_VALUE:
			// checked later
			break;
		case OMIT_VALUE:
			if (!allowOmit) {
				getLocation().reportSemanticError(OmitValue_Template.SPECIFICVALUEEXPECTED);
			}
			return;
		default:
			return;
		}

		final Expression_Value expressionValue = (Expression_Value) specificValue;
		if (!Operation_type.APPLY_OPERATION.equals(expressionValue.getOperationType())) {
			return;
		}

		expressionValue.setLoweridToReference(timestamp);
		IType type = ((ApplyExpression) expressionValue).getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
		if (type == null) {
			return;
		}

		type = type.getTypeRefdLast(timestamp);

		if (Type_type.TYPE_FUNCTION.equals(type.getTypetype()) && ((Function_Type) type).returnsTemplate()) {
			final ITTCN3Template template = setTemplatetype(timestamp, Template_type.TEMPLATE_INVOKE);
			template.checkSpecificValue(timestamp, allowOmit);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected void checkTemplateSpecificLengthRestriction(final CompilationTimeStamp timestamp, final Type_type typeType) {
		IValue value = getValue();
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		value = value.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		switch (value.getValuetype()) {
		case BITSTRING_VALUE:
			if (Type_type.TYPE_BITSTRING.equals(typeType)) {
				lengthRestriction.checkNofElements(timestamp, ((Bitstring_Value) value).getValueLength(), false, false, false, this);
			}
			break;
		case HEXSTRING_VALUE:
			if (Type_type.TYPE_HEXSTRING.equals(typeType)) {
				lengthRestriction.checkNofElements(timestamp, ((Hexstring_Value) value).getValueLength(), false, false, false, this);
			}
			break;
		case OCTETSTRING_VALUE:
			if (Type_type.TYPE_OCTETSTRING.equals(typeType)) {
				lengthRestriction
				.checkNofElements(timestamp, ((Octetstring_Value) value).getValueLength(), false, false, false, this);
			}
			break;
		case CHARSTRING_VALUE:
			if (Type_type.TYPE_CHARSTRING.equals(typeType)) {
				lengthRestriction.checkNofElements(timestamp, ((Charstring_Value) value).getValueLength(), false, false, false, this);
			} else if (Type_type.TYPE_UCHARSTRING.equals(typeType)) {
				value = value.setValuetype(timestamp, Value_type.UNIVERSALCHARSTRING_VALUE);
				lengthRestriction.checkNofElements(timestamp, ((UniversalCharstring_Value) value).getValueLength(), false, false,
						false, this);
			}
			break;
		case UNIVERSALCHARSTRING_VALUE:
			if (Type_type.TYPE_UCHARSTRING.equals(typeType)) {
				lengthRestriction.checkNofElements(timestamp, ((UniversalCharstring_Value) value).getValueLength(), false, false,
						false, this);
			}
			break;
		case SEQUENCEOF_VALUE:
			if (Type_type.TYPE_SEQUENCE_OF.equals(typeType)) {
				lengthRestriction.checkNofElements(timestamp, ((SequenceOf_Value) value).getNofComponents(), false, false, false,
						this);
			}
			break;
		case SETOF_VALUE:
			if (Type_type.TYPE_SET_OF.equals(typeType)) {
				lengthRestriction.checkNofElements(timestamp, ((SetOf_Value) value).getNofComponents(), false, false, false, this);
			}
			break;
		case OMIT_VALUE:
			lengthRestriction.getLocation().reportSemanticError("Length restriction cannot be used with omit value");
			break;
		default:
			// we cannot verify anything on other value types,
			// they are either correct or not applicable to the type
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this) && realTemplate != null && realTemplate != this && !realTemplate.getIsErroneous(timestamp)) {
			realTemplate.checkRecursions(timestamp, referenceChain);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisTemplateGeneric(final CompilationTimeStamp timestamp, final IType type, final boolean isModified,
			final boolean allowOmit, final boolean allowAnyOrOmit, final boolean subCheck, final boolean implicitOmit, final Assignment lhs) {
		if (getIsErroneous(timestamp)) {
			return false;
		}

		if (type == null) {
			return false;
		}

		boolean selfReference = false;
		type.check(timestamp);

		if (specificValue != null) {
			specificValue.setMyGovernor(type);
			final IValue temporalValue = type.checkThisValueRef(timestamp, specificValue);
			selfReference = type.checkThisValue(timestamp, temporalValue, lhs, new ValueCheckingOptions(Expected_Value_type.EXPECTED_TEMPLATE, isModified,
					allowOmit, true, implicitOmit, false));
		}

		checkLengthRestriction(timestamp, type);
		if (!allowOmit && isIfpresent) {
			if( location != null && !(location instanceof NULL_Location)) {
				location.reportSemanticError("`ifpresent' is not allowed here");
			} else if(specificValue != null && !(specificValue.getLocation() instanceof NULL_Location)) {
				specificValue.getLocation().reportSemanticError("`ifpresent' is not allowed here");
			}
		}
		if (subCheck) {
			type.checkThisTemplateSubtype(timestamp, this);
		}

		return selfReference;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isValue(final CompilationTimeStamp timestamp) {
		if (lengthRestriction != null || isIfpresent || getIsErroneous(timestamp)) {
			return false;
		}

		if (realTemplate != null && realTemplate != this) {
			return false;
		}

		if (Value_type.FUNCTION_REFERENCE_VALUE.equals(specificValue.getValuetype())) {
			final IType governor = ((Function_Reference_Value) specificValue).getExpressionGovernor(timestamp,
					Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
			if (governor == null) {
				return true;
			}

			final IType last = governor.getTypeRefdLast(timestamp);
			if (Type_type.TYPE_FUNCTION.equals(last.getTypetype()) && ((Function_Type) last).returnsTemplate()) {
				return false;
			}
		} else if (Value_type.REFERENCED_VALUE.equals(specificValue.getValuetype()) ) {
			final Reference reference = ((Referenced_Value) specificValue).getReference();
			final Assignment assignment = reference.getRefdAssignment(timestamp, true);
			if (assignment == null) {
				return true;
			}

			switch (assignment.getAssignmentType()) {
			case A_CONST:
			case A_TIMER:
			case A_EXT_CONST:
			case A_PAR_VAL:
			case A_PAR_VAL_IN:
			case A_PAR_VAL_OUT:
			case A_PAR_VAL_INOUT:
			case A_VAR:
			case A_FUNCTION_RVAL:
			case A_EXT_FUNCTION_RVAL:
			case A_MODULEPAR:
			case A_MODULEPAR_TEMPLATE:
				return true; //runtime evaluation!
			case A_TEMPLATE:
			case A_PAR_TEMP_IN:
			case A_PAR_TEMP_INOUT:
			case A_FUNCTION_RTEMP:
			case A_VAR_TEMPLATE:
				boolean result = true;
				final Restriction_type rt = ((Definition) assignment).getTemplateRestriction();
				if ( TemplateRestriction.Restriction_type.TR_OMIT.equals(rt) ||
						TemplateRestriction.Restriction_type.TR_PRESENT.equals(rt)) {
					result = false;
				}

				final TTCN3Template ttemplate = getTemplateReferencedLast(timestamp);
				if ((!ttemplate.equals(this)) && (ttemplate.isValue(timestamp))) {
					return result; //ok
				} else {
					return false;
				}
			default:
				return false;
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public IValue getValue() {
		if (realTemplate != null && realTemplate != this) {
			return realTemplate.getValue();
		}

		return specificValue;
	}

	/**
	 * @return true if the template is a reference.
	 * */
	public boolean isReference() {
		if (lengthRestriction != null || isIfpresent || specificValue == null) {
			return false;
		}

		switch (specificValue.getValuetype()) {
		case UNDEFINED_LOWERIDENTIFIER_VALUE:
			return true;
		case REFERENCED_VALUE:
			return ((Referenced_Value) specificValue).getReference() != null;
		default:
			return false;
		}
	}

	/**
	 * Returns the reference that should be used where this template is used
	 * as a referencing template.
	 *
	 * @return the reference.
	 * */
	public Reference getReference() {
		if (lengthRestriction != null || isIfpresent || specificValue == null) {
			return null;
		}

		switch (specificValue.getValuetype()) {
		case UNDEFINED_LOWERIDENTIFIER_VALUE:
			return ((Undefined_LowerIdentifier_Value) specificValue).getAsReference();
		case REFERENCED_VALUE:
			return ((Referenced_Value) specificValue).getReference();
		default:
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public TTCN3Template setTemplatetype(final CompilationTimeStamp timestamp, final Template_type newType) {
		lastTimeChecked = timestamp;

		switch (newType) {
		case TEMPLATE_REFD:
			realTemplate = new Referenced_Template(timestamp, this);
			break;
		case TEMPLATE_INVOKE:
			realTemplate = new Invoke_Template(timestamp, this);
			break;
		default:
			realTemplate = super.setTemplatetype(timestamp, newType);
		}

		return realTemplate;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkValueomitRestriction(final CompilationTimeStamp timestamp, final String definitionName, final boolean omitAllowed, final Location usageLocation) {
		if (omitAllowed) {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_OMIT, usageLocation);
		} else {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_VALUE, usageLocation);
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (lengthRestriction != null) {
			lengthRestriction.updateSyntax(reparser, false);
			reparser.updateLocation(lengthRestriction.getLocation());
		}

		if (baseTemplate instanceof IIncrementallyUpdateable) {
			((IIncrementallyUpdateable) baseTemplate).updateSyntax(reparser, false);
			reparser.updateLocation(baseTemplate.getLocation());
		} else if (baseTemplate != null) {
			throw new ReParseException();
		}

		if (specificValue instanceof IIncrementallyUpdateable) {
			((IIncrementallyUpdateable) specificValue).updateSyntax(reparser, false);
			reparser.updateLocation(specificValue.getLocation());
		} else if (specificValue != null) {
			throw new ReParseException();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (specificValue == null) {
			return;
		}

		specificValue.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (specificValue != null && !specificValue.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression() {
		if (lengthRestriction != null || isIfpresent /* TODO:  || get_needs_conversion()*/) {
			return false;
		}

		if (specificValue == null) {
			return false;
		}

		if (realTemplate != null && realTemplate != this) {
			return realTemplate.hasSingleExpression();
		}

		return specificValue.canGenerateSingleExpression();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getSingleExpression(final JavaGenData aData, final boolean castIsNeeded) {
		final StringBuilder result = new StringBuilder();

		if (castIsNeeded && (lengthRestriction != null || isIfpresent)) {
			result.append( "\t//TODO: fatal error while generating " );
			result.append( getClass().getSimpleName() );
			result.append( ".getSingleExpression() !\n" );
			// TODO: fatal error
			return result;
		}

		if (realTemplate != null && realTemplate != this) {
			result.append(realTemplate.getSingleExpression(aData, castIsNeeded));
		} else {
			result.append(specificValue.generateSingleExpression(aData));
		}

		//TODO handle cast needed

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression, final TemplateRestriction.Restriction_type templateRestriction) {
		IType governor = myGovernor;
		if (governor == null) {
			governor = getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
		}
		if (governor == null) {
			return;
		}

		final String genName = governor.getGenNameTemplate(aData, expression.expression, myScope);

		if (realTemplate != null && realTemplate != this) {
			realTemplate.generateCodeExpression(aData, expression, templateRestriction);
			return;
		}

		if (lengthRestriction == null && !isIfpresent && templateRestriction == Restriction_type.TR_NONE) {
			//The single expression must be tried first because this rule might cover some referenced templates.
			if (hasSingleExpression()) {
				expression.expression.append(MessageFormat.format("new {0}(", genName) );
				if(governor.getTypetype() == Type_type.TYPE_ARRAY){
					final Array_Type array_type = (Array_Type) governor;
					expression.expression.append(MessageFormat.format(" {0}.class, ",array_type.getElementType().getGenNameTemplate(aData, expression.expression, myScope)));
				}
				expression.expression.append(getSingleExpression(aData, true));
				expression.expression.append(')');
				return;
			}

			expression.expression.append(MessageFormat.format("new {0}(", genName) );
			specificValue.generateCodeExpression( aData, expression );
			expression.expression.append(')');
			return;
		}

		final String tempId = aData.getTemporaryVariableName();
		expression.preamble.append(MessageFormat.format("{0} {1} = new {0}();\n", governor.getGenNameTemplate(aData, expression.expression, myScope), tempId));

		generateCodeInit(aData, expression.preamble, tempId);

		if (templateRestriction != Restriction_type.TR_NONE) {
			TemplateRestriction.generateRestrictionCheckCode(aData, expression.expression, location, tempId, templateRestriction);
		}

		expression.expression.append(tempId);
	}

	@Override
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (realTemplate != null && realTemplate != this) {
			realTemplate.reArrangeInitCode(aData, source, usageModule);
			return;
		}

		if (specificValue != null) {
			specificValue.reArrangeInitCode(aData, source, usageModule);
		}

		if (lengthRestriction != null) {
			lengthRestriction.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		if (lastTimeBuilt != null && !lastTimeBuilt.isLess(aData.getBuildTimstamp())) {
			return;
		}
		lastTimeBuilt = aData.getBuildTimstamp();

		//TODO handle post init rearrangement
		if (realTemplate != null && realTemplate != this) {
			realTemplate.generateCodeInit(aData, source, name);
		} else {
			if(getCodeSection() == CodeSectionType.CS_POST_INIT) {
				specificValue.reArrangeInitCode(aData, source, myScope.getModuleScope());
			}
			specificValue.generateCodeInit( aData, source, name );
		}

		if (lengthRestriction != null) {
			if(getCodeSection() == CodeSectionType.CS_POST_INIT) {
				lengthRestriction.reArrangeInitCode(aData, source, myScope.getModuleScope());
			}
			lengthRestriction.generateCodeInit(aData, source, name);
		}

		if (isIfpresent) {
			source.append(name);
			source.append(".set_ifPresent();\n");
		}
	}
}
