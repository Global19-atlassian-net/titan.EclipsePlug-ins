/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.ASN1.Undefined_Assignment;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Altstep;
import org.eclipse.titan.designer.AST.TTCN3.definitions.For_Loop_Definitions;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameterList;
import org.eclipse.titan.designer.AST.TTCN3.definitions.RunsOnScope;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.AST.TTCN3.types.ComponentTypeBody;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.core.ProjectBasedBuilder;
import org.eclipse.titan.designer.declarationsearch.Declaration;
import org.eclipse.titan.designer.declarationsearch.IdentifierFinderVisitor;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * @author Adam Delic
 * */
public final class ReferenceFinder {
	public static final String NORECOGNISABLESCOPE = "No valid scope could be recognised";
	public static final String NORECOGNISABLEASSIGNMENT = "No valid definition could be recognised";
	public static final String NOASSIGNMENTTYPE = "Cannot determine the type of the definition";

	public static class Hit {
		public Identifier identifier;
		public Reference reference;

		public Hit(final Identifier identifier) {
			this.identifier = identifier;
			this.reference = null;
		}

		public Hit(final Identifier identifier, final Reference reference) {
			this.identifier = identifier;
			this.reference = reference;
		}
	}

	public Scope scope = null;
	public Assignment assignment = null;
	public IType type = null;
	public Identifier fieldId = null;

	public ReferenceFinder() {
		// Do nothing
	}

	/**
	 * Creates and configures a new ReferenceFinder
	 *
	 * @param assignment
	 *                The assignment
	 * @throws IllegalArgumentException
	 *                 if the type of the assignment can not be determined
	 */
	public ReferenceFinder(final Assignment assignment) {
		this.assignment = assignment;
		this.scope = detectSmallestScope(assignment);

		// in ASN.1 the parsed stuff is undefined, get the real
		// assignments which are created during SA
		if (assignment instanceof Undefined_Assignment) {
			this.assignment = ((Undefined_Assignment) assignment).getRealAssignment(CompilationTimeStamp.getBaseTimestamp());
			if (this.assignment == null) {
				throw new IllegalArgumentException();
			}
		}

		// if it is a type assignment/definition then detect if we are
		// in a field
		if (assignment.getAssignmentType() == Assignment_type.A_TYPE) {
			type = assignment.getType(CompilationTimeStamp.getBaseTimestamp());
			if (type == null) {
				throw new IllegalArgumentException();
			}
			scope = scope.getModuleScope();
			type = type.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		}
	}

	public boolean detectAssignmentDataByOffset(final Module module, final int offset, final IEditorPart targetEditor,
			final boolean reportErrors, final boolean reportDebugInformation) {
		// detect the scope we are in
		scope = module.getSmallestEnclosingScope(offset);
		if (scope == null) {
			if (reportErrors) {
				targetEditor.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(NORECOGNISABLESCOPE);
			}
			return false;
		}

		final IdentifierFinderVisitor visitor = new IdentifierFinderVisitor(offset);
		module.accept(visitor);

		final Declaration declaration = visitor.getReferencedDeclaration();

		if (declaration == null) {
			return false;
		}

		assignment = declaration.getAssignment();

		if (assignment == null) {
			if (reportErrors) {
				targetEditor.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(NORECOGNISABLEASSIGNMENT);
			}
			return false;
		}

		scope = detectSmallestScope(assignment);

		// in ASN.1 the parsed stuff is undefined, get the real
		// assignments which are created during SA
		if (assignment instanceof Undefined_Assignment) {
			assignment = ((Undefined_Assignment) assignment).getRealAssignment(CompilationTimeStamp.getBaseTimestamp());
			if (assignment == null) {
				if (reportErrors) {
					targetEditor.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(NORECOGNISABLEASSIGNMENT);
				}
				return false;
			}
		}

		// if it is a type assignment/definition then detect if we are
		// in a field
		if (assignment.getAssignmentType() == Assignment_type.A_TYPE) {
			type = assignment.getType(CompilationTimeStamp.getBaseTimestamp());
			if (type == null) {
				if (reportErrors) {
					targetEditor.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(NOASSIGNMENTTYPE);
				}
				return false;
			}
			type.getEnclosingField(offset, this);
			type = type.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		}

		if (reportDebugInformation) {
			final MessageConsoleStream stream = TITANDebugConsole.getConsole().newMessageStream();
			TITANDebugConsole.println("found scope: name=" + scope.getScopeName() + "  type=" + scope.getClass().getName(), stream);
			TITANDebugConsole.println("found assignment: name=" + assignment.getIdentifier().getDisplayName() + "  type="
					+ assignment.getClass().getName(),stream);
			if (type != null) {
				TITANDebugConsole.println("found type: name=" + type.getTypename() + "  type=" + type.getClass().getName(), stream);
			}
			if (fieldId != null) {
				TITANDebugConsole.println("found field: name=" + fieldId.getDisplayName(), stream);
			}
		}

		return true;
	}

