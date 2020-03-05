/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.topview;

import java.util.Set;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titanium.metrics.IMetricEnum;
import org.eclipse.titanium.metrics.utils.ModuleMetricsWrapper;

/**
 * Comparator class for the {@link TopView}.
 *
 * @author poroszd
 *
 */
class Comparator extends ViewerComparator {
	private final ModuleMetricsWrapper mw;
	private final Set<IMetricEnum> metrics;

	public Comparator(final ModuleMetricsWrapper m, final Set<IMetricEnum> ms) {
		mw = m;
		metrics = ms;
	}

	@Override
	public int compare(final Viewer viewer, final Object e1, final Object e2) {
		if (!(e1 instanceof Module && e2 instanceof Module)) {
			return 0;
		}
		final Module m1 = (Module) e1;
		final Module m2 = (Module) e2;

		double risk1 = 0;
		double risk2 = 0;
		for (final IMetricEnum m : metrics) {
			risk1 += mw.getRiskValue(m, m1.getName());
			risk2 += mw.getRiskValue(m, m2.getName());
		}

		final double r = risk2 - risk1;
		if (r < 0.0001) {
			return -1;
		} else if (r > 0.0001) {
			return 1;
		} else {
			return 0;
		}
	}
}
