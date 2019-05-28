/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.brokenpartsanalyzers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ASN1.ASN1Assignment;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 *
 * @author Peter Olah
 */
public final class AssignmentHandlerAFTRerences extends ReferencesProcessor {

	public AssignmentHandlerAFTRerences(final Assignment assignment) {
		super(assignment);
	}

	@Override
	public void check(final AssignmentHandler assignmentHandler) {
		if (assignmentHandler instanceof AssignmentHandlerComponent) {
			checkIsInfected((AssignmentHandlerComponent) assignmentHandler);
		} else {
			checkIsInfected(assignmentHandler);
		}
		checkIsContagious(assignmentHandler);
	}

	private Set<String> computeIsInfected(final Set<String> otherInfectedReferences) {

		final Set<String> result = new HashSet<String>();

		final Set<String> intersectionWithNonContagiousReferences = new HashSet<String>(getNonContagiousReferences());
		intersectionWithNonContagiousReferences.retainAll(otherInfectedReferences);

		final Set<String> intersectionWithContagiousReferences = new HashSet<String>(getContagiousReferences());
		intersectionWithContagiousReferences.retainAll(otherInfectedReferences);

		result.addAll(intersectionWithNonContagiousReferences);
		result.addAll(intersectionWithContagiousReferences);
		return result;
	}

	public void checkIsInfected(final AssignmentHandlerComponent other) {
		if (!other.getIsInfected()) {
			return;
		}

		final String otherName = other.getAssignment().getIdentifier().getDisplayName();
		if (other.getAssignment().getLastTimeChecked() == null && isContagiousReferencesContains(otherName)) {
			setIsInfected(true);
			addInfectedReference(otherName);
			return;
		}

		final Set<String> infectedReferences = computeIsInfected(other.getInfectedReferences());
		if (!infectedReferences.isEmpty() || isContagiousReferencesContains(otherName)) {
			setIsInfected(true);
			addReason("It uses " + otherName + "@" + other.getAssignment().getMyScope().getModuleScope().getIdentifier().getDisplayName() + " which is infected.");
		}
	}

	@Override
	public int visit(final IVisitableNode node) {
		if (node instanceof ASN1Assignment) {
			final ASN1Assignment assignment = (ASN1Assignment) node;
			if(assignment.getAssPard() != null) {
				return V_SKIP;
			}
		}
		if (node instanceof StatementBlock) {
			final ReferenceCollector referenceCollector = new ReferenceCollector();
			node.accept(referenceCollector);
			final Set<Reference> references = referenceCollector.getReferences();//TODO: broken if reference does not point anywhere
			addNonContagiousReferences(computeReferences(references));
			if(containsErroneousReference(references)) {
				setIsInfected(true);
			}
			return V_SKIP;
		}

		if (node instanceof Reference) {
			final Identifier identifier = ((Reference) node).getId();
			if(identifier != null) {
				addContagiousReference(identifier.getDisplayName());
			}

			if(((Reference) node).getIsErroneous(CompilationTimeStamp.getBaseTimestamp())) {
				setIsInfected(true);
				setIsContagious(true);
				return V_CONTINUE;
			}

			final Assignment assignment = ((Reference) node).getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false, null);
			if(assignment == null || assignment.getIdentifier() == null || !assignment.getIdentifier().equals(identifier)) {
				setIsInfected(true);
				setIsContagious(true);
				return V_CONTINUE;
			}
		}
		return V_CONTINUE;
	}
}
