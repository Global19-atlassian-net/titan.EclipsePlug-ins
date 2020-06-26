/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.tabpages.maincontroller;

import static org.eclipse.titan.common.utils.StringUtils.isNullOrEmpty;
import static org.eclipse.titan.executor.GeneralConstants.CONFIGFILEPATH;
import static org.eclipse.titan.executor.GeneralConstants.EXECUTABLEFILEPATH;
import static org.eclipse.titan.executor.GeneralConstants.EXECUTECONFIGFILEONLAUNCH;
import static org.eclipse.titan.executor.GeneralConstants.PROJECTNAME;
import static org.eclipse.titan.executor.GeneralConstants.WORKINGDIRECTORYPATH;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.titan.common.fieldeditors.TITANResourceLocator;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.parsers.cfg.ConfigFileHandler;
import org.eclipse.titan.common.path.PathConverter;
import org.eclipse.titan.common.path.TITANPathUtilities;
import org.eclipse.titan.common.utils.IOUtils;
import org.eclipse.titan.executor.TITANConsole;
import org.eclipse.titan.executor.TITANDebugConsole;
import org.eclipse.titan.executor.designerconnection.DesignerHelper;
import org.eclipse.titan.executor.designerconnection.DynamicLinkingHelper;
import org.eclipse.titan.executor.designerconnection.EnvironmentHelper;
import org.eclipse.titan.executor.graphics.ImageCache;
import org.eclipse.titan.executor.tabpages.testset.TestSetTab;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author Kristof Szabados
 * */
public abstract class BaseMainControllerTab extends AbstractLaunchConfigurationTab {
	protected static final String EMPTY = "";
	private static final String NAME = "Basic Main Controller options";
	private static final String PROJECT = "Project (REQUIRED):";
	private static final String PROJECT_TOOLTIP =
			"This field is required.\n" +
					"When an existing project is selected and the Designer plug-in is also present the working directory " +
					"and executable fields are filled out automatically\n  with the values set as project properties.";
	private static final String WORKING_DIR = "Working directory:";
	private static final String WORKING_DIR_REQUIRED = "Working directory (REQUIRED):";
	private static final String WORKING_DIR_TOOLTIP = "The directory the main controller should be started from.";
	private static final String EXECUTABLE = "Executable (REQUIRED):";
	private static final String EXECUTABLE_TOOLTIP =
			"This field is required.\nThe executable file used to make the creation and validation of testsets possible.";
	private static final String EXECUTABLE_REQUIRED = "Executable (REQUIRED):";
	private static final String EXECUTABLE_REQUIRED_TOOLTIP =
			"The executable file used to execute testcases and to make the creation and validation of testsets possible.";
	private static final String CONFIGFILE = "Configuration file (REQUIRED):";
	private static final String CONFIGFILE_TOOLTIP = "This field is required.\n" +
			"The runtime configuration file used to describe the runtime behaviour of the executable test program.";
	private static final String BROWSE_WORKSPACE = "Browse Workspace..";

	private final class BasicProjectSelectorListener extends SelectionAdapter implements ModifyListener {

		@Override
		public void modifyText(final ModifyEvent e) {
			final Object source = e.getSource();
			if (null == source) {
				return;
			}
			if (source.equals(projectNameText)) {
				handleProjectNameModified();
			} else if (source.equals(workingdirectoryText.getTextControl(workingDirGroup))) {
				handleWorkingDirectoryModified();
			} else if (source.equals(executableFileText.getTextControl(executableGroup))) {
				handleExecutableModified();
			} else if (source.equals(configurationFileText.getTextControl(configFileGroup))) {
				handleConfigurationModified();
			}
			updateLaunchConfigurationDialog();
		}

		@Override
		public void widgetSelected(final SelectionEvent e) {
			final Object source = e.getSource();
			if (null == source) {
				return;
			}
			if (source.equals(projectSelectionButton)) {
				handleProjectButtonSelected();
			}

			updateLaunchConfigurationDialog();
		}
	}

	private BasicProjectSelectorListener generalListener;
	private ILaunchConfigurationTabGroup tabGroup;
	private ILaunchConfigurationWorkingCopy lastConfiguration;

