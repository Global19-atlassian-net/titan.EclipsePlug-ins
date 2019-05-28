/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represent an error behavior setting in the codec API of the run-time
 * environment. The setting contains the error type identifier and the way of
 * error handling.
 *
 * @author Kristof Szabados
 */
public final class ErrorBehaviorSetting extends ASTNode implements IIncrementallyUpdateable {
	private final String errorType;
	private final String errorHandling;

	/**
	 * The location of the whole setting. This location encloses the setting
	 * fully, as it is used to report errors to.
	 **/
	private Location location;

	public ErrorBehaviorSetting(final String errorType, final String errorHandling) {
		this.errorType = errorType;
		this.errorHandling = errorHandling;
		location = NULL_Location.INSTANCE;
	}

	public void setLocation(final Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

	public String getErrorType() {
		return errorType;
	}

	public String getErrorHandling() {
		return errorHandling;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		// no members
		return true;
	}
}
