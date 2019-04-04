/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.TypeOwner_type;
import org.eclipse.titan.designer.AST.IType.ValueCheckingOptions;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NamingConventionHelper;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.types.Function_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.T3Doc;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.IIdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.IdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;
import org.eclipse.titan.designer.preferences.PreferenceConstants;

/**
 * The Def_ModulePar class represents TTCN3 module parameter definitions.
 *
 * @author Kristof Szabados
 * */
public final class Def_ModulePar extends Definition {
	private static final String FULLNAMEPART1 = ".<type>";
	private static final String FULLNAMEPART2 = ".<default_value>";
	public static final String PORTNOTALLOWED = "Module parameter can not be of port type `{0}''";
	public static final String SIGNATURENOTALLOWED = "Module parameter can not be of signature type `{0}''";
	public static final String RUNSONSELF_NOT_ALLOWED = "Module parameter can not be of function reference type `{0}'' which has runs on self clause";

	private static final String KIND = " module parameter";

	public static String getKind() {
		return KIND;
	}

	private final Type type;
	private final Value defaultValue;

	//store for code generation if the last semantic checking found implicit omit for this module parameter.
	private boolean hasImplicitOmit;

	public Def_ModulePar(final Identifier identifier, final Type type, final Value defaultValue) {
		super(identifier);
		this.type = type;
		this.defaultValue = defaultValue;

		if (type != null) {
			type.setOwnertype(TypeOwner_type.OT_MODPAR_DEF, this);
			type.setFullNameParent(this);
		}
		if (defaultValue != null) {
			defaultValue.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment_type getAssignmentType() {
		return Assignment_type.A_MODULEPAR;
	}

	public Value getDefaultValue() {
		return defaultValue;
	}


	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (type == child) {
			return builder.append(FULLNAMEPART1);
		} else if (defaultValue == child) {
			return builder.append(FULLNAMEPART2);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (type != null) {
			type.setMyScope(scope);
		}
		if (defaultValue != null) {
			defaultValue.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getAssignmentName() {
		return "module parameter";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "module_parameter.gif";
	}

	@Override
	/** {@inheritDoc} */
	public int category() {
		int result = super.category();
		if (type != null) {
			result += type.category();
		}
		return result;
	}

	@Override
	/** {@inheritDoc} */
	public Type getType(final CompilationTimeStamp timestamp) {
		check(timestamp);

		return type;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		check(timestamp, null);
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final IReferenceChain refChain) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}
		lastTimeChecked = timestamp;
		T3Doc.check(this.getCommentLocation(), KIND);

		isUsed = false;
		hasImplicitOmit = false;

		NamingConventionHelper.checkConvention(PreferenceConstants.REPORTNAMINGCONVENTION_MODULEPAR, identifier, this);
		NamingConventionHelper.checkNameContents(identifier, getMyScope().getModuleScope().getIdentifier(), getDescription());

		if (withAttributesPath != null) {
			withAttributesPath.checkGlobalAttributes(timestamp, false);
			withAttributesPath.checkAttributes(timestamp);
		}

		if (type == null) {
			return;
		}

		type.setGenName("_T_", getGenName());
		type.check(timestamp);

		final IType lastType = type.getTypeRefdLast(timestamp);
		switch (lastType.getTypetype()) {
		case TYPE_PORT:
			location.reportSemanticError(MessageFormat.format(PORTNOTALLOWED, lastType.getFullName()));
			break;
		case TYPE_SIGNATURE:
			location.reportSemanticError(MessageFormat.format(SIGNATURENOTALLOWED, lastType.getFullName()));
			break;
		case TYPE_FUNCTION:
		case TYPE_ALTSTEP:
		case TYPE_TESTCASE:
			if (((Function_Type) lastType).isRunsOnSelf()) {
				location.reportSemanticError(MessageFormat.format(RUNSONSELF_NOT_ALLOWED, lastType.getFullName()));
			}
			break;
		default:
			break;
		}

		hasImplicitOmit = hasImplicitOmitAttribute(timestamp);

		if (defaultValue != null) {
			defaultValue.setMyGovernor(type);
			final IValue temporalValue = type.checkThisValueRef(timestamp, defaultValue);
			type.checkThisValue(timestamp, temporalValue, null, new ValueCheckingOptions(Expected_Value_type.EXPECTED_CONSTANT, true, false,
					true, hasImplicitOmit, false));
			defaultValue.setCodeSection(CodeSectionType.CS_PRE_INIT);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void postCheck() {
		super.postCheck();
		postCheckPrivateness();
	}

	@Override
	/** {@inheritDoc} */
	public String getProposalKind() {
		final StringBuilder builder = new StringBuilder();
		if (type != null) {
			type.getProposalDescription(builder);
		}
		builder.append(KIND);
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int index) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= index) {
			return;
		}

		if (subrefs.size() == index + 1 && identifier.getName().toLowerCase().startsWith(subrefs.get(index).getId().getName().toLowerCase())) {
			super.addProposal(propCollector, index);
		} else if (subrefs.size() > index + 1 && type != null && identifier.getName().equals(subrefs.get(index).getId().getName())) {
			// perfect match
			type.addProposal(propCollector, index + 1);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int index) {
		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() > index && identifier.getName().equals(subrefs.get(index).getId().getName())) {
			if (subrefs.size() > index + 1 && type != null) {
				type.addDeclaration(declarationCollector, index + 1);
			} else if (subrefs.size() == index + 1 && Subreference_type.fieldSubReference.equals(subrefs.get(index).getReferenceType())) {
				declarationCollector.addDeclaration(this);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineText() {
		final StringBuilder text = new StringBuilder(getIdentifier().getDisplayName());
		text.append(" : ");
		text.append(type.getTypename());
		return text.toString();
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		final List<Integer> result = super.getPossibleExtensionStarterTokens();

		if (defaultValue == null) {
			result.add(Ttcn3Lexer.ASSIGNMENTCHAR);
		}

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			lastTimeChecked = null;
			boolean enveloped = false;

			final Location temporalIdentifier = identifier.getLocation();
			if (reparser.envelopsDamage(temporalIdentifier) || reparser.isExtending(temporalIdentifier)) {
				reparser.extendDamagedRegion(temporalIdentifier);
				final IIdentifierReparser r = new IdentifierReparser(reparser);
				final int result = r.parseAndSetNameChanged();
				identifier = r.getIdentifier();
				// damage handled
				if (result == 0 && identifier != null) {
					enveloped = true;
				} else {
					throw new ReParseException(result);
				}
			}

			if (type != null) {
				if (enveloped) {
					type.updateSyntax(reparser, false);
					reparser.updateLocation(type.getLocation());
				} else if (reparser.envelopsDamage(type.getLocation())) {
					type.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(type.getLocation());
				}
			}

			if (defaultValue != null) {
				if (enveloped) {
					defaultValue.updateSyntax(reparser, false);
					reparser.updateLocation(defaultValue.getLocation());
				} else if (reparser.envelopsDamage(defaultValue.getLocation())) {
					defaultValue.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(defaultValue.getLocation());
				}
			}

			if (withAttributesPath != null) {
				if (enveloped) {
					withAttributesPath.updateSyntax(reparser, false);
					reparser.updateLocation(withAttributesPath.getLocation());
				} else if (reparser.envelopsDamage(withAttributesPath.getLocation())) {
					withAttributesPath.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(withAttributesPath.getLocation());
				}
			}

			if (!enveloped) {
				throw new ReParseException();
			}

			return;
		}

		reparser.updateLocation(identifier.getLocation());

		if (type != null) {
			type.updateSyntax(reparser, false);
			reparser.updateLocation(type.getLocation());
		}

		if (defaultValue != null) {
			defaultValue.updateSyntax(reparser, false);
			reparser.updateLocation(defaultValue.getLocation());
		}

		if (withAttributesPath != null) {
			withAttributesPath.updateSyntax(reparser, false);
			reparser.updateLocation(withAttributesPath.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (type != null) {
			type.findReferences(referenceFinder, foundIdentifiers);
		}
		if (defaultValue != null) {
			defaultValue.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (type != null && !type.accept(v)) {
			return false;
		}
		if (defaultValue != null && !defaultValue.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final boolean cleanUp ) {
		final String genName = getGenName();

		if (defaultValue != null) {
			//defaultValue.setGenNamePrefix("modulepar_");//currently does not need the prefix
			defaultValue.setGenNameRecursive(genName);
		}

		aData.addBuiltinTypeImport("Param_Types");

		final StringBuilder globalVariable = new StringBuilder();
		if ( !isLocal() ) {
			if(VisibilityModifier.Private.equals(getVisibilityModifier())) {
				globalVariable.append( "\tprivate" );
			} else {
				globalVariable.append( "\tpublic" );
			}
			globalVariable.append( " static " );
		}
		globalVariable.append( "final " );
		final String typeGeneratedName = type.getGenNameValue( aData, globalVariable );
		globalVariable.append( typeGeneratedName );
		globalVariable.append( ' ' );
		globalVariable.append( genName );
		globalVariable.append( " = new " );
		globalVariable.append( typeGeneratedName );
		globalVariable.append( "();\n" );
		if ( defaultValue != null ) {
			getLocation().update_location_object(aData, aData.getPreInit());
			defaultValue.generateCodeInit( aData, aData.getPreInit(), genName );
		}

		aData.addGlobalVariable(typeGeneratedName, globalVariable.toString());

		if (hasImplicitOmit) {
			aData.getPostInit().append(MessageFormat.format("{0}.set_implicit_omit();\n", genName));
		}

		final StringBuilder moduleParamaterSetting = aData.getSetModuleParameters();
		moduleParamaterSetting.append(MessageFormat.format("if(par_name.equals(\"{0}\")) '{'\n", identifier.getDisplayName()));
		moduleParamaterSetting.append(MessageFormat.format("{0}.set_param(param);\n", genName));
		moduleParamaterSetting.append("return true;\n");
		moduleParamaterSetting.append("} else ");

		final StringBuilder listModulePars = aData.getListModulePars();
		listModulePars.append(MessageFormat.format("System.out.println(\"{0}.{1}\");\n", getMyScope().getModuleScope().getIdentifier().getDisplayName(), identifier.getDisplayName()));
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeString(final JavaGenData aData, final StringBuilder source) {
		ErrorReporter.INTERNAL_ERROR("Code generator reached erroneous definition `" + getFullName() + "''");
		aData.getSrc().append("FATAL_ERROR encountered while processing `" + getFullName() + "''\n");
	}
}
