/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.debug.actions;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.statements.Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.AST.TTCN3.types.ComponentTypeBody;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3Editor;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

public final class Export_Debug_AST implements IEditorActionDelegate {
	private TTCN3Editor targetEditor = null;
	StringBuilder paddingBuffer = new StringBuilder();
	
	@Override
	public void run(IAction action) {
		if (targetEditor == null) return;
		IFile file = (IFile) targetEditor.getEditorInput().getAdapter(IFile.class);
		ProjectSourceParser parser = GlobalParser.getProjectSourceParser(file.getProject());
		Module module = parser.containedModule(file);
		if (module == null) {
			TITANDebugConsole.println("No module was found");
		}
		
		TITANDebugConsole.println("*************************");
		TITANDebugConsole.println("Printing DEBUG information for module `" + module.getName() + "':");
		TITANDebugConsole.println("*************************");

		module.accept(new ASTVisitor() {
			private int padding = 0;
			@Override
			public int visit(IVisitableNode node) {
				if (node instanceof Assignment) {
					Assignment assignment = (Assignment) node;
					printInfoln(padding, assignment.getAssignmentName(), assignment.getFullName(), assignment.getLastTimeChecked(), assignment.getLocation());
				} else if (node instanceof Identifier) {
					printInfoln(padding, "identifier", ((Identifier) node).getDisplayName(), null, ((Identifier) node).getLocation());
				} else if (node instanceof Statement) {
					Statement statement = (Statement) node;
					printInfoln(padding, "statement", statement.getFullName(), statement.getLastTimeChecked(), statement.getLocation());
				} else if (node instanceof Reference) {
					Reference ref = (Reference) node;
					printInfoln(padding, "reference", ref.getFullName(), ref.getLastTimeChecked(), ref.getLocation());
					Assignment old = ref.getAssOld();
					if (old != null) {
						printInfoln(padding + 1, "This reference was last pointing to " + old.getFullName() + " analyzed at " + old.getLastTimeChecked());
					}
				} else if (node instanceof ComponentTypeBody) {
					ComponentTypeBody body = (ComponentTypeBody) node;
					Map<String, Definition> map = body.getDefinitionMap();
					printInfoln(padding + 1, " contains definitions:");
					for (Map.Entry<String, Definition> entry: map.entrySet()) {
						printInfoln(padding + 2, entry.getKey() + " was last checked at " + entry.getValue().getLastTimeChecked());
					}
				}

				if (node instanceof StatementBlock || node instanceof Definition) {
					padding++;
				}

				return super.visit(node);
			}
			@Override
			public int leave(IVisitableNode node) {
				if (node instanceof StatementBlock || node instanceof Definition) {
					padding--;
				}

				return super.leave(node);
			}
			
		});
		TITANDebugConsole.println("*************************");
		TITANDebugConsole.println("Printing DEBUG information for module `" + module.getName() + "' finished");
		TITANDebugConsole.println("*************************");
	}

	private void printInfoln(int padding, String text) {
		while (paddingBuffer.length() < padding * 2) {
			paddingBuffer.append("  ");
		}

		TITANDebugConsole.print(paddingBuffer.substring(0, padding * 2));
		TITANDebugConsole.println(text);
	}

	private void printInfoln(int padding, String kind, String fullname, CompilationTimeStamp timestamp, Location location) {
		while (paddingBuffer.length() < padding * 2) {
			paddingBuffer.append("  ");
		}

		TITANDebugConsole.print(paddingBuffer.substring(0, padding * 2));
		TITANDebugConsole.print(kind + " " + fullname);
		if (timestamp != null) {
			TITANDebugConsole.print(" last checked at " + timestamp);
		}
		if (location instanceof NULL_Location) {
			TITANDebugConsole.println(" is located at null location");
		} else {
			TITANDebugConsole.println(" is located at line " + location.getLine() + " between " + location.getOffset() + " - " + location.getEndOffset());
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to be done
	}

	@Override
	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		if (targetEditor instanceof TTCN3Editor) {
			this.targetEditor = (TTCN3Editor) targetEditor;
		} else {
			this.targetEditor = null;
		}
	}

}
