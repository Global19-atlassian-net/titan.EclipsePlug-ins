/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.definitions.For_Loop_Definitions;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock.ReturnStatus_type;
import org.eclipse.titan.designer.AST.TTCN3.types.Boolean_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * The For_Statement class represents TTCN3 for statements.
 *
 * @author Kristof Szabados
 * */
public final class For_Statement extends Statement {
	private static final String OPERANDERROR = "The final expression of a for statement should be a boolean value";

	private static final String FULLNAMEPART1 = ".init";
	private static final String FULLNAMEPART2 = ".final";
	private static final String FULLNAMEPART3 = ".step";
	private static final String FULLNAMEPART4 = ".block";
	private static final String STATEMENT_NAME = "for";

	/**
	 * The definitions declared in the initial declaration part of the for
	 * statement.
	 */
	private final For_Loop_Definitions definitions;

	/** The initial assignment. */
	private final Assignment_Statement initialAssignment;

	/** The stop condition. */
	private final Value finalExpression;

	/** The stepping assignment. */
	private final Assignment_Statement stepAssignment;

	/**
	 * the statementblock of the for statement.
	 * <p>
	 * This can be null
	 * */
	private final StatementBlock statementblock;

	public For_Statement(final For_Loop_Definitions definitions, final Value finalExpression, final Assignment_Statement incrementStep,
			final StatementBlock statementblock) {
		this.definitions = definitions;
		this.initialAssignment = null;
		this.finalExpression = finalExpression;
		this.stepAssignment = incrementStep;
		this.statementblock = statementblock;

		init();
	}

	public For_Statement(final Assignment_Statement initialAssignment, final Value finalExpression, final Assignment_Statement incrementStep,
			final StatementBlock statementblock) {
		this.definitions = null;
		this.initialAssignment = initialAssignment;
		this.finalExpression = finalExpression;
		this.stepAssignment = incrementStep;
		this.statementblock = statementblock;

		init();
	}

