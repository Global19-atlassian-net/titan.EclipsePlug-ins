/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.types.SignatureFormalParameter;
import org.eclipse.titan.designer.AST.TTCN3.types.SignatureFormalParameterList;
import org.eclipse.titan.designer.AST.TTCN3.types.Signature_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents the parameter redirection of a getcall/getreply operation.
 * <p>
 * Provided with variable list notation
 *
 * @author Kristof Szabados
 * */
public final class VariableList_Parameter_Redirection extends Parameter_Redirection {
	private static final String FULLNAMEPART = ".parametervariables";

	private final Variable_Entries entries;

	/**
	 * Constructs the variable list style parameter redirection with the
	 * variable entries provided.
	 *
	 * @param entries
	 *                the entries to manage.
	 * */
	public VariableList_Parameter_Redirection(final Variable_Entries entries) {
		this.entries = entries;

		if (entries != null) {
			entries.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (entries == child) {
			return builder.append(FULLNAMEPART);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (entries != null) {
			entries.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (entries != null) {
			entries.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasDecodedModifier() {
		for (int i = 0, size = entries.getNofEntries(); i < size; i++) {
			final Variable_Entry entry = entries.getEntryByIndex(i);
			if (entry.isDecoded()) {
				return true;
			}
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void checkErroneous(final CompilationTimeStamp timestamp) {
		for (int i = 0, size = entries.getNofEntries(); i < size; i++) {
			final Variable_Entry entry = entries.getEntryByIndex(i);
			checkVariableReference(timestamp, entry.getReference(), null);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final Signature_Type signature, final boolean isOut) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}
		this.checkErroneous(timestamp);
		if(signature == null) {
			return;
		}
		final SignatureFormalParameterList parameterList = signature.getParameterList();
		if (parameterList.getNofParameters() == 0) {
			getLocation().reportSemanticError(MessageFormat.format(SIGNATUREWITHOUTPARAMETERS, signature.getTypename()));
			checkErroneous(timestamp);
			return;
		}

		final int nofVariableEntries = entries.getNofEntries();
		final int nofParameters = isOut ? parameterList.getNofOutParameters() : parameterList.getNofInParameters();
		if (nofVariableEntries != nofParameters) {
			getLocation().reportSemanticError(
					MessageFormat.format(
							"Too {0} variable entries compared to the number of {1}/inout parameters in signature `{2}'': {3} was expected instead of {4}",
							(nofVariableEntries > nofParameters) ? "many" : "few", isOut ? "out" : "in",
									signature.getTypename(), nofParameters, nofVariableEntries));
		}

		for (int i = 0; i < nofVariableEntries; i++) {
			final Variable_Entry entry = entries.getEntryByIndex(i);
			if (i < nofParameters) {
				final SignatureFormalParameter parameter = isOut ? parameterList.getOutParameterByIndex(i) : parameterList
						.getInParameterByIndex(i);
				checkVariableReference(timestamp, entry.getReference(), parameter.getType());
			} else {
				checkVariableReference(timestamp, entry.getReference(), null);
			}
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		entries.updateSyntax(reparser, isDamaged);
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (entries == null) {
			return;
		}

		entries.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (entries != null && !entries.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final ExpressionStruct expression, final TemplateInstance matched_ti, final String lastGenTIExpression, final boolean is_out) {
		internalGenerateCode(aData, expression, entries, matched_ti, lastGenTIExpression, is_out);
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeDecoded(final JavaGenData aData, final StringBuilder source, final TemplateInstance matched_ti, final String tempID, final boolean is_out) {
		internalGenerateCodeDecoded(aData, source, entries, matched_ti, tempID, is_out);
	}
}
