/******************************************************************************
 * Copyright (c) 2000-2016 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.util.List;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class MatchExpression extends Expression_Value {
	private final Value value;
	private final TemplateInstance templateInstance;

	public MatchExpression(final Value value, final TemplateInstance templateInstance) {
		this.value = value;
		this.templateInstance = templateInstance;

		if (value != null) {
			value.setFullNameParent(this);
		}
		if (templateInstance != null) {
			templateInstance.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.MATCH_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("match(");
		builder.append(value.createStringRepresentation());
		builder.append(", ");
		builder.append(templateInstance.createStringRepresentation());
		builder.append(')');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (value != null) {
			value.setMyScope(scope);
		}
		if (templateInstance != null) {
			templateInstance.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (value == child) {
			return builder.append(OPERAND1);
		} else if (templateInstance == child) {
			return builder.append(OPERAND2);
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
		return true;
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
		if (value == null || templateInstance == null) {
			setIsErroneous(true);
			return;
		}

		if (value.getIsErroneous(timestamp) || templateInstance.getTemplateBody().getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return;
		}

		final Expected_Value_type internalExpectation = Expected_Value_type.EXPECTED_DYNAMIC_VALUE.equals(expectedValue) ? Expected_Value_type.EXPECTED_TEMPLATE
				: expectedValue;
		
		IType localGovernor = templateInstance.getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_TEMPLATE);
		if (localGovernor == null) {
			final ITTCN3Template template = templateInstance.getTemplateBody().setLoweridToReference(timestamp);
			localGovernor = template.getExpressionGovernor(timestamp, internalExpectation);
		}
		if(localGovernor == null) {
			setIsErroneous(true);
			return;
		}

		templateInstance.getTemplateBody().checkThisTemplateGeneric(timestamp, localGovernor, false, false, false, true, false);

		try {
			ExpressionUtilities.checkExpressionOperatorCompatibility(timestamp, this, referenceChain, Expected_Value_type.EXPECTED_TEMPLATE, value, templateInstance);
		} catch (StackOverflowError e) {
			ErrorReporter.logExceptionStackTrace("Stack overflow was detected while analysing `" + getFullName() + "'", e.getCause());
			getLocation().reportSemanticError("Titan was unable to analyse this statement");
			setIsErroneous(true);
		}

		if (getIsErroneous(timestamp)) {
			return;
		}

		value.getValueRefdLast(timestamp, expectedValue, referenceChain);
		templateInstance.getTemplateBody().getTemplateReferencedLast(timestamp, referenceChain);
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

		return lastValue;
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this)) {
			if (value != null) {
				referenceChain.markState();
				value.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
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

		if (value != null) {
			value.updateSyntax(reparser, false);
			reparser.updateLocation(value.getLocation());
		}

		if (templateInstance != null) {
			templateInstance.updateSyntax(reparser, false);
			reparser.updateLocation(templateInstance.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (value != null) {
			value.findReferences(referenceFinder, foundIdentifiers);
		}
		if (templateInstance != null) {
			templateInstance.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (value != null && !value.accept(v)) {
			return false;
		}
		if (templateInstance != null && !templateInstance.accept(v)) {
			return false;
		}
		return true;
	}
}
