/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1.Object;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.titan.common.parsers.SyntacticErrorStorage;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IReferenceChainElement;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.ASN1Object;
import org.eclipse.titan.designer.AST.ASN1.Block;
import org.eclipse.titan.designer.AST.ASN1.IObjectSet_Element;
import org.eclipse.titan.designer.AST.ASN1.ObjectSet;
import org.eclipse.titan.designer.compiler.BuildTimestamp;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ParserMarkerSupport;
import org.eclipse.titan.designer.parsers.asn1parser.Asn1Parser;
import org.eclipse.titan.designer.parsers.asn1parser.BlockLevelTokenStreamTracker;

/**
 * ObjectSet definition.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class ObjectSet_definition extends ObjectSet implements IReferenceChainElement {

	private final Block mBlock;

	private List<IObjectSet_Element> objectSetElements;
	private ASN1Objects objects;

	/** the time when code for this type was generated. */
	private BuildTimestamp lastTimeGenerated = null;

	public ObjectSet_definition() {
		objectSetElements = new ArrayList<IObjectSet_Element>();
		mBlock = null;
	}

	public ObjectSet_definition(final Block aBlock) {
		objectSetElements = new ArrayList<IObjectSet_Element>();
		this.mBlock = aBlock;
	}

	public ObjectSet_definition(final ASN1Objects objects) {
		objectSetElements = new ArrayList<IObjectSet_Element>();
		mBlock = null;
		this.objects = objects;
	}

	public ObjectSet_definition newInstance() {
		ObjectSet_definition temp;
		if (null != mBlock) {
			temp = new ObjectSet_definition(mBlock);
		} else if (null != objects) {
			temp = new ObjectSet_definition(objects);
		} else {
			temp = new ObjectSet_definition();
		}

		for (int i = 0; i < objectSetElements.size(); i++) {
			temp.addObjectSetElement(objectSetElements.get(i).newOseInstance());
		}
		temp.getObjectSetElements();

		return temp;
	}

	public final List<IObjectSet_Element> getObjectSetElements() {
		return objectSetElements;
	}

	public final void setObjectSetElements(final List<IObjectSet_Element> objectSetElements) {
		this.objectSetElements = objectSetElements;
	}

	@Override
	/** {@inheritDoc} */
	public final String chainedDescription() {
		return getFullName();
	}

	@Override
	/** {@inheritDoc} */
	public final Location getChainLocation() {
		return getLocation();
	}

	public final void steelObjectSetElements(final ObjectSet_definition definition) {
		if (null == definition) {
			return;
		}

		for (int i = 0; i < definition.getObjectSetElements().size(); i++) {
			addObjectSetElement(definition.getObjectSetElements().get(i).newOseInstance());
		}

		definition.getObjectSetElements().clear();
	}

	@Override
	/** {@inheritDoc} */
	public final StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0; i < objectSetElements.size(); i++) {
			if (objectSetElements.get(i) == child) {
				return builder.append(INamedNode.DOT).append(String.valueOf(i + 1));
			}
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public final void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		for (final IObjectSet_Element element : objectSetElements) {
			element.setMyScopeOse(scope);
		}
	}

	public final void addObjectSetElement(final IObjectSet_Element element) {
		if (null != element) {
			element.setFullNameParent(this);
			objectSetElements.add(element);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		isErroneous = false;

		if (null != mBlock && lastTimeChecked == null) {
			parseBlockObjectSetSpecifications();
		}

		final ObjectSetElementVisitor_checker checker = new ObjectSetElementVisitor_checker(timestamp, location, myGovernor);
		for (int i = 0; i < objectSetElements.size(); i++) {
			final IObjectSet_Element element = objectSetElements.get(i);
			element.setGenNameOse(getGenNameOwn(), Integer.toString(i + 1).toString());
			element.accept(checker);
		}

		lastTimeChecked = timestamp;

		createObjects(true);
	}

	@Override
	/** {@inheritDoc} */
	public final ObjectSet_definition getRefdLast(final CompilationTimeStamp timestamp, final IReferenceChain referenceChain) {
		if (1 != objectSetElements.size()) {
			return this;
		}

		final IObjectSet_Element element = objectSetElements.get(0);
		if (!(element instanceof Referenced_ObjectSet)) {
			return this;
		}

		final boolean newChain = null == referenceChain;
		IReferenceChain temporalReferenceChain;
		if (newChain) {
			temporalReferenceChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
		} else {
			temporalReferenceChain = referenceChain;
		}

		temporalReferenceChain.add(this);
		final ObjectSet_definition result = ((Referenced_ObjectSet) element).getRefdLast(timestamp, temporalReferenceChain);

		if (newChain) {
			temporalReferenceChain.release();
		}

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public final int getNofObjects() {
		createObjects(false);

		return objects.getNofObjects();
	}

	@Override
	/** {@inheritDoc} */
	public final ASN1Object getObjectByIndex(final int index) {
		createObjects(false);

		return objects.getObjectByIndex(index);
	}

	public final ASN1Objects getObjs() {
		createObjects(false);

		return objects;
	}

	@Override
	/** {@inheritDoc} */
	public final void accept(final ObjectSetElementVisitor_objectCollector v) {
		v.visitObjectSet(this, false);
	}

	private void parseBlockObjectSetSpecifications() {
		if (mBlock == null) {
			return;
		}

		final Asn1Parser parser = BlockLevelTokenStreamTracker.getASN1ParserForBlock(mBlock);
		if (parser == null) {
			return;
		}

		final ObjectSet_definition temporalDefinition = parser.pr_special_ObjectSetSpec().definition;
		// internalIndex += parser.nof_consumed_tokens();
		final List<SyntacticErrorStorage> errors = parser.getErrorStorage();
		if (null != errors && !errors.isEmpty()) {
			for (int i = 0; i < errors.size(); i++) {
				ParserMarkerSupport.createOnTheFlyMixedMarker((IFile) mBlock.getLocation().getFile(), errors.get(i),
						IMarker.SEVERITY_ERROR);
			}
		}

		if (null == temporalDefinition) {
			isErroneous = true;
			return;
		}

		final List<IObjectSet_Element> elements = temporalDefinition.getObjectSetElements();
		for (int i = 0; i < elements.size(); i++) {
			addObjectSetElement(elements.get(i));
		}
		temporalDefinition.setObjectSetElements(null);

		setMyScope(getMyScope());
	}

	protected final void createObjects(final boolean force) {
		if (null != objects && !force) {
			return;
		}

		if (null == myGovernor) {
			return;
		}

		if (null == lastTimeChecked) {
			check(CompilationTimeStamp.getBaseTimestamp());
		}

		final ObjectSetElementVisitor_objectCollector visitor = new ObjectSetElementVisitor_objectCollector(this, lastTimeChecked);
		for (int i = 0; i < objectSetElements.size(); i++) {
			objectSetElements.get(i).accept(visitor);
		}
		objects = visitor.getObjects();
		objects.trimToSize();
	}

	@Override
	/** {@inheritDoc} */
	public final void addProposal(final ProposalCollector propCollector, final int index) {
		final List<ISubReference> subreferences = propCollector.getReference().getSubreferences();
		if (subreferences.size() <= index) {
			return;
		}

		final ISubReference subreference = subreferences.get(index);
		if (Subreference_type.fieldSubReference.equals(subreference.getReferenceType())) {
			if (subreferences.size() > index + 1) {
				// the reference might go on
				Object_Definition def;
				for (int j = 0; j < objects.getNofObjects(); j++) {
					def = objects.getObjectByIndex(j);
					def.addProposal(propCollector, index + 1);
				}
			} else {
				// final part of the reference
				Object_Definition def;
				for (int j = 0; j < objects.getNofObjects(); j++) {
					def = objects.getObjectByIndex(j);
					def.addProposal(propCollector, index);
				}
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public final void addDeclaration(final DeclarationCollector declarationCollector, final int index) {
		final List<ISubReference> subreferences = declarationCollector.getReference().getSubreferences();
		if (subreferences.size() <= index) {
			return;
		}

		final ISubReference subreference = subreferences.get(index);
		if (Subreference_type.fieldSubReference.equals(subreference.getReferenceType())) {
			if (subreferences.size() > index + 1) {
				// the reference might go on
				Object_Definition def;
				for (int j = 0; j < objects.getNofObjects(); j++) {
					def = objects.getObjectByIndex(j);
					def.addDeclaration(declarationCollector, index + 1);
				}
			} else {
				// final part of the reference
				Object_Definition def;
				for (int j = 0; j < objects.getNofObjects(); j++) {
					def = objects.getObjectByIndex(j);
					def.addDeclaration(declarationCollector, index);
				}
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	protected final boolean memberAccept(final ASTVisitor v) {
		if (objectSetElements != null) {
			for (final IObjectSet_Element element : objectSetElements) {
				if (!element.memberAccept(v)) {
					return false;
				}
			}
		}

		if (objects != null && !objects.accept(v)) {
			return false;
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData) {
		if (lastTimeGenerated != null && !lastTimeGenerated.isLess(aData.getBuildTimstamp())) {
			return;
		}

		lastTimeGenerated = aData.getBuildTimstamp();

		final ObjectSetElementVisitor_codeGen osev = new ObjectSetElementVisitor_codeGen(this, aData);
		if (objectSetElements != null) {
			for (final IObjectSet_Element element : objectSetElements) {
				element.accept(osev);
			}
		}
	}
}
