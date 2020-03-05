/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.views.notification;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.titan.executor.executors.BaseExecutor;
import org.eclipse.titan.executor.executors.ITreeLeaf;
import org.eclipse.titan.executor.views.executormonitor.LaunchElement;
import org.eclipse.titan.executor.views.executormonitor.MainControllerElement;

/**
 * @author Kristof Szabados
 * */
public final class NotificationContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {
		// Do nothing
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		if (inputElement instanceof LaunchElement) {
			final List<ITreeLeaf> children = ((LaunchElement) inputElement).children();
			for (final ITreeLeaf aChildren : children) {
				final BaseExecutor executor = ((MainControllerElement) aChildren).executor();
				if (null != executor && null != executor.notifications()) {
					return executor.notifications().toArray();
				}
			}
		}
		return new String[] {};
	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		// Do nothing
	}
}
