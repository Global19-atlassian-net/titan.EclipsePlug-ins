/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType.ValueCheckingOptions;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.Integer_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.ArrayDimension;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a length restriction for a single length.
 *
 * @author Kristof Szabados
 * */
public final class SingleLenghtRestriction extends LengthRestriction {

	private final Value value;

	/** The time when this restriction was check the last time. */
	private CompilationTimeStamp lastTimeChecked;

	public SingleLenghtRestriction(final Value value) {
		super();
		this.value = value;

		if (value != null) {
			value.setFullNameParent(this);
		}
	}

	public IValue getRestriction(final CompilationTimeStamp timestamp) {
		if (value == null) {
			return null;
		}

		final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = value.getValueRefdLast(timestamp, chain);
		chain.release();

		return last;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		if (value == null) {
			return "<erroneous length restriction>";
		}

		final StringBuilder builder = new StringBuilder("length(");
		builder.append(value.createStringRepresentation());
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
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (value != null) {
			value.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}
		lastTimeChecked = timestamp;

		if( value == null) {
			return;
		}

		final Integer_Type integer = new Integer_Type();
		value.setMyGovernor(integer);
		IValue last = integer.checkThisValueRef(timestamp, value);
		integer.checkThisValue(timestamp, last, null, new ValueCheckingOptions(expectedValue, false, false, true, false, false));

		final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		last = last.getValueRefdLast(timestamp, chain);
		chain.release();

		if (last.getIsErroneous(timestamp)) {
			return;
		}

		switch (last.getValuetype()) {
		case INTEGER_VALUE: {
			final BigInteger temp = ((Integer_Value) last).getValueValue();
			if (temp.compareTo(BigInteger.ZERO) == -1) {
				value.getLocation().reportSemanticError(
						MessageFormat.format("The length restriction must be a non-negative integer value instead of {0}",
								temp));
				value.setIsErroneous(true);
			}
			break;
		}
		default:
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkArraySize(final CompilationTimeStamp timestamp, final ArrayDimension dimension) {
		if (lastTimeChecked == null || dimension.getIsErroneous(timestamp) || value == null) {
			return;
		}

		boolean errorFlag = false;
		final long arraySize = dimension.getSize();

		final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = value.getValueRefdLast(timestamp, chain);
		chain.release();

		if (Value_type.INTEGER_VALUE.equals(last.getValuetype()) && !last.getIsErroneous(timestamp)) {
			final BigInteger length = ((Integer_Value) last).getValueValue();
			final int compareResult = length.compareTo(BigInteger.valueOf(arraySize));
			if (compareResult != 0) {
				final String message = MessageFormat.format(
						"There number of elements allowed by the length restriction ({0}) contradicts the array size ({1})",
						length, arraySize);
				value.getLocation().reportSemanticError(message);
				errorFlag = true;
			}
		}

		if (!errorFlag) {
			getLocation().reportSemanticWarning("Length restriction is useless for an array template");
		}
	}

	/**
	 * Checks if the single length restriction is applicable for the given value
	 * If the the length restriction value is too small it reports semantic marker error
	 */
	@Override
	public void checkNofElements(final CompilationTimeStamp timestamp, final int nofElements, final boolean lessAllowed,
			final boolean moreAllowed, final boolean hasAnyornone, final ILocateableNode locatable) {
		if (value == null) {
			return;
		}

		final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = value.getValueRefdLast(timestamp, chain);
		chain.release();

		if (Value_type.INTEGER_VALUE.equals(last.getValuetype()) && !last.getIsErroneous(timestamp)) {
			final BigInteger length = ((Integer_Value) last).getValueValue();
			final int compareResult = length.compareTo(BigInteger.valueOf(nofElements));
			if (compareResult == -1 && !moreAllowed) {
				final String message = MessageFormat.format(
						"There are more ({0}{1}) elements than it is allowed by the length restriction ({2})",
						hasAnyornone ? "at least " : "", nofElements, length);
				locatable.getLocation().reportSemanticError(message);
			} else if (compareResult == 1 && !lessAllowed) {
				locatable.getLocation().reportSemanticError(
						MessageFormat.format(
								"There are fewer ({0}) elements than it is allowed by the length restriction ({1})",
								nofElements, length));
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
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (value != null) {
			value.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (value != null && !value.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (value != null) {
			value.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		ExpressionStruct expression = new ExpressionStruct();
		value.generateCodeExpression(aData, expression);

		if (expression.preamble.length() > 0 || expression.postamble.length() > 0) {
			source.append("{\n");
			source.append(expression.preamble);
			source.append(MessageFormat.format("{0}.set_single_length({1});\n", name, expression.expression));
			source.append(expression.postamble);
			source.append("}\n");
		} else {
			source.append(MessageFormat.format("{0}.set_single_length({1});\n", name, expression.expression));
		}
	}
}
