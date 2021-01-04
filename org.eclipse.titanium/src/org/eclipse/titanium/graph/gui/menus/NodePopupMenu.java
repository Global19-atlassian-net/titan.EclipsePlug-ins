/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.graph.gui.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titanium.graph.components.EdgeDescriptor;
import org.eclipse.titanium.graph.components.NodeColours;
import org.eclipse.titanium.graph.components.NodeDescriptor;
import org.eclipse.titanium.graph.utils.CheckDependentNodes;
import org.eclipse.titanium.graph.utils.CheckParallelPaths;
import org.eclipse.titanium.graph.visualization.GraphHandler;
import org.eclipse.titanium.utils.LocationHighlighter;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * This class implements the popup menu shown upon graph node click by right
 * mouse button
 *
 * @author Gabor Jenei
 * @author Bianka Bekefi
 */
public class NodePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = 1L;
	protected NodeDescriptor node;
	protected final GraphHandler handler;
	protected final JMenuItem goToDefinition = new JMenuItem("Go to definition");

	/**
	 * Constructor
	 *
	 * @param handler
	 *            : The handler of the analyzed graph
	 */
	public NodePopupMenu(final GraphHandler handler) {
		node = null;
		final JMenuItem selectNode = new JMenuItem("Select node");
		final JMenuItem getParalellPaths = new JMenuItem("Search paralell paths");
		final JMenuItem getDependentNodes = new JMenuItem("Show dependent nodes");
		final NodePopupMenu thisPopUpMenu = this;

		this.handler = handler;

		selectNode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final VisualizationViewer<NodeDescriptor, EdgeDescriptor> actVisualisator = thisPopUpMenu.handler.getVisualizator();
				if (actVisualisator == null) {
					return;
				}
				final Layout<NodeDescriptor, EdgeDescriptor> tmpLayout = actVisualisator.getGraphLayout();
				if (tmpLayout == null) {
					return;
				}
				final Graph<NodeDescriptor, EdgeDescriptor> g = tmpLayout.getGraph(); //actVisualisator.getGraphLayout().getGraph();
				if (g == null) {
					return;
				}
				if (node == null) {
					ErrorReporter.logError("null node attribute for NodePopupMenu");
					return;
				}

				actVisualisator.getPickedVertexState().clear();
				actVisualisator.getPickedVertexState().pick(node, true);
				actVisualisator.getPickedEdgeState().clear();
				for (final EdgeDescriptor edge : g.getIncidentEdges(node)) {
					actVisualisator.getPickedEdgeState().pick(edge, true);
				}
			}
		});

		getParalellPaths.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final VisualizationViewer<NodeDescriptor, EdgeDescriptor> actVisualisator = thisPopUpMenu.handler.getVisualizator();
				if (actVisualisator == null) {
					return;
				}

				final Layout<NodeDescriptor, EdgeDescriptor> tmpLayout = actVisualisator.getGraphLayout();
				if (tmpLayout == null) {
					return;
				}

				if (node == null) {
					ErrorReporter.logError("null node attribute for NodePopupMenu");
					return;
				}

				final Graph<NodeDescriptor, EdgeDescriptor> g = tmpLayout.getGraph(); //actVisualisator.getGraphLayout().getGraph();
				final Job searchJob = new Job("Searching for parallel paths...") {
					@Override
					protected IStatus run(final IProgressMonitor monitor) {
						CheckParallelPaths<NodeDescriptor, EdgeDescriptor> checker = new CheckParallelPaths<NodeDescriptor, EdgeDescriptor>(g, node);
						if (checker.hasParallelPaths()) {
							for (Deque<EdgeDescriptor> list : checker.getPaths()) {
								for (EdgeDescriptor edge : list) {
									edge.setColour(NodeColours.DARK_RED);
								}
							}
						}
						actVisualisator.repaint();
						return Status.OK_STATUS;
					}

				};
				searchJob.schedule();
			}
		});

		getDependentNodes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final VisualizationViewer<NodeDescriptor, EdgeDescriptor> actVisualisator = thisPopUpMenu.handler.getVisualizator();
				if (actVisualisator == null) {
					return;
				}

				final Layout<NodeDescriptor, EdgeDescriptor> tmpLayout = actVisualisator.getGraphLayout();
				if (tmpLayout == null) {
					return;
				}

				final Graph<NodeDescriptor, EdgeDescriptor> graph = tmpLayout.getGraph();
				if (graph == null) {
					return;
				}
				if (node == null) {
					ErrorReporter.logError("null node attribute for NodePopupMenu");
					return;
				}

				actVisualisator.getPickedVertexState().clear();
				actVisualisator.getPickedVertexState().pick(node, true);
				actVisualisator.getPickedEdgeState().clear();

				final CheckDependentNodes<NodeDescriptor, EdgeDescriptor> cdn = new CheckDependentNodes<NodeDescriptor, EdgeDescriptor>(graph);
				final List<NodeDescriptor> neighbors = new ArrayList<NodeDescriptor>();
				final List<NodeDescriptor> dependentNodes = new ArrayList<NodeDescriptor>();
				neighbors.add(node);
				dependentNodes.add(node);

				while(!neighbors.isEmpty()) {
					final NodeDescriptor neighbor = neighbors.get(0);
					final List<EdgeDescriptor> edges2 = new ArrayList<EdgeDescriptor>(graph.getOutEdges(neighbor));

					for (final EdgeDescriptor edge : edges2) {
						final NodeDescriptor vertex = graph.getDest(edge);
						if (!dependentNodes.contains(vertex)) {
							neighbors.add(vertex);
							dependentNodes.add(vertex);
						}
					}
					neighbors.remove(0);
				}

				int index = dependentNodes.size() -1;
				boolean changed = true;
				while(changed) {
					changed = false;
					index = dependentNodes.size() - 1;
					while(index >= 0) {
						final NodeDescriptor neighbor = dependentNodes.get(index);
						if (!cdn.isDependent(neighbor, dependentNodes) && !neighbor.equals(node)) {
							changed = true;
							dependentNodes.remove(index);
						}
						index--;
					}
				}

				index = dependentNodes.size() - 1;
				while(index >= 0) {
					final NodeDescriptor neighbor = dependentNodes.get(index);
					if (cdn.isDependent(neighbor, dependentNodes) && !neighbor.equals(node)) {
						actVisualisator.getPickedVertexState().pick(neighbor, true);
						final List<EdgeDescriptor> edges3 = new ArrayList<EdgeDescriptor>(graph.getInEdges(neighbor));
						for (final EdgeDescriptor edge : edges3) {
							actVisualisator.getPickedEdgeState().pick(edge, true);
						}

						final List<EdgeDescriptor> edges4 = new ArrayList<EdgeDescriptor>(graph.getOutEdges(neighbor));
						for (final EdgeDescriptor edge : edges4) {
							if (dependentNodes.contains(graph.getDest(edge))) {
								actVisualisator.getPickedEdgeState().pick(edge, true);
							}
						}
					}
					index--;
				}
			}
		});
		add(selectNode);
		add(getParalellPaths);
		add(getDependentNodes);

		goToDefinition.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (node != null) {
							final Location loc = node.getLocation();
							if (loc == null || loc instanceof NULL_Location) {
								return;
							}
							LocationHighlighter.jumpToLocation(loc);
						}
					}
				});
			}
		});

		add(goToDefinition);
	}

	protected NodePopupMenu(final String label) {
		super(label);
		handler = null;
	}

	/**
	 * Show the popup menu
	 *
	 * @param node
	 *            : The node where we show the menu
	 * @param x
	 *            : The X coordinate of the node
	 * @param y
	 *            : The Y coordinate of the node
	 */
	public void show(final NodeDescriptor node, final int x, final int y) {
		this.node = node;
		super.show(handler.getVisualizator(), x, y);
	}

	/**
	 * Adds a new menu entry to the popup menu
	 *
	 * @param title
	 *            : The entry's title
	 * @param listener
	 *            : The action listener for the entry
	 */
	public void addEntry(final String title, final ActionListener listener) {
		final JMenuItem newItem = new JMenuItem(title);
		newItem.addActionListener(listener);
		add(newItem);
	}

	/**
	 * Enables/disables the go to definition menu entry
	 *
	 * @param value
	 *            : True if entry should be enabled
	 */
	public void enableGoToDefinition(final boolean value) {
		goToDefinition.setEnabled(value);
	}

}
