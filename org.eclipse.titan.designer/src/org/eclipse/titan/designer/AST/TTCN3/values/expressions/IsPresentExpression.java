/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.Referenced_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.SpecificValue_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template.Template_type;
import org.eclipse.titan.designer.AST.TTCN3.values.Boolean_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Referenced_Value;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class IsPresentExpression extends Expression_Value {
	private static final String OPERANDERROR = "Cannot determine the argument type of `ispresent()' operation";
	private static final String CONSTEXPECTED1 = "Reference to a constant was expected instead of an in-line modified template";
	private static final String CONSTEXPECTED2 = "Reference to a constant value was expected instead of {0}";
	private static final String STATICEXPECTED1 = "Reference to a static was expected instead of an in-line modified template";
	private static final String STATICEXPECTED2 = "Reference to a static value was expected instead of {0}";

	private final TemplateInstance templateInstance;

	public IsPresentExpression(final TemplateInstance templateInstance) {
		this.templateInstance = templateInstance;

		if (templateInstance != null) {
			templateInstance.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.ISPRESENT_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("ispresent(");
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
		return Type_type.TYPE_BOOL;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (templateInstance == null) {
			return true;
		}

		final ITTCN3Template template = templateInstance.getTemplateBody().setLoweridToReference(timestamp);
		if (templateInstance.getDerivedReference() != null) {
			return true;
		}

		if (Template_type.SPECIFIC_VALUE.equals(template.getTemplatetype())) {
			final IValue specificValue = ((SpecificValue_Template) template).getValue();
			if (Value_type.REFERENCED_VALUE.equals(specificValue.getValuetype())) {
				final Reference reference = ((Referenced_Value) specificValue).getReference();
				final Assignment ass = reference.getRefdAssignment(timestamp, false);
				if (ass == null) {
					return true;
				}

				switch (ass.getAssignmentType()) {
				case A_OBJECT:
				case A_OS:
				case A_CONST:
				case A_EXT_CONST:
				case A_MODULEPAR:
				case A_VAR:
				case A_FUNCTION_RVAL:
				case A_EXT_FUNCTION_RVAL:
				case A_PAR_VAL:
				case A_PAR_VAL_IN:
				case A_PAR_VAL_OUT:
				case A_PAR_VAL_INOUT:
					break;
				default:
					return true;
				}

				// TODO improve to better detect unbound
				// elements
				final IValue last = specificValue.getValueRefdLast(timestamp, expectedValue, null);
				if (last == null) {
					return true;
				}
				if (last == this) {
					return getIsErroneous(timestamp);
				}

				return last.isUnfoldable(timestamp, expectedValue, referenceChain);
			}

			return specificValue.isUnfoldable(timestamp, expectedValue, referenceChain);
		} else if (Template_type.TEMPLATE_REFD.equals(template.getTemplatetype())) {
			final Reference reference = ((Referenced_Template) template).getReference();
			final Assignment ass = reference.getRefdAssignment(timestamp, true);
			if (ass == null) {
				return true;
			}

			switch (ass.getAssignmentType()) {
			case A_CONST:
				return false; //const is foldable
			case A_TEMPLATE:
				break;
			default:
				return true;
			}

			// TODO improve to better detect unbound elements
			final TTCN3Template last = template.getTemplateReferencedLast(timestamp);
			if (last == null) {
				return true;
			}
			if (last == template) {
				return last.getIsErroneous(timestamp);
			}

			if (Template_type.SPECIFIC_VALUE.equals(last.getTemplatetype())) {
				return ((SpecificValue_Template) last).getValue().isUnfoldable(timestamp, expectedValue, referenceChain);
			}
		}

		return true;
	}

	@Override
	public IValue setLoweridToReference(final CompilationTimeStamp timestamp) {
		if (templateInstance != null && templateInstance.getType() != null && templateInstance.getDerivedReference() != null) {
			templateInstance.getTemplateBody().setLoweridToReference(timestamp);
		}

		return this;
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
	 * */
	private void checkExpressionOperands(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		final Expected_Value_type internalExpectation = Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue) ? Expected_Value_type.EXPECTED_TEMPLATE
				: expectedValue;

		IType governor = templateInstance.getExpressionGovernor(timestamp, internalExpectation);
		if (governor == null) {
			final ITTCN3Template template = templateInstance.getTemplateBody().setLoweridToReference(timestamp);
			governor = template.getExpressionGovernor(timestamp, internalExpectation);
		}
		if (governor == null) {
			templateInstance.getLocation().reportSemanticError(OPERANDERROR);
			setIsErroneous(true);
		} else {
			templateInstance.getExpressionReturntype(timestamp, internalExpectation);
			checkExpressionTemplateInstance(timestamp, this, templateInstance, governor, referenceChain, expectedValue);
		}
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

		checkExpressionOperands(timestamp, expectedValue, referenceChain);

		if (getIsErroneous(timestamp)) {
			return lastValue;
		}

		if (isUnfoldable(timestamp, referenceChain)) {
			return lastValue;
		}

		ITTCN3Template template = templateInstance.getTemplateBody();
		template = template.getTemplateReferencedLast(timestamp);
		boolean result = false;
		if (template.getTemplatetype() == Template_type.TEMPLATE_REFD) {
			final TTCN3Template last = template.getTemplateReferencedLast(timestamp);
			if (last != null && Template_type.SPECIFIC_VALUE.equals(last.getTemplatetype())) {
				result = ((SpecificValue_Template) last).getValue().evaluateIspresent(timestamp,
						((Referenced_Template) template).getReference(), 1);
			}
		} else if (template.getTemplatetype() == Template_type.SPECIFIC_VALUE) {
			final IValue value = ((SpecificValue_Template) template).getValue();
			if (value.getValuetype() == Value_type.REFERENCED_VALUE) {
				result = value.evaluateIspresent(timestamp, ((Referenced_Value) value).getReference(), 1);
			} else {
				result = !value.getIsErroneous(timestamp);
			}
		}

		lastValue = new Boolean_Value(result);
		lastValue.copyGeneralProperties(this);
		return lastValue;
	}

	/**
	 * Checks if the templateinstance parameter (which is a parameter of the
	 * expression parameter) is actually a constant or static value or not.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param expression
	 *                the expression to report the possible errors to.
	 * @param instance
	 *                the template instance parameter of the expression to
	 *                be checked.
	 * @param type
	 *                the type against which the template instance shall be
	 *                checked.
	 * @param referenceChain
	 *                the reference chain to detect circular references.
	 * @param expectedValue
	 *                the expected value of the template instance.
	 *
	 * */
	private static void checkExpressionTemplateInstance(final CompilationTimeStamp timestamp, final Expression_Value expression,
			final TemplateInstance instance, final IType type, final IReferenceChain referenceChain,
			final Expected_Value_type expectedValue) {
		Expected_Value_type internalExpectation;
		if (Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue)) {
			internalExpectation = Expected_Value_type.EXPECTED_TEMPLATE;
		} else {
			internalExpectation = expectedValue;
		}

		final ITTCN3Template body = instance.getTemplateBody();
		if (body.getTemplatetype() == Template_type.TEMPLATE_REFD) {
			((Referenced_Template) body).getReference().setUsedInIsbound(); // FIXME
		} else if (body.getTemplatetype() == Template_type.SPECIFIC_VALUE) {
			final IValue value = ((SpecificValue_Template) body).getValue();
			if (value.getValuetype() == Value_type.REFERENCED_VALUE) {
				((Referenced_Value) value).getReference().setUsedInIsbound(); // FIXME
			}
		}

		instance.check(timestamp, type);

		if (!Expected_Value_type.EXPECTED_TEMPLATE.equals(internalExpectation) && instance.getDerivedReference() != null) {
			if (Expected_Value_type.EXPECTED_CONSTANT.equals(internalExpectation)) {
				instance.getLocation().reportSemanticError(CONSTEXPECTED1);
			} else {
				instance.getLocation().reportSemanticError(STATICEXPECTED1);
			}

			expression.setIsErroneous(true);
		}

		ITTCN3Template template = instance.getTemplateBody();
		if (template.getIsErroneous(timestamp)) {
			expression.setIsErroneous(true);
			return;
		}

		switch (template.getTemplatetype()) {
		case TEMPLATE_REFD:
			if (Expected_Value_type.EXPECTED_TEMPLATE.equals(internalExpectation)) {
				template = template.getTemplateReferencedLast(timestamp, referenceChain);
				if (template.getIsErroneous(timestamp)) {
					expression.setIsErroneous(true);
				}
			} else {
				if (Expected_Value_type.EXPECTED_CONSTANT.equals(internalExpectation)) {
					instance.getLocation().reportSemanticError(
							MessageFormat.format(CONSTEXPECTED2, ((Referenced_Template) template).getReference()
									.getRefdAssignment(timestamp, true).getDescription()));
				} else {
					instance.getLocation().reportSemanticError(
							MessageFormat.format(STATICEXPECTED2, ((Referenced_Template) template).getReference()
									.getRefdAssignment(timestamp, true).getDescription()));
				}

				expression.setIsErroneous(true);
			}
			break;
		case SPECIFIC_VALUE:
			final IValue tempValue = ((SpecificValue_Template) template).getSpecificValue();
			switch (tempValue.getValuetype()) {
			case REFERENCED_VALUE:
				type.checkThisValueRef(timestamp, tempValue);
				break;
			case EXPRESSION_VALUE:
				tempValue.getValueRefdLast(timestamp, referenceChain);
				break;
			default:
				break;
			}

			if (tempValue.getIsErroneous(timestamp)) {
				expression.setIsErroneous(true);
			}
			break;
		default:
			break;
		}
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
}
