/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.values;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.values.ObjectIdentifierComponent;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class RelativeObjectIdentifier_Value extends Value {
	private final List<ObjectIdentifierComponent> objectIdComponents = new ArrayList<ObjectIdentifierComponent>();

	@Override
	/** {@inheritDoc} */
	public Value_type getValuetype() {
		return Value_type.RELATIVEOBJECTIDENTIFIER_VALUE;
	}

	public void addObjectIdComponent(final ObjectIdentifierComponent component) {
		if (null == component) {
			return;
		}

		objectIdComponents.add(component);
		component.setFullNameParent(this);
		component.setMyScope(myScope);
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		ObjectIdentifierComponent component;
		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			component = objectIdComponents.get(i);
			if (component == child) {
				return builder.append(INamedNode.DOT).append(i + 1);
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_ROID;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		if (!isAsn()) {
			builder.append("objid ");
		}

		builder.append('{');
		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			if (i > 0) {
				builder.append(' ');
			}
			builder.append(objectIdComponents.get(i).createStringRepresentation());
		}
		builder.append('}');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			objectIdComponents.get(i).setMyScope(scope);
		}
	}

	public int getNofComponents() {
		return objectIdComponents.size();
	}

	/**
	 * Appends its own object identifier component parts to the provided
	 * list.
	 *
	 * @param components
	 *                the list to be extended
	 * */
	public void getOidComponents(final JavaGenData aData, final List<String> components) {
		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			objectIdComponents.get(i).getOidComponents(aData, components);
		}
	}

	@Override
	/** {@inheritDoc} */
	public IValue getReferencedSubValue(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference,
			final IReferenceChain refChain) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return this;
		}

		final IType type = myGovernor.getTypeRefdLast(timestamp);
		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(ArraySubReference.INVALIDVALUESUBREFERENCE, type.getTypename()));
			return null;
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId()
							.getDisplayName(), type.getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(ParameterisedSubReference.INVALIDVALUESUBREFERENCE);
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		checkROID(timestamp, referenceChain);

		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			if (objectIdComponents.get(i).isVariable()) {
				return true;
			}
		}

		return false;
	}

	public void checkROID(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (objectIdComponents.isEmpty()) {
			return;
		}

		if (!referenceChain.add(this)) {
			return;
		}

		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			referenceChain.markState();
			objectIdComponents.get(i).checkROID(timestamp, referenceChain);
			referenceChain.previousState();
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIsvalue(final boolean fromSequence) {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = other.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		if (!Value_type.RELATIVEOBJECTIDENTIFIER_VALUE.equals(last.getValuetype())) {
			return false;
		}

		final RelativeObjectIdentifier_Value otherObjid = (RelativeObjectIdentifier_Value) last;
		if (objectIdComponents.size() != otherObjid.objectIdComponents.size()) {
			return false;
		}

		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			if (objectIdComponents.get(i) != otherObjid.objectIdComponents.get(i)) {
				return false;
			}
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		for (int i = 0, size = objectIdComponents.size(); i < size; i++) {
			final ObjectIdentifierComponent component = objectIdComponents.get(i);

			component.updateSyntax(reparser, false);
			reparser.updateLocation(component.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (objectIdComponents != null) {
			for (final ObjectIdentifierComponent c : objectIdComponents) {
				if (!c.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateSingleExpression(final JavaGenData aData) {
		//TODO register as module level charstring literal and return the literal's name
		aData.addBuiltinTypeImport( "TitanObjectid" );

		final ArrayList<String> components = new ArrayList<String>();
		getOidComponents(aData, components);
		final StringBuilder oiString = new StringBuilder();
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < components.size(); i++) {
			if (i > 0) {
				oiString.append(", ");
			}
			oiString.append(components.get(i));
		}
		result.append(MessageFormat.format("new TitanObjectid({0}, {1})", components.size(), oiString));

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		aData.addBuiltinTypeImport( "TitanObjectid" );

		source.append(MessageFormat.format("{0}.operator_assign({1});\n", name, generateSingleExpression(aData)));

		lastTimeGenerated = aData.getBuildTimstamp();

		return source;
	}
}