	private void init() {
		if (definitions != null) {
			definitions.setFullNameParent(this);
		}
		if (initialAssignment != null) {
			initialAssignment.setFullNameParent(this);
		}
		if (finalExpression != null) {
			finalExpression.setFullNameParent(this);
		}
		if (stepAssignment != null) {
			stepAssignment.setFullNameParent(this);
		}
		if (statementblock != null) {
			statementblock.setFullNameParent(this);
			statementblock.setOwnerIsLoop();
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_WHILE;
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

		if (definitions == child) {
			return builder.append(FULLNAMEPART1);
		} else if (initialAssignment == child) {
			return builder.append(FULLNAMEPART1);
		} else if (finalExpression == child) {
			return builder.append(FULLNAMEPART2);
		} else if (stepAssignment == child) {
			return builder.append(FULLNAMEPART3);
		} else if (statementblock == child) {
			return builder.append(FULLNAMEPART4);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (definitions == null) {
			if (initialAssignment != null) {
				initialAssignment.setMyScope(scope);
			}
			if (finalExpression != null) {
				finalExpression.setMyScope(scope);
			}
			if (stepAssignment != null) {
				stepAssignment.setMyScope(scope);
			}
			if (statementblock != null) {
				statementblock.setMyScope(scope);
				scope.addSubScope(statementblock.getLocation(), statementblock);
			}
		} else {
			definitions.setParentScope(scope);
			final Location startLoc = definitions.getLocation();
			Location endLoc = null;
			if (finalExpression != null) {
				finalExpression.setMyScope(definitions);
				endLoc = finalExpression.getLocation();
			}
			if (stepAssignment != null) {
				stepAssignment.setMyScope(definitions);
				endLoc = stepAssignment.getLocation();
			}
			scope.addSubScope((endLoc == null) ? startLoc : Location.interval(startLoc, endLoc), definitions);
			if (statementblock != null) {
				statementblock.setMyScope(definitions);
				scope.addSubScope(statementblock.getLocation(), statementblock);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (definitions == null) {
			if (initialAssignment != null) {
				initialAssignment.setCodeSection(codeSection);
			}
		}
		if (finalExpression != null) {
			finalExpression.setCodeSection(codeSection);
		}
		if (stepAssignment != null) {
			stepAssignment.setCodeSection(codeSection);
		}
		if (statementblock != null) {
			statementblock.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyStatementBlock(final StatementBlock statementBlock, final int index) {
		super.setMyStatementBlock(statementBlock, index);
		if (statementblock != null) {
			statementblock.setMyStatementBlock(statementBlock, index);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyDefinition(final Definition definition) {
		if (statementblock != null) {
			statementblock.setMyDefinition(definition);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyAltguards(final AltGuards altGuards) {
		if (statementblock != null) {
			statementblock.setMyAltguards(altGuards);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StatementBlock.ReturnStatus_type hasReturn(final CompilationTimeStamp timestamp) {
		if (statementblock != null) {
			if (StatementBlock.ReturnStatus_type.RS_NO.equals(statementblock.hasReturn(timestamp))) {
				return StatementBlock.ReturnStatus_type.RS_NO;
			}

			return StatementBlock.ReturnStatus_type.RS_MAYBE;
		}

		return StatementBlock.ReturnStatus_type.RS_NO;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasReceivingStatement() {
		if (statementblock != null) {
			return statementblock.hasReceivingStatement(0);
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		if (definitions != null) {
			definitions.check(timestamp);
		}
		if (initialAssignment != null) {
			initialAssignment.check(timestamp);
		}
		if (finalExpression != null) {
			finalExpression.setLoweridToReference(timestamp);
			final IValue lastValue = finalExpression.getValueRefdLast(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, null);
			final Type_type temp = lastValue.getExpressionReturntype(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);

			switch (temp) {
			case TYPE_BOOL:
				break;
			default:
				location.reportSemanticError(OPERANDERROR);
				finalExpression.setIsErroneous(true);
				break;
			}

			if(finalExpression.getMyGovernor() == null) {
				finalExpression.setMyGovernor(new Boolean_Type());
			}
		}
		if (stepAssignment != null) {
			stepAssignment.check(timestamp);
		}
		if (statementblock != null) {
			statementblock.setMyLaicStmt(null,this);
			statementblock.check(timestamp);
			//warning for "return" has been removed. Not valid problem
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void checkAllowedInterleave() {
		if (statementblock != null) {
			statementblock.checkAllowedInterleave();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void postCheck() {
		if (definitions != null) {
			definitions.postCheck();
		}

		if (statementblock != null) {
			statementblock.postCheck();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			lastTimeChecked = null;
			boolean enveloped = false;

			if (definitions != null) {
				if (enveloped) {
					definitions.updateSyntax(reparser, false);
					reparser.updateLocation(definitions.getLocation());
				} else if (reparser.envelopsDamage(definitions.getLocation())) {
					definitions.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(definitions.getLocation());
				}
			}

			if (initialAssignment != null) {
				if (enveloped) {
					initialAssignment.updateSyntax(reparser, false);
					reparser.updateLocation(initialAssignment.getLocation());
				} else if (reparser.envelopsDamage(initialAssignment.getLocation())) {
					initialAssignment.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(initialAssignment.getLocation());
				}
			}

			if (finalExpression != null) {
				if (enveloped) {
					finalExpression.updateSyntax(reparser, false);
					reparser.updateLocation(finalExpression.getLocation());
				} else if (reparser.envelopsDamage(finalExpression.getLocation())) {
					finalExpression.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(finalExpression.getLocation());
				}
			}

			if (stepAssignment != null) {
				if (enveloped) {
					stepAssignment.updateSyntax(reparser, false);
					reparser.updateLocation(stepAssignment.getLocation());
				} else if (reparser.envelopsDamage(stepAssignment.getLocation())) {
					stepAssignment.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(stepAssignment.getLocation());
				}
			}

			if (statementblock != null) {
				if (enveloped) {
					statementblock.updateSyntax(reparser, false);
					reparser.updateLocation(statementblock.getLocation());
				} else if (reparser.envelopsDamage(statementblock.getLocation())) {
					statementblock.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(statementblock.getLocation());
				}
			}

			if (!enveloped) {
				throw new ReParseException();
			}

			return;
		}

		if (definitions != null) {
			definitions.updateSyntax(reparser, false);
			reparser.updateLocation(definitions.getLocation());
		}

		if (initialAssignment != null) {
			initialAssignment.updateSyntax(reparser, false);
			reparser.updateLocation(initialAssignment.getLocation());
		}

		if (finalExpression != null) {
			finalExpression.updateSyntax(reparser, false);
			reparser.updateLocation(finalExpression.getLocation());
		}

		if (stepAssignment != null) {
			stepAssignment.updateSyntax(reparser, false);
			reparser.updateLocation(stepAssignment.getLocation());
		}

		if (statementblock != null) {
			statementblock.updateSyntax(reparser, false);
			reparser.updateLocation(statementblock.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (definitions != null) {
			definitions.findReferences(referenceFinder, foundIdentifiers);
		}
		if (initialAssignment != null) {
			initialAssignment.findReferences(referenceFinder, foundIdentifiers);
		}
		if (finalExpression != null) {
			finalExpression.findReferences(referenceFinder, foundIdentifiers);
		}
		if (stepAssignment != null) {
			stepAssignment.findReferences(referenceFinder, foundIdentifiers);
		}
		if (statementblock != null) {
			statementblock.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (definitions != null && !definitions.accept(v)) {
			return false;
		}
		if (initialAssignment != null && !initialAssignment.accept(v)) {
			return false;
		}
		if (finalExpression != null && !finalExpression.accept(v)) {
			return false;
		}
		if (stepAssignment != null && !stepAssignment.accept(v)) {
			return false;
		}
		if (statementblock != null && !statementblock.accept(v)) {
			return false;
		}
		return true;
	}

	public Value getFinalExpression() {
		return finalExpression;
	}

	public StatementBlock getStatementBlock() {
		return statementblock;
	}

	public void generateCodeStepAssigment( final JavaGenData aData, final StringBuilder source ) {
		stepAssignment.generateCode(aData, source);
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		source.append("\t\t{\n");
		if(definitions != null) {
			definitions.generateCode(aData, source);
		} else if (initialAssignment != null) {
			initialAssignment.getLocation().update_location_object(aData, source);
			initialAssignment.generateCode(aData, source);
		}
		source.append("\t\t\tfor( ; ; ) {\n");

		getLocation().update_location_object(aData, source);
		final AtomicInteger blockCount = new AtomicInteger(0);
		if (finalExpression.returnsNative()) {
			finalExpression.generateCodeTmp(aData, source, "if (!", blockCount);
			source.append(") {\n");
		} else {
			aData.addBuiltinTypeImport( "TitanBoolean" );

			finalExpression.generateCodeTmp(aData, source, "if (!TitanBoolean.get_native(", blockCount);
			source.append(")) {\n");
		}
		source.append("break;\n");
		source.append("}\n");

		for(int i = 0 ; i < blockCount.get(); i++) {
			source.append("}\n");
		}

		statementblock.generateCode(aData, source);
		//TODO: do not generate stepAssignment if break or continue statement (without condition) occurred.
		//but this stepStatement is necessary if an earlier deeply wrapped continue occurred (???)

		//if the statement block always returns there is no need for code implementing stepAssignment
		if (!ReturnStatus_type.RS_YES.equals(statementblock.hasReturn(CompilationTimeStamp.getBaseTimestamp()))) {
			stepAssignment.getLocation().update_location_object(aData, source);
			stepAssignment.generateCode(aData, source);
		}
		source.append( "\t\t\t}\n" );
		source.append( "\t\t}\n" );
	}
}
