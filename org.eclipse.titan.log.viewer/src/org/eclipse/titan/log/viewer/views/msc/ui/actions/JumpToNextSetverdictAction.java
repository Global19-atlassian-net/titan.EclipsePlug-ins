/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.views.msc.ui.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.titan.log.viewer.views.MSCView;
import org.eclipse.titan.log.viewer.views.msc.model.ExecutionModel;
import org.eclipse.titan.log.viewer.views.msc.ui.view.MSCWidget;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Jumps to previous setverdict
 *
 */
public class JumpToNextSetverdictAction extends SelectionProviderAction {

	private IStructuredSelection selection;
	private final MSCView view;
	private final MSCWidget widget;

	public JumpToNextSetverdictAction(final MSCView view) {
		super(view.getMSCWidget(), "");
		this.view = view;
		this.widget = view.getMSCWidget();
	}

	@Override
	public void run() {
		if (this.widget == null) {
			return;
		}

		final ExecutionModel model = this.view.getModel();
		if (model == null) {
			return;
		}

		final int[] setverdictPlaces = model.getSetverdict();
		final int selectedLine = (Integer) this.selection.getFirstElement();
		selectSetVerdict(setverdictPlaces, selectedLine);
	}

	private void selectSetVerdict(final int[] setverdictPlaces, final int selectedLine) {
		for (final int setverdictPlace : setverdictPlaces) {
			if (setverdictPlace > selectedLine) {
				this.widget.setSelection(new StructuredSelection(setverdictPlace));
				return;
			}
		}
	}

	@Override
	public void selectionChanged(final IStructuredSelection selection) {
		this.selection = selection;

		final ExecutionModel model = this.view.getModel();
		if (model == null) {
			setEnabled(false);
			return;
		}

		boolean enable = false;
		final int selectedLine = (Integer) this.selection.getFirstElement();
		for (int j = 0; j < model.getSetverdict().length; j++) {
			if (model.getSetverdict()[j] > selectedLine) {
				enable = true;
				break;
			}
		}
		setEnabled(enable);
	}
}
