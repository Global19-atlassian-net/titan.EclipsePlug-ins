/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.actions;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.utils.SelectionUtils;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.core.TITANNature;
import org.eclipse.titan.designer.properties.data.CCompilerOptionsData;
import org.eclipse.titan.designer.properties.data.MakeAttributesData;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;
import org.eclipse.titan.designer.properties.data.ProjectFileHandler;
import org.eclipse.titan.designer.properties.data.TITANFlagsOptionsData;
import org.eclipse.titan.designer.properties.data.TTCN3PreprocessorOptionsData;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * This class can toggle the TITANNature on a project.
 * <p>
 * The TITANBuilder is also toggled at the same time.
 *
 * @author Kristof Szabados
 */
public final class NatureConverter extends AbstractHandler implements IObjectActionDelegate {
	private static final String NATURE_REMOVAL_TITLE = "TITAN Nature removal";
	private static final String NATURE_REMOVAL_MESSAGE = "Are you sure you wish to remove the TITAN Nature from project ";
	private ISelection selection;

	/**
	 * This method toogles the TITANNature on every project selected project.
	 *
	 * If the user is removing the TITAN nature a dialog is displayed asking the user if he is really willing to do this.
	 *
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 *
	 * @param action the action proxy that would handle the presentation portion of the action.
	 *   Not used.
	 */
	@Override
	/** {@inheritDoc} */
	public void run(final IAction action) {
		doConvertNature(selection);
	}

	@Override
	/** {@inheritDoc} */
	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	@Override
	/** {@inheritDoc} */
	public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
		// Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();

		doConvertNature(selection);

		return null;
	}

	/**
	 * Do the actual conversion.
	 *
	 * @param selection the objects selected to be converted
	 * */
	private void doConvertNature(final ISelection selection) {
		final List<IProject> selectedProjects = SelectionUtils.getProjectsFromSelection(selection);

		for (final IProject tempProject : selectedProjects) {
			try {
				convertNatureOnProject(tempProject);
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("Error while processing project: " + tempProject.getName(), e);
			} finally {
				Activator.getDefault().resumeHandlingResourceChanges();
			}
		}
	}

	private void convertNatureOnProject(final IProject tempProject) throws CoreException {
		final IProjectDescription description = tempProject.getDescription();

		if (!TITANNature.hasTITANNature(tempProject)) {
			Activator.getDefault().pauseHandlingResourceChanges();

			TITANNature.addTITANNatureToProject(description);
			tempProject.setDescription(description, IResource.FORCE, null);

			tempProject.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					ProjectBuildPropertyData.GENERATE_MAKEFILE_PROPERTY), "true");
			tempProject.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY), "cpp");
			tempProject.setPersistentProperty(
					new QualifiedName(ProjectBuildPropertyData.QUALIFIER, CCompilerOptionsData.CXX_COMPILER_PROPERTY), "g++");
			tempProject.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					TITANFlagsOptionsData.ADD_SOURCELINEINFO_PROPERTY), "true");
			tempProject.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakeAttributesData.TEMPORAL_WORKINGDIRECTORY_PROPERTY), "bin");

			final ProjectFileHandler pfHandler = new ProjectFileHandler(tempProject);
			final WorkspaceJob job = pfHandler.saveProjectSettingsJob();

			try {
				job.join();
			} catch (InterruptedException e) {
				ErrorReporter.logExceptionStackTrace("Interrupted", e);
			}

			try {
				tempProject.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("Error while refreshing resources", e);
			}
			Activator.getDefault().resumeHandlingResourceChanges();

			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					final PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(null, tempProject, GeneralConstants.PROJECT_PROPERTY_PAGE, null, null);
					if (dialog != null) {
						dialog.open();
					}
				}
			});
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (MessageDialog.openConfirm(null, NATURE_REMOVAL_TITLE, NATURE_REMOVAL_MESSAGE
							+ tempProject.getName() + '?')) {
						TITANNature.removeTITANNature(description);
						try {
							tempProject.setDescription(description, IResource.FORCE, null);
						} catch (CoreException e) {
							ErrorReporter.logExceptionStackTrace(e);
						}
					}
				}
			});
		}
	}
}
