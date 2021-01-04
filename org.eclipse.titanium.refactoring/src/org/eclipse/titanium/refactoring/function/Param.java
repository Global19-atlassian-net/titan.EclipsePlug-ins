/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.function;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Timer;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Var_Template;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Definition;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * The passing method of a function parameter. <code>NONE</code>: parameter will
 * not be created
 * */
enum ArgumentPassingType {
	IN, OUT, INOUT, NONE;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}

/**
 * This class represents a parameter of the new function.
 *
 * @author Viktor Varga
 */
class Param {
	private static final String FUNCTION_TEXT_PARAM_TYPE_SEPARATOR = " ";
	private static final String FUNCTION_TEXT_PARAM_TEMPLATE = "template";
	private static final String FUNCTION_TEXT_PARAM_TIMER = "timer";
	private static final String FUNCTION_TEXT_PARAM_COMMA = ", ";

	/**
	 * the original definition of the referred variable
	 * */
	private Definition def;
	/**
	 * the new local variable name
	 */
	private StringBuilder name;
	/**
	 * the string representation of the parameter's type
	 */
	private IType type;
	private ArgumentPassingType passingType;
	/**
	 * <code>true</code> if the parameter was originally declared inside the
	 * selection
	 */
	private boolean declaredInside;
	/**
	 * the original references to <code>def</code> from inside the selection
	 * */
	private List<ISubReference> refs = new ArrayList<ISubReference>();

	/**
	 * Creates the text representation of this parameter as a member of a
	 * function's formal parameter list
	 */
	public List<StringBuilder> createParamText(final boolean addCommaBefore) {
		final List<StringBuilder> ret = new ArrayList<StringBuilder>();
		if (passingType == ArgumentPassingType.NONE) {
			return ret;
		}
		if (addCommaBefore) {
			ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_COMMA));
		}
		if (def instanceof Def_Timer) {
			// format: timer timerName
			ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_TIMER));
			ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_TYPE_SEPARATOR));
		} else {
			// format: in|out|inout typeName varName
			ret.add(new StringBuilder(passingType.toString()));
			ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_TYPE_SEPARATOR));
			if (def instanceof Def_Var_Template) {
				// format: in|out|inout template varName
				ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_TEMPLATE));
				ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_TYPE_SEPARATOR));
			}
			ret.add(new StringBuilder(getShortTypename(type)));
			ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_TYPE_SEPARATOR));
		}
		ret.add(name);
		return ret;
	}

	/**
	 * Creates the text representation of this parameter as a member of a
	 * function call statement's actual parameter list.
	 * */
	public List<StringBuilder> createParamCallText(final boolean addCommaBefore) {
		final List<StringBuilder> ret = new ArrayList<StringBuilder>();
		if (passingType == ArgumentPassingType.NONE) {
			return ret;
		}
		if (addCommaBefore) {
			ret.add(new StringBuilder(FUNCTION_TEXT_PARAM_COMMA));
		}
		ret.add(new StringBuilder(def.getIdentifier().toString()));
		return ret;
	}

	public String createDebugInfo() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Param {");
		sb.append("\n    def: ").append(
				def == null ? "null" : def.getIdentifier());
		sb.append("\n    name: ").append(name);
		sb.append("\n    type: ").append(
				type == null ? "null" : type.getTypename());
		sb.append("\n    passing: ").append(passingType);
		sb.append("\n    declaredInside: ").append(declaredInside);
		sb.append("\n    ").append(refs.size()).append(" references inside");
		sb.append("\n}");
		return sb.toString();
	}

	// GETTERS, SETTERS

	public ArgumentPassingType getPassingType() {
		return passingType;
	}

	public StringBuilder getName() {
		return name;
	}

	public String getTypeName() {
		return getShortTypename(type);
	}

	public boolean isDeclaredInside() {
		return declaredInside;
	}

	public Definition getDef() {
		return def;
	}

	public List<ISubReference> getRefs() {
		return refs;
	}

	public void setPassingType(final ArgumentPassingType passingType) {
		this.passingType = passingType;
	}

	public void setName(final String name) {
		if (this.name == null) {
			this.name = new StringBuilder(name);
		} else {
			this.name.setLength(0);
			this.name.append(name);
		}
	}

	public void setDeclaredInside(final boolean declaredInside) {
		this.declaredInside = declaredInside;
	}

	public void setDef(final Definition def) {
		this.def = def;
		this.type = this.def.getType(CompilationTimeStamp.getBaseTimestamp());
	}

	public void setRefs(final List<ISubReference> refs) {
		this.refs = refs;
	}

	// GETTERS, SETTERS END

	public static String getShortTypename(final IType type) {
		if (type == null) {
			return "null";
		}

		final String tname = type.getTypename();
		if (tname == null) {
			return "null";
		}

		final int ind = tname.lastIndexOf('.');
		return (ind == -1 || ind >= tname.length()) ? tname : tname
				.substring(ind + 1);
	}

	@Override
	public boolean equals(final Object arg0) {
		if (arg0 == this) {
			return true;
		}
		if (!(arg0 instanceof Param)) {
			return false;
		}

		final Param o = (Param) arg0;
		return def.equals(o.def);
	}

	@Override
	public int hashCode() {
		return def.hashCode();
	}

}
