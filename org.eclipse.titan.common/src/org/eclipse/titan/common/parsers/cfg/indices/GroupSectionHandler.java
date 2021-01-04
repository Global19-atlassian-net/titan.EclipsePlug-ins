/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.common.parsers.cfg.indices;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;

/**
 * @author Kristof Szabados
 * @author Arpad Lovassy
 * */
public final class GroupSectionHandler extends ConfigSectionHandlerBase {

	private List<Group> groups = new ArrayList<Group>();

	public static class Group {
		private ParseTree root = null;
		private ParseTree groupName = null;
		private List<GroupItem> groupItems = new ArrayList<GroupItem>();

		public ParseTree getRoot() {
			return root;
		}

		public void setRoot(final ParseTree root) {
			this.root = root;
		}

		public ParseTree getGroupName() {
			return groupName;
		}

		public void setGroupName(final ParseTree groupName) {
			this.groupName = groupName;
		}

		public List<GroupItem> getGroupItems() {
			return groupItems;
		}

		public void setGroupItems(final List<GroupItem> groupItems) {
			this.groupItems = groupItems;
		}
	}

	public static class GroupItem {
		private ParseTree item = null;

		public GroupItem(final ParseTree item) {
			this.item = item;
		}

		public ParseTree getItem() {
			return item;
		}

		public void setItem(final ParseTree item) {
			this.item = item;
		}
	}

	public List<Group> getGroups() {
		return groups;
	}

	public void setGroups(final List<Group> groups) {
		this.groups = groups;
	}

}
