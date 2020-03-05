/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.visitors;

import org.eclipse.titan.designer.AST.ASTVisitor;

/**
 * A general aid visitor that counts something.
 * <p>
 * This class extends the {@link org.eclipse.titan.designer.AST.ASTVisitor
 * ASTVisitor} with a new {@link Counter} field, which can be used internally in
 * the {@link #visit(org.eclipse.titan.designer.AST.IVisitableNode)} and the
 * {@link #leave(org.eclipse.titan.designer.AST.IVisitableNode)} methods to
 * count something.
 * <p>
 * A normal use case is like:<br>
 * {@code Counter n = new Counter(0);
 *
 * module.accept(new CounterVisitor(n) <br>
 * <code>@Override</code><br>
 * public visit(IVisitableNode node) {<br>
 *
 * <pre>
 * if (node instanceof Def_Function)
 * 	n.inc();
 * </pre>
 *
 * <br>
 * }<br>
 * });<br>
 * // now n.val() is the number of function definitions in module.<br>
 * }
 *
 * @author poroszd
 *
 */
public class CounterVisitor extends ASTVisitor {
	protected final Counter count;

	public CounterVisitor(final Counter n) {
		count = n;
	}
}
