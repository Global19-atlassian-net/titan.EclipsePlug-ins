/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.jni;

import java.util.ArrayList;

/**
 * The callback interface that is used by the MainController to call back to the executor.
 *
 * @author Peter Dimitrov
 * */
public interface IJNICallback {
	void statusChangeCallback();
	void insertError(int severity, String msg);
	void errorCallback(int severity, String msg);
	void insertNotify(Timeval time, String source, int severity, String msg);
	void batchedInsertNotify(ArrayList<String[]> s);
	void notifyCallback(Timeval time, String source, int severity, String msg);
}
