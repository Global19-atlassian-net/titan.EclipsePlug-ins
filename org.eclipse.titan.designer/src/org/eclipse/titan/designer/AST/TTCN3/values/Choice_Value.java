/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values;

import java.text.MessageFormat;
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
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Choice_Type;
import org.eclipse.titan.designer.AST.ASN1.types.Open_Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Choice_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a choice value.
 * <p>
 * This kind of value is not parsed, but always converted.
 *
 * @author Kristof Szabados
 * */
public final class Choice_Value extends Value {
	private static final String ONEFIELDEXPECTED1 = "Union value must have one active field";
	private static final String ONEFIELDEXPECTED2 = "Only one field was expected in union value instead of {0}";
	private static final String NONEXISTENTFIELD = "Reference to non-existent union field `{0}'' in type `{1}''";
	private static final String INACTIVEFIELD = "Reference to inactive field `{0}'' in a value of union type `{1}''. The active field is `{2}''";

	private final Identifier name;
	private final IValue value;

	public Choice_Value(final Identifier name, final IValue value) {
		this.name = name;
		this.value = value;
	}

	public Choice_Value(final CompilationTimeStamp timestamp, final Sequence_Value value) {
		copyGeneralProperties(value);
		final int valueSize = value.getNofComponents();
		if (valueSize < 1) {
			this.name = null;
			this.value = null;
			value.getLocation().reportSemanticError(ONEFIELDEXPECTED1);
			setIsErroneous(true);
			lastTimeChecked = timestamp;
		} else if (valueSize > 1) {
			this.name = null;
			this.value = null;
			value.getLocation().reportSemanticError(MessageFormat.format(ONEFIELDEXPECTED2, valueSize));
			setIsErroneous(true);
			lastTimeChecked = timestamp;
		} else {
			final NamedValue namedValue = value.getSeqValueByIndex(0);
			this.name = namedValue.getName();
			this.value = namedValue.getValue();
		}
	}

	@Override
	/** {@inheritDoc} */
	public Value_type getValuetype() {
		return Value_type.CHOICE_VALUE;
	}

	public Identifier getName() {
		return name;
	}

	public IValue getValue() {
		return value;
	}

	public boolean hasComponentWithName(final Identifier name) {
		return this.name.getDisplayName().equals(name.getDisplayName());
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		if (isAsn()) {
			builder.append(name.getDisplayName()).append(" : ");
			builder.append(value.createStringRepresentation());
		} else {
			builder.append('{').append(name.getDisplayName()).append(" := ");
			builder.append(value.createStringRepresentation()).append('}');
		}

		return builder.toString();
	}

	public boolean fieldIsChosen(final Identifier name) {
		if (value == null) {
			return false;
		}

		return this.name.equals(name);
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_UNDEFINED;
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
		if (type.getIsErroneous(timestamp)) {
			return null;
		}

		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(ArraySubReference.INVALIDVALUESUBREFERENCE, type.getTypename()));
			return null;
		case fieldSubReference:
			final Identifier fieldId = ((FieldSubReference) subreference).getId();
			switch (type.getTypetype()) {
			case TYPE_TTCN3_CHOICE:
				if (!((TTCN3_Choice_Type) type).hasComponentWithName(fieldId.getName())) {
					subreference.getLocation().reportSemanticError(
							MessageFormat.format(NONEXISTENTFIELD, fieldId.getDisplayName(), type.getTypename()));
					return null;
				}
				break;
			case TYPE_ASN1_CHOICE:
				if (!((ASN1_Choice_Type) type).hasComponentWithName(fieldId)) {
					subreference.getLocation().reportSemanticError(
							MessageFormat.format(NONEXISTENTFIELD, fieldId.getDisplayName(), type.getTypename()));
					return null;
				}
				break;
			case TYPE_OPENTYPE:
				if (!((Open_Type) type).hasComponentWithName(fieldId)) {
					subreference.getLocation().reportSemanticError(
							MessageFormat.format(NONEXISTENTFIELD, fieldId.getDisplayName(), type.getTypename()));
					return null;
				}
				break;
			default:
				return null;
			}

			if (name.getDisplayName().equals(fieldId.getDisplayName())) {
				return value.getReferencedSubValue(timestamp, reference, actualSubReference + 1, refChain);
			}

			if (!reference.getUsedInIsbound()) {
				subreference.getLocation().reportSemanticError(
						MessageFormat.format(INACTIVEFIELD, fieldId.getDisplayName(), type.getTypename(), name.getDisplayName()));
			}

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
		if (value == null) {
			return true;
		}

