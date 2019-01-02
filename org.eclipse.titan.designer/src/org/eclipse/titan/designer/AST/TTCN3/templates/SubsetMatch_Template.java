/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.types.Array_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.SequenceOf_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.SetOf_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents a template for the subset matching mechanism.
 *
 * <p>
 * Example 1:
 * <p>
 * type set of integer SoI;
 * <p>
 * template SoI t_1 := subset ( 1,2,? );
 *
 * <p>
 * Example 2:
 * <p>
 * type set of integer SoI;
 * <p>
 * template SoI t_SoI1 := {1, 2, (6..9)};
 * <p>
 * template subset(all from t_SOI1) length(2);
 *
 * @author Kristof Szabados
 * */
public final class SubsetMatch_Template extends CompositeTemplate {

	public SubsetMatch_Template(final ListOfTemplates templates) {
		super(templates);
	}

	@Override
	/** {@inheritDoc} */
	public Template_type getTemplatetype() {
		return Template_type.SUBSET_MATCH;
	}

	@Override
	/** {@inheritDoc} */
	public String getTemplateTypeName() {
		if (isErroneous) {
			return "erroneous subset match";
		}

		return "subset match";
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		if (getIsErroneous(timestamp)) {
			return Type_type.TYPE_UNDEFINED;
		}

		return Type_type.TYPE_SET_OF;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReferenceTemplate(final CompilationTimeStamp timestamp, final Assignment lhs) {
		for (int i = 0, size = templates.getNofTemplates(); i < size; i++) {
			if(templates.getTemplateByIndex(i).checkExpressionSelfReferenceTemplate(timestamp, lhs)) {
				return true;
			}
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void checkSpecificValue(final CompilationTimeStamp timestamp, final boolean allowOmit) {
		getLocation().reportSemanticError("A specific value expected instead of a subset match");
	}

	@Override
	/** {@inheritDoc} */
	protected void checkTemplateSpecificLengthRestriction(final CompilationTimeStamp timestamp, final Type_type typeType) {
		if (Type_type.TYPE_SET_OF.equals(typeType)) {
			final boolean hasAnyOrNone = templateContainsAnyornone();
			lengthRestriction.checkNofElements(timestamp, getNofTemplatesNotAnyornone(timestamp), hasAnyOrNone, true, hasAnyOrNone, this);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected String getNameForStringRep() {
		return "subset";
	}

	@Override
	/** {@inheritDoc} */
	public boolean needsTemporaryReference() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression() {
		if (lengthRestriction != null || isIfpresent /*TODO: || get_needs_conversion () */) {
			return false;
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression, final TemplateRestriction.Restriction_type templateRestriction) {
		IType governor = myGovernor;
		if (governor == null) {
			governor = getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
		}
		if (governor == null) {
			return;
		}

		final String genName = governor.getGenNameTemplate(aData, expression.expression, myScope);
		final String tempId = aData.getTemporaryVariableName();

		expression.preamble.append(MessageFormat.format("final {0} {1} = new {0}();\n", genName, tempId));
		setGenNameRecursive(tempId);
		generateCodeInit(aData, expression.preamble, tempId);

		if (templateRestriction != Restriction_type.TR_NONE) {
			TemplateRestriction.generateRestrictionCheckCode(aData, expression.preamble, location, tempId, templateRestriction);
		}

		expression.expression.append(tempId);
	}

	@Override
	/** {@inheritDoc} */
	public void reArrangeInitCode(final JavaGenData aData, final StringBuilder source, final Module usageModule) {
		for (int i = 0; i < templates.getNofTemplates(); i++) {
			templates.getTemplateByIndex(i).reArrangeInitCode(aData, source, usageModule);
		}

		if (lengthRestriction != null) {
			lengthRestriction.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		lastTimeBuilt = aData.getBuildTimstamp();

		aData.addBuiltinTypeImport("Base_Template.template_sel");

		String ofTypeName;
		switch (myGovernor.getTypetype()) {
		case TYPE_SEQUENCE_OF:
			ofTypeName = ((SequenceOf_Type) myGovernor).getOfType().getGenNameTemplate(aData, source, myScope);
			break;
		case TYPE_SET_OF:
			ofTypeName = ((SetOf_Type) myGovernor).getOfType().getGenNameTemplate(aData, source, myScope);
			break;
		case TYPE_ARRAY:
			ofTypeName = ((Array_Type) myGovernor).getElementType().getGenNameTemplate(aData, source, myScope);
			break;
		default:
			ErrorReporter.INTERNAL_ERROR("FATAL ERROR while processing subset match template `" + getFullName() + "''");
			return;
		}

		final ArrayList<Integer> variables = new ArrayList<Integer>();
		long fixedPart = 0;
		for (int i = 0; i < templates.getNofTemplates(); i++) {
			final TTCN3Template templateListItem = templates.getTemplateByIndex(i);
			if (templateListItem.getTemplatetype() == Template_type.ALL_FROM) {
				variables.add(i);
			} else {
				fixedPart++;
			}
		}

		if (variables.size() > 0) {
			final StringBuilder preamble = new StringBuilder();
			final StringBuilder setType = new StringBuilder();
			final StringBuilder variableReferences[] = new StringBuilder[templates.getNofTemplates()];

			setType.append(MessageFormat.format("{0}.set_type(template_sel.SUBSET_MATCH, {1}", name, fixedPart));

			for (int v = 0; v < variables.size(); v++) {
				TTCN3Template template = templates.getTemplateByIndex(variables.get(v));
				// the template must be all from
				if ( template instanceof All_From_Template ) {
					template = ((All_From_Template)template).getAllFrom();
				}

				final Reference reference = ((SpecificValue_Template) template).getReference();
				final Assignment assignment = reference.getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false);

				setType.append(" + ");

				final ExpressionStruct expression = new ExpressionStruct();
				reference.generateCode(aData, expression);
				if (expression.preamble.length() > 0) {
					preamble.append(expression.preamble);
				}

				switch (assignment.getAssignmentType()) {
				case A_CONST:
				case A_EXT_CONST:
				case A_MODULEPAR:
				case A_VAR:
				case A_PAR_VAL:
				case A_PAR_VAL_IN:
				case A_PAR_VAL_OUT:
				case A_PAR_VAL_INOUT:
				case A_FUNCTION_RVAL:
				case A_EXT_FUNCTION_RVAL:
					if (assignment.getType(CompilationTimeStamp.getBaseTimestamp()).fieldIsOptional(reference.getSubreferences())) {
						expression.expression.append(".get()");
					}
					break;
				default:
					break;
				}

				variableReferences[variables.get(v)] = expression.expression;
				setType.append(expression.expression);
				setType.append(".n_elem().get_int()");
			}

			source.append(preamble);
			source.append(setType);
			source.append(");\n");

			final StringBuilder shifty = new StringBuilder();
			for (int i = 0; i < templates.getNofTemplates(); i++) {
				final TTCN3Template template = templates.getTemplateByIndex(i);

				switch (template.getTemplatetype()) {
				case ALL_FROM: {
					// the template must be all from
					final StringBuilder storedExpression = variableReferences[i];
					source.append(MessageFormat.format("for (int i_i = 0, i_lim = {0}.n_elem().get_int(); i_i < i_lim; ++i_i ) '{'\n", storedExpression));

					final String embeddedName = MessageFormat.format("{0}.set_item({1}{2} + i_i)", name, i, shifty);
					((All_From_Template) template).generateCodeInitAllFrom(aData, source, embeddedName, storedExpression);
					source.append("}\n");
					shifty.append(MessageFormat.format("-1 + {0}.n_elem().get_int()", storedExpression));
					break;
				}
				default:
					if (template.needsTemporaryReference()) {
						final String tempId = aData.getTemporaryVariableName();
						source.append("{\n");
						source.append(MessageFormat.format("final {0} {1} = {2}.set_item({3}{4});\n", ofTypeName, tempId, name, i, shifty));
						template.generateCodeInit(aData, source, tempId);
						source.append("}\n");
					} else {
						final String embeddedName = MessageFormat.format("{0}.set_item({1}{2})", name, i, shifty);
						template.generateCodeInit(aData, source, embeddedName);
					}
					break;
				}
			}
		} else {
			source.append(MessageFormat.format("{0}.set_type(template_sel.SUBSET_MATCH, {1});\n", name, templates.getNofTemplates()));
			for (int i = 0; i < templates.getNofTemplates(); i++) {
				final TTCN3Template template = templates.getTemplateByIndex(i);
				if (template.needsTemporaryReference()) {
					final String tempId = aData.getTemporaryVariableName();
					source.append("{\n");
					source.append(MessageFormat.format("final {0} {1} = {2}.set_item({3});\n", ofTypeName, tempId, name, i));
					template.generateCodeInit(aData, source, tempId);
					source.append("}\n");
				} else {
					final String embeddedName = MessageFormat.format("{0}.set_item({1})", name, i);
					template.generateCodeInit(aData, source, embeddedName);
				}
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
