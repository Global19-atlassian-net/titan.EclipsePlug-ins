/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   
 *   Keremi, Andras
 *   Eros, Levente
 *   Kovacs, Gabor
 *   Meszaros, Mate Robert
 *
 ******************************************************************************/

package org.eclipse.titan.codegenerator;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Type;

public class Def_Type_Set_Of_Writer implements JavaSourceProvider {

	private static Map<String, Def_Type_Set_Of_Writer> setOfHashes = new LinkedHashMap<>();

	public static Def_Type_Set_Of_Writer getInstance(Def_Type typeNode) {
		String id = typeNode.getIdentifier().toString();
		if (!setOfHashes.containsKey(id)) {
			setOfHashes.put(id, new Def_Type_Set_Of_Writer(typeNode));
		}
		return setOfHashes.get(id);
	}

	private SourceCode code = new SourceCode();

	private final String typeName;
	private String fieldType;

	public Def_Type_Set_Of_Writer(Def_Type typeNode) {
		typeName = typeNode.getIdentifier().toString();
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	@Override
	public String getJavaSource() {
		code.clear();
		AstWalkerJava.logToConsole("	Starting processing:  Set of " + typeName);
		code.line("import java.util.Arrays;");
		code.line();
		code.line("class ", typeName, " extends SetOfDef<", fieldType, "> {");
		writeTemplateObjects();
		code.line();
		writeConstructor();
		code.line();
		writeMatcher();
		code.line();
		writeEquals();
		code.line();
		writeCheckValue();
		code.line();
		code.line("}");
		AstWalkerJava.logToConsole("	Finished processing:  Set of " + typeName);
		return code.toString();
	}

	private void writeTemplateObjects() {
		code.indent(1).line("public static final ", typeName, " ANY = new ", typeName, "();");
		code.indent(1).line("public static final ", typeName, " OMIT = new ", typeName, "();");
		code.indent(1).line("public static final ", typeName, " ANY_OR_OMIT = new ", typeName, "();");
		code.newLine();
		code.indent(1).line("static {");
		code.indent(2).line("ANY.anyField = true;");
		code.indent(2).line("OMIT.omitField = true;");
		code.indent(2).line("ANY_OR_OMIT.anyOrOmitField = true;");
		code.indent(1).line("}");
	}

	private void writeConstructor() {
		code.indent(1).line("public ", typeName, "(", fieldType, "... values) {");
		code.indent(2).line("this(new HashSet<>(Arrays.asList(values)));");
		code.indent(1).line("}");
		code.line();
		code.indent(1).line("public ", typeName, "(HashSet<", fieldType, "> set) {");
		code.indent(2).line("value = set;");
		code.indent(1).line("}");
	}

	private void writeMatcher() {
		code.indent(1).line("public static boolean match(", typeName, " pattern, Object message) {");
		code.indent(2).line("if (!(message instanceof ", typeName, ")) return false;");
		// TODO : introduce a type-safe variable instead of casting it each time
		// TODO : simplify the if statements into one boolean expression (eg.: a && b || c)
		code.indent(2).line("if (pattern.omitField && ((", typeName, ")message).omitField) return true;");
		code.indent(2).line("if (pattern.anyOrOmitField) return true;");
		code.indent(2).line("if (pattern.anyField && !((", typeName, ")message).omitField) return true;");
		code.indent(2).line("if (pattern.anyField && !((", typeName, ")message).omitField) return true;");
		code.indent(2).line("if (pattern.omitField && !((", typeName, ")message).omitField) return false;");
		code.indent(2).line("if (pattern.anyField && ((", typeName, ")message).omitField) return false;");
		code.indent(2).line("return pattern.equals((", typeName, ")message).getValue();");
		code.indent(1).line("}");
	}

	private void writeEquals() {
		code.indent(1).line("public BOOLEAN equals(SetOfDef<", fieldType, "> v) {");
		code.indent(2).line("if (value.size() != v.value.size()) return BOOLEAN.FALSE;");
		code.indent(2).line("for (", fieldType, " i : value) {");
		code.indent(3).line("boolean found = false;");
		code.indent(3).line("for (", fieldType, " j : v.value) {");
		code.indent(4).line("if (i.equals(j).getValue()) {");
		code.indent(5).line("found = true;");
		code.indent(5).line("break;");
		code.indent(4).line("}");
		code.indent(3).line("}");
		code.indent(3).line("if (!found) return BOOLEAN.FALSE;");
		code.indent(2).line("}");
		code.indent(2).line("return BOOLEAN.TRUE;");
		code.indent(1).line("}");
	}
	
	private void writeCheckValue(){
		code.indent(1).line(" public void checkValue() throws IndexOutOfBoundsException {");
		code.indent(2).line("	return;");
		code.indent(1).line("}");
	}
	
}
