/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.ASN1.types.Open_Type;
import org.eclipse.titan.designer.AST.TTCN3.IAppendableSyntax;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ParserUtilities;
import org.eclipse.titan.designer.parsers.ttcn3parser.ITTCN3ReparseBase;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Reparser.Pr_reparse_StructFieldDefsContext;

/**
 * Map of component fields.
 * <p>
 * This class is used to represent the fields of a structured type.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class CompFieldMap extends ASTNode implements ILocateableNode, IIncrementallyUpdateable {
	public static final String DUPLICATEFIELDNAMEFIRST = "Duplicate field name `{0}'' was first declared here";
	public static final String DUPLICATEFIELDNAMEREPEATED = "Duplicate field name `{0}'' was declared here again";

	final ArrayList<CompField> fields;
	private WeakReference<Type> myType;

	/** Should be IDentifier based. */
	Map<String, CompField> componentFieldMap;

	/**
	 * Stores the components that were identified to be duplicates, to save time
	 * on re-analyzing.
	 */
	private List<CompField> doubleComponents;

	/** Holds the last time when the components were checked, or null if never. */
	private CompilationTimeStamp lastCompilationTimeStamp;
	private CompilationTimeStamp lastUniquenessCheck;

	/**
	 * The location of the whole component field map. This location encloses the
	 * statement fully, as it is used to report errors to.
	 **/
	private Location location = NULL_Location.INSTANCE;

	private static final Comparator<CompField> FIELD_INSERTION_COMPARATOR = new Comparator<CompField>() {

		@Override
		public int compare(final CompField o1, final CompField o2) {
			return o1.getLocation().getOffset() - o2.getLocation().getOffset();
		}

	};

	public CompFieldMap() {
		fields = new ArrayList<CompField>();
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
	 * Adds a component to the list of components.
	 *
	 * @param field the component to be added.
	 * */
	public void addComp(final CompField field) {
		if (field != null && field.getIdentifier() != null && field.getIdentifier().getName() != null) {
			fields.add(field);
			field.setFullNameParent(this);
			lastCompilationTimeStamp = null;
			lastUniquenessCheck = null;
		}
	}

	/**
	 * Adds a list of new field the already existing list of fields, in an
	 * ordered fashion.
	 *
	 * @param fields the list of new fields to be merged with the originals.
	 * */
	protected void addFieldsOrdered(final List<CompField> fields) {
		for (int i = 0, size = fields.size(); i < size; i++) {
			final CompField field = fields.get(i);

			if (field != null && field.getIdentifier() != null && field.getIdentifier().getName() != null) {
				final int position = Collections.binarySearch(this.fields, field, FIELD_INSERTION_COMPARATOR);

				if (position < 0) {
					this.fields.add((position + 1) * -1, field);
					field.setMyScope(getMyScope());
					field.setFullNameParent(this);
					lastCompilationTimeStamp = null;
					lastUniquenessCheck = null;
				}
			}
		}
	}
	/**
	 * Returns the componentFieldMap. If necessary, creates it and fills it. Never return null.
	 * @param timestamp
	 * @return the componentFieldMap
	 */
	public Map<String, CompField> getComponentFieldMap(final CompilationTimeStamp timestamp) {
		if (lastUniquenessCheck == null) {
			checkUniqueness(timestamp);
		}

		return componentFieldMap;
	}

	/**
	 * Sets the type, to whom this list of components belong to.
	 *
	 * @param type the type to be set.
	 * */
	public void setMyType(final Type type) {
		myType = new WeakReference<Type>(type);
	}

	/**
	 * Sets the actual scope of this component list.
	 * <p>
	 * Sets the scope for all components too.
	 *
	 * @param scope the scope to be set
	 * */
	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);

		fields.trimToSize();
		for (final CompField field : fields) {
			field.setMyScope(scope);
		}
	}

	/**
	 * Checks the uniqueness of the definitions, and also builds a hashmap of
	 * them to speed up further searches.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle, or -1
	 *            in silent check mode.
	 * */
	protected void checkUniqueness(final CompilationTimeStamp timestamp) {
		if (lastUniquenessCheck != null && !lastUniquenessCheck.isLess(timestamp)) {
			return;
		}


		if (doubleComponents != null) {
			doubleComponents.clear();
		}

		componentFieldMap = new HashMap<String, CompField>(fields.size());

		if(fields.size()==0) {
			return; //too early check
		}

		lastUniquenessCheck = timestamp;

		for (int i = 0, size = fields.size(); i < size; i++) {
			final CompField field = fields.get(i);

			final Identifier fieldIdentifier = field.getIdentifier();
			if(fieldIdentifier == null) {
				continue;
			}

			final String fieldName = fieldIdentifier.getName();
			if (componentFieldMap.containsKey(fieldName)) {
				if (doubleComponents == null) {
					doubleComponents = new ArrayList<CompField>();
				}
				doubleComponents.add(field);
			} else {
				componentFieldMap.put(fieldName, field);
			}
		}

		//In case of Open_Type it is normal that a type is referenced more times, but only once per Object
		//Now one field of an object class can one Open_Type, and each object of its object set can add a possible type to it
		//FIXME: Perhaps this class should be copied under asn1 to handle this ASN1 problem
		if (doubleComponents != null) {
			final INamedNode p = getNameParent();
			if( p instanceof Open_Type ) {
				return;
			}
			for (int i = 0, size = doubleComponents.size(); i < size; i++) {
				final CompField field = doubleComponents.get(i);
				//remove duplication from fields - not used anymore
				//fields.remove(field);
				//report duplication:
				final Identifier fieldIdentifier = field.getIdentifier();
				final String fieldName = fieldIdentifier.getName();
				componentFieldMap.get(fieldName).getIdentifier().getLocation().reportSingularSemanticError(
						MessageFormat.format(DUPLICATEFIELDNAMEFIRST, fieldIdentifier.getDisplayName()));
				fieldIdentifier.getLocation().reportSemanticError(
						MessageFormat.format(DUPLICATEFIELDNAMEREPEATED, fieldIdentifier.getDisplayName()));
			}
		}

	}
	/**
	 * Checks if the member 'fields' is empty
	 * @return
	 */
	public boolean isEmpty() {
		return (fields.size() == 0) ;
	}

	/**
	 * Does the semantic checking of the field.
	 *
	 * @param timestamp the timestamp of the actual semantic check cycle.
	 * */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastCompilationTimeStamp != null && !lastCompilationTimeStamp.isLess(timestamp)) {
			return;
		}
		checkUniqueness(timestamp);
		if (myType == null) {
			return;
		}
		final Type parentType = myType.get();
		lastCompilationTimeStamp = timestamp;
		for (int i = 0, size = fields.size(); i < size; i++) {
			final CompField field = fields.get(i);
			final Type fieldType = field.getType();
			fieldType.setGenName(myType.get().getGenNameOwn(), field.getIdentifier().getName());
			fieldType.setParentType(parentType);
			field.check(timestamp);
		}
	}

	/** @return the number of components */
	public final int getNofComponents() {
		return fields.size();
	}

	/**
	 * Returns the element at the specified position.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this list
	 */
	public final CompField getComponentByIndex(final int index) {
		return fields.get(index);
	}

	/**
	 * Returns the component identified by the given name. The list of
	 * components is also checked if it hasn't been checked before.
	 *
	 * @param id the identifier selecting the component to return.
	 * @return the component found, or null if none.
	 * */
	public CompField getCompWithName(final Identifier id) {
		checkUniqueness(CompilationTimeStamp.getBaseTimestamp());
		if (componentFieldMap!=null && componentFieldMap.containsKey(id.getName())) {
			return componentFieldMap.get(id.getName());
		}
		return null;
	}

	/**
	 * Returns the component identified by the given name. The list of
	 * components is also checked if it was not before.
	 *
	 * @param name of the identifier selecting the component to return.
	 * @return the component found, or null if none.
	 * */
	public CompField getCompWithName(final String name) {
		if (lastUniquenessCheck == null) {
			checkUniqueness(CompilationTimeStamp.getBaseTimestamp());
		}

		if (componentFieldMap!=null && componentFieldMap.containsKey(name)) {
			return componentFieldMap.get(name);
		}

		return null;
	}

	/**
	 * Returns a list of the components whose identifier starts with the given
	 * prefix.
	 *
	 * @param prefix the prefix used to select the component fields.
	 * @return the list of component fields which start with the provided
	 *         prefix.
	 * */
	public List<CompField> getComponentsWithPrefix(final String prefix) {

		checkUniqueness(CompilationTimeStamp.getBaseTimestamp());

		final List<CompField> compFields = new ArrayList<CompField>();
		Identifier id = null;
		for (int i = 0; i < fields.size(); i++) {
			id = fields.get(i).getIdentifier();
			if(id!=null){
				if(id.getName().startsWith(prefix)){
					compFields.add(fields.get(i));
				}
			}
		}
		return compFields;
	}

	/**
	 * Returns a list of the components whose identifier starts with the given
	 * prefix.
	 *
	 * @param prefix the prefix used to select the component fields.
	 * @return the list of component fields which start with the provided
	 *         prefix.
	 * */
	public List<CompField> getComponentsWithPrefixCaseInsensitive(final String prefix) {
		if (lastUniquenessCheck == null) {
			checkUniqueness(CompilationTimeStamp.getBaseTimestamp());
		}

		final List<CompField> compFields = new ArrayList<CompField>();
		for (int i = 0; i < fields.size(); i++) {
			final String componentName = fields.get(i).getIdentifier().getName();
			if (componentName.toLowerCase().startsWith(prefix.toLowerCase())) {
				compFields.add(fields.get(i));
			}
		}
		return compFields;
	}

	public Object[] getOutlineChildren() {
		return fields.toArray();
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			lastCompilationTimeStamp = null;
			if (doubleComponents != null) {
				fields.addAll(doubleComponents);
				doubleComponents = null;
				lastUniquenessCheck = null;
			}

			boolean enveloped = false;
			int nofDamaged = 0;
			int leftBoundary = location.getOffset();
			int rightBoundary = location.getEndOffset();
			final int damageOffset = reparser.getDamageStart();
			IAppendableSyntax lastAppendableBeforeChange = null;
			IAppendableSyntax lastPrependableBeforeChange = null;

			for (int i = 0, size = fields.size(); i < size && !enveloped; i++) {
				final CompField field = fields.get(i);
				final Location tempLocation = field.getLocation();
				//move offset to commentLocation
				if (field.getCommentLocation() != null) {
					tempLocation.setOffset(field.getCommentLocation().getOffset());
				}
				if (reparser.envelopsDamage(tempLocation)) {
					enveloped = true;
					leftBoundary = tempLocation.getOffset();
					rightBoundary = tempLocation.getEndOffset();
				} else if (reparser.isDamaged(tempLocation)) {
					nofDamaged++;
					if (reparser.getDamageStart() == tempLocation.getEndOffset()) {
						lastAppendableBeforeChange = field;
					} else if (reparser.getDamageEnd() == tempLocation.getOffset()) {
						lastPrependableBeforeChange = field;
					}
				} else {
					if (tempLocation.getEndOffset() < damageOffset && tempLocation.getEndOffset() > leftBoundary) {
						leftBoundary = tempLocation.getEndOffset();
						lastAppendableBeforeChange = field;
					}
					if (tempLocation.getOffset() >= damageOffset && tempLocation.getOffset() < rightBoundary) {
						rightBoundary = tempLocation.getOffset();
						lastPrependableBeforeChange = field;
					}
				}
			}

			// extend the reparser to the calculated values if the damage was not enveloped
			if (!enveloped) {
				reparser.extendDamagedRegion(leftBoundary, rightBoundary);
			}

			// if there is a component field that is right now being extended we
			// should add it to the damaged domain as the extension might be
			// correct
			if (lastAppendableBeforeChange != null) {
				final boolean isBeingExtended = reparser.startsWithFollow(lastAppendableBeforeChange.getPossibleExtensionStarterTokens());
				if (isBeingExtended) {
					leftBoundary = lastAppendableBeforeChange.getLocation().getOffset();
					nofDamaged++;
					enveloped = false;
					reparser.extendDamagedRegion(leftBoundary, rightBoundary);
				}
			}

			if (lastPrependableBeforeChange != null) {
				final List<Integer> temp = lastPrependableBeforeChange.getPossiblePrefixTokens();

				if (temp != null && reparser.endsWithToken(temp)) {
					rightBoundary = lastPrependableBeforeChange.getLocation().getEndOffset();
					nofDamaged++;
					enveloped = false;
					reparser.extendDamagedRegion(leftBoundary, rightBoundary);
				}
			}

			if (nofDamaged != 0) {
				removeStuffInRange(reparser);
			}

			for (int i = 0; i < fields.size(); i++) {
				final CompField field = fields.get(i);
				final Location tempLocation = field.getLocation();

				if (reparser.isAffectedAppended(tempLocation)) {
					try {
						field.updateSyntax(reparser, enveloped && reparser.envelopsDamage(tempLocation));
						reparser.updateLocation(field.getLocation());
					} catch (ReParseException e) {
						if (e.getDepth() == 1) {
							enveloped = false;
							fields.remove(i);
							i--;
							reparser.extendDamagedRegion(tempLocation);
						} else {
							e.decreaseDepth();
							throw e;
						}
					}
				}
			}

			if (!enveloped) {
				reparser.extendDamagedRegion(leftBoundary, rightBoundary);

				int result = reparse( reparser );
				if (result == 0) {
					return;
				}
				throw new ReParseException(Math.max(--result, 0));
			}

			return;
		}

		for (int i = 0, size = fields.size(); i < size; i++) {
			final CompField field = fields.get(i);

			field.updateSyntax(reparser, false);
			reparser.updateLocation(field.getLocation());
		}
		if (doubleComponents != null) {
			for (int i = 0, size = doubleComponents.size(); i < size; i++) {
				final CompField field = doubleComponents.get(i);

				field.updateSyntax(reparser, false);
				reparser.updateLocation(field.getLocation());
			}
		}
	}

	private int reparse(final TTCN3ReparseUpdater aReparser) {
		return aReparser.parse(new ITTCN3ReparseBase() {
			@Override
			public void reparse(final Ttcn3Reparser parser) {
				final Pr_reparse_StructFieldDefsContext root = parser.pr_reparse_StructFieldDefs();
				ParserUtilities.logParseTree( root, parser );
				final List<CompField> tempFields = root.fields;
				lastUniquenessCheck = null;
				if ( parser.isErrorListEmpty() ) {
					if (tempFields != null) {
						addFieldsOrdered(tempFields);
					}
				}
			}
		});
	}

	private void removeStuffInRange(final TTCN3ReparseUpdater reparser) {
		Location temp;
		for (int i = fields.size() - 1; i >= 0; i--) {
			temp = fields.get(i).getLocation();
			if (reparser.isDamaged(temp)) {
				reparser.extendDamagedRegion(temp);
				fields.remove(i);
			}
		}
	}

	public void getEnclosingField(final int offset, final ReferenceFinder rf) {
		for (final CompField field : fields) {
			if (field.getLocation().containsOffset(offset)) {
				rf.type = myType.get(); // FIXME?
				rf.fieldId = field.getIdentifier();
				field.getType().getEnclosingField(offset, rf);
				return;
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (fields == null) {
			return;
		}

		for (final CompField cf : fields) {
			cf.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (fields != null) {
			for (final CompField cf : fields) {
				if (!cf.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}
}
