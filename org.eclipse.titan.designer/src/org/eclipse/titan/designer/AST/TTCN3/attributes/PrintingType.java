/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents the printing type for a JSON encoder function.
 *
 * @author Botond Baranyi
 */
public final class PrintingType extends ASTNode implements ILocateableNode {
	public enum PrintingTypeEnum {
		/** Printing type not set */
		NONE,
		/**
		 * Print without adding white spaces to make the JSON code as
		 * short as possible
		 */
		COMPACT,
		/**
		 * Make the JSON code easier to read by adding indenting and new
		 * lines
		 */
		PRETTY
	}

	private final PrintingTypeEnum printingType;
	
	public PrintingTypeEnum getPrintingType() {
		return printingType;
	}

	private CompilationTimeStamp lastTimeChecked;
	private Location location;

	public PrintingType(final PrintingTypeEnum printingType) {
		this.printingType = printingType;
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return location;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		builder.append(".<printing>");

		return builder;
	}

	/**
	 * Does the semantic checking of the printing type..
	 *
	 * @param timestamp
	 *                the time stamp of the actual semantic check cycle.
	 * */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (PrintingTypeEnum.NONE == printingType) {
			return false;
		}
		return true;
	}
}
