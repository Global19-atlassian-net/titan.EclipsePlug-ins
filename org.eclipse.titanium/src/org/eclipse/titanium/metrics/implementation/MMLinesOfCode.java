/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.implementation;

import org.eclipse.titan.designer.AST.LargeLocation;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titanium.metrics.MetricData;
import org.eclipse.titanium.metrics.ModuleMetric;

public class MMLinesOfCode extends BaseModuleMetric {
	public MMLinesOfCode() {
		super(ModuleMetric.LINES_OF_CODE);
	}

	@Override
	public Number measure(final MetricData data, final Module module) {
		return ((LargeLocation) module.getLocation()).getEndLine();
	}
}
