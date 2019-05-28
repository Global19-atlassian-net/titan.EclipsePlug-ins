/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

/**
 * Represents the address attribute that can be assigned to port types, in order
 * to enable the support of the address type inside the port too.
 *
 * @author Kristof Szabados
 * */
public final class AddressPortTypeAttribute extends PortTypeAttribute {

	@Override
	/** {@inheritDoc} */
	public PortType_type getPortTypeType() {
		return PortType_type.ADDRESS;
	}
}