	protected Text projectNameText;
	private Group workingDirGroup;
	protected TITANResourceLocator workingdirectoryText;
	private Group executableGroup;
	protected TITANResourceLocator configurationFileText;
	private Group configFileGroup;
	protected TITANResourceLocator executableFileText;
	private Button projectSelectionButton;
	private Button automaticExecuteSectionExecution;
	protected boolean projectIsValid;
	protected boolean workingDirectoryIsValid;
	protected boolean configurationFileIsValid;
	protected boolean executableFileIsValid;
	protected boolean executableIsExecutable;
	protected boolean executableIsForSingleMode = false;

	protected boolean workingDirectoryRequired = false;
	protected boolean executableRequired = false;

	protected List<Throwable> exceptions = new ArrayList<Throwable>();

	protected BaseMainControllerTab(final ILaunchConfigurationTabGroup tabGroup) {
		this.tabGroup = tabGroup;
		generalListener = new BasicProjectSelectorListener();
		projectIsValid = true;
		workingDirectoryIsValid = false;
		configurationFileIsValid = false;
		executableFileIsValid = false;
	}

	@Override
	public final void createControl(final Composite parent) {
		final Composite pageComposite = new Composite(parent, SWT.NONE);
		final GridLayout pageCompositeLayout = new GridLayout();
		pageCompositeLayout.numColumns = 1;
		pageComposite.setLayout(pageCompositeLayout);
		final GridData pageCompositeGridData = new GridData();
		pageCompositeGridData.horizontalAlignment = GridData.FILL;
		pageCompositeGridData.grabExcessHorizontalSpace = true;
		pageCompositeGridData.grabExcessVerticalSpace = true;
		pageComposite.setLayoutData(pageCompositeGridData);

		createProjectEditor(pageComposite);
		createWorkingdirectoryEditor(pageComposite);
		createExecutableEditor(pageComposite);
		createConfigurationEditor(pageComposite);
		setControl(pageComposite);
	}

	@Override
	public final String getName() {
		return NAME;
	}

	@Override
	public final Image getImage() {
		return ImageCache.getImage("titan.gif");
	}

	@Override
	public final void initializeFrom(final ILaunchConfiguration configuration) {
		try {
			lastConfiguration = configuration.getWorkingCopy();
			String temp = configuration.getAttribute(PROJECTNAME, EMPTY);
			if (!temp.equals(projectNameText.getText())) {
				projectNameText.setText(temp);
			}
			temp = configuration.getAttribute(WORKINGDIRECTORYPATH, EMPTY);
			if (!temp.equals(workingdirectoryText.getStringValue())) {
				workingdirectoryText.setStringValue(temp);
			}
			temp = configuration.getAttribute(EXECUTABLEFILEPATH, EMPTY);
			if (!temp.equals(executableFileText.getStringValue())) {
				executableFileText.setStringValue(temp);
			}
			temp = configuration.getAttribute(CONFIGFILEPATH, EMPTY);
			if (!temp.equals(configurationFileText.getStringValue())) {
				configurationFileText.setStringValue(temp);
			}

			final boolean tempBoolean = configuration.getAttribute(EXECUTECONFIGFILEONLAUNCH, false);
			if (tempBoolean != automaticExecuteSectionExecution.getSelection()) {
				automaticExecuteSectionExecution.setSelection(tempBoolean);
			}

			final IProject project = getProject();
			if (project == null) {
				return;
			}

			final String projectPath = project.getLocation().toOSString(); //TODO should use URI based addresses
			workingdirectoryText.setRootPath(projectPath);
			configurationFileText.setRootPath(projectPath);
			executableFileText.setRootPath(projectPath);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
	}

	@Override
	public final void performApply(final ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(PROJECTNAME, projectNameText.getText());
		configuration.setAttribute(WORKINGDIRECTORYPATH, workingdirectoryText.getStringValue());
		configuration.setAttribute(EXECUTABLEFILEPATH, executableFileText.getStringValue());
		configuration.setAttribute(CONFIGFILEPATH, configurationFileText.getStringValue());
		configuration.setAttribute(EXECUTECONFIGFILEONLAUNCH, automaticExecuteSectionExecution.getSelection());

		final IProject project = getProject();
		configuration.setMappedResources(new IResource[] {project});
	}

	@Override
	public final void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(PROJECTNAME, EMPTY);
		configuration.setAttribute(WORKINGDIRECTORYPATH, EMPTY);
		configuration.setAttribute(EXECUTABLEFILEPATH, EMPTY);
		configuration.setAttribute(CONFIGFILEPATH, EMPTY);
		configuration.setAttribute(EXECUTECONFIGFILEONLAUNCH, false);
		configuration.setMappedResources(new IResource[0]);
	}

