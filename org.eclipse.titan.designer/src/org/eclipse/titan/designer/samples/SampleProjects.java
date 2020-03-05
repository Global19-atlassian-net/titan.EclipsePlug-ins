/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.samples;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Szabolcs Beres
 * */
public final class SampleProjects {
	private static final Map<String, SampleProject> AVAILABLE_PROJECTS = new HashMap<String, SampleProject>();
	static {
		final SampleProject emptyProject = new EmptyProjectSample();
		final HelloWorldSample helloWorld = new HelloWorldSample();

		AVAILABLE_PROJECTS.put(emptyProject.getName(), emptyProject);
		AVAILABLE_PROJECTS.put(helloWorld.getName(), helloWorld);
	}

	public static Map<String, SampleProject> getProjects() {
		return AVAILABLE_PROJECTS;
	}

	/** private constructor to disable instantiation */
	private SampleProjects() {
		// Do nothing
	}
}
