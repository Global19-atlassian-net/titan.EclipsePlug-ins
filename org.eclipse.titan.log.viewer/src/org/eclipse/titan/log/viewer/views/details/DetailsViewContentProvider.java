/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.views.details;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringTokenizer;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.titan.log.viewer.console.TITANDebugConsole;
import org.eclipse.ui.IViewSite;

/**
 * This class builds the tree view
 *
 */
public class DetailsViewContentProvider implements ITreeContentProvider {

	private TreeParent invisibleRoot;
	private String value = ""; //$NON-NLS-1$
	private String sourceInfo = "";

	@Override
	public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {
		if (newInput == null || oldInput == null) {
			this.value = ""; //$NON-NLS-1$
			buildTreeModel2();
			v.refresh();
			return;
		}

		final DetailData oI = (DetailData) oldInput;
		final DetailData nI = (DetailData) newInput;

		sourceInfo = nI.getSourceInfo();

		String oldValue = oI.getLine();
		final String newValue = nI.getLine();

		if (newValue == null) {
			TITANDebugConsole.getConsole().newMessageStream().println("c");
			this.value = ""; //$NON-NLS-1$
			buildTreeModel2();
			v.refresh();
			return;
		}

		if (oldValue == null) {
			oldValue = ""; //$NON-NLS-1$
		}

		if ((newValue.length() >= 2) && newValue.startsWith(": ")) { //$NON-NLS-1$
			this.value = newValue.substring(2);
		} else {
			this.value = newValue;
		}

		final String name = nI.getName();
		final String port = nI.getPort();
		if ((port != null) && (!port.trim().isEmpty())) {
			buildTreeModel(name + '(' + port + ')');
		} else {
			buildTreeModel(name);
		}
		v.refresh();
	}

	@Override
	public void dispose() {
		// Do nothing
	}

	@Override
	public Object[] getElements(final Object parent) {

		if (parent instanceof IViewSite) {
			return getChildren(this.invisibleRoot);

		}

		return getChildren(parent);
	}

	@Override
	public Object getParent(final Object child) {
		if (child instanceof TreeObject) {
			return ((TreeObject) child).getParent();
		}
		return null;
	}

	@Override
	public Object [] getChildren(final Object parent) {
		if (parent instanceof TreeParent) {
			return ((TreeParent) parent).getChildren();
		}
		return new Object[0];
	}

	@Override
	public boolean hasChildren(final Object parent) {
		return parent instanceof TreeParent
				&& ((TreeParent) parent).hasChildren();
	}

	/**
	 *  Build the tree hierarchy.
	 */
	private void buildTreeModel(final String namePort) {

		final String logString = this.value;
		final StringTokenizer tokenizer = new StringTokenizer(logString, "{},", true);
		final Deque<TreeParent> parentStack = new ArrayDeque<TreeParent>();
		final TreeParent root = new TreeParent(namePort);
		addSourceInfo(root);

		if (!logString.startsWith("{")) {
			root.addChild(new TreeObject(logString.trim()));
		} else {
			String prev = "";
			String token = "";
			while (tokenizer.hasMoreTokens()) {
				prev = token;
				token = tokenizer.nextToken().trim();

				if (",".equals(token) || "".equals(token)) {
					continue;
				}

				//			Set TreeParent named or unnamed and add to parentStack
				if ("{".equals(token)) {
					if (prev.endsWith(":=")) {
						final String parentName = prev.substring(0, prev.indexOf(' '));
						parentStack.push(new TreeParent(parentName));
					} else {
						parentStack.push(new TreeParent(""));
					}
					continue;
				}


				//			End of TreeParent is found if this TreeParent has a parent
				//			we add it as it's child. If there is no parent we add it to
				//			root.
				if (token.endsWith("}")) {
					if (!parentStack.isEmpty()) {
						final TreeParent tmpParent = parentStack.peek();
						parentStack.pop();
						if (!parentStack.isEmpty()) {
							parentStack.peek().addChild(tmpParent);
						} else {
							root.addChild(tmpParent);
						}
					}

					continue;
				}

				//			add child to parent or root.
				if (!(token.endsWith(":="))) {

					if (!parentStack.isEmpty()) {
						parentStack.peek().addChild(new TreeObject(token));
						continue;
					}

					root.addChild(new TreeObject(token));
				}

			}
		}

		this.invisibleRoot = new TreeParent("");
		this.invisibleRoot.addChild(root);

		parentStack.clear();
	}

	private void addSourceInfo(final TreeParent root) {
		if (this.sourceInfo != null && this.sourceInfo.trim().length() > 0) {
			root.addChild(new TreeObject("sourceInfo := " + this.sourceInfo));
		} else {
			root.addChild(new TreeObject("<SourceInfoFormat:=Single>"));
		}
	}

	/**
	 * Build an empty tree hierarchy.
	 */
	private void buildTreeModel2() {
		this.invisibleRoot = new TreeParent("");	//$NON-NLS-1$
	}
}


