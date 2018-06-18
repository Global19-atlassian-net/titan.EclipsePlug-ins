/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.titan.common.path.PathConverter;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.core.TITANInstallationValidator;
import org.eclipse.titan.designer.core.TITANJob;
import org.eclipse.titan.designer.license.LicenseValidator;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * This action invokes the compiler to get it's version information.
 * 
 * @author Kristof Szabados
 */
public final class CheckVersion extends ExternalTitanAction {
	private static final String JOB_TITLE = "Version check";
	private static final String DOT = ".";

	/**
	 * This method creates the needed {@link TITANJob} and schedules it.
	 * <p>
	 * The actual work:
	 * <ul>
	 * <li>creates the command that invokes the compiler with the -v flag
	 * <li>creates a TITANJob for invoking the command and redirecting the results
	 * <li>schedules the job.
	 * </ul>
	 *
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 *
	 * @param action the action proxy that handles the presentation portion of the action (not used here)
	 */
	@Override
	/** {@inheritDoc} */
	public void run(final IAction action) {
		doCheckVersion();
	}


	@Override
	/** {@inheritDoc} */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		doCheckVersion();

		return null;
	}

	/**
	 * do check the version of the compiler set.
	 * */
	private void doCheckVersion(){
		if (!TITANInstallationValidator.check(true)) {
			return;
		}

		if (!LicenseValidator.check()) {
			return;
		}

		processSelection();

		final TITANJob titanJob = new TITANJob(JOB_TITLE, new HashMap<String, IFile>(), new File(DOT), project);
		titanJob.setPriority(Job.DECORATE);
		titanJob.setUser(true);
		titanJob.setRule(project);

		final boolean reportDebugInformation =
				Platform.getPreferencesService().getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.DISPLAYDEBUGINFORMATION, false, null);


		final List<String> command = new ArrayList<String>();
		command.add(PathConverter.convert(getCompilerPath().toOSString(), reportDebugInformation, TITANDebugConsole.getConsole()));
		command.add('-' + VERSION_CHECK_FLAG);
		titanJob.addCommand(command, JOB_TITLE);

		titanJob.schedule();
	}
}