	protected final void createProjectEditor(final Composite parent) {
		final Font font = parent.getFont();
		final Group group = new Group(parent, SWT.NONE);
		group.setText(PROJECT);
		group.setToolTipText(PROJECT_TOOLTIP);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setFont(font);
		projectNameText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		projectNameText.setLayoutData(gd);
		projectNameText.setFont(font);
		projectNameText.addModifyListener(generalListener);
		projectSelectionButton = createPushButton(group, BROWSE_WORKSPACE, null);
		projectSelectionButton.addSelectionListener(generalListener);
	}

	protected final void createWorkingdirectoryEditor(final Composite parent) {
		final Font font = parent.getFont();
		workingDirGroup = new Group(parent, SWT.NONE);
		if (workingDirectoryRequired) {
			workingDirGroup.setText(WORKING_DIR_REQUIRED);
		} else {
			workingDirGroup.setText(WORKING_DIR);
		}
		workingDirGroup.setToolTipText(WORKING_DIR_TOOLTIP);
		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		workingDirGroup.setLayoutData(gd);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		workingDirGroup.setLayout(layout);
		workingDirGroup.setFont(font);

		final IProject project = getProject();
		if (project == null) {
			workingdirectoryText = new TITANResourceLocator("working directory:", workingDirGroup, IResource.FOLDER, "");
		} else {
			workingdirectoryText = new TITANResourceLocator("working directory:", workingDirGroup, IResource.FOLDER, getProject().getLocation().toOSString());
		}
		workingdirectoryText.getLabelControl(workingDirGroup).setToolTipText(
				"The location of the working directory. Where the build process will take place");
		workingdirectoryText.getTextControl(workingDirGroup).addModifyListener(generalListener);
	}

	protected final void createExecutableEditor(final Composite parent) {
		final Font font = parent.getFont();
		executableGroup = new Group(parent, SWT.NONE);
		if (executableRequired) {
			executableGroup.setText(EXECUTABLE_REQUIRED);
			executableGroup.setToolTipText(EXECUTABLE_REQUIRED_TOOLTIP);
		} else {
			executableGroup.setText(EXECUTABLE);
			executableGroup.setToolTipText(EXECUTABLE_TOOLTIP);
		}

		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		executableGroup.setLayoutData(gd);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		executableGroup.setLayout(layout);
		executableGroup.setFont(font);

		final IProject project = getProject();
		if (project == null) {
			executableFileText = new TITANResourceLocator(EXECUTABLE, executableGroup, IResource.FILE, "");
		} else {
			executableFileText = new TITANResourceLocator(EXECUTABLE, executableGroup, IResource.FILE, getProject().getLocation().toOSString());
		}
		executableFileText.getLabelControl(executableGroup).setToolTipText(EXECUTABLE_TOOLTIP);
		executableFileText.getTextControl(executableGroup).addModifyListener(generalListener);
	}

	protected final void createConfigurationEditor(final Composite parent) {
		final Font font = parent.getFont();
		configFileGroup = new Group(parent, SWT.NONE);
		configFileGroup.setText(CONFIGFILE);
		configFileGroup.setToolTipText(CONFIGFILE_TOOLTIP);
		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		configFileGroup.setLayoutData(gd);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		configFileGroup.setLayout(layout);
		configFileGroup.setFont(font);

		final IProject project = getProject();
		if (project == null) {
			configurationFileText = new TITANResourceLocator(CONFIGFILE, configFileGroup, IResource.FILE, "");
		} else {
			configurationFileText = new TITANResourceLocator(CONFIGFILE, configFileGroup, IResource.FILE, getProject().getLocation().toOSString());
		}
		configurationFileText.getLabelControl(configFileGroup).setToolTipText(CONFIGFILE_TOOLTIP);
		configurationFileText.getTextControl(configFileGroup).addModifyListener(generalListener);

		automaticExecuteSectionExecution = new Button(configFileGroup, SWT.CHECK);
		automaticExecuteSectionExecution.setText("Execute automatically");
		automaticExecuteSectionExecution.setToolTipText("Execute the `EXECUTE' section of the configuration file automatically when launched");
		automaticExecuteSectionExecution.setSelection(false);
		automaticExecuteSectionExecution.addSelectionListener(generalListener);
		automaticExecuteSectionExecution.setEnabled(true);
	}

