/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.log.viewer.models.LogFileMetaData;
import org.eclipse.titan.log.viewer.views.DetailsView;
import org.eclipse.titan.log.viewer.views.ILogViewerView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Help class to navigator actions
 */
public final class ActionUtils {

	private ActionUtils() {
		// Hide constructor
	}

	/**
	 * Closes all MSC and Text Table views connected to a project, a file or a folder that will be closed or deleted
	 * and clear the tabbed navigator if necessary
	 * @param activePage activePage page
	 * @param viewReferences views
	 * @param resource object
	 */
	public static void closeAssociatedViews(final IWorkbenchPage activePage, final IViewReference[] viewReferences, final IResource resource) {
		switch (resource.getType()) {
		case IResource.PROJECT:
			closeViewsForProject((IProject) resource, activePage, viewReferences);
			break;
		case IResource.FILE:
			closeViewsForFiles((IFile) resource, activePage, viewReferences);
			break;
		case IResource.FOLDER:
			closeViewsInclInFolder((IFolder) resource, activePage, viewReferences);
			break;
		default:
			break;
		}
	}

	/**
	 * Updates the log file and it's children in the project explorer view. It should be used when the test cases
	 * of the log file or the log file has changed. For example when the test case extraction is done.
	 * @param logFile
	 */
	public static void refreshLogFileInProjectsViewer(final IFile logFile) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				final IViewPart view = activePage.findView("org.eclipse.ui.navigator.ProjectExplorer");

				if (view instanceof CommonNavigator) {
					final CommonViewer viewer = ((CommonNavigator) view).getCommonViewer();
					viewer.refresh(logFile, true);
					viewer.expandToLevel(logFile, AbstractTreeViewer.ALL_LEVELS);
				}
			}
		});
	}

	/**
	 * Closes all connected views when closing/deleting a project
	 * and clear the test case tabbed navigator if necessary
	 * @param project  the selected project
	 * @param activePage activePage page
	 * @param viewReferences views
	 */
	private static void closeViewsForProject(final IProject project, final IWorkbenchPage activePage, final IViewReference[] viewReferences) {
		for (final IViewReference reference : viewReferences) {
			final IViewPart view = reference.getView(false);
			closeView(project, activePage, reference, view);
		}
	}

	private static void closeView(final IProject project, final IWorkbenchPage activePage, final IViewReference reference, final IViewPart view) {
		//	 a restored view with faulty content
		if (view == null) {
			activePage.hideView(reference);
			return;
		}

		if (!(view instanceof ILogViewerView)) {
			return;
		}

		final ILogViewerView logViewerView = (ILogViewerView) view;
		final LogFileMetaData metaData = logViewerView.getLogFileMetaData();
		if (metaData == null) {
			activePage.hideView(reference);
			return;
		}

		if (project.getName().equals(metaData.getProjectName())) {
			activePage.hideView(reference);
		}
	}

	/**
	 * Closes all connected views when closing/deleting a file
	 * and clear the test case tabbed navigator if necessary
	 * All used cache files are deleted also from the file system.
	 * @param file  the selected file
	 * @param activePage activePage page
	 * @param viewReferences views
	 */
	private static void closeViewsForFiles(final IFile file, final IWorkbenchPage activePage, final IViewReference[] viewReferences) {

		for (final IViewReference reference : viewReferences) {
			closeView(file, activePage, reference);
		}

		LogFileCacheHandler.clearCache(file);
	}

	private static void closeView(final IFile file, final IWorkbenchPage activePage, final IViewReference reference) {
		final IViewPart view = reference.getView(false);

		// a restored view with faulty content
		if (view == null) {
			activePage.hideView(reference);
			return;
		}

		if (!(view instanceof ILogViewerView)) {
			return;
		}

		final ILogViewerView logViewerView = (ILogViewerView) view;
		final LogFileMetaData metadata = logViewerView.getLogFileMetaData();
		if (metadata == null) {
			activePage.hideView(reference);
			return;
		}

		if (file.getLocationURI().equals(metadata.getFilePath())) {
			if (logViewerView instanceof DetailsView) {
				((DetailsView) logViewerView).setData(null, false);
			}
			activePage.hideView(reference);
		}
	}

	/**
	 * Closes all connected views when closing/deleting a folder
	 * and clear the test case tabbed navigator if necessary
	 * All files or folders inside the selected folder closes to.
	 * All used cache files are deleted also from the file system.
	 * @param folder  the selected folder
	 * @param activePage activePage page
	 * @param viewReferences views
	 */
	private static void closeViewsInclInFolder(final IFolder folder, final IWorkbenchPage activePage, final IViewReference[] viewReferences) {
		if (folder.isAccessible()) {
			try {
				final IResource[] resource = folder.members();

				closeViewsOfLogFiles(activePage, viewReferences, resource);
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace(e);
			}
		}

		LogFileCacheHandler.clearLogFolderCache(folder);
	}

	private static void closeViewsOfLogFiles(final IWorkbenchPage activePage, final IViewReference[] viewReferences, final IResource[] resource) {
		for (final IResource aResource : resource) {
			if (aResource instanceof IFile) {
				closeViewsForFiles((IFile) aResource, activePage, viewReferences);
			} else if (aResource instanceof IFolder) {
				closeViewsInclInFolder((IFolder) aResource, activePage, viewReferences);
			}
		}
	}

}
