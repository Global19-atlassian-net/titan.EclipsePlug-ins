/*******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/

package org.eclipse.titan.log.viewer.views.msc.ui.core;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.titan.log.viewer.Activator;
import org.eclipse.titan.log.viewer.views.msc.ui.view.IGC;
import org.eclipse.titan.log.viewer.views.msc.util.MSCConstants;

public final class RectangleDrawer {

	private RectangleDrawer() {
		// Hide constructor
	}

	public static void drawBox(final IGC context, final Rectangle rectangle, final Color backgroundColor, final Color gradientColor, final int shadowSize) {
		context.setBackground(backgroundColor);
		if (MSCConstants.DRAW_GRADIENT) {
			context.setGradientColor(gradientColor);
			context.fillGradientRectangle(rectangle.x, rectangle.y, rectangle.width - shadowSize, rectangle.height - shadowSize, true);
		} else {
			context.fillRectangle(rectangle.x, rectangle.y, rectangle.width - shadowSize, rectangle.height - shadowSize);
		}
	}

	public static void drawBorder(final IGC context, final Rectangle rectangle, final Color lineColor, final int shadowSize) {
		if (MSCConstants.DRAW_BORDER) {
			context.setForeground(lineColor);
			context.drawRectangle(rectangle.x, rectangle.y, rectangle.width - shadowSize, rectangle.height - shadowSize);
		}
	}

	public static void drawShadow(final IGC context, final Rectangle rectangle, final Color shadowColor, final int shadowSize) {
		if (MSCConstants.DRAW_SHADOW) {
			context.setLineStyle(context.getLineSolidStyle());
			context.setLineWidth(MSCConstants.NORMAL_LINE_WIDTH);
			context.setBackground(shadowColor);
			context.fillRectangle(rectangle.x + shadowSize, rectangle.y + shadowSize, rectangle.width, rectangle.height);
		}
	}

	public static Color getColor(final String key) {
		return (Color) Activator.getDefault().getCachedResource(key);
	}

	public static void drawText(final IGC context, final Rectangle rectangle, final String nodeText, final Color fontColor, final Font font, final int shadowSize) {
		context.setForeground(fontColor);
		context.setFont(font);
		context.drawTextTruncatedCentred(nodeText, rectangle.x, rectangle.y, rectangle.width - shadowSize, rectangle.width - shadowSize, true);
	}
}
