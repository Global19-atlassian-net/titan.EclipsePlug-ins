/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.graph.gui.utils;

import javax.swing.JRadioButtonMenuItem;

/**
 * This is a subclass of {@link JRadioButtonMenuItem}, it makes possible for the
 * menu entries to have a separate name to display (long name) and a shorter
 * code
 *
 * @author Gabor Jenei
 *
 */
public class LayoutEntry extends JRadioButtonMenuItem {
	private static final long serialVersionUID = 753825087884977221L;
	protected String wholeName;
	protected String shortName;

	/**
	 * Constructs a layout entry with a given short and display name
	 *
	 * @param code
	 *            : the short name to use inside the source code
	 * @param name
	 *            : the name shown on the frame
	 */
	public LayoutEntry(final String code, final String name) {
		super(name);
		wholeName = name;
		shortName = code;
	}

	/**
	 * @return returns the long name of the entry
	 */
	@Override
	public String getName() {
		return wholeName;
	}

	/**
	 * @return returns the short name of the entry
	 */
	public String getCode() {
		return shortName;
	}

	/**
	 * In case of <code>static</code> LayoutEntry attributes this method should
	 * be used in order to have separate event handlers.
	 */
	public LayoutEntry newInstance() {
		return new LayoutEntry(shortName, wholeName);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof LayoutEntry)) {
			return false;
		} else {
			return ((LayoutEntry) obj).getCode().equals(shortName);
		}
	}

	@Override
	public int hashCode() {
		return shortName.hashCode();
	}
}