		return value.isUnfoldable(timestamp, expectedValue, referenceChain);
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);

		if (value != null) {
			value.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (value == child) {
			builder.append('.').append(name.getDisplayName());
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIsvalue(final boolean fromSequence) {
		if (value == null) {
			return true;
		}

		return value.evaluateIsvalue(false);
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIsbound(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return true;
		}

		final IType type = myGovernor.getTypeRefdLast(timestamp);
		if (type.getIsErroneous(timestamp)) {
			return false;
		}

		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			return false;
		case fieldSubReference:
			final Identifier fieldId = ((FieldSubReference) subreference).getId();
			switch (type.getTypetype()) {
			case TYPE_TTCN3_CHOICE:
				if (!((TTCN3_Choice_Type) type).hasComponentWithName(fieldId.getName())) {
					return false;
				}
				break;
			case TYPE_ASN1_CHOICE:
				if (!((ASN1_Choice_Type) type).hasComponentWithName(fieldId)) {
					return false;
				}
				break;
			case TYPE_OPENTYPE:
				if (!((Open_Type) type).hasComponentWithName(fieldId)) {
					return false;
				}
				break;
			default:
				return false;
			}

			if (name.getDisplayName().equals(fieldId.getDisplayName())) {
				return value.evaluateIsbound(timestamp, reference, actualSubReference + 1);
			}

			return false;
		case parameterisedSubReference:
			return false;
		default:
			return false;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIspresent(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return true;
		}

		final IType type = myGovernor.getTypeRefdLast(timestamp);
		if (type.getIsErroneous(timestamp)) {
			return false;
		}

		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			return false;
		case fieldSubReference:
			final Identifier fieldId = ((FieldSubReference) subreference).getId();
			switch (type.getTypetype()) {
			case TYPE_TTCN3_CHOICE:
				if (!((TTCN3_Choice_Type) type).hasComponentWithName(fieldId.getName())) {
					return false;
				}
				break;
			case TYPE_ASN1_CHOICE:
				if (!((ASN1_Choice_Type) type).hasComponentWithName(fieldId)) {
					return false;
				}
				break;
			case TYPE_OPENTYPE:
				if (!((Open_Type) type).hasComponentWithName(fieldId)) {
					return false;
				}
				break;
			default:
				return false;
			}

			if (name.getDisplayName().equals(fieldId.getDisplayName())) {
				return value.evaluateIspresent(timestamp, reference, actualSubReference + 1);
			}

			return false;
		case parameterisedSubReference:
			return false;
		default:
			return false;
		}
	}


	@Override
	/** {@inheritDoc} */
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = other.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		if (!Value_type.CHOICE_VALUE.equals(last.getValuetype())) {
			return false;
		}

		final Choice_Value otherChoice = (Choice_Value) last;
		if (!name.equals(otherChoice.name)
				|| !value.checkEquality(timestamp, otherChoice.value)) {
			return false;
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		// all members are converted, so their processing is done on their original location
		if (isDamaged) {
			throw new ReParseException();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (value == null) {
			return;
		}

		value.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (name!=null && !name.accept(v)) {
			return false;
		}
		if (value!=null && !value.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNamePrefix(final String prefix) {
		super.setGenNamePrefix(prefix);
		value.setGenNamePrefix(prefix);
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNameRecursive(String parameterGenName) {
		super.setGenNameRecursive(parameterGenName);

		StringBuilder embeddedName = new StringBuilder(parameterGenName);
		embeddedName.append('.');
		if(Type_type.TYPE_ANYTYPE.equals(myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp()).getTypetype())) {
			embeddedName.append("AT_");
		}
		embeddedName.append(name.getName());
		embeddedName.append("()");
		value.setGenNameRecursive(embeddedName.toString());
	}

	@Override
	/** {@inheritDoc}
	 * generate_code_init_choice in the compiler
	 * */
	public StringBuilder generateCodeInit(final JavaGenData aData, StringBuilder source, String name) {
		String altName = this.name.getName();

		IType type = myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		String altPrefix = "";
		if (Type_type.TYPE_ANYTYPE.equals(type.getTypetype())) {
			altPrefix = "AT_";
		}

		//TODO handle the case when temporary reference is needed
		String embeddedName = MessageFormat.format("{0}.get{1}{2}()", name, altPrefix, FieldSubReference.getJavaGetterName(altName));
		return value.generateCodeInit(aData, source, embeddedName);
	}
}
