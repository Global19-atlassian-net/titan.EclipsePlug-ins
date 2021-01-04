/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.ActualParameterList;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Testcase;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameterList;
import org.eclipse.titan.designer.AST.TTCN3.templates.ParsedActualParameters;
import org.eclipse.titan.designer.AST.TTCN3.types.Testcase_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.Real_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Testcase_Reference_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class Referenced_Testcase_Instance_Statement extends Statement {
	private static final String FLOATEXPECTED = "float value expected";
	private static final String FLOATEXPECTED2 = "{0} can not be used as the testcase quard timer duration";
	private static final String DEFINITIONWITHOUTRUNSONEXPECTED = "A definition that has `runs on' clause cannot execute testcases";
	private static final String NEGATIVEDURATION = "The testcase quard timer has negative duration: `{0}''";

	private static final String FULLNAMEPART1 = ".testcasereference";
	private static final String FULLNAMEPART2 = ".<parameters>";
	private static final String FULLNAMEPART3 = ".timerValue";
	private static final String STATEMENT_NAME = "execute";

	private final Value dereferredValue;
	private final ParsedActualParameters actualParameterList;
	private final Value timerValue;

	private ActualParameterList actualParameterList2;

	public Referenced_Testcase_Instance_Statement(final Value dereferredValue, final ParsedActualParameters actualParameterList,
			final Value timerValue) {
		this.dereferredValue = dereferredValue;
		this.actualParameterList = actualParameterList;
		this.timerValue = timerValue;

		if (dereferredValue != null) {
			dereferredValue.setFullNameParent(this);
		}
		if (actualParameterList != null) {
			actualParameterList.setFullNameParent(this);
		}
		if (timerValue != null) {
			timerValue.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_REFERENCED_TESTCASE_INSTANCE;
	}

	@Override
	/** {@inheritDoc} */
	public String getStatementName() {
		return STATEMENT_NAME;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (dereferredValue == child) {
			return builder.append(FULLNAMEPART1);
		} else if (actualParameterList == child) {
			return builder.append(FULLNAMEPART2);
		} else if (timerValue == child) {
			return builder.append(FULLNAMEPART3);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (dereferredValue != null) {
			dereferredValue.setMyScope(scope);
		}
		if (actualParameterList != null) {
			actualParameterList.setMyScope(scope);
		}
		if (timerValue != null) {
			timerValue.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (dereferredValue != null) {
			dereferredValue.setCodeSection(codeSection);
		}
		if (actualParameterList != null) {
			actualParameterList.setCodeSection(codeSection);
		}
		if (timerValue != null) {
			timerValue.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		if (dereferredValue == null) {
			return;
		}

		final IValue temporalValue = dereferredValue.setLoweridToReference(timestamp);
		IType type = temporalValue.getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_TEMPLATE);
		if (type != null) {
			type = type.getTypeRefdLast(timestamp);
		}
		if (type == null) {
			return;
		}

		if (!Type_type.TYPE_TESTCASE.equals(type.getTypetype())) {
			dereferredValue.getLocation().reportSemanticError(
					MessageFormat.format(
							"A value of type testcase was expected in the argument of `derefers()'' instead of `{0}''",
							type.getTypename()));
			return;
		}

		if (myStatementBlock.getScopeRunsOn() != null) {
			dereferredValue.getLocation().reportSemanticError(DEFINITIONWITHOUTRUNSONEXPECTED);
			return;
		}

		actualParameterList2 = new ActualParameterList();
		final FormalParameterList formalParameterList = ((Testcase_Type) type).getFormalParameters();
		formalParameterList.checkActualParameterList(timestamp, actualParameterList, actualParameterList2);

		if (timerValue != null) {
			timerValue.setLoweridToReference(timestamp);
			final Type_type temporalType = timerValue.getExpressionReturntype(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);

			switch (temporalType) {
			case TYPE_REAL:
				final IValue last = timerValue.getValueRefdLast(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, null);
				if (!last.isUnfoldable(timestamp)) {
					final Real_Value real = (Real_Value) last;
					final double i = real.getValue();
					if (i < 0.0) {
						timerValue.getLocation().reportSemanticError(
								MessageFormat.format(NEGATIVEDURATION, real.createStringRepresentation()));
					} else if (real.isPositiveInfinity()) {
						timerValue.getLocation().reportSemanticError(
								MessageFormat.format(FLOATEXPECTED2, real.createStringRepresentation()));
					}
				}
				break;
			default:
				timerValue.getLocation().reportSemanticError(FLOATEXPECTED);
				break;
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (dereferredValue != null) {
			dereferredValue.updateSyntax(reparser, false);
			reparser.updateLocation(dereferredValue.getLocation());
		}

		if (actualParameterList != null) {
			actualParameterList.updateSyntax(reparser, false);
			reparser.updateLocation(actualParameterList.getLocation());
		}

		if (timerValue != null) {
			timerValue.updateSyntax(reparser, false);
			reparser.updateLocation(timerValue.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (dereferredValue != null) {
			dereferredValue.findReferences(referenceFinder, foundIdentifiers);
		}
		if (actualParameterList != null) {
			actualParameterList.findReferences(referenceFinder, foundIdentifiers);
		}
		if (timerValue != null) {
			timerValue.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (dereferredValue != null && !dereferredValue.accept(v)) {
			return false;
		}
		if (actualParameterList != null && !actualParameterList.accept(v)) {
			return false;
		}
		if (timerValue != null && !timerValue.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport("TitanFloat");
		aData.addBuiltinTypeImport("Ttcn3Float");

		final ExpressionStruct expression = new ExpressionStruct();
		final IValue last = dereferredValue.getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_DYNAMIC_VALUE, null);
		if (last.getValuetype().equals(Value_type.TESTCASE_REFERENCE_VALUE)) {
			final Testcase_Reference_Value refdValue = (Testcase_Reference_Value)last;
			final Def_Testcase definition = refdValue.getReferredTestcase();

			expression.expression.append(MessageFormat.format("testcase_{0}(", definition.getGenNameFromScope(aData, source, "")));
			if (actualParameterList2 != null && actualParameterList2.getNofParameters() > 0) {
				actualParameterList2.generateCodeAlias(aData, expression, definition.getFormalParameterList());
				expression.expression.append(", ");
			}
			if (timerValue == null) {
				expression.expression.append("false, new TitanFloat( new Ttcn3Float( 0.0 ) ))");
			} else {
				expression.expression.append("true, ");
				timerValue.generateCodeExpression(aData, expression, true);
				expression.expression.append(')');
			}
			expression.mergeExpression(source);

			return;
		}

		dereferredValue.generateCodeExpressionMandatory(aData, expression, true);
		expression.expression.append(".execute(");
		actualParameterList2.generateCodeAlias(aData, expression, null);
		if (actualParameterList2.getNofParameters() > 0) {
			expression.expression.append(", ");
		}
		if (timerValue == null) {
			expression.expression.append("false, new TitanFloat( new Ttcn3Float( 0.0 ) ))");
		} else {
			expression.expression.append("true, ");
			timerValue.generateCodeExpression(aData, expression, true);
			expression.expression.append(')');
		}
		expression.mergeExpression(source);
	}
}
