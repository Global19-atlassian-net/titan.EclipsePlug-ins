/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
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
 * Representation of a component creation in the sequence diagram
 *
 */
public class ComponentCreation extends ComponentEventNode {

	/**
	 * Constructor
	 *
	 * @param eventOccurrence the event occurrence for the creation of this component
	 * @param lifeline the life line of the event
	 */
	public ComponentCreation(final int eventOccurrence, final Lifeline lifeline) {
		super(eventOccurrence, lifeline);
	}

	@Override
	protected Color getBackgroundColor() {
		return (Color) Activator.getDefault().getCachedResource(MSCConstants.COMPONENT_BG_COLOR);
	}

	@Override
	public Type getType() {
		return Type.COMPONENT_CREATION;
	}

}
