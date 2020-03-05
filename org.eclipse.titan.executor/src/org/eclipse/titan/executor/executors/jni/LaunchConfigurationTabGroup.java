/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.executors.jni;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.titan.executor.tabpages.hostcontrollers.HostControllersTab;
import org.eclipse.titan.executor.tabpages.maincontroller.JNIMainControllerTab;
import org.eclipse.titan.executor.tabpages.performance.JniPerformanceSettingsTab;
import org.eclipse.titan.executor.tabpages.testset.TestSetTab;

/**
 * @author Kristof Szabados
 * */
public final class LaunchConfigurationTabGroup implements ILaunchConfigurationTabGroup {
	private ILaunchConfigurationTab[] tabs;

	@Override
	public void createTabs(final ILaunchConfigurationDialog arg0, final String arg1) {
		tabs = new ILaunchConfigurationTab[] {new JNIMainControllerTab(this), new HostControllersTab(this), new TestSetTab(),
				new JniPerformanceSettingsTab(), new EnvironmentTab(), new CommonTab()};
	}

	@Override
	public void dispose() {
		if (null != tabs) {
			for (final ILaunchConfigurationTab tab : tabs) {
				tab.dispose();
			}
		}
	}

	@Override
	public ILaunchConfigurationTab[] getTabs() {
		return tabs;
	}

	@Override
	public void initializeFrom(final ILaunchConfiguration arg0) {
		for (final ILaunchConfigurationTab tab : tabs) {
			tab.initializeFrom(arg0);
		}
	}

	@Override
	public void launched(final ILaunch arg0) {
		// Do nothing
	}

	@Override
	public void performApply(final ILaunchConfigurationWorkingCopy arg0) {
		for (final ILaunchConfigurationTab tab : tabs) {
			tab.performApply(arg0);
		}
	}

	@Override
	public void setDefaults(final ILaunchConfigurationWorkingCopy arg0) {
		for (final ILaunchConfigurationTab tab : tabs) {
			tab.setDefaults(arg0);
		}
	}

}