	/**
	 * Detect the smallest scope where the references should be searched
	 *
	 * @param assignment
	 *                The assignment
	 * @return The detected scope
	 */
	private Scope detectSmallestScope(final Assignment assignment) {
		Scope localScope = assignment.getMyScope();
		final Module module = localScope.getModuleScope();

		if (localScope instanceof StatementBlock) {
			final StatementBlock statementBlock = (StatementBlock) localScope;
			if (statementBlock.getMyDefinition() instanceof Def_Altstep) {
				localScope = localScope.getParentScope();
			}
			if (statementBlock.hasEnclosingLoopOrAltguard()) {
				localScope = localScope.getParentScope();
			}
		}

		if (localScope instanceof NamedBridgeScope) {
			localScope = localScope.getParentScope();
		}
		// if the definition is not inside a
		// control,function,testcase,altstep then it is global
		if (localScope instanceof Assignments && localScope.getParentScope() == module) {
			localScope = module;
		} else
			// component members might be seen in any testcase/function that
			// runs on that component or a compatible or self
			// treat it as global definition
			if (localScope instanceof ComponentTypeBody) {
				//FIXME this still does not make them global, that is something completely different.
				localScope = module;
			} else
				// search for actual named parameters everywhere
				if (localScope instanceof FormalParameterList || localScope instanceof RunsOnScope) {
					localScope = module;
				} else
					// this special scope does not contain other parts of the
					// For_Statement that must be searched
					if (localScope instanceof For_Loop_Definitions) {
						localScope = localScope.getParentScope();
					}

		return localScope;
	}

	public Map<Module, List<Hit>> findAllReferences(final Module module, final IProject project,
			final IProgressMonitor pMonitor, final boolean reportDebugInformation) {
		final IProgressMonitor monitor = pMonitor == null ? new NullProgressMonitor() : pMonitor;

		final List<IProject> relatedProjects = ProjectBasedBuilder.getProjectBasedBuilder(project).getAllReferencingProjects();
		relatedProjects.addAll(ProjectBasedBuilder.getProjectBasedBuilder(project).getAllReachableProjects());
		relatedProjects.add(project);

		int size = 0;
		for(IProject tempProject: relatedProjects) {
			size += GlobalParser.getProjectSourceParser(tempProject).getModules().size();
		}

		monitor.beginTask("Searching references.", size);
		final Map<Module, List<Hit>> foundIdsMap = new HashMap<Module, List<Hit>>();
		// in this scope
		//TODO this is efficient but only for local variables ... and if are not followed up by other searches
		List<Hit> foundIds = new ArrayList<Hit>();
		scope.findReferences(this, foundIds);
		if (!foundIds.isEmpty()) {
			foundIdsMap.put(scope.getModuleScope(), foundIds);
		}
		// in other modules that import this module, if the assignment
		// is global
		//FIXME but if component variable ... we might have to search all related modules in all related projects.
		if (scope instanceof Module) {
			for(IProject project2 : relatedProjects) {
				final ProjectSourceParser projectSourceParser2 = GlobalParser.getProjectSourceParser(project2);

				for (Module module2 : projectSourceParser2.getModules()) {
					if (monitor.isCanceled()) {
						return foundIdsMap;
					}

					for (Module m : module2.getImportedModules()) {
						if (m == scope) {
							if (reportDebugInformation) {
								TITANDebugConsole.println("found importing module: " + module2.getName());
							}
							foundIds = new ArrayList<Hit>();
							module2.findReferences(this, foundIds);
							if (!foundIds.isEmpty()) {
								foundIdsMap.put(module2, foundIds);
							}
							break;
						}
					}
					monitor.worked(1);
				}
			}
		}
		monitor.done();
		return foundIdsMap;
	}

	public String getSearchName() {
		if (fieldId == null) {
			return assignment.getDescription();
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("Field `").append(fieldId.getDisplayName()).append("' of ").append(type.getTypename());
		return sb.toString();
	}

	public Identifier getReferredIdentifier() {
		if (fieldId != null) {
			return fieldId;
		}
		if (assignment != null) {
			return assignment.getIdentifier();
		}
		return null;
	}

	public List<Hit> findReferencesInModule(final Module module) {
		final List<Hit> foundIds = new ArrayList<Hit>();

		module.findReferences(this, foundIds);

		return foundIds;
	}
}
