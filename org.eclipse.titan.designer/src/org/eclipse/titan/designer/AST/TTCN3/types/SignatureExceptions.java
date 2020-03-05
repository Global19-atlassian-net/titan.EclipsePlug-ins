/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents the exception types of a signature.
 *
 * @author Kristof Szabados
 * */
public final class SignatureExceptions extends ASTNode implements IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".<type";

	private final List<Type> exceptionTypes;
	private final Map<String, Type> exceptionMap;

	private Location location = NULL_Location.INSTANCE;

	private CompilationTimeStamp lastTimeChecked = null;

	public SignatureExceptions(final List<Type> exceptionTypes) {
		if (exceptionTypes == null) {
			this.exceptionTypes = new ArrayList<Type>();
		} else {
			this.exceptionTypes = exceptionTypes;
		}
		exceptionMap = new HashMap<String, Type>();

		for (final Type type : this.exceptionTypes) {
			type.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0; i < exceptionTypes.size(); i++) {
			if (exceptionTypes.get(i) == child) {
				return builder.append(FULLNAMEPART).append(Integer.toString(i)).append(INamedNode.MORETHAN);
			}
		}

		return builder;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		for (final Type exception : exceptionTypes) {
			exception.setMyScope(scope);
		}
	}

	/** @return the number of exceptions stored */
	public int getNofExceptions() {
		return exceptionTypes.size();
	}

	/**
	 * Returns the selected exception type.
	 *
	 * @param index the index of the exception to find.
	 * @return the exception at the given index.
	 * */
	public Type getExceptionByIndex(final int index) {
		return exceptionTypes.get(index);
	}

	/**
	 * Checks if an exception is in this list.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle
	 * @param type the exception to look for
	 *
	 * @return true if there is an exception with the same name, false
	 *         otherwise.
	 * */
	public boolean hasException(final CompilationTimeStamp timestamp, final Type type) {
		if (type == null) {
			return false;
		}

		if (type.getIsErroneous(timestamp)) {
			return true;
		}

		return exceptionMap.containsKey(type.getTypename());
	}

	/**
	 * Calculates the number of exceptions that are compatible with the provided
	 * type.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle
	 * @param type the type to check against
	 *
	 * @return the number of compatible exceptions
	 * */
	public int getNofCompatibleExceptions(final CompilationTimeStamp timestamp, final IType type) {
		if (type.getTypeRefdLast(timestamp).getIsErroneous(timestamp)) {
			return 1;
		}

		int result = 0;
		for (int i = 0; i < exceptionTypes.size(); i++) {
			if (exceptionTypes.get(i).isCompatible(timestamp, type, null, null, null)) {
				result++;
			}
		}

		return result;
	}

	/**
	 * Checks the type of the exception list.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle
	 * @param signature the signature type the exceptions belong to.
	 * */
	public void check(final CompilationTimeStamp timestamp, final Signature_Type signature) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		exceptionMap.clear();

		for (int i = 0; i < exceptionTypes.size(); i++) {
			final Type type = exceptionTypes.get(i);

			type.setParentType(signature);
			type.check(timestamp);
			if (!type.getIsErroneous(timestamp)) {
				type.checkEmbedded(timestamp, type.getLocation(), false, "on the exception list of a signature");

				final String name = type.getTypename();
				if (exceptionMap.containsKey(name)) {
					type.getLocation().reportSemanticError("Duplicate type in exception list");
					exceptionMap.get(name).getLocation()
					.reportSingularSemanticError(MessageFormat.format("Type `{0}'' is already given here", name));
				} else {
					exceptionMap.put(name, type);
				}
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

		Type exception;
		for (int i = 0, size = exceptionTypes.size(); i < size; i++) {
			exception = exceptionTypes.get(i);

			exception.updateSyntax(reparser, isDamaged);
			reparser.updateLocation(exception.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (exceptionTypes != null) {
			for (final Type t : exceptionTypes) {
				t.findReferences(referenceFinder, foundIdentifiers);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (exceptionTypes!=null) {
			for (final Type t : exceptionTypes) {
				if (!t.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}
}
