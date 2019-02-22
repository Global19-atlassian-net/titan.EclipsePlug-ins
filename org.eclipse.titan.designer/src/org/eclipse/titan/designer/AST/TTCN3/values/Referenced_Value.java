/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISetting;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IType.ValueCheckingOptions;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.ASN1.Value_Assignment;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Sequence_Type;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Set_Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.ActualParameterList;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Const;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Sequence_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Set_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * @author Kristof Szabados
 * */
public final class Referenced_Value extends Value {
	private static final String UNEXPECTEDCALL1 =
			"Reference to a value or template was expected instead of a call of {0}, which does not have a return type";
	private static final String UNEXPECTEDCALL2 =
			"Reference to a value was expected instead of a call of {0}, which does not have a return type";
	private static final String UNEXPECTEDASSIGNMENT1 = "Reference to a value or template was expected instead of {0}";
	private static final String UNEXPECTEDASSIGNMENT2 = "Reference to a value was expected instead of {0}";

	//private static final String CIRCULARVALUEREFERENCE = "circular value reference chain: `{0}''";
	private static final String INFORMATIONFROMOBJECTNOTVALUE = "InformationFromObjects construct `{0}'' does not refer to a value";
	private static final String VALUERETURNEXPECTED =
			"Reference to a value was expected instead of a call of {0}, which does not have a return type";

	private final Reference reference;

	private IValue referencedValue;

	public Referenced_Value(final Reference reference) {
		this.reference = reference;

		if (reference != null) {
			reference.setFullNameParent(this);
		}
	}

