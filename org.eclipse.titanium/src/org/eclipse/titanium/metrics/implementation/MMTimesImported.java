/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.implementation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titanium.metrics.MetricData;
import org.eclipse.titanium.metrics.ModuleMetric;

public class MMTimesImported extends BaseModuleMetric {
	private final Map<Module, Integer> imported = new HashMap<Module, Integer>();

	public MMTimesImported() {
		super(ModuleMetric.TIMES_IMPORTED);
	}

	@Override
	public void init(final MetricData data) {
		imported.clear();
		final List<Module> modules = data.getModules();
		for (final Module module : modules) {
			imported.put(module, 0);
		}
		for (final Module module : modules) {
			for (final Module imp : module.getImportedModules()) {
				final Integer count = imported.get(imp);
				if (count != null) {
					imported.put(imp, count + 1);
				}
			}
		}
	}

	@Override
	public Number measure(final MetricData data, final Module module) {
		return imported.get(module);
	}
}
