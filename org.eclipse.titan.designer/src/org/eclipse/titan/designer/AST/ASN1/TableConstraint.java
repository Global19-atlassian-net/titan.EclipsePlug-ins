/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.titan.common.parsers.SyntacticErrorStorage;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.AtNotation;
import org.eclipse.titan.designer.AST.AtNotations;
import org.eclipse.titan.designer.AST.BridgingNamedNode;
import org.eclipse.titan.designer.AST.Constraint;
import org.eclipse.titan.designer.AST.Constraints;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IReferencingType;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Identifier.Identifier_type;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.ASN1.Object.ASN1Objects;
import org.eclipse.titan.designer.AST.ASN1.Object.FieldName;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectSet_definition;
import org.eclipse.titan.designer.AST.ASN1.Object.Object_Definition;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Choice_Type;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Sequence_Type;
import org.eclipse.titan.designer.AST.ASN1.types.ASN1_Set_Type;
import org.eclipse.titan.designer.AST.ASN1.types.ObjectClassField_Type;
import org.eclipse.titan.designer.AST.ASN1.types.Open_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.CompField;
import org.eclipse.titan.designer.AST.TTCN3.types.RefdSpec_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.Referenced_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Choice_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Sequence_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TTCN3_Set_Type;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ParserMarkerSupport;
import org.eclipse.titan.designer.parsers.asn1parser.Asn1Parser;
import org.eclipse.titan.designer.parsers.asn1parser.BlockLevelTokenStreamTracker;

