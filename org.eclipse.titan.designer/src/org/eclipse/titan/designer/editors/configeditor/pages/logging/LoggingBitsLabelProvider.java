/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.logging;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.titan.common.parsers.cfg.indices.LoggingBit;

/**
 * @author Kristof Szabados
 * */
public final class LoggingBitsLabelProvider extends LabelProvider {

	@Override
	public String getText(final Object element) {
		if (element != null && element instanceof LoggingBit) {
			LoggingBit bit = (LoggingBit) element;
			return (bit).getName();
		}

		return super.getText(element);
	}

}
