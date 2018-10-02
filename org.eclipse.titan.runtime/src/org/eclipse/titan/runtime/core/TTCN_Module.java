/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;

/**
 * An experimental base class for a module.
 *
 * TODO: lots to implement
 *
 * @author Kristof Szabados
 */
public class TTCN_Module {
	//originally module_type_enum
	public static enum moduleTypeEnum {
		TTCN3_MODULE, ASN1_MODULE
	};

	private final moduleTypeEnum moduleType;
	public final String name;

	protected boolean pre_init_called = false;
	protected boolean post_init_called = false;

	public TTCN_Module(final String name, final moduleTypeEnum moduleType) {
		this.name = name;
		this.moduleType = moduleType;
	}

	public boolean set_module_param(final Param_Types.Module_Parameter param) {
		return false;
	}

	public void pre_init_module() {
		if (pre_init_called) {
			return;
		}
		pre_init_called = true;
	}

	public void post_init_module() {
		if (post_init_called) {
			return;
		}
		post_init_called = true;
		TTCN_Logger.log_module_init(name, false);
		TTCN_Logger.log_module_init(name, true);
	}

	public boolean start_ptc_function(final String function_name, final Text_Buf function_arguments) {
		function_arguments.cut_message();

		throw new TtcnError(MessageFormat.format("Internal error: Module {0} does not have startable functions.", name));
	}

	public boolean init_comp_type(final String component_type, final boolean init_base_comps) {
		throw new TtcnError(MessageFormat.format("Internal error: Module {0} does not have component types.", name));
	}

	public boolean init_system_port(final String component_type, final String port_name) {
		throw new TtcnError(MessageFormat.format("Internal error: Module {0} does not have a system port initializer function.", name));
	}

	public void control() {
		throw new TtcnError(MessageFormat.format("Module {0} does not have control part.", name));
	}

	public void execute_testcase(final String tescase_name) {
		throw new TtcnError(MessageFormat.format("Test case {0} does not exist in module {1}.", tescase_name, name));
	}

	public void execute_all_testcases() {
		throw new TtcnError(MessageFormat.format("Module {0} does not contain test cases.", name));
	}
}
