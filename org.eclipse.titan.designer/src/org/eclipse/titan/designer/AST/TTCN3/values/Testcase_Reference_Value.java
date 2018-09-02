/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.values;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ArraySubReference;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.ParameterisedSubReference;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Testcase;
import org.eclipse.titan.designer.AST.TTCN3.types.Testcase_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a testcase reference value.
 * <p>
 * Can not be parsed.
 *
 * @author Kristof Szabados
 */
public final class Testcase_Reference_Value extends Value {

	private final Def_Testcase referredTestcase;

	public Testcase_Reference_Value(final Def_Testcase referredTestcase) {
		this.referredTestcase = referredTestcase;
	}

	@Override
	/** {@inheritDoc} */
	public Value_type getValuetype() {
		return Value_type.TESTCASE_REFERENCE_VALUE;
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder("refers(");
		builder.append(referredTestcase.getAssignmentName()).append(')');
		return builder.toString();
	}

	public Def_Testcase getReferredTestcase() {
		return referredTestcase;
	}

	@Override
	/** {@inheritDoc} */
	public IValue getReferencedSubValue(final CompilationTimeStamp timestamp, final Reference reference,
			final int actualSubReference, final IReferenceChain refChain) {
		final List<ISubReference> subreferences = reference.getSubreferences();
		if (getIsErroneous(timestamp) || subreferences.size() <= actualSubReference) {
			return this;
		}

		final IType type = myGovernor.getTypeRefdLast(timestamp);
		final ISubReference subreference = subreferences.get(actualSubReference);
		switch (subreference.getReferenceType()) {
		case arraySubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(ArraySubReference.INVALIDVALUESUBREFERENCE, type.getTypename()));
			return null;
		case fieldSubReference:
			subreference.getLocation().reportSemanticError(MessageFormat.format(
					FieldSubReference.INVALIDSUBREFERENCE, ((FieldSubReference) subreference).getId().getDisplayName(), type.getTypename()));
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
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public Type_type getExpressionReturntype(final CompilationTimeStamp timestamp, final Expected_Value_type expectedValue) {
		return Type_type.TYPE_TESTCASE;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkEquality(final CompilationTimeStamp timestamp, final IValue other) {
		final IReferenceChain referenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		final IValue last = other.getValueRefdLast(timestamp, referenceChain);
		referenceChain.release();

		return Value_type.TESTCASE_REFERENCE_VALUE.equals(last.getValuetype())
				&& referredTestcase == ((Testcase_Reference_Value) last).getReferredTestcase();
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		// no members
		return true;
	}

	/** {@inheritDoc} */
	public boolean canGenerateSingleExpression() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateCodeInit(final JavaGenData aData, final StringBuilder source, final String name) {
		source.append(name);
		source.append(".assign( ");
		source.append(generateSingleExpression(aData));
		source.append(" );\n");

		return source;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder generateSingleExpression(final JavaGenData aData) {
		final StringBuilder result = new StringBuilder();

		IType governor = myGovernor;
		if (governor == null) {
			governor = getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
		}
		if (governor == null) {
			governor = myLastSetGovernor;
		}
		if (governor == null || referredTestcase == null) {
			ErrorReporter.INTERNAL_ERROR("FATAL ERROR while generating code for value `" + getFullName() + "''");
			return result;
		}

		final IType lastGovernor = governor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		result.append(MessageFormat.format("new {0}(new {0}.function_pointer() '{'\n", governor.getGenNameValue(aData, result, myScope)));
		result.append("@Override\n");
		result.append("public String getModuleName() {\n");
		result.append(MessageFormat.format("return \"{0}\";\n", referredTestcase.getMyScope().getModuleScope().getIdentifier().getName()));
		result.append("}\n");
		result.append("@Override\n");
		result.append("public String getDefinitionName() {\n");
		result.append(MessageFormat.format("return \"{0}\";\n", referredTestcase.getIdentifier().getName()));
		result.append("}\n");
		aData.addBuiltinTypeImport("TitanVerdictType");
		aData.addBuiltinTypeImport("TitanFloat");

		final String moduleName = referredTestcase.getMyScope().getModuleScope().getName();
		final String testcaseName = referredTestcase.getIdentifier().getName();
		result.append("@Override\n");
		result.append("public ");
		final Testcase_Type testcaseType = (Testcase_Type) lastGovernor;

		result.append("TitanVerdictType");
		result.append(" execute(");
		if (testcaseType.getFormalParameters().getNofParameters() > 0) {
			testcaseType.getFormalParameters().generateCode(aData, result);
			result.append(", ");
		}
		result.append("boolean has_timer, TitanFloat timer_value");
		result.append(") {\n");
		result.append(MessageFormat.format("return {0}.testcase_{1}(", moduleName, testcaseName));
		if (testcaseType.getFormalParameters().getNofParameters() > 0) {
			result.append(testcaseType.getFormalParameters().generateCodeActualParlist(""));
			result.append(", ");
		}
		result.append("has_timer, timer_value");
		result.append(");\n");
		result.append("}\n");
		result.append("})\n");

		return result;
	}
}
