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
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.ActualParameterList;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Real_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class ExecuteExpression extends Expression_Value {
	private static final String OPERANDERROR = "The second operand of the `execute' operation should be a float value";
	private static final String NEGATIVEDURATION = "The testcase guard timer has negative value: `{0}''";
	private static final String FLOATEXPECTED = "{0} can not be used as the testcase quard timer duration";

	private static final String OPERATIONNAME = "execute()";

	private final Reference reference;
	private final Value timerValue;

	public ExecuteExpression(final Reference reference, final Value timerValue) {
		this.reference = reference;
		this.timerValue = timerValue;

		if (reference != null) {
			reference.setFullNameParent(this);
		}
		if (timerValue != null) {
			timerValue.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Operation_type getOperationType() {
		return Operation_type.EXECUTE_OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReference(final CompilationTimeStamp timestamp, final Assignment lhs) {
		if (lhs == reference.getRefdAssignment(timestamp, false)) {
			return true;
		}
		if (timerValue != null && timerValue.checkExpressionSelfReferenceValue(timestamp, lhs)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("execute(");
		if (reference != null) {
			builder.append(reference.getDisplayName());
		}
		if (timerValue != null) {
			builder.append(", ");
			builder.append(timerValue.createStringRepresentation());
		}
		builder.append(')');

		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (reference != null) {
			reference.setMyScope(scope);
		}
		if (timerValue != null) {
			timerValue.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (reference == child) {
			return builder.append(OPERAND1);
		} else if (timerValue == child) {
			return builder.append(OPERAND2);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_VERDICT;
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
		final Assignment assignment = reference.getRefdAssignment(timestamp, true);

		if (assignment == null) {
			setIsErroneous(true);
		} else if (!Assignment_type.A_TESTCASE.semanticallyEquals(assignment.getAssignmentType())) {
			reference.getLocation().reportSemanticError(
					MessageFormat.format("Reference to a testcase was expected in the argument instead of {0}",
							assignment.getDescription()));
			setIsErroneous(true);
		}

		if (timerValue != null) {
			timerValue.setLoweridToReference(timestamp);
			final Type_type tempType = timerValue.getExpressionReturntype(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);

			switch (tempType) {
			case TYPE_REAL:
				final IValue last = timerValue.getValueRefdLast(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, referenceChain);
				if (!last.isUnfoldable(timestamp)) {
					final Real_Value real = (Real_Value) last;
					final double i = real.getValue();
					if (i < 0.0) {
						timerValue.getLocation().reportSemanticError(
								MessageFormat.format(NEGATIVEDURATION, real.createStringRepresentation()));
					} else if (real.isPositiveInfinity()) {
						timerValue.getLocation().reportSemanticError(
								MessageFormat.format(FLOATEXPECTED, real.createStringRepresentation()));
					}
				}
				return;
			case TYPE_UNDEFINED:
				setIsErroneous(true);
				return;
			default:
				if (!isErroneous) {
					timerValue.getLocation().reportSemanticError(OPERANDERROR);
					setIsErroneous(true);
				}
				return;
			}
		}

		checkExpressionDynamicPart(expectedValue, OPERATIONNAME, true, false, false);
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

		if (reference == null) {
			return lastValue;
		}

		checkExpressionOperands(timestamp, expectedValue, referenceChain);

		return lastValue;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (reference != null) {
			reference.updateSyntax(reparser, false);
			reparser.updateLocation(reference.getLocation());
		}

		if (timerValue != null) {
			timerValue.updateSyntax(reparser, false);
			reparser.updateLocation(timerValue.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (reference != null) {
			reference.findReferences(referenceFinder, foundIdentifiers);
		}
		if (timerValue != null) {
			timerValue.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (reference != null && !reference.accept(v)) {
			return false;
		}
		if (timerValue != null && !timerValue.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionExpression(final JavaGenData aData, final ExpressionStruct expression) {
		Assignment testcase = reference.getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false);
		expression.expression.append(MessageFormat.format("{0}(", testcase.getGenNameFromScope(aData, expression.expression, myScope, "testcase_")));

		List<ISubReference> subReferences = reference.getSubreferences();
		if (!subReferences.isEmpty() && subReferences.get(0) instanceof ParameterisedSubReference) {
			ActualParameterList actualParList = ((ParameterisedSubReference) subReferences.get(0)).getActualParameters();
			if (actualParList.getNofParameters() > 0) {
				actualParList.generateCodeAlias(aData, expression);
				expression.expression.append(", ");
			}
		}

		if (timerValue != null) {
			expression.expression.append("true, ");
			timerValue.generateCodeExpression(aData, expression);
			expression.expression.append(')');
		} else {
			expression.expression.append("false, new TitanFloat( new Ttcn3Float( 0.0 ) ))");
		}
	}
}