	protected Referenced_Value(final Undefined_LowerIdentifier_Value original) {
		copyGeneralProperties(original);
		final List<ISubReference> subReferences = new ArrayList<ISubReference>();
		subReferences.add(new FieldSubReference(original.getIdentifier()));
		reference = new Reference(null, subReferences);
		reference.setMyScope(myScope);
		reference.setLocation(original.getIdentifier().getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public Value_type getValuetype() {
		return Value_type.REFERENCED_VALUE;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		// do not evaluate the value now.
		if (referencedValue != null && referencedValue != this) {
			return referencedValue.createStringRepresentation();
		}

		return reference.getDisplayName();
	}

	@Override
	// Location is optimized not to store an object as it is not needed
	public Location getLocation() {
		return new Location(reference.getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		// Do nothing
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReferenceValue(final CompilationTimeStamp timestamp, final Assignment lhs) {
		IType governor = myGovernor;
		if (governor == null) {
			governor = getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
		}
		if (governor == null) {
			return false;
		}

		final boolean isStringElement = reference.refersToStringElement();

		return governor.checkThisValue(timestamp, this, lhs, new ValueCheckingOptions(Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false, true, false, false, isStringElement));
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		final IType temporalType = getExpressionGovernor(timestamp, expectedValue);
		if (temporalType == null) {
			return Type_type.TYPE_UNDEFINED;
		}

		if (temporalType.getIsErroneous(timestamp)) {
			setIsErroneous(true);
		}

		return temporalType.getTypeRefdLast(timestamp).getTypetypeTtcn3();
	}

	@Override
	/** {@inheritDoc} */
	public boolean isUnfoldable(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		final Assignment ass = reference.getRefdAssignment(timestamp, false);
		if (ass == null) {
			return true;
		}

		switch (ass.getAssignmentType()) {
		case A_OBJECT:
		case A_OS:
		case A_CONST:
		case A_EXT_CONST:
		case A_MODULEPAR:
		case A_VAR:
		case A_FUNCTION_RVAL:
		case A_EXT_FUNCTION_RVAL:
		case A_PAR_VAL:
		case A_PAR_VAL_IN:
		case A_PAR_VAL_OUT:
		case A_PAR_VAL_INOUT:
			break;
		default:
			return true;
		}

		final IValue last = getValueRefdLast(timestamp, expectedValue, referenceChain);
		if (last == this || last == null) {
			return true;
		}

		return last.isUnfoldable(timestamp, expectedValue, referenceChain);
	}

	@Override
	/** {@inheritDoc} */
	public IValue getReferencedSubValue(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference,
			final IReferenceChain refChain) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return this;
		}

		IValue result = getValueRefdLast(timestamp, refChain);
		if (result != null && result != this) {
			result = result.getReferencedSubValue(timestamp, reference, actualSubReference, refChain);
			if (result != null && result.getIsErroneous(timestamp)) {
				setIsErroneous(true);
			}
			return result;
		}

		return this;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (reference != null) {
			reference.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);

		if (reference != null) {
			reference.setCodeSection(codeSection);
		}
	}

	public Reference getReference() {
		return reference;
	}

	@Override
	/** {@inheritDoc} */
	public Value setValuetype(final CompilationTimeStamp timestamp, final Value_type newType) {
		if (Value_type.UNIVERSALCHARSTRING_VALUE.equals(newType)) {
			final IValue temp = getValueRefdLast(timestamp, null);

			if (temp != null && Value_type.CHARSTRING_VALUE.equals(temp.getValuetype())) {
				return new UniversalCharstring_Value((Charstring_Value) temp);
			}
		}

		return super.setValuetype(timestamp, newType);
	}

	@Override
	/** {@inheritDoc} */
	public IValue getValueRefdLast(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue,
			final IReferenceChain referenceChain) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return referencedValue;
		}

		final boolean newChain = null == referenceChain;
		IReferenceChain tempReferenceChain;
		if (newChain) {
			tempReferenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		} else {
			tempReferenceChain = referenceChain;
		}

		referencedValue = this;
		isErroneous = false;
		if (reference == null) {
			return referencedValue;
		}

		reference.setIsErroneous(false);

		final Assignment ass = reference.getRefdAssignment(timestamp, true);
		if (ass == null) {
			isErroneous = true;
			return referencedValue;
		}

		switch (ass.getAssignmentType()) {
		case A_OBJECT:
		case A_OS: {
			final ISetting setting = reference.getRefdSetting(timestamp);

			if (setting == null || setting.getIsErroneous(timestamp)) {
				isErroneous = true;
			} else if (!Setting_type.S_V.equals(setting.getSettingtype())) {
				reference.getLocation().reportSemanticError(MessageFormat.format(INFORMATIONFROMOBJECTNOTVALUE, reference));
				isErroneous = true;
			} else {
				tempReferenceChain.markState();

				if (tempReferenceChain.add(this)) {
					referencedValue = ((IValue) setting).getValueRefdLast(timestamp, expectedValue, referenceChain);
				} else {
					isErroneous = true;
				}

				tempReferenceChain.previousState();
			}
			break;
		}
		case A_CONST: {
			tempReferenceChain.markState();

			if (tempReferenceChain.add(this)) {
				if (ass instanceof Def_Const) {
					referencedValue = ((Def_Const) ass).getValue();
				} else if (ass instanceof Value_Assignment) {
					referencedValue = ((Value_Assignment) ass).getValue();
				} else {
					isErroneous = true;
				}

				if (referencedValue != null && !isErroneous) {
					referencedValue = referencedValue.getReferencedSubValue(timestamp, reference, 1, tempReferenceChain);
				} else {
					referencedValue = this;
					tempReferenceChain.previousState();
					return referencedValue;
				}

				if (referencedValue != null) {
					referencedValue = referencedValue.getValueRefdLast(timestamp, tempReferenceChain);
				} else if (reference.hasUnfoldableIndexSubReference(timestamp)) {
					referencedValue = this;
					tempReferenceChain.previousState();
					return referencedValue;
				} else if (reference.getUsedInIsbound()) {
					referencedValue = this;
					tempReferenceChain.previousState();
					return referencedValue;
				} else {
					isErroneous = true;
				}
			} else {
				isErroneous = true;
			}

			tempReferenceChain.previousState();
			break;
		}
		case A_EXT_CONST:
		case A_MODULEPAR:
		case A_VAR:
		case A_FUNCTION_RVAL:
		case A_FUNCTION_RTEMP:
		case A_EXT_FUNCTION_RTEMP:
		case A_EXT_FUNCTION_RVAL:
		case A_PAR_VAL:
		case A_PAR_VAL_IN:
		case A_PAR_VAL_OUT:
		case A_PAR_VAL_INOUT:
			referencedValue = this;
			break;
		case A_MODULEPAR_TEMPLATE:
		case A_PAR_TEMP_IN:
		case A_PAR_TEMP_INOUT:
		case A_PAR_TEMP_OUT:
		case A_TEMPLATE:
		case A_VAR_TEMPLATE:
			// the referred definition is not a constant
			//errors will be reported in Types.java
			referencedValue = this;
			if (!Expected_Value_type.EXPECTED_TEMPLATE.equals(expectedValue)) {
				getLocation().reportSemanticError(MessageFormat.format(UNEXPECTEDASSIGNMENT2, ass.getDescription()));
			}
			break;
		case A_FUNCTION:
		case A_EXT_FUNCTION:
			reference.getLocation().reportSemanticError(MessageFormat.format(VALUERETURNEXPECTED, ass.getDescription()));
			isErroneous = true;
			break;
		default:
			if (Expected_Value_type.EXPECTED_TEMPLATE.equals(expectedValue)) {
				getLocation().reportSemanticError(MessageFormat.format(UNEXPECTEDASSIGNMENT1, ass.getDescription()));
			} else {
				getLocation().reportSemanticError(MessageFormat.format(UNEXPECTEDASSIGNMENT2, ass.getDescription()));
			}
			isErroneous = true;
			break;
		}

		if (newChain) {
			tempReferenceChain.release();
		}

		lastTimeChecked = timestamp;

		if (referencedValue == null) {
			referencedValue = this;
			isErroneous = true;
		}

		return referencedValue;
	}

