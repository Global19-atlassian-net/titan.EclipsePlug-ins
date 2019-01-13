/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Var;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Var_Template;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.types.Signature_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents the parameter redirection of a getcall/getreply operation.
 *
 * @author Kristof Szabados
 * */
public abstract class Parameter_Redirect extends ASTNode implements ILocateableNode, IIncrementallyUpdateable {
	protected static final String SIGNATUREWITHOUTPARAMETERS = "Parameter redirect cannot be used because signature `{0}'' does not have parameters";

	private Location location = NULL_Location.INSTANCE;

	/** the time when this was checked the last time. */
	protected CompilationTimeStamp lastTimeChecked;

	@Override
	/** {@inheritDoc} */
	public final void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public final Location getLocation() {
		return location;
	}

	/**
	 * Sets the code_section attribute for the parameter redirection to the provided value.
	 *
	 * @param codeSection the code section where these statements should be generated.
	 * */
	public abstract void setCodeSection(final CodeSectionType codeSection);

	/**
	 * @return {@code true} if at least one of the value redirects has the
	 * '@decoded' modifier
	 */
	public abstract boolean has_decoded_modifier();

	/**
	 * Does the semantic checking of the redirected parameter.
	 * <p>
	 * Does report errors, should only be called if there were errors found
	 * before.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * */
	public abstract void checkErroneous(final CompilationTimeStamp timestamp);

	/**
	 * Check whether the reference points to a variable of the provided
	 * type.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param reference
	 *                the reference to check
	 * @param type
	 *                the type the parameter is expected to have.
	 * */
	public final void checkVariableReference(final CompilationTimeStamp timestamp, final Reference reference, final IType type) {
		if (reference == null) {
			return;
		}

		final IType variableType = reference.checkVariableReference(timestamp);
		if (type != null && variableType != null && !type.isIdentical(timestamp, variableType)) {
			final String message = MessageFormat.format(
					"Type mismatch in parameter redirect: A variable of type `{0}'' was expected instead of `{1}''",
					type.getTypename(), variableType.getTypename());
			reference.getLocation().reportSemanticError(message);
			return;
		}

		final Assignment assignment = reference.getRefdAssignment(timestamp, true);
		if (assignment != null) {
			switch (assignment.getAssignmentType()) {
			case A_PAR_VAL:
			case A_PAR_VAL_OUT:
			case A_PAR_VAL_INOUT:
				((FormalParameter) assignment).setWritten();
				break;
			case A_VAR:
				((Def_Var) assignment).setWritten();
				break;
			case A_PAR_TEMP_OUT:
			case A_PAR_TEMP_INOUT:
				((FormalParameter) assignment).setWritten();
				break;
			case A_VAR_TEMPLATE:
				((Def_Var_Template) assignment).setWritten();
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Does the semantic checking of the redirected parameter.
	 * <p>
	 * Does not report errors, that is done by check_erroneous.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param signature
	 *                the signature the parameter redirection belongs to.
	 * @param isOut
	 *                tells if this parameter is an out parameter, or not.
	 * */
	public abstract void check(CompilationTimeStamp timestamp, Signature_Type signature, boolean isOut);

	/**
	 * Add generated java code for parameter redirection.
	 * @param aData only used to update imports if needed
	 * @param expression the expression for code generated
	 * @param matched_ti the template instance matched by the original statement.
	 * @param is_out {@code true} if the parameters have out direction, {@code false} otherwise.
	 */
	public abstract void generateCode( final JavaGenData aData, final ExpressionStruct expression , final TemplateInstance matched_ti, final boolean is_out);

	/**
	 * Generate a helper class that is needed for parameter redirections that also have at least one parameter redirection with decoding.
	 * 
	 * @param aData only used to update imports if needed
	 * @param source the source to append.
	 * @param matched_ti the template instance matched by the original statement.
	 * @param tempID the temporary id to be used for naming the class.
	 * @param is_out {@code true} if the parameters have out direction, {@code false} otherwise.
	 */
	public abstract void generateCodeDecoded(final JavaGenData aData, final StringBuilder source, final TemplateInstance matched_ti, final String tempID, final boolean is_out);
}
