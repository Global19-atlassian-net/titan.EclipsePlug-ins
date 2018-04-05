/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.TypeOwner_type;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.NamedBridgeScope;
import org.eclipse.titan.designer.AST.NamingConventionHelper;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.attributes.AttributeSpecification;
import org.eclipse.titan.designer.AST.TTCN3.attributes.ExtensionAttribute;
import org.eclipse.titan.designer.AST.TTCN3.attributes.ExtensionAttribute.ExtensionAttribute_type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.PrototypeAttribute;
import org.eclipse.titan.designer.AST.TTCN3.attributes.Qualifiers;
import org.eclipse.titan.designer.AST.TTCN3.attributes.SingleWithAttribute;
import org.eclipse.titan.designer.AST.TTCN3.attributes.SingleWithAttribute.Attribute_Type;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.AST.TTCN3.types.Component_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.T3Doc;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3CodeSkeletons;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.extensionattributeparser.ExtensionAttributeAnalyzer;
import org.eclipse.titan.designer.parsers.ttcn3parser.IIdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.IdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.preferences.PreferenceConstants;

/**
 * The Def_Function class represents TTCN3 function definitions.
 *
 * @author Kristof Szabados
 * */
public final class Def_Function extends Definition implements IParameterisedAssignment {
	/**
	 * The encoding prototype. Used with the extension attributes. Also used
	 * by Def_ExtFunction
	 * */
	public enum EncodingPrototype_type {
		/** no prototype extension. */
		NONE("<no prototype>"),
		/** backtrack prototype. */
		BACKTRACK("backtrack"),
		/** conversion prototype. */
		CONVERT("convert"),
		/** fast prototype. */
		FAST("fast"),
		/** sliding window prototype */
		SLIDING("sliding");

		private String name;

