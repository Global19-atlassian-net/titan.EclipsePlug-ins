/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents the list of type mapping targets provided in a single type
 * mapping.
 *
 * @author Kristof Szabados
 * */
public final class TypeMappingTargets extends ASTNode implements IIncrementallyUpdateable {
	private final List<TypeMappingTarget> targets;

	public TypeMappingTargets() {
		targets = new ArrayList<TypeMappingTarget>();
	}

	public void addMappingTarget(final TypeMappingTarget mappingTarget) {
		targets.add(mappingTarget);
	}

	public int getNofTargets() {
		return targets.size();
	}

	public TypeMappingTarget getTargetByIndex(final int index) {
		return targets.get(index);
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0, size = targets.size(); i < size; i++) {
			final TypeMappingTarget target = targets.get(i);
			if (target == child) {
				return builder.append(".<target ").append(i + 1).append('>');
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		for (int i = 0, size = targets.size(); i < size; i++) {
			targets.get(i).setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		for (final TypeMappingTarget tmt : targets) {
			tmt.updateSyntax(reparser, false);
			reparser.updateLocation(tmt.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (targets == null) {
			return;
		}

		for (final TypeMappingTarget tmt : targets) {
			tmt.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (targets != null) {
			for (final TypeMappingTarget tmt : targets) {
				if (!tmt.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}
}
