/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.definitions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignments;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ModuleImportation;
import org.eclipse.titan.designer.AST.ModuleImportationChain;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.graphics.ImageCache;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;

/**
 * Import module.
 * Models an asn.1 module of the section IMPORTS in the parent asn1.module
 *
 * @author Kristof Szabados
 */
public final class ImportModule extends ModuleImportation {
	public static final String MISSINGMODULE = "There is no ASN.1 module with name `{0}''";
	private static final String NOTASN1MODULE = "The module referred by `{0}'' is not an ASN.1 module";
	private static final String SYMBOLNOTEXPORTED = "Symbol `{0}'' is not exported from module `{1}''";

	/** imported symbols FROM this module */
	private final Symbols symbols;

	public ImportModule(final Identifier identifier, final Symbols symbols) {
		super(identifier);
		this.identifier = identifier;
		this.symbols = (null == symbols) ? new Symbols() : symbols;
	}

	@Override
	/** {@inheritDoc} */
	public String chainedDescription() {
		return (null != identifier) ? identifier.getDisplayName() : null;
	}

	/** @return the symbols to be imported from this imported module */
	public Symbols getSymbols() {
		return symbols;
	}

	/**
	 * Checks if a given symbol is imported through this importation.
	 *
	 * @param identifier
	 *                the identifier to search for.
	 *
	 * @return true if a symbol with this identifier is imported, false
	 *         otherwise.
	 * */
	public boolean hasSymbol(final Identifier identifier) {
		return symbols.hasSymbol(identifier.getName());
	}

	@Override
	/** {@inheritDoc} */
	public void checkImports(final CompilationTimeStamp timestamp, final ModuleImportationChain referenceChain, final List<Module> moduleStack) {
		if (null != lastImportCheckTimeStamp && !lastImportCheckTimeStamp.isLess(timestamp)) {
			return;
		}

		symbols.checkUniqueness(timestamp);

		final ProjectSourceParser parser = GlobalParser.getProjectSourceParser(project);
		if (null == parser || null == identifier) {
			lastImportCheckTimeStamp = timestamp;
			//FIXME: is it correct? lastImportCheckTimeStamp will be set in extreme case only - very early running
			referredModule = null;
			return;
		}

		final Module temp = referredModule;
		referredModule = parser.getModuleByName(identifier.getName());
		if (temp != referredModule) {
			setUnhandledChange(true);
		}
		if (referredModule == null) {
			identifier.getLocation().reportSemanticError(MessageFormat.format(MISSINGMODULE, identifier.getDisplayName()));
		} else {
			if (!(referredModule instanceof ASN1Module)) {
				identifier.getLocation().reportSemanticError(MessageFormat.format(NOTASN1MODULE, identifier.getDisplayName()));
				lastImportCheckTimeStamp = timestamp;
				referredModule = null;
				return;
			}

			moduleStack.add(referredModule);
			if (!referenceChain.add(this)) {
				moduleStack.remove(moduleStack.size() - 1);
				lastImportCheckTimeStamp = timestamp;
				return;
			}

			referredModule.checkImports(timestamp, referenceChain, moduleStack);

			for (int i = 0; i < symbols.size(); i++) {
				final Identifier id = symbols.getNthElement(i);

				if (((ASN1Module) referredModule).getAssignments().hasAssignmentWithId(timestamp, id)
						|| referredModule.hasImportedAssignmentWithID(timestamp, id)) {
					if (!((ASN1Module) referredModule).exportsSymbol(timestamp, id)) {
						identifier.getLocation().reportSemanticError(
								MessageFormat.format(SYMBOLNOTEXPORTED, id.getDisplayName(), referredModule
										.getIdentifier().getDisplayName()));
					}
				} else {
					id.getLocation().reportSemanticError(
							MessageFormat.format(ASN1Module.NOASSIGNMENTORSYMBOL, id.getDisplayName(), identifier.getDisplayName()));
				}
			}

			moduleStack.remove(moduleStack.size() - 1);
		}

		lastImportCheckTimeStamp = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastImportCheckTimeStamp != null && !lastImportCheckTimeStamp.isLess(timestamp)) {
			return;
		}

		usedForImportation = false;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasImportedAssignmentWithID(final CompilationTimeStamp timestamp, final Identifier identifier) {
		if (referredModule == null) {
			return false;
		}

		final Assignments assignments = referredModule.getAssignments();
		if (assignments != null && assignments.hasLocalAssignmentWithID(timestamp, identifier)) {
			return true;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public Assignment importAssignment(final CompilationTimeStamp timestamp, final ModuleImportationChain referenceChain,
			final Identifier moduleId, final Reference reference, final List<ModuleImportation> usedImports) {
		// referenceChain is not used since this can only be the
		// endpoint of an importation chain.
		if (referredModule == null) {
			return null;
		}

		final Assignment result = referredModule.importAssignment(timestamp, moduleId, reference);
		if (result != null) {
			usedImports.add(this);
			setUsedForImportation();
		}

		return result;
	}

	/**
	 * Adds the imported module or definitions contained in it, to the list
	 * completion proposals.
	 *
	 * @param propCollector
	 *                the proposal collector.
	 * @param targetModuleId
	 *                the identifier of the module where the definition will
	 *                be inserted. It is used to check if it is visible
	 *                there or not.
	 * */
	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final Identifier targetModuleId) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();

		if (propCollector.getReference().getModuleIdentifier() == null && subrefs.size() == 1) {
			propCollector.addProposal(identifier, ImageCache.getImage(getOutlineIcon()), KIND);
		}

		final Module savedReferredModule = referredModule;
		if (savedReferredModule != null) {
			final Assignments assignments = savedReferredModule.getAssignments();
			for (int i = 0, size = assignments.getNofAssignments(); i < size; i++) {
				final Assignment temporalAssignment = assignments.getAssignmentByIndex(i);
				if (savedReferredModule.isVisible(CompilationTimeStamp.getBaseTimestamp(), targetModuleId, temporalAssignment)) {
					temporalAssignment.addProposal(propCollector, 0);
				}
			}
		}
	}

	/**
	 * Adds the imported module or definitions contained in it, to the list
	 * declaration proposals.
	 *
	 * @param declarationCollector
	 *                the declaration collector.
	 * @param targetModuleId
	 *                the identifier of the module where the declaration
	 *                should be inserted into.
	 * */
	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final Identifier targetModuleId) {
		final Module savedReferredModule = referredModule;
		if (savedReferredModule != null) {
			final Assignments assignments = savedReferredModule.getAssignments();
			for (int i = 0; i < assignments.getNofAssignments(); i++) {
				final Assignment temporalAssignment = assignments.getAssignmentByIndex(i);
				if (savedReferredModule.isVisible(CompilationTimeStamp.getBaseTimestamp(), targetModuleId, temporalAssignment)) {
					temporalAssignment.addDeclaration(declarationCollector, 0);
				}
			}

			final Identifier moduleId = declarationCollector.getReference().getModuleIdentifier();
			final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
			if (moduleId == null && subrefs.size() == 1 && identifier.getName().equals(subrefs.get(0).getId().getName())) {
				declarationCollector.addDeclaration(savedReferredModule.getIdentifier().getDisplayName(), savedReferredModule
						.getIdentifier().getLocation(), (Scope) null);
			}
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
		if (identifier != null && !identifier.accept(v)) {
			return false;
		}
		if (symbols != null && !symbols.accept(v)) {
			return false;
		}
		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}
}
