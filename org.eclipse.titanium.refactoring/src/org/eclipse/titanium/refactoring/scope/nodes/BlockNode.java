/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.scope.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Location;

/**
 * A node representing a StatementBlock.
 *
 * @author Viktor Varga
 */
public class BlockNode extends Node {

	protected StatementNode parent;

	protected final List<StatementNode> sts;

	public BlockNode(final IVisitableNode astNode) {
		super(astNode);
		this.sts = new ArrayList<StatementNode>();
	}

	public StatementNode getParent() {
		return parent;
	}
	public List<StatementNode> getStatements() {
		return sts;
	}

	public StatementNode getPreviousStatement(final StatementNode sn) {
		final int ind = sts.indexOf(sn);
		if (ind <= 0) {
			return null;
		}
		return sts.get(ind-1);
	}
	public StatementNode getNextStatement(final StatementNode sn) {
		final int ind = sts.indexOf(sn);
		if (ind >= sts.size()-1) {
			return null;
		}
		return sts.get(ind+1);
	}

	public void setParent(final StatementNode parent) {
		this.parent = parent;
	}
	public void addStatement(final StatementNode st) {
		sts.add(st);
	}

	@Override
	protected boolean containsNode(final Node n) {
		if (this.equals(n)) {
			return true;
		}
		for (StatementNode sn: sts) {
			if (sn.containsNode(n)) {
				return true;
			}
		}
		return false;
	}

	/** Returns false if they are equal. */
	@Override
	public boolean isBlockAncestorOfThis(final BlockNode bn) {
		if (this.equals(bn)) {
			return false;
		}
		BlockNode parent = this.parent == null ? null : this.parent.parent;
		while (parent != null && !parent.equals(bn)) {
			parent = parent.parent == null ? null : parent.parent.parent;
		}

		return parent != null;
	}
	@Override
	public boolean isStmtAncestorOfThis(final StatementNode sn) {
		StatementNode parent = this.getParent();
		while (parent != null && !parent.equals(sn)) {
			parent = parent.parent == null ? null : parent.parent.parent;
		}

		return parent != null;
	}

	public BlockNode findSmallestCommonAncestorBlock(final BlockNode bn) {
		if (bn.equals(this)) {
			return this;
		}
		if (bn.isBlockAncestorOfThis(this)) {
			return this;
		} else if (this.isBlockAncestorOfThis(bn)) {
			return bn;
		} else {
			BlockNode parentNode = this.parent == null ? null : this.parent.parent;
			while (parentNode != null) {
				if (bn.isBlockAncestorOfThis(parentNode)) {
					return parentNode;
				}
				parentNode = this.parent == null ? null : this.parent.parent;
			}
			return null;
		}
	}

	public StatementNode findStmtInBlockWhichContainsNode(final Node containedNode) {
		if (!containedNode.isBlockAncestorOfThis(this)) {
			//System.err.println("x");
			return null;
		}
		//System.err.println("containedNode: " + containedNode + ", loc: " + Utils.createLocationString(containedNode.astNode));
		//System.err.println("block: " + this + ", loc: " + Utils.createLocationString(this.astNode));
		final ListIterator<StatementNode> it = sts.listIterator();
		while (it.hasNext()) {
			final StatementNode currSt = it.next();
			//System.err.println("    st: " + currSt + ", loc: " + Utils.createLocationString(currSt.astNode));
			if (containedNode.equals(currSt) || containedNode.isStmtAncestorOfThis(currSt)) {
				return currSt;
			}
		}
		//invalid state
		//System.err.println("y");
		return null;
	}


	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("BN(").append(astNode.toString()).append("), loc: ");
		if (astNode instanceof ILocateableNode) {
			final Location loc = ((ILocateableNode)astNode).getLocation();
			sb.append(loc.getOffset()).append('-').append(loc.getEndOffset()).append(';');
		} else {
			sb.append("<none>;");
		}
		sb.append(" parent: ");
		if (parent == null) {
			sb.append("<null>");
		} else {
			sb.append("SN(").append(parent.astNode.toString()).append(')');
		}
		return sb.toString();
	}

	public String toStringRecursive(final boolean recursive, final int prefixLen) {
		final String prefix = new String(new char[prefixLen]).replace('\0', ' ');
		final StringBuilder sb = new StringBuilder();
		sb.append(prefix).append("BN: ").append(toString()).append('\n');
		if (recursive) {
			for (StatementNode sn: sts) {
				sb.append(sn.toStringRecursive(true, prefixLen+2)).append('\n');
			}
		}
		return sb.toString();
	}
}
