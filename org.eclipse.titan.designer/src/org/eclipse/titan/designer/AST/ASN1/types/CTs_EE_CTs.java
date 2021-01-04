/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.types;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IType.ValueCheckingOptions;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Identifier.Identifier_type;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.ASN1Assignment;
import org.eclipse.titan.designer.AST.ASN1.ASN1Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.CompField;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Component Type list + Extension and Exception + Component Type List.
 *
 * @author Kristof Szabados
 * */
public final class CTs_EE_CTs extends ASTNode {
	public static final String DUPLICATECOMPONENTFIRST = "{0} `{1}'' was first defined here";
	public static final String DUPLICATECOMPONENTREPEATED = "Duplicate {0} identifier in {1}: `{2}'' was declared here again";

	private final ComponentTypeList componentTypeList1;
	private final ExtensionAndException extensionAndException;
	private final ComponentTypeList componentTypeList2;

	/** Pointer to the owner type. */
	private ASN1Type myType;

	private CompilationTimeStamp lastTimeChecked;

	//calculated field, holding the component fields in a single list, to easy processing.
	private ArrayList<CompField> components = new ArrayList<CompField>();
	//calculated field, holding the component fields in hashmap for fast search.
	private HashMap<String, CompField> componentsMap = new HashMap<String, CompField>();

	public CTs_EE_CTs(final ComponentTypeList componentTypeList1, final ExtensionAndException extensionAndException,
			final ComponentTypeList componentTypeList2) {
		this.componentTypeList1 = (null != componentTypeList1) ? componentTypeList1 : new ComponentTypeList();
		this.extensionAndException = extensionAndException;
		this.componentTypeList2 = (null != componentTypeList2) ? componentTypeList2 : new ComponentTypeList();

		this.componentTypeList1.setFullNameParent(this);
		if (null != extensionAndException) {
			extensionAndException.setFullNameParent(this);
		}
		this.componentTypeList2.setFullNameParent(this);
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		componentTypeList1.setMyScope(scope);
		if (null != extensionAndException) {
			extensionAndException.setMyScope(scope);
		}
		componentTypeList2.setMyScope(scope);
	}

	public void setMyType(final ASN1Type type) {
		myType = type;
	}

	public int getNofComps() {
		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		return components.size();
	}

	public int getNofRootComps() {
		return componentTypeList1.getNofComps() + componentTypeList2.getNofComps();
	}

	public CompField getCompByIndex(final int index) {
		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		return components.get(index);
	}

	public CompField getRootCompByIndex(final int index) {
		final int cts1Size = componentTypeList1.getNofComps();
		if (index < cts1Size) {
			return componentTypeList1.getCompByIndex(index);
		}

		return componentTypeList2.getCompByIndex(index - cts1Size);
	}

	public boolean hasCompWithName(final Identifier identifier) {
		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		return componentsMap.containsKey(identifier.getName());
	}

	public CompField getCompByName(final Identifier identifier) {
		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		return componentsMap.get(identifier.getName());
	}

	public int indexOf(final CompField field) {
		return components.indexOf(field);
	}

	public void trCompsof(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain, final boolean inEllipsis) {
		final boolean isSet = Type_type.TYPE_ASN1_SET.equals(myType.getTypetype());
		if (inEllipsis) {
			if (null != extensionAndException) {
				extensionAndException.trCompsof(timestamp, referenceChain, isSet);
			}
		} else {
			componentTypeList1.trCompsof(timestamp, referenceChain, isSet);
			componentTypeList2.trCompsof(timestamp, referenceChain, isSet);
		}
	}

	public boolean hasEllipsis() {
		return null != extensionAndException;
	}

	public void check(final CompilationTimeStamp timestamp) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		if (null != myScope && null != lastTimeChecked) {
			final Module module = myScope.getModuleScope();
			if (null != module) {
				if (module.getSkippedFromSemanticChecking()) {
					lastTimeChecked = timestamp;
					return;
				}
			}
		}

		if (null == myType) {
			return;
		}

		lastTimeChecked = timestamp;

		final ArrayList<CompField> newComponents = new ArrayList<CompField>();
		final HashMap<String, CompField> newComponentsMap = new HashMap<String, CompField>();

		String typeName;
		String componentName;
		switch (myType.getTypetype()) {
		case TYPE_ASN1_SEQUENCE:
			typeName = "SEQUENCE";
			componentName = "Component";
			break;
		case TYPE_ASN1_SET:
			typeName = "SET";
			componentName = "Component";
			break;
		case TYPE_ASN1_CHOICE:
			typeName = "CHOICE";
			componentName = "Alternative";
			break;
		default:
			// some INTERNAL ERROR
			typeName = "<unknown>";
			componentName = "component";
			break;
		}

		for (int i = 0; i < componentTypeList1.getNofComps(); i++) {
			final CompField componentField = componentTypeList1.getCompByIndex(i);
			checkComponentField(newComponents, newComponentsMap, componentField, typeName, componentName);
		}

