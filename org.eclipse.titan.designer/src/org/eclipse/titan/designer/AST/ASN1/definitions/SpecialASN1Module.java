/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.definitions;

import java.io.StringReader;
import java.util.ArrayList;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.UnbufferedCharStream;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.parsers.SyntacticErrorStorage;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Identifier.Identifier_type;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.ModuleImportationChain;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.ASN1.ASN1Assignment;
import org.eclipse.titan.designer.AST.ASN1.ASN1Assignments;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.asn1parser.ASN1Listener;
import org.eclipse.titan.designer.parsers.asn1parser.Asn1Lexer;
import org.eclipse.titan.designer.parsers.asn1parser.Asn1Parser;
import org.eclipse.titan.designer.parsers.asn1parser.ModuleLevelTokenStreamTracker;
import org.eclipse.titan.designer.parsers.asn1parser.TokenWithIndexAndSubTokensFactory;

/**
 * This class handles a special ASN.1 module.
 * <p>
 * In ASN.1 there is a set of special assignments which are defined by the
 * standard, as built of other more basic elements. As such it is better to
 * "generate" them from their representation as if they would be part of a
 * special ASN.1 module.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class SpecialASN1Module {
	public static final String INTERNAL_MODULE = "<internal_module>";
	public static final String PARSINGFAILED = "Parsing failed for internal ASN.1 module";
	public static final String PARSINGFAILEDWITHREASON = "Parsing failed for internal ASN.1 module with: `{1}''";

	private static final String NEWLINE = System.getProperty("line.separator");

	private static final String EXTERNAL_ASSIGNMENT = " [UNIVERSAL 8] IMPLICIT SEQUENCE {" + NEWLINE
			+ "  identification CHOICE {" + NEWLINE
			+ "    syntaxes SEQUENCE {" + NEWLINE
			+ "      abstract OBJECT IDENTIFIER, " + NEWLINE
			+ "      transfer OBJECT IDENTIFIER " + NEWLINE
			+ "    }, " + NEWLINE
			+ "    syntax OBJECT IDENTIFIER, " + NEWLINE
			+ "    presentation-context-id INTEGER, " + NEWLINE
			+ "    context-negotiation SEQUENCE {" + NEWLINE
			+ "      presentation-context-id INTEGER, " + NEWLINE
			+ "      transfer-syntax OBJECT IDENTIFIER " + NEWLINE
			+ "    }, " + NEWLINE
			+ "    transfer-syntax OBJECT IDENTIFIER, " + NEWLINE
			+ "    fixed NULL " + NEWLINE
			+ "  }, " + NEWLINE
			+ "  data-value-descriptor ObjectDescriptor OPTIONAL, " + NEWLINE
			+ "  data-value OCTET STRING " + NEWLINE
			+ "} (WITH COMPONENTS {" + NEWLINE
			+ "  ..., " + NEWLINE
			+ "  identification (WITH COMPONENTS {" + NEWLINE
			+ "    ..., " + NEWLINE
			+ "    syntaxes        ABSENT, " + NEWLINE
			+ "    transfer-syntax ABSENT, " + NEWLINE
			+ "    fixed           ABSENT " + NEWLINE
			+ "  }) " + NEWLINE
			+ "})";

	private static final String EMBEDDED_PDV_ASSIGNMENT = "[UNIVERSAL 11] IMPLICIT SEQUENCE {" + NEWLINE
			+ "  identification CHOICE {" + NEWLINE
			+ "    syntaxes SEQUENCE {" + NEWLINE
			+ "      abstract OBJECT IDENTIFIER, " + NEWLINE
			+ "      transfer OBJECT IDENTIFIER " + NEWLINE
			+ "    }, " + NEWLINE
			+ "    syntax OBJECT IDENTIFIER, " + NEWLINE
			+ "    presentation-context-id INTEGER, " + NEWLINE
			+ "    context-negotiation SEQUENCE {" + NEWLINE
			+ "      presentation-context-id INTEGER, " + NEWLINE
			+ "      transfer-syntax OBJECT IDENTIFIER " + NEWLINE
			+ "    }, " + NEWLINE
			+ "    transfer-syntax OBJECT IDENTIFIER, " + NEWLINE
			+ "    fixed NULL " + NEWLINE
			+ "  }, " + NEWLINE
			+ "  data-value-descriptor ObjectDescriptor OPTIONAL, " + NEWLINE
			+ "  data-value OCTET STRING " + NEWLINE
			+ "} (WITH COMPONENTS {" + NEWLINE
			+ "  ..., " + NEWLINE
			+ "  data-value-descriptor ABSENT " + NEWLINE
			+ "})";

	private static final String CHARACTER_STRING_ASSIGNMENT = "[UNIVERSAL 29] IMPLICIT SEQUENCE {" + NEWLINE
			+ "  identification CHOICE {" + NEWLINE
			+ "    syntaxes SEQUENCE {" + NEWLINE
			+ "      abstract OBJECT IDENTIFIER, " + NEWLINE
			+ "      transfer OBJECT IDENTIFIER " + NEWLINE
			+ "    }, " + NEWLINE
			+ "    syntax OBJECT IDENTIFIER, " + NEWLINE
			+ "    presentation-context-id INTEGER, " + NEWLINE
			+ "    context-negotiation SEQUENCE {" + NEWLINE
			+ "      presentation-context-id INTEGER, " + NEWLINE
			+ "      transfer-syntax OBJECT IDENTIFIER " + NEWLINE
			+ "    }, " + NEWLINE
			+ "    transfer-syntax OBJECT IDENTIFIER, " + NEWLINE
			+ "    fixed NULL " + NEWLINE
			+ "  }, " + NEWLINE
			+ "  data-value-descriptor ObjectDescriptor OPTIONAL, " + NEWLINE
			+ "  string-value OCTET STRING " + NEWLINE
			+ "} (WITH COMPONENTS {" + NEWLINE
			+ "  ..., " + NEWLINE
			+ "  data-value-descriptor ABSENT " + NEWLINE
			+ "})";

	private static final String REAL_ASSIGNMENT = "[UNIVERSAL 9] IMPLICIT SEQUENCE {" + NEWLINE
			+ "  mantissa INTEGER, " + NEWLINE
			+ "  base INTEGER (2|10), " + NEWLINE
			+ "  exponent INTEGER " + NEWLINE
			+ "}";

	private static final String TYPE_IDENTIFIER_ASSIGNMENT = "CLASS " + NEWLINE
			+ "{" + NEWLINE
			+ "  &id OBJECT IDENTIFIER UNIQUE, " + NEWLINE
			+ "  &Type " + NEWLINE
			+ "} " + NEWLINE
			+ "WITH SYNTAX {" + NEWLINE
			+ "  &Type IDENTIFIED BY &id " + NEWLINE
			+ "}";

	private static final String ABSTRACT_SYNTAX_ASSIGNMENT = "CLASS {" + NEWLINE
			+ "  &id OBJECT IDENTIFIER UNIQUE, " + NEWLINE
			+ "  &Type, " + NEWLINE
			+ "  &property BIT STRING {handles-invalid-encodings(0)} DEFAULT {} " + NEWLINE
			+ "} " + NEWLINE
			+ "WITH SYNTAX {" + NEWLINE
			+ "  &Type IDENTIFIED BY &id [HAS PROPERTY &property] " + NEWLINE
			+ "}";

	private static final String [][] INTERNAL_ASSIGNMENTS = {{"EXTERNAL", EXTERNAL_ASSIGNMENT},
		{"EMBEDDED PDV", EMBEDDED_PDV_ASSIGNMENT},
		{"CHARACTER STRING", CHARACTER_STRING_ASSIGNMENT},
		{"REAL", REAL_ASSIGNMENT},
		{"TYPE-IDENTIFIER", TYPE_IDENTIFIER_ASSIGNMENT},
		{"ABSTRACT-SYNTAX", ABSTRACT_SYNTAX_ASSIGNMENT}};

	private static ASN1Module specialAssignmentsModule = createSpecAsss();

	/**
	 * Intentionally private constructor, to disable instantiation. This
	 * class works as a singleton wrapper for the special ASN.1 module.
	 * */
	private SpecialASN1Module() {
		// Do nothing
	}

	/**
	 * Getter for the contained special module.
	 *
	 * @return the special module.
	 * */
	public static ASN1Module getSpecialModule() {
		return specialAssignmentsModule;
	}

	/**
	 * Creates the special assignments by parsing the strings as if they
	 * were coming from an internal file and creating a module around them.
	 *
	 * @return the module of the special assignments created.
	 */
	private static ASN1Module createSpecAsss() {
		if (null != specialAssignmentsModule) {
			return specialAssignmentsModule;
		}

		final ASN1Assignments parsedAssignments = new ASN1Assignments();
		ASN1Assignment actualAssignment;

		for (final String[] assignment : INTERNAL_ASSIGNMENTS) {
			final String assignmentCode = assignment[1];
			final Identifier assignmentId = new Identifier(Identifier_type.ID_ASN, assignment[0]);
			actualAssignment = SpecialASN1Module.parseSpecialInternalAssignment(assignmentCode, assignmentId);
			parsedAssignments.addAssignment(actualAssignment);
		}

		// null as a project might not be a good idea
		final Identifier internalModuleID = new Identifier(Identifier_type.ID_ASN, INTERNAL_MODULE);
		specialAssignmentsModule = new ASN1Module(internalModuleID, null, Tag_types.AUTOMATIC_TAGS, false);
		specialAssignmentsModule.setExports(new Exports(true));
		specialAssignmentsModule.setImports(new Imports());
		specialAssignmentsModule.setAssignments(parsedAssignments);
		specialAssignmentsModule.setLocation(NULL_Location.INSTANCE);
		specialAssignmentsModule.setScopeName(INTERNAL_MODULE);

		final CompilationTimeStamp timestamp = CompilationTimeStamp.getBaseTimestamp();
		final ModuleImportationChain referenceChain = new ModuleImportationChain(ModuleImportationChain.CIRCULARREFERENCE, false);
		specialAssignmentsModule.checkImports(timestamp, referenceChain, new ArrayList<Module>());

		specialAssignmentsModule.check(timestamp);

		return specialAssignmentsModule;
	}

	/** Used to preload the class, also loading the ASN.1 parsing framework. */
	public static void preLoad() {
		createSpecAsss();
	}

	/**
	 * Checks whether the provided module is the module that contains the
	 * special assignments.
	 *
	 * @param module
	 *                the module to check
	 * @return true if the provided module is the module of the special
	 *         assignments, otherwise false.
	 * */
	public static boolean isSpecAsss(final Module module) {
		return null != specialAssignmentsModule && specialAssignmentsModule.equals(module);
	}

	/**
	 * Parses the special internal assignments to build their semantic
	 * representation.
	 *
	 * @param inputCode
	 *                the code to parse.
	 * @param identifier
	 *                the identifier for the assignment to be created.
	 *
	 * @return the parsed assignment.
	 */
	public static ASN1Assignment parseSpecialInternalAssignment(final String inputCode, final Identifier identifier) {
		ASN1Assignment assignment = null;
		final StringReader reader = new StringReader(inputCode);
		final CharStream charStream = new UnbufferedCharStream(reader);
		final Asn1Lexer lexer = new Asn1Lexer(charStream);
		lexer.setTokenFactory(new TokenWithIndexAndSubTokensFactory(true));

		final ASN1Listener lexerListener = new ASN1Listener();
		lexer.removeErrorListeners(); // remove ConsoleErrorListener
		lexer.addErrorListener(lexerListener);
		final ModuleLevelTokenStreamTracker tracker = new ModuleLevelTokenStreamTracker(lexer);
		tracker.discard(Asn1Lexer.WS);
		tracker.discard(Asn1Lexer.MULTILINECOMMENT);
		tracker.discard(Asn1Lexer.SINGLELINECOMMENT);
		final Asn1Parser parser = new Asn1Parser(tracker);
		parser.setBuildParseTree(false);
		final ASN1Listener parserListener = new ASN1Listener(parser);
		parser.removeErrorListeners(); // remove ConsoleErrorListener
		parser.addErrorListener(parserListener);
		assignment = parser.pr_TITAN_special_Assignment(identifier).assignment;
		if (!parser.getErrorStorage().isEmpty()) {
			ErrorReporter.INTERNAL_ERROR(PARSINGFAILED);
			for (final SyntacticErrorStorage temp : parser.getErrorStorage()) {
				ErrorReporter.logError(temp.message);
			}
		}
		return assignment;
	}
}
