/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.parsers.ttcn3parser;

public interface ITtcn3FileReparser {

	/**
	 * Runs the reparsing process
	 * @return true if syntactically outdated
	 *         false otherwise
	 */
	public boolean parse();
}
