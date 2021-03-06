/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.Object;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Identifier;

/**
 * Class to represent FieldName.
 * FieldName is a sequence of PrimitiveFieldNames.
 *
 * @author Kristof Szabados
 */
public final class FieldName extends ASTNode {
	private final List<Identifier> fields;

	public FieldName() {
		fields = new ArrayList<Identifier>(1);
	}

	public FieldName newInstance() {
		final FieldName temp = new FieldName();

		for (final Identifier field : fields) {
			temp.addField(field.newInstance());
		}

		return temp;
	}

	public String getDisplayName() {
		final StringBuilder builder = new StringBuilder();

		for (final Identifier field : fields) {
			builder.append('.').append(field.getDisplayName());
		}

		return builder.toString();
	}

	public void addField(final Identifier identifier) {
		if (null == identifier) {
			return;
		}

		fields.add(identifier);
	}

	public int getNofFields() {
		return fields.size();
	}

	public Identifier getFieldByIndex(final int index) {
		return fields.get(index);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (fields != null) {
			for (final Identifier id : fields) {
				if (!id.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}
}
