/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.extractors;

/**
 * Holder for ComponentsFetcher events
 *
 */
public class ComponentEvent {

	private final String compName;
	private final int progress;

	/**
	 * Constructor
	 *
	 * @param compName the name of the component
	 * @param progress the current progress (0 to 100)
	 */
	public ComponentEvent(final String compName, final int progress) {
		this.compName = compName;
		this.progress = progress;
	}

	/**
	 * Returns the component name
	 * @return the component name
	 */
	public String getCompName() {
		return this.compName;
	}

	/**
	 * Returns the current progress
	 * @return the current progress
	 */
	public int getProgress() {
		return this.progress;
	}
}
