/******************************************************************************
 * Copyright (c) 2000-2016 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.MarkerHandler;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IAppendableSyntax;
import org.eclipse.titan.designer.AST.TTCN3.attributes.MultipleWithAttributes;
import org.eclipse.titan.designer.AST.TTCN3.attributes.WithAttributesPath;
import org.eclipse.titan.designer.AST.TTCN3.statements.Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.SkeletonTemplateProposal;
import org.eclipse.titan.designer.editors.T3Doc;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3CodeSkeletons;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ParserUtilities;
import org.eclipse.titan.designer.parsers.ttcn3parser.ITTCN3ReparseBase;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser.Pr_reparser_optionalWithStatementContext;

/**
 * The ControlPart class represents the control parts of TTCN3 modules.
 * 
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class ControlPart extends Scope implements ILocateableNode, IAppendableSyntax {

	private static final String KIND = "controlpart";

	public static String getKind() {
		return KIND;
	}

	private StatementBlock statementblock;

	private WithAttributesPath withAttributesPath = null;

	private Location location;

	private Location commentLocation = null;

	/**
	 * @return The location of the comment assigned to this definition. Or
	 *         null if none.
	 * */
	@Override
	public Location getCommentLocation() {
		return commentLocation;
	}

	/**
	 * Sets the location of the comment that belongs to this definition.
	 * 
	 * @param commentLocation
	 *                the location of the comment
	 * */
	public void setCommentLocation(final Location commentLocation) {
		this.commentLocation = commentLocation;
	}

	/** the time when this control part was checked the last time. */
	private CompilationTimeStamp lastTimeChecked;

	public ControlPart(final StatementBlock statementblock) {
		setScopeMacroName("control");
		if (statementblock == null) {
			this.statementblock = new StatementBlock();
		} else {
			this.statementblock = statementblock;
			setLocation(statementblock.getLocation());
			addSubScope(statementblock.getLocation(), statementblock);
		}
		this.statementblock.setFullNameParent(this);
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return location;
	}

	/**
	 * Sets the scope of the control part.
	 * 
	 * @param scope
	 *                the scope to be set
	 * */
	public void setMyScope(final Scope scope) {
		parentScope = scope;
		statementblock.setMyScope(this);
	}

	/**
	 * @return the scope of this control part
	 * */
	public Scope getMyScope() {
		return statementblock;
	}

	/**
	 * Sets the with attributes for this control part if it has any. Also
	 * creates the with attribute path, to store the attributes in.
	 * 
	 * @param attributes
	 *                the attribute to be added.
	 * */
	public void setWithAttributes(final MultipleWithAttributes attributes) {
		if (withAttributesPath == null) {
			withAttributesPath = new WithAttributesPath();
		}
		if (attributes != null) {
			withAttributesPath.setWithAttributes(attributes);
		}
	}

	/**
	 * @return the with attribute path element of this control part. If it
	 *         did not exist it will be created.
	 * */
	public WithAttributesPath getAttributePath() {
		if (withAttributesPath == null) {
			withAttributesPath = new WithAttributesPath();
		}

		return withAttributesPath;
	}

	/**
	 * Sets the parent path for the with attribute path element of this
	 * control part. Also, creates the with attribute path node if it did
	 * not exist before.
	 * 
	 * @param parent
	 *                the parent to be set.
	 * */
	public void setAttributeParentPath(final WithAttributesPath parent) {
		if (withAttributesPath == null) {
			withAttributesPath = new WithAttributesPath();
		}

		withAttributesPath.setAttributeParent(parent);
	}

	/**
	 * Does the semantic checking of the control part.
	 * 
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}
		
		MarkerHandler.markAllSemanticMarkersForRemoval(this);
		lastTimeChecked = timestamp;
		
		T3Doc.check(this.getCommentLocation(), KIND);

		statementblock.check(timestamp);

		if (withAttributesPath != null) {
			withAttributesPath.checkGlobalAttributes(timestamp, false);
			withAttributesPath.checkAttributes(timestamp);
		}

		
	}

	/**
	 * Checks the properties of the control part, that can only be checked
	 * after the semantic check was completely run.
	 * */
	public void postCheck() {
		statementblock.postCheck();
	}

	@Override
	public ControlPart getControlPart() {
		return this;
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getAssBySRef(final CompilationTimeStamp timestamp, final Reference reference) {
		return getAssBySRef(timestamp, reference, null);
	}
	
	@Override
	/** {@inheritDoc} */
	public Assignment getAssBySRef(final CompilationTimeStamp timestamp, final Reference reference, final IReferenceChain refChain) {
			return getParentScope().getAssBySRef(timestamp, reference);
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector) {
		for (SkeletonTemplateProposal templateProposal : TTCN3CodeSkeletons.CONTROL_PART_FUNCTIONS) {
			propCollector.addTemplateProposal(templateProposal.getPrefix(), templateProposal.getProposal(),
					TTCN3CodeSkeletons.SKELETON_IMAGE);
		}
		super.addProposal(propCollector);
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		if (withAttributesPath == null || withAttributesPath.getAttributes() == null) {
			final List<Integer> result = new ArrayList<Integer>();
			result.add(Ttcn3Lexer.WITH);
			return result;
		}

		return null;
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossiblePrefixTokens() {
		return new ArrayList<Integer>(0);
	}

	/**
	 * Handles the incremental parsing of this control part.
	 * 
	 * @param reparser
	 *                the parser doing the incremental parsing.
	 * @throws ReParseException
	 *                 if there was an error while refreshing the location
	 *                 information and it could not be solved internally.
	 * */
	public void updateSyntax(final TTCN3ReparseUpdater reparser) throws ReParseException {
		if (reparser.isDamaged(getLocation())) {
			lastTimeChecked = null;
			boolean enveloped = false;

			if (reparser.envelopsDamage(statementblock.getLocation())) {
				statementblock.updateSyntax(reparser, true);
				enveloped = true;
				reparser.updateLocation(statementblock.getLocation());
			} else if (reparser.isDamaged(statementblock.getLocation())) {
				throw new ReParseException();
			} else {
				statementblock.updateSyntax(reparser, false);
				reparser.updateLocation(statementblock.getLocation());
			}

			if (withAttributesPath != null) {
				if (enveloped) {
					withAttributesPath.updateSyntax(reparser, false);
					reparser.updateLocation(withAttributesPath.getLocation());
				} else if (reparser.envelopsDamage(withAttributesPath.getLocation())) {
					reparser.extendDamagedRegion(withAttributesPath.getLocation());
					final int result = reparse( reparser );
					if (result == 0) {
						enveloped = true;
					} else {
						throw new ReParseException();
					}
				} else if (reparser.isDamaged(withAttributesPath.getLocation())) {
					throw new ReParseException();
				} else {
					withAttributesPath.updateSyntax(reparser, false);
					reparser.updateLocation(withAttributesPath.getLocation());
				}
			}

			if (!enveloped) {
				throw new ReParseException();
			}

			return;
		}

		statementblock.updateSyntax(reparser, false);
		reparser.updateLocation(statementblock.getLocation());

		if (withAttributesPath != null) {
			withAttributesPath.updateSyntax(reparser, false);
			reparser.updateLocation(withAttributesPath.getLocation());
		}
	}
	
	private int reparse( final TTCN3ReparseUpdater aReparser ) {
		return aReparser.parse(new ITTCN3ReparseBase() {
			@Override
			public void reparse(final Ttcn3Reparser parser) {
				final Pr_reparser_optionalWithStatementContext root = parser.pr_reparser_optionalWithStatement();
				ParserUtilities.logParseTree( root, parser );
				final MultipleWithAttributes attributes = root.attributes;

				final ParseTree rootEof = parser.pr_EndOfFile();
				ParserUtilities.logParseTree( rootEof, parser );
				if ( parser.isErrorListEmpty() ) {
					withAttributesPath.setWithAttributes(attributes);
					if (attributes != null) {
						getLocation().setEndOffset(attributes.getLocation().getEndOffset());
					}
				}
			}
		});
	}

	@Override
	public Assignment getEnclosingAssignment(final int offset) {
		if (statementblock == null) {
			return null;
		}
		return statementblock.getEnclosingAssignment(offset);
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (statementblock != null) {
			statementblock.findReferences(referenceFinder, foundIdentifiers);
		}
		if (withAttributesPath != null) {
			withAttributesPath.findReferences(referenceFinder, foundIdentifiers);
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
		if (statementblock != null) {
			if (!statementblock.accept(v)) {
				return false;
			}
		}
		if (withAttributesPath != null) {
			if (!withAttributesPath.accept(v)) {
				return false;
			}
		}
		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}

	/**
	 * Add generated java code on this level.
	 * @param aData the generated java code with other info
	 */
	public void generateJava( final JavaGenData aData ) {
		final StringBuilder sb = aData.getSrc();
		sb.append( "\tpublic static void main( String[] args ) {\n" );
		sb.append( "//TODO this is only temporal implementation!\n" );
		final int size = statementblock.getSize();
		for ( int i = 0; i < size; i++ ) {
			final Statement statement = statementblock.getStatementByIndex( i );
			statement.generateJava( aData );
		}
		sb.append( "\t}\n" );
	}
}
