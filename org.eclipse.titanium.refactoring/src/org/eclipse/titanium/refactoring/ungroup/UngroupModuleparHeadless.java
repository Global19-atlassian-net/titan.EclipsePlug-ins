/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.ungroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.titan.common.logging.ErrorReporter;

/**
 * @author Nagy Matyas
 * */
public class UngroupModuleparHeadless {

	private final IStructuredSelection selection;

	public UngroupModuleparHeadless(final IStructuredSelection selection) {
		this.selection = selection;
	}

	public void run() {
		final UngroupModuleparRefactoring refactoring = new UngroupModuleparRefactoring(selection);
		try {
			final Change change = refactoring.createChange(null);
			change.perform(new NullProgressMonitor());
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
	}



}
