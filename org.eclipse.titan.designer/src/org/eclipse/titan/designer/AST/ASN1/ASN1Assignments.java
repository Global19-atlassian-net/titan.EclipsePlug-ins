/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignments;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.definitions.SpecialASN1Module;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.core.LoadBalancingUtilities;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Class to store assignments. It also contains the ASN-related pre-defined
 * (so-called "useful") stuff.
 *
 * @author Kristof Szabados
 */
public final class ASN1Assignments extends Assignments implements ILocateableNode {
	public static final String RESERVEDIDENTIFIER = "`{0}'' is a reserved identifier";

	private HashMap<String, ASN1Assignment> assignmentMap;
	private final List<ASN1Assignment> assignments;
	private final List<ASN1Assignment> dynamic_assignments;

	/**
	 * Holds the last time when these assignments were checked, or null if
	 * never.
	 */
	private CompilationTimeStamp lastCompilationTimeStamp;
	private CompilationTimeStamp lastUniqueNessCheckTimeStamp;

	private Location location = NULL_Location.INSTANCE;

	public ASN1Assignments() {
		assignments = new ArrayList<ASN1Assignment>();
		dynamic_assignments = new ArrayList<ASN1Assignment>();
	}

	/**
	 * Sets the scope of the right hand side of the assignments.
	 *
	 * @param right_scope
	 *                the scope to be set.
	 * */
	public void setRightScope(final Scope rightScope) {
		for (final ASN1Assignment assignment : assignments) {
			assignment.setRightScope(rightScope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (final Assignment assignment : assignments) {
			if (assignment == child) {
				return builder.append(INamedNode.DOT).append(assignment.getIdentifier().getDisplayName());
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Object[] getOutlineChildren() {
		return assignments.toArray();
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "asn.gif";
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

	@Override
	/** {@inheritDoc} */
	public boolean hasLocalAssignmentWithID(final CompilationTimeStamp timestamp, final Identifier identifier) {
		if (null == lastUniqueNessCheckTimeStamp) {
			checkUniqueness(timestamp);
		}

		if (assignmentMap.containsKey(identifier.getName())) {
			return true;
		}

		final Assignments temp = SpecialASN1Module.getSpecialModule().getAssignments();
		if (!this.equals(temp)) {
			return temp.hasAssignmentWithId(timestamp, identifier);
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getLocalAssignmentByID(final CompilationTimeStamp timestamp, final Identifier identifier) {
		if (null == lastUniqueNessCheckTimeStamp) {
			checkUniqueness(timestamp);
		}

		final String name = identifier.getName();
		if (assignmentMap.containsKey(name)) {
			final Assignment temp = assignmentMap.get(name);
			if (temp instanceof Undefined_Assignment) {
				final ASN1Assignment real = ((Undefined_Assignment) temp).getRealAssignment(timestamp);
				if (null != real) {
					return real;
				}
			}

			return temp;
		}

		final Assignments temp = SpecialASN1Module.getSpecialModule().getAssignments();
		if (!this.equals(temp)) {
			return temp.getLocalAssignmentByID(timestamp, identifier);
		}

		return null;
	}

	@Override
	/** {@inheritDoc} */
	public int getNofAssignments() {
		return assignments.size() + dynamic_assignments.size();
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getAssignmentByIndex(final int i) {
		if (i < assignments.size()) {
			return assignments.get(i);
		}

		return dynamic_assignments.get(i - assignments.size());
	}

	/**
	 * Adds an assignment to the list of assignments.
	 * <p>
	 * The scope of the newly added assignment is set to this scope scope
	 * here.
	 *
	 * @param assignment
	 *                the assignment to be added
	 * */
	public void addAssignment(final Assignment assignment) {
		if (null != assignment && null != assignment.getIdentifier() && null != assignment.getLocation()
				&& (assignment instanceof ASN1Assignment)) {
			assignments.add((ASN1Assignment) assignment);
			assignment.setMyScope(this);
			assignment.setFullNameParent(this);
		}
	}

	/**
	 * Adds a dynamic assignment to the list of assignments.
	 * <p>
	 * Only assignments which were created dynamically during the semantic
	 * check shall be added with this function. The scope of the newly added
	 * assignment is set to this scope scope here.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param assignment
	 *                the assignment to be added
	 * */
	public void addDynamicAssignment(final CompilationTimeStamp timestamp, final ASN1Assignment assignment) {
		if (null == assignment || null == assignment.getIdentifier()) {
			return;
		}

		if (null == lastUniqueNessCheckTimeStamp) {
			checkUniqueness(timestamp);
		}

		dynamic_assignments.add(assignment);
		assignment.setMyScope(this);

		final Identifier identifier = assignment.getIdentifier();
		final String assignmentName = identifier.getName();
		final Assignments specialAssignments = SpecialASN1Module.getSpecialModule().getAssignments();
		if (specialAssignments.hasAssignmentWithId(timestamp, identifier)) {
			final Location tempLocation = assignment.getIdentifier().getLocation();
			tempLocation.reportSemanticError(MessageFormat.format(RESERVEDIDENTIFIER, identifier.getDisplayName()));
		} else if (assignmentMap.containsKey(assignmentName)) {
			final Location otherLocation = assignmentMap.get(assignmentName).getIdentifier().getLocation();
			otherLocation.reportSingularSemanticError(MessageFormat.format(DUPLICATEDEFINITIONFIRST, identifier.getDisplayName()));
			final Location selfLocation = assignment.getIdentifier().getLocation();
			selfLocation.reportSemanticError(MessageFormat.format(DUPLICATEDEFINITIONREPEATED, identifier.getDisplayName()));
		} else {
			assignmentMap.put(assignmentName, assignment);
		}
	}

	/**
	 * Removes a previously registered dynamically created assignment.
	 *
	 * @param assignment the assignment to be removed.
	 * */
	public void removeDynamicAssignment(final ASN1Assignment assignment) {
		if (assignment == null) {
			return;
		}

		dynamic_assignments.remove(assignment);
	}

	/**
	 * Checks the uniqueness of the definitions, and also builds a hashmap
	 * of them to speed up further searches.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * */
	public void checkUniqueness(final CompilationTimeStamp timestamp) {
		if (null != lastUniqueNessCheckTimeStamp && !lastUniqueNessCheckTimeStamp.isLess(timestamp)) {
			return;
		}

		if (null == assignmentMap) {
			assignmentMap = new HashMap<String, ASN1Assignment>(assignments.size());
		}

		lastUniqueNessCheckTimeStamp = timestamp;

		assignmentMap.clear();

		final Assignments specialAssignments = SpecialASN1Module.getSpecialModule().getAssignments();
		for (final ASN1Assignment assignment : assignments) {
			final Identifier identifier = assignment.getIdentifier();
			final String assignmentName = identifier.getName();
			if (specialAssignments.hasAssignmentWithId(timestamp, identifier)) {
				final Location selfLocation = assignment.getIdentifier().getLocation();
				selfLocation.reportSemanticError(MessageFormat.format(RESERVEDIDENTIFIER, identifier.getDisplayName()));
			} else if (assignmentMap.containsKey(assignmentName)) {
				final Location otherLocation = assignmentMap.get(assignmentName).getIdentifier().getLocation();
				otherLocation.reportSingularSemanticError(MessageFormat.format(DUPLICATEDEFINITIONFIRST, identifier.getDisplayName()));
				final Location selfLocation = assignment.getIdentifier().getLocation();
				selfLocation.reportSemanticError(MessageFormat.format(DUPLICATEDEFINITIONREPEATED, identifier.getDisplayName()));
			} else {
				assignmentMap.put(assignmentName, assignment);
			}
		}

		checkSimilarTypeNames();
	}

	/**
	 * Checks and marks type assignments inside this assignment list, whose name might cause problems during code generation.
	 * Aka. the names only differ in the capitality of their letters.
	 * */
	private void checkSimilarTypeNames() {
		final HashMap<String, Type_Assignment> similarityMap = new HashMap<String, Type_Assignment>(assignments.size());
		for (final ASN1Assignment assignment : assignments) {
			if (assignment instanceof Type_Assignment) {
				((Type_Assignment)assignment).setHasSimilarName(false);
			}
		}
		for (final ASN1Assignment assignment : assignments) {
			if (assignment instanceof Type_Assignment) {
				final String assignmentName = assignment.getIdentifier().getName();
				final String lowerCaseName = assignmentName.toLowerCase(Locale.ENGLISH);
				if (similarityMap.containsKey(lowerCaseName)) {
					final Type_Assignment similarDef = similarityMap.get(lowerCaseName);
					((Type_Assignment)similarDef).setHasSimilarName(true);
					((Type_Assignment)assignment).setHasSimilarName(true);
				} else {
					similarityMap.put(lowerCaseName, (Type_Assignment)assignment);
				}
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (null != lastCompilationTimeStamp && !lastCompilationTimeStamp.isLess(timestamp)) {
			return;
		}

		checkUniqueness(timestamp);

		lastCompilationTimeStamp = timestamp;

		for (int i = 0; i < assignments.size(); i++) {
			assignments.get(i).check(timestamp);
			LoadBalancingUtilities.astNodeChecked();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void postCheck() {
		for (int i = 0; i < assignments.size(); i++) {
			assignments.get(i).postCheck();
		}
	}



	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector) {
		if (null == propCollector.getReference().getModuleIdentifier()) {
			for (final ASN1Assignment assignment : assignments) {
				assignment.addProposal(propCollector, 0);
			}
		}

		super.addProposal(propCollector);
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector) {
		final Reference reference = declarationCollector.getReference();
		if (null == reference.getModuleIdentifier()) {
			final Identifier id = reference.getId();
			final String name = id.getName();
			for (final ASN1Assignment assignment : assignments) {
				if(assignment.getIdentifier().getName().equals(name)) {
					assignment.addDeclaration(declarationCollector, 0);
				}
			}
		}
		super.addDeclaration(declarationCollector);
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getEnclosingAssignment(final int offset) {
		// getLocation() doesn't work since the locations are not set
		// properly
		// for ASN.1 assignments and types ( e.g. fields have -1 offsets
		// )
		// hack: getLikelyLocation() calculates location from some
		// components that have locations set properly (identifiers).
		// ASN.1 fields: must click on field identifier to find it
		// TODO: remove the getLikelyLocation() hack
		for (final ASN1Assignment assignment : assignments) {
			if (assignment.getLikelyLocation().containsOffset(offset)) {
				return assignment;
			}
		}

		for (final ASN1Assignment assignment : dynamic_assignments) {
			if (assignment.getLikelyLocation().containsOffset(offset)) {
				return assignment;
			}
		}

		return null;
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		for (final ASN1Assignment ass : assignments) {
			ass.findReferences(referenceFinder, foundIdentifiers);
		}
		for (final ASN1Assignment ass : dynamic_assignments) {
			ass.findReferences(referenceFinder, foundIdentifiers);
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
		for (final ASN1Assignment ass : assignments) {
			if (!ass.accept(v)) {
				return false;
			}
		}

		final List<ASN1Assignment> tempAssignments = new ArrayList<ASN1Assignment>(dynamic_assignments);
		for (final ASN1Assignment ass : tempAssignments) {
			if (!ass.accept(v)) {
				return false;
			}
		}
		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public Iterator<Assignment> iterator() {
		return new Iterator<Assignment>() {
			Iterator<ASN1Assignment> it = assignments.iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Assignment next() {
				return it.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Add generated java code on this level.
	 * @param aData the generated java code with other info
	 */
	public void generateCode( final JavaGenData aData ) {
		for (final ASN1Assignment assignment : assignments ) {
			assignment.generateCode( aData, false );
		}

		for (final ASN1Assignment assignment : dynamic_assignments ) {
			assignment.generateCode( aData, false );
		}
	}
}
