/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.types.PortGenerator;
import org.eclipse.titan.designer.AST.TTCN3.types.PortTypeBody;
import org.eclipse.titan.designer.AST.TTCN3.types.PortTypeBody.OperationModes;
import org.eclipse.titan.designer.AST.TTCN3.types.Port_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Signature_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TypeSet;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;

/**
 * @author Kristof Szabados
 * */
public final class Getreply_Statement extends Statement {
	private static final String SIGNATUREEXPECTED = "The type of parameter is `{0}'', which is not a signature";
	private static final String VALUEREDIRECTWITHOUTRETURNTYPE = "Value redirect cannot be used because signature `{0}'' does not have return type";
	private static final String VALUEMATCHWITHOUTRETURNTYPE = "Value match cannot be used becuse signature `{0}'' does not have return type";
	private static final String NONBLOCKINGSIGNATURE = "Operation `{0}'' is not applicable to non-blocking signature `{1}''";
	private static final String ANYPORTWITHPARAMETERREDIRECTION = "Operation `any port.{0}'' cannot have parameter redirection";
	private static final String ANYPORTWITHVALUEREDIRECTION = "Operation `any port.{0}'' cannot have value redirection";
	private static final String ANYPORTWITHVALUEMATCH = "Operation `any port.{0}'' cannot have value match";
	private static final String ANYPORTWITHPARAMETER = "Operation `any port.{0}'' cannot have parameter";
	private static final String SIGNATUREMISSING = "Signature `{0}'' is not present on the outgoing list of port type `{1}''";
	private static final String UNKNOWNSIGNATURETYPE = "Cannot determine the type of the signature";
	private static final String PARAMETERREDIRECTWITHOUTSIGNATURE = "Parameter redirect cannot be used without signature template";
	private static final String VALUEREDIRECTWITHOUTSIGNATURE = "Value redirect cannot be used without signature template";
	private static final String GETREPLYNOTSUPPORTEDONPORT = "Port type `{0}'' does not have any outgoing signatures that support reply";
	private static final String GETREPLYONMESSAGEPORT = "Procedure-based operation `{0}'' is not applicable to a message-based port of type `{1}''";

	private static final String FULLNAMEPART1 = ".portreference";
	private static final String FULLNAMEPART2 = ".parameter";
	private static final String FULLNAMEPART3 = ".valuematch";
	private static final String FULLNAMEPART4 = ".from";
	private static final String FULLNAMEPART5 = ".redirecvalue";
	private static final String FULLNAMEPART6 = ".parameters";
	private static final String FULLNAMEPART7 = ".redirectSender";
	private static final String STATEMENT_NAME = "getreply";

	private final Reference portReference;
	private final TemplateInstance parameter;
	private final TemplateInstance valueMatch;
	private final TemplateInstance fromClause;
	private final Reference redirectValue;
	private final Parameter_Redirect redirectParameter;
	private final Reference redirectSender;

