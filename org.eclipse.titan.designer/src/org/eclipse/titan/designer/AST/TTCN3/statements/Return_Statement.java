/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IType.ValueCheckingOptions;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReparseUtilities;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class Return_Statement extends Statement {
	private static final String SPECIFICVALUEEXPECTED = "A specific value without matching symbols was expected as return value";
	private static final String MISSINGTEMPLATE = "Missing return template. The function should return a template of type `{0}''";
	private static final String MISSINGVALUE = "Missing return value. The function should return a value of type `{0}''";
	private static final String UNEXPECTEDRETURNVALUE = "Unexpected return value. The function does not have return type";
	private static final String UNEXPETEDRETURNSTATEMENT = "Return statement cannot be used in a {0}. It is allowed only in functions and altsteps";
	private static final String ALTSTEPRETURNINGVALUE = "An altstep cannot return a value";
	private static final String USAGEINCONTROLPART = "Return statement cannot be used in the control part. It is alowed only in functions and altsteps";
	private static final String FULLNAMEPART = ".returnexpression";
	private static final String STATEMENT_NAME = "return";

	private final TTCN3Template template;
	private boolean genRestrictionCheck = false;

	public Return_Statement(final TTCN3Template template) {
		this.template = template;

		if (template != null) {
			template.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_RETURN;
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

		if (template == child) {
			return builder.append(FULLNAMEPART);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (template != null) {
			template.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (template != null) {
			template.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isTerminating(final CompilationTimeStamp timestamp) {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;
		genRestrictionCheck = false;

		final Definition definition = myStatementBlock.getMyDefinition();
		if (definition == null) {
			location.reportSemanticError(USAGEINCONTROLPART);
			return;
		}

		switch (definition.getAssignmentType()) {
		case A_FUNCTION:
			if (template != null) {
				template.getLocation().reportSemanticError(UNEXPECTEDRETURNVALUE);
			}
			break;
		case A_FUNCTION_RVAL:
			final Type returnType = ((Def_Function) definition).getType(timestamp);
			if (template == null) {
				location.reportSemanticError(MessageFormat.format(MISSINGVALUE,  returnType.getTypename()));
				break;
			}
			if (!template.isValue(timestamp)) {
				template.getLocation().reportSemanticError(SPECIFICVALUEEXPECTED);
				break;
			}
			// General:
			template.setMyGovernor(returnType);
			final IValue value = template.getValue();
			if (value != null) {
				value.setMyGovernor(returnType);
				returnType.checkThisValueRef(timestamp, value);
				returnType.checkThisValue(timestamp, value, null, new ValueCheckingOptions(Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false, false, true, false, false));
			}

			break;

		case A_FUNCTION_RTEMP:
			if (template == null) {
				location.reportSemanticError(MessageFormat.format(MISSINGTEMPLATE, ((Def_Function) definition).getType(timestamp)
						.getTypename()));
			} else {
				final Type returnType1 = ((Def_Function) definition).getType(timestamp);
				template.setMyGovernor(returnType1);
				final ITTCN3Template temporalTemplate1 = returnType1.checkThisTemplateRef(timestamp, template,Expected_Value_type.EXPECTED_TEMPLATE,null);
				temporalTemplate1.checkThisTemplateGeneric(timestamp, returnType1, true, true, true, true, true, null);
				genRestrictionCheck = TemplateRestriction.check(timestamp, definition, temporalTemplate1, null);
			}
			break;
		case A_ALTSTEP:
			if (template != null) {
				template.getLocation().reportSemanticError(ALTSTEPRETURNINGVALUE);
			}
			break;
		default:
			location.reportSemanticError(MessageFormat.format(UNEXPETEDRETURNSTATEMENT, definition.getAssignmentName()));
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkAllowedInterleave() {
		location.reportSemanticError("Return statement is not allowed within an interleave statement");
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		if (template != null) {
			return null;
		}

		return ReparseUtilities.getAllValidTokenTypes();
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (template != null) {
			template.updateSyntax(reparser, false);
			reparser.updateLocation(template.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (template == null) {
			return;
		}

		template.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (template != null && !template.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		//TODO more nuanced code generation

		final ExpressionStruct expression = new ExpressionStruct();
		expression.expression.append("return ");

		final Definition definition = myStatementBlock.getMyDefinition();

		//No return value:
		if ( template == null) {
			if (definition.getAssignmentType() == Assignment_type.A_ALTSTEP) {
				//inside an altstep
				expression.expression.append("TitanAlt_Status.ALT_YES");
			}

			expression.mergeExpression(source);

			return;
		}

		if(definition.getAssignmentType() == Assignment_type.A_FUNCTION_RVAL && template.isValue(CompilationTimeStamp.getBaseTimestamp())) {
			final IValue value = template.getValue();

			final ExpressionStruct valueExpression = new ExpressionStruct();
			value.generateCodeExpressionMandatory(aData, valueExpression, true);

			expression.preamble.append(valueExpression.preamble);
			expression.expression.append(valueExpression.expression);
			expression.postamble.append(valueExpression.postamble);
		} else {
			final Definition myDefinition = myStatementBlock.getMyDefinition();
			if (myDefinition.getTemplateRestriction() != TemplateRestriction.Restriction_type.TR_NONE
					&& genRestrictionCheck) {
				template.generateCodeExpression(aData, expression, myDefinition.getTemplateRestriction());
			} else {
				template.generateCodeExpression( aData, expression, Restriction_type.TR_NONE );
			}
			//TODO might need conversion
		}

		expression.mergeExpression(source);
	}
}