	/**
	 * Show a dialog that lets the user select a project. This in turn provides context for the main type, allowing the user to key a main type name,
	 * or constraining the search for main types to the specified project.
	 */
	protected final void handleProjectButtonSelected() {
		final ILabelProvider labelProvider = new WorkbenchLabelProvider();
		final ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle("Project selection");
		dialog.setMessage("Select a project to constrain your search.");
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		final List<IProject> availableProjects = new ArrayList<IProject>(projects.length);
		for (final IProject project : projects) {
			try {
				if (project.isAccessible() && project.hasNature(DesignerHelper.NATURE_ID)) {
					availableProjects.add(project);
				}
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace(e);
			}
		}
		dialog.setElements(availableProjects.toArray(new IProject[availableProjects.size()]));
		if (dialog.open() == Window.OK) {
			final String projectName = ((IProject) dialog.getFirstResult()).getName();
			if (!projectName.equals(projectNameText.getText())) {
				projectNameText.setText(projectName);
			}
		}
	}

	/**
	 * @return the project selected or null if none.
	 * */
	public final IProject getProject() {
		if (projectNameText == null) {
			return null;
		}

		final String projectName = projectNameText.getText();
		final IProject projectWithTitanNature = DynamicLinkingHelper.getProject(projectName);
		if (projectWithTitanNature != null) {
			projectIsValid = true;
		}
		return projectWithTitanNature;
	}

	private void handleProjectNameModified() {
		final IProject project = getProject();
		if (project == null) {
			projectIsValid = false;
			return;
		}

		projectIsValid = true;
		workingdirectoryText.setStringValue(getRAWWorkingDirectoryForProject(project));
		final String executable = getExecutableForProject(project);
		if (executable != null) {
			executableFileText.setStringValue(executable);
		}
	}

	protected final void handleWorkingDirectoryModified() {
		if (!EMPTY.equals(workingdirectoryText.getStringValue())) {
			final IProject project = getProject();
			URI uri;
			if (project == null) {
				uri = URIUtil.toURI(workingdirectoryText.getStringValue());
			} else {
				uri = TITANPathUtilities.resolvePathURI(workingdirectoryText.getStringValue(), getProject().getLocation().toOSString());
			}

			final File file = new File(uri);
			if (file.exists() && file.isDirectory()) {
				workingDirectoryIsValid = true;
				return;
			}
			workingDirectoryIsValid = false;
		}
	}

	public static final class ExecutableCalculationHelper {
		public boolean executableFileIsValid = false;
		public boolean executableIsExecutable = false;
		public boolean executableIsForSingleMode = false;
		public List<String> availableTestcases = new ArrayList<String>();

		/**
		 * Checks that the executable is existing, executable and of the right kind.
		 * Also extracts the available testcases.
		 *
		 * @param configuration the configuration to get the environmental variables from.
		 * @param project the project to be used.
		 * @param executableFileName the resolved path of the executable.
		 *
		 * @return the information extracted.
		 * */
		public static ExecutableCalculationHelper checkExecutable(final ILaunchConfiguration configuration, final IProject project, final URI executableFileName) {
			final ExecutableCalculationHelper result = new ExecutableCalculationHelper();

			final File file = new File(executableFileName);
			if (!file.exists() || !file.isFile()) {
				return result;
			}

			final ProcessBuilder pb = new ProcessBuilder();
			final Map<String, String> env = pb.environment();
			Map<String, String> tempEnvironmentalVariables;

			try {
				tempEnvironmentalVariables = (HashMap<String, String>) configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>());
				EnvironmentHelper.resolveVariables(env, tempEnvironmentalVariables);
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace(e);
				result.executableFileIsValid = false;
			}

