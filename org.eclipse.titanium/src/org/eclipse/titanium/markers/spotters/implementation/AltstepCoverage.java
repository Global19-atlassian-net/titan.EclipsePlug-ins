/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.markers.spotters.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Altstep;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Port;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Testcase;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameterList;
import org.eclipse.titan.designer.AST.TTCN3.definitions.RunsOnScope;
import org.eclipse.titan.designer.AST.TTCN3.statements.AltGuard;
import org.eclipse.titan.designer.AST.TTCN3.statements.AltGuards;
import org.eclipse.titan.designer.AST.TTCN3.statements.Alt_Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.Operation_Altguard;
import org.eclipse.titan.designer.AST.TTCN3.statements.Receive_Port_Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.Statement;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.types.Component_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Port_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Referenced_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TypeSet;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titanium.markers.spotters.BaseModuleCodeSmellSpotter;
import org.eclipse.titanium.markers.types.CodeSmellType;

public class AltstepCoverage {
	public static class OnAltstep extends Base {
		private static final String NOT_COVERED = "This altstep does not handle all possible incoming message types.";

		@Override
		public void process(final IVisitableNode node, final Problems problems) {
			if (node instanceof Def_Altstep) {
				final Def_Altstep alt = (Def_Altstep) node;
				final Map<Identifier, Set<String>> visiblePorts = new HashMap<Identifier, Set<String>>();

				final CompilationTimeStamp timestamp = CompilationTimeStamp.getBaseTimestamp();
				// processing ports from the component the altstep runs on
				final Component_Type runsOn = alt.getRunsOnType(timestamp);
				if (runsOn != null) {
					addPortsOfComponent(runsOn, visiblePorts);
				}

				// processing ports from the parameter list
				final FormalParameterList parameters = alt.getFormalParameterList();
				if (parameters != null) {
					addPortsOfParameterList(parameters, visiblePorts);
				}

				// remove those types that are handled by any alt branch
				removeHandledByGuards(alt.getAltGuards(), visiblePorts);

				// report the remaining (if any) as a problem
				handleRemainingPorts(NOT_COVERED, alt.getIdentifier().getLocation(), visiblePorts, problems);
			}
		}

		@Override
		public List<Class<? extends IVisitableNode>> getStartNode() {
			final List<Class<? extends IVisitableNode>> ret = new ArrayList<Class<? extends IVisitableNode>>(1);
			ret.add(Def_Altstep.class);
			return ret;
		}
	}

	public static class OnAltStatement extends Base {
		private static final String NOT_COVERED = "This alt statement does not handle all possible incoming message types.";

		@Override
		public void process(final IVisitableNode node, final Problems problems) {
			if (node instanceof Alt_Statement) {
				final Alt_Statement alt = (Alt_Statement) node;
				final Map<Identifier, Set<String>> visiblePorts = new HashMap<Identifier, Set<String>>();

				// find runs on component, if any
				final RunsOnScope runsOnScope = alt.getMyScope().getScopeRunsOn();
				if (runsOnScope != null) {
					// processing ports from the component the alt statement
					// runs on
					final Component_Type runsOn = runsOnScope.getComponentType();
					if (runsOn != null) {
						addPortsOfComponent(runsOn, visiblePorts);
					}
				}

				// find parameter list of the enclosing definition
				final Definition parentDef = alt.getMyStatementBlock().getMyDefinition();
				FormalParameterList parameters = null;
				if (parentDef instanceof Def_Function) {
					parameters = ((Def_Function) parentDef).getFormalParameterList();
				} else if (parentDef instanceof Def_Testcase) {
					parameters = ((Def_Testcase) parentDef).getFormalParameterList();
				} else if (parentDef instanceof Def_Altstep) {
					parameters = ((Def_Altstep) parentDef).getFormalParameterList();
				}
				if (parameters != null) {
					// processing ports from the parameter list
					addPortsOfParameterList(parameters, visiblePorts);
				}
				// remove those types that are handled by any alt branch
				removeHandledByGuards(alt.getAltGuards(), visiblePorts);

				// report the remaining (if any) as a problem
				handleRemainingPorts(NOT_COVERED, alt.getLocation(), visiblePorts, problems);
			}
		}

		@Override
		public List<Class<? extends IVisitableNode>> getStartNode() {
			final List<Class<? extends IVisitableNode>> ret = new ArrayList<Class<? extends IVisitableNode>>(1);
			ret.add(Alt_Statement.class);
			return ret;
		}
	}

	private abstract static class Base extends BaseModuleCodeSmellSpotter {
		public Base() {
			super(CodeSmellType.ALTSTEP_COVERAGE);
		}

		void addPortsOfComponent(final Component_Type comp, final Map<Identifier, Set<String>> visiblePorts) {
			final CompilationTimeStamp timestamp = CompilationTimeStamp.getBaseTimestamp();
			for (final Definition def : comp.getComponentBody().getDefinitions()) {
				if (def instanceof Def_Port) {
					final Def_Port portDef = (Def_Port) def;
					final Identifier portId = portDef.getIdentifier();
					final Set<String> receivableTypes = new HashSet<String>();
					final Port_Type type = portDef.getType(timestamp);
					if (type != null) {
						final TypeSet incomingTypes = type.getPortBody().getInMessages();
						if (incomingTypes != null) {
							final int size = incomingTypes.getNofTypes();
							for (int i = 0; i < size; ++i) {
								receivableTypes.add(incomingTypes.getTypeByIndex(i).getTypename());
							}
						}
					}
					visiblePorts.put(portId, receivableTypes);
				}
			}
		}

