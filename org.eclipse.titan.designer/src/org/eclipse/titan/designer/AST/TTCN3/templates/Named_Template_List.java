/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Choice_Type;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Sequence_Type;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Set_Type;
import org.eclipse.titan.designer.AST.ASN1.types.Open_Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.types.Anytype_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.CompField;
import org.eclipse.titan.designer.AST.TTCN3.types.Signature_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Choice_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Sequence_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Set_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.NamedValue;
import org.eclipse.titan.designer.AST.TTCN3.values.NamedValues;
import org.eclipse.titan.designer.AST.TTCN3.values.Sequence_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represent a list of named templates.
 *
 * @author Kristof Szabados
 * */
public final class Named_Template_List extends TTCN3Template {
	private static final String ALLARENOTUSED = "All elements of value list notation for type `{0}'' are not used symbols (`-'')";
	private static final String TOOMANYELEMENTS = "Too many elements in value list notation for type `{0}'': {1} was expected instead of {2}";

	private final NamedTemplates namedTemplates;

	// cache storing the value form of this if already created, or null
	private Sequence_Value asValue = null;

	public Named_Template_List(final NamedTemplates namedTemplates) {
		this.namedTemplates = namedTemplates;

		namedTemplates.setFullNameParent(this);
	}

	/**
	 * function used to convert a template written without naming the fields into a template with all field names provided.
	 *
	 * @param timestamp the timestamp of the actual build cycle
	 * @param other the template to be converted
	 * */
	public static Named_Template_List convert(final CompilationTimeStamp timestamp, final Template_List other) {
		final IType lastType = other.getMyGovernor().getTypeRefdLast(timestamp);
		final int nofTemplates = other.getNofTemplates();
		int nofTypeComponents = 0;
		switch (lastType.getTypetype()) {
		case TYPE_TTCN3_SEQUENCE:
			nofTypeComponents = ((TTCN3_Sequence_Type) lastType).getNofComponents();
			break;
		case TYPE_ASN1_SEQUENCE:
			nofTypeComponents = ((ASN1_Sequence_Type) lastType).getNofComponents();
			break;
		case TYPE_SIGNATURE:
			nofTypeComponents = ((Signature_Type) lastType).getNofParameters();
			break;
		case TYPE_TTCN3_SET:
			nofTypeComponents = ((TTCN3_Set_Type) lastType).getNofComponents();
			break;
		case TYPE_ASN1_SET:
			nofTypeComponents = ((ASN1_Set_Type) lastType).getNofComponents();
			break;
		default:
		}

		if (nofTemplates > nofTypeComponents) {
			other.getLocation().reportSemanticError(MessageFormat.format(TOOMANYELEMENTS, lastType.getTypename(), nofTypeComponents, nofTemplates));
			other.setIsErroneous(true);
		}

		int upperLimit;
		boolean allNotUsed;
		if (nofTemplates <= nofTypeComponents) {
			upperLimit = nofTemplates;
			allNotUsed = true;
		} else {
			upperLimit = nofTypeComponents;
			allNotUsed = false;
		}

		final NamedTemplates namedTemplates = new NamedTemplates();
		for (int i = 0; i < upperLimit; i++) {
			final TTCN3Template template = other.getTemplateByIndex(i);
			if (!Template_type.TEMPLATE_NOTUSED.equals(template.getTemplatetype())) {
				allNotUsed = false;
				Identifier identifier = null;
				switch (lastType.getTypetype()) {
				case TYPE_TTCN3_SEQUENCE:
					identifier = ((TTCN3_Sequence_Type) lastType).getComponentIdentifierByIndex(i);
					break;
				case TYPE_ASN1_SEQUENCE:
					identifier = ((ASN1_Sequence_Type) lastType).getComponentIdentifierByIndex(i);
					break;
				case TYPE_SIGNATURE:
					identifier = ((Signature_Type) lastType).getParameterIdentifierByIndex(i);
					break;
				case TYPE_TTCN3_SET:
					identifier = ((TTCN3_Set_Type) lastType).getComponentIdentifierByIndex(i);
					break;
				case TYPE_ASN1_SET:
					identifier = ((ASN1_Set_Type) lastType).getComponentIdentifierByIndex(i);
					break;
				default:
					// can not reach here because of a
					// previous check
					break;
				}

				if (identifier != null) {
					final NamedTemplate namedTemplate = new NamedTemplate(identifier.newInstance(), template);
					namedTemplate.setLocation(template.getLocation());
					namedTemplates.addTemplate(namedTemplate);
				}
			}
		}

		namedTemplates.setMyScope(other.getMyScope());
		namedTemplates.setFullNameParent(other);

		if (allNotUsed && nofTemplates > 0 && !Type_type.TYPE_SIGNATURE.equals(lastType.getTypetype())) {
			other.getLocation().reportSemanticWarning(MessageFormat.format(ALLARENOTUSED, lastType.getTypename()));
			other.setIsErroneous(true);
		}

		final Named_Template_List target = new Named_Template_List(namedTemplates);
		target.copyGeneralProperties(other);

		return target;
	}

