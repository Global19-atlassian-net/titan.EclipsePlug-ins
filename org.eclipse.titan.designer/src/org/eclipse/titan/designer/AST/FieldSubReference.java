/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameterList;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * The FieldSubReference class represents a part of a TTCN3 or ASN.1 reference, which was given in fieldreference notation ('.field1').
 *
 * @author Kristof Szabados
 * */
public final class FieldSubReference implements ISubReference, ILocateableNode {
	public static final String INVALIDSUBREFERENCE = "Invalid field reference `{0}'': type `{1}'' does not have fields.";
	public static final String NONEXISTENTSUBREFERENCE = "Reference to a non-existent field `{0}'' in type `{1}''";
	//	public static final String INVALIDREFERENCE1 = "Type `{0}'' does not have fields.";

	private final Identifier fieldId;

	public FieldSubReference(final Identifier fieldId) {
		this.fieldId = fieldId;
	}

	@Override
	/** {@inheritDoc} */
	public Subreference_type getReferenceType() {
		return Subreference_type.fieldSubReference;
	}

	@Override
	/** {@inheritDoc} */
	public Identifier getId() {
		return fieldId;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		//Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		//Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public String getFullName() {
		return "";
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		return new StringBuilder();
	}

	@Override
	/** {@inheritDoc} */
	public INamedNode getNameParent() {
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public void setFullNameParent(final INamedNode nameParent) {
		//Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		if (null != fieldId) {
			fieldId.setLocation(location);
		}
	}

	// Location is optimized not to store an object as it is not needed
	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return new Location(fieldId.getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public String toString() {
		return "fieldSubReference: " + fieldId.getDisplayName();
	}

	@Override
	/** {@inheritDoc} */
	public void appendDisplayName(final StringBuilder builder) {
		if (builder.length() > 0) {
			builder.append('.');
		}
		builder.append(fieldId.getDisplayName());
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		reparser.updateLocation(fieldId.getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		//Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public boolean accept(final ASTVisitor v) {
		switch (v.visit(this)) {
		case ASTVisitor.V_ABORT: return false;
		case ASTVisitor.V_SKIP: return true;
		}
		if (fieldId != null) {
			if (!fieldId.accept(v)) {
				return false;
			}
		}
		if (v.leave(this)==ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}

	/**
	 * Generates getter/setter name without "get"/"set" for TTCN-3 record fields,
	 * which will be class member variables in java
	 * @return aTtcn3RecField TTCN-3 record field name
	 */
	public static String getJavaGetterName( final String aTtcn3RecField ) {
		//TODO at this point this function no longer looks useful, lets just keep it for some time, in case we need it.
		return aTtcn3RecField.replace('.', '_');
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression(final FormalParameterList formalParameterList) {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final ExpressionStruct expression, final boolean isFirst ) {
		//TODO actually there should be several ways to generate a reference
		// for example left and right side usage in an assignment
		if (fieldId != null) {
			if (isFirst) {
				expression.expression.append( fieldId.getName() );
			} else {
				expression.expression.append(MessageFormat.format(".get_field_{0}()", getJavaGetterName(fieldId.getName())));
			}
		}
	}
}
