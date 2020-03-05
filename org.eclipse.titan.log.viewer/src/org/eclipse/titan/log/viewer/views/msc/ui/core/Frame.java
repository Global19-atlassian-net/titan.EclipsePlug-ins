/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.views.msc.ui.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.titan.log.viewer.Activator;
import org.eclipse.titan.log.viewer.views.msc.model.MSCModel;
import org.eclipse.titan.log.viewer.views.msc.ui.view.IGC;
import org.eclipse.titan.log.viewer.views.msc.util.MSCConstants;
import org.eclipse.titan.log.viewer.views.msc.util.MSCNodeComparator;

/**
 * Represents the base sequence diagram graph nodes container
 */
public class Frame extends BasicFrame {
	private final MSCModel model;
	private boolean hasChildren = false;

	private List<Lifeline> lifelineNodes;
	private List<LifelineHeader> lifelineHeaderNodes;

	private final MSCNodeComparator comparator;
	private int selectedLine = 3;

	private String name = ""; //$NON-NLS-1$

	private List<MSCNode> nodeCache = null;

	/**
	 * Constructor
	 */
	public Frame(final MSCModel model) {
		this.comparator = new MSCNodeComparator();
		this.model = model;
	}

	public MSCModel getModel() {
		return model;
	}

	/**
	 * Set the graph node name.<br>
	 * It is the name display in the view to label the graph node.
	 * @param nodeName name to set
	 */
	public void setName(final String nodeName) {
		this.name = nodeName;
	}

	/**
	 * Returns the graph node name.<br>
	 * It is the name display in the view to label the graph node.
	 * @return the graph node name
	 */
	public String getName()	{
		return this.name;
	}

	/**
	 * Returns the lifeline at the given index in the lifelines array
	 * @param index the position in the lifeline array
	 * @return the lifeline
	 */
	public Lifeline getLifeline(final int index) {
		if ((getLifelines() != null) && (index >= 0) && (index < lifeLinesCount())) {
			return getLifelines().get(index);
		}
		return null;
	}

	/**
	 * Returns the number of lifelines stored in the frame
	 * @return the number of lifelines
	 */
	public int lifeLinesCount()	{
		final List<Lifeline> lifelines = getLifelines();
		if (lifelines != null) {
			return lifelines.size();
		}

		return 0;
	}

	/**
	 * Returns a list of all lifelines known by this frame.
	 * Known lifelines are the only one which can be displayed on screen.
	 * @return the lifelines list
	 */
	private List<Lifeline> getLifelines() {
		if (!this.hasChildren) {
			return new ArrayList<Lifeline>();
		}

		return lifelineNodes;
	}

	/**
	 * Moves the to most first position (first to the left)
	 * @param lifeLine the lifeline to move
	 */
	public void moveLifeLineToPosition(final Lifeline lifeLine, final int position) {
		// Validate position
		if ((position == lifeLine.getIndex()) || (position > lifeLinesCount())) {
			return;
		}

		// Update index of lifelines
		final int oldPosition = lifeLine.getIndex();

		if (oldPosition > position) {
			moveLeft(oldPosition, position);
		} else {
			moveRight(oldPosition, position);
		}

		// Set new index
		lifeLine.setIndex(position);

		// Update index
		Collections.<Lifeline>sort(getLifelines(), this.comparator);
	}

	private void moveLeft(final int oldPosition, final int newPosition) {
		final List<Lifeline> lifelines = getLifelines();
		for (final Lifeline currLifeLine : lifelines) {
			final int currLifeLineIndex = currLifeLine.getIndex();
			if ((currLifeLineIndex < oldPosition) && (currLifeLineIndex >= newPosition)) {
				currLifeLine.setIndex(currLifeLineIndex + 1);
			}
		}
	}

