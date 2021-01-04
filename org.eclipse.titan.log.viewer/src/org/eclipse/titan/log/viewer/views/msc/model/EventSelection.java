/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.views.msc.model;

import org.eclipse.jface.viewers.ISelection;

/**
 * This class represents an event selection in the MSC View
 *
 */
public class EventSelection implements ISelection {

	private final EventObject eventObject;
	private final String currTestCase;

	/**
	 * Constructor
	 */
	public EventSelection(final EventObject eventObject, final String currTestCase) {
		this.eventObject = eventObject;
		this.currTestCase = currTestCase;
	}

	@Override
	public boolean isEmpty() {
		return this.eventObject == null;
	}

	/**
	 * Returns the event object
	 * @return the event object (which can be null)
	 */
	public EventObject getEventObject() {
		return this.eventObject;
	}

	/**
	 * Returns the test case name
	 * @return the test case name
	 */
	public String getTestCaseName() {
		return this.currTestCase;
	}

}