		void addPortsOfParameterList(final FormalParameterList parameters, final Map<Identifier, Set<String>> visiblePorts) {
			final CompilationTimeStamp timestamp = CompilationTimeStamp.getBaseTimestamp();
			for (int i = 0; i < parameters.getNofParameters(); ++i) {
				final FormalParameter param = parameters.getParameterByIndex(i);
				IType t = param.getType(timestamp);
				if (t instanceof Referenced_Type) {
					t = t.getTypeRefdLast(timestamp);
				}
				if (t instanceof Port_Type) {
					final Port_Type type = (Port_Type) t;
					final Identifier portId = param.getIdentifier();
					final Set<String> receivableTypes = new HashSet<String>();
					final TypeSet incomingTypes = type.getPortBody().getInMessages();
					if (incomingTypes != null) {
						for (int j = 0; j < incomingTypes.getNofTypes(); ++j) {
							receivableTypes.add(incomingTypes.getTypeByIndex(j).getTypename());
						}
						visiblePorts.put(portId, receivableTypes);
					}
				}
			}
		}

		void removeHandledByGuards(final AltGuards guards, final Map<Identifier, Set<String>> visiblePorts) {
			// We also remove those ports that are ignored (not mentioned) in
			// the altguards,
			// as usually they are used for sending messages, not receiving
			// them.
			final List<Identifier> ports = new ArrayList<Identifier>();
			ports.addAll(visiblePorts.keySet());
			for (int i = 0; i < guards.getNofAltguards(); ++i) {
				final AltGuard ag = guards.getAltguardByIndex(i);
				if (ag instanceof Operation_Altguard) {
					final Operation_Altguard opAg = (Operation_Altguard) ag;
					final Statement action = opAg.getGuardStatement();
					if (action instanceof Receive_Port_Statement) {
						final Receive_Port_Statement receive = (Receive_Port_Statement) action;
						final ReceiveTemplateType typeVisitor = new ReceiveTemplateType();
						receive.accept(typeVisitor);
						if (receive.getPort() == null) {
							// This should mean an 'any port.receive(...)'
							// action
							if (typeVisitor.getReceivable() == null) {
								// template accepting any type (* or ?)
								visiblePorts.clear();
							} else {
								// specific type
								for (final Set<String> receiveFromPort : visiblePorts.values()) {
									receiveFromPort.remove(typeVisitor.getReceivable().getTypename());
								}
							}
						} else {
							// specific port
							ports.remove(receive.getPort().getId());
							if (typeVisitor.getReceivable() == null) {
								// template accepting any type (* or ?)
								final Set<String> receiveFromPort = visiblePorts.get(receive.getPort().getId());
								if (receiveFromPort != null) {
									receiveFromPort.clear();
								}
							} else {
								// specific type
								final Set<String> receiveFromPort = visiblePorts.get(receive.getPort().getId());
								if (receiveFromPort != null) {
									receiveFromPort.remove(typeVisitor.getReceivable().getTypename());
								}
							}
						}
					}
				}
			}
			for (final Identifier unusedPorts : ports) {
				visiblePorts.remove(unusedPorts);
			}
		}

		void handleRemainingPorts(final String prepend, final Location reportAt, final Map<Identifier, Set<String>> visiblePorts, final Problems problems) {
			// remove empty sets (those ports that have all their outgoing types
			// handled:
			final Iterator<Entry<Identifier, Set<String>>> it = visiblePorts.entrySet().iterator();
			while (it.hasNext()) {
				final Entry<Identifier, Set<String>> entry = it.next();
				if (entry.getValue().isEmpty()) {
					it.remove();
				}
			}

			// the remaining types are not handled:
			if (!visiblePorts.isEmpty()) {
				final StringBuilder sb = new StringBuilder(prepend);
				sb.append(" Unhandled cases:\n");
				for (final Entry<Identifier, Set<String>> entry: visiblePorts.entrySet()) {
					sb.append("port " + entry.getKey().getDisplayName() + ": ");
					final Iterator<String> typeIt = entry.getValue().iterator();
					if (typeIt.hasNext()) {
						sb.append(typeIt.next());
					}
					while (typeIt.hasNext()) {
						sb.append(", ").append(typeIt.next());
					}
					sb.append('\n');
				}
				problems.report(reportAt, sb.toString());
			}
		}
	}

	// not instantiable
	private AltstepCoverage() {
	}
}

final class ReceiveTemplateType extends ASTVisitor {
	// these are initialized according to the receive statement of the
	// constructor
	private IType receiveType;

	public IType getReceivable() {
		return receiveType;
	}

	@Override
	public int visit(final IVisitableNode node) {
		if (node instanceof TemplateInstance) {
			final TemplateInstance template = (TemplateInstance) node;
			if (template.getTemplateBody().isValue(CompilationTimeStamp.getBaseTimestamp())) {
				receiveType = template.getTemplateBody().getValue().getMyGovernor();
			} else {
				receiveType = template.getType();
			}
			return V_SKIP;
		}
		return V_CONTINUE;
	}
}
