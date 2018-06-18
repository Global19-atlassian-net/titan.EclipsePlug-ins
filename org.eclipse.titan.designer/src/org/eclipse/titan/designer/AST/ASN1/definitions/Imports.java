/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.definitions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IOutlineElement;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ModuleImportationChain;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.core.LoadBalancingUtilities;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titan.designer.parsers.ProjectStructureDataCollector;

/**
 * Imported modules.
 *
 * @author Kristof Szabados
 */
public final class Imports extends ASTNode implements IOutlineElement, ILocateableNode {
	private static final String DUPLICATEIMPORTFIRST = "Duplicate import from module `{0}'' was first declared here";
	private static final String DUPLICATEIMPORTREPEATED = "Duplicate import from module `{0}'' was declared here again";
	private static final String TTCN3IMPORT = "An ASN.1 module cannot import from a TTCN-3 module";
	private static final String SELFIMPORT = "A module cannot import from itself";

	/** my module. */
	private ASN1Module module;

	/** The list of imported modules contained here. */
	private final List<ImportModule> importedModules_v = new ArrayList<ImportModule>();

	/** A hashmap of imported modules, used to to speed up searches. */
	private final Map<String, ImportModule> importedModules_map = new HashMap<String, ImportModule>();

	/**
	 * A hashmap of imported symbol names which were only imported from one
	 * location and their source module.
	 */
	protected final Map<String, Module> singularImportedSymbols_map = new HashMap<String, Module>();
	/**
	 * A hashset of imported symbol names which were imported from multiple
	 * locations.
	 */
	protected final Set<String> pluralImportedSymbols = new HashSet<String>();

	private CompilationTimeStamp lastImportCheckTimeStamp;
	private IProject project;

	/**
	 * The location of the whole import list. This location encloses the
	 * import list fully, as it is used to report errors to.
	 **/
	private Location location = NULL_Location.INSTANCE;

	public void addImportModule(final ImportModule importedModule) {
		if (null != importedModule && null != importedModule.getIdentifier() && null != importedModule.getIdentifier().getName()) {
			importedModule.setProject(project);
			importedModules_v.add(importedModule);
		}
	}