			if (project != null) {
				final IProject actualProject = DynamicLinkingHelper.getProject(project.getName());
				if (actualProject != null) {
					EnvironmentHelper.setTitanPath(env);
					EnvironmentHelper.set_LICENSE_FILE_PATH(env);
					EnvironmentHelper.set_LD_LIBRARY_PATH(DynamicLinkingHelper.getProject(actualProject.getName()), env);
				}
			}

			Process proc;
			final StringBuilder output = new StringBuilder();
			final String exename = PathConverter.convert(file.getPath(), true, output);
			TITANDebugConsole.println(output);

			StringBuilder lastParam = new StringBuilder(exename);
			lastParam.append(" -v");
			List<String> shellCommand;
			shellCommand = new ArrayList<String>(3);
			shellCommand.add("sh");
			shellCommand.add("-c");
			shellCommand.add(lastParam.toString());
			// "sh", "-c", "exename", "-v" doesn't work :(   It has to be
			// "sh", "-c", "exename -v"
			for (final String c : shellCommand) {
				TITANConsole.print(c + ' ');
			}
			TITANConsole.println("");
			pb.command(shellCommand);
			final Pattern singlePattern = Pattern.compile("TTCN-3 Test Executor \\(single mode\\).*");
			result.executableIsForSingleMode = false;
			BufferedReader stderr = null;
			try {
				proc = pb.start();

				stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line = stderr.readLine();
				while (line != null) {
					if (singlePattern.matcher(line).matches()) {
						result.executableIsForSingleMode = true;
					}
					line = stderr.readLine();
				}
				proc.waitFor();
				result.executableFileIsValid = true;
			} catch (IOException e) {
				ErrorReporter.logExceptionStackTrace(e);
				result.executableFileIsValid = false;
			} catch (InterruptedException e) {
				ErrorReporter.logExceptionStackTrace(e);
				result.executableFileIsValid = false;
			} finally {
				IOUtils.closeQuietly(stderr);
			}

			// ProcessBuilder.command did not make a copy but took a reference;
			// changes to shellCommand continue to affect the variable pb.
			lastParam = new StringBuilder(exename);
			lastParam.append(" -l");
			// replace the last parameter
			shellCommand.set(shellCommand.size() - 1, lastParam.toString());
			for (final String c : shellCommand) {
				TITANConsole.print(c + ' ');
			}
			TITANConsole.println("");

			BufferedReader stdout = null;
			try {
				proc = pb.start();

				stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line;
				while ((line = stdout.readLine()) != null) {
					result.availableTestcases.add(line);
					TITANConsole.println(line);
				}

				final int exitval = proc.waitFor();
				if (exitval != 0 && stderr.ready()) {
					TITANConsole.println("Testing of the executable failed");
					TITANConsole.println("  with value:" + exitval);
					TITANConsole.println("Sent the following error messages:");
					while ((line = stderr.readLine()) != null) {
						TITANConsole.println(line);
					}

					result.executableIsExecutable = false;
				} else {
					result.executableIsExecutable = true;
				}
				result.executableFileIsValid = true;
			} catch (IOException e) {
				ErrorReporter.logExceptionStackTrace(e);
				result.executableFileIsValid = false;
			} catch (InterruptedException e) {
				ErrorReporter.logExceptionStackTrace(e);
				result.executableFileIsValid = false;
			} finally {
				IOUtils.closeQuietly(stdout, stderr);
			}