	@Override
	/** {@inheritDoc} */
	public IType getExpressionGovernor(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (myGovernor != null) {
			return myGovernor;
		}

		reference.setIsErroneous(false);
		final Assignment ass = reference.getRefdAssignment(timestamp, true);
		if (ass == null) {
			setIsErroneous(true);
			return null;
		}
		IType temporalType = null;

		switch (ass.getAssignmentType()) {
		case A_CONST:
		case A_EXT_CONST:
		case A_MODULEPAR:
		case A_MODULEPAR_TEMPLATE:
		case A_TEMPLATE:
		case A_TIMER:
		case A_VAR:
		case A_VAR_TEMPLATE:
		case A_FUNCTION_RVAL:
		case A_FUNCTION_RTEMP:
		case A_EXT_FUNCTION_RVAL:
		case A_EXT_FUNCTION_RTEMP:
		case A_PAR_VAL:
		case A_PAR_VAL_IN:
		case A_PAR_VAL_OUT:
		case A_PAR_VAL_INOUT:
		case A_PAR_TEMP_IN:
		case A_PAR_TEMP_OUT:
		case A_PAR_TEMP_INOUT:
			temporalType = ass.getType(timestamp);
			break;
		case A_FUNCTION:
		case A_EXT_FUNCTION:
			if (Expected_Value_type.EXPECTED_TEMPLATE.equals(expectedValue)) {
				getLocation().reportSemanticError(MessageFormat.format(UNEXPECTEDCALL1, ass.getDescription()));
			} else {
				getLocation().reportSemanticError(MessageFormat.format(UNEXPECTEDCALL2, ass.getDescription()));
			}
			setIsErroneous(true);
			return null;
		default:
			if (Expected_Value_type.EXPECTED_TEMPLATE.equals(expectedValue)) {
				getLocation().reportSemanticError(MessageFormat.format(UNEXPECTEDASSIGNMENT1, ass.getDescription()));
			} else {
				getLocation().reportSemanticError(MessageFormat.format(UNEXPECTEDASSIGNMENT2, ass.getDescription()));
			}
			setIsErroneous(true);
			return null;
		}

		if (temporalType == null) {
			setIsErroneous(true);
			return null;
		}

		temporalType = temporalType.getFieldType(timestamp, reference, 1, expectedValue, false);
		if (temporalType == null || reference.getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return null;
		}

		return temporalType;
	}