/**
 * Represents a TableConstraint (SimpleTableConstraint and
 * ComponentRelationConstraint)
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class TableConstraint extends Constraint {
	private static final String FULLNAMEPART = ".<tableconstraint-os>";
	private static final String OCFTEXPECTED = "TableConstraint can only be applied to ObjectClassFieldType";
	private static final String CANNOTDETERMINEPARENT = "Invalid use of ComponentRelationConstraint (cannot determine parent type)";
	private static final String TOOMANYDOTS = "Too many dots. This component has only {0} parameters.";
	private static final String NOCOMPONENTERROR = "Type `{0}'' has no component with name `{1}''.";
	private static final String SECHOEXPECTED = "Type `{0}'' is not a SEQUENCE, SET or CHOICE type";
	private static final String SAMECONSTRAINTEXPECTED = "The referenced components must be value (set) fields"
			+ " constrained by the same objectset as the referencing component";

	private final Block mObjectSetBlock;
	private final Block mAtNotationsBlock;

	protected ObjectSet objectSet;
	protected AtNotations atNotationList;
	private Identifier objectClassFieldname;

	private IType constrainedType;

	public TableConstraint(final Block aObjectSetBlock, final Block aAtNotationsBlock) {
		super(Constraint_type.CT_TABLE);
		this.mObjectSetBlock = aObjectSetBlock;
		this.mAtNotationsBlock = aAtNotationsBlock;
	}

	public TableConstraint newInstance() {
		return new TableConstraint(mObjectSetBlock, mAtNotationsBlock);
	}

	public ObjectSet getObjectSet() {
		return objectSet;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		if (lastTimeChecked == null) {
			parseBlocks();
		}
		lastTimeChecked = timestamp;

		if (null == myType) {
			return;
		}

		objectSet.setMyScope(myType.getMyScope());

		final BridgingNamedNode bridge = new BridgingNamedNode(this, FULLNAMEPART);
		objectSet.setFullNameParent(bridge);
		objectSet.setGenName(myType.getGenNameOwn(), "os");

		// search the constrained type (not the reference to it)
		constrainedType = myType;
		while (true) {
			if (constrainedType.getIsErroneous(timestamp)) {
				return;
			}

			if (Type_type.TYPE_OPENTYPE.equals(constrainedType.getTypetype())
					|| Type_type.TYPE_OBJECTCLASSFIELDTYPE.equals(constrainedType.getTypetype())) {
				break;
			} else if (constrainedType instanceof IReferencingType) {
				final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
				constrainedType = ((IReferencingType) constrainedType).getTypeRefd(timestamp, chain);
				chain.release();
			} else {
				myType.getLocation().reportSemanticError(OCFTEXPECTED);
				return;
			}
		}

		if (Type_type.TYPE_OBJECTCLASSFIELDTYPE.equals(constrainedType.getTypetype())) {
			final ObjectClassField_Type ocfType = (ObjectClassField_Type) constrainedType;
			objectClassFieldname = ocfType.getObjectClassFieldName();
			objectSet.setMyGovernor(ocfType.getMyObjectClass());
			objectSet.check(timestamp);
			return;
		}

		// can only be opentype at this point.
		final Open_Type openType = (Open_Type) constrainedType;
		openType.setMyTableConstraint(this);
		objectClassFieldname = openType.getObjectClassFieldName();
		objectSet.setMyGovernor(openType.getMyObjectClass());
		objectSet.check(timestamp);

		if (null == atNotationList) {
			return;
		}

		// componentrelationconstraint...
		// search the outermost textually enclosing seq, set or choice
		IType outermostParent = null;
		IType tempType = myType;
		do {
			switch (tempType.getTypetype()) {
			case TYPE_ASN1_CHOICE:
			case TYPE_TTCN3_CHOICE:
			case TYPE_OPENTYPE:
			case TYPE_ASN1_SEQUENCE:
			case TYPE_TTCN3_SEQUENCE:
			case TYPE_ASN1_SET:
			case TYPE_TTCN3_SET:
				outermostParent = tempType;
				break;
			default:
				break;
			}

			tempType = tempType.getParentType();
		} while (null != tempType);

		if (null == outermostParent) {
			myType.getLocation().reportSemanticError(CANNOTDETERMINEPARENT);
			return;
		}

		//outermostparent->set_opentype_outermost();

		// TODO implement the setting of set_has_openType
		AtNotation atNotation;
		for (int i = 0; i < atNotationList.getNofAtNotations(); i++) {
			atNotation = atNotationList.getAtNotationByIndex(i);

			IType parent = null;
			if (0 == atNotation.getLevels()) {
				parent = outermostParent;
			} else {
				parent = myType;
				for (int level = atNotation.getLevels(); level > 0; level--) {
					parent = parent.getParentType();
					if (null == parent) {
						myType.getLocation().reportSemanticError(MessageFormat.format(TOOMANYDOTS, atNotation.getLevels()));
						return;
					}
				}
			}

			tempType = parent;
			atNotation.setFirstComponent(parent);

			// component identifiers... do they exist? yes, if the refd type is constrained
			final FieldName componentIdentifiers = atNotation.getComponentIdentifiers();
			for (int j = 0; j < componentIdentifiers.getNofFields(); j++) {
				final Identifier identifier = componentIdentifiers.getFieldByIndex(j);
				switch (tempType.getTypetype()) {
				case TYPE_ASN1_CHOICE: {
					final ASN1_Choice_Type temp2 = (ASN1_Choice_Type) tempType;
					if ( temp2.hasComponentWithName(identifier) ) {
						final CompField cf = temp2.getComponentByName(identifier);
						tempType = cf.getType();
					} else {
						myType.getLocation().reportSemanticError(
								MessageFormat.format(NOCOMPONENTERROR, tempType.getFullName(),
										identifier.getDisplayName()));
						return;
					}
					break;
				}
				case TYPE_TTCN3_CHOICE: {
					final TTCN3_Choice_Type temp2 = (TTCN3_Choice_Type) tempType;
					if (temp2.hasComponentWithName(identifier.getName())) {
						tempType = temp2.getComponentByName(identifier.getName()).getType();
					} else {
						myType.getLocation().reportSemanticError(
								MessageFormat.format(NOCOMPONENTERROR, tempType.getFullName(),
										identifier.getDisplayName()));
						return;
					}
					break;
				}
				case TYPE_OPENTYPE: {
					final Open_Type temp2 = (Open_Type) tempType;
					if (temp2.hasComponentWithName(identifier)) {
						final CompField cf = temp2.getComponentByName(identifier);
						tempType = cf.getType();
					} else {
						myType.getLocation().reportSemanticError(
								MessageFormat.format(NOCOMPONENTERROR, tempType.getFullName(),
										identifier.getDisplayName()));
						return;
					}
					break;
				}
				case TYPE_ASN1_SEQUENCE: {
					final ASN1_Sequence_Type temp2 = (ASN1_Sequence_Type) tempType;
					if (temp2.hasComponentWithName(identifier)) {
						final CompField cf = temp2.getComponentByName(identifier);
						tempType = cf.getType();
					} else {
						myType.getLocation().reportSemanticError(
								MessageFormat.format(NOCOMPONENTERROR, tempType.getFullName(),
										identifier.getDisplayName()));
						return;
					}
					break;
				}
				case TYPE_TTCN3_SEQUENCE: {
					final TTCN3_Sequence_Type temp2 = (TTCN3_Sequence_Type) tempType;
					if (temp2.hasComponentWithName(identifier.getName())) {
						tempType = temp2.getComponentByName(identifier.getName()).getType();
					} else {
						myType.getLocation().reportSemanticError(
								MessageFormat.format(NOCOMPONENTERROR, tempType.getFullName(),
										identifier.getDisplayName()));
						return;
					}
					break;
				}
				case TYPE_ASN1_SET: {
					final ASN1_Set_Type temp2 = (ASN1_Set_Type) tempType;
					if (temp2.hasComponentWithName(identifier)) {
						final CompField cf = temp2.getComponentByName(identifier);
						tempType = cf.getType();
					} else {
						myType.getLocation().reportSemanticError(
								MessageFormat.format(NOCOMPONENTERROR, tempType.getFullName(),
										identifier.getDisplayName()));
						return;
					}
					break;
				}
				case TYPE_TTCN3_SET: {
					final TTCN3_Set_Type temp2 = (TTCN3_Set_Type) tempType;
					if (temp2.hasComponentWithName(identifier.getName())) {
						tempType = temp2.getComponentByName(identifier.getName()).getType();
					} else {
						myType.getLocation().reportSemanticError(
								MessageFormat.format(NOCOMPONENTERROR, tempType.getFullName(),
										identifier.getDisplayName()));
						return;
					}
					break;
				}
				default:
					myType.getLocation().reportSemanticError(MessageFormat.format(SECHOEXPECTED, tempType.getFullName()));
					return;
				}
			}
			atNotation.setLastComponent(tempType);

			/*
			 * check if the referenced component is constrained by the same objectset...
			 */
			boolean ok = false;
			final Constraints constraints = tempType.getConstraints();
			if (constraints != null) {
				constraints.check(timestamp);
				final TableConstraint tableConstraint = constraints.getTableConstraint();
				if (tableConstraint != null) {
					final IType ocft = tableConstraint.constrainedType;
					if (Type_type.TYPE_OBJECTCLASSFIELDTYPE.equals(ocft.getTypetype())) {
						atNotation.setObjectClassFieldname(((ObjectClassField_Type) ocft).getObjectClassFieldName());

						IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
						final ObjectSet_definition osdef1 = tableConstraint.objectSet.getRefdLast(timestamp, chain);
						chain.release();
						chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
						final ObjectSet_definition osdef2 = objectSet.getRefdLast(timestamp, chain);
						chain.release();
						if( osdef1 == osdef2) {
							ok = true;
						} else {
							ok = false;
						}
					}
				}
			}
			if (!ok) {
				myType.getLocation().reportSemanticError(SAMECONSTRAINTEXPECTED);
				return;
			}
		}  //for loop

		// well, the atnotations seems to be ok, let's produce the alternatives for the opentype
		final ObjectSet_definition temp_Def = objectSet.getRefdLast(timestamp, null);
		final ASN1Objects objects = temp_Def.getObjs();
		collectTypesOfOpenType(timestamp, objects, openType);
	}

	private void collectTypesOfOpenType(final CompilationTimeStamp timestamp, final ASN1Objects objects, final Open_Type aOpenType) {
		for (int i = 0; i < objects.getNofObjects(); i++) {
			final Object_Definition object = objects.getObjectByIndex(i);
			if (!object.hasFieldSettingWithNameDefault(objectClassFieldname)) {
				continue;
			}

			//inside object definitions the settings of field specifications can only be types.
			final Type type = (Type)object.getSettingByNameDefault(objectClassFieldname);
			final AtomicBoolean isStrange = new AtomicBoolean();
			final Identifier altname = getOpenTypeAlternativeName(timestamp, type, isStrange);
			if (!aOpenType.hasComponentWithName(altname)) {
				final RefdSpec_Type otype = new RefdSpec_Type(type);
				otype.setMyScope(type.getMyScope());
				otype.setGenName(type.getGenNameOwn());
				aOpenType.addComponent(new CompField( altname, otype, false, null));
				if (isStrange.get()) {
					aOpenType.getLocation().reportSemanticWarning(MessageFormat.format("Strange alternative name (`{0}') was added to open type `{1}'", altname.getDisplayName(), aOpenType.getFullName()));
				}
			}
		}

		aOpenType.setFullNameParent(this);
		aOpenType.check(timestamp);
	}


	//Original titan.core version: t_type->get_otaltname(is_strange);
	private Identifier getOpenTypeAlternativeName(final CompilationTimeStamp timestamp, final Type type, final AtomicBoolean isStrange) {
		final StringBuffer sb = new StringBuffer();
		//TODO:  if (is_tagged() || is_constrained() || hasRawAttrs()) {
		if (!type.getIsErroneous(timestamp) && type.isConstrained()) {
			sb.append(type.getGenNameOwn());
			isStrange.set(true);
		} else if (!type.getIsErroneous(timestamp) && type instanceof Referenced_Type) {
			final Reference t_ref = ((Referenced_Type) type).getReference();
			if (t_ref != null) {
				final Identifier id = t_ref.getId();
				final String dn = id.getDisplayName();
				final int i = dn.indexOf('.');
				if (i >= 0 && i < dn.length()) {
					// id is not regular because t_ref is a parameterized reference
					sb.append(id.getName());
					isStrange.set(true);
				} else {
					final Assignment as = t_ref.getRefdAssignment(timestamp, true);
					if( as == null) {
						return null;
					}

					final Scope assScope = as.getMyScope();
					if (assScope.getParentScope() == assScope.getModuleScope()) {
						sb.append(id.getName());
						isStrange.set(false);
					} else {
						// t_ref is a dummy reference in a parameterized assignment
						// (i.e. it points to a parameter assignment of an instantiation)
						// perform the same examination recursively on the referenced type
						// (which is the actual parameter)
						final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
						final IType referencedType = ((Referenced_Type) type).getTypeRefd(timestamp, chain);
						chain.release();

						return getOpenTypeAlternativeName(timestamp, (Type) referencedType, isStrange);
					}
				}
			} else {
				// the type comes from an information object [class]
				// examine the referenced type recursively
				final IReferenceChain chain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
				final IType referencedType = ((Referenced_Type) type).getTypeRefd(timestamp, chain);
				chain.release();

				return getOpenTypeAlternativeName(timestamp, (Type) referencedType, isStrange);
			}
		} else {
			final Identifier tmpId1 = new Identifier(Identifier_type.ID_NAME, type.getFullName());
			String s = tmpId1.getDisplayName();
			//module name will be cut off:
			if (s.startsWith("@") && s.indexOf('.') > 0) {
				s = s.substring(s.indexOf('.') + 1);
			}

			final Identifier tmpId2 = new Identifier(Identifier_type.ID_ASN, s);
			sb.append(tmpId2.getTtcnName());
		}
		// conversion to lower case initial:
		sb.replace(0, 1, sb.substring(0, 1).toLowerCase());
		// trick:
		final Identifier tmpId = new Identifier(Identifier_type.ID_NAME, sb.toString());
		return new Identifier(Identifier_type.ID_ASN, tmpId.getAsnName());
	}

	private void parseBlocks() {
		if (mObjectSetBlock == null) {
			return;
		}

		objectSet = null;
		atNotationList = null;
		if (null != mObjectSetBlock) {
			if (mAtNotationsBlock == null) {
				// SimpleTableConstraint
				final Asn1Parser parser = BlockLevelTokenStreamTracker.getASN1ParserForBlock(mObjectSetBlock, 0);
				if (parser != null) {
					objectSet = parser.pr_special_ObjectSetSpec().definition;
					final List<SyntacticErrorStorage> errors = parser.getErrorStorage();
					if (null != errors && !errors.isEmpty()) {
						objectSet = null;
						for (int i = 0; i < errors.size(); i++) {
							ParserMarkerSupport.createOnTheFlyMixedMarker((IFile) mObjectSetBlock.getLocation().getFile(), errors.get(i),
									IMarker.SEVERITY_ERROR);
						}
					}
				}
			} else {
				// ComponentRelationConstraint
				Asn1Parser parser = BlockLevelTokenStreamTracker.getASN1ParserForBlock(mObjectSetBlock, 0);
				if (parser != null) {
					objectSet = parser.pr_DefinedObjectSetBlock().objectSet;
					final List<SyntacticErrorStorage> errors = parser.getErrorStorage();
					if (null != errors && !errors.isEmpty()) {
						objectSet = null;
						for (int i = 0; i < errors.size(); i++) {
							ParserMarkerSupport.createOnTheFlySyntacticMarker((IFile) mObjectSetBlock.getLocation().getFile(), errors.get(i),
									IMarker.SEVERITY_ERROR);
						}
					}
				}
				parser = BlockLevelTokenStreamTracker.getASN1ParserForBlock(mAtNotationsBlock, 0);
				if (parser != null) {
					atNotationList = parser.pr_AtNotationList().notationList;
					final List<SyntacticErrorStorage> errors = parser.getErrorStorage();
					if (null != errors && !errors.isEmpty()) {
						objectSet = null;
						for (int i = 0; i < errors.size(); i++) {
							ParserMarkerSupport.createOnTheFlySyntacticMarker((IFile) mAtNotationsBlock.getLocation().getFile(), errors.get(i),
									IMarker.SEVERITY_ERROR);
						}
					}
				}
				if (atNotationList == null) {
					atNotationList = new AtNotations();
				}
			}
		}
		if (objectSet == null) {
			objectSet = new ObjectSet_definition();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (objectSet != null) {
			objectSet.findReferences(referenceFinder, foundIdentifiers);
		}
		if (atNotationList != null) {
			atNotationList.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (objectSet != null && !objectSet.accept(v)) {
			return false;
		}
		if (atNotationList != null && !atNotationList.accept(v)) {
			return false;
		}

		return true;
	}

}
