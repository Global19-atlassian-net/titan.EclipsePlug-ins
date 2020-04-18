/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.Assignments;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TypeCompatibilityInfo;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.WithAttributesPath;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Const;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_ExternalConst;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Extfunction;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_ModulePar;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Var;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Expression_Value.Operation_type;
import org.eclipse.titan.designer.AST.TTCN3.values.Referenced_Value;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3CodeSkeletons;
import org.eclipse.titan.designer.graphics.ImageCache;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * component type (TTCN-3).
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class Component_Type extends Type {
	private static final String COMPONENT_GIF = "component.gif";
	private static final String COMPONENTVALUEEXPECTED = "Component value was expected";
	private static final String TEMPLATENOTALLOWED = "{0} cannot be used for type `{1}''";
	private static final String LENGTHRESTRICTIONNOTALLOWED = "Length restriction is not allowed for type `{0}''";
	private static final String INVALIDSUBREFERENCE = "Referencing fields of a component is not allowed";


	private static final String[] SIMPLE_COMPONENT_PROPOSALS = new String[] {"alive", "create;", "create alive;", "done", "kill;", "killed",
		"running", "stop;" };
	private static final String[] ANY_COMPONENT_PROPOSALS = new String[] {"running", "alive", "done", "killed" };
	private static final String[] ALL_COMPONENT_PROPOSALS = new String[] {"running", "alive", "done", "killed", "stop;", "kill;" };

	private final ComponentTypeBody componentBody;

	public Component_Type(final ComponentTypeBody component) {
		this.componentBody = component;

		componentBody.setFullNameParent(this);
		componentBody.setMyType(this);
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetype() {
		return Type_type.TYPE_COMPONENT;
	}

	/**
	 * @return the body of this component type.
	 * */
	public ComponentTypeBody getComponentBody() {
		return componentBody;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		componentBody.setMyScope(scope);
	}

	@Override
	/** {@inheritDoc} */
	public void setAttributeParentPath(final WithAttributesPath parent) {
		super.setAttributeParentPath(parent);
		componentBody.setAttributeParentPath(withAttributesPath);
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getProposalDescription(final StringBuilder builder) {
		return builder.append("component");
	}

	@Override
	/** {@inheritDoc} */
	public boolean isCompatible(final CompilationTimeStamp timestamp, final IType otherType, final TypeCompatibilityInfo info,
			final TypeCompatibilityInfo.Chain leftChain, final TypeCompatibilityInfo.Chain rightChain) {
		check(timestamp);
		otherType.check(timestamp);
		final IType temp = otherType.getTypeRefdLast(timestamp);

		if (getIsErroneous(timestamp) || temp.getIsErroneous(timestamp) || this == temp) {
			return true;
		}

		return Type_type.TYPE_COMPONENT.equals(temp.getTypetype()) && componentBody.isCompatible(timestamp, ((Component_Type) temp).componentBody);
	}

	@Override
	/** {@inheritDoc} */
	public boolean isCompatibleByPort(final CompilationTimeStamp timestamp, final IType otherType) {
		check(timestamp);
		otherType.check(timestamp);

		final IType t2 = otherType.getTypeRefdLast(timestamp);
		if (t2.getTypetype() != Type_type.TYPE_COMPONENT) {
			return false;
		}

		if (getIsErroneous(timestamp) || t2.getIsErroneous(timestamp)) {
			return false;
		}

		final ComponentTypeBody b2 = ((Component_Type)t2).getComponentBody();

		// Does b2 contains every port with the same type and name as this?
		final Assignments b1Assignments = componentBody.getAssignmentsScope();
		for (int i = 0; i < b1Assignments.getNofAssignments(); i++) {
			final Assignment assignment = b1Assignments.getAssignmentByIndex(i);
			if (assignment.getAssignmentType() == Assignment_type.A_PORT) {
				final IType portType = assignment.getType(timestamp).getTypeRefdLast(timestamp);
				final Identifier identifier = assignment.getIdentifier();
				boolean found = false;
				final Assignments b2Assignments = b2.getAssignmentsScope();
				for (int j = 0; j < b2Assignments.getNofAssignments(); j++) {
					final Assignment assignment2 = b2Assignments.getAssignmentByIndex(j);
					final Identifier identifier2 = assignment2.getIdentifier();
					if (identifier.equals(identifier2) && assignment2.getAssignmentType() == Assignment_type.A_PORT) {
						final IType portType2 = assignment2.getType(timestamp).getTypeRefdLast(timestamp);
						if (portType.equals(portType2)) {
							found = true;
							break;
						} else {
							return false;
						}
					}
				}
				if (!found) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isIdentical(final CompilationTimeStamp timestamp, final IType type) {
		check(timestamp);
		type.check(timestamp);
		final IType temp = type.getTypeRefdLast(timestamp);
		if (getIsErroneous(timestamp) || temp.getIsErroneous(timestamp)) {
			return true;
		}

		return this == temp;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getTypetypeTtcn3() {
		if (isErroneous) {
			return Type_type.TYPE_UNDEFINED;
		}

		return getTypetype();
	}

	@Override
	/** {@inheritDoc} */
	public String getTypename() {
		return getFullName();
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return COMPONENT_GIF;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		initAttributes(timestamp);

		componentBody.check(timestamp);

		lastTimeChecked = timestamp;

		if (myScope != null) {
			checkEncode(timestamp);
			checkVariants(timestamp);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisValue(final CompilationTimeStamp timestamp, final IValue value, final Assignment lhs, final ValueCheckingOptions valueCheckingOptions) {
		final boolean selfReference = super.checkThisValue(timestamp, value, lhs, valueCheckingOptions);

		final IValue last = value.getValueRefdLast(timestamp, valueCheckingOptions.expected_value, null);
		if (last == null || last.getIsErroneous(timestamp)) {
			return selfReference;
		}

		// already handled ones
		switch (value.getValuetype()) {
		case OMIT_VALUE:
		case REFERENCED_VALUE:
			return selfReference;
		case UNDEFINED_LOWERIDENTIFIER_VALUE:
			if (Value_type.REFERENCED_VALUE.equals(last.getValuetype())) {
				return selfReference;
			}
			break;
		default:
			break;
		}

		switch (last.getValuetype()) {
		case TTCN3_NULL_VALUE:
			value.setValuetype(timestamp, Value_type.EXPRESSION_VALUE);
			break;
		case EXPRESSION_VALUE:
		case MACRO_VALUE:
			// already checked
			break;
		default:
			value.getLocation().reportSemanticError(COMPONENTVALUEEXPECTED);
			value.setIsErroneous(true);
		}

		value.setLastTimeChecked(timestamp);

		return selfReference;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkThisTemplate(final CompilationTimeStamp timestamp, final ITTCN3Template template,
			final boolean isModified, final boolean implicitOmit, final Assignment lhs) {
		registerUsage(template);
		template.setMyGovernor(this);

		template.getLocation().reportSemanticError(MessageFormat.format(TEMPLATENOTALLOWED, template.getTemplateTypeName(), getTypename()));

		if (template.getLengthRestriction() != null) {
			template.getLocation().reportSemanticError(MessageFormat.format(LENGTHRESTRICTIONNOTALLOWED, getTypename()));
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public IType getFieldType(final CompilationTimeStamp timestamp, final Reference reference, final int actualSubReference,
			final Expected_Value_type expectedIndex, final IReferenceChain refChain, final boolean interruptIfOptional) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (subreferences.size() <= actualSubReference) {
			return this;
		}

		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(ArraySubReference.INVALIDSUBREFERENCE, getTypename()));
			return null;
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		case parameterisedSubReference:
			subreference.getLocation().reportSemanticError(
					MessageFormat.format(FieldSubReference.INVALIDSUBREFERENCE, ((ParameterisedSubReference) subreference).getId().getDisplayName(),
							getTypename()));
			return null;
		default:
			subreference.getLocation().reportSemanticError(ISubReference.INVALIDSUBREFERENCE);
			return null;
		}
	}

	/**
	 * Searches and adds a completion proposal to the provided collector if a
	 * valid one is found.
	 * <p>
	 * handles the following proposals:
	 * <ul>
	 * <li>create, create alive, create(name), create(name) alive
	 * <li>start(function_instance)
	 * <li>stop
	 * <li>kill
	 * <li>alive
	 * <li>running
	 * <li>done
	 * <li>killed
	 * </ul>
	 *
	 * @param propCollector the proposal collector to add the proposal to, and
	 *            used to get more information
	 * @param i index, used to identify which element of the reference (used by
	 *            the proposal collector) should be checked for completions.
	 * */
	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int i) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= i || Subreference_type.arraySubReference.equals(subrefs.get(i).getReferenceType())) {
			return;
		}

		componentBody.addProposal(propCollector, i);

		if (subrefs.size() == i + 1) {
			for (final String proposal : SIMPLE_COMPONENT_PROPOSALS) {
				propCollector.addProposal(proposal, proposal, ImageCache.getImage(getOutlineIcon()), "");
			}
			propCollector.addTemplateProposal("create", new Template("create( name )", "", propCollector.getContextIdentifier(),
					"create( ${name} );", false), TTCN3CodeSkeletons.SKELETON_IMAGE);
			propCollector.addTemplateProposal("create", new Template("create( name ) alive", "", propCollector.getContextIdentifier(),
					"create( ${name} ) alive;", false), TTCN3CodeSkeletons.SKELETON_IMAGE);
			propCollector.addTemplateProposal("create", new Template("create( name, location )", "", propCollector.getContextIdentifier(),
					"create( ${name}, ${location} );", false), TTCN3CodeSkeletons.SKELETON_IMAGE);
			propCollector.addTemplateProposal("create", new Template("create( name, location ) alive", "", propCollector.getContextIdentifier(),
					"create( ${name}, ${location} ) alive;", false), TTCN3CodeSkeletons.SKELETON_IMAGE);
			propCollector.addTemplateProposal("start", new Template("start( function name )", "", propCollector.getContextIdentifier(),
					"start( ${functionName} );", false), TTCN3CodeSkeletons.SKELETON_IMAGE);
		}
	}

	public static void addAnyorAllProposal(final ProposalCollector propCollector, final int i) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (i != 0 || subrefs.isEmpty() || Subreference_type.arraySubReference.equals(subrefs.get(0).getReferenceType())) {
			return;
		}

		final String fakeModuleName = propCollector.getReference().getModuleIdentifier().getDisplayName();

		if ("any component".equals(fakeModuleName)) {
			for (final String proposal : ANY_COMPONENT_PROPOSALS) {
				propCollector.addProposal(proposal, proposal, ImageCache.getImage(COMPONENT_GIF), "");
			}
		} else if ("all component".equals(fakeModuleName)) {
			for (final String proposal : ALL_COMPONENT_PROPOSALS) {
				propCollector.addProposal(proposal, proposal, ImageCache.getImage(COMPONENT_GIF), "");
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() <= i || Subreference_type.arraySubReference.equals(subrefs.get(i).getReferenceType())) {
			return;
		}

		componentBody.addDeclaration(declarationCollector, i);
	}

	@Override
	/** {@inheritDoc} */
	public Object[] getOutlineChildren() {
		return componentBody.getDefinitions().toArray();
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			lastTimeChecked = null;
			boolean handled = false;

			if (componentBody != null) {
				if (reparser.envelopsDamage(componentBody.getLocation())) {
					componentBody.updateSyntax(reparser, true);
					reparser.updateLocation(componentBody.getLocation());
					handled = true;
				}
			}

			if (subType != null) {
				subType.updateSyntax(reparser, false);
				handled = true;
			}

			if (handled) {
				return;
			}

			throw new ReParseException();
		}

		componentBody.updateSyntax(reparser, false);
		reparser.updateLocation(componentBody.getLocation());

		if (subType != null) {
			subType.updateSyntax(reparser, false);
		}

		if (withAttributesPath != null) {
			withAttributesPath.updateSyntax(reparser, false);
			reparser.updateLocation(withAttributesPath.getLocation());
		}
	}

	/**
	 * Checks if the provided value is a reference to a component or not.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle.
	 * @param value the value to be checked
	 * @param expected_value the value kind expected from the actual parameter.
	 * @param anyFrom is the reference used from any from context
	 * */
	public static IType checkExpressionOperandComponentRefernce(final CompilationTimeStamp timestamp,
			final IValue value, final String operationName, final boolean anyFrom) {
		IType returnValue;
		switch (value.getValuetype()) {
		case EXPRESSION_VALUE: {
			final Expression_Value expression = (Expression_Value) value;
			if (Operation_type.APPLY_OPERATION.equals(expression.getOperationType())) {
				final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
				final IValue last = value.getValueRefdLast(timestamp, chain);
				chain.release();
				if (last == null || last.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null;
				}

				returnValue = last.getExpressionGovernor(timestamp, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}

				IType type = returnValue.getTypeRefdLast(timestamp);
				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}
				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}
				return returnValue;
			}

			return null;
		}
		case REFERENCED_VALUE: {
			final Reference reference = ((Referenced_Value) value).getReference();
			final Assignment assignment = reference.getRefdAssignment(timestamp, true);
			if (assignment == null) {
				value.setIsErroneous(true);
				return null;
			}


			switch (assignment.getAssignmentType()) {
			case A_CONST: {
				returnValue = ((Def_Const) assignment).getType(timestamp).getFieldType(
						timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}

				IType type = returnValue.getTypeRefdLast(timestamp);
				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}

				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						reference.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}

				IValue tempValue = ((Def_Const) assignment).getValue();
				if (tempValue == null) {
					return null;
				}

				IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
				tempValue = tempValue.getReferencedSubValue(timestamp, reference, 1, chain);
				chain.release();
				if (tempValue == null) {
					return null;
				}
				chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
				tempValue = tempValue.getValueRefdLast(timestamp, chain);
				chain.release();
				if (Value_type.TTCN3_NULL_VALUE.equals(tempValue.getValuetype())) {
					reference.getLocation().reportSemanticError(MessageFormat.format(
							"The first operand of operation `{0}'' refers to the `null'' component reference", operationName));
					value.setIsErroneous(true);
					return null;
				}
				if (!Value_type.EXPRESSION_VALUE.equals(tempValue.getValuetype())) {
					return null;
				}
				switch (((Expression_Value) tempValue).getOperationType()) {
				case MTC_COMPONENT_OPERATION:
					reference.getLocation().reportSemanticError(MessageFormat.format(
							"The first operand of operation `{0}'' refers to the component reference of the `mtc''", operationName));
					value.setIsErroneous(true);
					return null;
				case COMPONENT_NULL_OPERATION:
					reference.getLocation().reportSemanticError(MessageFormat.format(
							"The first operand of operation `{0}'' refers to the `null'' component reference", operationName));
					value.setIsErroneous(true);
					return null;
				case SYSTEM_COMPONENT_OPERATION:
					reference.getLocation().reportSemanticError(MessageFormat.format(
							"The first operand of operation `{0}'' refers to the component reference of the `system''", operationName));
					value.setIsErroneous(true);
					return null;
				default:
					break;
				}
				break; }
			case A_EXT_CONST: {
				returnValue = ((Def_ExternalConst) assignment).getType(timestamp).getFieldType(
						timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}

				IType type = returnValue.getTypeRefdLast(timestamp);
				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}

				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						reference.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}
				break; }
			case A_MODULEPAR: {
				returnValue = ((Def_ModulePar) assignment).getType(timestamp).getFieldType(
						timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}

				IType type = returnValue.getTypeRefdLast(timestamp);
				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}

				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						reference.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}
				break; }
			case A_VAR: {
				returnValue = ((Def_Var) assignment).getType(timestamp).getFieldType(
						timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}

				IType type = returnValue.getTypeRefdLast(timestamp);
				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}

				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						reference.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}
				break; }
			case A_FUNCTION_RVAL: {
				returnValue = ((Def_Function) assignment).getType(timestamp).getFieldType(
						timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}

				IType type = returnValue.getTypeRefdLast(timestamp);
				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}

				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						reference.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}
				break; }
			case A_EXT_FUNCTION_RVAL: {
				returnValue = ((Def_Extfunction) assignment).getType(timestamp).getFieldType(
						timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}
				IType type = returnValue.getTypeRefdLast(timestamp);

				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}

				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						reference.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}
				break; }
			case A_PAR_VAL:
			case A_PAR_VAL_IN:
			case A_PAR_VAL_OUT:
			case A_PAR_VAL_INOUT: {
				returnValue = ((FormalParameter) assignment).getType(timestamp).getFieldType(
						timestamp, reference, 1, Expected_Value_type.EXPECTED_DYNAMIC_VALUE, false);
				if (returnValue == null) {
					value.setIsErroneous(true);
					return null;
				}
				IType type = returnValue.getTypeRefdLast(timestamp);

				if( type.getIsErroneous(timestamp)) {
					value.setIsErroneous(true);
					return null; //don't let spread an earlier mistake
				}

				if (anyFrom) {
					if (!Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}

					while (Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
						type = ((Array_Type)type).getElementType().getTypeRefdLast(timestamp);
					}
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						value.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component array reference was expected instead of array of type `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				} else {
					if (!Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
						reference.getLocation().reportSemanticError(MessageFormat.format(
								"The first operand of operation `{0}'': Type mismatch: component reference was expected instead of `{1}''",
								operationName, type.getTypename()));
						value.setIsErroneous(true);
						return null;
					}
				}
				break; }
			default:
				reference.getLocation().reportSemanticError(MessageFormat.format(
						"The first operand of operation `{0}'' should be a component reference instead of `{1}''",
						operationName, assignment.getDescription()));
				value.setIsErroneous(true);
				return null;
			}
			return returnValue;
		}
		default:
			// the error was already reported if possible.
			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkMapParameter(final CompilationTimeStamp timestamp, final IReferenceChain refChain, final Location errorLocation) {
		if (refChain.contains(this)) {
			return;
		}

		refChain.add(this);
		errorLocation.reportSemanticError("The `map'/`unmap' parameters of a port type cannot be or contain a field/element of component type");
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (componentBody != null) {
			componentBody.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (componentBody!=null && !componentBody.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean generatesOwnClass(JavaGenData aData, StringBuilder source) {
		return needsAlias();
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return;
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		if(needsAlias()) {
			final String ownName = getGenNameOwn();
			source.append(MessageFormat.format("\tpublic static class {0} extends {1} '{'\n", ownName, getGenNameValue(aData, source)));

			final StringBuilder descriptor = new StringBuilder();
			generateCodeTypedescriptor(aData, source, descriptor);
			generateCodeDefaultCoding(aData, source, descriptor);
			generateCodeForCodingHandlers(aData, source, descriptor);
			source.append(descriptor);

			source.append("\t}\n");

			source.append(MessageFormat.format("\tpublic static class {0}_template extends {1} '{' '}'\n", ownName, getGenNameTemplate(aData, source)));
		} else {
			generateCodeTypedescriptor(aData, source, null);
			generateCodeDefaultCoding(aData, source, null);
			generateCodeForCodingHandlers(aData, source, null);
		}

		if (hasDoneAttribute()) {
			generateCodeDone(aData, source);
		}
		if (subType != null) {
			subType.generateCode(aData, source);
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameValue( final JavaGenData aData, final StringBuilder source ) {
		aData.addBuiltinTypeImport( "TitanComponent" );
		return "TitanComponent";
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTemplate(final JavaGenData aData, final StringBuilder source) {
		aData.addBuiltinTypeImport( "TitanComponent_template" );
		return "TitanComponent_template";
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeTypedescriptor(final JavaGenData aData, final StringBuilder source, StringBuilder localTarget) {
		aData.addBuiltinTypeImport("Base_Type.TTCN_Typedescriptor");

		final String genname = getGenNameOwn();
		final String descriptorName = MessageFormat.format("{0}_descr_", genname);
		if (localTarget == null && aData.hasGlobalVariable(descriptorName)) {
			return;
		}

		final String globalVariable = MessageFormat.format("\tpublic static final TTCN_Typedescriptor {0}_descr_ = {1}_descr_;\n", genname, getGenNameValue(aData, source));
		if (localTarget == null) {
			aData.addGlobalVariable(descriptorName, globalVariable.toString());
		} else {
			localTarget.append(globalVariable);
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameTypeDescriptor(final JavaGenData aData, final StringBuilder source) {
		if (rawAttribute != null || jsonAttribute != null ||
				hasVariantAttributes(CompilationTimeStamp.getBaseTimestamp())
				|| hasEncodeAttribute("JSON")) {
			if (needsAlias()) {
				String baseName = getGenNameOwn(aData);
				return baseName + "." + getGenNameOwn();
			} else if (getParentType() != null) {
				final IType parentType = getParentType();
				if (parentType.generatesOwnClass(aData, source)) {
					return parentType.getGenNameOwn(aData) + "." + getGenNameOwn();
				}

				return getGenNameOwn(aData);
			}

			return getGenNameOwn(aData);
		}

		if (needsAlias()) {
			String baseName = getGenNameOwn(aData);
			return baseName + "." + getGenNameOwn();
		}

		aData.addBuiltinTypeImport( "Base_Type" );
		return "Base_Type.TitanComponent";
	}
}
