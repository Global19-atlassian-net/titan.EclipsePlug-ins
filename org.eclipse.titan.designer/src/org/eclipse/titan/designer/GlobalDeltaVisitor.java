/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.titan.designer.commonFilters.ResourceExclusionHelper;
import org.eclipse.titan.designer.core.ProjectBasedBuilder;
import org.eclipse.titan.designer.parsers.FileSaveTracker;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.ui.PlatformUI;

/**
 * General project visitor which is able to detect if a file included in the build process has changed, and is also able to mark it as one needing
 * re-parsing.
 * 
 * @author Kristof Szabados
 */
public final class GlobalDeltaVisitor implements IResourceDeltaVisitor {
	private IProject project;
	private final IContainer[] workingDirectories;
	private List<IFile> outdatedFiles = new ArrayList<IFile>();

	public GlobalDeltaVisitor(final IProject project) {
		this.project = project;
		final boolean reportError = 	PlatformUI.isWorkbenchRunning();	//Do not report error in headless mode
		workingDirectories = ProjectBasedBuilder.getProjectBasedBuilder(project).getWorkingDirectoryResources(reportError);
	}

	@Override
	public boolean visit(final IResourceDelta delta) {
		final IResource resource = delta.getResource();
		if (resource.getName().startsWith(GlobalParser.DOT)) {
			return false;
		}
		switch (resource.getType()) {
		case IResource.FILE:
			if (delta.getFlags() != IResourceDelta.MARKERS) {
				if ((delta.getFlags() == IResourceDelta.REMOVED || !resource.isAccessible())
						|| (!ResourceExclusionHelper.isDirectlyExcluded((IFile) resource) && GlobalParser.isSupportedExtension(resource.getFileExtension()))) {
					final IFile file = (IFile) resource;
					if (FileSaveTracker.isFileBeingSaved(file)) {
						FileSaveTracker.fileSaved(file);
					} else {
						outdatedFiles.add(file);
					}
				}
			}
			return false;
		case IResource.FOLDER:
			for (final IContainer workingDirectory : workingDirectories) {
				if (workingDirectory.equals(resource)) {
					return false;
				}
			}
			if (!resource.isAccessible() || resource.getLocation() == null) { 
				// The folder is removed, the contained modules need to be removed as well
				return true;
			}

			if (ResourceExclusionHelper.isDirectlyExcluded((IFolder) resource)) {
				return false;
			}
			break;
		default:
			break;
		}

		return true;
	}

	/**
	 * Reports the collected list of outdated files, to let the internal storage know, that their data is outdated..
	 * */
	public WorkspaceJob[] reportOutdatedFiles() {
		WorkspaceJob[] jobs = new WorkspaceJob[2];
		if (!outdatedFiles.isEmpty()) {
			jobs[0] = GlobalParser.getProjectSourceParser(project).reportOutdating(outdatedFiles);
			jobs[1] = GlobalParser.getConfigSourceParser(project).reportOutdating(outdatedFiles);
		}
		return jobs;
	}
}
