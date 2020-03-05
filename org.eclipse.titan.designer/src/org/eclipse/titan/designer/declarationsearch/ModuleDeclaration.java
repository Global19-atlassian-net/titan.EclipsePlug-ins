/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.declarationsearch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * @author Szabolcs Beres
 * */
class ModuleDeclaration extends Declaration {
	private final Module module;

	private static boolean markOccurrences;

	static {
		markOccurrences = Platform.getPreferencesService().getBoolean(ProductConstants.PRODUCT_ID_DESIGNER,
				PreferenceConstants.MARK_OCCURRENCES_TTCN3_ASSIGNMENTS, false, null);

		final Activator activator = Activator.getDefault();
		if (activator != null) {
			activator.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {

				@Override
				public void propertyChange(final PropertyChangeEvent event) {
					final String property = event.getProperty();
					if (PreferenceConstants.MARK_OCCURRENCES_TTCN3_ASSIGNMENTS.equals(property)) {
						markOccurrences = Platform.getPreferencesService().getBoolean(ProductConstants.PRODUCT_ID_DESIGNER,
								PreferenceConstants.MARK_OCCURRENCES_TTCN3_ASSIGNMENTS, false, null);
						return;
					}
				}
			});
		}
	}

	public ModuleDeclaration(final Module module) {
		this.module = module;
	}

	@Override
	public List<Hit> getReferences(final Module module) {
		final List<Hit> result = new ArrayList<ReferenceFinder.Hit>();
		// TODO The reference search for modules is not implemented
		if (module == this.module) {
			result.add(new Hit(this.module.getIdentifier()));
		}
		return result;
	}

	@Override
	public boolean shouldMarkOccurrences() {
		return markOccurrences;
	}

	@Override
	public Identifier getIdentifier() {
		return module.getIdentifier();
	}

	@Override
	public ReferenceFinder getReferenceFinder(final Module module) {
		// No reference finder present, guard with null
		return null;
	}

	@Override
	public Assignment getAssignment() {
		return null;
	}
}