	@Override
	/** {@inheritDoc} */
	public void checkExpressionOmitComparison(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (getIsErroneous(timestamp)) {
			return;
		}

		final List<ISubReference> subreferences = new ArrayList<ISubReference>();
		subreferences.addAll(reference.getSubreferences());
		if (subreferences.size() <= 1) {
			return;
		}

		final ISubReference subreference = subreferences.remove(subreferences.size() - 1);
		final Identifier id = subreference.getId();
		if (id == null) {
			getLocation().reportSemanticError("Only a reference pointing to an optional record or set field can be compared with `omit'");
			setIsErroneous(true);
			return;
		}

		final Assignment assignment = reference.getRefdAssignment(timestamp, true);
		if (assignment == null) {
			setIsErroneous(true);
			return;
		}

		IType type = assignment.getType(timestamp);
		if (type == null) {
			setIsErroneous(true);
			return;
		}

		final Reference tempReference = new Reference(null, subreferences);
		tempReference.setFullNameParent(this);
		tempReference.setMyScope(myScope);
		type = type.getFieldType(timestamp, tempReference, 1, expectedValue, false);
		if (type == null) {
			setIsErroneous(true);
			return;
		}

		type = type.getTypeRefdLast(timestamp);
		if (type == null || type.getIsErroneous(timestamp)) {
			setIsErroneous(true);
			return;
		}

		switch (type.getTypetype()) {
		case TYPE_ASN1_SEQUENCE:
			if (!((ASN1_Sequence_Type) type).hasComponentWithName(id)) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Type `{0}'' does not have field named `{1}''", type.getTypename(), id.getDisplayName()));
				setIsErroneous(true);
			} else if (!((ASN1_Sequence_Type) type).getComponentByName(id).isOptional()) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Field `{0}'' is mandatory in type`{1}''. It cannot be compared with `omit''", id.getDisplayName(), type.getTypename()));
				setIsErroneous(true);
			}
			break;
		case TYPE_TTCN3_SEQUENCE:
			if (!((TTCN3_Sequence_Type) type).hasComponentWithName(id.getName())) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Type `{0}'' does not have field named `{1}''", type.getTypename(), id.getDisplayName()));
				setIsErroneous(true);
			} else if (!((TTCN3_Sequence_Type) type).getComponentByName(id.getName()).isOptional()) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Field `{0}'' is mandatory in type`{1}''. It cannot be compared with `omit''", id.getDisplayName(), type.getTypename()));
				setIsErroneous(true);
			}
			break;
		case TYPE_ASN1_SET:
			if (!((ASN1_Set_Type) type).hasComponentWithName(id)) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Type `{0}'' does not have field named `{1}''", type.getTypename(), id.getDisplayName()));
				setIsErroneous(true);
			} else if (!((ASN1_Set_Type) type).getComponentByName(id).isOptional()) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Field `{0}'' is mandatory in type`{1}''. It cannot be compared with `omit''", id.getDisplayName(), type.getTypename()));
				setIsErroneous(true);
			}
			break;
		case TYPE_TTCN3_SET:
			if (!((TTCN3_Set_Type) type).hasComponentWithName(id.getName())) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Type `{0}'' does not have field named `{1}''", type.getTypename(), id.getDisplayName()));
				setIsErroneous(true);
			} else if (!((TTCN3_Set_Type) type).getComponentByName(id.getName()).isOptional()) {
				getLocation().reportSemanticError(MessageFormat.format(
						"Field `{0}'' is mandatory in type`{1}''. It cannot be compared with `omit''", id.getDisplayName(), type.getTypename()));
				setIsErroneous(true);
			}
			break;
		default:
			getLocation().reportSemanticError("Only a reference pointing to an optional record or set field can be compared with `omit'");
			setIsErroneous(true);
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		if (lastTimeChecked == null) {
			getValueRefdLast(timestamp, null);
		}

		if (referencedValue == null) {
			return false;
		}

		if (referencedValue == this) {
			return true;
		}

		return referencedValue.checkEquality(timestamp, other);
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (reference != null) {
			reference.updateSyntax(reparser, false);
			reparser.updateLocation(reference.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIsvalue(final boolean fromSequence) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), referenceChain);
		referenceChain.release();

		if (last == this) {
			return false;
		}

		return last.evaluateIsvalue(false);
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIsbound(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), referenceChain);
		referenceChain.release();

		if (last == this) {
			return false;
		}

		return last.evaluateIsbound(timestamp, reference, actualSubReference);
	}

	@Override
	/** {@inheritDoc} */
	public boolean evaluateIspresent(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = getValueRefdLast(CompilationTimeStamp.getBaseTimestamp(), referenceChain);
		referenceChain.release();

		if (last == this) {
			return false;
		}

		return last.evaluateIspresent(timestamp, reference, actualSubReference);
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (reference == null) {
			return;
		}

		reference.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (reference != null && !reference.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		if (referencedValue != null && referencedValue != this && referencedValue.canGenerateSingleExpression() && referencedValue.getMyScope().getModuleScopeGen() == myScope.getModuleScopeGen()) {
			return true;
		}

		return reference.hasSingleExpression();
	}

	@Override
	/** {@inheritDoc} */
	public boolean returnsNative() {
		if (referencedValue != null && referencedValue != this) {
			return referencedValue.returnsNative();
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public boolean needsShortCircuit() {
		// TODO this should be more nuanced to improve generated code efficiency
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateSingleExpression(final JavaGenData aData) {
		if (referencedValue != null && referencedValue != this && referencedValue.canGenerateSingleExpression() && referencedValue.getMyScope().getModuleScopeGen() == myScope.getModuleScopeGen()) {
			return referencedValue.generateSingleExpression(aData);
		}

		final ExpressionStruct expression = new ExpressionStruct();
		reference.generateConstRef(aData, expression);

		return expression.expression;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		if (referencedValue == null || referencedValue == this) {
			final ExpressionStruct expression = new ExpressionStruct();
			expression.expression.append(name);
			expression.expression.append(".operator_assign(");
			reference.generateConstRef(aData, expression);
			expression.expression.append(")");
			expression.mergeExpression(source);

			lastTimeGenerated = aData.getBuildTimstamp();

			return source;
		}

		// the referred value is available at compile time
		// the code generation is based on the referred value
		if (referencedValue.canGenerateSingleExpression() &&
				myScope.getModuleScopeGen() == referencedValue.getMyScope().getModuleScopeGen()) {
			// simple substitution for in-line values within the same module
			source.append(MessageFormat.format("{0}.operator_assign({1});\n", name, referencedValue.generateSingleExpression(aData)));
		} else {
			// use a simple reference to reduce code size
			if (needsInitPrecede(aData, referencedValue)) {
				// the referred value must be initialized first
				if (!referencedValue.isTopLevel() && referencedValue.needsTemporaryReference()) {
					// temporary id should be introduced for the lhs
					final String tempId = aData.getTemporaryVariableName();
					source.append("{\n");
					source.append(MessageFormat.format("{0} {1} = new {0}({2});\n", referencedValue.getMyGovernor().getGenNameValue(aData, source), tempId, referencedValue.get_lhs_name()));
					referencedValue.generateCodeInit(aData, source, tempId);
					source.append("}\n");
				} else {
					referencedValue.generateCodeInit(aData, source, referencedValue.get_lhs_name());
				}
			}

			source.append(MessageFormat.format("{0}.operator_assign({1});\n", name, referencedValue.getGenNameOwn(aData)));
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		return source;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateCodeInitMandatory(final JavaGenData aData, final StringBuilder source, final String name) {
		if (referencedValue == null || referencedValue == this) {
			final ExpressionStruct expression = new ExpressionStruct();
			expression.expression.append(name);
			expression.expression.append(".operator_assign(");
			reference.generateConstRef(aData, expression);
			generateCodeExpressionOptionalFieldReference(aData, expression, reference);
			expression.expression.append(")");
			expression.mergeExpression(source);

			return source;
		}

		// the referred value is available at compile time
		// the code generation is based on the referred value
		if (referencedValue.canGenerateSingleExpression() &&
				myScope.getModuleScopeGen() == referencedValue.getMyScope().getModuleScopeGen()) {
			// simple substitution for in-line values within the same module
			source.append(MessageFormat.format("{0}.operator_assign({1});\n", name, referencedValue.generateSingleExpression(aData)));
		} else {
			// TODO might need initialization see needs_init_precede
			// TODO Value.cc:generate_code_init_refd

			source.append(MessageFormat.format("{0}.operator_assign({1});\n", name, referencedValue.getGenNameOwn(aData)));
		}

		return source;
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (subreferences != null && subreferences.size() > 0 && subreferences.get(0) instanceof ParameterisedSubReference) {
			final ActualParameterList actualParameterList = ((ParameterisedSubReference) subreferences.get(0)).getActualParameters();
			if (actualParameterList != null) {
				actualParameterList.reArrangeInitCode(aData, source, usageModule);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression, final boolean forceObject) {
		// TODO check and handle conversion needs
		reference.generateConstRef(aData, expression);
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpressionMandatory(final JavaGenData aData, final ExpressionStruct expression, final boolean forceObject) {
		generateCodeExpression(aData, expression, true);

		generateCodeExpressionOptionalFieldReference(aData, expression, reference);
	}
}
