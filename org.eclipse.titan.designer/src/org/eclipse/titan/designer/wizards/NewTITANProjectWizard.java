/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.core.TITANNature;
import org.eclipse.titan.designer.productUtilities.ProductConstants;
import org.eclipse.titan.designer.properties.data.CCompilerOptionsData;
import org.eclipse.titan.designer.properties.data.FolderBuildPropertyData;
import org.eclipse.titan.designer.properties.data.MakeAttributesData;
import org.eclipse.titan.designer.properties.data.MakefileCreationData;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;
import org.eclipse.titan.designer.properties.data.ProjectDocumentHandlingUtility;
import org.eclipse.titan.designer.properties.data.ProjectFileHandler;
import org.eclipse.titan.designer.properties.data.TTCN3PreprocessorOptionsData;
import org.eclipse.titan.designer.samples.SampleProject;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/**
 * @author Kristof Szabados
 * */
public final class NewTITANProjectWizard extends BasicNewResourceWizard implements IExecutableExtension {

	public static final String NEWTITANPROJECTWIZARD = ProductConstants.PRODUCT_ID_DESIGNER + ".wizards.NewTITANProjectWizard";

	private NewTITANProjectCreationPage mainPage;
	private NewTITANProjectOptionsWizardPage optionsPage;
	private NewTITANProjectContentPage contentPage;
	private IProject newProject;
	private static final String NEWPROJECT_WINDOWTITLE = "New TITAN Project";
	private static final String NEWPROJECT_TITLE = "Create a TITAN Project";
	private static final String NEWPROJECT_DESCRIPTION = "Create a new TITAN project in the workspace or in an external location";
	private static final String CREATING_PROJECT = "creating project";
	private static final String CREATION_FAILED = "Project creation failed";
	private static final String TRUE = "true";

	private boolean isCreated;
	private IConfigurationElement config;

	public NewTITANProjectWizard() {
		isCreated = false;
	}

	@Override
	public void addPages() {
		super.addPages();

		mainPage = new NewTITANProjectCreationPage(NEWPROJECT_WINDOWTITLE);
		mainPage.setTitle(NEWPROJECT_TITLE);
		mainPage.setDescription(NEWPROJECT_DESCRIPTION);
		addPage(mainPage);
		optionsPage = new NewTITANProjectOptionsWizardPage();
		addPage(optionsPage);
		contentPage = new NewTITANProjectContentPage();
		addPage(contentPage);
	}

	/**
	 * @return the path of the project to be created.
	 * */
	IPath getProjectPath() {
		final IPath path = mainPage.getLocationPath();
		final String name = mainPage.getProjectName();

		return path.append(name);
	}

	/**
	 * Creating a new project.
	 * 
	 * @return the new project created.
	 */
	private IProject createNewProject() {
		final IProject tempProjectHandle = mainPage.getProjectHandle();

		URI location = null;
		if (!mainPage.useDefaults()) {
			location = mainPage.getLocationURI();
		}

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final String tempExecutableName = tempProjectHandle.getName();

		final IProject newProjectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(tempExecutableName);

		final IProjectDescription description = workspace.newProjectDescription(tempExecutableName);
		description.setLocationURI(location);
		TITANNature.addTITANNatureToProject(description);

		final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(final IProgressMonitor monitor) throws CoreException {
				createProject(description, newProjectHandle, monitor);

				String sourceFolder = optionsPage.getSourceFolder();
				if (!"".equals(sourceFolder)) {
					IFolder folder = newProjectHandle.getFolder(sourceFolder);
					if (!folder.exists()) {
						try {
							folder.create(true, true, null);
						} catch (CoreException e) {
							ErrorReporter.logExceptionStackTrace(e);
						}
					}
					final SampleProject sample = contentPage.getSampleProject();
					if (sample != null) {
						sample.setupProject(newProjectHandle.getProject(), folder);
						ProjectFileHandler pfHandler = new ProjectFileHandler(newProjectHandle.getProject());
						pfHandler.saveProjectSettings();
					}
					if (optionsPage.isExcludeFromBuildSelected()) {
						folder.setPersistentProperty(new QualifiedName(FolderBuildPropertyData.QUALIFIER,
								FolderBuildPropertyData.EXCLUDE_FROM_BUILD_PROPERTY), TRUE);
					}
				}

				newProjectHandle.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						ProjectBuildPropertyData.GENERATE_MAKEFILE_PROPERTY), TRUE);
				newProjectHandle.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY), "cpp");
				newProjectHandle.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						CCompilerOptionsData.CXX_COMPILER_PROPERTY), "g++");
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return null;
		} catch (final InvocationTargetException e) {
			final Throwable t = e.getTargetException();
			if (t != null) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(CREATION_FAILED, t.getMessage());
			}
			return null;
		}

		newProject = newProjectHandle;

		return newProject;
	}

	/**
	 * Creating a new project.
	 * 
	 * @param description
	 *                - IProjectDescription that belongs to the newly
	 *                created project.
	 * @param projectHandle
	 *                - a project handle that is used to create the new
	 *                project.
	 * @param monitor
	 *                - reference to the monitor object
	 * @exception CoreException
	 *                    thrown if access to the resources throws a
	 *                    CoreException.
	 * @exception OperationCanceledException
	 *                    if the operation was canceled by the user.
	 */
	protected void createProject(final IProjectDescription description, final IProject projectHandle, final IProgressMonitor monitor)
			throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 101);
		try {
			progress.setTaskName(CREATING_PROJECT);

			projectHandle.create(description, progress.newChild(50));

			if (progress.isCanceled()) {
				throw new OperationCanceledException();
			}

			projectHandle.open(IResource.BACKGROUND_REFRESH, progress.newChild(50));

			projectHandle.refreshLocal(IResource.DEPTH_ONE, progress.newChild(1));
			isCreated = true;
		} finally {
			progress.done();
		}
	}

	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setNeedsProgressMonitor(true);
		setWindowTitle(NEWPROJECT_WINDOWTITLE);
	}

	@Override
	public boolean performFinish() {
		Activator.getDefault().pauseHandlingResourceChanges();

		if (!isCreated) {
			createNewProject();
		}

		if (newProject == null) {
			Activator.getDefault().resumeHandlingResourceChanges();

			return false;
		}

		try {
			newProject.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakeAttributesData.TEMPORAL_WORKINGDIRECTORY_PROPERTY), optionsPage.getWorkingFolder());

			final String executable = MakefileCreationData.getDefaultTargetExecutableName(newProject);

			newProject.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					MakefileCreationData.TARGET_EXECUTABLE_PROPERTY), executable);
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		}

		ProjectDocumentHandlingUtility.createDocument(newProject);
		ProjectFileHandler pfHandler;
		pfHandler = new ProjectFileHandler(newProject);
		final WorkspaceJob job = pfHandler.saveProjectSettingsJob();

		try {
			job.join();
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		Activator.getDefault().resumeHandlingResourceChanges();

		try {
			newProject.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		try {
			TITANNature.addTITANBuilderToProject(newProject);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		BasicNewProjectResourceWizard.updatePerspective(config);
		selectAndReveal(newProject);

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				final PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(null, newProject,
						GeneralConstants.PROJECT_PROPERTY_PAGE, null, null);
				if (dialog != null) {
					dialog.open();
				}
			}
		});

		return true;
	}

	@Override
	public void setInitializationData(final IConfigurationElement config, final String propertyName, final Object data) {
		this.config = config;
	}
}
