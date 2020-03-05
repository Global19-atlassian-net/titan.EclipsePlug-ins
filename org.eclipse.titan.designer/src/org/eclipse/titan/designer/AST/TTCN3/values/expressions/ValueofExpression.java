/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * @author Kristof Szabados
 * */
public final class ValueofExpression extends Expression_Value {
	private static final String OPERANDERROR = "Cannot determine the argument type of `valueof' operation";

	private final TemplateInstance templateInstance;

	public ValueofExpression(final TemplateInstance templateInstance) {
		this.templateInstance = templateInstance;

		if (templateInstance != null) {
			templateInstance.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.VALUEOF_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (templateInstance!= null && templateInstance.getTemplateBody().checkExpressionSelfReferenceTemplate(timestamp, lhs)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("valueof(");
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

	public TemplateInstance getTemplateInstance() {
		return templateInstance;
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
		if (templateInstance != null) {
			return templateInstance.getExpressionReturntype(timestamp, Expected_Value_type.EXPECTED_TEMPLATE);
		}

		return Type_type.TYPE_UNDEFINED;
	}

	@Override
	/** {@inheritDoc} */
	public IType getExpressionGovernor(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		final IType governor = super.getMyGovernor();

		if (governor != null) {
			return governor;
		}

		if (templateInstance == null) {
			return null;
		}

		IType tempType;
		if (Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue)) {
			tempType = templateInstance.getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_TEMPLATE);
		} else {
			tempType = templateInstance.getExpressionGovernor(timestamp, expectedValue);
		}

		if (tempType != null) {
			tempType = tempType.getTypeRefdLast(timestamp);
		}
		return tempType;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if(templateInstance == null) {
			return true;
		}
		if (templateInstance.getDerivedReference() != null) {
			return true;
		}

		final ITTCN3Template temp = templateInstance.getTemplateBody().setLoweridToReference(timestamp);
		if (temp == null || !temp.isValue(timestamp) || temp.getValue().isUnfoldable(timestamp)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
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
		if (templateInstance == null) {
			setIsErroneous(true);
			return;
		}

		final Expected_Value_type internalExpectation = Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue) ? Expected_Value_type.EXPECTED_TEMPLATE
				: expectedValue;
		ITTCN3Template template = templateInstance.getTemplateBody();
		template.setMyGovernor(null);
		IType governor = myGovernor;
		if (governor == null) {
			governor = templateInstance.getExpressionGovernor(timestamp, internalExpectation);
		}
		if (governor == null) {
			template = template.setLoweridToReference(timestamp);
			governor = template.getExpressionGovernor(timestamp, internalExpectation);
		}

		if (governor == null) {
			if (!template.getIsErroneous(timestamp)) {
				templateInstance.getLocation().reportSemanticError(OPERANDERROR);
			}
			setIsErroneous(true);
			return;
		}

		IsValueExpression.checkExpressionTemplateInstance(timestamp, this, templateInstance, governor, referenceChain, expectedValue);

		if (getIsErroneous(timestamp)) {
			return;
		}

		templateInstance.getTemplateBody().checkSpecificValue(timestamp, false);
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

		if (getIsErroneous(timestamp) || isUnfoldable(timestamp, referenceChain)) {
			return lastValue;
		}

		if (templateInstance.getDerivedReference() == null && templateInstance.getTemplateBody().isValue(timestamp)) {
			lastValue = templateInstance.getTemplateBody().getValue();
			lastValue = lastValue.setLoweridToReference(timestamp);
			lastValue = lastValue.getValueRefdLast(timestamp, expectedValue, referenceChain);
			getLocation().reportConfigurableSemanticProblem(
					Platform.getPreferencesService().getString(ProductConstants.PRODUCT_ID_DESIGNER,
							PreferenceConstants.REPORTUNNECESSARYCONTROLS, GeneralConstants.WARNING, null),
					"Applying the `valueof' operation to a value will result in the original value");
		}

		return lastValue;
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this)) {
			if (templateInstance != null) {
				referenceChain.markState();
				templateInstance.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
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

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (templateInstance != null) {
			templateInstance.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		if (templateInstance != null) {
			templateInstance.generateCode(aData, expression, Restriction_type.TR_NONE);
			expression.expression.append( ".valueof()" );
		}
	}
}
