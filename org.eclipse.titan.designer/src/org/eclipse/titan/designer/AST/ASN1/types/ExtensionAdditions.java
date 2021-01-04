/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.types;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.types.CompField;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * ExtensionAdditionList.
 *
 * @author Kristof Szabados
 */
public final class ExtensionAdditions extends ASTNode {

	private final List<ExtensionAddition> extensionAdditions;

	public ExtensionAdditions() {
		extensionAdditions = new ArrayList<ExtensionAddition>();
	}

	public ExtensionAdditions(final List<ExtensionAddition> extensionAdditions) {
		this.extensionAdditions = extensionAdditions;
		for (final ExtensionAddition extensionAddition : extensionAdditions) {
			extensionAddition.setFullNameParent(this);
		}
	}

	public void addExtensionAddition(final ExtensionAddition extensionAddition) {
		if (null != extensionAddition) {
			extensionAdditions.add(extensionAddition);
			extensionAddition.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		for (final ExtensionAddition extensionAddition : extensionAdditions) {
			extensionAddition.setMyScope(scope);
		}
	}

	public int getNofComps() {
		int result = 0;
		for (final ExtensionAddition extensionAddition : extensionAdditions) {
			result += extensionAddition.getNofComps();
		}
		return result;
	}

	public CompField getCompByIndex(final int index) {
		int offset = index;
		for (int i = 0; i < extensionAdditions.size(); i++) {
			final int subSize = extensionAdditions.get(i).getNofComps();

			if (offset < subSize) {
				return extensionAdditions.get(i).getCompByIndex(offset);
			}

			offset -= subSize;
		}

		// FATAL ERROR
		return null;
	}

	public boolean hasCompWithName(final Identifier identifier) {
		for (final ExtensionAddition extensionAddition : extensionAdditions) {
			if (extensionAddition.hasCompWithName(identifier)) {
				return true;
			}
		}

		return false;
	}

	public CompField getCompByName(final Identifier identifier) {
		for (final ExtensionAddition extensionAddition : extensionAdditions) {
			if (extensionAddition.hasCompWithName(identifier)) {
				return extensionAddition.getCompByName(identifier);
			}
		}

		// FATAL ERROR
		return null;
	}

	public void trCompsof(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain, final boolean isSet) {
		for (final ExtensionAddition extensionAddition : extensionAdditions) {
			extensionAddition.trCompsof(timestamp, referenceChain, isSet);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (extensionAdditions == null) {
			return;
		}

		for (final ExtensionAddition ea : extensionAdditions) {
			ea.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (extensionAdditions != null) {
			for (final ExtensionAddition ea : extensionAdditions) {
				if (!ea.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}
}
