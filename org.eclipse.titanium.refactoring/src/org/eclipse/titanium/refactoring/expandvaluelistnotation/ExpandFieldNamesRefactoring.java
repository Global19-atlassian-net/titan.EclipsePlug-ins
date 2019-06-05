package org.eclipse.titanium.refactoring.expandvaluelistnotation;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * This class represents the 'Expand field names' refactoring operation.
 * <p>
 * This refactoring operation expands all field names in the given
 *   files/folders/projects, which are contained in a {@link IStructuredSelection} object.
 * The operation can be executed using the mechanisms in the superclass, through a wizard for example
 *
 * @author Zsolt Tabi
 */
public class ExpandFieldNamesRefactoring extends Refactoring {
	public static final String PROJECTCONTAINSERRORS = "The project `{0}'' contains errors, which might corrupt the result of the refactoring";
	public static final String PROJECTCONTAINSTTCNPPFILES = "The project `{0}'' contains .ttcnpp files, which might corrupt the result of the refactoring";
	private static final String ONTHEFLYANALAYSISDISABLED = "The on-the-fly analysis is disabled, there is semantic information present to work on";

	private final IStructuredSelection selection;
	private final Set<IProject> projects = new HashSet<IProject>();

	private Object[] affectedObjects;		//the list of objects affected by the change

	/*
	 * TODO dev:
	 *
	 *
	 * TODO fix:
	 *
	 * */

	public ExpandFieldNamesRefactoring(final IStructuredSelection selection) {
		this.selection = selection;

		final Iterator<?> it = selection.iterator();
		while (it.hasNext()) {
			final Object o = it.next();
			if (o instanceof IResource) {
				final IProject temp = ((IResource) o).getProject();
				projects.add(temp);
			}
		}
	}

	public Object[] getAffectedObjects() {
		return affectedObjects;
	}

	//METHODS FROM REFACTORING

	@Override
	public String getName() {
		return "Expand record field names";
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus result = new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);

			final IPreferencesService prefs = Platform.getPreferencesService();//PreferenceConstants.USEONTHEFLYPARSING
			if (! prefs.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.USEONTHEFLYPARSING, false, null)) {
				result.addError(ONTHEFLYANALAYSISDISABLED);
			}

			// check that there are no ttcnpp files in the
			// project
			for (final IProject project : projects) {
				if (GlobalParser.hasTtcnppFiles(project)) {
					result.addError(MessageFormat.format(PROJECTCONTAINSTTCNPPFILES, project));
				}
			}

			pm.worked(1);
			// check that there are no error markers in the
			// project
			for (final IProject project : projects) {
				final IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
				for (final IMarker marker : markers) {
					if (IMarker.SEVERITY_ERROR == marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR)) {
						result.addError(MessageFormat.format(PROJECTCONTAINSERRORS, project));
						break;
					}
				}
			}
			pm.worked(1);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
			result.addFatalError(e.getMessage());
		} finally {
			pm.done();
		}
		return result;
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (selection == null) {
			return null;
		}

		final CompositeChange cchange = new CompositeChange("ExpandFieldNamesRefactoring");
		final Iterator<?> it = selection.iterator();
		while (it.hasNext()) {
			final Object o = it.next();
			if (!(o instanceof IResource)) {
				continue;
			}

			final IResource res = (IResource)o;
			final ResourceVisitor vis = new ResourceVisitor();
			res.accept(vis);
			cchange.add(vis.getChange());
		}
		affectedObjects = cchange.getAffectedObjects();
		return cchange;
	}

	//METHODS FROM REFACTORING END



	/**
	 * Visits all the files of a folder or project (any {@link IResource}).
	 * Creates the {@link Change} for all files and then merges them into a single
	 * {@link CompositeChange}.
	 * <p>
	 * Call on any {@link IResource} object.
	 *  */
	private class ResourceVisitor implements IResourceVisitor {

		private final CompositeChange change;

		public ResourceVisitor() {
			this.change = new CompositeChange("ExpandFieldNamesRefactoring");
		}

		private CompositeChange getChange() {
			return change;
		}

		@Override
		public boolean visit(final IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				final ChangeCreator chCreator = new ChangeCreator((IFile)resource);
				chCreator.perform();
				final Change ch = chCreator.getChange();
				if (ch != null) {
					change.add(ch);
				}
				//SKIP
				return false;
			}
			//CONTINUE
			return true;
		}

	}

}