	public Getreply_Statement(final Reference portReference, final TemplateInstance parameter, final TemplateInstance valueMatch,
			final TemplateInstance fromClause, final Reference redirectValue, final Parameter_Redirect redirectParameter,
			final Reference redirectSender) {
		this.portReference = portReference;
		this.parameter = parameter;
		this.valueMatch = valueMatch;
		this.fromClause = fromClause;
		this.redirectValue = redirectValue;
		this.redirectParameter = redirectParameter;
		this.redirectSender = redirectSender;

		if (portReference != null) {
			portReference.setFullNameParent(this);
		}
		if (parameter != null) {
			parameter.setFullNameParent(this);
		}
		if (valueMatch != null) {
			valueMatch.setFullNameParent(this);
		}
		if (fromClause != null) {
			fromClause.setFullNameParent(this);
		}
		if (redirectValue != null) {
			redirectValue.setFullNameParent(this);
		}
		if (redirectParameter != null) {
			redirectParameter.setFullNameParent(this);
		}
		if (redirectSender != null) {
			redirectSender.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_GETREPLY;
	}

	@Override
	/** {@inheritDoc} */
	public String getStatementName() {
		return STATEMENT_NAME;
	}

	public Reference getPortReference() {
		return portReference;
	}

	public TemplateInstance getReceiveParameter() {
		return parameter;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (portReference == child) {
			return builder.append(FULLNAMEPART1);
		} else if (parameter == child) {
			return builder.append(FULLNAMEPART2);
		} else if (valueMatch == child) {
			return builder.append(FULLNAMEPART3);
		} else if (fromClause == child) {
			return builder.append(FULLNAMEPART4);
		} else if (redirectValue == child) {
			return builder.append(FULLNAMEPART5);
		} else if (redirectParameter == child) {
			return builder.append(FULLNAMEPART6);
		} else if (redirectSender == child) {
			return builder.append(FULLNAMEPART7);
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
		if (parameter != null) {
			parameter.setMyScope(scope);
		}
		if (valueMatch != null) {
			valueMatch.setMyScope(scope);
		}
		if (fromClause != null) {
			fromClause.setMyScope(scope);
		}
		if (redirectValue != null) {
			redirectValue.setMyScope(scope);
		}
		if (redirectParameter != null) {
			redirectParameter.setMyScope(scope);
		}
		if (redirectSender != null) {
			redirectSender.setMyScope(scope);
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

		checkGetreply(timestamp, this, "getreply", portReference, parameter, valueMatch, fromClause, redirectValue, redirectParameter,
				redirectSender);

		if (redirectValue != null) {
			redirectValue.setUsedOnLeftHandSide();
		}
		if (redirectSender != null) {
			redirectSender.setUsedOnLeftHandSide();
		}

		lastTimeChecked = timestamp;
	}

	public static void checkGetreply(final CompilationTimeStamp timestamp, final Statement source, final String statementName,
			final Reference portReference, final TemplateInstance parameter, final TemplateInstance valueMatch,
			final TemplateInstance fromClause, final Reference redirectValue, final Parameter_Redirect redirectParameter,
			final Reference redirectSender) {
		final Port_Type portType = Port_Utility.checkPortReference(timestamp, source, portReference);
		if (parameter == null) {
			if (portType != null) {
				final PortTypeBody body = portType.getPortBody();
				if (!body.getreplyAllowed(timestamp)) {
					if (OperationModes.OP_Message.equals(body.getOperationMode())) {
						portReference.getLocation().reportSemanticError(
								MessageFormat.format(GETREPLYONMESSAGEPORT, statementName, portType.getTypename()));
					} else {
						portReference.getLocation().reportSemanticError(
								MessageFormat.format(GETREPLYNOTSUPPORTEDONPORT, portType.getTypename()));
					}
				}
			}

			if (redirectValue != null) {
				redirectValue.getLocation().reportSemanticError(VALUEREDIRECTWITHOUTSIGNATURE);
				Port_Utility.checkValueRedirect(timestamp, redirectValue, null);
			}

			if (redirectParameter != null) {
				redirectParameter.check(timestamp, null, true);//true?
				redirectParameter.getLocation().reportSemanticError(PARAMETERREDIRECTWITHOUTSIGNATURE);
			}
		} else {
			IType signature = null;
			boolean signatureDetermined = false;
			if (portType != null) {
				final PortTypeBody body = portType.getPortBody();
				if (OperationModes.OP_Message.equals(body.getOperationMode())) {
					portReference.getLocation().reportSemanticError(
							MessageFormat.format(GETREPLYONMESSAGEPORT, statementName, portType.getTypename()));
				} else if (body.getreplyAllowed(timestamp)) {
					final TypeSet outSignatures = body.getOutSignatures();

					if (outSignatures.getNofTypes() == 1) {
						signature = outSignatures.getTypeByIndex(0);
					} else {
						signature = Port_Utility.getOutgoingType(timestamp, parameter);

						if (signature == null) {
							parameter.getLocation().reportSemanticError(UNKNOWNSIGNATURETYPE);
						} else {
							if (!outSignatures.hasType(timestamp, signature)) {
								parameter.getLocation().reportSemanticError(
										MessageFormat.format(SIGNATUREMISSING, signature.getTypename(),
												portType.getTypename()));
							}
						}
					}

					signatureDetermined = true;
				} else {
					portReference.getLocation().reportSemanticError(
							MessageFormat.format(GETREPLYNOTSUPPORTEDONPORT, portType.getTypename()));
				}
			} else if (portReference == null) {
				// the statement refers to any port or there was
				// a syntax error
				parameter.getLocation().reportSemanticError(MessageFormat.format(ANYPORTWITHPARAMETER, statementName));
				if (valueMatch != null) {
					valueMatch.getLocation().reportSemanticError(MessageFormat.format(ANYPORTWITHVALUEMATCH, statementName));
				}
				if (redirectValue != null) {
					redirectValue.getLocation().reportSemanticError(
							MessageFormat.format(ANYPORTWITHVALUEREDIRECTION, statementName));
				}
				if (redirectParameter != null) {
					redirectParameter.getLocation().reportSemanticError(
							MessageFormat.format(ANYPORTWITHPARAMETERREDIRECTION, statementName));
				}
			}

			if (!signatureDetermined) {
				signature = Port_Utility.getOutgoingType(timestamp, parameter);
			}

			if (signature != null) {
				parameter.check(timestamp, signature);

				signature = signature.getTypeRefdLast(timestamp);
				Type returnType = null;

				switch (signature.getTypetype()) {
				case TYPE_SIGNATURE: {
					final Signature_Type signatureType = (Signature_Type) signature;
					if (signatureType.isNonblocking()) {
						final String message = MessageFormat.format(NONBLOCKINGSIGNATURE, statementName,
								signatureType.getTypename());
						source.getLocation().reportSemanticError(message);
					} else {
						returnType = signatureType.getSignatureReturnType();
					}

					if (redirectParameter != null) {
						redirectParameter.check(timestamp, signatureType, true);
					}

					if (returnType == null) {
						if (valueMatch != null) {
							valueMatch.getLocation().reportSemanticError(
									MessageFormat.format(VALUEMATCHWITHOUTRETURNTYPE, signature.getTypename()));
						}
						if (redirectValue != null) {
							final String message = MessageFormat.format(VALUEREDIRECTWITHOUTRETURNTYPE,
									signature.getTypename());
							redirectValue.getLocation().reportSemanticError(message);
						}
					}
					break;
				}
				default:
					parameter.getLocation().reportSemanticError(MessageFormat.format(SIGNATUREEXPECTED, signature.getTypename()));
					if (redirectParameter != null) {
						redirectParameter.checkErroneous(timestamp);
					}
					break;
				}

				if (valueMatch != null) {
					if (returnType != null) {
						valueMatch.check(timestamp, returnType);
					}
				}

				Port_Utility.checkValueRedirect(timestamp, redirectValue, returnType);
			}
		}

		Port_Utility.checkFromClause(timestamp, source, portType, fromClause, redirectSender);
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		if (redirectSender != null) {
			return null;
		}

		final List<Integer> result = new ArrayList<Integer>();
		result.add(Ttcn3Lexer.SENDER);

		if (redirectParameter != null) {
			return result;
		}

		result.add(Ttcn3Lexer.PARAM);

		if (redirectValue != null) {
			return result;
		}

		result.add(Ttcn3Lexer.PORTREDIRECTSYMBOL);

		if (fromClause != null) {
			return result;
		}

		result.add(Ttcn3Lexer.FROM);

		if (parameter != null) {
			return result;
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

		if (parameter != null) {
			parameter.updateSyntax(reparser, false);
			reparser.updateLocation(parameter.getLocation());
		}

		if (valueMatch != null) {
			valueMatch.updateSyntax(reparser, false);
			reparser.updateLocation(valueMatch.getLocation());
		}

		if (fromClause != null) {
			fromClause.updateSyntax(reparser, false);
			reparser.updateLocation(fromClause.getLocation());
		}

		if (redirectValue != null) {
			redirectValue.updateSyntax(reparser, false);
			reparser.updateLocation(redirectValue.getLocation());
		}

		if (redirectParameter != null) {
			redirectParameter.updateSyntax(reparser, false);
			reparser.updateLocation(redirectParameter.getLocation());
		}

		if (redirectSender != null) {
			redirectSender.updateSyntax(reparser, false);
			reparser.updateLocation(redirectSender.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (portReference != null) {
			portReference.findReferences(referenceFinder, foundIdentifiers);
		}
		if (parameter != null) {
			parameter.findReferences(referenceFinder, foundIdentifiers);
		}
		if (valueMatch != null) {
			valueMatch.findReferences(referenceFinder, foundIdentifiers);
		}
		if (fromClause != null) {
			fromClause.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirectValue != null) {
			redirectValue.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirectParameter != null) {
			redirectParameter.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirectSender != null) {
			redirectSender.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (portReference != null && !portReference.accept(v)) {
			return false;
		}
		if (parameter != null && !parameter.accept(v)) {
			return false;
		}
		if (valueMatch != null && !valueMatch.accept(v)) {
			return false;
		}
		if (fromClause != null && !fromClause.accept(v)) {
			return false;
		}
		if (redirectValue != null && !redirectValue.accept(v)) {
			return false;
		}
		if (redirectParameter != null && !redirectParameter.accept(v)) {
			return false;
		}
		if (redirectSender != null && !redirectSender.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final StringBuilder source) {
		final ExpressionStruct expression = new ExpressionStruct();
		generateCodeExpression(aData, expression);

		PortGenerator.generateCodeStandalone(aData, source, expression.expression.toString(), getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression) {
		if (portReference != null) {
			portReference.generateCode(aData, expression);
			expression.expression.append(".getreply(");
			if (parameter != null) {
				//FIXME handle redirect
				parameter.generateCode(aData, expression, Restriction_type.TR_NONE);
				final IType signature = parameter.getTemplateBody().getMyGovernor();
				final IType signatureType = signature.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
				final IType returnType = ((Signature_Type) signatureType).getSignatureReturnType();
				if (returnType != null) {
					expression.expression.append(".set_value_template(");
					if (valueMatch != null) {
						valueMatch.generateCode(aData, expression, Restriction_type.TR_NONE);
					} else {
						// the value match is not present
						// we must substitute it with ? in the signature template
						expression.expression.append(MessageFormat.format("new {0}(template_sel.ANY_VALUE)", returnType.getGenNameTemplate(aData, expression.expression, myScope)));
					}
					expression.expression.append(')');
				}
				expression.expression.append(", ");
				generateCodeExprFromclause(aData, expression);
				//FIXME handle redirections
				expression.expression.append(", ");
				if (redirectSender == null) {
					expression.expression.append("null");
				} else {
					redirectSender.generateCode(aData, expression);
				}
			} else {
				// the signature template is not present
				generateCodeExprFromclause(aData, expression);
				expression.expression.append(", ");
				if (redirectSender == null) {
					expression.expression.append("null");
				} else {
					redirectSender.generateCode(aData, expression);
				}
			}
			//FIXME handle index redirection
			expression.expression.append(", null");
		} else {
			// the operation refers to any port
			expression.expression.append("TitanPort.any_getreply(");
			generateCodeExprFromclause(aData, expression);
			expression.expression.append(", ");
			if (redirectSender == null) {
				expression.expression.append("null");
			} else {
				redirectSender.generateCode(aData, expression);
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
			expression.expression.append("TitanComponent_template.any_compref");
		}
	}
}