	/**
	 * Sets the parser of the project this module importation belongs to.
	 *
	 * @param project
	 *                the project this module importation belongs to
	 * */
	public void setProject(final IProject project) {
		this.project = project;
		for (ImportModule temp : importedModules_v) {
			temp.setProject(project);
		}
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
	 * Sets the module of this importation list to be the provided module.
	 *
	 * @param module
	 *                the module of this importations.
	 * */
	public void setMyModule(final ASN1Module module) {
		this.module = module;
	}

	@Override
	/** {@inheritDoc} */
	public Identifier getIdentifier() {
		return new Identifier(Identifier.Identifier_type.ID_ASN, "imports", location, true);
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineText() {
		return "";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "imports.gif";
	}

	@Override
	/** {@inheritDoc} */
	public Object[] getOutlineChildren() {
		return importedModules_v.toArray();
	}

	@Override
	/** {@inheritDoc} */
	public int category() {
		return 0;
	}

	/**
	 * Checks the import hierarchies of this importation (and the ones in
	 * the imported module recursively).
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param referenceChain
	 *                a chain of references used to find circularly imported
	 *                modules.
	 * @param moduleStack
	 *                the stack of modules visited so far, from the starting
	 *                point.
	 * */
	public void checkImports(final CompilationTimeStamp timestamp, final ModuleImportationChain referenceChain, final List<Module> moduleStack) {
		if (null != lastImportCheckTimeStamp && !lastImportCheckTimeStamp.isLess(timestamp)) {
			return;
		}

		lastImportCheckTimeStamp = timestamp;

		if (null == project) {
			return;
		}

		importedModules_map.clear();
		singularImportedSymbols_map.clear();
		pluralImportedSymbols.clear();

		final ProjectSourceParser parser = GlobalParser.getProjectSourceParser(project);
		if (null == parser) {
			return;
		}

		for (ImportModule importModule : importedModules_v) {
			final Identifier identifier = importModule.getIdentifier();

			if (null == identifier || null == identifier.getLocation()) {
				continue;
			}

			final Module referredModule = parser.getModuleByName(identifier.getName());

			if (null == referredModule) {
				identifier.getLocation().reportSemanticError(
						MessageFormat.format(ImportModule.MISSINGMODULE, identifier.getDisplayName()));
				continue;
			} else if (!(referredModule instanceof ASN1Module)) {
				identifier.getLocation().reportSemanticError(TTCN3IMPORT);
				continue;
			} else if (referredModule == module) {
				identifier.getLocation().reportSemanticError(SELFIMPORT);
				continue;
			}

			String name = identifier.getName();
			if (importedModules_map.containsKey(name)) {
				final Location importedLocation = importedModules_map.get(name).getIdentifier().getLocation();
				importedLocation.reportSingularSemanticError(MessageFormat.format(DUPLICATEIMPORTFIRST,
						identifier.getDisplayName()));
				identifier.getLocation().reportSemanticError(
						MessageFormat.format(DUPLICATEIMPORTREPEATED, identifier.getDisplayName()));
			} else {
				importedModules_map.put(name, importModule);
			}

			final Symbols symbols = importModule.getSymbols();
			if (null == symbols) {
				continue;
			}

			for (int i = 0; i < symbols.size(); i++) {
				name = symbols.getNthElement(i).getName();
				if (singularImportedSymbols_map.containsKey(name)) {
					if (!referredModule.equals(singularImportedSymbols_map.get(name))) {
						singularImportedSymbols_map.remove(name);
						pluralImportedSymbols.add(name);
					}
				} else if (!pluralImportedSymbols.contains(name)) {
					singularImportedSymbols_map.put(name, referredModule);
				}
			}
			importModule.setUnhandledChange(false);
			LoadBalancingUtilities.astNodeChecked();
		}

		for (ImportModule importModule : importedModules_v) {
			// check the imports recursively
			referenceChain.markState();
			importModule.checkImports(timestamp, referenceChain, moduleStack);
			referenceChain.previousState();

		}
	}

	/**
	 * checks the import statement itself.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 **/
	public void check(final CompilationTimeStamp timestamp) {
		for (ImportModule importModule : importedModules_v) {
			importModule.check(timestamp);
		}
	}

	/**
	 * Collects and returns the list of imported modules.
	 * <p>
	 * Module importations that does not point to an existing module are
	 * reported as null objects.
	 *
	 * @return the list of modules imported by the actual module.
	 * */
	public List<Module> getImportedModules() {
		final List<Module> result = new ArrayList<Module>();

		for (ImportModule impmod : importedModules_v) {
			final Module module = impmod.getReferredModule();
			if (module != null) {
				result.add(module);
			}
		}

		return result;
	}

	/**
	 * @return Whether any of the importations has changed to an other
	 *         module one since the last importation check.
	 * */
	public boolean hasUnhandledImportChanges() {
		for (ImportModule impmod : importedModules_v) {
			if (impmod.hasUnhandledChange()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if an importation was not used to import elements into the
	 * module.
	 * */
	public void checkImportedness() {
		for (ImportModule importModule : importedModules_v) {
			importModule.postCheck();
		}
	}

	/**
	 * Checks if a module with the provided id is imported in this module.
	 *
	 * @param id
	 *                the identifier to use.
	 *
	 * @return true if a module with the provided name is imported, false
	 *         otherwise.
	 * */
	public boolean hasImportedModuleWithId(final Identifier id) {
		final String name = id.getName();
		return importedModules_map.containsKey(name);
	}

	/**
	 * Checks if a module with the provided id is imports this module.
	 *
	 * @param id
	 *                the identifier to use.
	 *
	 * @return true if a module with the provided name imports this module,
	 *         false otherwise.
	 * */
	public ImportModule getImportedModuleById(final Identifier id) {
		final String name = id.getName();
		if (importedModules_map.containsKey(name)) {
			return importedModules_map.get(name);
		}

		return null;
	}

	/**
	 * Checks if there is a symbol imported from anywhere with the provided
	 * identifier.
	 *
	 * @param identifier
	 *                the identifier used to search for a symbol.
	 *
	 * @return true if a symbol with the provided name is imported, false
	 *         otherwise.
	 * */
	public boolean hasImportedSymbolWithId(final Identifier identifier) {
		return singularImportedSymbols_map.containsKey(identifier.getName()) || pluralImportedSymbols.contains(identifier.getName());
	}

	/**
	 * Searches for elements that could complete the provided prefix, and if
	 * found, they are added to the provided proposal collector.
	 * <p>
	 *
	 * @see ProposalCollector
	 * @param propCollector
	 *                the proposal collector holding the prefix and
	 *                collecting the proposals.
	 * */
	public void addProposal(final ProposalCollector propCollector) {
		for (ImportModule importation : importedModules_map.values()) {
			importation.addProposal(propCollector, module.getIdentifier());
		}
	}

	/**
	 * Searches for elements that could be referred to be the provided
	 * reference, and if found, they are added to the declaration collector.
	 * <p>
	 *
	 * @see DeclarationCollector
	 * @param declarationCollector
	 *                the declaration collector folding the reference and
	 *                collecting the declarations.
	 * */
	public void addDeclaration(final DeclarationCollector declarationCollector) {
		final Reference reference = declarationCollector.getReference();
		final Identifier identifier = reference.getId();
		if (singularImportedSymbols_map.containsKey(identifier.getName())) {
			final Module tempModule = singularImportedSymbols_map.get(identifier.getName());
			tempModule.getAssignmentsScope().addDeclaration(declarationCollector);
		}
		if (!declarationCollector.getCollected().isEmpty()) {
			return;
		}

		for (ImportModule importation : importedModules_map.values()) {
			importation.addDeclaration(declarationCollector, module.getIdentifier());
		}
	}

	public void extractStructuralInformation(final Identifier from, final ProjectStructureDataCollector collector) {
		for (ImportModule imported : importedModules_v) {
			collector.addImportation(from, imported.getIdentifier());
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (importedModules_v != null) {
			for (ImportModule im : importedModules_v) {
				if (!im.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Generate code for importing from other modules.
	 *
	 * @param aData the generated java code with other info
	 */
	public void generateCode( final JavaGenData aData ) {
		if (importedModules_v != null) {
			for (ImportModule im : importedModules_v) {
				aData.addInterModuleImport(im.getIdentifier().getName());
			}
		}
	}
}
