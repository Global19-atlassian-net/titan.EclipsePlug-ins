/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.implementation;

import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titanium.metrics.FunctionMetric;
import org.eclipse.titanium.metrics.MetricData;
import org.eclipse.titanium.metrics.visitors.Counter;
import org.eclipse.titanium.metrics.visitors.DepthVisitor;

public class FMNesting extends BaseFunctionMetric {
	public FMNesting() {
		super(FunctionMetric.NESTING);
	}

	@Override
	public Number measure(final MetricData data, final Def_Function function) {
		final Counter count = new Counter(0);
		function.accept(new DepthVisitor(count));
		return count.val();
	}
}
