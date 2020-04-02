/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.parsers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.parsers.TITANMarker;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.OutOfMemoryCheck;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.MarkerHandler;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ModuleImportationChain;
import org.eclipse.titan.designer.AST.ASN1.Ass_pard;
import org.eclipse.titan.designer.AST.ASN1.definitions.ASN1Module;
import org.eclipse.titan.designer.AST.TTCN3.definitions.ImportModule;
import org.eclipse.titan.designer.AST.TTCN3.definitions.TTCN3Module;
import org.eclipse.titan.designer.AST.brokenpartsanalyzers.BrokenPartsChecker;
import org.eclipse.titan.designer.AST.brokenpartsanalyzers.BrokenPartsViaReferences;
import org.eclipse.titan.designer.AST.brokenpartsanalyzers.SelectionMethodBase;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.core.TITANNature;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * Helper class to separate the responsibility of the source parser into smaller
 * parts. This class is responsible for handling the semantic checking of the
 * source code of the projects
 *
 * @author Kristof Szabados
 * */
public class ProjectSourceSemanticAnalyzer {
	private static final String DUPLICATEMODULE = "Modules must be unique, but `{0}'' was declared multiple times";
	public static final String CIRCULARIMPORTCHAIN = "Circular import chain is not recommended: {0}";

	/**
	 * file to Module map received from the syntax analysis.
	 * Serves as the main/reliable source of information.
	 * */
	private final Map<IFile, Module> fileModuleMap;

	/**
	 * module name to module mapping to speed up searching in the list of uptodate modules.
	 *
	 * Calculated during semantic check, maintained in-between semantic checks.
	 * */
	private final Map<String, Module> moduleMap;
	/**
	 * module name to module mapping to speed up searching in the list of outdated modules.
	 *
	 * Cleared during semantic check, maintained in-between semantic checks.
	 * */
	private final Map<String, Module> outdatedModuleMap;

	/**
	 * The names of the modules, which were checked at the last semantic check.
	 *
	 * Caculated during the semantic check, maintained in-between semantic checks.
	 * */
	private final Set<String> semanticallyUptodateModules;

	public ProjectSourceSemanticAnalyzer() {

		fileModuleMap = new ConcurrentHashMap<IFile, Module>();
		moduleMap = new ConcurrentHashMap<String, Module>();
		outdatedModuleMap = new HashMap<String, Module>();
		semanticallyUptodateModules = new HashSet<String>();
	}

	/**
	 * Checks whether the internal data belonging to the provided file is
	 * semantically out-dated.
	 *
	 * @param file
	 *            the file to check.
	 *
	 * @return true if the data was reported to be out-dated since the last
	 *         analysis.
	 * */
	public boolean isOutdated(final IFile file) {
		final Module module = fileModuleMap.get(file);

		return module == null || !semanticallyUptodateModules.contains(module.getName());
	}

	/**
	 * Returns the module with the provided name, or null.
	 *
	 * @param name
	 *            the name of the module to return.
	 * @param uptodateOnly
	 *            allow finding only the up-to-date modules.
	 *
	 * @return the module having the provided name
	 * */
	Module internalGetModuleByName(final String name, final boolean uptodateOnly) {
		if (moduleMap.containsKey(name)) {
			return moduleMap.get(name);
		}

		if (!uptodateOnly && outdatedModuleMap.containsKey(name)) {
			return outdatedModuleMap.get(name);
		}

		return null;
	}

	/**
	 * Returns the actually known module's names.
	 *
	 * @return a set of the module names known in this project or in the
	 *         ones referenced.
	 * */
	Set<String> internalGetKnownModuleNames() {
		final Set<String> temp = new HashSet<String>();
		temp.addAll(moduleMap.keySet());
		return temp;
	}

	Collection<Module> internalGetModules() {
		return moduleMap.values();
	}

	Module getModulebyFile(final IFile file) {
		return fileModuleMap.get(file);
	}