		if (null != extensionAndException) {
			for (int i = 0; i < extensionAndException.getNofComps(); i++) {
				final CompField componentField = extensionAndException.getCompByIndex(i);
				checkComponentField(newComponents, newComponentsMap, componentField, typeName, componentName);
			}

			final ExceptionSpecification es = extensionAndException.getExceptionSpecification();
			if (es != null ){
				es.getType().check(timestamp);
			}
		}

		for (int i = 0; i < componentTypeList2.getNofComps(); i++) {
			final CompField componentField = componentTypeList2.getCompByIndex(i);
			checkComponentField(newComponents, newComponentsMap, componentField, typeName, componentName);
		}

		newComponents.trimToSize();
		components = newComponents;
		componentsMap = newComponentsMap;

		IType type;
		for (final CompField componentField : components) {
			type = componentField.getType();
			type.setGenName(myType.getGenNameOwn(), componentField.getIdentifier().getName());
			type.setParentType(myType);
			componentField.check(timestamp);

			if (componentField.hasDefault()) {
				final IValue defaultValue = componentField.getDefault();
				defaultValue.setMyGovernor(type);
				final IType lastType = type.getTypeRefdLast(timestamp);
				final IValue tempValue = lastType.checkThisValueRef(timestamp, defaultValue);
				lastType.checkThisValue(timestamp, tempValue, null, new ValueCheckingOptions(Expected_Value_type.EXPECTED_CONSTANT, false, false, true, false, false));

				defaultValue.setGenNameRecursive(type.getGenNameOwn() + "_defval_");
				defaultValue.setCodeSection(CodeSectionType.CS_PRE_INIT);
			}
		}
	}

	//This function checks only name uniqueness and fill in  structures "componentsMap" and "components"
	private void checkComponentField(final ArrayList<CompField> newComponents, final HashMap<String, CompField> newComponentMap, final CompField componentField, final String typeName, final String componentName) {
		final Identifier identifier = componentField.getIdentifier();
		final String name = identifier.getName();
		if (newComponentMap.containsKey(name)) {
			final Location tempLocation = newComponentMap.get(name).getIdentifier().getLocation();
			tempLocation.reportSingularSemanticError(MessageFormat.format(DUPLICATECOMPONENTFIRST, componentName,
					identifier.getDisplayName()));
			identifier.getLocation().reportSingularSemanticError(
					MessageFormat.format(DUPLICATECOMPONENTREPEATED, componentName, typeName, identifier.getDisplayName()));
		} else {
			newComponentMap.put(name, componentField);
			newComponents.add(componentField);
			if (!identifier.getHasValid(Identifier_type.ID_TTCN)) {
				identifier.getLocation().reportSingularSemanticWarning(
						MessageFormat.format(ASN1Assignment.UNREACHABLE, identifier.getDisplayName()));
			}
		}
	}

	/**
	 * Returns a list of the components whose identifier starts with the
	 * given prefix.
	 *
	 * @param prefix
	 *                the prefix used to select the component fields.
	 * @return the list of component fields which start with the provided
	 *         prefix.
	 * */
	public List<CompField> getComponentsWithPrefix(final String prefix) {
		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		final List<CompField> compFields = new ArrayList<CompField>();
		for (int i = 0; i < components.size(); i++) {
			if (components.get(i).getIdentifier().getName().startsWith(prefix)) {
				compFields.add(components.get(i));
			}
		}
		return compFields;
	}

	public Object[] getOutlineChildren() {
		final List<CompField> result = new ArrayList<CompField>();
		if (null != componentTypeList1) {
			final int size = componentTypeList1.getNofComps();
			for (int i = 0; i < size; i++) {
				result.add(componentTypeList1.getCompByIndex(i));
			}
		}
		if (null != componentTypeList2) {
			final int size = componentTypeList2.getNofComps();
			for (int i = 0; i < size; i++) {
				result.add(componentTypeList2.getCompByIndex(i));
			}
		}

		return result.toArray();
	}

	public void getEnclosingField(final int offset, final ReferenceFinder rf) {
		for (final CompField field : components) {
			if (field.getLocation().containsOffset(offset)
					// TODO: remove the below line when the above line starts to work (location problem)
					|| field.getIdentifier().getLocation().containsOffset(offset)) {
				rf.type = myType;
				rf.fieldId = field.getIdentifier();
				field.getType().getEnclosingField(offset, rf);
				return;
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (componentTypeList1 != null) {
			componentTypeList1.findReferences(referenceFinder, foundIdentifiers);
		}
		if (extensionAndException != null) {
			extensionAndException.findReferences(referenceFinder, foundIdentifiers);
		}
		if (componentTypeList2 != null) {
			componentTypeList2.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (componentTypeList1 != null && !componentTypeList1.accept(v)) {
			return false;
		}
		if (extensionAndException != null && !extensionAndException.accept(v)) {
			return false;
		}
		if (componentTypeList2 != null && !componentTypeList2.accept(v)) {
			return false;
		}
		return true;
	}
}
