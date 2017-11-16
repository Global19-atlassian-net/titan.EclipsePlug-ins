/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * The If_Statement class represents TTCN3 if statements.
 *
 * @see If_Clauses
 * @see If_Clause
 *
 * @author Kristof Szabados
 * */
public final class If_Statement extends Statement {
	private static final String NEVERREACH = "Control never reaches this code because of previous effective condition(s)";
	private static final String IFWITHOUTELSE = "Conditional operation without else clause";

	private static final String FULLNAMEPART1 = ".ifclauses";
	private static final String FULLNAMEPART2 = ".elseblock";
	private static final String STATEMENT_NAME = "if";

	private final If_Clauses ifClauses;

	/**
	 * this statementblock represents the else clause.
	 * <p>
	 * Null is a valid value.
	 * */
	private final StatementBlock statementblock;

	/** whether to report the problem of not having an else branch */
	private static String reportIfWithoutElse;

	static {
		final IPreferencesService ps = Platform.getPreferencesService();
		if ( ps != null ) {
			reportIfWithoutElse = ps.getString(ProductConstants.PRODUCT_ID_DESIGNER,
					PreferenceConstants.REPORT_IF_WITHOUT_ELSE, GeneralConstants.IGNORE, null);

			final Activator activator = Activator.getDefault();
			if (activator != null) {
				activator.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {

					@Override
					public void propertyChange(final PropertyChangeEvent event) {
						final String property = event.getProperty();
						if (PreferenceConstants.REPORT_IF_WITHOUT_ELSE.equals(property)) {
							reportIfWithoutElse = ps.getString(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.REPORT_IF_WITHOUT_ELSE,
									GeneralConstants.IGNORE, null);
						}
					}
				});
			}
		}
	}

	public If_Statement(final If_Clauses ifClauses, final StatementBlock statementblock) {
		this.ifClauses = ifClauses;
		this.statementblock = statementblock;

		if (ifClauses != null) {
			ifClauses.setFullNameParent(this);
		}
		if (statementblock != null) {
			statementblock.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_IF;
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

		if (ifClauses == child) {
			return builder.append(FULLNAMEPART1);
		} else if (statementblock == child) {
			return builder.append(FULLNAMEPART2);
		}

		return builder;
	}

	public If_Clauses getIfClauses() {
		return ifClauses;
	}

	public StatementBlock getStatementBlock() {
		return statementblock;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (ifClauses != null) {
			ifClauses.setMyScope(scope);
		}
		if (statementblock != null) {
			statementblock.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyStatementBlock(final StatementBlock statementBlock, final int index) {
		super.setMyStatementBlock(statementBlock, index);
		if (ifClauses != null) {
			ifClauses.setMyStatementBlock(statementBlock, index);
		}
		if (statementblock != null) {
			statementblock.setMyStatementBlock(statementBlock, index);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyDefinition(final Definition definition) {
		if (ifClauses != null) {
			ifClauses.setMyDefinition(definition);
		}
		if (statementblock != null) {
			statementblock.setMyDefinition(definition);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyAltguards(final AltGuards altGuards) {
		if (ifClauses != null) {
			ifClauses.setMyAltguards(altGuards);
		}
		if (statementblock != null) {
			statementblock.setMyAltguards(altGuards);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StatementBlock.ReturnStatus_type hasReturn(final CompilationTimeStamp timestamp) {
		if (ifClauses != null) {
			return ifClauses.hasReturn(timestamp, statementblock);
		}

		return StatementBlock.ReturnStatus_type.RS_NO;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasReceivingStatement() {
		boolean result = false;
		if (ifClauses != null) {
			result = ifClauses.hasReceivingStatement();
		}
		if (statementblock != null) {
			result  = result || statementblock.hasReceivingStatement(0);
		}

		return result;
	}

	@Override
	protected void setMyLaicStmt(AltGuards pAltGuards, Statement pLoopStmt) {
		ifClauses.setMyLaicStmt(pAltGuards, pLoopStmt);
	    if (statementblock != null) { //	if (if_stmt.elseblock)
	        statementblock.setMyLaicStmt(pAltGuards, pLoopStmt);
	    }
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		boolean unreachable = false;
		if (ifClauses != null) {
			unreachable = ifClauses.check(timestamp, unreachable);
		}

		if (statementblock != null) {
			if (unreachable) {
				statementblock.getLocation().reportConfigurableSemanticProblem(
						Platform.getPreferencesService().getString(ProductConstants.PRODUCT_ID_DESIGNER,
								PreferenceConstants.REPORTUNNECESSARYCONTROLS, GeneralConstants.WARNING, null),
								NEVERREACH);
			}
			statementblock.check(timestamp);
		} else {
			getLocation().reportConfigurableSemanticProblem(reportIfWithoutElse, IFWITHOUTELSE);
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void checkAllowedInterleave() {
		if (ifClauses != null) {
			ifClauses.checkAllowedInterleave();
		}
		if (statementblock != null) {
			statementblock.checkAllowedInterleave();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void postCheck() {
		if (ifClauses != null) {
			ifClauses.postCheck();
		}
		if (statementblock != null) {
			statementblock.postCheck();
		}
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		if (statementblock != null) {
			return null;
		}

		final List<Integer> result = new ArrayList<Integer>();
		result.add(Ttcn3Lexer.ELSE);

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (ifClauses != null) {
			ifClauses.updateSyntax(reparser, false);
		}

		if (statementblock != null) {
			statementblock.updateSyntax(reparser, false);
			reparser.updateLocation(statementblock.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (ifClauses != null) {
			ifClauses.findReferences(referenceFinder, foundIdentifiers);
		}
		if (statementblock != null) {
			statementblock.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (ifClauses != null && !ifClauses.accept(v)) {
			return false;
		}
		if (statementblock != null && !statementblock.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		//TODO this is just a simplified version to enable early performance testing
		final AtomicInteger blockCount = new AtomicInteger(0);
		final AtomicBoolean unReachable = new AtomicBoolean(false);
		final AtomicBoolean eachFalse = new AtomicBoolean(true);

		ifClauses.generateCode(aData, source, blockCount, unReachable, eachFalse);
		if (statementblock != null && !unReachable.get()) {
			if(!eachFalse.get()) {
				source.append("else ");
			}
			eachFalse.set(false);
			source.append("{\n");
			blockCount.incrementAndGet();
			statementblock.generateCode(aData, source);
		}

		for(int i = 0 ; i < blockCount.get(); i++) {
			source.append("}\n");
		}

		if(eachFalse.get()) {
			source.append("/* never occurs */;\n");
		}
	}
}
