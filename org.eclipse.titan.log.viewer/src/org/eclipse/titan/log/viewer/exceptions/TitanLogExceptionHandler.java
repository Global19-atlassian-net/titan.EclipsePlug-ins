/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.exceptions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.log.viewer.utils.Constants;
import org.eclipse.titan.log.viewer.utils.Messages;

/**
 * Class for managing Exceptions
 *
 */
public final class TitanLogExceptionHandler {

	private TitanLogExceptionHandler() {
	}

	/**
	 * Responsible for show and label an exception dialog. The
	 * Error is also written to the console
	 * @param exception, the exception to be handled
	 */
	//FIXME check if we need this at all
	public static void handleException(final Throwable exception) {
		if (Constants.DEBUG) {
			assert exception != null;
		}
		final Display display = Display.getDefault();
		if (exception instanceof UserException) {
			final UserException userException = (UserException) exception;
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openError(null, Messages.getString("TitanLogExceptionHandler.0"), userException.getMessage());
				}
			});
		} else if (exception instanceof TechnicalException) {
			final TechnicalException technicalException = (TechnicalException) exception;
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openError(null, Messages.getString("TitanLogExceptionHandler.1"), technicalException.getMessage());
					ErrorReporter.logExceptionStackTrace(technicalException);
				}
			});
		} else {
			final Exception otherException = (Exception) exception;
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openError(null, Messages.getString("TitanLogExceptionHandler.2"), otherException.getMessage());
					ErrorReporter.logExceptionStackTrace(otherException);
				}
			});
		}
	}
}
