/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3Editor;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titan.designer.properties.data.CCompilerOptionsData;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;
import org.eclipse.titan.designer.properties.data.TTCN3PreprocessorOptionsData;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * A class for utilities used by the refactoring classes.
 * TODO needed? used consistently?
 * 
 * @author Viktor Varga
 */
public class Utils {

	private static final String SOURCE_DIR = "src";

	private Utils() {
		// private constructor to disable instantiation.
	}

	public static TTCN3Editor getActiveEditor() {
		final IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (!(editor instanceof TTCN3Editor)) {
			return null;
		}

		return (TTCN3Editor) editor;
	}

	public static IFile getSelectedFileInEditor(final String refactoringName) {
		final TTCN3Editor targetEditor = getActiveEditor();
		if (targetEditor == null) {
			ErrorReporter.logError("Utils.getSelectedFileInEditor(): " +
					"No TTCN3Editor available, during refactoring: " + refactoringName);
			return null;
		}
		return extractFile(targetEditor, refactoringName);
	}

	private static IFile extractFile(final IEditorPart editor, final String refactoringName) {
		final IEditorInput input = editor.getEditorInput();
		if (!(input instanceof IFileEditorInput)) {
			TITANDebugConsole.getConsole().newMessageStream()
					.println("Utils.extractFile() during refactoring " +
					refactoringName + ": IEditorInput is not an IFileEditorInput. ");
			return null;
		}
		return ((IFileEditorInput) input).getFile();
	}

	/**
	 * Reanalyzes the project which contains the file that is currently active in the editor window.
	 * */
	public static void updateASTForProjectActiveInEditor(final String refactoringName) {
		// getting editor
		final IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		TTCN3Editor editor;
		if (editorPart == null || !(editorPart instanceof TTCN3Editor)) {
			TITANDebugConsole.getConsole().newMessageStream()
					.println("Utils.updateASTForProjectActiveInEditor() during refactoring " +
					refactoringName + ": Only for TTCN3 editors!");
			return;
		} else {// TODO not needed else
			editor = (TTCN3Editor) editorPart;
		}
		// getting selected file
		final IFile selFile = extractFile(editor, refactoringName);
		if (selFile == null) {
			return;
		}
		//
		final IProject selProject = selFile.getProject();
		final ProjectSourceParser projectSourceParser = GlobalParser.getProjectSourceParser(selProject);
		WorkspaceJob job = projectSourceParser.reportOutdating((IFile) selFile);
		if (job == null) {
			TITANDebugConsole.getConsole().newMessageStream()
			.println("Utils.updateASTForProjectActiveInEditor() during refactoring " +
			refactoringName + ": WorkspaceJob to report outdating could not be created for project: " + selProject);
			return;
		}
		try {
			job.join();
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
			return;
		}

		job = projectSourceParser.analyzeAll();
		if (job == null) {
			TITANDebugConsole.getConsole().newMessageStream()
			.println("Utils.updateASTForProjectActiveInEditor() during refactoring " +
			refactoringName + ": WorkspaceJob to reanalyze project could not be created for project: " + selProject);
			return;
		}
		try {
			job.join();
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
			return;
		}
	}

	/**
	 * Reanalyzes the given projects.
	 * */
	public static void updateASTBeforeRefactoring(final Set<IProject> projsToUpdate, final String name) {
		final UpdateASTOp updateAST = new UpdateASTOp(projsToUpdate, name);
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(null);
		try {
			pmd.run(true, true, updateAST);
		} catch (InvocationTargetException ite) {
			ErrorReporter.logExceptionStackTrace("Utils.updateASTBeforeRefactoring(): " +
					"Error while updating AST before using the refactoring: " + name, ite);
		} catch (InterruptedException ie) {
			ErrorReporter.logExceptionStackTrace("Utils.updateASTBeforeRefactoring(): " +
					"Error while updating AST before using the refactoring: " + name, ie);
			return;
		}
	}

