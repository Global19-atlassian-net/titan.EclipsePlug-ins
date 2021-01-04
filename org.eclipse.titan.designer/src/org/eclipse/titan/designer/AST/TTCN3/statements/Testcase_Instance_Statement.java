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
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.ActualParameterList;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Testcase;
import org.eclipse.titan.designer.AST.TTCN3.values.Real_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
/**
 * @author Kristof Szabados
 * */
public final class Testcase_Instance_Statement extends Statement {
	private static final String FLOATEXPECTED = "float value expected";
	private static final String FLOATEXPECTED2 = "{0} can not be used as the testcase quard timer duration";
	private static final String DEFINITIONWITHOUTRUNSONEXPECTED = "A definition that has `runs on' clause cannot execute testcases";
	private static final String NEGATIVEDURATION = "The testcase quard timer has negative duration: `{0}''";
	private static final String TESTCASEEXPECTED = "Reference to a testcase was expected in the argument instead of {0}";

	private static final String FULLNAMEPART1 = ".testcasereference";
	private static final String FULLNAMEPART2 = ".timerValue";
	private static final String STATEMENT_NAME = "execute";

	private final Reference testcaseReference;
	private final Value timerValue;

	public Testcase_Instance_Statement(final Reference testcaseReference, final Value timerValue) {
		this.testcaseReference = testcaseReference;
		this.timerValue = timerValue;

		if (testcaseReference != null) {
			testcaseReference.setFullNameParent(this);
		}
		if (timerValue != null) {
			timerValue.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_TESTCASE_INSTANCE;
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

		if (testcaseReference == child) {
			return builder.append(FULLNAMEPART1);
		} else if (timerValue == child) {
			return builder.append(FULLNAMEPART2);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (testcaseReference != null) {
			testcaseReference.setMyScope(scope);
		}
		if (timerValue != null) {
			timerValue.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (testcaseReference != null) {
			testcaseReference.setCodeSection(codeSection);
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

		if (testcaseReference == null) {
			return;
		}

		final Assignment assignment = testcaseReference.getRefdAssignment(timestamp, true);
		if (assignment == null) {
			return;
		}

		if (!Assignment_type.A_TESTCASE.semanticallyEquals(assignment.getAssignmentType())) {
			testcaseReference.getLocation().reportSemanticError(MessageFormat.format(TESTCASEEXPECTED, assignment.getFullName()));
			return;
		}

		if (myStatementBlock.getScopeRunsOn() != null) {
			testcaseReference.getLocation().reportSemanticError(DEFINITIONWITHOUTRUNSONEXPECTED);
			return;
		}

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

		if (testcaseReference != null) {
			testcaseReference.updateSyntax(reparser, false);
			reparser.updateLocation(testcaseReference.getLocation());
		}

		if (timerValue != null) {
			timerValue.updateSyntax(reparser, false);
			reparser.updateLocation(timerValue.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (testcaseReference != null) {
			testcaseReference.findReferences(referenceFinder, foundIdentifiers);
		}
		if (timerValue != null) {
			timerValue.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (testcaseReference != null && !testcaseReference.accept(v)) {
			return false;
		}
		if (timerValue != null && !timerValue.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		source.append( "\t\t" );
		final ExpressionStruct expression = new ExpressionStruct();
		final Assignment testcase = testcaseReference.getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false);
		expression.expression.append(MessageFormat.format("{0}(", testcase.getGenNameFromScope(aData, source, "testcase_")));

		final List<ISubReference> subReferences = testcaseReference.getSubreferences();
		if (!subReferences.isEmpty() && subReferences.get(0) instanceof ParameterisedSubReference) {
			final ActualParameterList actualParList = ((ParameterisedSubReference) subReferences.get(0)).getActualParameters();
			if (actualParList.getNofParameters() > 0) {
				actualParList.generateCodeAlias(aData, expression, ((Def_Testcase)testcase).getFormalParameterList());
				expression.expression.append(", ");
			}
		}

		if (timerValue != null) {
			expression.expression.append("true, ");
			timerValue.generateCodeExpression(aData, expression, true);
			expression.expression.append(')');
		} else {
			aData.addBuiltinTypeImport("TitanFloat");
			aData.addBuiltinTypeImport("Ttcn3Float");

			expression.expression.append("false, new TitanFloat( new Ttcn3Float( 0.0 ) ))");
		}

		expression.mergeExpression(source);
	}
}