	private void moveRight(final int oldPosition, final int newPosition) {
		final List<Lifeline> lifelines = getLifelines();
		for (final Lifeline currLifeLine : lifelines) {
			final int currLifeLineIndex = currLifeLine.getIndex();
			if ((currLifeLineIndex > oldPosition) && (currLifeLineIndex <= newPosition)) {
				currLifeLine.setIndex(currLifeLineIndex - 1);
			}
		}
	}

	/**
	 * Adds a lifeline to the frame lifelines list.
	 * The lifeline X drawing order depends on the lifeline
	 * addition order into the frame lifelines list.
	 *
	 * @param lifeLine the lifeline to add
	 */
	public void addLifeLine(final Lifeline lifeLine) {
		if (lifeLine == null) {
			return;
		}
		//set the lifeline parent frame
		lifeLine.setFrame(this);
		//Increate the frame lifeline counter
		//and set the lifeline drawing order
		lifeLine.setIndex(getNewHorizontalIndex());
		//add the lifeline to the lifelines list

		if (!this.hasChildren) {
			lifelineNodes = new ArrayList<Lifeline>();
			lifelineHeaderNodes = new ArrayList<LifelineHeader>();
			this.hasChildren = true;
		}
		lifelineNodes.add(lifeLine);
	}

	/**
	 * Adds a lifeline header to the frame's lifeline headers list.
	 *
	 * @param lifeLineHeader the lifeline header to add
	 */
	public void addLifeLineHeader(final LifelineHeader lifeLineHeader) {
		if (!this.hasChildren) {
			lifelineNodes = new ArrayList<Lifeline>();
			lifelineHeaderNodes = new ArrayList<LifelineHeader>();
			this.hasChildren = true;
		}

		lifelineHeaderNodes.add(lifeLineHeader);
	}

	/**
	 * Gets the closest lifeline for an x value
	 *
	 * @param x the x value
	 * @return the closest lifeline
	 */
	public Lifeline getCloserLifeline(final int x) {
		int index = x / MSCConstants.COLUMN_WIDTH  - 1;
		if (index < 0) {
			index = 0;
		}
		if (index > lifeLinesCount()) {
			index = lifeLinesCount();
		}
		return getLifeline(index);
	}

	/**
	 * Returns the graph node which contains the point given in parameter
	 * WARNING: Only graph nodes in the current visible area can be returned
	 * @param x the x coordinate of the point to test
	 * @param y the y coordinate of the point to test
	 * @return the graph node containing the point given in parameter, null otherwise
	 */
	public MSCNode getNodeAt(final int x, final int y) {
		if (!this.hasChildren) {
			return null;
		}

		final MSCNode toReturn = getHeaderNodeFromListAt(x, y, lifelineHeaderNodes, 0);
		if (toReturn != null) {
			return toReturn;
		}

		if (nodeCache == null) {
			return null;
		}

		return getNodeFromListAt(x, y, nodeCache, 0);
	}

	/**
	 * Draws the children nodes on the given context.<br>
	 * This method start width GraphNodes ordering if needed.<br>
	 * After, depending on the visible area, only visible GraphNodes are drawn.<br>
	 * @param context the context to draw to
	 * @see org.eclipse.hyades.uml2sd.ui.core.MSCNode#draw(IGC)
	 */
	protected void drawChildsNodes(final IGC context) {
		if (!this.hasChildren) {
			return;
		}

		int arrayStep = 1;
		if ((MSCConstants.getFontHeight() + MSCConstants.MESSAGES_TEXT_VERTICAL_SPACING * 2) * context.getZoom() < MSCConstants.MESSAGE_SIGNIFICANT_VSPACING) {
			arrayStep = Math.round(MSCConstants.MESSAGE_SIGNIFICANT_VSPACING / ((MSCConstants.getFontHeight()
					+ MSCConstants.MESSAGES_TEXT_VERTICAL_SPACING * 2) * context.getZoom()));
		}

		// Draw all nodes
		drawLifeLineNodes(context, lifelineNodes, 0, arrayStep);

		final int yStart = (context.getContentsY() - MSCConstants.ROW_SPACING / 2) / MSCConstants.ROW_HEIGHT;
		final int yEnd = (context.getContentsY() + context.getVisibleHeight() - MSCConstants.ROW_SPACING / 2) / MSCConstants.ROW_HEIGHT;
		nodeCache = model.getNodes(yStart, yEnd + 3);

		drawNodes(context, nodeCache, 0, arrayStep);

		drawLifeLineNodes(context, lifelineHeaderNodes, 0, arrayStep);
	}

