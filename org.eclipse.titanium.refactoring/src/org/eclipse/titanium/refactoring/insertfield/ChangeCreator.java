/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.insertfield;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Template;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.templates.Named_Template_List;
import org.eclipse.titan.designer.AST.TTCN3.templates.TTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.Template_List;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Sequence_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Set_Seq_Choice_BaseType;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Set_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.Sequence_Value;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titanium.refactoring.insertfield.InsertFieldRefactoring.Settings;

/**
 * This class is only instantiated by the {@link InsertFieldRefactoring} once per each refactoring operation.
 * <p>
 * By passing the selection through the constructor and calling {@link ChangeCreator#perform()}, this class
 *  creates a {@link Change} object, which can be returned by the standard
 *  {@link Refactoring#createChange(IProgressMonitor)} method in the refactoring class.
 *
 * @author Bianka Bekefi
 */
class ChangeCreator {

	// in
	private final IFile selectedFile;
	private final Definition selection;
	private final Settings settings;

	// out
	private Change change;

	private Type type;

	ChangeCreator(final IFile selectedFile, final Definition selection, final Settings settings) {
		this.selectedFile = selectedFile;
		this.selection = selection;
		this.settings = settings;
	}

	public Change getChange() {
		return change;
	}

	/**
	 * Creates the {@link #change} object, which contains all the inserted
	 * fields in the selected resources.
	 * */
	public void perform() {
		if (selectedFile == null) {
			return;
		}
		change = createFileChange(selectedFile);
	}

	private Change createFileChange(final IFile toVisit) {
		if (toVisit == null) {
			return null;
		}
		final ProjectSourceParser sourceParser = GlobalParser.getProjectSourceParser(toVisit.getProject());
		final Module module = sourceParser.containedModule(toVisit);
		if (module == null) {
			return null;
		}

		if (selection instanceof Def_Type) {
			final Def_Type df = (Def_Type) selection;
			type = df.getType(CompilationTimeStamp.getBaseTimestamp());
		}

		final DefinitionVisitor vis = new DefinitionVisitor(type);
		module.accept(vis);
		final NavigableSet<ILocateableNode> nodes = vis.getLocations();
		if (nodes.isEmpty()) {
			return null;
		}

		final TextFileChange tfc = new TextFileChange(toVisit.getName(), toVisit);
		final MultiTextEdit rootEdit = new MultiTextEdit();

		try {
			final WorkspaceJob job1 = calculateEditLocations(nodes, toVisit, rootEdit);
			job1.join();
		} catch (InterruptedException ie) {
			ErrorReporter.logExceptionStackTrace(ie);
		} catch (CoreException ce) {
			ErrorReporter.logError("InsertFieldRefactoring/CreateChange.createFileChange(): "
					+ "CoreException while calculating edit locations. ");
			ErrorReporter.logExceptionStackTrace(ce);
		}

		if (!rootEdit.hasChildren()) {
			return null;
		}

		tfc.setEdit(rootEdit);
		return tfc;
	}

