/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.graph.gui.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.titanium.graph.components.EdgeDescriptor;
import org.eclipse.titanium.graph.components.NodeDescriptor;
import org.eclipse.titanium.graph.gui.dialogs.InfoWindow;
import org.eclipse.titanium.graph.visualization.MeasureableGraphHandler;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * This class implements the popup menu on a graph that has metrics
 *
 * @author Gabor Jenei
 */
public class MeasureableNodePopupMenu extends NodePopupMenu {
	private static final long serialVersionUID = 1L;
	protected JMenuItem showInfo;
	protected InfoWindow infwind;

	/**
	 * Constructor
	 *
	 * @param handler
	 *            : The graph handler of a graph that has metrics (the
	 *            associated graph)
	 */
	public MeasureableNodePopupMenu(final MeasureableGraphHandler handler, final Shell parent) {
		super(handler);
		showInfo = new JMenuItem("Show Info Window");
		showInfo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final VisualizationViewer<NodeDescriptor, EdgeDescriptor> actVisualisator = handler.getVisualizator();
				if (actVisualisator == null) {
					return;
				}
				final Layout<NodeDescriptor, EdgeDescriptor> tmpLayout = actVisualisator.getGraphLayout();
				if (tmpLayout == null) {
					return;
				}
				final Graph<NodeDescriptor, EdgeDescriptor> g = actVisualisator.getGraphLayout().getGraph();

				actVisualisator.getPickedVertexState().clear();
				actVisualisator.getPickedVertexState().pick(node, true);
				actVisualisator.getPickedEdgeState().clear();
				for (final EdgeDescriptor edge : g.getIncidentEdges(node)) {
					actVisualisator.getPickedEdgeState().pick(edge, true);
				}

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (infwind != null) {
							infwind.dispose();
						}
						infwind = new InfoWindow(node, handler.getChosenMetric(), parent);
					}
				});
			}
		});
		add(showInfo);
	}

	/**
	 * Sets the Info Window menu item enabled or disabled
	 *
	 * @param value
	 *            : the <code>boolean</code> value to set
	 */
	public void enableInfoWindow(final boolean value) {
		showInfo.setEnabled(value);
	}

}