	private int drawLifeLineNodes(final IGC context, final List<? extends MSCNode> list, final int startIndex, final int step)	{
		if (!this.hasChildren || list.isEmpty()) {
			return 0;
		}

		int nodesCount = 0;
		final int contextX = context.getContentsX();
		final int contextY = context.getContentsY();
		final int contextWidth = context.getVisibleWidth();
		final int contextHeight = context.getVisibleHeight();

		for (int i = startIndex; i < list.size(); i = i + step) {
			final MSCNode toDraw = list.get(i);
			// ***Common*** nodes visibility
			if (toDraw.isVisible(contextX, contextY, contextWidth, contextHeight)) {
				toDraw.draw(context);
				nodesCount++;
			}
		}

		return nodesCount;
	}

	/**
	 * Draw the GraphNode stored in the given list, starting at
	 * index startIndex with the given step
	 * @param context the context to draw to
	 * @param list the GraphNodes list
	 * @param startIndex the start index
	 * @param step the step to browse the list
	 * @return the number of GraphNodes drawn
	 */
	private int drawNodes(final IGC context, final List<MSCNode> list, final int startIndex, final int step) {
		if (!this.hasChildren || list.isEmpty()) {
			return 0;
		}

		final int contextX = context.getContentsX();
		final int contextY = context.getContentsY();
		final int contextWidth = context.getVisibleWidth();
		final int contextHeight = context.getVisibleHeight();

		final List<MSCNode> shouldBeDrawn = new ArrayList<MSCNode>();
		for (int i = startIndex; i < list.size(); i = i + step) {
			final MSCNode toDraw = list.get(i);
			// ***Common*** nodes visibility
			if (toDraw.isVisible(contextX, contextY, contextWidth, contextHeight)) {
				shouldBeDrawn.add(toDraw);
			}
		}

		for (final MSCNode temp : shouldBeDrawn) {
			if (temp instanceof SetverdictUnknown) {
				((SetverdictUnknown) temp).setNumLifelines(lifeLinesCount());
			}
			temp.draw(context);
		}

		return shouldBeDrawn.size();
	}

	/**
	 * Draws the contents of the frame.
	 *
	 * @param context the graphical context to use
	 * */
	public void draw(final IGC context) {
		if (!this.hasChildren) {
			return;
		}

		// Draw selection
		final int x = 0;
		final int y = MSCConstants.ROW_HEIGHT * this.selectedLine + MSCConstants.ROW_HEIGHT; // Lifeline Header
		final int width = getWidth();
		final int height = MSCConstants.ROW_HEIGHT;
		context.setBackground((Color) Activator.getDefault().getCachedResource(MSCConstants.SELECTION_COLOR));
		context.fillRectangle(x, y, width, height);

		// Draw children
		setVisibleArea(context.getContentsX(), context.getContentsY(), context.getVisibleWidth(), context.getVisibleHeight());
		drawChildsNodes(context);
	}

	/**
	 * @return the selectedLine
	 */
	public int getSelectedLine() {
		return this.selectedLine;
	}

	/**
	 * @param selectedLine the selectedLine to set
	 */
	public void setSelectedLine(final int selectedLine) {
		// Two first and last rows reserved for System and MTC creation/termination
		if (selectedLine < 4) {
			this.selectedLine = 3;
		} else if (selectedLine > (getMaxEventOccurrence() - 4)) {
			this.selectedLine = getMaxEventOccurrence() - 4;
		} else {
			this.selectedLine = selectedLine;
		}
	}
}
