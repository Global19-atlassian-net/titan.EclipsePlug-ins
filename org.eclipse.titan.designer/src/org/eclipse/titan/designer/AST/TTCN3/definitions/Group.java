/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IOutlineElement;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.MarkerHandler;
import org.eclipse.titan.designer.AST.NamingConventionHelper;
import org.eclipse.titan.designer.AST.TTCN3.IAppendableSyntax;
import org.eclipse.titan.designer.AST.TTCN3.attributes.MultipleWithAttributes;
import org.eclipse.titan.designer.AST.TTCN3.attributes.WithAttributesPath;
import org.eclipse.titan.designer.editors.T3Doc;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ParserUtilities;
import org.eclipse.titan.designer.parsers.ttcn3parser.ITTCN3ReparseBase;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser.Pr_reparse_IdentifierContext;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser.Pr_reparse_ModuleDefinitionsListContext;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser.Pr_reparser_optionalWithStatementContext;
import org.eclipse.titan.designer.preferences.PreferenceConstants;

/**
 * Class to represent pr_GroupDef nodes.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class Group extends ASTNode implements IOutlineElement, ILocateableNode, IAppendableSyntax {
	/** Definitions in the current group. */
	private final List<Definition> definitions;

	/** Groups in the current group. */
	private final List<Group> groups;

	/**
	 * Vector of the module importations. A wrapper for ArrayList would be
	 * better, but more redundant. It's needed to make a difference between
	 * top level importations and importations in groups.
	 */
	private final List<ImportModule> importedModules;

	/** The friend module list inside this group */
	private final List<FriendModule> friendModules;

	/**
	 * Holds the last time when these definitions were checked or null if
	 * never.
	 */
	private CompilationTimeStamp lastCompilationTimeStamp;

	/**
	 * Holds the last time when the uniqueness of these groups were checked
	 * or null if never.
	 */
	private CompilationTimeStamp lastUniquenessCheckTimeStamp;

	/** The identifier of the group. */
	private Identifier identifier;

	private WithAttributesPath withAttributesPath = null;

	/** The location of the group. */
	private Location location;

	private Location innerLocation;

	private Location commentLocation = null;

	/** The enclosing group of the current one. */
	private Group parentGroup;

	public static final String DUPLICATEGROUPFIRST = "Duplicate group definition with name `{0}'' was first defined here";
	public static final String DUPLICATEGROUPREPEATED = "Duplicate group definition with name `{0}'' was defined here again";
	public static final String GROUPCLASHGROUP = "Group name `{0}'' clashes with a definition";
	public static final String GROUPCLASHDEFINITION = "Definition of `{0}'' is here";

	public Group(final Identifier identifier) {
		this.identifier = identifier;
		definitions = new CopyOnWriteArrayList<Definition>();
		groups = new ArrayList<Group>();
		importedModules = new CopyOnWriteArrayList<ImportModule>();
		friendModules = new CopyOnWriteArrayList<FriendModule>();

	}

	public final CompilationTimeStamp getLastTimeChecked() {
		return lastCompilationTimeStamp;
	}

	@Override
	/** {@inheritDoc} */
	public Identifier getIdentifier() {
		return identifier;
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

	public void setInnerLocation(final Location location) {
		innerLocation = location;
	}

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

	/**
	 * Sets the parent group of the current one.
	 *
	 * @param parentGroup
	 *                the parent group to be set.
	 */
	public void setParentGroup(final Group parentGroup) {
		this.parentGroup = parentGroup;
	}

	/** @return the parent group of the assignment */
	public Group getParentGroup() {
		return parentGroup;
	}

	/** @return the module this group belongs to */
	protected TTCN3Module getModule() {
		Group temp = this;
		while (temp.parentGroup != null) {
			temp = temp.parentGroup;
		}

		return (TTCN3Module) ((Definitions) temp.getNameParent()).getParentScope();
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (final Group group : groups) {
			if (group == child) {
				final Identifier tempIdentifier = group.getIdentifier();
				return builder.append(INamedNode.DOT).append(tempIdentifier.getDisplayName());
			}
		}

		return builder;
	}

	/**
	 * Adds a module importation to the list of module importations. The
	 * parent group of the newly added module importation is set to this
	 * group here. The parent groups of the top level module importations
	 * are initially set to null.
	 *
	 * @param impmod
	 *                The module importation to add.
	 */
	public void addImportedModule(final ImportModule impmod) {
		if (impmod != null && impmod.getIdentifier() != null && impmod.getIdentifier().getLocation() != null) {
			impmod.setParentGroup(this);
			importedModules.add(impmod);
		}
	}

	/**
	 * Adds a friend module to the list of friend modules. The parent group
	 * of the newly added friend module is set to this group. The parent
	 * groups of top level module friendships are set to null.
	 *
	 * @param friendModule
	 *                the friend module to add.
	 * */
	public void addFriendModule(final FriendModule friendModule) {
		if (friendModule != null) {
			friendModule.setParentGroup(this);
			friendModules.add(friendModule);
		}
	}

	/**
	 * Adds a list of definitions to the list of definitions. The scope of
	 * the newly added definitions are set to this scope here.
	 *
	 * @param definitionList
	 *                The definitions to add.
	 */
	public void addDefinitions(final List<Definition> definitionList) {
		if (definitionList != null) {
			final ArrayList<Definition> safeToAdd = new ArrayList<Definition>(definitionList.size());
			for (final Definition definition : definitionList) {
				if (definition != null && definition.getIdentifier() != null && definition.getIdentifier().getLocation() != null) {
					definition.setParentGroup(this);
					safeToAdd.add(definition);
				}
			}
			definitions.addAll(safeToAdd);
		}
	}

	/**
	 * Adds a group to the list of groups.
	 *
	 * @param group
	 *                The group to add to the list of groups.
	 */
	public void addGroup(final Group group) {
		if (group != null && group.getIdentifier() != null && group.getIdentifier().getLocation() != null) {
			groups.add(group);
			group.setFullNameParent(this);
			group.setParentGroup(this);
		}
	}

	public void addDeclaration(final DeclarationCollector declarationCollector) {

		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() == 1 && identifier.getName().equals(subrefs.get(0).getId().getName())) {
			declarationCollector.addDeclaration(this);
		}
	}

	/**
	 * Sets the with attributes for this group if it has any. Also creates
	 * the with attribute path, to store the attributes in.
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
	 * @return the with attribute path element of this group. If it did not
	 *         exist it will be created.
	 * */
	public WithAttributesPath getAttributePath() {
		if (withAttributesPath == null) {
			withAttributesPath = new WithAttributesPath();
		}

		return withAttributesPath;
	}

	/**
	 * Sets the parent path for the with attribute path element of this
	 * group. Also, creates the with attribute path node if it did not exist
	 * before.
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

	/** @return the group definitions and other definitions. */
	@Override
	public Object[] getOutlineChildren() {
		final List<Object> outlineElements = new ArrayList<Object>();
		if (!importedModules.isEmpty()) {
			outlineElements.add(importedModules);
		}

		if (!friendModules.isEmpty()) {
			outlineElements.add(friendModules);
		}

		// Take care of ordering.
		int from = 0;
		for (final Definition definition : definitions) {
			for (int j = from; j < groups.size(); j++) {
				final Group grp = groups.get(j);
				if (definition.getLocation().getLine() >= grp.getLocation().getLine()) {
					outlineElements.add(grp);
					++from;
				}
			}
			outlineElements.add(definition);
		}
		if (from < groups.size()) {
			for (int i = from; i < groups.size(); i++) {
				outlineElements.add(groups.get(i));
			}
		}

		return outlineElements.toArray();
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineText() {
		return "";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "outline_group.gif";
	}

	@Override
	/** {@inheritDoc} */
	public int category() {
		return 0;
	}

	/**
	 * Checks the uniqueness of the definitions and groups.
	 *
	 * @param timestamp
	 *                The timestamp of the actual semantic check cycle.
	 */
	private void checkUniqueness(final CompilationTimeStamp timestamp) {
		if (lastUniquenessCheckTimeStamp != null && !lastUniquenessCheckTimeStamp.isLess(timestamp)) {
			return;
		}

		final Map<String, Definition> definitionMap = new HashMap<String, Definition>(definitions.size());
		final Map<String, Group> groupMap = new HashMap<String, Group>(groups.size());

		for (final Definition def : definitions) {
			final String defName = def.getIdentifier().getName();
			if (!definitionMap.containsKey(defName)) {
				definitionMap.put(defName, def);
			}
		}

		for (final Group group : groups) {
			final String groupName = group.getIdentifier().getName();
			if (groupMap.containsKey(groupName)) {
				groupMap.get(groupName).getIdentifier().getLocation()
				.reportSingularSemanticError(MessageFormat.format(DUPLICATEGROUPFIRST, groupName));
				group.getIdentifier().getLocation().reportSemanticError(MessageFormat.format(DUPLICATEGROUPREPEATED, groupName));
			} else {
				groupMap.put(groupName, group);
			}
			if (definitionMap.containsKey(groupName)) {
				group.getIdentifier().getLocation().reportSemanticError(MessageFormat.format(GROUPCLASHGROUP, groupName));
				definitionMap.get(groupName).getIdentifier().getLocation()
				.reportSingularSemanticError(MessageFormat.format(GROUPCLASHDEFINITION, groupName));
			}
		}

		lastUniquenessCheckTimeStamp = timestamp;
	}


	public void markMarkersForRemoval(final CompilationTimeStamp timestamp){
		if (lastCompilationTimeStamp != null && !lastCompilationTimeStamp.isLess(timestamp)) {
			return;
		}

		//definitions are handled separately!
		MarkerHandler.markAllSemanticMarkersForRemoval(this.getCommentLocation()); //for example t3doc markers
		MarkerHandler.markAllSemanticMarkersForRemoval(this.getIdentifier());
		MarkerHandler.markAllSemanticMarkersForRemoval(this.withAttributesPath);
		for (final Group innerGroup : groups) {
			innerGroup.markMarkersForRemoval(timestamp);
		}
	}

	/**
	 * Checks the whole group for semantic errors.
	 *
	 * @param timestamp
	 *                The timestamp of the actual semantic check cycle.
	 */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastCompilationTimeStamp != null && !lastCompilationTimeStamp.isLess(timestamp)) {
			return;
		}
		lastCompilationTimeStamp = timestamp;

		T3Doc.check(this.getCommentLocation(), "group");

		NamingConventionHelper.checkConvention(PreferenceConstants.REPORTNAMINGCONVENTION_GROUP, identifier, "group");
		NamingConventionHelper.checkNameContents(identifier, getModule().getIdentifier(), "group");

		checkUniqueness(timestamp);

		if (withAttributesPath != null) {
			withAttributesPath.checkGlobalAttributes(timestamp, false);
			withAttributesPath.checkAttributes(timestamp);
		}

		for (int i = 0, size = groups.size(); i < size; i++) {
			groups.get(i).check(timestamp);
		}
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
		if (withAttributesPath == null || withAttributesPath.getAttributes() == null) {
			final List<Integer> result = new ArrayList<Integer>(2);
			result.add(Ttcn3Lexer.PUBLIC);
			result.add(Ttcn3Lexer.PRIVATE);
			return result;
		}

		return new ArrayList<Integer>(0);
	}

	private int reparseIdentifier(final TTCN3ReparseUpdater aReparser) {
		return aReparser.parse(new ITTCN3ReparseBase() {
			@Override
			public void reparse(final Ttcn3Reparser parser) {
				final Pr_reparse_IdentifierContext root = parser.pr_reparse_Identifier();
				ParserUtilities.logParseTree( root, parser );
				identifier = root.identifier;
			}
		});
	}

	private int reparseOptionalWithStatement(final TTCN3ReparseUpdater aReparser) {
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

	private int reparseModuleDefinitionsList(final TTCN3ReparseUpdater aReparser) {
		return aReparser.parse(new ITTCN3ReparseBase() {
			@Override
			public void reparse(final Ttcn3Reparser parser) {
				final List<Definition> allDefinitions = new ArrayList<Definition>();
				final List<Definition> localDefinitions = new ArrayList<Definition>();
				final List<Group> localGroups = new ArrayList<Group>();
				final List<ImportModule> allImports = new ArrayList<ImportModule>();
				final List<ImportModule> localImports = new ArrayList<ImportModule>();
				final List<FriendModule> allFriends = new ArrayList<FriendModule>();
				final List<FriendModule> localFriends = new ArrayList<FriendModule>();

				final TTCN3Module temp = getModule();

				parser.setModule(temp);
				final Pr_reparse_ModuleDefinitionsListContext root =
						parser.pr_reparse_ModuleDefinitionsList( null, allDefinitions, localDefinitions, localGroups, allImports,
								localImports, allFriends, localFriends, null );
				ParserUtilities.logParseTree( root, parser );

				if ( parser.isErrorListEmpty() ) {
					temp.addDefinitions(allDefinitions);
					for (final ImportModule impmod : allImports) {
						temp.addImportedModule(impmod);
					}

					for (final FriendModule friend : allFriends) {
						temp.addFriendModule(friend);
					}

					addDefinitions(localDefinitions);

					for (final ImportModule impmod : localImports) {
						addImportedModule(impmod);
					}

					for (final Group group : localGroups) {
						addGroup(group);
					}

					for (final FriendModule friend : localFriends) {
						addFriendModule(friend);
					}
				}
			}
		});
	}

	/**
	 * Handles the incremental parsing of this list of definitions.
	 *
	 * @param reparser
	 *                the parser doing the incremental parsing.
	 * @param allImportedModules
	 *                the list of module importations found in the same
	 *                module.
	 * @param allDefinitions
	 *                the list of definitions found in the same module.
	 * @param allFriends
	 *                the list of friend module declarations found in the
	 *                same module.
	 * @return in case of processing error the minimum amount of semantic
	 *         levels that must be destroyed to handle the syntactic
	 *         changes, otherwise 0.
	 * */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final List<ImportModule> allImportedModules,
			final List<Definition> allDefinitions, final List<FriendModule> allFriends) throws ReParseException {
		int result = 0;
		Location tempLocation = identifier.getLocation();
		if (reparser.isDamaged(tempLocation)) {
			if (reparser.envelopsDamage(tempLocation) || reparser.isExtending(tempLocation)) {
				reparser.extendDamagedRegion(tempLocation);
				result = reparseIdentifier( reparser );
				if (result != 0) {
					throw new ReParseException(result);
				}
			} else {
				throw new ReParseException();
			}
		} else {
			reparser.updateLocation(tempLocation);
		}

		if (reparser.isDamaged(innerLocation) && !reparser.envelopsDamage(innerLocation)) {
			throw new ReParseException();
		}

		boolean enveloped = false;
		int nofDamaged = 0;
		int leftBoundary = innerLocation.getOffset();
		int rightBoundary = innerLocation.getEndOffset();
		final int damageOffset = reparser.getDamageStart();
		IAppendableSyntax lastAppendableBeforeChange = null;
		IAppendableSyntax lastPrependableBeforeChange = null;

		for (int i = 0, size = groups.size(); i < size && !enveloped; i++) {
			final Group temp = groups.get(i);
			tempLocation = temp.getLocation();
			if (reparser.envelopsDamage(tempLocation)) {
				enveloped = true;
				leftBoundary = tempLocation.getOffset();
				rightBoundary = tempLocation.getEndOffset();
			} else if (reparser.isDamaged(tempLocation)) {
				nofDamaged++;
			} else {
				if (tempLocation.getEndOffset() < damageOffset && tempLocation.getEndOffset() > leftBoundary) {
					leftBoundary = tempLocation.getEndOffset();
					lastAppendableBeforeChange = temp;
				}
				if (tempLocation.getOffset() >= damageOffset && tempLocation.getOffset() < rightBoundary) {
					rightBoundary = tempLocation.getOffset();
					lastPrependableBeforeChange = temp;
				}
			}
		}

		for (int i = 0, size = importedModules.size(); i < size && !enveloped; i++) {
			final ImportModule temp = importedModules.get(i);
			tempLocation = temp.getLocation();
			if (reparser.envelopsDamage(tempLocation)) {
				enveloped = true;
				leftBoundary = tempLocation.getOffset();
				rightBoundary = tempLocation.getEndOffset();
			} else if (reparser.isDamaged(tempLocation)) {
				nofDamaged++;
			} else {
				if (tempLocation.getEndOffset() < damageOffset && tempLocation.getEndOffset() > leftBoundary) {
					leftBoundary = tempLocation.getEndOffset();
					lastAppendableBeforeChange = temp;
				}
				if (tempLocation.getOffset() >= damageOffset && tempLocation.getOffset() < rightBoundary) {
					rightBoundary = tempLocation.getOffset();
					lastPrependableBeforeChange = temp;
				}
			}
		}

		for (int i = 0, size = friendModules.size(); i < size && !enveloped; i++) {
			final FriendModule temp = friendModules.get(i);
			tempLocation = temp.getLocation();
			if (reparser.envelopsDamage(tempLocation)) {
				enveloped = true;
				leftBoundary = tempLocation.getOffset();
				rightBoundary = tempLocation.getEndOffset();
			} else if (reparser.isDamaged(tempLocation)) {
				nofDamaged++;
			} else {
				if (tempLocation.getEndOffset() < damageOffset && tempLocation.getEndOffset() > leftBoundary) {
					leftBoundary = tempLocation.getEndOffset();
					lastAppendableBeforeChange = temp;
				}
				if (tempLocation.getOffset() >= damageOffset && tempLocation.getOffset() < rightBoundary) {
					rightBoundary = tempLocation.getOffset();
					lastPrependableBeforeChange = temp;
				}
			}
		}

		for (int i = 0, size = definitions.size(); i < size && !enveloped; i++) {
			final Definition temp = definitions.get(i);
			tempLocation = temp.getLocation();
			if (reparser.envelopsDamage(tempLocation)) {
				enveloped = true;
				leftBoundary = tempLocation.getOffset();
				rightBoundary = tempLocation.getEndOffset();
			} else if (reparser.isDamaged(tempLocation)) {
				nofDamaged++;
				if (reparser.getDamageStart() == tempLocation.getEndOffset()) {
					lastAppendableBeforeChange = temp;
				} else if (reparser.getDamageEnd() == tempLocation.getOffset()) {
					lastPrependableBeforeChange = temp;
				}
				// reparser.extendDamagedRegion(temp_location);
			} else {
				if (tempLocation.getEndOffset() < damageOffset && tempLocation.getEndOffset() > leftBoundary) {
					leftBoundary = tempLocation.getEndOffset();
					lastAppendableBeforeChange = temp;
				}
				if (tempLocation.getOffset() >= damageOffset && tempLocation.getOffset() < rightBoundary) {
					rightBoundary = tempLocation.getOffset();
					lastPrependableBeforeChange = temp;
				}
			}

			final Location tempCommentLocation = temp.getCommentLocation();
			if (tempCommentLocation != null && reparser.isDamaged(tempCommentLocation)) {
				rightBoundary = tempCommentLocation.getOffset();
				lastPrependableBeforeChange = temp;
			}
		}

		// extend the reparser to the calculated values if the damage
		// was not enveloped
		if (!enveloped && reparser.envelopsDamage(location)) {
			reparser.extendDamagedRegion(leftBoundary, rightBoundary);
		}

		// if there is an element that is right now being extended we
		// should add it to the damaged domain as the extension might be
		// correct
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
			// remove damaged stuff
			removeStuffInRange(reparser, allImportedModules, allDefinitions, friendModules);
		}

		// update what is left
		for (int i = 0; i < groups.size(); i++) {
			final Group temp = groups.get(i);
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				try {
					temp.updateSyntax(reparser, allImportedModules, allDefinitions, friendModules);
				} catch (ReParseException e) {
					if (e.getDepth() == 1) {
						enveloped = false;
						groups.remove(i);
						i--;
						reparser.extendDamagedRegion(tempLocation);
						result = 1;
					} else {
						e.decreaseDepth();
						throw e;
					}
				}
			}
		}

		for (int i = 0; i < importedModules.size(); i++) {
			final ImportModule temp = importedModules.get(i);
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				try {
					temp.updateSyntax(reparser, enveloped && reparser.envelopsDamage(tempLocation));
				} catch (ReParseException e) {
					if (e.getDepth() == 1) {
						enveloped = false;
						importedModules.remove(i);
						i--;
						reparser.extendDamagedRegion(tempLocation);
						result = 1;
					} else {
						e.decreaseDepth();
						throw e;
					}
				}
			}
		}

		for (int i = 0; i < friendModules.size(); i++) {
			final FriendModule temp = friendModules.get(i);
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				try {
					temp.updateSyntax(reparser, enveloped && reparser.envelopsDamage(tempLocation));
				} catch (ReParseException e) {
					if (e.getDepth() == 1) {
						enveloped = false;
						friendModules.remove(i);
						i--;
						reparser.extendDamagedRegion(tempLocation);
						result = 1;
					} else {
						e.decreaseDepth();
						throw e;
					}
				}
			}
		}

		for (final Iterator<Definition> iterator = definitions.iterator(); iterator.hasNext();) {
			final Definition temp = iterator.next();
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				try {
					final boolean isDamaged = enveloped && reparser.envelopsDamage(tempLocation);
					temp.updateSyntax(reparser, isDamaged);
					if (reparser.getNameChanged()) {
						lastUniquenessCheckTimeStamp = null;
						reparser.setNameChanged(false);
					}
					if(isDamaged) {
						temp.checkRoot();
					}
				} catch (ReParseException e) {
					if (e.getDepth() == 1) {
						enveloped = false;
						definitions.remove(temp);
						reparser.extendDamagedRegion(tempLocation);
						result = 1;
					} else {
						e.decreaseDepth();
						throw e;
					}
				}
			}
		}

		if (result == 1) {
			removeStuffInRange(reparser, allImportedModules, allDefinitions, friendModules);
		}

		for (int i = 0, size = groups.size(); i < size; i++) {
			final Group temp = groups.get(i);
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				reparser.updateLocation(tempLocation);
			}
		}

		for (int i = 0, size = importedModules.size(); i < size; i++) {
			final ImportModule temp = importedModules.get(i);
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				reparser.updateLocation(tempLocation);
			}
		}

		for (int i = 0, size = friendModules.size(); i < size; i++) {
			final FriendModule temp = friendModules.get(i);
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				reparser.updateLocation(tempLocation);
			}
		}

		for (final Definition temp : definitions) {
			tempLocation = temp.getLocation();
			if (reparser.isAffected(tempLocation)) {
				reparser.updateLocation(tempLocation);
			}
		}

		if (withAttributesPath != null && reparser.isAffected(withAttributesPath.getLocation())) {
			if (reparser.envelopsDamage(withAttributesPath.getLocation())) {
				reparser.extendDamagedRegion(withAttributesPath.getLocation());
				result = reparseOptionalWithStatement( reparser );

				if (result != 0) {
					throw new ReParseException(result);
				}

				return;
			}

			withAttributesPath.updateSyntax(reparser, reparser.envelopsDamage(withAttributesPath.getLocation()));

			reparser.updateLocation(withAttributesPath.getLocation());
		}

		if (!enveloped && reparser.envelopsDamage(innerLocation)) {
			reparser.extendDamagedRegion(leftBoundary, rightBoundary);
			result = reparseModuleDefinitionsList( reparser );
		}

		reparser.updateLocation(innerLocation);
		if (result > 1) {
			throw new ReParseException(result - 1);
		}

		return;
	}

	/**
	 * Destroy every element trapped inside the damage radius.
	 *
	 * @param reparser
	 *                the parser doing the incremental parsing.
	 * @param allImportedModules
	 *                the list of module importations found in the same
	 *                module.
	 * @param allDefinitions
	 *                the list of definitions found in the same module.
	 * @param allFriends
	 *                the list of friend modules found in the same module.
	 * */
	private void removeStuffInRange(final TTCN3ReparseUpdater reparser, final List<ImportModule> allImportedModules,
			final List<Definition> allDefinitions, final List<FriendModule> allFriends) {
		for (int i = groups.size() - 1; i >= 0; i--) {
			final Group temp = groups.get(i);
			if (reparser.isDamaged(temp.getLocation())) {
				reparser.extendDamagedRegion(temp.getLocation());
				groups.remove(i);
			}
		}

		final ArrayList<ImportModule> importsToBeRemoved = new ArrayList<ImportModule>();
		for (int i = importedModules.size() - 1; i >= 0; i--) {
			final ImportModule temp = importedModules.get(i);
			if (reparser.isDamaged(temp.getLocation())) {
				reparser.extendDamagedRegion(temp.getLocation());
				importsToBeRemoved.add(temp);
			}
		}
		importedModules.removeAll(importsToBeRemoved);

		importsToBeRemoved.clear();
		for (int i = allImportedModules.size() - 1; i >= 0; i--) {
			final ImportModule temp = allImportedModules.get(i);
			if (reparser.isDamaged(temp.getLocation())) {
				reparser.extendDamagedRegion(temp.getLocation());
				importsToBeRemoved.add(temp);
			}
		}
		allImportedModules.removeAll(importsToBeRemoved);

		for (int i = friendModules.size() - 1; i >= 0; i--) {
			final FriendModule temp = friendModules.get(i);
			if (reparser.isDamaged(temp.getLocation())) {
				reparser.extendDamagedRegion(temp.getLocation());
				friendModules.remove(i);
			}
		}

		for (int i = allFriends.size() - 1; i >= 0; i--) {
			final FriendModule temp = allFriends.get(i);
			if (reparser.isDamaged(temp.getLocation())) {
				reparser.extendDamagedRegion(temp.getLocation());
				allFriends.remove(i);
			}
		}

		final ArrayList<Definition> definitionsToBeRemoved = new ArrayList<Definition>();
		for (final Definition temp : definitions) {
			if (reparser.isDamaged(temp.getLocation())) {
				reparser.extendDamagedRegion(temp.getLocation());
				definitionsToBeRemoved.add(temp);
			}
		}
		definitions.removeAll(definitionsToBeRemoved);

		definitionsToBeRemoved.clear();
		for (final Definition temp : allDefinitions) {
			if (reparser.isDamaged(temp.getLocation())) {
				reparser.extendDamagedRegion(temp.getLocation());
				definitionsToBeRemoved.add(temp);
			}
		}
		allDefinitions.removeAll(definitionsToBeRemoved);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (identifier != null && !identifier.accept(v)) {
			return false;
		}
		return true;
	}
}
