/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.IType.TypeOwner_type;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NamedBridgeScope;
import org.eclipse.titan.designer.AST.NamingConventionHelper;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.AttributeSpecification;
import org.eclipse.titan.designer.AST.TTCN3.attributes.ExtensionAttribute;
import org.eclipse.titan.designer.AST.TTCN3.attributes.MultipleWithAttributes;
import org.eclipse.titan.designer.AST.TTCN3.attributes.Qualifier;
import org.eclipse.titan.designer.AST.TTCN3.attributes.Qualifiers;
import org.eclipse.titan.designer.AST.TTCN3.attributes.SingleWithAttribute;
import org.eclipse.titan.designer.AST.TTCN3.attributes.SingleWithAttribute.Attribute_Type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.WithAttributesPath;
import org.eclipse.titan.designer.AST.TTCN3.types.Address_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Altstep_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Component_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Function_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Testcase_Type;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.T3Doc;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.extensionattributeparser.ExtensionAttributeAnalyzer;
import org.eclipse.titan.designer.parsers.ttcn3parser.IIdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.IdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;
import org.eclipse.titan.designer.preferences.PreferenceConstants;

/**
 * The Def_Type class represents TTCN3 type definitions.
 *
 * @author Kristof Szabados
 * */
public final class Def_Type extends Definition {
	private final Type type;

	private NamedBridgeScope bridgeScope = null;

	/**
	 * Helper for the code generator. Indicates if the name of this type
	 * would collide with an other type's name in the same module.
	 * */
	private boolean hasSimilarName = false;

