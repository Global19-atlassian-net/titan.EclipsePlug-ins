/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer;

public final class DebugUtils {

	/**
	 * Returns the first "depth" number of elements of the call chain,
	 * e.g.  (internalDoAnalyzeWithReferences -> markAllMarkersForRemoval -> markMarkersForRemoval)
	 *
	 * @param the depth
	 * @return the first "depth" number of elements of the call chain
	 * */
	// Returns the first "depth" number of elements of the call chain,
	// e.g.  (internalDoAnalyzeWithReferences -> markAllMarkersForRemoval -> markMarkersForRemoval)
	public static String getStackTrace(final int depth) {
		if (depth < 1) {
			return "";
		}

		final StringBuilder builder = new StringBuilder("(");
		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (int i = depth+1 > stackTrace.length - 1 ? stackTrace.length - 1 : depth+1;i > 1; i--) {
			final String[] classPath = stackTrace[i].getClassName().split("\\.");
			if (classPath.length > 0) {
				builder.append(classPath[classPath.length -1]).append(':');
			}
			builder.append(stackTrace[i].getMethodName());
			if (i>2) {
				builder.append(" -> ");
			}
		}
		builder.append(')');
		return builder.toString();
	}

	/**
	 * @return a String representation of the current unix-style timestamp on the system
	 */
	public static String getTimestamp() {
		return String.valueOf(System.currentTimeMillis());
	}
}