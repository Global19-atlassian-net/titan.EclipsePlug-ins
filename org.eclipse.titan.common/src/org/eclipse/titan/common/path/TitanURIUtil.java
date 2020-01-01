/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.common.path;

import java.net.URI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;

public final class TitanURIUtil {

	private TitanURIUtil() {
		// Disable constructor
	}

	/**
	 * Checks whether the source URI is a prefix of the other URI.
	 *
	 * @param source
	 *                the source URI to check
	 * @param other
	 *                the URI to check the source against
	 *
	 * @return true if the source is a prefix of the other URI, false
	 *         otherwise
	 * */
	public static boolean isPrefix(final URI source, final URI other) {
		if ((source.getScheme() == null && other.getScheme() != null) || !source.getScheme().equals(other.getScheme())) {
			return false;
		}

		final IPath sourcePath = new Path(source.getPath());
		final IPath otherPath = new Path(other.getPath());
		return sourcePath.isPrefixOf(otherPath);
	}

	/**
	 * Removes the last segment of the provided URI.
	 *
	 * @param original the original URI.
	 * @return the resulting URI.
	 * */
	public static URI removeLastSegment(final URI original) {
		final String lastSegment = URIUtil.lastSegment(original);
		if (lastSegment == null) {
			return original;
		}

		final String originalAsString = original.toString();
		final String newAsString = originalAsString.substring(0, originalAsString.length() - lastSegment.length() - 1);
		return URI.create(newAsString);
	}
}
