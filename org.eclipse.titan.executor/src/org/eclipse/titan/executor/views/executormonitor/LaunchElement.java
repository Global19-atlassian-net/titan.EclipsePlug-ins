/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.views.executormonitor;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.titan.executor.executors.BaseExecutor;
import org.eclipse.titan.executor.executors.TreeBranch;

/**
 * @author Kristof Szabados
 * */
public final class LaunchElement extends TreeBranch {
	private ILaunch launch;
	private boolean terminated;

	public LaunchElement(final String name, final ILaunch launch) {
		super(name);
		this.launch = launch;
		terminated = launch.isTerminated();
	}

	@Override
	public void dispose() {
		if (ExecutorStorage.getExecutorMap().containsKey(launch)) {
			final BaseExecutor executor = ExecutorStorage.getExecutorMap().get(launch);
			executor.dispose();
			ExecutorStorage.getExecutorMap().remove(launch);
		}
		LaunchStorage.getLaunchElementMap().remove(launch);
		launch = null;
		super.dispose();
	}

	public boolean getTerminated() {
		return terminated;
	}

	public void setTerminated() {
		terminated = true;
	}

	public ILaunch launch() {
		return launch;
	}

	public void changed() {
		terminated = launch.isTerminated();
	}
}
