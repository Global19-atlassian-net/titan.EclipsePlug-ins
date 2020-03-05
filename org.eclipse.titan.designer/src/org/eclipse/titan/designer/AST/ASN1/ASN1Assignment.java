/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Identifier.Identifier_type;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * Abstract class to represent ASN.1 assignments.
 *
 * @author Kristof Szabados
 */
public abstract class ASN1Assignment extends Assignment {
	private static final String NOTPARAMETERIZEDASSIGNMENT = "`{0}'' is not a parameterized assignment";
	public static final String UNREACHABLE = "The identifier `{0}'' is not reachable from TTCN-3";

	private static boolean markOccurrences;

	protected final Ass_pard assPard;
	protected boolean dontGenerate;

	static {
		final IPreferencesService prefService = Platform.getPreferencesService();
		markOccurrences = prefService.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.MARK_OCCURRENCES_ASN1_ASSIGNMENTS,
				false, null);

		final Activator activator = Activator.getDefault();
		if (activator != null) {
			activator.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(final PropertyChangeEvent event) {
					final String property = event.getProperty();
					if (PreferenceConstants.MARK_OCCURRENCES_ASN1_ASSIGNMENTS.equals(property)) {
						markOccurrences = prefService.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER,
								PreferenceConstants.MARK_OCCURRENCES_ASN1_ASSIGNMENTS, false, null);
					}
				}
			});
		}
	}

	protected ASN1Assignment(final Identifier id, final Ass_pard assPard) {
		super(id);
		this.assPard = assPard;
		this.dontGenerate = false;
	}

	public void setDontGenerate() {
		dontGenerate = true;
	}

	/** @return the parameterizes assignment related to the assignment */
	public final Ass_pard getAssPard() {
		return assPard;
	}

	/**
	 * Internal new instance creating function, will only be called for
	 * parameterized assignments.
	 *
	 * @param identifier
	 *                the name the new assignment instance shall have.
	 * @return a copy of the assignment.
	 * */
	protected abstract ASN1Assignment internalNewInstance(final Identifier identifier);

	/**
	 * Sets the scope of the right side of the assignment.
	 *
	 * @param rightScope
	 *                the scope to be set for the right side.
	 * */
	public abstract void setRightScope(Scope rightScope);

	/**
	 * Creates a new instance of a parameterized assignment and returns i.
	 * In case of assignments which are not parameterized should return
	 * null.
	 *
	 * @param module
	 *                the module in which the new assignment should be
	 *                created.
	 *
	 * @return the assignment created.
	 * */
	public final ASN1Assignment newInstance(final Module module) {
		if (null == assPard) {
			if (null != location) {
				location.reportSemanticError(MessageFormat.format(NOTPARAMETERIZEDASSIGNMENT, getFullName()));
			}

			return null;
		}

		final StringBuilder newName = new StringBuilder();
		newName.append(getIdentifier().getAsnName()).append('.').append(module.getIdentifier().getAsnName()).append(".inst");
		newName.append(assPard.newInstanceNumber(module));
		return internalNewInstance(new Identifier(Identifier_type.ID_ASN, newName.toString()));
	}

	/**
	 * Checks whether the assignment has a valid TTCN-3 identifier, i.e. is
	 * reachable from TTCN.
	 * */
	public final void checkTTCNIdentifier() {
		if (null == myScope || null == getIdentifier()) {
			return;
		}

		final Module myModule = myScope.getModuleScope();

		if (!getIdentifier().getHasValid(Identifier_type.ID_TTCN) && myScope.getParentScope().equals(myModule)
				&& null != myModule.getIdentifier() && '<' != myModule.getIdentifier().getDisplayName().charAt(0)) {
			location.reportSemanticWarning(MessageFormat.format(UNREACHABLE, getIdentifier().getDisplayName()));
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		check(timestamp, null);
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final IReferenceChain refChain) {
		if (null != assPard) {
			assPard.check(timestamp);
			lastTimeChecked = timestamp;
		}
	}

	/**
	 * Checks whether the actual assignment is of a specified type.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param assignmentType
	 *                the type to check against.
	 * @param referenceChain
	 *                the reference chain to detect circular references
	 *
	 * @return true if the assignment is of the specified type, false
	 *         otherwise
	 * */
	public boolean isAssignmentType(final CompilationTimeStamp timestamp, final Assignment_type assignmentType,
			final IReferenceChain referenceChain) {
		return getAssignmentType().semanticallyEquals(assignmentType);
	}

	@Override
	/** {@inheritDoc} */
	public String getGenName() {
		if(myScope == null ||
				myScope.getParentScope().equals(myScope.getModuleScopeGen())) {
			// use the simple identifier if the assignment does not have scope
			// or it is a simple assignment at module scope
			return identifier.getName();
		} else {
			// this assignment belongs to an instantiation of a parameterized
			// assignment: use the name of the parent scope to obtain genname
			final StringBuilder nameBuilder = new StringBuilder("@");
			nameBuilder.append(myScope.getScopeName());
			final String displayName = identifier.getDisplayName();
			final boolean isParameterised = displayName.lastIndexOf('.') == displayName.length();
			if(isParameterised) {
				nameBuilder.append('.');
				nameBuilder.append(displayName);
			}

			final StringBuilder returnValue = new StringBuilder(Identifier.getNameFromAsnName(nameBuilder.toString()));
			if(isParameterised) {
				returnValue.append("_par_");
			}

			return returnValue.toString();
		}
	}

	// TODO: remove when location is fixed
	public Location getLikelyLocation() {
		return getLocation();
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (assPard == null) {
			return;
		}

		assPard.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	public boolean shouldMarkOccurrences() {
		return markOccurrences;
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (identifier != null && !identifier.accept(v)) {
			return false;
		}
		if (assPard != null && !assPard.accept(v)) {
			return false;
		}

		return true;
	}
}
