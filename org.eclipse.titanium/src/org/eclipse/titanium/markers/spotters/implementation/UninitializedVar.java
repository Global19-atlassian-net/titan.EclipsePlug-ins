/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.markers.spotters.implementation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Var;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Var_Template;
import org.eclipse.titanium.markers.spotters.BaseModuleCodeSmellSpotter;
import org.eclipse.titanium.markers.types.CodeSmellType;

public class UninitializedVar extends BaseModuleCodeSmellSpotter {
	private static final String TEMPLATE_ERROR_MESSAGE = "Variable templates should be initialized";
	private static final String VARIABLE_ERROR_MESSAGE = "Variables should be initialized";

	public UninitializedVar() {
		super(CodeSmellType.UNINITIALIZED_VARIABLE);
	}

	@Override
	protected void process(final IVisitableNode node, final Problems problems) {
		if (node instanceof Def_Var_Template) {
			final Def_Var_Template d = (Def_Var_Template)node;
			if (d.getInitialValue() == null) {
				problems.report(d.getLocation(), TEMPLATE_ERROR_MESSAGE);
			}
		} else if (node instanceof Def_Var) {
			final Def_Var d = (Def_Var)node;
			if (d.getInitialValue() == null) {
				problems.report(d.getLocation(), VARIABLE_ERROR_MESSAGE);
			}
		} else {
			return;
		}
	}

	@Override
	public List<Class<? extends IVisitableNode>> getStartNode() {
		final List<Class<? extends IVisitableNode>> ret = new ArrayList<Class<? extends IVisitableNode>>(2);
		ret.add(Def_Var_Template.class);
		ret.add(Def_Var.class);
		return ret;
	}

}