/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.actions;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.commonFilters.ResourceExclusionHelper;
import org.eclipse.titan.designer.core.TITANJob;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 * This action invokes an external TITAN job on a selection of files.
 * 
 * @author Kristof Szabados
 */
public abstract class ExternalTitanAction extends AbstractHandler implements IWorkbenchWindowActionDelegate {
	public static final String EXECUTION_FAILED = "execution failed";
	public static final String INTERRUPTION = "execution failed because of interruption";

	protected static final String NO_VALID_FILES = "No valid files were selected.";
	protected static final String FAILURE_SUFFIX = " ...failed";

	public static final String SHELL = "sh";

	public static final String SYNTAX_CHECK_FLAG = "p";
	public static final String SEMANTIC_CHECK_FLAG = "s";
	public static final String VERSION_CHECK_FLAG = "v";
	public static final String GENERATE_TESTPORT_FLAG = "t";

	private static final String DOT = ".";
	private static final String COMPILER_SUBPATH = File.separatorChar + "bin" + File.separatorChar + "compiler";

	protected ISelection selection;

	protected Map<String, IFile> files = new HashMap<String, IFile>();
	protected File workingDir;
	protected IProject project;

	protected IProject singleSelectedProject = null;

	private final class InternalResourceVisitor implements IResourceVisitor {
		private final boolean processExludedOnes;
		private ResourceExclusionHelper helper = null;

		public InternalResourceVisitor(final boolean processExludedOnes) {
			this.processExludedOnes = processExludedOnes;
			if (!processExludedOnes) {
				helper = new ResourceExclusionHelper();
			}
		}

		@Override
		public boolean visit(final IResource resource) {
			if (!resource.isAccessible() || resource.getLocation().lastSegment().startsWith(DOT)) {
				return false;
			}
			switch (resource.getType()) {
			case IResource.FILE:
				if (!processExludedOnes) {
					if (ResourceExclusionHelper.isDirectlyExcluded((IFile) resource) || helper.isExcludedByRegexp(resource.getName())) {
						return false;
					}
				}

				final IFile file = (IFile) resource;
				files.put(file.getLocation().toOSString(), file);

				return true;
			case IResource.FOLDER:
				if (!processExludedOnes) {
					if (ResourceExclusionHelper.isDirectlyExcluded((IFolder) resource) || helper.isExcludedByRegexp(resource.getName())) {
						return false;
					}
				}
				return true;
			default:
				return true;
			}
		}
	}

	/**
	 * Initialize the files HashMap, for TITANJob.
	 * 
	 * @see TITANJob
	 * 
	 * @param window
	 *                the window that provides the context for this delegate
	 */
	@Override
	/** {@inheritDoc} */
	public final void init(final IWorkbenchWindow window) {
		if (!files.isEmpty()) {
			files.clear();
		}
	}

	/**
	 * Free the resources.
	 */
	@Override
	/** {@inheritDoc} */
	public final void dispose() {
		files.clear();
		workingDir = null;
		project = null;
	}

	protected final Path getCompilerPath() {
		final IPreferencesService prefs = Platform.getPreferencesService();
		final String pathOfTITAN = prefs.getString(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.TITAN_INSTALLATION_PATH, "", null);
		return new Path(pathOfTITAN + COMPILER_SUBPATH);

	}

	/**
	 * Removes markers that might have been previously generated by such an
	 * external TITAN action.
	 * 
	 * @see #run(IAction)
	 */
	public final void reportOnTheFlyOutdating() {
		if (project == null) {
			return;
		}
		try {
			if (!files.isEmpty()) {
				for (final IFile file : files.values()) {
					// As all of the on-the-fly markers will
					// be deleted during the build, we
					// have to mark (outdate) those files
					// with missing markers to be parsed on
					// the subsequent
					// invocation of the on-the-fly parser,
					// which happens when the user is
					// editing
					// an arbitrary file.
					if (file.findMarkers(GeneralConstants.ONTHEFLY_SYNTACTIC_MARKER, true, IResource.DEPTH_INFINITE).length != 0) {
						GlobalParser.getProjectSourceParser(project).reportOutdating(file);
					}
					if (file.findMarkers(GeneralConstants.ONTHEFLY_SEMANTIC_MARKER, true, IResource.DEPTH_INFINITE).length != 0) {
						GlobalParser.getProjectSourceParser(project).reportOutdating(file);
					}
					if (file.findMarkers(GeneralConstants.ONTHEFLY_MIXED_MARKER, true, IResource.DEPTH_INFINITE).length != 0) {
						GlobalParser.getProjectSourceParser(project).reportOutdating(file);
					}
				}
			}
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
	}

	/**
	 * Checks the flags that can be used to configure the behavior of the
	 * compiler when called by the external TITAN actions.
	 * 
	 * @return the flags
	 * */
	protected final String getTITANActionFlags() {
		final StringBuilder builder = new StringBuilder();

		final IPreferencesService prefs = Platform.getPreferencesService();
		if (prefs.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.TITANACTIONS_DEFAULT_AS_OMIT, false, null)) {
			builder.append('d');
		}

		return builder.toString();
	}

	/**
	 * Adds every file in the given resource to the list of checked files.
	 * 
	 * @param resource
	 *                The resource whose files are to be added to the list
	 *                of files to be checked
	 */
	public final void buildFileList(final IResource resource) {
		if (resource == null) {
			return;
		}

		project = resource.getProject();
		if (!project.isAccessible() || !resource.isAccessible()) {
			return;
		}

		workingDir = new File(project.getLocation().toOSString());

		final IPreferencesService prefs = Platform.getPreferencesService();
		final boolean processExludedOnes = prefs.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER,
				PreferenceConstants.TITANACTIONS_PROCESSEXCLUDEDRESOURCES, true, null);

		try {
			// Is the source already in an excluded folder ?
			if (!processExludedOnes) {
				IContainer parent = resource.getParent();
				while (parent instanceof IFolder) {
					if (ResourceExclusionHelper.isDirectlyExcluded((IFolder) parent)) {
						return;
					}

					parent = parent.getParent();
				}
			}
			resource.accept(new InternalResourceVisitor(processExludedOnes));
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
	}

	/**
	 * This function processes the previously stored selection. It should be
	 * called just before the processed data (list of selected resources) is
	 * needed as this might take some time.
	 * */
	protected final void processSelection() {
		singleSelectedProject = null;

		/**
		 * This is needed because AbstractHandler does not deal with
		 * selection, and selectionChanged is not called.
		 */
		final IWorkbenchPage iwPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		selection = iwPage.getSelection();

		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structSelection = (IStructuredSelection) selection;

			files = new HashMap<String, IFile>();

			final List<?> selectionList = structSelection.toList();
			if (selectionList.size() == 1) {
				if (selectionList.get(0) instanceof IProject) {
					singleSelectedProject = (IProject) selectionList.get(0);
				}
			}

			for (final Object selected : selectionList) {
				if (selected instanceof IFile) {
					buildFileList((IFile) selected);
				} else if (selected instanceof IFolder) {
					buildFileList((IFolder) selected);
				} else if (selected instanceof IProject) {
					buildFileList((IProject) selected);
				}
			}
		}
	}

	/**
	 * This method tells the action what the user has selected.
	 * 
	 * @param action
	 *                the action proxy that handles presentation portion of
	 *                the action
	 * @param selection
	 *                the current selection, or <code>null</code> if there
	 *                is no selection.
	 */
	@Override
	/** {@inheritDoc} */
	public final void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}
}
