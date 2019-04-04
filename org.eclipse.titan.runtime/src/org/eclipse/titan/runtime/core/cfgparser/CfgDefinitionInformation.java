/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core.cfgparser;

import java.util.List;

/**
 * @author eferkov
 * @author Arpad Lovassy
 */
public final class CfgDefinitionInformation {
	private String value = null;
	private List<CfgLocation> locations = null;

	public CfgDefinitionInformation(final String value, final List<CfgLocation> locations) {
		this.value = value;
		this.locations = locations;
	}

	public String getValue() {
		return value;
	}

	public List<CfgLocation> getLocations() {
		return locations;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		sb.append(value);
		sb.append(", ( ");
		boolean first = true;
		for (final CfgLocation l : locations ) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(l);
		}
		sb.append(" )");
		sb.append(" }");
		return sb.toString();
	}
}