	/**
	 * Reports that the provided file has changed and so it's stored
	 * information became out of date.
	 * <p>
	 * Stores that this file is out of date for later
	 * <p>
	 *
	 * <p>
	 * Files which are excluded from the build should not be reported.
	 * </p>
	 *
	 * @param outdatedFile
	 *            the file which seems to have changed
	 * @param useOnTheFlyParsing
	 *            true if on-the-fly parsing is enabled.
	 * */
	public void reportOutdating(final IFile outdatedFile, final boolean useOnTheFlyParsing) {
		if (!useOnTheFlyParsing) {
			return;
		}

		final Module module = fileModuleMap.get(outdatedFile);
		if (module == null) {
			return;
		}

		final IResource moduleFile = module.getIdentifier().getLocation().getFile();
		if (!outdatedFile.equals(moduleFile)) {
			return;
		}

		final String moduleName = module.getName();
		moduleMap.remove(moduleName);
		fileModuleMap.remove(moduleFile);

		synchronized (outdatedModuleMap) {
			outdatedModuleMap.put(moduleName, module);
		}

		synchronized (semanticallyUptodateModules) {
			semanticallyUptodateModules.remove(moduleName);
		}
	}

	/**
	 * Reports that the semantic meaning of the provided file might have
	 * changed and so it's stored information became out of date.
	 * <p>
	 * Stores that this file is semantically out of date for later
	 * <p>
	 *
	 * @param outdatedFile
	 *            the file which seems to have changed
	 * */
	public void reportSemanticOutdating(final IFile outdatedFile) {
		final Module module = fileModuleMap.get(outdatedFile);
		if(module != null) {
			synchronized (semanticallyUptodateModules) {
				semanticallyUptodateModules.remove(module.getName());
			}
		}
	}

	/**
	 * Force the next semantic analysis to reanalyze everything.
	 * */
	void clearSemanticInformation() {
		synchronized (semanticallyUptodateModules) {
			semanticallyUptodateModules.clear();
		}
	}

	/**
	 * Removes data related to modules, that were deleted or moved.
	 *
	 * @param file
	 *            the file that was changed.
	 * @param moduleName
	 *            the name of the module in that file.
	 **/
	void removedReferencestoRemovedFiles(final IFile file, final String moduleName) {
		synchronized (semanticallyUptodateModules) {
			semanticallyUptodateModules.remove(moduleName);
		}

		moduleMap.remove(moduleName);
		fileModuleMap.remove(file);
		synchronized (outdatedModuleMap) {
			outdatedModuleMap.remove(moduleName);
		}
	}

	/**
	 * Removes a module from the set of semantically analyzed modules.
	 *
	 * @param moduleName
	 *            the name of the module to be removed.
	 * */
	void removeModule(final String moduleName) {
		final Module module = internalGetModuleByName(moduleName, false);
		if (module == null) {
			return;
		}

		fileModuleMap.remove(module.getLocation().getFile());
		synchronized (outdatedModuleMap) {
			outdatedModuleMap.remove(moduleName);
		}

		moduleMap.remove(moduleName);
	}

	/**
	 * Adds a module to the set of semantically analyzed modules.
	 *
	 * @param module
	 *            the module to be added.
	 * @return true if it was successfully added, false otherwise.
	 * */
	public void addModule(final Module module) {
		fileModuleMap.put((IFile)module.getLocation().getFile(), module);
	}

