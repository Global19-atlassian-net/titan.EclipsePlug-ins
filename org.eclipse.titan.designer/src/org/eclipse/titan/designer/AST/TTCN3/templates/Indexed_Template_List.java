/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.types.Array_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.SequenceOf_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.SetOf_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.ArrayDimension;
import org.eclipse.titan.designer.AST.TTCN3.values.IndexedValue;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SequenceOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Values;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a list of templates given with indexed notation.
 *
 * @author Kristof Szabados
 * */
public final class Indexed_Template_List extends TTCN3Template {

	private final IndexedTemplates indexedTemplates;

	// cache storing the value form of this if already created, or null
	private SequenceOf_Value asValue = null;

	public Indexed_Template_List(final IndexedTemplates indexedTemplates) {
		this.indexedTemplates = indexedTemplates;

		indexedTemplates.setFullNameParent(this);
	}

	@Override
	/** {@inheritDoc} */
	public Template_type getTemplatetype() {
		return Template_type.INDEXED_TEMPLATE_LIST;
	}

	@Override
	/** {@inheritDoc} */
	public String getTemplateTypeName() {
		if (isErroneous) {
			return "erroneous indexed assignment notation";
		}

		return "indexed assignment notation";
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		for (int i = 0, size = indexedTemplates.getNofTemplates(); i < size; i++) {
			if (i > 0) {
				builder.append(", ");
			}

			final IndexedTemplate indexedTemplate = indexedTemplates.getTemplateByIndex(i);
			builder.append(" [").append(indexedTemplate.getIndex().getValue().createStringRepresentation());
			builder.append("] := ");
			builder.append(indexedTemplate.getTemplate().createStringRepresentation());
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
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0, size = indexedTemplates.getNofTemplates(); i < size; i++) {
			final IndexedTemplate template = indexedTemplates.getTemplateByIndex(i);
			if (template == child) {
				final IValue index = template.getIndex().getValue();
				return builder.append(INamedNode.SQUAREOPEN).append(index.createStringRepresentation())
						.append(INamedNode.SQUARECLOSE);
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (indexedTemplates != null) {
			indexedTemplates.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		super.setCodeSection(codeSection);
		for (int i = 0, size = indexedTemplates.getNofTemplates(); i < size; i++) {
			indexedTemplates.getTemplateByIndex(i).setCodeSection(codeSection);
		}
		if (lengthRestriction != null) {
			lengthRestriction.setCodeSection(codeSection);
		}
	}

	/** @return the number of templates in the list */
	public int getNofTemplates() {
		if (indexedTemplates == null) {
			return 0;
		}

		return indexedTemplates.getNofTemplates();
	}

	/**
	 * @param index
	 *                the index of the element to return.
	 *
	 * @return the template on the indexed position.
	 * */
	public IndexedTemplate getIndexedTemplateByIndex(final int index) {
		return indexedTemplates.getTemplateByIndex(index);
	}

	@Override
	/** {@inheritDoc} */
	protected ITTCN3Template getReferencedArrayTemplate(final CompilationTimeStamp timestamp, final IValue arrayIndex,
			final IReferenceChain referenceChain, final boolean silent) {
		IValue indexValue = arrayIndex.setLoweridToReference(timestamp);
		indexValue = indexValue.getValueRefdLast(timestamp, referenceChain);
		if (indexValue.getIsErroneous(timestamp)) {
			return null;
		}

		long index = 0;
		if (!indexValue.isUnfoldable(timestamp)) {
			if (Value_type.INTEGER_VALUE.equals(indexValue.getValuetype())) {
				index = ((Integer_Value) indexValue).getValue();
			} else {
				if (!silent) {
					arrayIndex.getLocation().reportSemanticError("An integer value was expected as index");
				}
				return null;
			}
		} else {
			return null;
		}

		final IType tempType = myGovernor.getTypeRefdLast(timestamp);
		if (tempType.getIsErroneous(timestamp)) {
			return null;
		}

		switch (tempType.getTypetype()) {
		case TYPE_SEQUENCE_OF: {
			if (index < 0) {
				if (!silent) {
					final String message = MessageFormat
						.format("A non-negative integer value was expected instead of {0} for indexing a template of `sequence of'' type `{1}''",
								index, tempType.getTypename());
					arrayIndex.getLocation().reportSemanticError(message);
				}
				return null;
			}
			break;
		}
		case TYPE_SET_OF: {
			if (index < 0) {
				if (!silent) {
					final String message = MessageFormat
						.format("A non-negative integer value was expected instead of {0} for indexing a template of `set of'' type `{1}''",
								index, tempType.getTypename());
					arrayIndex.getLocation().reportSemanticError(message);
				}
				return null;
			}
			break;
		}
		case TYPE_ARRAY: {
			final ArrayDimension dimension = ((Array_Type) tempType).getDimension();
			dimension.checkIndex(timestamp, indexValue, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
			if (!dimension.getIsErroneous(timestamp)) {
				// re-base the index
				index -= dimension.getOffset();
				if (index < 0 || index > getNofTemplates()) {
					if (!silent) {
						arrayIndex.getLocation().reportSemanticError(
							MessageFormat.format("The index value {0} is outside the array indexable range", index
									+ dimension.getOffset()));
					}
					return null;
				}
			} else {
				return null;
			}
			break;
		}
		default:
			if (!silent) {
				arrayIndex.getLocation().reportSemanticError(
					MessageFormat.format("Invalid array element reference: type `{0}'' cannot be indexed",
							tempType.getTypename()));
			}
			return null;
		}

		for (int i = 0, size = indexedTemplates.getNofTemplates(); i < size; i++) {
			final IndexedTemplate template = indexedTemplates.getTemplateByIndex(i);
			IValue lastValue = template.getIndex().getValue();

			final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
			lastValue = lastValue.getValueRefdLast(timestamp, chain);
			chain.release();

			if (Value_type.INTEGER_VALUE.equals(lastValue.getValuetype())) {
				final long tempIndex = ((Integer_Value) lastValue).getValue();
				if (index == tempIndex) {
					final ITTCN3Template realTemplate = template.getTemplate();
					if (Template_type.TEMPLATE_NOTUSED.equals(realTemplate.getTemplatetype())) {
						if (baseTemplate != null) {
							return baseTemplate.getTemplateReferencedLast(timestamp, referenceChain)
									.getReferencedArrayTemplate(timestamp, indexValue, referenceChain, silent);
						}

						return null;
					}

					return realTemplate;
				}
			}
		}

		switch (tempType.getTypetype()) {
		case TYPE_SEQUENCE_OF:
		case TYPE_SET_OF:
		case TYPE_ARRAY:
			// unfoldable (for now)
			break;
		default:
			// the error was reported earlier
			break;
		}

		return null;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isValue(final CompilationTimeStamp timestamp) {
		if (lengthRestriction != null || isIfpresent || getIsErroneous(timestamp)) {
			return false;
		}

		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			if (!indexedTemplates.getTemplateByIndex(i).getTemplate().isValue(timestamp)) {
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

		final Values values = new Values(true);
		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			final IndexedTemplate indexedTemplate = indexedTemplates.getTemplateByIndex(i);
			final IndexedValue indexedValue = new IndexedValue(indexedTemplate.getIndex(), indexedTemplate.getTemplate().getValue());
			indexedValue.setLocation(indexedTemplate.getLocation());
			values.addIndexedValue(indexedValue);
		}
		asValue = new SequenceOf_Value(values);
		asValue.setLocation(getLocation());
		asValue.setMyScope(getMyScope());
		asValue.setFullNameParent(getNameParent());
		asValue.setMyGovernor(getMyGovernor());

		return asValue;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkExpressionSelfReferenceTemplate(final CompilationTimeStamp timestamp, final Assignment lhs) {
		for (int i = 0, size = indexedTemplates.getNofTemplates(); i < size; i++) {
			if(indexedTemplates.getTemplateByIndex(i).getTemplate().checkExpressionSelfReferenceTemplate(timestamp, lhs)) {
				return true;
			}
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void checkSpecificValue(final CompilationTimeStamp timestamp, final boolean allowOmit) {
		ITTCN3Template temp;
		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			temp = indexedTemplates.getTemplateByIndex(i).getTemplate();
			if (temp != null) {
				temp.checkSpecificValue(timestamp, true);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void checkRecursions(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (referenceChain.add(this)) {
			for (int i = 0, size = indexedTemplates.getNofTemplates(); i < size; i++) {
				final IndexedTemplate template = indexedTemplates.getTemplateByIndex(i);
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
	public boolean checkValueomitRestriction(final CompilationTimeStamp timestamp, final String definitionName, final boolean omitAllowed, final Location usageLocation) {
		if (omitAllowed) {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_OMIT, usageLocation);
		} else {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_VALUE, usageLocation);
		}

		for (int i = 0, size = indexedTemplates.getNofTemplates(); i < size; i++) {
			indexedTemplates.getTemplateByIndex(i).getTemplate().checkValueomitRestriction(timestamp, definitionName, true, usageLocation);
		}

		// complete check was not done, always needs runtime check
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		indexedTemplates.updateSyntax(reparser, false);
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (asValue != null) {
			asValue.findReferences(referenceFinder, foundIdentifiers);
		} else if (indexedTemplates != null) {
			indexedTemplates.findReferences(referenceFinder, foundIdentifiers);
		}
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
		} else if (indexedTemplates != null) {
			if (!indexedTemplates.accept(v)) {
				return false;
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNamePrefix(final String prefix) {
		super.setGenNamePrefix(prefix);
		for (int i = 0; i < indexedTemplates.getNofTemplates(); i++) {
			indexedTemplates.getTemplateByIndex(i).getTemplate().setGenNamePrefix(prefix);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean needsTemporaryReference() {
		if (lengthRestriction != null || isIfpresent) {
			return true;
		}

		// temporary reference is needed if the template has at least one
		// element (excluding not used symbols)
		for (int i = 0; i < indexedTemplates.getNofTemplates(); i++) {
			final TTCN3Template template = indexedTemplates.getTemplateByIndex(i).getTemplate();
			if (template.getTemplatetype() != Template_type.TEMPLATE_NOTUSED) {
				return true;
			}
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression() {
		if (lengthRestriction != null || isIfpresent /* TODO:  || get_needs_conversion()*/) {
			return false;
		}

		return indexedTemplates.getNofTemplates() == 0;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getSingleExpression(final JavaGenData aData, final boolean castIsNeeded) {
		if (indexedTemplates.getNofTemplates() != 0) {
			ErrorReporter.INTERNAL_ERROR("INTERNAL ERROR: Can not generate single expression for indexed template list `" + getFullName() + "''");

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

		for (int i = 0; i < indexedTemplates.getNofTemplates(); i++) {
			indexedTemplates.getTemplateByIndex(i).getTemplate().reArrangeInitCode(aData, source, usageModule);
		}

		if (lengthRestriction != null) {
			lengthRestriction.reArrangeInitCode(aData, source, usageModule);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		lastTimeBuilt = aData.getBuildTimstamp();

		if (asValue != null) {
			asValue.generateCodeInit(aData, source, name);
			return;
		}

		if (myGovernor == null) {
			return;
		}

		final IType type = myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		String ofTypeName;
		switch(type.getTypetype()) {
		case TYPE_SEQUENCE_OF:
			ofTypeName = ((SequenceOf_Type) type).getOfType().getGenNameTemplate(aData, source);
			break;
		case TYPE_SET_OF:
			ofTypeName = ((SetOf_Type) type).getOfType().getGenNameTemplate(aData, source);
			break;
		case TYPE_ARRAY:
			ofTypeName = ((Array_Type) type).getElementType().getGenNameTemplate(aData, source);
			break;
		default:
			ErrorReporter.INTERNAL_ERROR("FATAL ERROR while processing indexed template `" + getFullName() + "''");
			return;
		}

		if (indexedTemplates.getNofTemplates() == 0) {
			aData.addBuiltinTypeImport("TitanNull_Type");

			source.append(MessageFormat.format("{0}.operator_assign(TitanNull_Type.NULL_VALUE);\n", name));
		}//else is not needed as the loop will not run
		for (int i = 0; i < indexedTemplates.getNofTemplates(); i++) {
			final IndexedTemplate indexedTemplate = indexedTemplates.getTemplateByIndex(i);
			final String tempId = aData.getTemporaryVariableName();
			source.append("{\n");
			final Value index = indexedTemplate.getIndex().getValue();
			if (Value_type.INTEGER_VALUE.equals(index.getValuetype())) {
				source.append(MessageFormat.format("final {0} {1} = {2}.get_at({3});\n", ofTypeName, tempId, name, ((Integer_Value) index).getValue()));
			} else {
				final String tempId2 = aData.getTemporaryVariableName();
				source.append(MessageFormat.format("final TitanInteger {0} = new TitanInteger();\n", tempId2));
				index.generateCodeInit(aData, source, tempId2);
				source.append(MessageFormat.format("final {0} {1} = {2}.get_at({3});\n", ofTypeName, tempId, name, tempId2));
			}
			indexedTemplate.getTemplate().generateCodeInit(aData, source, tempId);
			source.append("}\n");
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
