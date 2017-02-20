/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.properties.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles both the TTCN-3 preprocess and C/C++ preprocessor related property
 * settings of projects. Also loading, saving of these properties from and into
 * external formats.
 * 
 * @author Kristof Szabados
 * */
public final class PreprocessorIncludedOptionsData {
	public static final String TTCN3_PREPROCESSOR_INCLUDES_PROPERTY = "TTCN3preprocessorIncludes";
	public static final String TTCN3_PREPROCESSOR_INCLUDES_TAG = "TTCN3preprocessorIncludes";
	public static final String PREPROCESSOR_INCLUDES_PROPERTY = "preprocessorIncludes";
	public static final String PREPROCESSOR_INCLUDES_TAG = "preprocessorIncludes";

	private static final String[] PROPERTIES = { TTCN3_PREPROCESSOR_INCLUDES_PROPERTY, PREPROCESSOR_INCLUDES_PROPERTY };
	private static final String[] TAGS = { TTCN3_PREPROCESSOR_INCLUDES_TAG, PREPROCESSOR_INCLUDES_TAG };
	private static final String[] DEFAULT_VALUES = { "", "" };

	private PreprocessorIncludedOptionsData() {
		// Do nothing
	}

	public static String[] getTTCN3PreprocessorIncludes(final IProject project) {
		try {
			final String temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					PreprocessorIncludedOptionsData.TTCN3_PREPROCESSOR_INCLUDES_PROPERTY));
			return ListConverter.convertToList(temp);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace("While getting TTCN-3 preprocessor includes of `" + project.getName() + "'", e);
			return new String[] {};
		}
	}

	public static String[] getPreprocessorIncludes(final IProject project) {
		try {
			final String temp = project.getPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					PreprocessorIncludedOptionsData.PREPROCESSOR_INCLUDES_PROPERTY));
			return ListConverter.convertToList(temp);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace("While getting preprocessor includes of `" + project.getName() + "'", e);
			return new String[] {};
		}
	}

	/**
	 * Remove the TITAN provided attributes from a project.
	 * 
	 * @param project
	 *                the project to remove the attributes from.
	 * */
	public static void removeTITANAttributes(final IProject project) {
		for (int i = 0; i < PROPERTIES.length; i++) {
			try {
				project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER, PROPERTIES[i]), null);
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("While removing attribute " + PROPERTIES[i] + " of `" + project.getName() + "'",
						e);
			}
		}
	}

	/**
	 * Loads and sets the Makefile related settings contained in the XML
	 * tree for this project.
	 * 
	 * @see ProjectFileHandler#loadProjectSettings()
	 * @see ProjectBuildPropertyData#loadMakefileSettings(Node, IProject,
	 *      HashSet)
	 * 
	 * @param root
	 *                the root of the subtree containing Makefile related
	 *                attributes
	 * @param project
	 *                the project to set the found attributes on.
	 * */
	public static void loadMakefileSettings(final Node root, final IProject project) {
		final NodeList resourceList = root.getChildNodes();

		final String[] newValues = new String[TAGS.length];
		System.arraycopy(DEFAULT_VALUES, 0, newValues, 0, TAGS.length);

		for (int i = 0, size = resourceList.getLength(); i < size; i++) {
			final Node resource = resourceList.item(i);
			final String name = resource.getNodeName();
			for (int j = 0; j < TAGS.length; j++) {
				if (TAGS[j].equals(name)) {
					final NodeList subResources = resource.getChildNodes();
					Node subResource;
					final List<String> values = new ArrayList<String>();
					for (int i2 = 0; i2 < subResources.getLength(); i2++) {
						subResource = subResources.item(i2);
						if ("listItem".equals(subResource.getNodeName())) {
							values.add(subResource.getTextContent());
						}
					}

					newValues[j] = ListConverter.convertFromList(values.toArray(new String[values.size()]));
				}
			}
		}

		for (int i = 0; i < PROPERTIES.length; i++) {
			final QualifiedName qualifiedName = new QualifiedName(ProjectBuildPropertyData.QUALIFIER, PROPERTIES[i]);
			try {
				final String oldValue = project.getPersistentProperty(qualifiedName);
				if (newValues[i] != null && !newValues[i].equals(oldValue)) {
					project.setPersistentProperty(qualifiedName, newValues[i]);
				}
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("While loading property " + PROPERTIES[i] + " of `" + project.getName() + "'", e);
			}
		}
	}

	/**
	 * Creates an XML tree from the Makefile related settings of the
	 * project.
	 * 
	 * @see ProjectFileHandler#saveProjectSettings()
	 * @see ProjectBuildPropertyData#saveMakefileSettings(Document,
	 *      IProject)
	 * @see InternalMakefileCreationData#saveMakefileSettings(Element,
	 *      Document, IProject)
	 * 
	 * @param makefileSettings
	 *                the node to use as root node
	 * @param document
	 *                the document used for creating the tree nodes
	 * @param project
	 *                the project to work on
	 * */
	public static void saveMakefileSettings(final Element makefileSettings, final Document document, final IProject project) {
		for (int i = 0; i < TAGS.length; i++) {
			final QualifiedName name = new QualifiedName(ProjectBuildPropertyData.QUALIFIER, PROPERTIES[i]);
			try {
				final String temp = project.getPersistentProperty(name);
				final String[] tempList = ListConverter.convertToList(temp);

				if (tempList.length > 0) {
					final Element list = document.createElement(TAGS[i]);
					makefileSettings.appendChild(list);

					for (int j = 0; j < tempList.length; j++) {
						if (tempList[j] != null && tempList[j].length() > 0) {
							final Element item = document.createElement("listItem");
							item.appendChild(document.createTextNode(tempList[j]));

							list.appendChild(item);
						}
					}
				}
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("While saving property " + PROPERTIES[i] + " of `" + project.getName() + "'", e);
			}
		}
	}

	/**
	 * Copies the project information related to Makefiles from the source
	 * node to the target makefileSettings node.
	 * 
	 * @see ProjectFileHandler#copyProjectInfo(Node, Node, IProject,
	 *      TreeMap, TreeMap, boolean)
	 * 
	 * @param source
	 *                the node used as the source of the information.
	 * @param makefileSettings
	 *                the node used as the target of the operation.
	 * @param document
	 *                the document to contain the result, used to create the
	 *                XML nodes.
	 * @param saveDefaultValues
	 *                whether the default values should be forced to be
	 *                added to the output.
	 * */
	public static void copyMakefileSettings(final Node source, final Node makefileSettings, final Document document,
			final boolean saveDefaultValues) {
		final NodeList resourceList = source.getChildNodes();

		final String[] newValues = new String[TAGS.length];
		System.arraycopy(DEFAULT_VALUES, 0, newValues, 0, TAGS.length);

		for (int i = 0, size = resourceList.getLength(); i < size; i++) {
			final Node resource = resourceList.item(i);
			final String name = resource.getNodeName();
			for (int j = 0; j < TAGS.length; j++) {
				if (TAGS[j].equals(name)) {
					final NodeList subResources = resource.getChildNodes();
					Node subResource;
					final List<String> values = new ArrayList<String>();
					for (int i2 = 0; i2 < subResources.getLength(); i2++) {
						subResource = subResources.item(i2);
						if ("listItem".equals(subResource.getNodeName())) {
							values.add(subResource.getTextContent());
						}
					}

					newValues[j] = ListConverter.convertFromList(values.toArray(new String[values.size()]));
				}
			}
		}

		for (int i = 0; i < TAGS.length; i++) {
			final String temp = newValues[i];
			final String[] tempList = ListConverter.convertToList(temp);

			if (saveDefaultValues || tempList.length > 0) {
				final Element list = document.createElement(TAGS[i]);
				makefileSettings.appendChild(list);

				for (int j = 0; j < tempList.length; j++) {
					if (tempList[j] != null && tempList[j].length() > 0) {
						final Element item = document.createElement("listItem");
						item.appendChild(document.createTextNode(tempList[j]));

						list.appendChild(item);
					}
				}
			}
		}
	}
}
