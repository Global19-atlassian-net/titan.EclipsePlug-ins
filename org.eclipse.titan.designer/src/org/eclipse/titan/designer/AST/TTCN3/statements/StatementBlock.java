/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.Assignments;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IAppendableSyntax;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.TTCN3Scope;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Altstep;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Testcase;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.statements.Statement.Statement_type;
import org.eclipse.titan.designer.AST.TTCN3.types.Component_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.SkeletonTemplateProposal;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3CodeSkeletons;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3Keywords;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ParserUtilities;
import org.eclipse.titan.designer.parsers.ttcn3parser.ITTCN3ReparseBase;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser.Pr_reparse_FunctionStatementOrDefListContext;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * The StatementBlock class represents TTCN3 statement block (the scope unit).
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class StatementBlock extends TTCN3Scope implements ILocateableNode, IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".statement_";
	private static final String INFINITELOOP = "Inifinite loop detected: the program can not escape from this goto statement";
	public static final String HIDINGSCOPEELEMENT = "Definition with identifier `{0}'' is not unique in the scope hierarchy";
	public static final String HIDDENSCOPEELEMENT = "Previous definition with identifier `{0}'' in higher scope unit is here";
	public static final String HIDINGMODULEIDENTIFIER = "Definition with name `{0}'' hides a module identifier";
	private static final String NEVER_REACH = "Control never reaches this statement";
	private static final String UNUSEDLABEL = "Label `{0}'' is defined, but not used";
	private static final String DUPLICATEDLABELFIRST = "Previous definition of label `{0}'' is here";
	private static final String DUPLICATELABELAGAIN = "Duplicated label `{0}''";

	private static final String EMPTY_STATEMENT_BLOCK = "Empty statement block";
	private static final String TOOMANYSTATEMENTS = "More than {0} statements in a single statementblock";

	public enum ReturnStatus_type {
		/** the block does not have a return statement */
		RS_NO,
		/**
		 * some branches of embedded statements have, some does not have
		 * return
		 */
		RS_MAYBE,
		/**
		 * the block or all branches of embedded statements have a
		 * return statement
		 */
		RS_YES
	}

	private Location location = NULL_Location.INSTANCE;

	private List<Statement> statements;

	/** The definitions stored in the scope. */
	private Map<String, Definition> definitionMap;

	/** The labels stored in the scope. */
	private Map<String, Label_Statement> labelMap;

	/** the statementblock in which this statement block resides. */
	private StatementBlock myStatementBlock;
	private int myStatementBlockIndex;

	/** the definition containing this statement block. */
	private Definition myDefinition;

	/** the time when this statement block was check the last time. */
	private CompilationTimeStamp lastTimeChecked;

	/** Indicates if it is a statement block of a loop. */
	private boolean ownerIsLoop;

	/**
	 * Indicates if it is a statement block of an AltGuard (in alt,
	 * interleave, altstep, call).
	 */
	private boolean ownerIsAltguard;

	/**
	 * Caches whether this function has return statement or not. Used to
	 * speed up computation.
	 **/
	private ReturnStatus_type returnStatus;

	private static final Comparator<Statement> STATEMENT_INSERTION_COMPARATOR = new Comparator<Statement>() {

		@Override
		public int compare(final Statement o1, final Statement o2) {
			return o1.getLocation().getOffset() - o2.getLocation().getOffset();
		}

	};

	/** whether to report the problem of an empty statement block */
	private static String reportEmptyStatementBlock;
	/** whether to report the problem of having too many parameters or not */
	private static String reportTooManyStatements;
	/** the amount that counts to be too many */
	private static int reportTooManyStatementsSize;

	static {
		final IPreferencesService ps = Platform.getPreferencesService();
		if ( ps != null ) {
			reportEmptyStatementBlock = ps.getString(ProductConstants.PRODUCT_ID_DESIGNER,
					PreferenceConstants.REPORT_EMPTY_STATEMENT_BLOCK, GeneralConstants.WARNING, null);
			reportTooManyStatements = ps.getString(ProductConstants.PRODUCT_ID_DESIGNER,
					PreferenceConstants.REPORT_TOOMANY_STATEMENTS, GeneralConstants.WARNING, null);
			reportTooManyStatementsSize = ps.getInt(ProductConstants.PRODUCT_ID_DESIGNER,
					PreferenceConstants.REPORT_TOOMANY_STATEMENTS_SIZE, 150, null);

			final Activator activator = Activator.getDefault();
			if (activator != null) {
				activator.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
					@Override
					public void propertyChange(final PropertyChangeEvent event) {
						final String property = event.getProperty();
						if (PreferenceConstants.REPORT_EMPTY_STATEMENT_BLOCK.equals(property)) {
							reportEmptyStatementBlock = ps.getString(ProductConstants.PRODUCT_ID_DESIGNER,
									PreferenceConstants.REPORT_EMPTY_STATEMENT_BLOCK, GeneralConstants.WARNING, null);
						} else if (PreferenceConstants.REPORT_TOOMANY_STATEMENTS.equals(property)) {
							reportTooManyStatements = ps.getString(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.REPORT_TOOMANY_STATEMENTS,
									GeneralConstants.WARNING, null);
						} else if (PreferenceConstants.REPORT_TOOMANY_STATEMENTS_SIZE.equals(property)) {
							reportTooManyStatementsSize = ps.getInt(ProductConstants.PRODUCT_ID_DESIGNER,
									PreferenceConstants.REPORT_TOOMANY_STATEMENTS_SIZE, 150, null);
						}
					}
				});
			}
		}
	}

	public StatementBlock() {
		scopeName = "statementblock";
		statements = new ArrayList<Statement>();
		ownerIsLoop = false;
		ownerIsAltguard = false;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0, size = statements.size(); i < size; i++) {
			if (statements.get(i) == child) {
				return builder.append(FULLNAMEPART).append(Integer.toString(i + 1));
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return location;
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		this.location = location;
	}

	public Definition getMyDefinition() {
		return myDefinition;
	}

	/**
	 * Sets the definition in which this statement block resides.
	 *
	 * @param definition
	 *                the definition to be set.
	 * */
	public void setMyDefinition(final Definition definition) {
		myDefinition = definition;
		for (int i = 0, size = statements.size(); i < size; i++) {
			statements.get(i).setMyDefinition(definition);
		}
	}

	/**
	 * Sets the scope in which this statementblock resides.
	 * <p>
	 * The scope to be set will become the parent scope of scope of this
	 * statementblock.
	 *
	 * @param scope
	 *                the scope to be set.
	 * */
	public void setMyScope(final Scope scope) {
		setParentScope(scope);
		if (location != null) {
			scope.addSubScope(location, this);
		}
		for (int i = 0, size = statements.size(); i < size; i++) {
			statements.get(i).setMyScope(this);
		}
	}

	/**
	 * Sets the code_section attribute for the statements in this block to the provided value.
	 *
	 * @param codeSection the code section where these statements should be generated.
	 * */
	public void setCodeSection(final CodeSectionType codeSection) {
		for (int i = 0, size = statements.size(); i < size; i++) {
			statements.get(i).setCodeSection(codeSection);
		}
	}

	/**
	 * Sets indication that it is a statement block of a loop.
	 * */
	public void setOwnerIsLoop() {
		ownerIsLoop = true;
	}

	/**
	 * Sets indication that it is a statement block of an altguard.
	 * */
	public void setOwnerIsAltguard() {
		ownerIsAltguard = true;
	}

	/**
	 * Adds a statement to the list of statements stored in this statement
	 * block.
	 * <p>
	 * Statements of null value are not added to keep the semantic checks at
	 * a relatively low complexity (such statements are syntactically
	 * erroneous)
	 * <p>
	 * The modification happens during initial parsing.
	 *
	 * @param statement
	 *                the statement to be added.
	 * */
	public void addStatement(final Statement statement) {
		addStatement(statement, true);
	}

	/**
	 * Adds a statement to the list of statements stored in this statement
	 * block.
	 * <p>
	 * Statements of null value are not added to keep the semantic checks at
	 * a relatively low complexity (such statements are syntactically
	 * erroneous)
	 * <p>
	 * The modification happens during initial parsing.
	 *
	 * @param statement
	 *                the statement to be added.
	 * @param append should the new statement added to the end of the list or to front.
	 * */
	public void addStatement(final Statement statement, final boolean append) {
		if (statement != null) {
			if (append) {
				statements.add(statement);
			} else {
				// add to front
				statements.add(0, statement);
			}
			statement.setMyStatementBlock(this, statements.size() - 1);
			statement.setMyScope(this);
			statement.setFullNameParent(this);
		}
	}

	/**
	 * Adds a list of new statements into the actual list of statement in an
	 * ordered fashion.
	 *
	 * @param newStatements
	 *                the new list of statements to be merged with the
	 *                original.
	 * */
	void addStatementsOrdered(final List<Statement> newStatements) {
		if (newStatements == null || newStatements.isEmpty()) {
			return;
		}

		final List<Statement> localStatements = new ArrayList<Statement>(this.statements);
		Statement statement;
		for (int i = 0, size = newStatements.size(); i < size; i++) {
			statement = newStatements.get(i);

			final int position = Collections.binarySearch(localStatements, statement, STATEMENT_INSERTION_COMPARATOR);

			if (position < 0) {
				localStatements.add((position + 1) * -1, statement);
			} else {
				localStatements.add(position + 1, statement);
			}

			statement.setMyScope(this);
			statement.setFullNameParent(this);
			statement.setMyDefinition(myDefinition);
		}
		// refresh indices
		for (int i = 0, size = localStatements.size(); i < size; i++) {
			statement = localStatements.get(i);

			statement.setMyStatementBlock(this, i);
		}

		statements = localStatements;
	}

	/**
	 * @return the number of statements in this statement block.
	 * */
	public int getSize() {
		return statements.size();
	}

	public Statement getStatementByIndex(final int i) {
		return statements.get(i);
	}

	public Statement getFirstStatement() {
		for (int i = 0, size = statements.size(); i < size; i++) {
			final Statement statement = statements.get(i);
			switch (statement.getType()) {
			case S_LABEL:
				// skip this statement
				break;
			case S_BLOCK: {
				final Statement firstStatement = ((StatementBlock_Statement) statement).getStatementBlock().getFirstStatement();
				if (firstStatement != null) {
					return firstStatement;
				}
				break;
			}
			case S_DOWHILE: {
				final Statement firstStatement = ((DoWhile_Statement) statement).getStatementBlock().getFirstStatement();
				if (firstStatement != null) {
					return firstStatement;
				}
				break;
			}
			default:
				return statement;
			}
		}

		return null;
	}

	/** @return the parent statement block */
	public StatementBlock getMyStatementBlock() {
		return myStatementBlock;
	}

	/**
	 * Sets the statementblock in which this statement was found.
	 *
	 * @param statementBlock
	 *                the statementblock containing this statement.
	 * @param index
	 *                the index of this statement in the statement block.
	 * */
	public void setMyStatementBlock(final StatementBlock statementBlock, final int index) {
		myStatementBlock = statementBlock;
		myStatementBlockIndex = index;
		for (int i = 0, size = statements.size(); i < size; i++) {
			statements.get(i).setMyStatementBlock(this, i);
		}
	}

	/**
	 * @return the index of this statement block in its parent statement
	 *         block
	 */
	public int getMyStatementBlockIndex() {
		return myStatementBlockIndex;
	}

	public void setMyAltguards(final AltGuards altGuards) {
		for (int i = 0, size = statements.size(); i < size; i++) {
			statements.get(i).setMyAltguards(altGuards);
		}
	}

	/**
	 * Used to tell break and continue statements if they are located with an altstep, a loop or none.
	 *
	 * @param pAltGuards the altguards set only within altguards
	 * @param pLoopStmt the loop statement, set only within loops.
	 * */
	protected void setMyLaicStmt(final AltGuards pAltGuards, final Statement pLoopStmt) {
		for(final Statement statment : statements) {
			statment.setMyLaicStmt(pAltGuards, pLoopStmt);
		}
	}

	/**
	 * Checks whether the statementblock has a return statement, either
	 * directly or embedded.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 *
	 * @return the return status of the statement block.
	 * */
	public ReturnStatus_type hasReturn(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && returnStatus != null) {
			return returnStatus;
		}

		returnStatus = ReturnStatus_type.RS_NO;

		for (int i = 0, size = statements.size(); i < size; i++) {
			Statement statement = statements.get(i);
			if (Statement_type.S_GOTO.equals(statement.getType())) {
				final Goto_statement gotoStatement = (Goto_statement) statement;
				if (gotoStatement.getJumpsForward()) {
					// heuristics without deep analysis of
					// the control flow graph:
					// skip over the next statements until a
					// (used) label is found
					// the behavior will be sound (i.e. no
					// false errors will be reported)
					for (i++; i < size; i++) {
						statement = statements.get(i);
						if (statement instanceof Label_Statement && ((Label_Statement) statement).labelIsUsed()) {
							break;
						}
					}
				} else {
					if (ReturnStatus_type.RS_NO.equals(returnStatus)) {
						statement.getLocation().reportConfigurableSemanticProblem(
								Platform.getPreferencesService().getString(ProductConstants.PRODUCT_ID_DESIGNER,
										PreferenceConstants.REPORTINFINITELOOPS, GeneralConstants.WARNING,
										null), INFINITELOOP);
					}

					return returnStatus;
				}
			}

			switch (statement.hasReturn(timestamp)) {
			case RS_YES:
				returnStatus = ReturnStatus_type.RS_YES;
				return returnStatus;
			case RS_MAYBE:
				returnStatus = ReturnStatus_type.RS_MAYBE;
				break;
			default:
				break;
			}
		}

		return returnStatus;
	}

	/**
	 * Used when generating code for interleaved statement.
	 * If the block has no receiving statements, then the general code generation can be used
	 *  (which may use blocks).
	 *
	 *  @param index the index to start checking from
	 * */
	public boolean hasReceivingStatement(final int index) {
		for (int i = index; i < statements.size(); i++) {
			if (statements.get(i).hasReceivingStatement()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Registers a definition (for example new variable) into the list of
	 * definitions available in this statement block.
	 *
	 * Please note, that this is done while the semantic check is happening,
	 * as it must not be allowed to reach definitions not yet defined.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param definition
	 *                the definition to register.
	 * */
	public void registerDefinition(final CompilationTimeStamp timestamp, final Definition definition) {
		if (definition == null) {
			return;
		}

		final Identifier identifier = definition.getIdentifier();
		if (identifier == null) {
			return;
		}

		if (definitionMap == null) {
			definitionMap = new HashMap<String, Definition>(3);
		}

		final String definitionName = identifier.getName();
		if (definitionMap.containsKey(definitionName)) {
			if (definition.getLocation() != null && definitionMap.get(definitionName).getLocation() != null) {
				final Location otherLocation = definitionMap.get(definitionName).getLocation();
				otherLocation.reportSingularSemanticError(MessageFormat.format(Assignments.DUPLICATEDEFINITIONFIRST,
						identifier.getDisplayName()));
				definition.getLocation().reportSemanticError(
						MessageFormat.format(Assignments.DUPLICATEDEFINITIONREPEATED, identifier.getDisplayName()));
			}
		} else {
			definitionMap.put(definitionName, definition);
			if (parentScope != null && definition.getLocation() != null) {
				if (parentScope.hasAssignmentWithId(timestamp, identifier)) {
					definition.getLocation().reportSemanticError(
							MessageFormat.format(HIDINGSCOPEELEMENT, identifier.getDisplayName()));

					final List<ISubReference> subReferences = new ArrayList<ISubReference>();
					subReferences.add(new FieldSubReference(identifier));
					final Reference reference = new Reference(null, subReferences);
					final Assignment assignment = parentScope.getAssBySRef(timestamp, reference);
					if (assignment != null && assignment.getLocation() != null) {
						assignment.getLocation().reportSingularSemanticError(
								MessageFormat.format(HIDDENSCOPEELEMENT, identifier.getDisplayName()));
					}
				} else if (parentScope.isValidModuleId(identifier)) {
					definition.getLocation().reportSemanticWarning(
							MessageFormat.format(HIDINGMODULEIDENTIFIER, identifier.getDisplayName()));
				}
			}
		}
	}

	/**
	 * Does the semantic checking of the statement block.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		if (definitionMap != null) {
			definitionMap.clear();
		}
		if (labelMap != null) {
			labelMap.clear();
		}

		checkLabels(timestamp);

		boolean unreachableFound = false;
		Statement previousStatement = null;
		for (int i = 0, size = statements.size(); i < size; i++) {
			final Statement statement = statements.get(i);
			try {
				statement.check(timestamp);
			} catch (Exception e) {
				final Location loc = statement.getLocation();
				ErrorReporter.logExceptionStackTrace("An exception was thrown when analyzing the statement in file '"
						+ loc.getFile().getLocationURI() + "' at line " + loc.getLine(), e);
			}

			if (!unreachableFound && !Statement_type.S_LABEL.equals(statement.getType()) && previousStatement != null
					&& ReturnStatus_type.RS_YES.equals(previousStatement.hasReturn(timestamp))) {
				// a statement is unreachable if:
				// - it is not a label (i.e. goto cannot jump to
				// it)
				// - it is not the first statement of the block
				// - the previous statement terminates the
				// control flow
				statement.getLocation().reportSemanticWarning(NEVER_REACH);
				unreachableFound = true;
			}
			// check try-catch statement block usage
			previousStatement = statement;
		}

		if (statements.isEmpty()) {
			getLocation().reportConfigurableSemanticProblem(reportEmptyStatementBlock, EMPTY_STATEMENT_BLOCK);
		} else if (statements.size() > reportTooManyStatementsSize) {
			getLocation().reportConfigurableSemanticProblem(reportTooManyStatements,
					MessageFormat.format(TOOMANYSTATEMENTS, reportTooManyStatementsSize));
		}

		checkUnusedLabels(timestamp);

		//lastTimeChecked = timestamp;
	}

	/**
	 * Checks the properties of the statement block, that can only be
	 * checked after the semantic check was completely run.
	 * */
	public void postCheck() {
		if (statements.isEmpty()) {
			return;
		}

		for (int i = 0, size = statements.size(); i < size; i++) {
			statements.get(i).postCheck();
		}
	}

	/**
	 * Pre-check the labels for duplicates and also sets them unused.
	 *
	 * @param timestamp
	 *                the actual semantic check cycle
	 * */
	private void checkLabels(final CompilationTimeStamp timestamp) {
		for (int i = 0, size = statements.size(); i < size; i++) {
			final Statement statement = statements.get(i);
			if (Statement_type.S_LABEL.equals(statement.getType())) {
				final Label_Statement labelStatement = (Label_Statement) statement;
				labelStatement.setUsed(false);
				final Identifier identifier = labelStatement.getLabelIdentifier();
				if (hasLabel(identifier)) {
					statement.getLocation().reportSemanticError(
							MessageFormat.format(DUPLICATELABELAGAIN, identifier.getDisplayName()));
					final Statement statement2 = getLabel(identifier);
					statement2.getLocation().reportSemanticError(
							MessageFormat.format(DUPLICATEDLABELFIRST, identifier.getDisplayName()));
				} else {
					if (labelMap == null) {
						labelMap = new HashMap<String, Label_Statement>(1);
					}
					labelMap.put(identifier.getName(), labelStatement);
				}
			}
		}
	}

	/**
	 * Post-checks the label for ones that were not used.
	 *
	 * @param timestamp
	 *                the actual semantic check cycle
	 * */
	private void checkUnusedLabels(final CompilationTimeStamp timestamp) {
		for (int i = 0, size = statements.size(); i < size; i++) {
			final Statement statement = statements.get(i);
			if (Statement_type.S_LABEL.equals(statement.getType())) {
				final Label_Statement labelStatement = (Label_Statement) statement;
				if (!labelStatement.labelIsUsed()) {
					statement.getLocation().reportSemanticError(
							MessageFormat.format(UNUSEDLABEL, labelStatement.getLabelIdentifier().getDisplayName()));
				}
			}
		}
	}

	/**
	 * Checks if some statements are allowed in an interleave or not
	 * */
	public void checkAllowedInterleave() {
		for (int i = 0, size = statements.size(); i < size; i++) {
			statements.get(i).checkAllowedInterleave();
		}
	}

	/**
	 * Checks if this statement block or any of its parents has a label
	 * declared with a name.
	 *
	 * @param identifier
	 *                the identifier of the label to search for
	 *
	 * @return true if a label with the given name exists, false otherwise
	 * */
	protected boolean hasLabel(final Identifier identifier) {
		for (StatementBlock statementBlock = this; statementBlock != null; statementBlock = statementBlock.myStatementBlock) {
			if (statementBlock.labelMap != null && statementBlock.labelMap.containsKey(identifier.getName())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if this statement block or any of its parents has a label
	 * declared with a name.
	 *
	 * @param identifier
	 *                the identifier of the label to search for
	 *
	 * @return the {@link Label_Statement} having the provided name, or null
	 *         if none was found.
	 * */
	protected Label_Statement getLabel(final Identifier identifier) {
		for (StatementBlock statementBlock = this; statementBlock != null; statementBlock = statementBlock.myStatementBlock) {
			if (statementBlock.labelMap != null && statementBlock.labelMap.containsKey(identifier.getName())) {
				return statementBlock.labelMap.get(identifier.getName());
			}
		}

		return null;
	}

	/**
	 * @return indication if the statement block is enclosed by a loop.
	 * */
	public boolean hasEnclosingLoop() {
		return ownerIsLoop || (myStatementBlock != null && myStatementBlock.hasEnclosingLoop());
	}

	/**
	 * @return indication if the statement block is enclosed by an altguard.
	 * */
	public boolean hasEnclosingLoopOrAltguard() {
		return ownerIsLoop || ownerIsAltguard || (myStatementBlock != null && myStatementBlock.hasEnclosingLoopOrAltguard());
	}

	@Override
	/** {@inheritDoc} */
	public StatementBlock getStatementBlockScope() {
		return this;
	}

	@Override
	/** {@inheritDoc} */
	public Component_Type getMtcSystemComponentType(final CompilationTimeStamp timestamp, final boolean isSystem) {
		if (myDefinition == null) {
			return null;
		}

		if (Assignment_type.A_TESTCASE.semanticallyEquals(myDefinition.getAssignmentType())) {
			final Def_Testcase testcase = (Def_Testcase) myDefinition;
			if (isSystem) {
				final Component_Type type = testcase.getSystemType(timestamp);
				if (type != null) {
					return type;
				}
				// if the system clause is not set the type of the
				// `system' is the same as the type of the `mtc'
			}

			return testcase.getRunsOnType(timestamp);

		} else if (Assignment_type.A_FUNCTION.semanticallyEquals(myDefinition.getAssignmentType())) {
			if (isSystem) {
				return ((Def_Function) myDefinition).getSystemType(timestamp);
			} else {
				return ((Def_Function) myDefinition).getMtcType(timestamp);
			}
		} else if (Assignment_type.A_ALTSTEP.semanticallyEquals(myDefinition.getAssignmentType())) {
			if (isSystem) {
				return ((Def_Altstep) myDefinition).getSystemType(timestamp);
			} else {
				return ((Def_Altstep) myDefinition).getMTCType(timestamp);
			}
		}
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasAssignmentWithId(final CompilationTimeStamp timestamp, final Identifier identifier) {
		if (definitionMap != null && definitionMap.containsKey(identifier.getName())) {
			return true;
		}
		if (parentScope != null) {
			return parentScope.hasAssignmentWithId(timestamp, identifier);
		}
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getAssBySRef(final CompilationTimeStamp timestamp, final Reference reference) {
		return getAssBySRef(timestamp, reference, null);
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getAssBySRef(final CompilationTimeStamp timestamp, final Reference reference, final IReferenceChain refChain) {
		if (reference.getModuleIdentifier() != null || definitionMap == null) {
			return getParentScope().getAssBySRef(timestamp, reference);
		}

		final Assignment assignment = definitionMap.get(reference.getId().getName());
		if (assignment != null) {
			return assignment;
		}

		return getParentScope().getAssBySRef(timestamp, reference);
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector) {
		if (definitionMap != null && propCollector.getReference().getModuleIdentifier() == null) {
			final HashMap<String, Definition> temp = new HashMap<String, Definition>(definitionMap);
			for (final Definition definition : temp.values()) {
				definition.addProposal(propCollector, 0);
			}
		}
		if (labelMap != null && propCollector.getReference().getModuleIdentifier() == null) {
			final HashMap<String, Label_Statement> temp = new HashMap<String, Label_Statement>(labelMap);
			for (final String name : temp.keySet()) {
				propCollector.addProposal(name, name, null);
			}
		}

		super.addProposal(propCollector);
	}

	@Override
	/** {@inheritDoc} */
	public void addSkeletonProposal(final ProposalCollector propCollector) {
		for (final SkeletonTemplateProposal templateProposal : TTCN3CodeSkeletons.STATEMENT_LEVEL_SKELETON_PROPOSALS) {
			propCollector.addTemplateProposal(templateProposal.getPrefix(), templateProposal.getProposal(),
					TTCN3CodeSkeletons.SKELETON_IMAGE);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addKeywordProposal(final ProposalCollector propCollector) {
		propCollector.addProposal(TTCN3Keywords.STATEMENT_SCOPE, null, TTCN3Keywords.KEYWORD);
		super.addKeywordProposal(propCollector);
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector) {
		if (definitionMap != null && declarationCollector.getReference().getModuleIdentifier() == null) {
			final String name = declarationCollector.getReference().getId().getName();
			if (definitionMap.containsKey(name)) {
				declarationCollector.addDeclaration(name, definitionMap.get(name).getLocation(), this);
			}
		}
		if (labelMap != null && declarationCollector.getReference().getModuleIdentifier() == null) {
			final String name = declarationCollector.getReference().getId().getName();
			if (labelMap.containsKey(name)) {
				declarationCollector.addDeclaration(name, labelMap.get(name).getLocation(), this);
			}
		}
		super.addDeclaration(declarationCollector);
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (!isDamaged) {
			// handle the simple case quickly
			for (int i = 0, size = statements.size(); i < size; i++) {
				final Statement statement = statements.get(i);

				statement.updateSyntax(reparser, false);
				reparser.updateLocation(statement.getLocation());
			}

			return;
		}

		returnStatus = null;
		lastTimeChecked = null;
		boolean enveloped = false;
		int nofDamaged = 0;
		int leftBoundary = location.getOffset();
		int rightBoundary = location.getEndOffset();
		final int damageOffset = reparser.getDamageStart();
		IAppendableSyntax lastAppendableBeforeChange = null;
		IAppendableSyntax lastPrependableBeforeChange = null;

		for (int i = 0, size = statements.size(); i < size && !enveloped; i++) {
			final Statement statement = statements.get(i);
			final Location temporalLocation = statement.getLocation();
			Location cumulativeLocation;
			if(statement instanceof Definition_Statement) {
				cumulativeLocation = ((Definition_Statement) statement).getDefinition().getCumulativeDefinitionLocation();
			} else {
				cumulativeLocation = temporalLocation;
			}

			if (temporalLocation.equals(cumulativeLocation) && reparser.envelopsDamage(cumulativeLocation)) {
				enveloped = true;
				leftBoundary = cumulativeLocation.getOffset();
				rightBoundary = cumulativeLocation.getEndOffset();
			} else if (reparser.isDamaged(cumulativeLocation)) {
				nofDamaged++;
				if (reparser.getDamageStart() == cumulativeLocation.getEndOffset()) {
					lastAppendableBeforeChange = statement;
				} else if (reparser.getDamageEnd() == cumulativeLocation.getOffset()) {
					lastPrependableBeforeChange = statement;
				}
			} else {
				if (cumulativeLocation.getEndOffset() < damageOffset && cumulativeLocation.getEndOffset() > leftBoundary) {
					leftBoundary = cumulativeLocation.getEndOffset();
					lastAppendableBeforeChange = statement;
				}
				if (cumulativeLocation.getOffset() >= damageOffset && cumulativeLocation.getOffset() < rightBoundary) {
					rightBoundary = cumulativeLocation.getOffset();
					lastPrependableBeforeChange = statement;
				}
			}
		}

		// extend the reparser to the calculated values if the damage
		// was not enveloped
		if (!enveloped) {
			reparser.extendDamagedRegion(leftBoundary, rightBoundary);
		}

		// if there is a component field that is right now being
		// extended we should add it to the damaged domain as the
		// extension might be correct
		if (lastAppendableBeforeChange != null) {
			final boolean isBeingExtended = reparser.startsWithFollow(lastAppendableBeforeChange.getPossibleExtensionStarterTokens());
			if (isBeingExtended) {
				leftBoundary = lastAppendableBeforeChange.getLocation().getOffset();
				nofDamaged++;
				enveloped = false;
				reparser.extendDamagedRegion(leftBoundary, rightBoundary);
			}
		}

		if (lastPrependableBeforeChange != null) {
			final List<Integer> temp = lastPrependableBeforeChange.getPossiblePrefixTokens();

			if (temp != null && reparser.endsWithToken(temp)) {
				rightBoundary = lastPrependableBeforeChange.getLocation().getEndOffset();
				nofDamaged++;
				enveloped = false;
				reparser.extendDamagedRegion(leftBoundary, rightBoundary);
			}
		}

		if (nofDamaged != 0) {
			removeStuffInRange(reparser);
		}

		final List<Statement> tempList = new ArrayList<Statement>(statements);
		boolean modified = false;
		for (final Iterator<Statement> iterator = tempList.iterator(); iterator.hasNext();) {
			final Statement statement = iterator.next();
			final Location temporalLocation = statement.getLocation();
			Location cumulativeLocation;
			if(statement instanceof Definition_Statement) {
				cumulativeLocation = ((Definition_Statement) statement).getDefinition().getCumulativeDefinitionLocation();
			} else {
				cumulativeLocation = temporalLocation;
			}

			if (reparser.isAffectedAppended(cumulativeLocation)) {
				try {
					statement.updateSyntax(reparser, enveloped && reparser.envelopsDamage(cumulativeLocation));
					reparser.updateLocation(statement.getLocation());
				} catch (ReParseException e) {
					if (e.getDepth() == 1) {
						enveloped = false;
						iterator.remove();
						modified = true;
						reparser.extendDamagedRegion(cumulativeLocation);
					} else {
						e.decreaseDepth();
						throw e;
					}
				}
			}
		}

		if (modified) {
			statements = tempList;
		}

		if (!enveloped) {
			reparser.extendDamagedRegion(leftBoundary, rightBoundary);
			final int result = reparse( reparser );
			if (result > 1) {
				throw new ReParseException(result - 1);
			}
		}
	}

	private int reparse( final TTCN3ReparseUpdater aReparser ) {
		return aReparser.parse(new ITTCN3ReparseBase() {
			@Override
			public void reparse(final Ttcn3Reparser parser) {
				final Pr_reparse_FunctionStatementOrDefListContext root = parser.pr_reparse_FunctionStatementOrDefList();
				ParserUtilities.logParseTree( root, parser );
				final List<Statement> statements = root.statements;
				if ( parser.isErrorListEmpty() ) {
					if (statements != null) {
						addStatementsOrdered(statements);
					}
				}
			}
		});
	}

	private void removeStuffInRange(final TTCN3ReparseUpdater reparser) {
		final List<Statement> tempList = new ArrayList<Statement>(statements);
		boolean modified = false;
		for (int i = tempList.size() - 1; i >= 0; i--) {
			final Statement statement = tempList.get(i);
			Location cumulativeLocation;
			if(statement instanceof Definition_Statement) {
				cumulativeLocation = ((Definition_Statement) statement).getDefinition().getCumulativeDefinitionLocation();
			} else {
				cumulativeLocation = statement.getLocation();
			}
			if (reparser.isDamaged(cumulativeLocation)) {
				reparser.extendDamagedRegion(cumulativeLocation);
				tempList.remove(i);
				modified = true;
			}
		}

		if (modified) {
			statements = tempList;
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getEnclosingAssignment(final int offset) {
		if (definitionMap == null) {
			return null;
		}
		for (final Definition definition : definitionMap.values()) {
			if (definition.getLocation().containsOffset(offset)) {
				return definition;
			}
		}
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (statements == null) {
			return;
		}

		final List<Statement> tempList = new ArrayList<Statement>(statements);
		for (final Statement statement : tempList) {
			statement.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean accept(final ASTVisitor v) {
		switch (v.visit(this)) {
		case ASTVisitor.V_ABORT:
			return false;
		case ASTVisitor.V_SKIP:
			return true;
		}
		if (statements != null) {
			for (final Statement statement : statements) {
				if (!statement.accept(v)) {
					return false;
				}
			}
		}

		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}

	public boolean isEmpty() {
		return statements.isEmpty();
	}

	/**
	 * Add generated java code on this level.
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		final int size = statements.size();
		for ( int i = 0; i < size; i++ ) {
			final Statement statement = statements.get(i);
			statement.getLocation().update_location_object(aData, source);
			statement.generateCode( aData, source );

			//FIXME: perhaps it would be better to remove this and handle the situation in semantic check (???)
			switch (statement.getType()) {
			case S_BREAK:
			case S_CONTINUE:
			case S_RETURN:
				return;
			default:
				break;//only the switch
			}

		}
	}
}
