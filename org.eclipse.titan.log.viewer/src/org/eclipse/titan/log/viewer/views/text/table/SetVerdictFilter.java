/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.views.text.table;

import org.eclipse.titan.log.viewer.preferences.PreferencesHolder;

class SetVerdictFilter {
	boolean setverdictError;
	boolean setverdictFail;
	boolean setverdictInconc;
	boolean setverdictNone;
	boolean setverdictPass;

	public SetVerdictFilter(final PreferencesHolder preferences) {
		setverdictError = preferences.getSetverdictError();
		setverdictFail = preferences.getSetverdictFail();
		setverdictInconc = preferences.getSetverdictInconc();
		setverdictNone = preferences.getSetverdictNone();
		setverdictPass = preferences.getSetverdictPass();
	}

	public boolean isSetverdictError() {
		return setverdictError;
	}

	public boolean isSetverdictFail() {
		return setverdictFail;
	}

	public boolean isSetverdictInconc() {
		return setverdictInconc;
	}

	public boolean isSetverdictNone() {
		return setverdictNone;
	}

	public boolean isSetverdictPass() {
		return setverdictPass;
	}
}
