/*******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/

package org.eclipse.titan.log.viewer.views.msc.ui.core;

import static org.eclipse.titan.log.viewer.views.msc.ui.core.RectangleDrawer.drawBorder;
import static org.eclipse.titan.log.viewer.views.msc.ui.core.RectangleDrawer.drawBox;
import static org.eclipse.titan.log.viewer.views.msc.ui.core.RectangleDrawer.drawShadow;
import static org.eclipse.titan.log.viewer.views.msc.ui.core.RectangleDrawer.getColor;
import static org.eclipse.titan.log.viewer.views.msc.util.MSCConstants.COMPONENT_FONT_COLOR;
import static org.eclipse.titan.log.viewer.views.msc.util.MSCConstants.COMPONENT_LINE_COLOR;
import static org.eclipse.titan.log.viewer.views.msc.util.MSCConstants.COMPONENT_SHADOW_COLOR;
import static org.eclipse.titan.log.viewer.views.msc.util.MSCConstants.COMPONENT_SHADOW_SIZE;
import static org.eclipse.titan.log.viewer.views.msc.util.MSCConstants.COMPONENT_TEXT_H_SPACING;
import static org.eclipse.titan.log.viewer.views.msc.util.MSCConstants.DEFAULT_BACKGROUND_COLOR;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.titan.log.viewer.Activator;
import org.eclipse.titan.log.viewer.views.msc.ui.view.IGC;
import org.eclipse.titan.log.viewer.views.msc.util.MSCConstants;

public abstract class ComponentEventNode extends MSCNode {

	private final Lifeline lifeline;

	public ComponentEventNode(final int eventOccurrence, final Lifeline lifeline) {
		super(eventOccurrence);
		this.lifeline = lifeline;
	}

	@Override
	public int getX() {
		return MSCConstants.COLUMN_WIDTH * this.lifeline.getIndex() + MSCConstants.COLUMN_SPACING / 2;
	}

	@Override
	public int getY() {
		return MSCConstants.ROW_HEIGHT * getStartOccurrence() + MSCConstants.ROW_SPACING / 2;
	}

	@Override
	public int getWidth() {
		return MSCConstants.COLUMN_WIDTH - MSCConstants.COLUMN_SPACING + COMPONENT_SHADOW_SIZE;
	}

	@Override
	public int getHeight() {
		return MSCConstants.ROW_HEIGHT - MSCConstants.ROW_SPACING + COMPONENT_SHADOW_SIZE;
	}

	@Override
	public boolean positiveDistanceToPoint(final int x, final int y) {
		return true;
	}

	@Override
	public void draw(final IGC context) {
		final Rectangle rectangle = new Rectangle(getX(), getY(), getWidth(), getHeight());
		drawShadow(context, rectangle, getColor(COMPONENT_SHADOW_COLOR), COMPONENT_SHADOW_SIZE);
		drawBox(context, rectangle, getBackgroundColor(), getColor(DEFAULT_BACKGROUND_COLOR), COMPONENT_SHADOW_SIZE);
		drawBorder(context, rectangle, getColor(COMPONENT_LINE_COLOR), COMPONENT_SHADOW_SIZE);

		drawText(context, rectangle);
	}

	private void drawText(final IGC context, final Rectangle rectangle) {
		context.setForeground(getColor(COMPONENT_FONT_COLOR));
		context.setFont((Font) Activator.getDefault().getCachedResource(MSCConstants.MSC_DEFAULT_FONT));
		context.drawTextTruncatedCentred(getName(),
				rectangle.x + COMPONENT_TEXT_H_SPACING / 2,
				rectangle.y,
				rectangle.width - COMPONENT_SHADOW_SIZE - COMPONENT_TEXT_H_SPACING,
				rectangle.height - COMPONENT_SHADOW_SIZE,
				true);
	}

	protected abstract Color getBackgroundColor();
}