	public Def_Type(final Identifier identifier, final Type type) {
		super(identifier);
		this.type = type;

		if (type != null) {
			type.setFullNameParent(this);
			type.setOwnertype(TypeOwner_type.OT_TYPE_DEF, this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment_type getAssignmentType() {
		return Assignment_type.A_TYPE;
	}

	@Override
	/** {@inheritDoc} */
	public String getAssignmentName() {
		return "type";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		if (type != null) {
			return type.getOutlineIcon();
		}

		return "type.gif";
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

	/**
	 * Indicates for the code generation if the name of this type would
	 * collide with an other type's name in the same module.
	 *
	 * @param status
	 *                {@code true} to indicate collision, {@code false}
	 *                otherwise.
	 * */
	public final void setHasSimilarName(final boolean status) {
		hasSimilarName = status;
	}

	/**
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 *
	 * @return the type defined by this type definition
	 * */
	@Override
	public Type getType(final CompilationTimeStamp timestamp) {
		check(timestamp);

		return type;
	}

	@Override
	/** {@inheritDoc} */
	public Type getSetting(final CompilationTimeStamp timestamp) {
		check(timestamp);

		return type;
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
		if (type != null) {
			type.setMyScope(bridgeScope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setWithAttributes(final MultipleWithAttributes attributes) {
		if (type == null) {
			return;
		}

		if (withAttributesPath == null) {
			withAttributesPath = new WithAttributesPath();
			type.setAttributeParentPath(withAttributesPath);
		}

		type.setWithAttributes(attributes);
	}

	@Override
	/** {@inheritDoc} */
	public void setAttributeParentPath(final WithAttributesPath parent) {
		super.setAttributeParentPath(parent);
		if (type == null) {
			return;
		}

		type.setAttributeParentPath(getAttributePath());
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
		isUsed = false;

		if (!"ADDRESS".equals(identifier.getTtcnName()) && !"anytype".equals(identifier.getTtcnName())) {
			NamingConventionHelper.checkConvention(PreferenceConstants.REPORTNAMINGCONVENTION_TYPE, identifier, this);
			NamingConventionHelper.checkNameContents(identifier, getMyScope().getModuleScope().getIdentifier(), getDescription());
		}

		if (type == null) {
			return;
		}

		T3Doc.check(this.getCommentLocation(), type.getTypetypeTtcn3().toString());

		type.setGenName(getGenName() + (hasSimilarName ? "_at_offset" + getLocation().getOffset() : ""));
		if (Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
			((Component_Type)type).getComponentBody().setGenName(getGenName() + "_component_");
		}
		type.check(timestamp);
		type.checkConstructorName(identifier.getName());
		if ("ADDRESS".equals(identifier.getTtcnName())) {
			Address_Type.checkAddress(timestamp, type);
		}

		final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		type.checkRecursions(timestamp, chain);
		chain.release();

		if (withAttributesPath != null) {
			// type definitions don't have their own attributes,
			// but this is still valid as a fallback.
			withAttributesPath.checkGlobalAttributes(timestamp, false);
			withAttributesPath.checkAttributes(timestamp, type.getTypetype());
			hasImplicitOmitAttribute(timestamp);
			analyzeExtensionAttributes(timestamp, withAttributesPath);
		}

		switch (type.getTypetype()) {
		case TYPE_FUNCTION:
			((Function_Type) type).getFormalParameters().setMyDefinition(this);
			break;
		case TYPE_ALTSTEP:
			((Altstep_Type) type).getFormalParameters().setMyDefinition(this);
			break;
		case TYPE_TESTCASE:
			((Testcase_Type) type).getFormalParameters().setMyDefinition(this);
			break;
		default:
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void postCheck() {
		super.postCheck();
		postCheckPrivateness();
	}

	/**
	 * Convert and check the encoding attributes applied to this function.
	 *
	 * @param timestamp
	 *                the timestamp of the actual build cycle.
	 * */
	public void analyzeExtensionAttributes(final CompilationTimeStamp timestamp, final WithAttributesPath withAttributesPath) {
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
				} else {
					for (int j = 0, size2 = qualifiers.getNofQualifiers(); j < size2; j++) {
						final Qualifier tempQualifier = qualifiers.getQualifierByIndex(i);
						final ISubReference tempSubReference = tempQualifier.getSubReferenceByIndex(0);
						if (tempSubReference.getReferenceType() == Subreference_type.arraySubReference) {
							tempQualifier.getLocation().reportSemanticError(Qualifier.INVALID_INDEX_QUALIFIER);
						} else {
							tempQualifier.getLocation().reportSemanticError(
									MessageFormat.format(Qualifier.INVALID_FIELD_QUALIFIER, tempSubReference
											.getId().getDisplayName()));
						}
					}
				}
			}
		}

		if (specifications == null) {
			return;
		}

		final List<ExtensionAttribute> attributes = new ArrayList<ExtensionAttribute>();
		for (int i = 0; i < specifications.size(); i++) {
			final AttributeSpecification specification = specifications.get(i);
			final ExtensionAttributeAnalyzer analyzer = new ExtensionAttributeAnalyzer();
			analyzer.parse(specification);

			final List<ExtensionAttribute> temp = analyzer.getAttributes();
			if (temp != null) {
				attributes.addAll(temp);
			}
		}

		for (int i = 0; i < attributes.size(); i++) {
			final ExtensionAttribute extensionAttribute = attributes.get(i);
			switch (extensionAttribute.getAttributeType()) {
			case ANYTYPE:
			case VERSION:
			case REQUIRES:
			case TITANVERSION:
				break;
			default:
				// only extension attributes are allowed ... and
				// only because they can not be stopped earlier.
				extensionAttribute.getLocation().reportSemanticError("Extension attributes are not supported for types");
				break;
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getProposalKind() {
		if (type != null) {
			return type.getProposalDescription(new StringBuilder()).toString();
		}
		return "unknown type";
	}

	@Override
	/** {@inheritDoc} */
	public Object[] getOutlineChildren() {
		if (type == null) {
			return super.getOutlineChildren();
		}

		return type.getOutlineChildren();
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int index) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= index) {
			return;
		}

		if (subrefs.size() == index + 1 && identifier.getName().toLowerCase(Locale.ENGLISH).startsWith(subrefs.get(index).getId().getName().toLowerCase(Locale.ENGLISH))) {
			super.addProposal(propCollector, index);
		} else if (subrefs.size() > index + 1 && type != null && identifier.getName().equals(subrefs.get(index).getId().getName())) {
			// perfect match
			type.addProposal(propCollector, index + 1);
		}

		if (type != null && Type_type.TYPE_TTCN3_ENUMERATED.equals(type.getTypetype())) {
			type.addProposal(propCollector, index);
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
		} else {
			if (type != null && Type_type.TYPE_TTCN3_SEQUENCE.equals(type.getTypetype())) {

				if ((declarationCollector.getReference().getModuleIdentifier() != null && index == 1) || index == 0) {
					type.addDeclaration(declarationCollector, index);
				}

			}
		}

		if (type != null && Type_type.TYPE_TTCN3_ENUMERATED.equals(type.getTypetype())) {
			if ((declarationCollector.getReference().getModuleIdentifier() != null && index == 1) || index == 0) {
				type.addDeclaration(declarationCollector, index);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		final List<Integer> result = new ArrayList<Integer>();
		// might be extended with a subtype
		result.add(Ttcn3Lexer.LPAREN);
		// length restriction
		result.add(Ttcn3Lexer.LENGTH);
		// dimension
		result.add(Ttcn3Lexer.SQUAREOPEN);

		if (withAttributesPath == null || withAttributesPath.getAttributes() == null) {
			// might be extended with a with attribute
			result.add(Ttcn3Lexer.WITH);
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
					removeBridge();
					throw new ReParseException(result);
				}
			}

			if (type != null) {
				if (enveloped) {
					type.updateSyntax(reparser, false);
					reparser.updateLocation(type.getLocation());
				} else if (reparser.envelopsDamage(type.getLocation())) {
					try {
						type.updateSyntax(reparser, true);
						enveloped = true;
						reparser.updateLocation(type.getLocation());
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
		if (type != null) {
			type.updateSyntax(reparser, false);
			reparser.updateLocation(type.getLocation());
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
		if (type != null) {
			type.findReferences(referenceFinder, foundIdentifiers);
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
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final boolean cleanUp ) {
		final String genName = getGenName();

		if (type == null) {
			return;
		}

		final StringBuilder sb = aData.getCodeForType(genName);//aData.getSrc();
		final StringBuilder source = new StringBuilder();
		type.generateCode( aData, source );

		if (Type_type.TYPE_COMPONENT.equals(type.getTypetype())) {
			((Component_Type)type).getComponentBody().generateCode(aData, source);
		}

		sb.append(source);
	}
}
