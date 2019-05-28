/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.data;

import org.eclipse.titan.common.logging.ErrorReporter;

/**
 * @author Kristof Szabados
 * */
public final class ValidatorErrorHandler implements org.xml.sax.ErrorHandler {
	private static final String ERROR = "ERROR: ";
	private static final String FATAL_ERROR = "FATAL ERROR: ";
	private static final String WARNING = "WARNING: ";

	/** private constructor to disable instantiation. */
	private ValidatorErrorHandler() {
		// Do nothing
	}

	@Override
	public void error(final org.xml.sax.SAXParseException sAXParseException) throws org.xml.sax.SAXException {
		ErrorReporter.logError(ERROR + sAXParseException.toString());
	}

	@Override
	public void fatalError(final org.xml.sax.SAXParseException sAXParseException) throws org.xml.sax.SAXException {
		ErrorReporter.logError(FATAL_ERROR + sAXParseException.toString());
	}

	@Override
	public void warning(final org.xml.sax.SAXParseException sAXParseException) throws org.xml.sax.SAXException {
		ErrorReporter.logWarning(WARNING + sAXParseException.toString());
	}
}
