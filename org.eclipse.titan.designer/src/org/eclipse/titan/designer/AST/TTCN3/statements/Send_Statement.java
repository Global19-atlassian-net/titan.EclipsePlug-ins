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
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.PortScope;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template.Template_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.SpecificValue_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.types.PortTypeBody;
import org.eclipse.titan.designer.AST.TTCN3.types.PortTypeBody.OperationModes;
import org.eclipse.titan.designer.AST.TTCN3.types.Port_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TypeSet;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;

/**
 * @author Kristof Szabados
 * 
 * */
public final class Send_Statement extends Statement {
	private static final String SENDONPORT = "Message-based operation `send'' is not applicable to a procedure-based port of type `{0}''";
	private static final String UNKNOWNOUTGOINGMESSAGE = "Cannot determine the type of the outgoing message";
	private static final String TYPENOTPRESENT = "Message type `{0}'' is not present on the outgoing list of port type `{1}''";
	private static final String TYPEISAMBIGUOUS = "Type of the message is amiguous:"
			+ " `{0}'' is compatible with more than one outgoing message types of port type `{1}''";
	private static final String NOOUTGOINGMESSAGETYPES = "Port type `{0}'' does not have any outgoing message types";
	private static final String SENDPARAMETERSIGNATURE = "The type of send parameter is signature `{0}'' which cannot be a message type";
	private static final String SENDPARAMETERPORT = "The type of send parameter is port type `{0}'' which can not be a message type";
	private static final String SENDPARAMETERDEFAULT = "The type of send parameter is the `default'' type, which cannot be a message type";

	private static final String FULLNAMEPART1 = ".portreference";
	private static final String FULLNAMEPART2 = ".sendparameter";
	private static final String FULLNAMEPART3 = ".to";
	private static final String STATEMENT_NAME = "send";

	private final Reference portReference;
	private final boolean translate;
	private final TemplateInstance parameter;
	private final IValue toClause;

	public Send_Statement(final Reference portReference, final TemplateInstance parameter, final IValue toClause, final boolean translate) {
		this.portReference = portReference;
		this.translate = translate;
		this.parameter = parameter;
		this.toClause = toClause;

		if (portReference != null) {
			portReference.setFullNameParent(this);
		}
		if (parameter != null) {
			parameter.setFullNameParent(this);
		}
		if (toClause != null) {
			toClause.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_SEND;
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
		} else if (parameter == child) {
			return builder.append(FULLNAMEPART2);
		} else if (toClause == child) {
			return builder.append(FULLNAMEPART3);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (portReference != null && !translate) {
			portReference.setMyScope(scope);
		}
		if (parameter != null) {
			parameter.setMyScope(scope);
		}
		if (toClause != null) {
			toClause.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		Port_Type portType;
		if (translate) {
			PortScope ps = myStatementBlock.getScopePort();
			if (ps != null) {
				portType = ps.getPortType();
			} else {
				getLocation().reportSemanticError("Cannot determine the type of the port: Missing port clause on the function.");
				lastTimeChecked = timestamp;

				return;
			}
		} else {
			portType = Port_Utility.checkPortReference(timestamp, this, portReference, false);
		}

		if (parameter == null) {
			lastTimeChecked = timestamp;
			return;
		}

		IType messageType = null;
		boolean messageTypeDetermined = false;
		if (portType != null) {
			// the port type is known
			portType.check(timestamp);

			final PortTypeBody portTypeBody = portType.getPortBody();
			final TypeSet outMessages = portTypeBody.getOutMessage();
			if (OperationModes.OP_Procedure.equals(portTypeBody.getOperationMode())) {
				portReference.getLocation().reportSemanticError(MessageFormat.format(SENDONPORT, portType.getTypename()));
			} else if (outMessages != null) {
				if (outMessages.getNofTypes() == 1) {
					messageType = outMessages.getTypeByIndex(0);
				} else {
					messageType = Port_Utility.getOutgoingType(timestamp, parameter);
					if (messageType == null) {
						parameter.getLocation().reportSemanticError(UNKNOWNOUTGOINGMESSAGE);
					} else {
						final int nofCompatibleTypes = outMessages.getNofCompatibleTypes(timestamp, messageType);
						if (nofCompatibleTypes == 0) {
							parameter.getLocation().reportSemanticError(
									MessageFormat.format(TYPENOTPRESENT, messageType.getTypename(),
											portType.getTypename()));
						} else if (nofCompatibleTypes > 1) {
							parameter.getLocation().reportSemanticError(
									MessageFormat.format(TYPEISAMBIGUOUS, messageType.getTypename(),
											portType.getTypename()));
						}
					}
				}

				messageTypeDetermined = true;
			} else {
				portReference.getLocation().reportSemanticError(MessageFormat.format(NOOUTGOINGMESSAGETYPES, portType.getTypename()));
			}
		}

		if (!messageTypeDetermined) {
			messageType = Port_Utility.getOutgoingType(timestamp, parameter);
		}

		if (messageType != null) {
			parameter.check(timestamp, messageType);
			messageType = messageType.getTypeRefdLast(timestamp);
			switch (messageType.getTypetype()) {
			case TYPE_SIGNATURE:
				parameter.getLocation().reportSemanticError(MessageFormat.format(SENDPARAMETERSIGNATURE, messageType.getTypename()));
				break;
			case TYPE_PORT:
				parameter.getLocation().reportSemanticError(MessageFormat.format(SENDPARAMETERPORT, messageType.getTypename()));
				break;
			case TYPE_DEFAULT:
				parameter.getLocation().reportSemanticError(MessageFormat.format(SENDPARAMETERDEFAULT, messageType.getTypename()));
				break;
			default:
				break;
			}

			parameter.getTemplateBody().checkSpecificValue(timestamp, false);

			Port_Utility.checkToClause(timestamp, this, portType, toClause);
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		if (toClause != null) {
			return null;
		}

		final List<Integer> result = new ArrayList<Integer>();
		result.add(Ttcn3Lexer.TO);

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

		if (toClause instanceof IIncrementallyUpdateable) {
			((IIncrementallyUpdateable) toClause).updateSyntax(reparser, false);
			reparser.updateLocation(toClause.getLocation());
		} else if (toClause != null) {
			throw new ReParseException();
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
		if (toClause != null) {
			toClause.findReferences(referenceFinder, foundIdentifiers);
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
		if (toClause != null && !toClause.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final StringBuilder source) {
		final ExpressionStruct expression = new ExpressionStruct();

		if (!translate) {
			portReference.generateCode(aData, expression);
			expression.expression.append(".send(");
		} else {
			expression.expression.append("send(");
		}

		final TTCN3Template templateBody = parameter.getTemplateBody();
		if (parameter.getDerivedReference() == null && Template_type.SPECIFIC_VALUE.equals(templateBody.getTemplatetype())
				&& ((SpecificValue_Template) templateBody).isValue(CompilationTimeStamp.getBaseTimestamp())) {
			//optimize for value
			final IValue value = ((SpecificValue_Template) templateBody).getValue();
			//FIXME check if casting is needed
			value.generateCodeExpressionMandatory(aData, expression, true);
		} else {
			//real template, can not be optimized
			parameter.generateCode(aData, expression, Restriction_type.TR_NONE);
		}

		

		if (toClause != null) {
			expression.expression.append(", ");
			toClause.generateCodeExpression(aData, expression, true);
		}
		expression.expression.append(")");
		expression.mergeExpression(source);
	}
}
