/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.preferences.pages;

/**
 * Helper class used to represent a colorable element in the Syntax highlight
 * selection tree.
 * 
 * @author Kristof Szabados
 */
final class SyntaxHighlightColoringElement implements ISyntaxHighlightTreeElement {
	private ISyntaxHighlightTreeElement parent;
	private final String name;
	private final String basePreferenceKey;
	private String words;

	public SyntaxHighlightColoringElement(final String name, final String key) {
		this.name = name;
		this.basePreferenceKey = key;
		this.words = null;
	}

	public SyntaxHighlightColoringElement(final String name, final String key, final String example) {
		this.name = name;
		this.basePreferenceKey = key;
		this.words = example;
	}

	public SyntaxHighlightColoringElement(final String name, final String key, final String[] words) {
		this.name = name;
		this.basePreferenceKey = key;

		if (words != null) {
			final StringBuilder builder = new StringBuilder();
			for (int i = 0; i < words.length; i++) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(words[i]);
			}
			this.words = builder.toString();
		} else {
			this.words = null;
		}
	}

	public ISyntaxHighlightTreeElement getParent() {
		return parent;
	}

	public void setParent(final ISyntaxHighlightTreeElement treeElement) {
		parent = treeElement;
	}

	public ISyntaxHighlightTreeElement[] getChildren() {
		return new ISyntaxHighlightTreeElement[]{};
	}

	public String getName() {
		return name;
	}

	public String getBasePreferenceKey() {
		return basePreferenceKey;
	}

	public String getWords() {
		return words;
	}
}