	/**
	 * Reanalyzes only the modified files after a refactoring operation.
	 * If the operation was cancelled, nothing is reanalyzed.
	 * @param wiz The refactoring wizard
	 * @param affectedObjects The affected objects of the Change which was produced by the refactoring operation.
	 * 				This needs to be saved before {@link Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)} returns,
	 * 				because a successful refactoring operation clears the affected objects' list in the Change object.
	 * @param refactoringName The name of the operation
	 * 
	 * */
	public static void updateASTAfterRefactoring(final RefactoringWizard wiz, final Object[] affectedObjects, final String refactoringName) {
		if (wiz.getChange() == null || wiz.getChange().getAffectedObjects() == null) {
			return;
		}
		if (wiz.getChange().getAffectedObjects().length != 0) {
			return;	//Change object after the refactoring is not empty -> operation was cancelled
		}
		if (affectedObjects == null) {
			return;
		}

		final Map<IProject, List<IFile>> affectedProjects = new HashMap<IProject, List<IFile>>();
		for (final Object o : affectedObjects) {
			if (o instanceof IFile) {
				final IFile f = (IFile) o;
				final IProject pr = f.getProject();
				GlobalParser.getProjectSourceParser(pr).reportOutdating(f);
				final List<IFile> fs = affectedProjects.get(pr);
				if (fs == null) {
					final List<IFile> newFs = new ArrayList<IFile>();
					newFs.add(f);
					affectedProjects.put(pr, newFs);
				} else {
					fs.add(f);
				}
			} else {
				ErrorReporter.logWarning("Utils.updateASTAfterRefactoring(): " + refactoringName + " -> " +
						"An affected object is not an IFile " + o);
			}
		}
		for (final Map.Entry<IProject, List<IFile>> e : affectedProjects.entrySet()) {
			final IProject pr = e.getKey();
			final List<IFile> fs = e.getValue();
			final ProjectSourceParser psp = GlobalParser.getProjectSourceParser(pr);
			for (final IFile f : fs) {
				psp.reportOutdating(f);
			}
			psp.analyzeAll();
		}
	}

	/**
	 * Returns the projects contained in the selection object.
	 * */
	public static Set<IProject> findAllProjectsInSelection(final IStructuredSelection ssel) {
		final Set<IProject> projs = new HashSet<IProject>();
		if (ssel == null) {
			return projs;
		}

		final Iterator<?> it = ssel.iterator();
		while (it.hasNext()) {
			final Object o = it.next();
			if (!(o instanceof IResource)) {
				continue;
			}
			if (o instanceof IProject) {
				projs.add((IProject) o);
				continue;
			}

			final IResource res = (IResource) o;
			projs.add(res.getProject());
		}
		return projs;
	}

	/**
	 * This class contains an operation which updates the AST for the given projects, while
	 * displaying a progress bar.
	 * <p>
	 * Use an instance of this class as a parameter of the
	 * {@link ProgressMonitorDialog#run(boolean, boolean, IRunnableWithProgress)} method.
	 * 
	 * */
	private static class UpdateASTOp implements IRunnableWithProgress {

		private final Set<IProject> toUpdate;
		private final String name;

		/**
		 * @param toUpdate the projects to reanalyze
		 * @param name the string (name of the refactoring operation) to display in error messages
		 * */
		public UpdateASTOp(final Set<IProject> toUpdate, final String name) {
			this.toUpdate = toUpdate;
			this.name = name;
		}

		@Override
		public void run(final IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			if (toUpdate == null) {
				return;
			}
			monitor.beginTask(name, toUpdate.size());
			// update AST for each project
			for (final IProject proj : toUpdate) {
				monitor.subTask("Waiting for semantic analysis on project " + proj.getName());
				final ProjectSourceParser projectSourceParser = GlobalParser.getProjectSourceParser(proj);
				final WorkspaceJob job = projectSourceParser.analyzeAll();
				if (job == null) {
					TITANDebugConsole.getConsole().newMessageStream()
							.println("Utils.updateASTOp: WorkspaceJob to analyze project could not be created for project "
									+ proj.getName() + ", during the refactoring: " + name);
					return;
				}
				try {
					job.join();
				} catch (InterruptedException e) {
					TITANDebugConsole.getConsole().newMessageStream()
							.println("Utils.updateASTOp: Error during semantic analysis of the project: "
									+ proj.getName() + ", during the refactoring: " + name);
					return;
				}
				if (monitor.isCanceled()) {
					throw new InterruptedException();
				}
				monitor.worked(1);
			}
		}
	}

