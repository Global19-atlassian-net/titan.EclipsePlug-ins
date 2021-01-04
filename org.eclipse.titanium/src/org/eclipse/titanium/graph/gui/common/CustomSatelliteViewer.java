/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.graph.gui.common;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

import org.eclipse.titanium.graph.components.EdgeDescriptor;
import org.eclipse.titanium.graph.components.NodeDescriptor;

import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.SatelliteTranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.SatelliteVisualizationViewer;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * This class implements a custom satellite view, to be able to resize it as we
 * need. This class is the subclass of {@link SatelliteVisualizationViewer}
 *
 * Satellite view is the small view that always shows the whole graph
 *
 * @author Gabor Jenei
 */
public class CustomSatelliteViewer extends SatelliteVisualizationViewer<NodeDescriptor, EdgeDescriptor> {
	private static final long serialVersionUID = 4410062275063286243L;
	private double actZoom;

	/**
	 * Constructor
	 * @param master : the {@link VisualizationViewer} of the whole big graph. (Usually part of the graph editor window)
	 */
	public CustomSatelliteViewer(final VisualizationViewer<NodeDescriptor, EdgeDescriptor> master) {
		super(master, new Dimension(600,600));
		actZoom = 1.0f;
		final PluggableGraphMouse satMouse = new PluggableGraphMouse() {
			@Override
			public void mouseWheelMoved(final MouseWheelEvent e) {
				float scale;
				if (e.getWheelRotation() > 0) {
					scale = 0.9f;
				} else {
					scale = 1.1f;
				}
				CrossoverScalingControl control = new CrossoverScalingControl();
				control.scale(getMaster(), scale, getCenter());
			}
		};

		satMouse.add(new SatelliteTranslatingGraphMousePlugin() {
			@Override
			public void mouseDragged(final MouseEvent arg0) {
				if (down == null) {
					return;
				}
				super.mouseDragged(arg0);
			}

			@Override
			@SuppressWarnings("unchecked")
			public void mousePressed(final MouseEvent e) {
				final VisualizationViewer<String, String> vv = (VisualizationViewer<String, String>) e.getSource();
				final boolean accepted = checkModifiers(e);
				if (accepted && vv instanceof SatelliteVisualizationViewer) {
					final VisualizationViewer<String, String> vvMaster = ((SatelliteVisualizationViewer<String, String>) vv).
							getMaster();

					final MutableTransformer modelTransformerMaster = vvMaster.getRenderContext().getMultiLayerTransformer()
							.getTransformer(Layer.LAYOUT);
					final Point2D orig = vvMaster.getRenderContext().getMultiLayerTransformer().inverseTransform(vvMaster.getCenter());
					final Point2D mouse = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(e.getPoint());
					modelTransformerMaster.translate(orig.getX() - mouse.getX(), orig.getY() - mouse.getY());
				}
				super.mousePressed(e);
			}
		});

		setGraphMouse(satMouse);
	}

	/**
	 * This method scales the view from its original size to the preferred size,
	 * it should not be called if the view is not in its original state
	 */
	@Override
	public void scaleToLayout(final ScalingControl scaler) {
		Dimension newSize = getPreferredSize();

		if (this.isShowing()) {
			newSize = getSize();
		}

		final Dimension currentSize = getGraphLayout().getSize();

		if (!newSize.equals(currentSize) && newSize.getWidth() > 0 && newSize.getHeight() > 0) {
			actZoom = newSize.getWidth() / currentSize.getWidth();
			scaler.scale(this, (float) (actZoom), new Point2D.Double());
		}
	}

	/**
	 * This method sets a new size for the view, it does also the needed
	 * zooming.
	 *
	 * @param size
	 *            : The size to set
	 */
	public void changeSize(final Dimension size) {
		setPreferredSize(size);
		final ScalingControl scaler = new CrossoverScalingControl();
		scaler.scale(this, (float) (1.0f / actZoom), new Point2D.Double());
		scaleToLayout(new CrossoverScalingControl());
		setSize(size);
	}

}