	public void addNamedValue(final NamedTemplate template) {
		if (template != null) {
			namedTemplates.addTemplate(template);
			template.setMyScope(myScope);
		}
	}

	/**
	 * Checks if there is a named template in the list, with a given name.
	 *
	 * @param name
	 *                the name to search for.
	 * @return true if the list has a template with the provided name, false
	 *         otherwise.
	 */
	public boolean hasNamedTemplate(final Identifier name) {
		if (name == null || namedTemplates == null) {
			return false;
		}

		return namedTemplates.hasNamedTemplateWithName(name);
	}

	/**
	 * Returns the named template referred by the identifier.
	 *
	 * @param name
	 *                the name to identify the namedTemplate
	 *
	 * @return the named template with the provided name, or null if there
	 *         is none with that name.
	 * */
	public NamedTemplate getNamedTemplate(final Identifier name) {
		if (name == null || namedTemplates == null) {
			return null;
		}

		return namedTemplates.getNamedTemplateByName(name);
	}

	@Override
	/** {@inheritDoc} */
	public Template_type getTemplatetype() {
		return Template_type.NAMED_TEMPLATE_LIST;
	}

	@Override
	/** {@inheritDoc} */
	public String getTemplateTypeName() {
		if (isErroneous) {
			return "erroneous assignment notation";
		}

		return "assignment notation";
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		for (int i = 0, size = namedTemplates.getNofTemplates(); i < size; i++) {
			if (i > 0) {
				builder.append(", ");
			}

			final NamedTemplate namedTemplate = namedTemplates.getTemplateByIndex(i);
			builder.append(namedTemplate.getName().getDisplayName());
			builder.append(" := ");
			builder.append(namedTemplate.getTemplate().createStringRepresentation());
		}
		builder.append(" }");

		if (lengthRestriction != null) {
			builder.append(lengthRestriction.createStringRepresentation());
		}
		if (isIfpresent) {
			builder.append("ifpresent");
		}

		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (namedTemplates != null) {
			namedTemplates.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);
		for (int i = 0, size = namedTemplates.getNofTemplates(); i < size; i++) {
			namedTemplates.getTemplateByIndex(i).getTemplate().setCodeSection(codeSection);
		}
		if (lengthRestriction != null) {
			lengthRestriction.setCodeSection(codeSection);
		}
	}

	/**
	 * Remove all named templates that were not parsed, but generated during
	 * previous semantic checks.
	 * */
	public void removeGeneratedValues() {
		if (namedTemplates != null) {
			namedTemplates.removeGeneratedValues();
		}
	}

	/** @return the number of templates in the list */
	public int getNofTemplates() {
		return namedTemplates.getNofTemplates();
	}

	/**
	 * @param index
	 *                the index of the element to return.
	 *
	 * @return the template on the indexed position.
	 * */
	public NamedTemplate getTemplateByIndex(final int index) {
		return namedTemplates.getTemplateByIndex(index);
	}

