/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.util.List;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferencingElement;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.ITypeWithComponents;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.declarationsearch.Declaration;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Class to represent a NamedTemplate.
 *
 * @author Kristof Szabados
 */
public final class NamedTemplate extends ASTNode implements ILocateableNode, IIncrementallyUpdateable, IReferencingElement {

	private final Identifier name;
	// TODO: Check if removing template property "final" causes problem or not
	private final TTCN3Template template;

	/**
	 * The location of the whole template. This location encloses the
	 * template fully, as it is used to report errors to.
	 **/
	private Location location;

	/**
	 * Tells if this named template was parsed or created while doing the
	 * semantic check.
	 * */
	private final boolean parsed;

	public NamedTemplate(final Identifier name, final TTCN3Template template) {
		//		final boolean parsed = true;
		//		NamedTemplate(name,template,parsed);
		super();
		this.name = name;
		this.template = template;
		location = NULL_Location.INSTANCE;
		this.parsed = true;

		if (template != null) {
			template.setFullNameParent(this);
		}
	}

	public NamedTemplate(final Identifier name, final TTCN3Template template, final boolean parsed) {
		super();
		this.name = name;
		this.template = template;
		location = NULL_Location.INSTANCE;
		this.parsed = parsed;

		if (template != null) {
			template.setFullNameParent(this);
		}
	}

	public Identifier getName() {
		return name;
	}

	public TTCN3Template getTemplate() {
		return template;
	}

	public boolean isParsed() {
		return parsed;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (template != null) {
			template.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return location;
	}

	/**
	 * Handles the incremental parsing of this named template.
	 *
	 * @param reparser
	 *                the parser doing the incremental parsing.
	 * @param isDamaged
	 *                true if the location contains the damaged area, false
	 *                if only its' location needs to be updated.
	 * */
	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		reparser.updateLocation(name.getLocation());
		if (template != null) {
			template.updateSyntax(reparser, false);
			reparser.updateLocation(template.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (template != null) {
			template.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (name != null && !name.accept(v)) {
			return false;
		}
		if (template != null && !template.accept(v)) {
			return false;
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public Declaration getDeclaration() {

		INamedNode inamedNode = getNameParent();

		while (!(inamedNode instanceof Named_Template_List)) {
			if( inamedNode == null) {
				return null; //FIXME: this is just a temp solution! find the reason!
			}
			inamedNode = inamedNode.getNameParent();
		}

		final Named_Template_List namedTemplList = (Named_Template_List) inamedNode;
		IType type = namedTemplList.getMyGovernor();
		if (type == null) {
			return null;
		}

		type = type.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());

		if (type instanceof ITypeWithComponents) {
			final Identifier id = ((ITypeWithComponents) type).getComponentIdentifierByName(getName());
			return Declaration.createInstance(type.getDefiningAssignment(), id);
		}

		return null;
	}
}