			return result;
		}
	}

	protected final void handleExecutableModified() {
		if (EMPTY.equals(executableFileText.getStringValue())) {
			return;
		}

		BusyIndicator.showWhile(null, new Runnable() {
			@Override
			public void run() {
				final IProject project = getProject();
				URI uri;
				if (project == null) {
					uri = URIUtil.toURI(executableFileText.getStringValue());
				} else {
					uri = TITANPathUtilities.resolvePathURI(executableFileText.getStringValue(), getProject().getLocation().toOSString());
				}

				final ExecutableCalculationHelper helper = ExecutableCalculationHelper.checkExecutable(lastConfiguration, DynamicLinkingHelper.getProject(projectNameText.getText()), uri);
				executableFileIsValid = helper.executableFileIsValid;
				executableIsExecutable = helper.executableIsExecutable;
				executableIsForSingleMode = helper.executableIsForSingleMode;
				final List<String> availableTestcases = helper.availableTestcases;

				// find the testset tab
				final ILaunchConfigurationTab[] tabs = tabGroup.getTabs();
				TestSetTab testSetTab = null;
				for (final ILaunchConfigurationTab tab : tabs) {
					if (tab instanceof TestSetTab) {
						testSetTab = (TestSetTab) tab;
					}
				}

				if (testSetTab != null) {
					testSetTab.setAvailableTestcases(availableTestcases.toArray(new String[availableTestcases.size()]));
				}
			}
		});

	}

	protected final void handleConfigurationModified() {
		if (EMPTY.equals(configurationFileText.getStringValue())) {
			automaticExecuteSectionExecution.setEnabled(false);
			return;
		}

		final IProject project = getProject();
		URI uri;
		if (project == null) {
			uri = URIUtil.toURI(configurationFileText.getStringValue());
		} else {
			uri = TITANPathUtilities.resolvePathURI(configurationFileText.getStringValue(), getProject().getLocation().toOSString());
		}

		final File file = new File(uri);
		if (file.exists() && file.isFile()) {
			exceptions.clear();
			final ConfigFileHandler configHandler = new ConfigFileHandler();
			configHandler.readFromFile(file.getPath());
			if (configHandler.parseExceptions().isEmpty()) {
				final Map<String, String> env = new HashMap<String, String>(System.getenv());
				Map<String, String> tempEnvironmentalVariables;

				try {
					tempEnvironmentalVariables = lastConfiguration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<String, String>());
					EnvironmentHelper.resolveVariables(env, tempEnvironmentalVariables);
					configHandler.setEnvMap(env);
					configHandler.processASTs();
				} catch (CoreException e) {
					exceptions.add(e);
					configurationFileIsValid = false;
				}
			}
			exceptions.addAll(configHandler.parseExceptions());

			if (exceptions.isEmpty()) {
				configurationFileIsValid = true;
				automaticExecuteSectionExecution.setEnabled(true);
				return;
			}
		}
		configurationFileIsValid = false;
		exceptions.clear();
		exceptions.add(new Exception("The path `" + URIUtil.toPath(uri) + "' does not seem to be correct."));
		automaticExecuteSectionExecution.setEnabled(false);
	}

	/**
	 * Calculates the working directory of the provided project.
	 *
	 * @param project the project to use.
	 *
	 * @return the working directory.
	 * */
	public static String getRAWWorkingDirectoryForProject(final IProject project) {
		try {
			final String workingDirectory = project.getPersistentProperty(
					new QualifiedName(DesignerHelper.PROJECT_BUILD_PROPERTYPAGE_QUALIFIER,	DesignerHelper.WORKINGDIR_PROPERTY));
			return workingDirectory;
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		return "bin";
	}

	/**
	 * Calculates the executable of the provided project.
	 *
	 * @param project the project to use.
	 *
	 * @return the executable.
	 * */
	public static String getExecutableForProject(final IProject project) {
		try {
			final String executable = project.getPersistentProperty(
					new QualifiedName(DesignerHelper.PROJECT_BUILD_PROPERTYPAGE_QUALIFIER,	DesignerHelper.EXECUTABLE_PROPERTY));
			if (executable != null) {
				return executable;
			}
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		return "";
	}

	@Override
	public boolean canSave() {
		if (!EMPTY.equals(projectNameText.getText()) && !projectIsValid) {
			return false;
		}
		if (EMPTY.equals(workingdirectoryText.getStringValue()) || !workingDirectoryIsValid) {
			return false;
		}

		return super.canSave();
	}

	@Override
	public boolean isValid(final ILaunchConfiguration launchConfig) {

		if (!EMPTY.equals(projectNameText.getText()) && !projectIsValid) {
			setErrorMessage("The name of the project is not valid.");
			return false;
		}

		if (EMPTY.equals(workingdirectoryText.getStringValue())) {
			setErrorMessage("The working directory must be set.");
			return false;
		} else if (!workingDirectoryIsValid) {
			setErrorMessage("The working directory is not valid.");
			return false;
		}

		if(EMPTY.equals(configurationFileText.getStringValue())) {
			setErrorMessage("The configuration file must be set."); //<<<<== This should be set to "setErrorMessage(null);"
																	//if the cfg file is not mandatory !
			return false; // <<<<== This can be set to true if the cfg file is not mandatory !
		} else if (!configurationFileIsValid) {
			if (null != exceptions && !exceptions.isEmpty()) {
				setErrorMessage("Problem in config file: " + exceptions.get(0).getMessage());
			} else {
				setErrorMessage("The configuration file is not valid.");
			}
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	/**
	 * Initializes the provided launch configuration for a general TITAN based execution.
	 *
	 * @param configuration the configuration to initialize.
	 * @param project the project to gain data from.
	 * @param configFilePath the path of the configuration file.
	 * @param singleMode whether the execution should happen in single mode or not.
	 * */
	public static boolean initLaunchConfiguration(final ILaunchConfigurationWorkingCopy configuration,
	                                              final IProject project, final String configFilePath, final boolean singleMode) {

		configuration.setAttribute(PROJECTNAME, project.getName());
		final String workingDirectory = getRAWWorkingDirectoryForProject(project);
		if (isNullOrEmpty(workingDirectory)) {
			ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The working directory must be set.");
			return false;
		}

		final URI uri = TITANPathUtilities.resolvePathURI(workingDirectory, project.getLocation().toOSString());
		File file = new File(uri);
		if (!file.exists()) {
			ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The working directory does not exist.");
			return false;
		}
		if (!file.isDirectory()) {
			ErrorReporter.parallelErrorDisplayInMessageDialog(
				"An error was found while creating the default launch configuration for project " + project.getName(),
				"The path set as working directory does not point to a folder.");
			return false;
		}
		configuration.setAttribute(WORKINGDIRECTORYPATH, workingDirectory);

		final String executable = getExecutableForProject(project);
		ExecutableCalculationHelper helper;
		final URI uri2 = TITANPathUtilities.resolvePathURI(executable, project.getLocation().toOSString());
		if (null != executable && executable.length() > 0) {
			file = new File(uri2);
			helper = ExecutableCalculationHelper.checkExecutable(configuration, project, uri2);
			if (!file.exists()) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The executable file does not exist.");
				return false;
			}
			if (file.isDirectory()) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The file set as the executable is a folder.");
				return false;
			}
			if (!helper.executableFileIsValid) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The executable file is not valid.");
				return false;
			}
		} else {
			helper = new ExecutableCalculationHelper();
		}
		if (!helper.executableIsExecutable) {
			ErrorReporter.parallelErrorDisplayInMessageDialog(
				"An error was found while creating the default launch configuration for project " + project.getName(),
				"The executable is not actually executable. Please set an executable generated for parallel mode execution as the executable.");
			return false;
		}

		if (singleMode) {
			if (!helper.executableIsForSingleMode) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The executable was built for parallel mode execution, it can not be launched using a single mode launcher.");
				return false;
			}
		} else {
			if (helper.executableIsForSingleMode) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The executable was built for single mode execution, it can not be launched in a parallel mode launcher.");
				return false;
			}
		}

		configuration.setAttribute(EXECUTABLEFILEPATH, executable);
		TestSetTab.setTestcases(configuration, helper.availableTestcases.toArray(new String[helper.availableTestcases.size()]));

		if (!"".equals(configFilePath)) {
			final URI uri3 = TITANPathUtilities.resolvePathURI(configFilePath, project.getLocation().toOSString());
			file = new File(uri3);
			if (!file.exists()) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(
					"An error was found while creating the default launch configuration for project " + project.getName(),
					"The configurationfile does not exist.");
			}
			if (!file.isFile()) {
				ErrorReporter.parallelErrorDisplayInMessageDialog(
						"An error was found while creating the default launch configuration for project " + project.getName(),
						"The file set as the configuration file is a folder.");
			}
		}
		configuration.setAttribute(CONFIGFILEPATH, configFilePath);
		configuration.setAttribute(EXECUTECONFIGFILEONLAUNCH, !"".equals(configFilePath));

		return true;
	}
}