	@Override
	/** {@inheritDoc} */
	public boolean isValue(final CompilationTimeStamp timestamp) {
		if (lengthRestriction != null || isIfpresent || getIsErroneous(timestamp)) {
			return false;
		}

		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			if (!namedTemplates.getTemplateByIndex(i).getTemplate().isValue(timestamp)) {
				return false;
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public IValue getValue() {
		if (asValue != null) {
			return asValue;
		}

		final NamedValues values = new NamedValues();
		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			final NamedTemplate namedTemplate = namedTemplates.getTemplateByIndex(i);
			final NamedValue namedValue = new NamedValue(namedTemplate.getName(), namedTemplate.getTemplate().getValue());
			namedValue.setLocation(namedTemplate.getLocation());
			values.addNamedValue(namedValue);
		}
		asValue = new Sequence_Value(values);
		asValue.setLocation(getLocation());
		asValue.setMyScope(getMyScope());
		asValue.setFullNameParent(getNameParent());
		asValue.setMyGovernor(getMyGovernor());

		return asValue;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReferenceTemplate(final CompilationTimeStamp timestamp, final Assignment lhs) {
		for (int i = 0, size = namedTemplates.getNofTemplates(); i < size; i++) {
			if(namedTemplates.getTemplateByIndex(i).getTemplate().checkExpressionSelfReferenceTemplate(timestamp, lhs)) {
				return true;
			}
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void checkSpecificValue(final CompilationTimeStamp timestamp, final boolean allowOmit) {
		ITTCN3Template temp;
		for (int i = 0, size = namedTemplates.getNofTemplates(); i < size; i++) {
			temp = namedTemplates.getTemplateByIndex(i).getTemplate();
			if (temp != null) {
				temp.checkSpecificValue(timestamp, true);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this)) {
			for (int i = 0; i < namedTemplates.getNofTemplates(); i++) {
				final NamedTemplate template = namedTemplates.getTemplateByIndex(i);
				if (template != null) {
					referenceChain.markState();
					template.getTemplate().checkRecursions(timestamp, referenceChain);
					referenceChain.previousState();
				}
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean chkRestrictionNamedListBaseTemplate(final CompilationTimeStamp timestamp, final String definitionName,
			final boolean omitAllowed,
			final Set<String> checkedNames, final int neededCheckedCnt, final Location usageLocation) {
		boolean needsRuntimeCheck = false;
		if (checkedNames.size() >= neededCheckedCnt) {
			return needsRuntimeCheck;
		}

		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			final ITTCN3Template tmpl = namedTemplates.getTemplateByIndex(i).getTemplate();
			final String name = namedTemplates.getTemplateByIndex(i).getName().getName();
			if (!checkedNames.contains(name)) {
				if (tmpl.checkValueomitRestriction(timestamp, definitionName, true, usageLocation)) {
					needsRuntimeCheck = true;
				}
				checkedNames.add(name);
			}
		}
		if (baseTemplate instanceof Named_Template_List) {
			if (((Named_Template_List) baseTemplate).chkRestrictionNamedListBaseTemplate(timestamp, definitionName,
					omitAllowed,checkedNames, neededCheckedCnt, usageLocation)) {
				needsRuntimeCheck = true;
			}
		}
		return needsRuntimeCheck;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkValueomitRestriction(final CompilationTimeStamp timestamp, final String definitionName, final boolean omitAllowed, final Location usageLocation) {
		if (omitAllowed) {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_OMIT, usageLocation);
		} else {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_VALUE, usageLocation);
		}

		boolean needsRuntimeCheck = false;
		int neededCheckedCnt = 0;
		final HashSet<String> checkedNames = new HashSet<String>();
		if (baseTemplate != null && myGovernor != null) {
			switch (myGovernor.getTypetype()) {
			case TYPE_TTCN3_SEQUENCE:
				neededCheckedCnt = ((TTCN3_Sequence_Type) myGovernor).getNofComponents();
				break;
			case TYPE_ASN1_SEQUENCE:
				neededCheckedCnt = ((ASN1_Sequence_Type) myGovernor).getNofComponents();
				break;
			case TYPE_TTCN3_SET:
				neededCheckedCnt = ((TTCN3_Set_Type) myGovernor).getNofComponents();
				break;
			case TYPE_ASN1_SET:
				neededCheckedCnt = ((ASN1_Set_Type) myGovernor).getNofComponents();
				break;
			case TYPE_SIGNATURE:
				neededCheckedCnt = ((Signature_Type) myGovernor).getNofParameters();
				break;
			default:
				// can not reach here because of a previous
				// check
				break;
			}
		}

		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			final NamedTemplate temp = namedTemplates.getTemplateByIndex(i);
			if (temp.getTemplate().checkValueomitRestriction(timestamp, definitionName, true, usageLocation)) {
				needsRuntimeCheck = true;
			}
				if (neededCheckedCnt > 0) {
				checkedNames.add(temp.getName().getName());
			}
		}
		if (neededCheckedCnt > 0) {
			if (baseTemplate instanceof Named_Template_List
					&& ((Named_Template_List) baseTemplate).chkRestrictionNamedListBaseTemplate(timestamp,
							definitionName, omitAllowed, checkedNames, neededCheckedCnt, usageLocation)) {
				needsRuntimeCheck = true;
			}
		}
		return needsRuntimeCheck;
	}

	@Override
	/** {@inheritDoc} */
	public ITTCN3Template getReferencedSetSequenceFieldTemplate(final CompilationTimeStamp timestamp, final Identifier fieldIdentifier,
			final Reference reference, final IReferenceChain referenceChain, final boolean silent) {
		if (hasNamedTemplate(fieldIdentifier)) {
			return getNamedTemplate(fieldIdentifier).getTemplate();
		} else if (baseTemplate != null) {
			// take the field from the base template
			final TTCN3Template temp = baseTemplate.getTemplateReferencedLast(timestamp, referenceChain);
			if (temp == null) {
				return null;
			}

			return temp.getReferencedFieldTemplate(timestamp, fieldIdentifier, reference, referenceChain, silent);
		} else {
			if (!reference.getUsedInIsbound() && !silent) {
				reference.getLocation().reportSemanticError(
						MessageFormat.format("Reference to unbound field `{0}''.", fieldIdentifier.getDisplayName()));
			}

			return null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (lengthRestriction != null) {
			lengthRestriction.updateSyntax(reparser, false);
			reparser.updateLocation(lengthRestriction.getLocation());
		}

		if (baseTemplate instanceof IIncrementallyUpdateable) {
			((IIncrementallyUpdateable) baseTemplate).updateSyntax(reparser, false);
			reparser.updateLocation(baseTemplate.getLocation());
		} else if (baseTemplate != null) {
			throw new ReParseException();
		}

		namedTemplates.updateSyntax(reparser, false);
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (asValue != null) {
			asValue.findReferences(referenceFinder, foundIdentifiers);
			return;
		}
		if (namedTemplates == null) {
			return;
		}
		if (referenceFinder.assignment.getAssignmentType() == Assignment_type.A_TYPE && referenceFinder.fieldId != null && myGovernor != null) {
			// check if this is the type and field we are searching
			// for
			final IType govLast = myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
			if (referenceFinder.type == govLast) {
				final NamedTemplate nt = namedTemplates.getNamedTemplateByName(referenceFinder.fieldId);
				if (nt != null) {
					foundIdentifiers.add(new Hit(nt.getName()));
				}
			}
		}
		namedTemplates.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (asValue != null) {
			if (!asValue.accept(v)) {
				return false;
			}
		} else if (namedTemplates != null) {
			if (!namedTemplates.accept(v)) {
				return false;
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNamePrefix(final String prefix) {
		super.setGenNamePrefix(prefix);
		for (int i = 0; i < namedTemplates.getNofTemplates(); i++) {
			namedTemplates.getTemplateByIndex(i).getTemplate().setGenNamePrefix(prefix);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNameRecursive(final String parameterGenName) {
		super.setGenNameRecursive(parameterGenName);

		if(myGovernor == null) {
			return;
		}

		for (int i = 0; i < namedTemplates.getNofTemplates(); i++) {
			final NamedTemplate namedTemplate = namedTemplates.getTemplateByIndex(i);

			final String javaGetterName = FieldSubReference.getJavaGetterName(namedTemplate.getName().getName());
			final String embeddedName = MessageFormat.format("{0}.get_field_{1}()", parameterGenName, javaGetterName);
			namedTemplate.getTemplate().setGenNameRecursive(embeddedName);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean needsTemporaryReference() {
		if (lengthRestriction != null || isIfpresent) {
			return true;
		}

		return namedTemplates.getNofTemplates() > 0;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression() {
		if (lengthRestriction != null || isIfpresent || get_needs_conversion()) {
			return false;
		}

		if (myGovernor == null) {
			return false;
		}

		switch (myGovernor.getTypetype()) {
		case TYPE_TTCN3_SEQUENCE:
			return ((TTCN3_Sequence_Type) myGovernor).getNofComponents() == 0;
		case TYPE_ASN1_SEQUENCE:
			return ((ASN1_Sequence_Type) myGovernor).getNofComponents() == 0;
		case TYPE_SIGNATURE:
			return ((Signature_Type) myGovernor).getNofParameters() == 0;
		case TYPE_TTCN3_SET:
			return ((TTCN3_Set_Type) myGovernor).getNofComponents() == 0;
		case TYPE_ASN1_SET:
			return ((ASN1_Set_Type) myGovernor).getNofComponents() == 0;
		default:
			return false;
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getSingleExpression(final JavaGenData aData, final boolean castIsNeeded) {
		if (namedTemplates.getNofTemplates() != 0) {
			ErrorReporter.INTERNAL_ERROR("INTERNAL ERROR: Can not generate single expression for named template list `" + getFullName() + "''");

			return new StringBuilder("FATAL_ERROR encountered while processing `" + getFullName() + "''\n");
		}

		aData.addBuiltinTypeImport("TitanNull_Type");

		if (myGovernor == null) {
			return new StringBuilder("TitanNull_Type.NULL_VALUE");
		}

		final StringBuilder result = new StringBuilder();
		final String genName = myGovernor.getGenNameTemplate(aData, result);
		result.append(MessageFormat.format("new {0}(TitanNull_Type.NULL_VALUE)", genName));
		return result;

	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression, final TemplateRestriction.Restriction_type templateRestriction) {
		if (lengthRestriction == null && !isIfpresent && templateRestriction == Restriction_type.TR_NONE) {
			//The single expression must be tried first because this rule might cover some referenced templates.
			if (hasSingleExpression()) {
				expression.expression.append(getSingleExpression(aData, true));
				return;
			}
		}

		if (asValue != null) {
			asValue.generateCodeExpression(aData, expression, true);
			return;
		}

		IType governor = myGovernor;
		if (governor == null) {
			governor = getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
		}
		if (governor == null) {
			return;
		}

		final String tempId = aData.getTemporaryVariableName();
		final String genName = governor.getGenNameTemplate(aData, expression.expression);
		expression.preamble.append(MessageFormat.format("final {0} {1} = new {0}();\n", genName, tempId));
		setGenNameRecursive(genName);
		generateCodeInit(aData, expression.preamble, tempId);

		if (templateRestriction != Restriction_type.TR_NONE) {
			TemplateRestriction.generateRestrictionCheckCode(aData, expression.preamble, location, tempId, templateRestriction);
		}

		expression.expression.append(tempId);
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		if (asValue != null) {
			asValue.reArrangeInitCode(aData, source, usageModule);
			return;
		}

		for (int i = 0; i < namedTemplates.getNofTemplates(); i++) {
			namedTemplates.getTemplateByIndex(i).getTemplate().reArrangeInitCode(aData, source, usageModule);
		}

		if (lengthRestriction != null) {
			lengthRestriction.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		if (lastTimeBuilt != null && !lastTimeBuilt.isLess(aData.getBuildTimstamp())) {
			return;
		}
		lastTimeBuilt = aData.getBuildTimstamp();

		if (asValue != null) {
			asValue.generateCodeInit(aData, source, name);
			return;
		}

		if (myGovernor == null) {
			return;
		}

		final IType type = myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		if (type == null) {
			return;
		}

		int nofTypeMembers;
		switch (myGovernor.getTypetype()) {
		case TYPE_TTCN3_SEQUENCE:
			nofTypeMembers = ((TTCN3_Sequence_Type) myGovernor).getNofComponents();
			break;
		case TYPE_ASN1_SEQUENCE:
			nofTypeMembers = ((ASN1_Sequence_Type) myGovernor).getNofComponents();
			break;
		case TYPE_SIGNATURE:
			nofTypeMembers = ((Signature_Type) myGovernor).getNofParameters();
			break;
		case TYPE_TTCN3_SET:
			nofTypeMembers = ((TTCN3_Set_Type) myGovernor).getNofComponents();
			break;
		case TYPE_ASN1_SET:
			nofTypeMembers = ((ASN1_Set_Type) myGovernor).getNofComponents();
			break;
		default:
			//some value not 0
			nofTypeMembers = 1;
			break;
		}

		if (nofTypeMembers == 0) {
			aData.addBuiltinTypeImport("TitanNull_Type");

			source.append(MessageFormat.format("{0}.operator_assign(TitanNull_Type.NULL_VALUE);\n", name));

			if (lengthRestriction != null) {
				if(getCodeSection() == CodeSectionType.CS_POST_INIT) {
					lengthRestriction.reArrangeInitCode(aData, source, myScope.getModuleScopeGen());
				}
				lengthRestriction.generateCodeInit(aData, source, name);
			}

			if (isIfpresent) {
				source.append(name);
				source.append(".set_ifPresent();\n");
			}

			return;
		}

		for (int i = 0; i < namedTemplates.getNofTemplates(); i++) {
			final NamedTemplate namedTemplate = namedTemplates.getTemplateByIndex(i);
			final Identifier fieldId = namedTemplate.getName();
			String fieldName = fieldId.getName();
			String generatedFieldName = FieldSubReference.getJavaGetterName(fieldName);
			final TTCN3Template template = namedTemplate.getTemplate();
			if (template.needsTemporaryReference()) {
				Type fieldType;
				switch(type.getTypetype()) {
				case TYPE_SIGNATURE:
					fieldType = ((Signature_Type) type).getParameterByName(fieldName).getType();
					break;
				case TYPE_TTCN3_SEQUENCE:
					fieldType = ((TTCN3_Sequence_Type) type).getComponentByName(fieldName).getType();
					break;
				case TYPE_TTCN3_SET:
					fieldType = ((TTCN3_Set_Type) type).getComponentByName(fieldName).getType();
					break;
				case TYPE_ASN1_SEQUENCE:
					fieldType = ((ASN1_Sequence_Type) type).getComponentByName(fieldId).getType();
					break;
				case TYPE_ASN1_SET:
					fieldType = ((ASN1_Set_Type) type).getComponentByName(fieldId).getType();
					break;
				case TYPE_ASN1_CHOICE:
					fieldType = ((ASN1_Choice_Type) type).getComponentByName(fieldId).getType();
					break;
				case TYPE_TTCN3_CHOICE:
					fieldType = ((TTCN3_Choice_Type) type).getComponentByName(fieldName).getType();
					break;
				case TYPE_OPENTYPE: {
					final CompField field = ((Open_Type) type).getComponentByName(fieldId);
					fieldType = field.getType();
					fieldName = field.getIdentifier().getName();
					generatedFieldName = FieldSubReference.getJavaGetterName(fieldName);
					break;
				}
				case TYPE_ANYTYPE:
					fieldType = ((Anytype_Type) type).getComponentByName(fieldName).getType();
					break;
				default:
					ErrorReporter.INTERNAL_ERROR("FATAL ERROR while processing named template list `" + getFullName() + "''");
					return;
				}

				final String tempId = aData.getTemporaryVariableName();
				source.append("{\n");
				source.append(MessageFormat.format("final {0} {1} = {2}.get_field_{3}();\n", fieldType.getGenNameTemplate(aData, source), tempId, name, generatedFieldName));
				template.generateCodeInit(aData, source, tempId);
				source.append("}\n");
			} else {
				if (type.getTypetype() == Type_type.TYPE_OPENTYPE) {
					final CompField field = ((Open_Type) type).getComponentByName(fieldId);
					if (field != null) {
						fieldName = field.getIdentifier().getName();
						generatedFieldName = FieldSubReference.getJavaGetterName(fieldName);
					}
				}
				final String embeddedName = MessageFormat.format("{0}.get_field_{1}()", name, generatedFieldName);
				template.generateCodeInit(aData, source, embeddedName);
			}
		}

		if (lengthRestriction != null) {
			if(getCodeSection() == CodeSectionType.CS_POST_INIT) {
				lengthRestriction.reArrangeInitCode(aData, source, myScope.getModuleScopeGen());
			}
			lengthRestriction.generateCodeInit(aData, source, name);
		}

		if (isIfpresent) {
			source.append(name);
			source.append(".set_ifPresent();\n");
		}
	}
}
