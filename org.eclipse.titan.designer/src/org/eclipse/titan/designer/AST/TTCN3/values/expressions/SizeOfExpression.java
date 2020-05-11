/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.values.RelativeObjectIdentifier_Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Const;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Port;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Template;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Timer;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template.Template_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.LengthRestriction;
import org.eclipse.titan.designer.AST.TTCN3.templates.Named_Template_List;
import org.eclipse.titan.designer.AST.TTCN3.templates.RangeLenghtRestriction;
import org.eclipse.titan.designer.AST.TTCN3.templates.Referenced_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.SingleLenghtRestriction;
import org.eclipse.titan.designer.AST.TTCN3.templates.SpecificValue_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.SubsetMatch_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.SupersetMatch_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.templates.Template_List;
import org.eclipse.titan.designer.AST.TTCN3.types.Array_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.ArrayDimensions;
import org.eclipse.titan.designer.AST.TTCN3.values.Array_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.ObjectIdentifier_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Real_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Referenced_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SequenceOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Sequence_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SetOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Set_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class SizeOfExpression extends Expression_Value {
	private final TemplateInstance templateInstance;

	// private Reference reference;

	public SizeOfExpression(final TemplateInstance templateInstance) {
		this.templateInstance = templateInstance;

		if (templateInstance != null) {
			templateInstance.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.SIZEOF_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (templateInstance != null && templateInstance.getTemplateBody().checkExpressionSelfReferenceTemplate(timestamp, lhs)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("sizeof(");
		builder.append(templateInstance.createStringRepresentation());
		builder.append(')');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);

		if (templateInstance != null) {
			templateInstance.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);

		if (templateInstance != null) {
			templateInstance.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (templateInstance == child) {
			return builder.append(OPERAND);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_INTEGER;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		return !(lastValue instanceof Integer_Value);
	}

	/**
	 * Helper function for checking the dimensions of time and port arrays.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param ref
	 *                reference to the assignment
	 * @param dimensions
	 *                the dimensions of the port or time array.
	 * @param assignment
	 *                the assignment itself, used to get its name and
	 *                description.
	 * */
	private long checkTimerPort(final CompilationTimeStamp timestamp, final Reference ref, final ArrayDimensions dimensions,
			final Assignment assignment) {
		if (dimensions == null) {
			templateInstance.getLocation().reportSemanticError(
					MessageFormat.format("operation is not applicable to single {0}", assignment.getDescription()));
			setIsErroneous(true);
			return -1;
		}

		dimensions.checkIndices(timestamp, ref, assignment.getAssignmentName(), true, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
		final List<ISubReference> subreferences = ref.getSubreferences();
		int referencedDimensions;
		if (subreferences.size() > 1) {
			referencedDimensions = subreferences.size() - 1;
			final int nofDimensions = dimensions.size();
			if (referencedDimensions < nofDimensions) {
				setIsErroneous(true);
				return -1;
			} else if (referencedDimensions == nofDimensions) {
				templateInstance.getLocation().reportSemanticError(
						MessageFormat.format("Operation is not applicable to a {0}", assignment.getAssignmentName()));
				setIsErroneous(true);
				return -1;
			}
		} else {
			referencedDimensions = 0;
		}

		return dimensions.get(referencedDimensions).getSize();
	}

	/**
	 * Checks the parameters of the expression and if they are valid in
	 * their position in the expression or not.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param expectedValue
	 *                the kind of value expected.
	 * @param referenceChain
	 *                a reference chain to detect cyclic references.
	 *
	 * @return the size of the expression, or -1 in case of error
	 * */
	private long checkExpressionOperands(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		Expected_Value_type internalExpectedValue;

		if (Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue)) {
			internalExpectedValue = Expected_Value_type.EXPECTED_TEMPLATE;
		} else {
			internalExpectedValue = expectedValue;
		}

		ITTCN3Template template = templateInstance.getTemplateBody();
		template.setMyGovernor(null);
		template.setLoweridToReference(timestamp);
		template = template.getTemplateReferencedLast(timestamp, referenceChain);
		if (template.getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return -1;
		}

		// Timer and port arrays are handled separately
		if (template.getTemplatetype() == Template_type.SPECIFIC_VALUE) {
			final SpecificValue_Template specValTempl = (SpecificValue_Template) template;
			IValue val = specValTempl.getSpecificValue();
			val.setMyGovernor(specValTempl.getMyGovernor());
			if (val.getValuetype() == Value_type.UNDEFINED_LOWERIDENTIFIER_VALUE) {
				val = val.setLoweridToReference(timestamp);
			}

			if (val != null && val.getValuetype() == Value_type.REFERENCED_VALUE) {
				final Referenced_Value referencedValue = (Referenced_Value) val;
				final Reference ref = referencedValue.getReference();
				final Assignment temporalAss = ref.getRefdAssignment(timestamp, true);
				if (temporalAss != null) {
					final Assignment_type asstype = temporalAss.getAssignmentType();
					ArrayDimensions dimensions;
					if (asstype == Assignment_type.A_PORT) {
						dimensions = ((Def_Port) temporalAss).getDimensions();
						return checkTimerPort(timestamp, ref, dimensions, temporalAss);
					} else if (asstype == Assignment_type.A_TIMER) {
						dimensions = ((Def_Timer) temporalAss).getDimensions();
						return checkTimerPort(timestamp, ref, dimensions, temporalAss);
					}
				}
			}
		}

		IType governor = templateInstance.getExpressionGovernor(timestamp, internalExpectedValue);
		if (governor == null) {
			final ITTCN3Template templ = template.setLoweridToReference(timestamp);
			governor = templ.getExpressionGovernor(timestamp, internalExpectedValue);
		}
		if (governor == null) {
			if (!template.getIsErroneous(timestamp)) {
				templateInstance.getLocation().reportSemanticError("Cannot determine the type of the argument in the `sizeof' operation. If type is known, use valueof(<type>: ...) as argument.");
			}
			setIsErroneous(true);
			return -1;
		}

		IsValueExpression.checkExpressionTemplateInstance(timestamp, this, templateInstance, governor, referenceChain, internalExpectedValue);
		if (isErroneous) {
			return -1;
		}

		IType type = governor.getTypeRefdLast(timestamp);
		switch (type.getTypetype()) {
		case TYPE_SEQUENCE_OF:
		case TYPE_SET_OF:
		case TYPE_TTCN3_SEQUENCE:
		case TYPE_TTCN3_SET:
		case TYPE_ASN1_SEQUENCE:
		case TYPE_ASN1_SET:
		case TYPE_ARRAY:
		case TYPE_OBJECTID:
		case TYPE_ROID:
		case TYPE_UNDEFINED:
			break;
		default:
			templateInstance.getLocation().reportSemanticError(
					"Reference to a value or template of type record, record of, set, set of, objid or array was expected");
			setIsErroneous(true);
			return -1;
		}

		IValue value = null;
		Reference reference = null;
		Assignment assignment = null;
		List<ISubReference> subreferences = null;
		switch (template.getTemplatetype()) {
		case INDEXED_TEMPLATE_LIST:
			return -1;
		case TEMPLATE_REFD:
			reference = ((Referenced_Template)template).getReference();
			assignment = reference.getRefdAssignment(timestamp, false);
			subreferences = reference.getSubreferences();
			break;
		case TEMPLATE_LIST:
		case NAMED_TEMPLATE_LIST:
		case SUBSET_MATCH:
		case SUPERSET_MATCH:
			// compute later
			break;
		case SPECIFIC_VALUE:
			value = ((SpecificValue_Template) template).getSpecificValue().getValueRefdLast(timestamp, referenceChain);
			if (value != null) {
				switch(value.getValuetype()) {
				case SEQUENCEOF_VALUE:
				case SETOF_VALUE:
				case ARRAY_VALUE:
				case RELATIVEOBJECTIDENTIFIER_VALUE:
				case OBJECTID_VALUE:
				case SEQUENCE_VALUE:
				case SET_VALUE:
					break;
				case REFERENCED_VALUE: {
					reference = ((Referenced_Value)value).getReference();
					assignment = reference.getRefdAssignment(timestamp, false);
					subreferences = reference.getSubreferences();
					break;
				}
				default:
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("`sizeof'' operation is not applicable to `{0}''", value.createStringRepresentation()));
					setIsErroneous(true);
					return -1;
				}
			}
			break;
		default:
			templateInstance.getLocation().reportSemanticError(MessageFormat.format("`sizeof'' operation is not applicable to {0}", template.getTemplateTypeName()));
			setIsErroneous(true);
			return -1;
		}

		if (assignment != null) {
			if (assignment.getIsErroneous()) {
				setIsErroneous(true);
				return -1;
			}
			switch(assignment.getAssignmentType()) {
			case A_CONST:
				value = ((Def_Const)assignment).getValue();
				break;
			case A_EXT_CONST:
			case A_MODULEPAR:
			case A_MODULEPAR_TEMPLATE:
				if (Expected_Value_type.EXPECTED_CONSTANT.equals(internalExpectedValue)) {
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to an (evaluable) constant value was expected instead of {0}", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				}
				break;
			case A_VAR:
			case A_PAR_VAL:
			case A_PAR_VAL_IN:
			case A_PAR_VAL_OUT:
			case A_PAR_VAL_INOUT:
				switch (internalExpectedValue) {
				case EXPECTED_CONSTANT:
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a constant value was expected instead of {0}", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				case EXPECTED_STATIC_VALUE:
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a static value was expected instead of {0}", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				default:
					break;
				}
				break;
			case A_TEMPLATE:
				template = ((Def_Template)assignment).getTemplate(timestamp);
				if (!Expected_Value_type.EXPECTED_TEMPLATE.equals(internalExpectedValue)) {
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a value was expected instead of {0}", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				}
				break;
			case A_VAR_TEMPLATE:
			case A_PAR_TEMP_IN:
			case A_PAR_TEMP_OUT:
			case A_PAR_TEMP_INOUT:
				if (!Expected_Value_type.EXPECTED_TEMPLATE.equals(internalExpectedValue)) {
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a value was expected instead of {0}", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				}
				break;
			case A_FUNCTION_RVAL:
			case A_EXT_FUNCTION_RVAL:
				switch (internalExpectedValue) {
				case EXPECTED_CONSTANT:
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a constant value was expected instead of the return value of {0}", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				case EXPECTED_STATIC_VALUE:
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a static value was expected instead of the return value of {0}", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				default:
					break;
				}
				break;
			case A_FUNCTION_RTEMP:
			case A_EXT_FUNCTION_RTEMP:
				if (!Expected_Value_type.EXPECTED_TEMPLATE.equals(internalExpectedValue)) {
					templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a value was expected instead of a call of {0}, which returns a template", assignment.getDescription()));
					setIsErroneous(true);
					return -1;
				}
				break;
			case A_TIMER:
			case A_PORT:
				// were already checked separately.
				break;
			default:
				templateInstance.getLocation().reportSemanticError(MessageFormat.format("Reference to a {0} was expected instead of {1}", Expected_Value_type.EXPECTED_TEMPLATE.equals(internalExpectedValue) ? "value or template" : "value", assignment.getDescription()));
				setIsErroneous(true);
				return -1;
			}

			type = assignment.getType(timestamp).getFieldType(timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
			if (type == null || type.getIsErroneous(timestamp)) {
				setIsErroneous(true);
				return -1;
			}
			type = type.getTypeRefdLast(timestamp);

			switch (type.getTypetype()) {
			case TYPE_SEQUENCE_OF:
			case TYPE_SET_OF:
			case TYPE_TTCN3_SEQUENCE:
			case TYPE_TTCN3_SET:
			case TYPE_ASN1_SEQUENCE:
			case TYPE_ASN1_SET:
			case TYPE_ARRAY:
			case TYPE_OBJECTID:
			case TYPE_ROID:
			case TYPE_UNDEFINED:
				break;
			default:
				templateInstance.getLocation().reportSemanticError(
						"Reference to a value or template of type record, record of, set, set of, objid or array was expected");
				setIsErroneous(true);
				return -1;
			}
		}

		// check for index overflows in subrefs if possible
		if (value != null) {
			switch (value.getValuetype()) {
			case SEQUENCEOF_VALUE:
				if (((SequenceOf_Value)value).isIndexed()) {
					return -1;
				}
				break;
			case SETOF_VALUE:
				if (((SetOf_Value)value).isIndexed()) {
					return -1;
				}
				break;
			case ARRAY_VALUE:
				if (((Array_Value)value).isIndexed()) {
					return -1;
				}
				break;
			default:
				break;
			}
			/* The reference points to a constant.  */
			if (subreferences != null && !reference.hasUnfoldableIndexSubReference(timestamp)) {
				value = value.getReferencedSubValue(timestamp, reference, 1, referenceChain);
				if (value == null) {
					setIsErroneous(true);
					return -1;
				}
				value = value.getValueRefdLast(timestamp, referenceChain);
			} else {
				//stop processing
				value = null;
			}
		} else if (template != null) {
			/* The size of INDEXED_TEMPLATE_LIST nodes is unknown at compile
		         time.  Don't try to evaluate it at compile time.  */
			if (reference != null && reference.hasUnfoldableIndexSubReference(timestamp)) {
				return -1;
			}
			if (reference != null && subreferences != null) {
				template = template.getReferencedSubTemplate(timestamp, reference, referenceChain, false);
				if (template == null) {
					setIsErroneous(true);
					return -1;
				}
				template = template.getTemplateReferencedLast(timestamp);
			}
		}

		if (template != null) {
			if (template.getIsErroneous(timestamp)) {
				setIsErroneous(true);
				return -1;
			}
			switch(template.getTemplatetype()) {
			case TEMPLATE_REFD:
				template = null;
				break;
			case SPECIFIC_VALUE:
				value = ((SpecificValue_Template) template).getSpecificValue().getValueRefdLast(timestamp, referenceChain);
				template = null;
				break;
			case TEMPLATE_LIST:
			case NAMED_TEMPLATE_LIST:
			case SUBSET_MATCH:
			case SUPERSET_MATCH:
				break;
			default:
				//FIXME this can not happen
				templateInstance.getLocation().reportSemanticError(MessageFormat.format("`sizeof'' operation is not applicable to {0}", template.getTemplateTypeName()));
				setIsErroneous(true);
				return -1;
			}
		}

		if (value != null) {
			switch(value.getValuetype()) {
			case SEQUENCEOF_VALUE:
			case SETOF_VALUE:
			case ARRAY_VALUE:
			case RELATIVEOBJECTIDENTIFIER_VALUE:
			case OBJECTID_VALUE:
			case SEQUENCE_VALUE:
			case SET_VALUE:
				break;
			default:
				value = null;
				return -1;
			}
		}

		/* evaluation */
		if (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
			return ((Array_Type)type).getDimension().getSize();
		} else if (template != null) {
			return evaluateTemplate(template, timestamp);
		} else if (value != null) {
			return evaluateValue(value);
		} else {
			return -1;
		}
	}

	/**
	 * Evaluates a checked value
	 *
	 * @param value
	 *                The value to evaluate.
	 * @return The folded value or -1 if the value is unfoldable.
	 */
	private long evaluateValue(final IValue value) {
		switch (value.getValuetype()) {
		case SEQUENCEOF_VALUE: {
			final SequenceOf_Value seqOfValue = (SequenceOf_Value) value;
			if (seqOfValue.isIndexed()) {
				return -1;
			}
			return seqOfValue.getNofComponents();
		}
		case SETOF_VALUE: {
			final SetOf_Value setOfValue = (SetOf_Value) value;
			if (setOfValue.isIndexed()) {
				return -1;
			}
			return setOfValue.getNofComponents();
		}
		case ARRAY_VALUE: {
			final Array_Value arrayValue = (Array_Value) value;
			if (arrayValue.isIndexed()) {
				return -1;
			}
			return arrayValue.getNofComponents();
		}
		case OBJECTID_VALUE:
			return ((ObjectIdentifier_Value) value).getNofComponents();
		case RELATIVEOBJECTIDENTIFIER_VALUE:
			return ((RelativeObjectIdentifier_Value) value).getNofComponents();
		case SEQUENCE_VALUE: {
			int result = 0;
			final Sequence_Value temp = (Sequence_Value) value;
			for (int i = 0, size = temp.getNofComponents(); i < size; i++) {
				if (!Value_type.OMIT_VALUE.equals(temp.getSeqValueByIndex(i).getValue().getValuetype())) {
					result++;
				}
			}
			return result;
		}
		case SET_VALUE: {
			int result = 0;
			final Set_Value temp = (Set_Value) value;
			for (int i = 0, size = temp.getNofComponents(); i < size; i++) {
				if (!Value_type.OMIT_VALUE.equals(temp.getSequenceValueByIndex(i).getValue().getValuetype())) {
					result++;
				}
			}
			return result;
		}
		default:
			return -1;
		}
	}

	/**
	 * Evaluates a checked template.
	 *
	 * @param template
	 *                The template to evaluate
	 * @param timestamp
	 *                The compilation timestamp
	 * @return The folded value or -1 if the template is unfoldable.
	 */
	private long evaluateTemplate(final ITTCN3Template template, final CompilationTimeStamp timestamp) {
		switch (template.getTemplatetype()) {
		case TEMPLATE_LIST: {
			final Template_List temp = (Template_List) template;
			if (temp.templateContainsAnyornone()) {
				final LengthRestriction lengthRestriction = temp.getLengthRestriction();
				if (lengthRestriction == null) {
					templateInstance.getLocation()
					.reportSemanticError(
							"`sizeof' operation is not applicable for templates containing `*' without length restriction");
					setIsErroneous(true);
					return -1;
				}

				if (lengthRestriction instanceof RangeLenghtRestriction) {
					final IValue upper = ((RangeLenghtRestriction) lengthRestriction).getUpperValue(timestamp);
					if (Value_type.REAL_VALUE.equals(upper.getValuetype()) && ((Real_Value) upper).isPositiveInfinity()) {
						templateInstance.getLocation()
						.reportSemanticError(
								"`sizeof' operation is not applicable for templates containing `*' without upper boundary in the length restriction");
						setIsErroneous(true);
						return -1;
					}

					if (Value_type.INTEGER_VALUE.equals(upper.getValuetype())) {
						final int nofComponents = temp.getNofTemplatesNotAnyornone(timestamp);
						if (nofComponents == ((Integer_Value) upper).intValue()) {
							return nofComponents;
						}

						final IValue lower = ((RangeLenghtRestriction) lengthRestriction).getLowerValue(timestamp);
						if (lower != null && Value_type.INTEGER_VALUE.equals(lower.getValuetype())
								&& ((Integer_Value) upper).intValue() == ((Integer_Value) lower).intValue()) {
							return ((Integer_Value) upper).intValue();
						}

						templateInstance.getLocation().reportSemanticError(
								"`sizeof' operation is not applicable for templates without exact size");
						setIsErroneous(true);
						return -1;
					}
				} else {
					final IValue restriction = ((SingleLenghtRestriction) lengthRestriction).getRestriction(timestamp);
					if (Value_type.INTEGER_VALUE.equals(restriction.getValuetype())) {
						return ((Integer_Value) restriction).intValue();
					}
				}
			} else {
				int result = 0;
				for (int i = 0, size = temp.getNofTemplates(); i < size; i++) {
					final ITTCN3Template tmp = temp.getTemplateByIndex(i);
					switch (tmp.getTemplatetype()) {
					case SPECIFIC_VALUE:
						if (tmp.getValue().getValuetype() != Value_type.OMIT_VALUE) {
							++result;
						}
						break;
					default:
						++result;
						break;
					}
				}
				return result;
			}
			break;
		}
		case NAMED_TEMPLATE_LIST: {
			int result = 0;
			final Named_Template_List temp = (Named_Template_List) template;
			for (int i = 0, size = temp.getNofTemplates(); i < size; i++) {
				final ITTCN3Template tmp = temp.getTemplateByIndex(i).getTemplate();
				switch (tmp.getTemplatetype()) {
				case SPECIFIC_VALUE:
					if (tmp.getValue().getValuetype() != Value_type.OMIT_VALUE) {
						++result;
					}
					break;
				default:
					++result;
					break;
				}
			}
			return result;
		}
		case SUBSET_MATCH:{
			final LengthRestriction restriction = template.getLengthRestriction();
			if (restriction instanceof SingleLenghtRestriction) {
				final IValue value = ((SingleLenghtRestriction) restriction).getRestriction(timestamp);
				if (value.getValuetype() == Value_type.INTEGER_VALUE && !value.isUnfoldable(timestamp)) {
					return ((Integer_Value)value).getValue();
				} else {
					return -1;
				}
			} else if (restriction instanceof RangeLenghtRestriction) {
				final IValue minValue = ((RangeLenghtRestriction) restriction).getLowerValue(timestamp);
				if (minValue.getValuetype() != Value_type.INTEGER_VALUE || minValue.isUnfoldable(timestamp)) {
					return -1;
				}

				final SubsetMatch_Template temp = (SubsetMatch_Template) template;
				if (temp.getNofTemplates() != ((Integer_Value)minValue).getValue()) {
					return -1;
				}

				for (int i = 0, size = temp.getNofTemplates(); i < size; i++) {
					final ITTCN3Template tmp = temp.getTemplateByIndex(i);
					switch (tmp.getTemplatetype()) {
					case SPECIFIC_VALUE:
						break;
					default:
						return -1;
					}
				}

				return temp.getNofTemplates();
			}

			return -1;
		}
		case SUPERSET_MATCH:{
			final LengthRestriction restriction = template.getLengthRestriction();
			if (restriction instanceof SingleLenghtRestriction) {
				final IValue value = ((SingleLenghtRestriction) restriction).getRestriction(timestamp);
				if (value.getValuetype() == Value_type.INTEGER_VALUE && !value.isUnfoldable(timestamp)) {
					return ((Integer_Value)value).getValue();
				} else {
					return -1;
				}
			} else if (restriction instanceof RangeLenghtRestriction) {
				final IValue maxValue = ((RangeLenghtRestriction) restriction).getUpperValue(timestamp);
				if (maxValue.getValuetype() != Value_type.INTEGER_VALUE || maxValue.isUnfoldable(timestamp)) {
					return -1;
				}

				final SupersetMatch_Template temp = (SupersetMatch_Template) template;
				if (temp.getNofTemplates() != ((Integer_Value)maxValue).getValue()) {
					return -1;
				}

				for (int i = 0, size = temp.getNofTemplates(); i < size; i++) {
					final ITTCN3Template tmp = temp.getTemplateByIndex(i);
					switch (tmp.getTemplatetype()) {
					case SPECIFIC_VALUE:
						break;
					default:
						return -1;
					}
				}

				return temp.getNofTemplates();
			}

			return -1;
		}
		default:
			return -1;
		}

		return -1;
	}

	@Override
	/** {@inheritDoc} */
	public IValue evaluateValue(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return lastValue;
		}

		isErroneous = false;
		lastTimeChecked = timestamp;
		lastValue = this;

		if (templateInstance == null) {
			return lastValue;
		}

		final long i = checkExpressionOperands(timestamp, expectedValue, referenceChain);
		if (i != -1) {
			lastValue = new Integer_Value(i);
			lastValue.copyGeneralProperties(this);
		}

		if (getIsErroneous(timestamp) || isUnfoldable(timestamp, referenceChain)) {
			return lastValue;
		}

		return lastValue;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (templateInstance != null) {
			templateInstance.updateSyntax(reparser, false);
			reparser.updateLocation(templateInstance.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (templateInstance == null) {
			return;
		}

		templateInstance.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (templateInstance != null && !templateInstance.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (templateInstance != null) {
			templateInstance.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return templateInstance.hasSingleExpression();
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		final TTCN3Template templateBody = templateInstance.getTemplateBody();
		// FIXME actually a bit more complex
		if (templateInstance.getDerivedReference() == null && Template_type.SPECIFIC_VALUE.equals(templateBody.getTemplatetype())
				&& templateBody.getLengthRestriction() == null
				&& !templateBody.getIfPresent()) {
			final IValue value = ((SpecificValue_Template) templateBody).getSpecificValue();
			// FIXME implement support for explicit cast
			value.generateCodeExpressionMandatory(aData, expression, true);
		} else {
			templateInstance.generateCode(aData, expression, Restriction_type.TR_NONE);
		}

		expression.expression.append(".size_of()");
	}
}