	private WorkspaceJob calculateEditLocations(final NavigableSet<ILocateableNode> nodes, final IFile file
			, final MultiTextEdit rootEdit) throws CoreException {
		final WorkspaceJob job = new WorkspaceJob("InsertFieldRefactoring: calculate edit locations") {

			@Override
			public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
				for (ILocateableNode node : nodes) {
					int vmLen = settings.getType().length() + settings.getId().getTtcnName().length();
					if (node instanceof Def_Type) {
						Def_Type df = (Def_Type) node;
						Type type = df.getType(CompilationTimeStamp.getBaseTimestamp());
						if (type instanceof TTCN3_Sequence_Type || type instanceof TTCN3_Set_Type) {
							vmLen = insertField((TTCN3_Set_Seq_Choice_BaseType) type, node, rootEdit, vmLen);
						}
					} else if (node instanceof Sequence_Value) {
						Sequence_Value sv = (Sequence_Value) node;
						vmLen += 6;
						final Location nodeLocation = node.getLocation();
						if (settings.getPosition() < sv.getNofComponents()) {
							final Location valueLocation = sv.getSeqValueByIndex(settings.getPosition()).getLocation();
							Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
									valueLocation.getOffset(), valueLocation.getEndOffset() + vmLen);
							rootEdit.addChild(new InsertEdit(l.getOffset(), settings.getId().getTtcnName() + " := " + settings.getValue() + ", "));
						} else {
							int max = sv.getNofComponents();
							final Location valueLocation = sv.getSeqValueByIndex(max - 1).getLocation();
							Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
									valueLocation.getEndOffset(), valueLocation.getEndOffset() + vmLen);
							rootEdit.addChild(new InsertEdit(l.getOffset(), ", " + settings.getId().getTtcnName() + " := " + settings.getValue()));
						}
					} else if (node instanceof TTCN3Template) {
						TTCN3Template template = (TTCN3Template) node;
						vmLen += 6;

						if (template instanceof Named_Template_List) {
							Named_Template_List ntl = (Named_Template_List) template;
							final Location nodeLocation = node.getLocation();
							if (settings.getPosition() < ntl.getNofTemplates()) {
								final Location templateLocation = ntl.getTemplateByIndex(settings.getPosition()).getLocation();
								Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
										templateLocation.getOffset(), templateLocation.getEndOffset() + vmLen);
								rootEdit.addChild(new InsertEdit(l.getOffset(), settings.getId().getTtcnName() + " := " + settings.getValue() + ", "));
							} else {
								int max = ntl.getNofTemplates();
								final Location templateLocation = ntl.getTemplateByIndex(max - 1).getLocation();
								Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
										templateLocation.getEndOffset(), templateLocation.getEndOffset() + vmLen);
								rootEdit.addChild(new InsertEdit(l.getOffset(), ", " + settings.getId().getTtcnName() + " := " + settings.getValue()));
							}
						} else if (template instanceof Template_List) {
							Template_List tl = (Template_List) template;
							final Location nodeLocation = node.getLocation();
							if (settings.getPosition() < tl.getNofTemplates()) {
								final Location templateLocation = tl.getTemplateByIndex(settings.getPosition()).getLocation();
								Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
										templateLocation.getOffset(), templateLocation.getEndOffset() + vmLen);
								rootEdit.addChild(new InsertEdit(l.getOffset(), settings.getValue() + ","));
							} else {
								int max = tl.getNofTemplates();
								final Location templateLocation = tl.getTemplateByIndex(max - 1).getLocation();
								Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
										templateLocation.getEndOffset(), templateLocation.getEndOffset() + vmLen);
								rootEdit.addChild(new InsertEdit(l.getOffset(), "," + settings.getValue()));
							}
						}
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return job;
	}

	public int insertField(final TTCN3_Set_Seq_Choice_BaseType ss, final ILocateableNode node, final MultiTextEdit rootEdit, int vmLen) {
		final Location nodeLocation = node.getLocation();

		final int noc = ss.getNofComponents();
		if (settings.getPosition() < noc) {
			vmLen += 6;
			final ILocateableNode cf = (ILocateableNode)ss.getComponentByIndex(settings.getPosition());
			final Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
					cf.getLocation().getOffset(), cf.getLocation().getEndOffset()+vmLen);
			rootEdit.addChild(new InsertEdit(l.getOffset(), settings.getType()+" "+settings.getId().getTtcnName()+", \n  "));
		} else {
			vmLen += 5;
			final ILocateableNode cf = (ILocateableNode)ss.getComponentByIndex(noc-1);
			final Location l = new Location(nodeLocation.getFile(), nodeLocation.getLine(),
					cf.getLocation().getEndOffset(), cf.getLocation().getEndOffset()+vmLen);
			rootEdit.addChild(new InsertEdit(l.getOffset()-1, ",\n  "+settings.getType()+" "+settings.getId().getTtcnName()));
		}

		return vmLen;
	}

	/**
	 * Collects the locations of all the definitions in a module where the type is the same
	 * as the type of the selected node;
	 * <p>
	 * Call on modules.
	 * */
	private static class DefinitionVisitor extends ASTVisitor {

		private final NavigableSet<ILocateableNode> locations;
		private final Type type;

		DefinitionVisitor(final Type type) {
			locations = new TreeSet<ILocateableNode>(new LocationComparator());
			this.type = type;
		}

		private NavigableSet<ILocateableNode> getLocations() {
			return locations;
		}

		@Override
		public int visit(final IVisitableNode node) {
			if (node instanceof Def_Type && ((Def_Type)node).getType(CompilationTimeStamp.getBaseTimestamp()).equals(type)) {
				final Definition d = (Definition)node;
				locations.add(d);
			} else if (node instanceof Sequence_Value) {
				final Sequence_Value sv = (Sequence_Value)node;
				if (sv.getMyGovernor() != null && sv.getMyGovernor().getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp()).equals(type)) {
					locations.add(sv);
				} else if (sv.getMyGovernor() == null && sv.getDefiningAssignment() instanceof Def_Template) {
					TITANDebugConsole.println("Governor NULL: "+sv.getFullName()+"   "+sv.getDefiningAssignment()+"   "+sv.toString());
				}
			} else if (node instanceof TTCN3Template) {
				final TTCN3Template tt = (TTCN3Template)node;
				if (tt.getMyGovernor() != null && tt.getMyGovernor().getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp()).equals(type)) {
					locations.add(tt);
				}
			}

			return V_CONTINUE;
		}
	}

	/**
	 * Compares {@link ILocateableNode}s by comparing the file paths as strings.
	 * If the paths are equal, the two offset integers are compared.
	 * */
	private static class LocationComparator implements Comparator<ILocateableNode> {

		@Override
		public int compare(final ILocateableNode arg0, final ILocateableNode arg1) {
			final IResource f0 = arg0.getLocation().getFile();
			final IResource f1 = arg1.getLocation().getFile();
			if (!f0.equals(f1)) {
				return f0.getFullPath().toString().compareTo(f1.getFullPath().toString());
			}

			final int o0 = arg0.getLocation().getOffset();
			final int o1 = arg1.getLocation().getOffset();
			return (o0 < o1) ? -1 : ((o0 == o1) ? 0 : 1);
		}
	}
}
