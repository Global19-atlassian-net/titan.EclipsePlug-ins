/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.types.PortGenerator;
import org.eclipse.titan.designer.AST.TTCN3.types.Signature_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;

/**
 * @author Kristof Szabados
 * */
public final class Check_Catch_Statement extends Statement {
	private static final String FULLNAMEPART1 = ".portreference";
	private static final String FULLNAMEPART2 = ".signaturereference";
	private static final String FULLNAMEPART3 = ".parameter";
	private static final String FULLNAMEPART4 = ".from";
	private static final String FULLNAMEPART5 = ".redirectValue";
	private static final String FULLNAMEPART6 = ".redirectSender";
	private static final String FULLNAMEPART7 = ".redirectIndex";
	private static final String FULLNAMEPART8 = ".redirectTimestamp";
	private static final String STATEMENT_NAME = "check-catch";

	private final Reference portReference;
	private final boolean anyfrom;
	private final Reference signatureReference;
	private final TemplateInstance parameter;
	private final boolean timeout;
	private final TemplateInstance fromClause;
	private final Value_Redirection redirectValue;
	private final Reference redirectSender;
	private final Reference redirectIndex;
	private final Reference redirectTimestamp;

	// calculated field
	private Signature_Type signature;

	public Check_Catch_Statement(final Reference portReference, final boolean anyFrom, final Reference signatureReference, final TemplateInstance parameter,
			final boolean timeout, final TemplateInstance fromClause, final Value_Redirection redirectValue, final Reference redirectSender,
			final Reference redirectIndex, final Reference redirectTimestamp) {
		this.portReference = portReference;
		this.anyfrom = anyFrom;
		this.signatureReference = signatureReference;
		this.parameter = parameter;
		this.timeout = timeout;
		this.fromClause = fromClause;
		this.redirectValue = redirectValue;
		this.redirectSender = redirectSender;
		this.redirectIndex = redirectIndex;
		this.redirectTimestamp = redirectTimestamp;

		if (portReference != null) {
			portReference.setFullNameParent(this);
		}
		if (signatureReference != null) {
			signatureReference.setFullNameParent(this);
		}
		if (parameter != null) {
			parameter.setFullNameParent(this);
		}
		if (fromClause != null) {
			fromClause.setFullNameParent(this);
		}
		if (redirectValue != null) {
			redirectValue.setFullNameParent(this);
		}
		if (redirectSender != null) {
			redirectSender.setFullNameParent(this);
		}
		if (redirectIndex != null) {
			redirectIndex.setFullNameParent(this);
		}
		if (redirectTimestamp != null) {
			redirectTimestamp.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_CHECK_CATCH;
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

		if (portReference == child) {
			return builder.append(FULLNAMEPART1);
		} else if (signatureReference == child) {
			return builder.append(FULLNAMEPART2);
		} else if (parameter == child) {
			return builder.append(FULLNAMEPART3);
		} else if (fromClause == child) {
			return builder.append(FULLNAMEPART4);
		} else if (redirectValue == child) {
			return builder.append(FULLNAMEPART5);
		} else if (redirectSender == child) {
			return builder.append(FULLNAMEPART6);
		} else if (redirectIndex == child) {
			return builder.append(FULLNAMEPART7);
		} else if (redirectTimestamp == child) {
			return builder.append(FULLNAMEPART8);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (portReference != null) {
			portReference.setMyScope(scope);
		}
		if (signatureReference != null) {
			signatureReference.setMyScope(scope);
		}
		if (parameter != null) {
			parameter.setMyScope(scope);
		}
		if (fromClause != null) {
			fromClause.setMyScope(scope);
		}
		if (redirectValue != null) {
			redirectValue.setMyScope(scope);
		}
		if (redirectSender != null) {
			redirectSender.setMyScope(scope);
		}
		if (redirectIndex != null) {
			redirectIndex.setMyScope(scope);
		}
		if (redirectTimestamp != null) {
			redirectTimestamp.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (portReference != null) {
			portReference.setCodeSection(codeSection);
		}
		if (signatureReference != null) {
			signatureReference.setCodeSection(codeSection);
		}
		if (parameter != null) {
			parameter.setCodeSection(codeSection);
		}
		if (fromClause != null) {
			fromClause.setCodeSection(codeSection);
		}
		if (redirectValue != null) {
			redirectValue.setCodeSection(codeSection);
		}
		if (redirectSender != null) {
			redirectSender.setCodeSection(codeSection);
		}
		if (redirectIndex != null) {
			redirectIndex.setCodeSection(codeSection);
		}
		if (redirectTimestamp != null) {
			redirectTimestamp.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasReceivingStatement() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canRepeat() {
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		if (signatureReference != null) {
			signature = Port_Utility.checkSignatureReference(timestamp, signatureReference);
		}

		Catch_Statement.checkCatch(timestamp, this, "check-catch", portReference, anyfrom, signatureReference, parameter, timeout, fromClause,
				redirectValue, redirectSender, redirectIndex, redirectTimestamp);

		if (redirectValue != null) {
			//redirectValue.setUsedOnLeftHandSide();
		}
		if (redirectSender != null) {
			redirectSender.setUsedOnLeftHandSide();
		}
		if (redirectIndex != null) {
			redirectIndex.setUsedOnLeftHandSide();
		}
		if (redirectTimestamp != null) {
			redirectTimestamp.setUsedOnLeftHandSide();
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		if (redirectSender != null) {
			return null;
		}

		final List<Integer> result = new ArrayList<Integer>();
		result.add(Ttcn3Lexer.SENDER);

		if (redirectValue != null) {
			return result;
		}

		result.add(Ttcn3Lexer.PORTREDIRECTSYMBOL);

		if (fromClause != null) {
			return result;
		}

		result.add(Ttcn3Lexer.FROM);

		if (signatureReference == null || !timeout) {
			result.add(Ttcn3Lexer.LPAREN);
		}

		result.add(Ttcn3Lexer.LPAREN);

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (portReference != null) {
			portReference.updateSyntax(reparser, false);
			reparser.updateLocation(portReference.getLocation());
		}

		if (signatureReference != null) {
			signatureReference.updateSyntax(reparser, false);
			reparser.updateLocation(signatureReference.getLocation());
		}

		if (parameter != null) {
			parameter.updateSyntax(reparser, false);
			reparser.updateLocation(parameter.getLocation());
		}

		if (fromClause != null) {
			fromClause.updateSyntax(reparser, false);
			reparser.updateLocation(fromClause.getLocation());
		}

		if (redirectValue != null) {
			redirectValue.updateSyntax(reparser, false);
			reparser.updateLocation(redirectValue.getLocation());
		}

		if (redirectSender != null) {
			redirectSender.updateSyntax(reparser, false);
			reparser.updateLocation(redirectSender.getLocation());
		}

		if (redirectIndex != null) {
			redirectIndex.updateSyntax(reparser, false);
			reparser.updateLocation(redirectIndex.getLocation());
		}

		if (redirectTimestamp != null) {
			redirectTimestamp.updateSyntax(reparser, false);
			reparser.updateLocation(redirectTimestamp.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (portReference != null) {
			portReference.findReferences(referenceFinder, foundIdentifiers);
		}
		if (signatureReference != null) {
			signatureReference.findReferences(referenceFinder, foundIdentifiers);
		}
		if (parameter != null) {
			parameter.findReferences(referenceFinder, foundIdentifiers);
		}
		if (fromClause != null) {
			fromClause.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirectValue != null) {
			redirectValue.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirectSender != null) {
			redirectSender.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirectIndex != null) {
			redirectIndex.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirectTimestamp != null) {
			redirectTimestamp.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (portReference != null && !portReference.accept(v)) {
			return false;
		}
		if (signatureReference != null && !signatureReference.accept(v)) {
			return false;
		}
		if (parameter != null && !parameter.accept(v)) {
			return false;
		}
		if (fromClause != null && !fromClause.accept(v)) {
			return false;
		}
		if (redirectValue != null && !redirectValue.accept(v)) {
			return false;
		}
		if (redirectSender != null && !redirectSender.accept(v)) {
			return false;
		}
		if (redirectIndex != null && !redirectIndex.accept(v)) {
			return false;
		}
		if (redirectTimestamp != null && !redirectTimestamp.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final StringBuilder source) {
		final ExpressionStruct expression = new ExpressionStruct();
		generateCodeExpression(aData, expression, null);

		PortGenerator.generateCodeStandalone(aData, source, expression.expression.toString(), getStatementName(), canRepeat(), getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression, final String callTimer) {
		if (portReference != null) {
			// the operation refers to a specific port
			if (timeout && callTimer != null) {
				// the operation catches the timeout exception
				expression.expression.append(MessageFormat.format("{0}.timeout()", callTimer));
				return;
			}
			portReference.generateCode(aData, expression);
			expression.expression.append(".check_catch(");
			if (signatureReference != null) {
				// the signature reference and the exception template is present
				expression.expression.append(MessageFormat.format("new {0}_exception_template(", signature.getGenNameValue(aData, expression.expression, myScope)));
				final boolean hasDecodedRedirect = redirectValue != null && redirectValue.has_decoded_modifier();
				parameter.generateCode(aData, expression, Restriction_type.TR_NONE, hasDecodedRedirect);
				expression.expression.append(", ");
				if (redirectValue == null) {
					expression.expression.append("null");
				} else {
					redirectValue.generateCode(aData, expression, parameter);
				}
				expression.expression.append("), ");
			}
		} else {
			// the operation refers to any port
			expression.expression.append("TitanPort.any_check_catch(");
		}

		generateCodeExprFromclause(aData, expression);
		expression.expression.append(", ");
		if (redirectSender == null) {
			expression.expression.append("null");
		} else {
			redirectSender.generateCode(aData, expression);
		}

		expression.expression.append(", ");
		if (redirectTimestamp == null) {
			expression.expression.append("null");
		}else {
			redirectTimestamp.generateCode(aData, expression);
		}

		if (portReference != null) {
			expression.expression.append(", ");
			if (redirectIndex == null) {
				expression.expression.append("null");
			} else {
				generateCodeIndexRedirect(aData, expression, redirectIndex, getMyScope());
			}
		}
		expression.expression.append(')');
	}

	/**
	 * helper to generate the from part.
	 *
	 * originally generate_code_expr_fromclause
	 * */
	private void generateCodeExprFromclause(final JavaGenData aData, final ExpressionStruct expression) {
		if (fromClause != null) {
			fromClause.generateCode(aData, expression, Restriction_type.TR_NONE);
			//FIXME handle redirect
		} else {
			// neither from clause nor sender redirect is present
			// the operation cannot refer to address type
			aData.addBuiltinTypeImport("TitanComponent_template");
			expression.expression.append("TitanComponent_template.any_compref");
		}
	}
}
