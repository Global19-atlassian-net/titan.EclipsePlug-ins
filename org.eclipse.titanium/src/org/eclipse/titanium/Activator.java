/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium;

import java.net.URL;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.titanium"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * Default constructor for the activator.
	 * Called by the system when the plug-in gets activated.
	 * */
	public Activator() {
		setDefault(this);
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		setDefault(null);
		super.stop(context);
	}

	/**
	 * Sets the default singleton instance of this plug-in,
	 * that later can be used to access plug-in specific preference settings.
	 *
	 * @param activator the single instance of this plug-in class.
	 * */
	private static void setDefault(final Activator activator) {
		if (plugin == null) {
			plugin = activator;
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static URL getResourcePath() {
		return getDefault().getBundle().getResource("resources");
	}

}
