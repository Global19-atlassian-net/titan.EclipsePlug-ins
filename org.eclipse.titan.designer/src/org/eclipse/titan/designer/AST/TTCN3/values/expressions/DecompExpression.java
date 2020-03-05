/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values.expressions;

import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class DecompExpression extends Expression_Value {
	private static final String OPERANDERROR = "Built-in function `decomp' is not yet supported";

	private final Value value1;
	private final Value value2;
	private final Value value3;

	public DecompExpression(final Value value1, final Value value2, final Value value3) {
		this.value1 = value1;
		this.value2 = value2;
		this.value3 = value3;

		if (value1 != null) {
			value1.setFullNameParent(this);
		}
		if (value2 != null) {
			value2.setFullNameParent(this);
		}
		if (value3 != null) {
			value3.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.DECOMP_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (value1 != null) {
			value1.setMyScope(scope);
		}
		if (value2 != null) {
			value2.setMyScope(scope);
		}
		if (value3 != null) {
			value3.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);

		if (value1 != null) {
			value1.setCodeSection(codeSection);
		}
		if (value2 != null) {
			value2.setCodeSection(codeSection);
		}
		if (value3 != null) {
			value3.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (value1 == child) {
			return builder.append(OPERAND1);
		} else if (value2 == child) {
			return builder.append(OPERAND2);
		} else if (value3 == child) {
			return builder.append(OPERAND3);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_OBJECTID;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (value1 != null && value1.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}
		if (value2 != null && value2.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}
		if (value3 != null && value3.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("decomp(");
		builder.append(value1.createStringRepresentation());
		builder.append(", ");
		builder.append(value2.createStringRepresentation());
		builder.append(", ");
		builder.append(value3.createStringRepresentation());
		builder.append(')');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (value1 == null || value2 == null || value3 == null) {
			return true;
		}

		final boolean result = value1.isUnfoldable(timestamp, expectedValue, referenceChain)
				|| value2.isUnfoldable(timestamp, expectedValue, referenceChain)
				|| value3.isUnfoldable(timestamp, expectedValue, referenceChain);

		return result || getIsErroneous(timestamp);
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

		location.reportSemanticError(OPERANDERROR);
		setIsErroneous(true);

		return lastValue;
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this)) {
			if (value1 != null) {
				referenceChain.markState();
				value1.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
			if (value2 != null) {
				referenceChain.markState();
				value2.checkRecursions(timestamp, referenceChain);
				referenceChain.previousState();
			}
			if (value3 != null) {
				referenceChain.markState();
				value3.checkRecursions(timestamp, referenceChain);
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
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (value1 != null) {
			value1.findReferences(referenceFinder, foundIdentifiers);
		}
		if (value2 != null) {
			value2.findReferences(referenceFinder, foundIdentifiers);
		}
		if (value3 != null) {
			value3.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (value1 != null && !value1.accept(v)) {
			return false;
		}
		if (value2 != null && !value2.accept(v)) {
			return false;
		}
		if (value3 != null && !value3.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return value1.canGenerateSingleExpression() && value2.canGenerateSingleExpression()
				&& value3.canGenerateSingleExpression();
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (value1 != null) {
			value1.reArrangeInitCode(aData, source, usageModule);
		}
		if (value2 != null) {
			value2.reArrangeInitCode(aData, source, usageModule);
		}
		if (value3 != null) {
			value3.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		aData.addCommonLibraryImport("AdditionalFunctions");

		expression.expression.append("AdditionalFunctions.decomp( ");
		final IValue lastValue1 = value1.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE, null);
		lastValue1.generateCodeExpressionMandatory(aData, expression, true);
		expression.expression.append(", ");
		final IValue lastValue2 = value2.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE, null);
		lastValue2.generateCodeExpressionMandatory(aData, expression, true);
		expression.expression.append(", ");
		final IValue lastValue3 = value3.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE, null);
		lastValue3.generateCodeExpressionMandatory(aData, expression, true);
		expression.expression.append(')');
	}
}