	/**
	 * Internal function.
	 * <p>
	 * Does the semantic checking of the modules located in multiple projects.
	 * It is important to call this function after the
	 * {@link #internalDoAnalyzeSyntactically(IProgressMonitor, CompilationTimeStamp)}
	 * function was executed on all involved projects, as the out-dated markers will be cleared here.
	 *
	 * @param tobeSemanticallyAnalyzed the list of projects to be analyzed.
	 * @param monitor
	 *                the progress monitor to provide feedback to the user
	 *                about the progress.
	 * @param compilationCounter
	 *            the timestamp of the actual build cycle.
	 *
	 * @return the status of the operation when it finished.
	 * */
	static IStatus analyzeMultipleProjectsSemantically(final List<IProject> tobeSemanticallyAnalyzed, final IProgressMonitor monitor, final CompilationTimeStamp compilationCounter) {
		for (int i = 0; i < tobeSemanticallyAnalyzed.size(); i++) {
			if (!tobeSemanticallyAnalyzed.get(i).isAccessible() || !TITANNature.hasTITANNature(tobeSemanticallyAnalyzed.get(i))) {
				return Status.CANCEL_STATUS;
			}
		}

		final long semanticCheckStart = System.nanoTime();

		for (int i = 0; i < tobeSemanticallyAnalyzed.size(); i++) {
			final ProjectSourceSemanticAnalyzer semanticAnalyzer = GlobalParser.getProjectSourceParser(tobeSemanticallyAnalyzed.get(i)).getSemanticAnalyzer();
			synchronized (semanticAnalyzer.outdatedModuleMap) {
				semanticAnalyzer.outdatedModuleMap.clear();
			}
			semanticAnalyzer.moduleMap.clear();
		}
		// Semantic checking starts here
		final SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.setTaskName("On-the-fly semantic checking of everything ");
		progress.subTask("Checking the importations of the modules");

		try{

			// clean the instantiated parameterized assignments,
			// from their instances
			Ass_pard.resetAllInstanceCounters();

			//check for duplicated module names
			final HashMap<String, Module> uniqueModules = new HashMap<String, Module>();
			final Set<String> duplicatedModules = new HashSet<String>();

			// collect all modules and semantically checked modules to work on.
			final List<Module> allModules = new ArrayList<Module>();
			final List<String> semanticallyChecked = new ArrayList<String>();

			//remove module name duplication markers. It shall be done before starting the next for-loop!
			for (int i = 0; i < tobeSemanticallyAnalyzed.size(); i++) {
				final ProjectSourceSemanticAnalyzer semanticAnalyzer =
						GlobalParser.getProjectSourceParser(tobeSemanticallyAnalyzed.get(i)).getSemanticAnalyzer();
				for (final Module module: semanticAnalyzer.fileModuleMap.values()) {
					if(module instanceof TTCN3Module){
						MarkerHandler.markAllSemanticMarkersForRemoval(module.getIdentifier());
					}
				}
			}

			for (int i = 0; i < tobeSemanticallyAnalyzed.size(); i++) {
				final ProjectSourceSemanticAnalyzer semanticAnalyzer =
						GlobalParser.getProjectSourceParser(tobeSemanticallyAnalyzed.get(i)).getSemanticAnalyzer();
				for (final Module module: semanticAnalyzer.fileModuleMap.values()) {
					final String name = module.getIdentifier().getName();
					allModules.add(module);
					//ASN1 modules are not been analyzed incrementally, therefore their markers can be removed in one step:
					if(module instanceof ASN1Module){
						MarkerHandler.markAllSemanticMarkersForRemoval(module.getLocation().getFile());
					}
					if(uniqueModules.containsKey(name)) {
						final Location location = uniqueModules.get(name).getIdentifier().getLocation();
						final Location location2 = module.getIdentifier().getLocation();
						location.reportSemanticError(MessageFormat.format(DUPLICATEMODULE, module.getIdentifier().getDisplayName()));
						location2.reportSemanticError(MessageFormat.format(DUPLICATEMODULE, module.getIdentifier().getDisplayName()));
						duplicatedModules.add(name);
						semanticAnalyzer.semanticallyUptodateModules.remove(name);
					} else {
						uniqueModules.put(name, module);
						semanticAnalyzer.moduleMap.put(name, module);
						if(semanticAnalyzer.semanticallyUptodateModules.contains(name)) {
							semanticallyChecked.add(name);
						}
					}
				}
			}

			int nofModulesTobeChecked = 0;
			if(allModules.size() > semanticallyChecked.size()) {

				// check and build the import hierarchy of the modules
				final ModuleImportationChain referenceChain = new ModuleImportationChain(CIRCULARIMPORTCHAIN, false);

				//remove markers from import lines
				for(final Module module : allModules) {
					if(module instanceof TTCN3Module) {
						final List<ImportModule> imports = ((TTCN3Module) module).getImports();
						for(final ImportModule imp : imports) {
							MarkerHandler.markAllSemanticMarkersForRemoval(imp.getLocation());
						}
					}
					// markers are removed in one step in ASN1 modules
				}

				for(final Module module : allModules) {
					module.checkImports(compilationCounter, referenceChain, new ArrayList<Module>());
					referenceChain.clear();
				}

				progress.subTask("Calculating the list of modules to be checked");

				final BrokenPartsViaReferences selectionMethod = new BrokenPartsViaReferences(compilationCounter);
				final SelectionMethodBase selectionMethodBase = (SelectionMethodBase)selectionMethod;
				selectionMethodBase.setModules(allModules, semanticallyChecked);
				selectionMethod.execute();

				if (OutOfMemoryCheck.isOutOfMemory()) {
					OutOfMemoryCheck.outOfMemoryEvent();
					return Status.CANCEL_STATUS;
				}

				final BrokenPartsChecker brokenPartsChecker = new BrokenPartsChecker(progress.newChild(1), compilationCounter, selectionMethodBase);
				brokenPartsChecker.doChecking();

				// re-enable the markers on the skipped modules.
				for (final Module module2 : selectionMethodBase.getModulesToSkip()) {
					MarkerHandler.reEnableAllMarkers((IFile) module2.getLocation().getFile());
				}

				nofModulesTobeChecked = selectionMethodBase.getModulesToCheck().size();
			} else {
				//re-enable all markers
				for (final Module module2 : allModules) {
					MarkerHandler.reEnableAllMarkers((IFile) module2.getLocation().getFile());
				}
			}

			//Not supported markers are handled here, at the and of checking. Otherwise they would be deleted
			final IPreferencesService preferenceService = Platform.getPreferencesService();
			final String option = preferenceService.getString(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.REPORTUNSUPPORTEDCONSTRUCTS, GeneralConstants.WARNING, null);
			for (int i = 0; i < tobeSemanticallyAnalyzed.size(); i++) {
				// report the unsupported constructs in the project
				final ProjectSourceSyntacticAnalyzer syntacticAnalyzer = GlobalParser.getProjectSourceParser(tobeSemanticallyAnalyzed.get(i)).getSyntacticAnalyzer();
				for (final IFile file : syntacticAnalyzer.unsupportedConstructMap.keySet()) {
					final List<TITANMarker> markers = syntacticAnalyzer.unsupportedConstructMap.get(file);
					if (markers != null && file.isAccessible()) {
						for (final TITANMarker marker : markers) {
							final Location location = new Location(file, marker.getLine(), marker.getOffset(), marker.getEndOffset());
							location.reportConfigurableSemanticProblem(option, marker.getMessage());
						}
					}
				}
			}

			if (preferenceService.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.DISPLAYDEBUGINFORMATION, true, null)) {
				TITANDebugConsole.println("  ** Had to start checking at " + nofModulesTobeChecked + " modules. ");
				TITANDebugConsole.println("  **On-the-fly semantic checking of projects (" + allModules.size() + " modules) took " + (System.nanoTime() - semanticCheckStart) * (1e-9) + " seconds");
			}
			progress.subTask("Cleanup operations");

			for (int i = 0; i < tobeSemanticallyAnalyzed.size(); i++) {
				final ProjectSourceSemanticAnalyzer semanticAnalyzer = GlobalParser.getProjectSourceParser(tobeSemanticallyAnalyzed.get(i)).getSemanticAnalyzer();
				synchronized (semanticAnalyzer.semanticallyUptodateModules) {
					semanticAnalyzer.semanticallyUptodateModules.clear();
					semanticAnalyzer.semanticallyUptodateModules.addAll(semanticAnalyzer.moduleMap.keySet());
					for (final String name: duplicatedModules) {
						semanticAnalyzer.semanticallyUptodateModules.remove(name);
					}
				}
			}
		} catch (Exception e) {
			// This catch is extremely important, as it is supposed
			// to protect the project parser, from whatever might go
			// wrong inside the analysis.
			ErrorReporter.logExceptionStackTrace(e);
		}
		progress.done();

		for (int i = 0; i < tobeSemanticallyAnalyzed.size(); i++) {
			final IProject actualProject = tobeSemanticallyAnalyzed.get(i);
			final ProjectSourceParser parser = GlobalParser.getProjectSourceParser(actualProject);
			parser.setLastTimeChecked(compilationCounter);

			GlobalProjectStructureTracker.updateData(actualProject);

			MarkerHandler.removeAllOnTheFlyMarkedMarkers(tobeSemanticallyAnalyzed.get(i));
		}

		return Status.OK_STATUS;
	}
}