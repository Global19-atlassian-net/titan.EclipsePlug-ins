/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NamingConventionHelper;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.types.ComponentTypeBody;
import org.eclipse.titan.designer.AST.TTCN3.types.Port_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.ArrayDimensions;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.T3Doc;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.IIdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.IdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.preferences.PreferenceConstants;

/**
 * The Def_Port class represents TTCN3 port definitions.
 *
 * @author Kristof Szabados
 * */
public final class Def_Port extends Definition {
	private static final String FULLNAMEPART1 = ".<type>";
	private static final String FULLNAMEPART2 = ".<dimensions>";
	public static final String PORTEXPECTED = "Port type expected";
	public static final String TYPEEXPECTED = "Type reference expected";

	private static final String KIND = "port definition";

	public static String getKind() {
		return KIND;
	}

	private final Reference portTypeReference;
	private Port_Type portType = null;
	private final ArrayDimensions dimensions;

	public Def_Port(final Identifier identifier, final Reference portTypeReference, final ArrayDimensions dimensions) {
		super(identifier);
		this.portTypeReference = portTypeReference;
		this.dimensions = dimensions;

		if (portTypeReference != null) {
			portTypeReference.setFullNameParent(this);
		}
		if (dimensions != null) {
			dimensions.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment_type getAssignmentType() {
		return Assignment_type.A_PORT;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (dimensions != null) {
			dimensions.setMyScope(scope);
		}
		if (portTypeReference != null) {
			portTypeReference.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (portTypeReference == child) {
			return builder.append(FULLNAMEPART1);
		} else if (dimensions == child) {
			return builder.append(FULLNAMEPART2);
		}

		return builder;
	}

	public ArrayDimensions getDimensions() {
		return dimensions;
	}

	@Override
	/** {@inheritDoc} */
	public String getAssignmentName() {
		return "port";
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "port.gif";
	}

	@Override
	/** {@inheritDoc} */
	public Port_Type getType(final CompilationTimeStamp timestamp) {
		check(timestamp);

		return portType;
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

		NamingConventionHelper.checkConvention(PreferenceConstants.REPORTNAMINGCONVENTION_GLOBAL_PORT, identifier, this);
		NamingConventionHelper.checkNameContents(identifier, getMyScope().getModuleScope().getIdentifier(), getDescription());

		if (portTypeReference == null) {
			return;
		}

		final Assignment assignment = portTypeReference.getRefdAssignment(timestamp, true);
		if (assignment == null) {
			return;
		}

		if (Assignment_type.A_TYPE.semanticallyEquals(assignment.getAssignmentType()) && assignment.getType(timestamp) != null) {
			IType type = assignment.getType(timestamp);
			type = type.getTypeRefdLast(timestamp);
			if (type != null && !type.getIsErroneous(timestamp)) {
				switch (type.getTypetype()) {
				case TYPE_PORT:
					portType = (Port_Type) type;
					break;
				case TYPE_REFERENCED:
					break;
				default:
					portTypeReference.getLocation().reportSemanticError(PORTEXPECTED);
					break;
				}
			}
		} else {
			portTypeReference.getLocation().reportSemanticError(TYPEEXPECTED);
		}

		if (dimensions != null) {
			dimensions.check(timestamp);
		}

		if (withAttributesPath != null) {
			withAttributesPath.checkGlobalAttributes(timestamp, false);
			withAttributesPath.checkAttributes(timestamp);
		}

	}

	@Override
	/** {@inheritDoc} */
	public boolean checkIdentical(final CompilationTimeStamp timestamp, final Definition definition) {
		check(timestamp);
		definition.check(timestamp);

		if (!Assignment_type.A_PORT.semanticallyEquals(definition.getAssignmentType())) {
			location.reportSemanticError(MessageFormat.format(
					"Local definition `{0}'' is a port, but the definition inherited from component type `{1}'' is a {2}",
					identifier.getDisplayName(), definition.getMyScope().getFullName(), definition.getAssignmentName()));
			return false;
		}

		final Def_Port otherPort = (Def_Port) definition;
		if (!portType.isIdentical(timestamp, otherPort.portType)) {
			final String mesage = MessageFormat.format(
					"Local port `{0}'' has type `{1}'', but the port inherited from component type `{2}'' has type `{3}''",
					identifier.getDisplayName(), portType.getTypename(), otherPort.getMyScope().getFullName(),
					otherPort.portType.getTypename());
			portTypeReference.getLocation().reportSemanticError(mesage);
			return false;
		}

		if (dimensions != null) {
			if (otherPort.dimensions != null) {
				if (!dimensions.isIdenticial(timestamp, otherPort.dimensions)) {
					location.reportSemanticError(MessageFormat
							.format("Local port `{0}'' and the port inherited from component type `{1}'' have different array dimensions",
									identifier.getDisplayName(), otherPort.getMyScope().getFullName()));
					return false;
				}
			} else {
				location.reportSemanticError(MessageFormat
						.format("Local definition `{0}'' is a port array, but the definition inherited from component type `{1}'' is a single port",
								identifier.getDisplayName(), otherPort.getMyScope().getFullName()));
				return false;
			}
		} else if (otherPort.dimensions != null) {
			location.reportSemanticError(MessageFormat
					.format("Local definition `{0}'' is a single port, but the definition inherited from component type `{1}'' is a port array",
							identifier.getDisplayName(), otherPort.getMyScope().getFullName()));
			return false;
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public String getProposalKind() {
		return KIND;
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int i) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= i) {
			return;
		}

		if (subrefs.size() == i + 1 && identifier.getName().toLowerCase().startsWith(subrefs.get(i).getId().getName().toLowerCase())) {
			super.addProposal(propCollector, i);
		} else if (subrefs.size() > i + 1 && portType != null && identifier.getName().equals(subrefs.get(i).getId().getName())) {
			// perfect match
			portType.addProposal(propCollector, i + 1);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int i) {
		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() > i && identifier.getName().equals(subrefs.get(i).getId().getName())) {
			if (subrefs.size() > i + 1 && portType != null) {
				portType.addDeclaration(declarationCollector, i + 1);
			} else if (subrefs.size() == i + 1 && Subreference_type.fieldSubReference.equals(subrefs.get(i).getReferenceType())) {
				declarationCollector.addDeclaration(this);
			}
		}
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
				if (result == 0 && identifier != null) {
					enveloped = true;
				} else {
					throw new ReParseException(result);
				}
			}

			if (portTypeReference != null) {
				portTypeReference.updateSyntax(reparser, false);
				reparser.updateLocation(portTypeReference.getLocation());
			}

			if (dimensions != null) {
				dimensions.updateSyntax(reparser, false);
			}

			if (withAttributesPath != null) {
				withAttributesPath.updateSyntax(reparser, false);
				reparser.updateLocation(withAttributesPath.getLocation());
			}

			if (!enveloped) {
				throw new ReParseException();
			}

			return;
		}

		reparser.updateLocation(identifier.getLocation());

		if (portTypeReference != null) {
			portTypeReference.updateSyntax(reparser, false);
			reparser.updateLocation(portTypeReference.getLocation());
		}

		if (dimensions != null) {
			dimensions.updateSyntax(reparser, false);
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
		if (portTypeReference != null) {
			portTypeReference.findReferences(referenceFinder, foundIdentifiers);
		}
		if (dimensions != null) {
			dimensions.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (portTypeReference != null && !portTypeReference.accept(v)) {
			return false;
		}
		if (dimensions != null && !dimensions.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final boolean cleanUp ) {
		final String genName = getGenName();
		final StringBuilder sb = aData.getSrc();
		final StringBuilder source = new StringBuilder();
		if ( !isLocal() ) {
			if(VisibilityModifier.Private.equals(getVisibilityModifier())) {
				source.append( "private" );
			} else {
				source.append( "public" );
			}
			source.append( " static " );
		}

		aData.addBuiltinTypeImport( "TitanPort" );
		aData.addBuiltinTypeImport("Base_Template.template_sel");
		aData.addBuiltinTypeImport("TtcnError");

		if (getMyScope() instanceof ComponentTypeBody) {
			if(dimensions == null) {
				source.append(MessageFormat.format("ThreadLocal<{0}> {1} = new ThreadLocal<{0}>() '{'\n", portType.getGenNameOwn(), genName));
				source.append("@Override\n" );
				source.append(MessageFormat.format("protected {0} initialValue() '{'\n", portType.getGenNameOwn()));
				source.append(MessageFormat.format("return new {0}(\"{1}\");\n", portType.getGenNameOwn(), identifier.getDisplayName()));
				source.append("}\n");
				source.append("};\n");
			} else {
				final StringBuilder tempSb = new StringBuilder();
				final String typeGenName = portType.generateCodePort(aData, tempSb, dimensions);
				aData.getCodeForType(typeGenName).append(tempSb);
	
				source.append(MessageFormat.format("ThreadLocal<{0}> {1} = new ThreadLocal<{0}>() '{'\n", typeGenName, genName));
				source.append("@Override\n" );
				source.append(MessageFormat.format("protected {0} initialValue() '{'\n", typeGenName));
				source.append(MessageFormat.format("final {0} temp = new {0}();\n", typeGenName));
				source.append(MessageFormat.format("final String port_name = \"{0}\";\n", identifier.getDisplayName()));
				source.append(MessageFormat.format("temp.set_name(port_name);\n", genName));
				source.append("return temp;\n");
				source.append("}\n");
				source.append("};\n");
	
				final StringBuilder preInit = aData.getPreInit();
				preInit.append("{\n");
				preInit.append(MessageFormat.format("final String port_name = \"{0}\";\n", identifier.getDisplayName()));
				preInit.append(MessageFormat.format("{0}.get().set_name(port_name);\n", genName));
				preInit.append("}\n");
			}

			sb.append(source);

			aData.getInitComp().append(MessageFormat.format("{0}.get().activate_port(false);\n", genName));
		} else {
			if(dimensions == null) {
				source.append(MessageFormat.format("{0} {1} = new {0}(\"{2}\");\n", portType.getGenNameOwn(), genName, identifier.getDisplayName()));
			} else {
				final StringBuilder tempSb = new StringBuilder();
				final String typeGenName = portType.generateCodePort(aData, tempSb, dimensions);
				aData.getCodeForType(typeGenName).append(tempSb);
	
				source.append(MessageFormat.format("{0} {1} = new {0}();\n", typeGenName, genName));
	
				final StringBuilder preInit = aData.getPreInit();
				preInit.append("{\n");
				preInit.append(MessageFormat.format("final String port_name = \"{0}\";\n", identifier.getDisplayName()));
				preInit.append(MessageFormat.format("{0}.set_name(port_name);\n", genName));
				preInit.append("}\n");
			}
	
			sb.append(source);
	
			aData.getInitComp().append(MessageFormat.format("{0}.activate_port(false);\n", genName));
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInitComp(final JavaGenData aData, final StringBuilder initComp, final Definition definition) {
		initComp.append(definition.getGenNameFromScope(aData, initComp, myScope, ""));
		initComp.append(".activate_port(false);\n");
	}
}
