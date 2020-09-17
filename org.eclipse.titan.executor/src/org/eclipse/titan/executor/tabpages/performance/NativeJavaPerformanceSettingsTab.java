/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.tabpages.performance;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Kristof Szabados
 * */
public class NativeJavaPerformanceSettingsTab extends BasePerformanceSettingsTab {

	@Override
	public void executorSpecificControls(final Composite pageComposite) {
		createConsoleLoggingArea(pageComposite);
		createSeverityLevelExtractionArea(pageComposite);
	}


}