		private EncodingPrototype_type(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	private static final String FULLNAMEPART1 = ".<runs_on_type>";
	private static final String FULLNAMEPART2 = ".<formal_parameter_list>";
	private static final String FULLNAMEPART3 = ".<type>";
	private static final String FULLNAMEPART4 = ".<statement_block>";
	public static final String PORTRETURNNOTALLOWED = "Functions can not return ports";

	private static final String DASHALLOWEDONLYFORTEMPLATES = "Using not used symbol (`-') as the default parameter"
			+ " is allowed only for modified templates";

	private static final String KIND = "function";

	public static String getKind() {
		return KIND;
	}

	private final Assignment_type assignmentType;
	private final FormalParameterList formalParList;
	private final Reference runsOnRef;
	private Component_Type runsOnType = null;
	private final Type returnType;
	private final boolean returnsTemplate;
	private final TemplateRestriction.Restriction_type templateRestriction;
	private final StatementBlock block;
	private EncodingPrototype_type prototype;
	private Type inputType;
	private Type outputType;

	// stores whether this function can be started or not
	private boolean isStartable;
	private NamedBridgeScope bridgeScope = null;

	public Def_Function(final Identifier identifier, final FormalParameterList formalParameters, final Reference runsOnRef,
			final Type returnType, final boolean returnsTemplate, final TemplateRestriction.Restriction_type templateRestriction,
			final StatementBlock block) {
		super(identifier);
		assignmentType = (returnType == null) ? Assignment_type.A_FUNCTION : (returnsTemplate ? Assignment_type.A_FUNCTION_RTEMP
				: Assignment_type.A_FUNCTION_RVAL);
		this.formalParList = formalParameters;
		if (formalParList != null) {
			formalParList.setMyDefinition(this);
			formalParList.setFullNameParent(this);
		}
		this.runsOnRef = runsOnRef;
		this.returnType = returnType;
		this.returnsTemplate = returnsTemplate;
		this.templateRestriction = templateRestriction;
		this.block = block;

		this.prototype = EncodingPrototype_type.NONE;
		inputType = null;
		outputType = null;

		if (block != null) {
			block.setMyDefinition(this);
			block.setFullNameParent(this);
		}
		if (runsOnRef != null) {
			runsOnRef.setFullNameParent(this);
		}
		if (returnType != null) {
			returnType.setOwnertype(TypeOwner_type.OT_FUNCTION_DEF, this);
			returnType.setFullNameParent(this);
		}
	}

	public boolean isStartable() {
		return isStartable;
	}

	@Override
	/** {@inheritDoc} */
	public Assignment_type getAssignmentType() {
		return assignmentType;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (runsOnRef == child) {
			return builder.append(FULLNAMEPART1);
		} else if (formalParList == child) {
			return builder.append(FULLNAMEPART2);
		} else if (returnType == child) {
			return builder.append(FULLNAMEPART3);
		} else if (block == child) {
			return builder.append(FULLNAMEPART4);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public FormalParameterList getFormalParameterList() {
		return formalParList;
	}

	public EncodingPrototype_type getPrototype() {
		return prototype;
	}

	public Type getInputType() {
		return inputType;
	}

	public Type getOutputType() {
		return outputType;
	}

	@Override
	/** {@inheritDoc} */
	public String getProposalKind() {
		return KIND;
	}

	@Override
	/** {@inheritDoc} */
	public String getAssignmentName() {
		return "function";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		if (returnType == null) {
			return "function.gif";
		}

		return "function_return.gif";
	}

	@Override
	/** {@inheritDoc} */
	public String getProposalDescription() {
		final StringBuilder nameBuilder = new StringBuilder(identifier.getDisplayName());
		nameBuilder.append('(');
		formalParList.getAsProposalDesriptionPart(nameBuilder);
		nameBuilder.append(')');
		return nameBuilder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		if (bridgeScope != null && bridgeScope.getParentScope() == scope) {
			return;
		}

		bridgeScope = new NamedBridgeScope();
		bridgeScope.setParentScope(scope);
		scope.addSubScope(getLocation(), bridgeScope);
		bridgeScope.setScopeMacroName(identifier.getDisplayName());

		super.setMyScope(bridgeScope);
		if (runsOnRef != null) {
			runsOnRef.setMyScope(bridgeScope);
		}
		formalParList.setMyScope(bridgeScope);
		if (returnType != null) {
			returnType.setMyScope(bridgeScope);
		}
		if (block != null) {
			block.setMyScope(formalParList);
		}
		if (block != null) {
			bridgeScope.addSubScope(block.getLocation(), block);
		}
		bridgeScope.addSubScope(formalParList.getLocation(), formalParList);
	}

	@Override
	/** {@inheritDoc} */
	public Type getType(final CompilationTimeStamp timestamp) {
		check(timestamp);

		return returnType;
	}

	public Component_Type getRunsOnType(final CompilationTimeStamp timestamp) {
		check(timestamp);

		return runsOnType;
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
		runsOnType = null;
		isStartable = false;

		if (runsOnRef != null) {
			runsOnType = runsOnRef.chkComponentypeReference(timestamp);
			if (runsOnType != null) {
				final Scope formalParlistPreviosScope = formalParList.getParentScope();
				if (formalParlistPreviosScope instanceof RunsOnScope
						&& ((RunsOnScope) formalParlistPreviosScope).getParentScope() == myScope) {
					((RunsOnScope) formalParlistPreviosScope).setComponentType(runsOnType);
				} else {
					final Scope tempScope = new RunsOnScope(runsOnType, myScope);
					formalParList.setMyScope(tempScope);
				}
			}
		}

		boolean canSkip = false;
		if (myScope != null) {
			final Module module = myScope.getModuleScope();
			if (module != null) {
				if (module.getSkippedFromSemanticChecking()) {
					canSkip = true;
				}
			}
		}

		if(!canSkip) {
			formalParList.reset();
		}

		formalParList.check(timestamp, getAssignmentType());

		if (returnType != null) {
			returnType.check(timestamp);
			final IType returnedType = returnType.getTypeRefdLast(timestamp);
			if (Type_type.TYPE_PORT.equals(returnedType.getTypetype()) && returnType.getLocation() != null) {
				returnType.getLocation().reportSemanticError(PORTRETURNNOTALLOWED);
			}
		}

		if (formalParList.hasNotusedDefaultValue()) {
			formalParList.getLocation().reportSemanticError(DASHALLOWEDONLYFORTEMPLATES);
			return;
		}

		// decision of startability
		isStartable = runsOnRef != null;
		isStartable &= formalParList.getStartability();
		if (isStartable && returnType != null && returnType.isComponentInternal(timestamp)) {
			isStartable = false;
		}

		if(canSkip) {
			return;
		}

		NamingConventionHelper.checkConvention(PreferenceConstants.REPORTNAMINGCONVENTION_FUNCTION, identifier, this);
		NamingConventionHelper.checkNameContents(identifier, getMyScope().getModuleScope().getIdentifier(), getDescription());

		if (block != null) {
			block.check(timestamp);

			if (returnType != null) {
				// check the presence of return statements
				switch (block.hasReturn(timestamp)) {
				case RS_NO:
					identifier.getLocation().reportSemanticError(
							"The function has a return type, but it does not have any return statement");
					break;
				case RS_MAYBE:
					identifier.getLocation()
					.reportSemanticError(
							"The function has return type, but control might leave it without reaching a return statement");
					break;
				default:
					break;
				}
			}

			block.postCheck();
			block.setCodeSection(CodeSectionType.CS_INLINE);
		}

		prototype = EncodingPrototype_type.NONE;
		inputType = null;
		outputType = null;

		if (withAttributesPath != null) {
			withAttributesPath.checkGlobalAttributes(timestamp, false);
			withAttributesPath.checkAttributes(timestamp);
			analyzeExtensionAttributes(timestamp);
			checkPrototype(timestamp);
		}
	}

	/**
	 * Checks and returns whether the function is startable. Reports the
	 * appropriate error messages.
	 *
	 * @param timestamp
	 *                the timestamp of the actual build cycle.
	 * @param errorLocation
	 *                the location to report the error to, if needed.
	 *
	 * @return true if startable, false otherwise
	 * */
	public boolean checkStartable(final CompilationTimeStamp timestamp, final Location errorLocation) {
		check(timestamp);

		if (runsOnRef == null) {
			errorLocation.reportSemanticError(MessageFormat.format(
					"Function `{0}'' cannot be started on parallel test component because it does not have a `runs on'' clause",
					getFullName()));
		}

		formalParList.checkStartability(timestamp, "Function", this, errorLocation);

		if (returnType != null && returnType.isComponentInternal(timestamp)) {
			final Set<IType> typeSet = new HashSet<IType>();
			final String errorMessage = "the return type or embedded in the return type of function `" + getFullName()
					+ "' if it is started on a parallel test component";
			returnType.checkComponentInternal(timestamp, typeSet, errorMessage);
		}

		if (isStartable) {
			return true;
		}

		return false;
	}

	/**
	 * Convert and check the encoding attributes applied to this function.
	 *
	 * @param timestamp
	 *                the timestamp of the actual build cycle.
	 * */
	public void analyzeExtensionAttributes(final CompilationTimeStamp timestamp) {
		final List<SingleWithAttribute> realAttributes = withAttributesPath.getRealAttributes(timestamp);

		SingleWithAttribute attribute;
		List<AttributeSpecification> specifications = null;
		for (int i = 0, size = realAttributes.size(); i < size; i++) {
			attribute = realAttributes.get(i);
			if (Attribute_Type.Extension_Attribute.equals(attribute.getAttributeType())) {
				final Qualifiers qualifiers = attribute.getQualifiers();
				if (qualifiers == null || qualifiers.getNofQualifiers() == 0) {
					if (specifications == null) {
						specifications = new ArrayList<AttributeSpecification>();
					}
					specifications.add(attribute.getAttributeSpecification());
				}
			}
		}

		if (specifications == null) {
			return;
		}

		final List<ExtensionAttribute> attributes = new ArrayList<ExtensionAttribute>();
		for (int i = 0, size = specifications.size(); i < size; i++) {
			final AttributeSpecification specification = specifications.get(i);
			final ExtensionAttributeAnalyzer analyzer = new ExtensionAttributeAnalyzer();
			analyzer.parse(specification);
			final List<ExtensionAttribute> temp = analyzer.getAttributes();
			if (temp != null) {
				attributes.addAll(temp);
			}
		}

		for (int i = 0, size = attributes.size(); i < size; i++) {
			final ExtensionAttribute extensionAttribute = attributes.get(i);

			if (ExtensionAttribute_type.PROTOTYPE.equals(extensionAttribute.getAttributeType())) {
				final PrototypeAttribute realAttribute = (PrototypeAttribute) extensionAttribute;
				if (EncodingPrototype_type.NONE.equals(prototype)) {
					prototype = realAttribute.getPrototypeType();
				} else {
					realAttribute.getLocation().reportSingularSemanticError("Duplicate attribute `prototype'");
				}
			}
		}
	}

	/**
	 * Checks the prototype attribute set for this function definition.
	 *
	 * @param timestamp
	 *                the timestamp of the actual build cycle.
	 * */
	public void checkPrototype(final CompilationTimeStamp timestamp) {
		if (EncodingPrototype_type.NONE.equals(prototype)) {
			return;
		}

		// checking formal parameter list
		if (EncodingPrototype_type.CONVERT.equals(prototype)) {
			if (formalParList.getNofParameters() == 1) {
				final FormalParameter parameter = formalParList.getParameterByIndex(0);
				final Assignment_type assignmentType = parameter.getRealAssignmentType();
				if (Assignment_type.A_PAR_VAL_IN.semanticallyEquals(assignmentType)) {
					inputType = parameter.getType(timestamp);
				} else {
					parameter.getLocation()
					.reportSemanticError(
							MessageFormat.format(
									"The parameter must be an `in'' value parameter for attribute `prototype({0})'' instead of {1}",
									prototype.getName(), parameter.getAssignmentName()));
				}
			} else {
				formalParList.getLocation()
				.reportSemanticError(
						MessageFormat.format(
								"The function must have one parameter instead of {0} for attribute `prototype({1})''",
								formalParList.getNofParameters(), prototype.getName()));
			}
		} else if (formalParList.getNofParameters() == 2) {
			final FormalParameter firstParameter = formalParList.getParameterByIndex(0);
			if (EncodingPrototype_type.SLIDING.equals(prototype)) {
				if (Assignment_type.A_PAR_VAL_INOUT.semanticallyEquals(firstParameter.getRealAssignmentType())) {
					final Type firstParameterType = firstParameter.getType(timestamp);
					final IType last = firstParameterType.getTypeRefdLast(timestamp);
					if (last.getIsErroneous(timestamp)) {
						inputType = firstParameterType;
					} else {
						switch (last.getTypetypeTtcn3()) {
						case TYPE_OCTETSTRING:
						case TYPE_CHARSTRING:
							inputType = firstParameterType;
							break;
						default:
							firstParameter.getLocation()
							.reportSemanticError(
									MessageFormat.format(
											"The type of the first parameter must be `octetstring'' or `charstring'' for attribute `prototype({0})''",
											prototype.getName()));
							break;
						}
					}
				} else {
					firstParameter.getLocation()
					.reportSemanticError(
							MessageFormat.format(
									"The first parameter must be an `inout'' value parameter for attribute `prototype({0})'' instead of {1}",
									prototype.getName(), firstParameter.getAssignmentName()));
				}
			} else {
				final Assignment_type assignmentType = firstParameter.getRealAssignmentType();
				if (Assignment_type.A_PAR_VAL_IN.semanticallyEquals(assignmentType) || Assignment_type.A_PAR_VAL.semanticallyEquals(assignmentType)) {
					inputType = firstParameter.getType(timestamp);
				} else {
					firstParameter.getLocation()
					.reportSemanticError(
							MessageFormat.format(
									"The first parameter must be an `in'' value parameter for attribute `prototype({0})'' instead of {1}",
									prototype.getName(), firstParameter.getAssignmentName()));
				}
			}

			final FormalParameter secondParameter = formalParList.getParameterByIndex(1);
			if (Assignment_type.A_PAR_VAL_OUT.semanticallyEquals(secondParameter.getRealAssignmentType())) {
				outputType = secondParameter.getType(timestamp);
			} else {
				secondParameter.getLocation()
				.reportSemanticError(
						MessageFormat.format(
								"The second parameter must be an `out'' value parameter for attribute `prototype({0})'' instead of {1}",
								prototype.getName(), secondParameter.getAssignmentName()));
			}
		} else {
			formalParList.getLocation().reportSemanticError(
					MessageFormat.format("The function must have two parameters for attribute `prototype({0})'' instead of {1}",
							prototype.getName(), formalParList.getNofParameters()));
		}

		// checking the return type
		if (EncodingPrototype_type.FAST.equals(prototype)) {
			if (returnType != null) {
				returnType.getLocation().reportSemanticError(
						MessageFormat.format("The function cannot have return type for attribute `prototype({0})''",
								prototype.getName()));
			}
		} else {
			if (returnType == null) {
				location.reportSemanticError(MessageFormat.format(
						"The function must have a return type for attribute `prototype({0})''", prototype.getName()));
			} else {
				if (Assignment_type.A_FUNCTION_RTEMP.semanticallyEquals(assignmentType)) {
					returnType.getLocation()
					.reportSemanticError(
							MessageFormat.format(
									"The function must return a value instead of a template for attribute `prototype({0})''",
									prototype.getName()));
				}

				if (EncodingPrototype_type.CONVERT.equals(prototype)) {
					outputType = returnType;
				} else {
					final IType last = returnType.getTypeRefdLast(timestamp);

					if (!last.getIsErroneous(timestamp) && !Type_type.TYPE_INTEGER.equals(last.getTypetypeTtcn3())) {
						returnType.getLocation()
						.reportSemanticError(
								MessageFormat.format(
										"The return type of the function must be `integer'' instead of `{0}'' for attribute `prototype({1})''",
										returnType.getTypename(), prototype.getName()));
					}
				}
			}
		}

		// checking the runs on clause
		if (runsOnType != null && runsOnRef != null) {
			runsOnRef.getLocation().reportSemanticError(
					MessageFormat.format("The function cannot have `runs on'' clause for attribute `prototype({0})''",
							prototype.getName()));
		}
	}

	@Override
	/** {@inheritDoc} */
	public void postCheck() {
		if (myScope != null) {
			final Module module = myScope.getModuleScope();
			if (module != null) {
				if (module.getSkippedFromSemanticChecking()) {
					return;
				}
			}
		}

		super.postCheck();
		postCheckPrivateness();

		formalParList.postCheck();
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int i) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= i || Subreference_type.arraySubReference.equals(subrefs.get(i).getReferenceType())) {
			return;
		}

		if (subrefs.size() == i + 1 && identifier.getName().toLowerCase().startsWith(subrefs.get(i).getId().getName().toLowerCase())) {
			final StringBuilder patternBuilder = new StringBuilder(identifier.getDisplayName());
			patternBuilder.append('(');
			formalParList.getAsProposalPart(patternBuilder);
			patternBuilder.append(')');
			propCollector.addTemplateProposal(identifier.getDisplayName(),
					new Template(getProposalDescription(), "", propCollector.getContextIdentifier(), patternBuilder.toString(),
							false), TTCN3CodeSkeletons.SKELETON_IMAGE);
			super.addProposal(propCollector, i);
		} else if (subrefs.size() > i + 1 && returnType != null
				&& Subreference_type.parameterisedSubReference.equals(subrefs.get(i).getReferenceType())
				&& identifier.getName().equals(subrefs.get(i).getId().getName())) {
			// perfect match
			returnType.addProposal(propCollector, i + 1);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() <= i || Subreference_type.arraySubReference.equals(subrefs.get(i).getReferenceType())) {
			return;
		}

		if (identifier.getName().equals(subrefs.get(i).getId().getName())) {
			if (subrefs.size() > i + 1 && returnType != null) {
				returnType.addDeclaration(declarationCollector, i + 1);
			} else {
				declarationCollector.addDeclaration(this);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineText() {
		if (lastTimeChecked == null) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		final StringBuilder text = new StringBuilder(identifier.getDisplayName());
		if (formalParList == null) {
			return text.toString();
		}

		text.append('(');
		for (int i = 0; i < formalParList.getNofParameters(); i++) {
			if (i != 0) {
				text.append(", ");
			}
			final FormalParameter parameter = formalParList.getParameterByIndex(i);
			if (Assignment_type.A_PAR_TIMER.semanticallyEquals(parameter.getRealAssignmentType())) {
				text.append("timer");
			} else {
				final IType type = parameter.getType(lastTimeChecked);
				if (type == null) {
					text.append("Unknown type");
				} else {
					text.append(type.getTypename());
				}
			}
		}
		text.append(')');
		return text.toString();
	}

	@Override
	/** {@inheritDoc} */
	public TemplateRestriction.Restriction_type getTemplateRestriction() {
		return templateRestriction;
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
					removeBridge();
					throw new ReParseException(result);
				}
			}

			if (formalParList != null) {
				if (enveloped) {
					formalParList.updateSyntax(reparser, false);
					reparser.updateLocation(formalParList.getLocation());
				} else if (reparser.envelopsDamage(formalParList.getLocation())) {
					try {
						formalParList.updateSyntax(reparser, true);
						enveloped = true;
						reparser.updateLocation(formalParList.getLocation());
					} catch (ReParseException e) {
						removeBridge();
						throw e;
					}
				}
			}

			if (runsOnRef != null) {
				if (enveloped) {
					runsOnRef.updateSyntax(reparser, false);
					reparser.updateLocation(runsOnRef.getLocation());
				} else if (reparser.envelopsDamage(runsOnRef.getLocation())) {
					try {
						runsOnRef.updateSyntax(reparser, true);
						enveloped = true;
						reparser.updateLocation(runsOnRef.getLocation());
					} catch (ReParseException e) {
						removeBridge();
						throw e;
					}
				}
			}

			if (returnType != null) {
				if (enveloped) {
					returnType.updateSyntax(reparser, false);
					reparser.updateLocation(returnType.getLocation());
				} else if (reparser.envelopsDamage(returnType.getLocation())) {
					try {
						returnType.updateSyntax(reparser, true);
						enveloped = true;
						reparser.updateLocation(returnType.getLocation());
					} catch (ReParseException e) {
						removeBridge();
						throw e;
					}
				}
			}

			if (block != null) {
				if (enveloped) {
					block.updateSyntax(reparser, false);
					reparser.updateLocation(block.getLocation());
				} else if (reparser.envelopsDamage(block.getLocation())) {
					try {
						block.updateSyntax(reparser, true);
						enveloped = true;
						reparser.updateLocation(block.getLocation());
					} catch (ReParseException e) {
						removeBridge();
						throw e;
					}
				}
			}

			if (withAttributesPath != null) {
				if (enveloped) {
					withAttributesPath.updateSyntax(reparser, false);
					reparser.updateLocation(withAttributesPath.getLocation());
				} else if (reparser.envelopsDamage(withAttributesPath.getLocation())) {
					try {
						withAttributesPath.updateSyntax(reparser, true);
						enveloped = true;
						reparser.updateLocation(withAttributesPath.getLocation());
					} catch (ReParseException e) {
						removeBridge();
						throw e;
					}
				}
			}

			if (!enveloped) {
				removeBridge();
				throw new ReParseException();
			}

			return;
		}

		reparser.updateLocation(identifier.getLocation());

		if (formalParList != null) {
			formalParList.updateSyntax(reparser, false);
			reparser.updateLocation(formalParList.getLocation());
		}

		if (runsOnRef != null) {
			runsOnRef.updateSyntax(reparser, false);
			reparser.updateLocation(runsOnRef.getLocation());
		}

		if (returnType != null) {
			returnType.updateSyntax(reparser, false);
			reparser.updateLocation(returnType.getLocation());
		}

		if (block != null) {
			block.updateSyntax(reparser, false);
			reparser.updateLocation(block.getLocation());
		}

		if (withAttributesPath != null) {
			withAttributesPath.updateSyntax(reparser, false);
			reparser.updateLocation(withAttributesPath.getLocation());
		}
	}

	/**
	 * Removes the name bridging scope.
	 * */
	private void removeBridge() {
		if (bridgeScope != null) {
			bridgeScope.remove();
			bridgeScope = null;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (formalParList != null) {
			formalParList.findReferences(referenceFinder, foundIdentifiers);
		}
		if (returnType != null) {
			returnType.findReferences(referenceFinder, foundIdentifiers);
		}
		if (runsOnRef != null) {
			runsOnRef.findReferences(referenceFinder, foundIdentifiers);
		}
		if (block != null) {
			block.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (formalParList != null && !formalParList.accept(v)) {
			return false;
		}
		if (returnType != null && !returnType.accept(v)) {
			return false;
		}
		if (runsOnRef != null && !runsOnRef.accept(v)) {
			return false;
		}
		if (block != null && !block.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final boolean cleanUp ) {
		final String genName = getGenName();
		if (formalParList != null) {
			formalParList.setGenName(genName);
		}

		//TODO add support for functions with port clause
		final StringBuilder sb = aData.getSrc();
		final StringBuilder source = new StringBuilder();
		if(VisibilityModifier.Private.equals(getVisibilityModifier())) {
			source.append( "private" );
		} else {
			source.append( "public" );
		}
		source.append( " static final " );

		// return value
		switch (assignmentType) {
		case A_FUNCTION:
			source.append( "void" );
			break;
		case A_FUNCTION_RVAL:
			source.append( returnType.getGenNameValue( aData, source, getMyScope() ) );
			break;
		case A_FUNCTION_RTEMP:
			source.append( returnType.getGenNameTemplate( aData, source, getMyScope() ) );
			break;
		default:
			ErrorReporter.INTERNAL_ERROR("Code generator reached erroneous definition `" + getFullName() + "''");
		}

		source.append( ' ' );

		// function name
		source.append( genName );

		// arguments
		source.append( '(' );
		if ( formalParList != null ) {
			formalParList.generateCode( aData, source );
		}
		source.append( ") {\n" );
		block.generateCode(aData, source);
		source.append( "}\n" );

		if (isStartable) {
			aData.addBuiltinTypeImport("TitanComponent");

			source.append(MessageFormat.format("public static final void start_{0}(final TitanComponent component_reference", genName));
			if (formalParList != null && formalParList.getNofParameters() > 0) {
				source.append(", ");
				formalParList.generateCode(aData, source);
			}
			source.append(") {\n");
			source.append("TtcnLogger.begin_event(Severity.PARALLEL_PTC);\n");
			source.append(MessageFormat.format("TtcnLogger.log_event_str(\"Starting function {0}(\");\n", identifier.getDisplayName()));
			if (formalParList != null) {
				for (int i = 0; i < formalParList.getNofParameters(); i++) {
					if (i > 0) {
						source.append("TtcnLogger.log_event_str(\", \");\n");
					}
					source.append(MessageFormat.format("{0}.log();\n", formalParList.getParameterByIndex(i).getGenName()));
				}
			}
			source.append("TtcnLogger.log_event_str(\") on component \");\n");
			source.append("component_reference.log();\n");
			source.append("TtcnLogger.log_char('.');\n");
			source.append("TtcnLogger.end_event();\n");
			//FIXME implement once encoding is ready
			source.append("throw new TtcnError(\"Starting a function on a remote component is not yet implemented!\");\n");
			source.append("}\n");

			//FIXME add to start_ptc_function
		}

		sb.append(source);
	}
}
