/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titanium.graph.gui.dialogs;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.titanium.error.GUIErrorHandler;
import org.eclipse.titanium.graph.components.NodeDescriptor;
import org.eclipse.titanium.metrics.IMetricEnum;
import org.eclipse.titanium.metrics.MetricGroup;
import org.eclipse.titanium.metrics.StatColumn;
import org.eclipse.titanium.metrics.Statistics;
import org.eclipse.titanium.metrics.preferences.PreferenceManager;
import org.eclipse.titanium.metrics.utils.ModuleMetricsWrapper;
import org.eclipse.titanium.metrics.utils.WrapperStore;
import org.eclipse.titanium.swt.SWTResourceManager;

/**
 * This class makes a {@link Dialog} and shows the informations of a selected
 * node of module graph
 *
 * @author Gabor Jenei
 * @see GraphView
 */
public class InfoWindow extends Dialog {
	private final Table table;
	private final NodeDescriptor module;
	private final ModuleMetricsWrapper metricsProvider;
	private final IMetricEnum chosenMetric;
	private final Shell shell;
	protected final GUIErrorHandler errorHandler = new GUIErrorHandler();

	private static final Point WINDOW_SIZE = new Point(300, 300);
	private static final Point TABLE_BORDER_SIZE = new Point(15, 40);
	private static final int COLUMN_WIDTH = 150;

	/**
	 * Creates a small dialog that is used to show information about a certain
	 * graph node.
	 *
	 * @param node
	 *            : The node to show information about (it should be a node ID)
	 * @param chosenMetric
	 *            : The chosen metric, it is needed to set node colours.
	 * @param parent
	 *            : A reference to the parent shell
	 */
	public InfoWindow(final NodeDescriptor node, final IMetricEnum chosenMetric, final Shell parent) {
		super(parent);
		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
		shell.setSize(WINDOW_SIZE);
		shell.setText("Module information - " + node.getDisplayName());
		shell.setImage(SWTResourceManager.getImage("resources/icons/metrics_top_worst.gif"));
		shell.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(final Event ev) {
				if (table != null && shell != null) {
					final Point size = shell.getSize();
					table.setSize(size.x - TABLE_BORDER_SIZE.x, size.y - TABLE_BORDER_SIZE.y);
				}
			}
		});

		this.chosenMetric = chosenMetric;
		setText("Module information - " + node.getDisplayName());

		this.metricsProvider = WrapperStore.getWrapper(node.getProject());

		module = node;

		table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
		table.setSize(WINDOW_SIZE.x - TABLE_BORDER_SIZE.x, WINDOW_SIZE.y - TABLE_BORDER_SIZE.y);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		final TableColumn tmpCol = new TableColumn(table, SWT.NONE);
		tmpCol.setText("Property");
		tmpCol.setWidth(COLUMN_WIDTH);
		for (final StatColumn actCol : StatColumn.values()) {
			final TableColumn tempCol = new TableColumn(table, SWT.BORDER);
			tempCol.setText(actCol.getName());
			tempCol.setWidth(COLUMN_WIDTH);
		}

		open();
	}

	/**
	 * Fills the table with metrics data, and shows the InfoWindow
	 *
	 * @param node
	 *            : The module to show info about.
	 */
	private void createContents(final NodeDescriptor node) {
		if (node.isMissing()) {
			errorHandler.reportErrorMessage("The module \"" + node.getDisplayName() + "\" cannot be found on the disk!");
			return;
		}

		final String shownName = node.getDisplayName();
		List<String> actRow = null;

		addRow(new ArrayList<String>(Arrays.asList("General Information")), Color.lightGray);
		addRow(new ArrayList<String>(Arrays.asList("Module name", shownName)), Color.white);

		for (final MetricGroup type : new MetricGroup[] { MetricGroup.MODULE }) {
			addRow(new ArrayList<String>(Arrays.asList(type.getGroupName() + " metrics")), Color.lightGray);

			for (final IMetricEnum metric : type.getMetrics()) {
				if (!PreferenceManager.isEnabledOnModuleGraph(metric)) {
					continue;
				}

				actRow = new ArrayList<String>();
				actRow.add(metric.getName());
				String val = null;
				final Number tempVal = metricsProvider.getValue(metric, module.getName());
				if (tempVal != null) {
					val = tempVal.toString();
				}
				actRow.add(val);

				if (chosenMetric.equals(metric)) {
					addRow(actRow, node.getColor());
				} else {
					addRow(actRow, Color.white);
				}
			}
		}

		for (final MetricGroup type : new MetricGroup[] { MetricGroup.ALTSTEP, MetricGroup.FUNCTION, MetricGroup.TESTCASE }) {
			addRow(new ArrayList<String>(Arrays.asList(type.getGroupName())), Color.lightGray);
			for (final IMetricEnum metric : type.getMetrics()) {
				if (!PreferenceManager.isEnabledOnModuleGraph(metric)) {
					continue;
				}
				actRow = new ArrayList<String>();
				actRow.add(metric.getName());
				final Statistics stat = metricsProvider.getStats(metric, module.getName());

				for (final StatColumn actCol : StatColumn.values()) {
					final Number val = stat == null ? null : stat.get(actCol);
					actRow.add(val == null ? "-" : val.toString());
				}

				if (chosenMetric.equals(metric)) {
					addRow(actRow, node.getColor());
				} else {
					addRow(actRow, Color.white);
				}
			}
		}
	}

	/**
	 * Open the dialog.
	 *
	 * @return constant null
	 */
	private Object open() {
		createContents(module);
		shell.open();
		shell.layout();
		final Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return null;
	}

	/**
	 * Adds a new row to the table
	 *
	 * @param rowData
	 *            : A string containing the cells
	 * @param rowColour
	 *            : The background color of the row
	 */
	private void addRow(final List<String> rowData, final Color rowColour) {
		final TableItem actItem = new TableItem(table, SWT.BORDER);
		actItem.setBackground(SWTResourceManager.getColor(new RGB(rowColour.getRed(), rowColour.getGreen(), rowColour.getBlue())));
		String[] row = new String[rowData.size()];
		int i = 0;
		for (final String cell : rowData) {
			row[i++] = cell;
		}

		actItem.setText(row);
	}

	/**
	 * Method to call upon closing the graph view
	 */
	public void dispose() {
		if (!shell.isDisposed()) {
			shell.close();
		}
	}
}