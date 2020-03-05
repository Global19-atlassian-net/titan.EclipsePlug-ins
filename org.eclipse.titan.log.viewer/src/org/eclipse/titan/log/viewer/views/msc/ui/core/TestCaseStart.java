/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.views.msc.ui.core;

import org.eclipse.swt.graphics.Color;
import org.eclipse.titan.log.viewer.Activator;
import org.eclipse.titan.log.viewer.views.msc.util.MSCConstants;

/**
 * Represents a test case start
 *
 */
public class TestCaseStart extends TestCaseEventNode {

	/**
	 * Constructor
	 * @param eventOccurrence the occurrence of this event
	 */
	public TestCaseStart(final int eventOccurrence, final int lifelines) {
		super(eventOccurrence, lifelines);
		this.lifelines = lifelines;
	}

	@Override
	public int getHeight() {
		return MSCConstants.ROW_HEIGHT - MSCConstants.ROW_SPACING;
	}

	@Override
	protected String getNodeText() {
		return "Test case " + getName() + " started.";
	}

	@Override
	protected Color getBackgroundColor() {
		return (Color) Activator.getDefault().getCachedResource(MSCConstants.MESSAGE_LINE_COLOR);
	}

	@Override
	public Type getType() {
		return Type.TESTCASE_START;
	}

}
