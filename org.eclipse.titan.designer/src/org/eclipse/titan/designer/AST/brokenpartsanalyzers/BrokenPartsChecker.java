/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.brokenpartsanalyzers;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.TTCN3.definitions.TTCN3Module;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Helper class to check broken parts.
 *
 * @author Peter Olah
 */
public final class BrokenPartsChecker {

	private final SubMonitor progress;

	private final IProgressMonitor monitor;

	private final CompilationTimeStamp compilationCounter;

	private final SelectionMethodBase selectionMethod;

	public BrokenPartsChecker(final SubMonitor monitor, final CompilationTimeStamp compilationCounter, final SelectionMethodBase selectionMethod) {
		this.compilationCounter = compilationCounter;
		this.selectionMethod = selectionMethod;
		this.monitor = monitor;

		progress = SubMonitor.convert(monitor, 100);
	}

	public void doChecking() {
		monitor.subTask("Semantic checking");

		final BrokenPartsViaReferences brokenParts = (BrokenPartsViaReferences)selectionMethod;
		if (brokenParts.getAnalyzeOnlyDefinitions()) {
			final Map<Module, List<Assignment>> moduleAndBrokenDefinitions = brokenParts.getModuleAndBrokenDefs();
			definitionsChecker(moduleAndBrokenDefinitions);
		} else {
			generalChecker();
		}


		monitor.subTask("Doing post semantic checks");

		for (final Module module : selectionMethod.getModulesToCheck()) {
			module.postCheck();
		}

		progress.done();
	}

	private void generalChecker() {
		progress.setTaskName("Semantic check");
		progress.setWorkRemaining(selectionMethod.getModulesToCheck().size());

		for (final Module module : selectionMethod.getModulesToSkip()) {
			module.setSkippedFromSemanticChecking(true);
		}
		for (final Module module : selectionMethod.getModulesToCheck()) {
			module.setSkippedFromSemanticChecking(false);
		}

		// process the modules one-by-one
		for (final Module module : selectionMethod.getModulesToCheck()) {
			progress.subTask("Semantically checking module: " + module.getName());

			module.check(compilationCounter);

			progress.worked(1);
		}

		for (final Module module : selectionMethod.getModulesToSkip()) {
			module.setSkippedFromSemanticChecking(false);
		}
	}

	private void definitionsChecker(final Map<Module, List<Assignment>> moduleAndBrokenDefs) {
		progress.setTaskName("Semantic check");
		progress.setWorkRemaining(moduleAndBrokenDefs.size());

		for (final Map.Entry<Module, List<Assignment>> entry : moduleAndBrokenDefs.entrySet()) {
			final Module module = entry.getKey();

			progress.subTask("Semantically checking broken parts in module: " + module.getName());

			if (module instanceof TTCN3Module) {
				((TTCN3Module) module).checkWithDefinitions(compilationCounter, entry.getValue());
			} else {
				module.check(compilationCounter);
			}

			progress.worked(1);
		}
	}
}