	/**
	 * Collects all the files of a folder or project (any {@link IResource}).
	 * <p>
	 * Call on any {@link IResource} object.
	 * */
	public static class ResourceVisitor implements IResourceVisitor {

		private final List<IFile> files;

		private ResourceVisitor() {
			files = new ArrayList<IFile>();
		}

		private List<IFile> getFiles() {
			return files;
		}

		@Override
		public boolean visit(final IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				files.add((IFile) resource);
				// SKIP
				return false;
			}
			// CONTINUE
			return true;
		}

	}

	/**
	 * FOR DEBUG
	 * Lists all children nodes.
	 * */
	public static class DebugVisitor extends ASTVisitor {

		private String prefix = "";

		@Override
		public int visit(final IVisitableNode node) {
			prefix = prefix + "    ";
			TITANDebugConsole.print(prefix + node.getClass() + "; " + node);
			if (node instanceof ILocateableNode) {
				final ILocateableNode ln = (ILocateableNode) node;
				TITANDebugConsole.print(" loc: " + ln.getLocation().getOffset() + "-" + ln.getLocation().getEndOffset());
				if (node instanceof Definition) {
					final Definition d = (Definition) ln;
					TITANDebugConsole.print(", cummloc: " + d.getCumulativeDefinitionLocation().getOffset() + "-" + d.getCumulativeDefinitionLocation().getEndOffset());
				}
			}
			TITANDebugConsole.println();

			return V_CONTINUE;
		}

		@Override
		public int leave(final IVisitableNode node) {
			prefix = prefix.substring(4);
			return V_CONTINUE;
		}

	}

	public static String createLocationString(final IVisitableNode node) {
		if (node == null) {
			return "<null node>";
		}
		if (!(node instanceof ILocateableNode)) {
			return "<no location info>";
		}

		final StringBuilder sb = new StringBuilder();
		final ILocateableNode lnode = (ILocateableNode) node;
		sb.append(' ').append(lnode.getLocation().getOffset()).append('-').append(lnode.getLocation().getEndOffset());
		sb.append(" in file ").append(lnode.getLocation().getFile().getName()).append(':');
		sb.append(lnode.getLocation().getLine()).append(' ');
		return sb.toString();
	}

	public static boolean createProject(final IProjectDescription description, final IProject projectHandle)
			throws CoreException {

		final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(final IProgressMonitor monitor) throws CoreException {
				projectHandle.create(description, null);
				projectHandle.open(IResource.BACKGROUND_REFRESH, null);
				projectHandle.refreshLocal(IResource.DEPTH_ONE, null);
				//
				String sourceFolder = SOURCE_DIR;
				IFolder folder = projectHandle.getFolder(sourceFolder);
				if (!folder.exists()) {
					try {
						folder.create(true, true, null);
					} catch (CoreException e) {
						ErrorReporter.logExceptionStackTrace(e);
					}
				}
				projectHandle.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						ProjectBuildPropertyData.GENERATE_MAKEFILE_PROPERTY), "true");
				projectHandle.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						TTCN3PreprocessorOptionsData.TTCN3_PREPROCESSOR_PROPERTY), "cpp");
				projectHandle.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
						CCompilerOptionsData.CXX_COMPILER_PROPERTY), "g++");
			}
		};

		try {
			op.run(null);
		} catch (InterruptedException e) {
			ErrorReporter.logError("Project creation was interupted: " + description.getName());
			return false;
		} catch (InvocationTargetException e) {
			ErrorReporter.logError("Project creation was unsuccessful: " + description.getName());
			ErrorReporter.logExceptionStackTrace(e);
			return false;
		}
		return true;
	}

}
