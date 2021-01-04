/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.logging;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.titan.common.parsers.cfg.indices.LoggingSectionHandler.PluginSpecificParam;

/**
 * @author Balazs Andor Zalanyi
 * */
public class ParamDataLabelProvider extends LabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(final Object element, final int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(final Object element, final int columnIndex) {
		if (element != null && element instanceof PluginSpecificParam) {
			final PluginSpecificParam item = (PluginSpecificParam) element;
			switch (columnIndex) {
			case 0:
				final String param = item.getParamName();
				if (param == null || param.length() == 0) {
					return "";
				}
				return param;
			case 1:
				final String value = item.getValue().getText();
				if (value == null || value.length() == 0) {
					return "";
				}
				return value;
			default:
				return "";
			}
		}
		return "";
	}

}
