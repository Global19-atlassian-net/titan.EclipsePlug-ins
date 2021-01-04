/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.wizards.projectFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.path.TITANPathUtilities;
import org.eclipse.titan.common.path.TitanURIUtil;
import org.eclipse.titan.common.utils.IOUtils;
import org.eclipse.titan.designer.Activator;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.consoles.TITANDebugConsole;
import org.eclipse.titan.designer.core.LoadBalancingUtilities;
import org.eclipse.titan.designer.core.TITANNature;
import org.eclipse.titan.designer.graphics.ImageCache;
import org.eclipse.titan.designer.productUtilities.ProductConstants;
import org.eclipse.titan.designer.properties.data.DOMErrorHandlerImpl;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;
import org.eclipse.titan.designer.properties.data.ProjectDocumentHandlingUtility;
import org.eclipse.titan.designer.properties.data.ProjectFileHandler;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.IProgressConstants;
import org.osgi.framework.Bundle;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.xml.sax.SAXException;

/**
 * This class should be the importation of modules described in a provided Tpd
 * file.
 * 
 * @author Kristof Szabados
 * */
public class TpdImporter {
	private static final String CREATING_PROJECT = "creating project";
	private static final String CREATION_FAILED = "Project creation failed";
	private static final String TPD_XSD = "schema/TPD.xsd";

	private DOMImplementationLS domImplLS;
	private LSParser parser;
	private DOMConfiguration config;

	private final Map<String, String> finalProjectNames = new HashMap<String, String>();
	private final Map<URI, Document> projectsToImport = new HashMap<URI, Document>();
	private final List<URI> importChain = new ArrayList<URI>();
	private final boolean wasAutoBuilding;
	private final Shell shell;
	private final boolean headless;
	private List<String> searchPaths;
	private final Map<String, String> tpdNameAttrMap = new HashMap<String, String>();
	private final Map<String, String> tpdURIMap = new HashMap<String, String>();

	public TpdImporter(final Shell shell, final boolean headless) {
		this.shell = shell;
		this.headless = headless;
		final IWorkspaceDescription description = ResourcesPlugin.getWorkspace().getDescription();
		wasAutoBuilding = description.isAutoBuilding();
		description.setAutoBuilding(false);
		try {
			ResourcesPlugin.getWorkspace().setDescription(description);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace("while disabling autobuild on the workspace", e);
		}
		if (!headless) {
			Activator.getDefault().pauseHandlingResourceChanges();
		}
	}

	/**
	 * Internal function used to do the import job. It is needed to extract this
	 * functionality in order to be able to handle erroneous situations.
	 *
	 * @param projectFile
	 *            the file path string of the project descriptor file (tpd)
	 * @param projectsCreated
	 *            the list of projects created so far. In case of problems we
	 *            will try to delete them.
	 * @param monitor
	 *            the monitor used to report progress.
	 *
	 * @return true if the import was successful, false otherwise.
	 * */
	public boolean internalFinish(final String projectFile, final boolean isSkipExistingProjects,
			final boolean isOpenPropertiesForAllImports, final List<IProject> projectsCreated, final IProgressMonitor monitor,
			final List<String> searchPaths) {
		if (projectFile == null || "".equals(projectFile.trim())) {
			return false;
		}
		if(searchPaths != null) {
			this.searchPaths = new ArrayList<String>(searchPaths);
		}
		System.setProperty(DOMImplementationRegistry.PROPERTY, ProjectFormatConstants.DOM_IMPLEMENTATION_SOURCE);
		DOMImplementationRegistry registry = null;
		try {
			registry = DOMImplementationRegistry.newInstance();
		} catch (Exception e) {
			ErrorReporter.logExceptionStackTrace("While importing from `" + projectFile + "'", e);
			activatePreviousSettings();
			return false;
		}

		// Specifying "LS 3.0" in the features list ensures that the
		// DOMImplementation
		// object implements the load and save features of the DOM 3.0
		// specification.
		final DOMImplementation domImpl = registry.getDOMImplementation(ProjectFormatConstants.LOAD_SAVE_VERSION);
		domImplLS = (DOMImplementationLS) domImpl;
		// If the mode is MODE_SYNCHRONOUS, the parse and parseURI
		// methods of the LSParser
		// object return the org.w3c.dom.Document object. If the mode is
		// MODE_ASYNCHRONOUS, the parse and parseURI methods return null.
		parser = domImplLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, ProjectFormatConstants.XML_SCHEMA);

		config = parser.getDomConfig();
		final DOMErrorHandlerImpl errorHandler = new DOMErrorHandlerImpl();
		config.setParameter("error-handler", errorHandler);
		config.setParameter("validate", Boolean.TRUE);
		config.setParameter("schema-type", ProjectFormatConstants.XML_SCHEMA);
		config.setParameter("well-formed", Boolean.TRUE);
		config.setParameter("validate-if-schema", Boolean.TRUE);

		Validator tpdValidator = null;
		try {
			final Schema tpdXsd = getTPDSchema();
			tpdValidator = tpdXsd.newValidator();
		} catch (Exception e) {
			ErrorReporter.INTERNAL_ERROR(e.getMessage());
			// Hint: cp $TTCN3_DIR/etc/xsd/TPD.xsd designer/schema/
		}

		final URI resolvedProjectFileURI = TITANPathUtilities.resolvePath(projectFile, (URI) null);
		//====================================
		// Loading all URI Documents (tpds) 
		// and collect projects to be imported
		//====================================
		if (!loadURIDocuments(resolvedProjectFileURI, tpdValidator)) {
			return false;
		}

		final SubMonitor progress = SubMonitor.convert(monitor, 3);
		progress.setTaskName("Loading data");

		final IProgressMonitor projectCreationMonitor = progress.newChild(1);
		projectCreationMonitor.beginTask("Creating required projects", projectsToImport.size());
		//========================
		// Create projects and 
		// store load location 
		// (where they are loaded from)
		//========================
		final Map<URI, IProject> projectMap = new ConcurrentHashMap<URI, IProject>();
		final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r);
				t.setPriority(LoadBalancingUtilities.getThreadPriority());
				return t;
			}
		});

		final AtomicBoolean isErroneous = new AtomicBoolean(false);
		final LinkedBlockingDeque<IProject> projectsCreatedTemp = new LinkedBlockingDeque<IProject>();
		final CountDownLatch latch = new CountDownLatch(projectsToImport.size());
		for (final URI file : projectsToImport.keySet()) {
			final Document actualDocument = projectsToImport.get(file);

			executor.execute(new Runnable() {
				@Override
				public void run() {
					final IProject project = createProject(actualDocument.getDocumentElement(), file.equals(resolvedProjectFileURI) || !isSkipExistingProjects);
					if (project == null) {
						if (file.equals(resolvedProjectFileURI)) {
							isErroneous.set(true);
							latch.countDown();
							return;
						} else {
							latch.countDown();
							return;
						}
					}
					projectsCreatedTemp.add(project);
					projectMap.put(file, project);
					try {
						project.setPersistentProperty(
								new QualifiedName(ProjectBuildPropertyData.QUALIFIER, ProjectBuildPropertyData.LOAD_LOCATION), file.getPath()
										.toString());
					} catch (CoreException e) {
						ErrorReporter.logExceptionStackTrace("While loading referenced project from `" + file.getPath() + "'", e);
					} finally {
						latch.countDown();
					}
				}
			});
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		executor.shutdown();
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}

		executor.shutdownNow();
		projectsCreated.addAll(projectsCreatedTemp);
		projectCreationMonitor.done();
		if (isErroneous.get()) {
			return false;
		}

		final IProgressMonitor normalInformationLoadingMonitor = progress.newChild(1);
		normalInformationLoadingMonitor.beginTask("Loading directly stored project information", projectsToImport.size());

		//====================================
		//Load Project Data from all projects:
		//====================================
		final ExecutorService executor2 = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r);
				t.setPriority(LoadBalancingUtilities.getThreadPriority());
				return t;
			}
		});

		final CountDownLatch latch2 = new CountDownLatch(projectsToImport.size());
		for (final URI file : projectsToImport.keySet()) {
			executor2.execute(new Runnable() {
				@Override
				public void run() {
					if (!projectMap.containsKey(file)) {
						latch2.countDown();
						return;
					}
		
					final IProject project = projectMap.get(file);
					final IPath projectFileFolderPath = new Path(file.getPath()).removeLastSegments(1);
					final URI projectFileFolderURI = URIUtil.toURI(projectFileFolderPath);
					final Document actualDocument = projectsToImport.get(file);
		
					if (searchPaths != null && !searchPaths.isEmpty()) {
						final String tpdNameAttrVal = tpdNameAttrMap.get(project.getName());
						final String tpdURIVal = tpdURIMap.get(project.getName());
						if (tpdNameAttrVal != null) {
							try {
								project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER, ProjectBuildPropertyData.USE_TPD_NAME),
										tpdNameAttrVal);
							} catch (CoreException e) {
								ErrorReporter.logExceptionStackTrace("While setting `useTpdName' for project `" + project.getName() + "'", e);
							}
						}
						if (tpdURIVal != null) {
							try {
								project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER, ProjectBuildPropertyData.ORIG_TPD_URI),
										tpdURIVal);
							} catch (CoreException e) {
								ErrorReporter.logExceptionStackTrace("While setting `origTpdURI' for project `" + project.getName() + "'", e);
							}
						}
					}
		
					final Element mainElement = actualDocument.getDocumentElement();
					//=== Get the copyright text ===
					final Node node = mainElement.getFirstChild();
		
					String commentStr = ""; //default value. This will be changed for PreferenceConstants.COPYRIGHT_DEFAULT_STRING at export
					if( node !=null && node.getNodeType() == Element.COMMENT_NODE ){
						//process comment node
						final Comment comment = (Comment) node;
						commentStr = comment.getData();
					}
					try {
						project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER, ProjectBuildPropertyData.PROJECT_COPYRIGHT_STRING_ID),
								commentStr);
					} catch (CoreException e) {
						ErrorReporter.logExceptionStackTrace("While setting `copyright string' for project `" + project.getName() + "'", e);
					}
		
					if (!loadProjectDataFromNode(mainElement, project, projectFileFolderURI)) {
						latch2.countDown();
						isErroneous.set(true);
						return;
					}
		
					latch2.countDown();
				}
			});
		}
		try {
			latch2.await();
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		executor2.shutdown();
		try {
			executor2.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		executor2.shutdownNow();
		if (isErroneous.get()) {
			return false;
		}
		normalInformationLoadingMonitor.done();
		//=====================================
		//Load information from packed projects
		//=====================================
		final IPath mainProjectFileFolderPath = new Path(resolvedProjectFileURI.getPath()).removeLastSegments(1);
		final URI mainProjectFileFolderURI = URIUtil.toURI(mainProjectFileFolderPath);

		final List<Node> packedProjects = loadPackedProjects(projectsToImport.get(resolvedProjectFileURI));
		final IProgressMonitor packedInformationLoadingMonitor = progress.newChild(1);
		packedInformationLoadingMonitor.beginTask("Loading packed project information", packedProjects.size());
		for (final Node node : packedProjects) {
			final IProject project = createProject(node, false);
			if (project == null) {
				packedInformationLoadingMonitor.worked(1);
				continue;
			}
			projectsCreated.add(project);

			try {
				project.setPersistentProperty(
						new QualifiedName(ProjectBuildPropertyData.QUALIFIER, ProjectBuildPropertyData.LOAD_LOCATION),
						resolvedProjectFileURI.toString());
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("While loading packed project `" + project.getName() + "'", e);
			}

			if (!loadProjectDataFromNode(node, project, mainProjectFileFolderURI)) {
				return false;
			}

			packedInformationLoadingMonitor.worked(1);
		}
		packedInformationLoadingMonitor.done();

		final IProject mainProject = projectMap.get(resolvedProjectFileURI);
		if (mainProject == null) {
			progress.done();
			return false;
		}
		try {
			mainProject.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER, ProjectBuildPropertyData.USE_TPD_NAME),
					mainProject.getName() + ".tpd");
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace("While setting `useTpdName' for project `" + mainProject.getName() + "'", e);
		}
		final List<WorkspaceJob> jobs = new ArrayList<WorkspaceJob>();
		List<IProject> projectsToBeConfigured;
		if (isOpenPropertiesForAllImports) {
			projectsToBeConfigured = projectsCreated;
		} else {
			projectsToBeConfigured = new ArrayList<IProject>();
			projectsToBeConfigured.add(mainProject);
		}

		if (!headless) {
			for (final IProject project : projectsToBeConfigured) {
				final WorkspaceJob loadJob = new WorkspaceJob("Property initilizer for " + project.getName()) {
					@Override
					public IStatus runInWorkspace(final IProgressMonitor monitor) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(null, project,
										GeneralConstants.PROJECT_PROPERTY_PAGE, null, null);
								if (dialog != null) {
									dialog.open();
								}
							}
						});
						return Status.OK_STATUS;
					}
				};
				loadJob.setUser(false);
				loadJob.setSystem(true);
				loadJob.setRule(project.getWorkspace().getRuleFactory().refreshRule(project));
				loadJob.setProperty(IProgressConstants.ICON_PROPERTY, ImageCache.getImageDescriptor("titan.gif"));
				loadJob.schedule();
				jobs.add(loadJob);
			}

			for (final WorkspaceJob job : jobs) {
				try {
					job.join();
				} catch (InterruptedException e) {
					ErrorReporter.logExceptionStackTrace("Interrupted while performing: " + job.getName(), e);
				}
			}
		}

		activatePreviousSettings();

		progress.done();
		return true;
	}

	public static void validateTpd(final File tpdFile) throws IOException, SAXException {
		final Schema tpdXsd = getTPDSchema();
		final Validator validator = tpdXsd.newValidator();
		validator.validate(new StreamSource(tpdFile));
	}

	public static Schema getTPDSchema() throws IOException, SAXException {
		final Bundle bundle = Platform.getBundle(ProductConstants.PRODUCT_ID_DESIGNER);
		InputStream xsdInputStream = null;
		try {
			xsdInputStream = FileLocator.openStream(bundle, new Path(TPD_XSD), false);
			final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			final Schema result = factory.newSchema(new StreamSource(xsdInputStream));
			xsdInputStream.close();
			return result;
		} finally {
			IOUtils.closeQuietly(xsdInputStream);
		}
	}

	private void activatePreviousSettings() {
		final IWorkspaceDescription description = ResourcesPlugin.getWorkspace().getDescription();
		if (description.isAutoBuilding() != wasAutoBuilding) {
			description.setAutoBuilding(wasAutoBuilding);
			try {
				ResourcesPlugin.getWorkspace().setDescription(description);
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("Resetting autobuild settings to" + wasAutoBuilding, e);
			}
		}
		if (!headless) {
			Activator.getDefault().resumeHandlingResourceChanges();
		}
	}

	/**
	 * Collects the list of packed projects from the provided document.
	 *
	 * @param document
	 *            the document to check.
	 *
	 * @return the list of found packed projects, an empty list if none.
	 * */
	private List<Node> loadPackedProjects(final Document document) {
		final NodeList referencedProjectsList = document.getDocumentElement().getChildNodes();
		final Node packed = ProjectFileHandler.getNodebyName(referencedProjectsList, ProjectFormatConstants.PACKED_REFERENCED_PROJECTS_NODE);
		if (packed == null) {
			return new ArrayList<Node>();
		}

		final List<Node> result = new ArrayList<Node>();
		final NodeList projects = packed.getChildNodes();
		for (int i = 0, size = projects.getLength(); i < size; i++) {
			final Node referencedProjectNode = projects.item(i);
			if (ProjectFormatConstants.PACKED_REFERENCED_PROJECT_NODE.equals(referencedProjectNode.getNodeName())) {
				result.add(referencedProjectNode);
			}
		}

		return result;
	}

	/**
	 * Loads the project data from the provided node onto the provided project.
	 *
	 * @param mainElement
	 *            the node to load the data from.
	 * @param project
	 *            the project to set the loaded data on.
	 * @param projectFileFolderURI
	 *            the URI of the folder to calculate all paths relative to.
	 *
	 * @return true if the import was successful, false otherwise.
	 * */
	private boolean loadProjectDataFromNode(final Node mainElement, final IProject project, final URI projectFileFolderURI) {
		final NodeList mainNodes = mainElement.getChildNodes();

		final Node referencedProjectsNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.REFERENCED_PROJECTS_NODE);
		if (referencedProjectsNode != null) {
			if (!loadReferencedProjectsData(referencedProjectsNode, project)) {
				return false;
			}
		}

		final Node pathVariablesNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.PATH_VARIABLES);
		if (pathVariablesNode != null) {
			if (!loadPathVariables(pathVariablesNode, project.getName())) {
				return false;
			}
		}

		final Node foldersNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.FOLDERS_NODE);
		if (foldersNode != null) {
			if (!loadFoldersData(foldersNode, project, projectFileFolderURI)) {
				return false;
			}
		}

		final Node filesNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.FILES_NODE);
		if (filesNode != null) {
			if (!loadFilesData(filesNode, project, projectFileFolderURI)) {
				return false;
			}
		}

		ProjectDocumentHandlingUtility.createDocument(project);

		if (!loadConfigurationData(project, mainNodes)) {
			return false;
		}

		return true;
	}

	/**
	 * Load the data related to project references.
	 *
	 * @param referencedProjectsNode
	 *            the node containing information on referenced projects.
	 * @param project
	 *            the project to set the data on.
	 *
	 * @return true if the import was successful, false otherwise.
	 * */
	private boolean loadReferencedProjectsData(final Node referencedProjectsNode, final IProject project) {
		final NodeList referencedProjectsList = referencedProjectsNode.getChildNodes();
		final LinkedBlockingDeque<IProject> referencedProjects = new LinkedBlockingDeque<IProject>();
		final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r);
				t.setPriority(LoadBalancingUtilities.getThreadPriority());
				return t;
			}
		});

		final AtomicBoolean isErroneous = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(referencedProjectsList.getLength());
		for (int i = 0, size = referencedProjectsList.getLength(); i < size; i++) {
			final Node referencedProjectNode = referencedProjectsList.item(i);
			final int index = i;

			executor.execute(new Runnable() {
				@Override
				public void run() {
					if (referencedProjectNode.getNodeType() != Node.ELEMENT_NODE) {
						latch.countDown();
						return;
					}
		
					final NamedNodeMap attributeMap = referencedProjectNode.getAttributes();
					if (attributeMap == null) {
						latch.countDown();
						return;
					}
		
					final Node nameNode = attributeMap.getNamedItem(ProjectFormatConstants.REFERENCED_PROJECT_NAME_ATTRIBUTE);
					if (nameNode == null) {
						displayError("Import failed", "Error while importing project " + project.getName() + " the name attribute of the " + index
								+ " th referenced project is missing");
						isErroneous.set(true);
						latch.countDown();
						return;
					}
		
					final String projectName = nameNode.getTextContent();
					final String realProjectName = finalProjectNames.get(projectName);
					final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
					if (realProjectName != null && realProjectName.length() > 0) {
						final IProject tempProject = workspaceRoot.getProject(realProjectName);
						referencedProjects.add(tempProject);
					} else {
						//already existing projects:
						final IProject tempProject = workspaceRoot.getProject(projectName);
						referencedProjects.add(tempProject);
					}

					latch.countDown();
				}
			});
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		executor.shutdown();
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		executor.shutdownNow();
		if (isErroneous.get()) {
			return false;
		}

		try {
			final IProjectDescription description = project.getDescription();
			description.setReferencedProjects(referencedProjects.toArray(new IProject[referencedProjects.size()]));
			project.setDescription(description, null);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace("While setting project references for `" + project.getName() + "'", e);
			return false;
		}

		return true;
	}

	/**
	 * Load the information describing folders.
	 *
	 * @param foldersNode
	 *            the node to load from.
	 * @param project
	 *            the project to set this information on.
	 * @param projectFileFolderURI
	 *            the location of the project file's folder.
	 *
	 * @return true if the import was successful, false otherwise.
	 * */
	private boolean loadFoldersData(final Node foldersNode, final IProject project, final URI projectFileFolderURI) {
		final URI projectLocationURI = project.getLocationURI();
		final NodeList folderNodeList = foldersNode.getChildNodes();

		for (int i = 0, size = folderNodeList.getLength(); i < size; i++) {
			final Node folderItem = folderNodeList.item(i);
			if (folderItem.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			final NamedNodeMap attributeMap = folderItem.getAttributes();
			if (attributeMap == null) {
				continue;
			}

			final Node projectRelativePathNode = attributeMap.getNamedItem(ProjectFormatConstants.FOLDER_ECLIPSE_LOCATION_NODE);
			if (projectRelativePathNode == null) {
				displayError("Import failed", "Error while importing project " + project.getName()
						+ " the project relative path attribute of the " + i + " th folder is missing");
				return false;
			}

			final String projectRelativePath = projectRelativePathNode.getTextContent();

			final Node relativeURINode = attributeMap.getNamedItem(ProjectFormatConstants.FOLDER_RELATIVE_LOCATION);
			final Node rawURINode = attributeMap.getNamedItem(ProjectFormatConstants.FOLDER_RAW_LOCATION);

			final IFolder folder = project.getFolder(projectRelativePath);
			if (!folder.exists()) {
				try {
					if (relativeURINode != null) {
						final String relativeLocation = relativeURINode.getTextContent();
						//if relativeLocation == "virtual:/virtual" then
						//create a workaround according to the rawURI branch
						if( "virtual:/virtual".equals(relativeLocation) ) {
							folder.createLink(URI.create(relativeLocation), IResource.ALLOW_MISSING_LOCAL, null);
							continue;
						}

						final URI absoluteURI = TITANPathUtilities.resolvePathURI(relativeLocation, URIUtil.toPath(projectFileFolderURI).toOSString());
						if (absoluteURI == null) {
							// The URI cannot be resolved - for example it
							// contains not existing environment variables
							continue; 
						}
						if (TitanURIUtil.isPrefix(projectLocationURI, absoluteURI)) {
							folder.create(false, true, null);
						} else {
							final File tmpFolder = new File(absoluteURI);
							if (tmpFolder.exists()) {
								folder.createLink(absoluteURI, IResource.ALLOW_MISSING_LOCAL, null);
							} else {
								ErrorReporter.logError("Error while importing folders into project `" + project.getName() + "'. Folder `"
										+ absoluteURI + "' does not exist");
								continue;
							}
						}
					} else if (rawURINode != null) {
						final String rawLocation = rawURINode.getTextContent();
						folder.createLink(URI.create(rawLocation), IResource.ALLOW_MISSING_LOCAL, null);
					} else {
						TITANDebugConsole.println("Cannot create the resource " + folder.getFullPath().toString()
								+ " the location information is missing or corrupted");
					}
				} catch (CoreException e) {
					ErrorReporter.logError("Error while importing folders into project: " + e );
					//be silent, it can happen normally
				}
			} else {
				ErrorReporter.logWarning("Folder to be imported `" + folder.getName() + "' already exists in project `" + project.getName()
						+ "'");
			}
		}
		return true;
	}

	/**
	 * Load the information describing files.
	 *
	 * @param filesNode
	 *            the node to load from.
	 * @param project
	 *            the project to set this information on.
	 * @param projectFileFolderURI
	 *            the location of the project file's folder.
	 *
	 * @return true if the import was successful, false otherwise.
	 * */
	private boolean loadFilesData(final Node filesNode, final IProject project, final URI projectFileFolderURI) {
		final NodeList fileNodeList = filesNode.getChildNodes();
		final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r);
				t.setPriority(LoadBalancingUtilities.getThreadPriority());
				return t;
			}
		});

		final AtomicBoolean isErroneous = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(fileNodeList.getLength());
		for (int i = 0, size = fileNodeList.getLength(); i < size; i++) {
			final Node fileItem = fileNodeList.item(i);
			final int index = i;

			executor.execute(new Runnable() {
				@Override
				public void run() {

					if (fileItem.getNodeType() != Node.ELEMENT_NODE) {
						latch.countDown();
						return;
					}

					final NamedNodeMap attributeMap = fileItem.getAttributes();
					if (attributeMap == null) {
						// there is no attribute, check next node
						latch.countDown();
						return;
					}

					final Node projectRelativePathNode = attributeMap.getNamedItem(ProjectFormatConstants.FILE_ECLIPSE_LOCATION_NODE);
					if (projectRelativePathNode == null) {
						displayError("Import failed", "Error while importing project " + project.getName() + " some attributes of the " + index
								+ " th file are missing");
						latch.countDown();
						return;
					}

					final String projectRelativePath = projectRelativePathNode.getTextContent();

					final Node relativeURINode = attributeMap.getNamedItem(ProjectFormatConstants.FILE_RELATIVE_LOCATION);
					final Node rawURINode = attributeMap.getNamedItem(ProjectFormatConstants.FILE_RAW_LOCATION);

					final IFile targetFile = project.getFile(projectRelativePath);
					if (!targetFile.exists()) {
						try {
							if (relativeURINode != null) {
								final String relativeLocation = relativeURINode.getTextContent();
								//perhaps the next few lines should be implemented as in the function loadFoldersData()
								final URI absoluteURI = TITANPathUtilities.resolvePathURI(relativeLocation, URIUtil.toPath(projectFileFolderURI).toOSString());
								if (absoluteURI == null) {
									ErrorReporter.logError("Error while importing files into project `" + project.getName() + "'. File `"
											+ absoluteURI + "' does not exist!");
									latch.countDown();
									return;
								}

								final File file = new File(absoluteURI);
								if (file.exists()) {
									targetFile.createLink(absoluteURI, IResource.ALLOW_MISSING_LOCAL, null);
								} else {
									ErrorReporter.logError("Error while importing files into project `" + project.getName() + "'. File `"
											+ absoluteURI + "' does not exist");
									latch.countDown();
									return;
								}
							} else if (rawURINode != null) {
								final String rawURI = rawURINode.getTextContent();
								targetFile.createLink(URI.create(rawURI), IResource.ALLOW_MISSING_LOCAL, null);
							} else {
								TITANDebugConsole.println("Can not create the resource " + targetFile.getFullPath().toString()
										+ " the location information is missing or corrupted");
								latch.countDown();
								return;
							}
						} catch (CoreException e) {
							ErrorReporter.logExceptionStackTrace("While creating link for `" + targetFile + "'", e);
							isErroneous.set(true);
							latch.countDown();
							return;
						}
					}

					latch.countDown();
				}
			});
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		executor.shutdown();
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			ErrorReporter.logExceptionStackTrace(e);
		}
		executor.shutdownNow();
		if (isErroneous.get()) {
			return false;
		}

		return true;
	}

	// Perhaps variableValue is not in a form of URI (with optional file
	// scheme) or Path and it shall be converted in more steps:
	private URI convertPathOrUriStringToURI(final String pathOrUriString) throws URISyntaxException {
		final URI uri = new URI(pathOrUriString);
		if (uri.getScheme() == null || "file".equals(uri.getScheme())) {
			return uri;
		} else {
			final Path tempPath = new Path(pathOrUriString);
			if ((tempPath.isValidPath(pathOrUriString))) {
				return URIUtil.toURI(tempPath);
			} else {
				return URIUtil.toURI(pathOrUriString, false); // perhaps it is unnecessary
			}
		}
	}

	/**
	 * Load the information on path variables.
	 *
	 * @param rootNode
	 *            the node to load from.
	 * @param projectName
	 *            the name of the project to be used on the user interface.
	 *
	 * @return true if the import was successful, false otherwise.
	 * */
	private boolean loadPathVariables(final Node rootNode, final String projectName) {
		final IPathVariableManager pathVariableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();

		final NodeList variableNodes = rootNode.getChildNodes();
		for (int i = 0, size = variableNodes.getLength(); i < size; i++) {
			final Node variable = variableNodes.item(i);
			if (variable.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			final NamedNodeMap attributeMap = variable.getAttributes();
			if (attributeMap == null) {
				continue;
			}

			final Node nameNode = attributeMap.getNamedItem("name");
			final Node valueNode = attributeMap.getNamedItem("value");

			if (nameNode == null || valueNode == null) {
				displayError("Import failed", "Error while importing project " + projectName
						+ " some attributes of a path variable are missing");
				return false;
			}

			final String variableName = nameNode.getTextContent();
			final String variableValue = valueNode.getTextContent();

			if (headless || shell == null) {
				try {
					pathVariableManager.setURIValue(variableName, convertPathOrUriStringToURI(variableValue));
				} catch (CoreException e) {
					ErrorReporter.logExceptionStackTrace("While setting path variable `" + variableName + "' in headless mode", e);
				} catch (URISyntaxException e) {
					ErrorReporter.logExceptionStackTrace("While setting path variable `" + variableName + "' in headless mode", e);
				}
			} else {
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						try {
							final URI variableValueURI = convertPathOrUriStringToURI(variableValue);
							final String variableValue1 = variableValueURI.toString();
							if (pathVariableManager.isDefined(variableName)) {
								final URI uri = pathVariableManager.getURIValue(variableName);
								if (!variableValue1.equals( uri.toString())) {
									final EditPathVariableDialog dialog = new EditPathVariableDialog(shell, variableName, uri, variableValueURI);
									if (Window.OK == dialog.open()) {
										final URI actualValue = dialog.getActualValue();
										pathVariableManager.setURIValue(variableName, actualValue);
									}
								}
							} else {
								//Modification dialog has been removed
								pathVariableManager.setURIValue(variableName, variableValueURI);
							}
						} catch (CoreException e) {
							ErrorReporter.logExceptionStackTrace("While setting path variable `" + variableName + "' in GUI mode", e);
						} catch (URISyntaxException e) {
							ErrorReporter.logExceptionStackTrace("While setting path variable `" + variableName + "' in GUI mode", e);
						}
					}
				});
			}
		}

		return true;
	}

	/**
	 * Loads the configuration related options onto the project from the
	 * document being loaded.
	 *
	 * @param project
	 *            the project to load onto.
	 * @param mainNodes
	 *            the mainNodes to check for the configuration related options.
	 *
	 * @return true if the import was successful, false otherwise.
	 * */
	private boolean loadConfigurationData(final IProject project, final NodeList mainNodes) {
		final Document targetDocument = ProjectDocumentHandlingUtility.getDocument(project);
		final Node activeConfigurationNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.ACTIVE_CONFIGURATION_NODE);
		String activeConfiguration = ProjectFormatConstants.DEFAULT_CONFIGURATION_NAME;
		if (activeConfigurationNode != null) {
			activeConfiguration = activeConfigurationNode.getTextContent();
		} else {
			activeConfiguration = "Default";
		}
		try {
			project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					ProjectBuildPropertyData.ACTIVECONFIGURATION), activeConfiguration);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(
					"While setting `" + activeConfiguration + "' as configuration for project `" + project.getName() + "'", e);
		}

		// Remove possible target configuration nodes in existence
		removeConfigurationNodes(targetDocument.getDocumentElement());


		final Node targetActiveConfiguration = targetDocument.createElement(ProjectFormatConstants.ACTIVE_CONFIGURATION_NODE);
		targetActiveConfiguration.appendChild(targetDocument.createTextNode(activeConfiguration));
		targetDocument.getDocumentElement().appendChild(targetActiveConfiguration);

		final Node targetConfigurationsRoot = targetDocument.createElement(ProjectFormatConstants.CONFIGURATIONS_NODE);
		targetDocument.getDocumentElement().appendChild(targetConfigurationsRoot);

		final Node configurationsNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.CONFIGURATIONS_NODE);
		if (configurationsNode == null) {
			ProjectDocumentHandlingUtility.saveDocument(project);
			ProjectBuildPropertyData.setProjectAlreadyExported(project, false);
			final ProjectFileHandler handler = new ProjectFileHandler(project);
			handler.loadProjectSettingsFromDocument(targetDocument);

			return true;
		}

		final NodeList configurationsNodeList = configurationsNode.getChildNodes();
		for (int i = 0, size = configurationsNodeList.getLength(); i < size; i++) {
			final Node configurationNode = configurationsNodeList.item(i);
			if (configurationNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			final NamedNodeMap attributeMap = configurationNode.getAttributes();
			if (attributeMap == null) {
				continue;
			}

			final Node nameNode = attributeMap.getNamedItem(ProjectFormatConstants.CONFIGURATION_NAME_ATTRIBUTE);
			if (nameNode == null) {
				displayError("Import failed", "Error while importing project " + project.getName()
						+ " the name attribute of a referenced project is missing");
				return false;
			}

			final String configurationName = nameNode.getTextContent();

			if (ProjectFormatConstants.DEFAULT_CONFIGURATION_NAME.equals(configurationName)) {
				copyConfigurationData(targetDocument.getDocumentElement(), configurationNode);
			} else {
				final Element targetConfiguration = targetDocument.createElement(ProjectFormatConstants.CONFIGURATION_NODE);
				targetConfiguration.setAttribute(ProjectFormatConstants.CONFIGURATION_NAME_ATTRIBUTE, configurationName);
				targetConfigurationsRoot.appendChild(targetConfiguration);

				copyConfigurationData(targetConfiguration, configurationNode);
			}
		}

		ProjectDocumentHandlingUtility.saveDocument(project);
		ProjectBuildPropertyData.setProjectAlreadyExported(project, false);
		final ProjectFileHandler handler = new ProjectFileHandler(project);
		handler.loadProjectSettingsFromDocument(targetDocument);

		return true;
	}

	/**
	 * Remove those child nodes of the provided node, which are related to
	 * handling configuration data.
	 *
	 * @param rootNode
	 *            the node to use.
	 * */
	private void removeConfigurationNodes(final Node rootNode) {
		final NodeList rootNodeList = rootNode.getChildNodes();
		Node tempNode = ProjectFileHandler.getNodebyName(rootNodeList, ProjectFormatConstants.CONFIGURATIONS_NODE);
		if (tempNode != null) {
			rootNode.removeChild(tempNode);
		}
		tempNode = ProjectFileHandler.getNodebyName(rootNodeList, ProjectFileHandler.PROJECTPROPERTIESXMLNODE);
		if (tempNode != null) {
			rootNode.removeChild(tempNode);
		}
		tempNode = ProjectFileHandler.getNodebyName(rootNodeList, ProjectFileHandler.FOLDERPROPERTIESXMLNODE);
		if (tempNode != null) {
			rootNode.removeChild(tempNode);
		}
		tempNode = ProjectFileHandler.getNodebyName(rootNodeList, ProjectFileHandler.FILEPROPERTIESXMLNODE);
		if (tempNode != null) {
			rootNode.removeChild(tempNode);
		}
		tempNode = ProjectFileHandler.getNodebyName(rootNodeList, ProjectFormatConstants.ACTIVE_CONFIGURATION_NODE);
		if (tempNode != null) {
			rootNode.removeChild(tempNode);
		}
	}

	/**
	 * Copies the configuration related data from the source node, to the target
	 * node.
	 *
	 * @param targetRoot
	 *            the node where the configuration data should be moved to.
	 * @param sourceRoot
	 *            the node from where the configuration data is moved.
	 * */
	private void copyConfigurationData(final Element targetRoot, final Node sourceRoot) {
		final Document document = targetRoot.getOwnerDocument();
		final NodeList rootList = sourceRoot.getChildNodes();
		Node targetNode = null;
		for (int i = 0, size = rootList.getLength(); i < size; i++) {
			final Node tempNode = rootList.item(i);
			final String nodeName = tempNode.getNodeName();
			if (ProjectFileHandler.PROJECTPROPERTIESXMLNODE.equals(nodeName) || ProjectFileHandler.FOLDERPROPERTIESXMLNODE.equals(nodeName)
					|| ProjectFileHandler.FILEPROPERTIESXMLNODE.equals(nodeName)) {
				targetNode = document.importNode(tempNode, true);
				ProjectFileHandler.clearNode(targetNode);
				targetRoot.appendChild(targetNode);
			}
		}
	}

	class ProjectSelector implements Runnable {
		private String projectName;
		private IProject project = null;
		private boolean cancelled = false;

		public ProjectSelector(final String projectName) {
			this.projectName = projectName;
		}

		public String getProjectName() {
			return projectName;
		}

		public IProject getProject() {
			return project;
		}

		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void run() {
			final NewProjectNameDialog dialog = new NewProjectNameDialog(shell, projectName);
			if (dialog.open() == Window.OK) {
				projectName = dialog.getName();
				project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (project.exists()) {
					cancelled = true;
				}
			} else {
				cancelled = true;
			}
		}

	}

	/**
	 * Create a new project based on the information found in the provided
	 * document.
	 * */
	private IProject createProject(final Node mainElement, final boolean treatExistingProjectAsError) {
		final NodeList mainNodes = mainElement.getChildNodes();
		final Node projectNameNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.PROJECTNAME_NODE);
		if (null == projectNameNode) {
			TITANDebugConsole.println("The name of the project could not be found in the project descriptor, it will not be created.");
			return null;
		}

		final String originalProjectName = projectNameNode.getFirstChild().getTextContent();
		String projectName = originalProjectName;

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.exists()) {

			if (!treatExistingProjectAsError || headless) {

				if(!project.isOpen()) {
					try {
						project.open(null);
					} catch(CoreException e) {
						ErrorReporter.logError("An existing project with the name " + projectName + " cannot be opened!");
					}
				}
				ErrorReporter.logWarning("A project with the name " + projectName + " already exists, skipping it!");
				//It will be skipped => 
				return null;
			}
			//Error dialog:
			//gets a new project name instead of the existing one:
			final ProjectSelector temp = new ProjectSelector(projectName);
			Display.getDefault().syncExec(temp);
			if (temp.cancelled) {
				return null;
			}

			projectName = temp.getProjectName();
			project = temp.getProject();
		}

		finalProjectNames.put(originalProjectName, projectName);

		project = createNewProject(project, projectName);
		if (project == null) {
			TITANDebugConsole.println("There was an error while creating the project " + projectName);
			return null;
		}

		try {
			TITANNature.addTITANBuilderToProject(project);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace("While adding builder to `" + project.getName() + "'", e);
		}

		return project;
	}

	/**
	 * Load the project information document from the provided file and
	 * recursively for all project files mentioned in the referenced projects
	 * section.
	 *
	 * @param file
	 *            the file to load the data from.
	 * @param validator
	 *            the xml validator. can be <code>null</code>
	 *
	 * @return true if there were no errors, false otherwise.
	 * */
	private boolean loadURIDocuments(final URI file, final Validator validator) {
		if (projectsToImport.containsKey(file)) {
			return true;
		}

		if (!"file".equals(file.getScheme()) && !"".equals(file.getScheme())) {
			ErrorReporter.logError("Loading of project information is only supported for local files right now. " + file.toString()
					+ " could not be loaded");
			return false;
		}

		final Document document = getDocumentFromFile(file.getPath());
		if (document == null) {
			final StringBuilder builder = new StringBuilder("It was not possible to load the imported project file: '" + file.toString()
					+ "'\n");
			for (int i = importChain.size() - 1; i >= 0; --i) {
				builder.append("imported by: '");
				builder.append(importChain.get(i).toString());
				builder.append("'\n");
			}
			ErrorReporter.logError(builder.toString());
			return false;
		}
		if (validator != null) {
			try {
				validator.validate(new StreamSource(new File(file)));
			} catch (final Exception e) {
				ErrorReporter.logExceptionStackTrace(
						"Error while importing from file " + file + ": " + System.getProperty("line.separator"), e);
				return false;
			}
		}

		ProjectFileHandler.clearNode(document);

		projectsToImport.put(file, document);

		final Element mainElement = document.getDocumentElement();
		final NodeList mainNodes = mainElement.getChildNodes();
		final Node referencedProjectsNode = ProjectFileHandler.getNodebyName(mainNodes, ProjectFormatConstants.REFERENCED_PROJECTS_NODE);
		if (referencedProjectsNode == null) {
			return true;
		}

		// === Get referenced projects ===
		final IPath projectFileFolderPath = new Path(file.getPath()).removeLastSegments(1);
		final NodeList referencedProjectsList = referencedProjectsNode.getChildNodes();
		boolean result = true;
		for (int i = 0, size = referencedProjectsList.getLength(); i < size; i++) {
			final Node referencedProjectNode = referencedProjectsList.item(i);
			if (referencedProjectNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			final NamedNodeMap attributeMap = referencedProjectNode.getAttributes();
			if (attributeMap == null) {
				continue;
			}
			final Node nameNode = attributeMap.getNamedItem(ProjectFormatConstants.REFERENCED_PROJECT_NAME_ATTRIBUTE);
			if (nameNode == null) {
				displayError("Import failed", "Error while importing from file " + file
						+ " the name attribute of a referenced project is missing");
				return false;
			}

			final String projectName = nameNode.getTextContent();
			final Node locationNode = attributeMap.getNamedItem(ProjectFormatConstants.REFERENCED_PROJECT_LOCATION_ATTRIBUTE);
			if (locationNode == null) {
				if (i > 0) {
					displayError("Import failed", "Error while importing from file " + file
							+ " the location attribute of the referenced project " + projectName + " is not given.");
				} else {
					ErrorReporter.logWarning( "Import failed while importing from file " + file
							+ " the location attribute of the referenced project " 
							+ projectName + " is not given.\nPerhaps it is under PackedReferencedProjects");
				}
				break; // project handling continues in processing PackedReferencedProjects
			}

			final String unresolvedProjectLocationURI = locationNode.getTextContent();

			URI absoluteURI = TITANPathUtilities.resolvePath(unresolvedProjectLocationURI, URIUtil.toURI(projectFileFolderPath));

			String fileName;
			// Determine tpdname
			final Node tpdNameNode = attributeMap.getNamedItem(ProjectFormatConstants.REFERENCED_PROJECT_TPD_NAME_ATTRIBUTE);
			if(tpdNameNode != null) {
				fileName = tpdNameNode.getTextContent();
			}else {
				fileName = projectName + ".tpd";
			}

			tpdNameAttrMap.put(projectName, fileName);
			if (searchPaths != null && !searchPaths.isEmpty()) {
				File f = new File(absoluteURI);
				final IPath unresolvedProjectLocationURIPath = new Path(unresolvedProjectLocationURI);
				if(!unresolvedProjectLocationURIPath.isAbsolute() && (!f.exists() || f.isDirectory())) {;
					// Try search paths
					for (final String path : searchPaths) {
						String filePath = path;
						if(path.charAt(path.length() - 1) != '/') {
							filePath += "/";
						}
						filePath += fileName;
						final String systemPath = new Path(filePath).toOSString();
						f = new File(systemPath);
						// tpd found
						if (f.exists() && !f.isDirectory()) {
							absoluteURI = URIUtil.toURI(systemPath);
							tpdURIMap.put(projectName, unresolvedProjectLocationURI);
							break;
						}
					}
				}
			}

			if (absoluteURI!=null && !"file".equals(absoluteURI.getScheme())) {
				final StringBuilder builder = new StringBuilder(
						"Loading of project information is only supported for local files right now. " + absoluteURI.toString()
								+ " could not be loaded\n");
				for (int j = importChain.size() - 1; j >= 0; --j) {
					builder.append("imported by: '");
					builder.append(importChain.get(j).toString());
					builder.append("'\n");
				}
				ErrorReporter.logError(builder.toString());
				continue;
			}

			importChain.add(file);
//			if( !projectsWithUnresolvedName.containsKey(absoluteURI) ) {
//				projectsWithUnresolvedName.put(absoluteURI, unresolvedProjectLocationURI);
//			}
			result &= loadURIDocuments(absoluteURI, validator);
			importChain.remove(importChain.size() - 1);
		}

		return result;
	}

	private void displayError(final String title, final String message) {
		if (!headless) {
			ErrorReporter.parallelErrorDisplayInMessageDialog(title, message);
		}
		ErrorReporter.logError(message);
	}

	/**
	 * Extracts an XML document from the provided file.
	 *
	 * @param file
	 *            the file to read from.
	 * @return the extracted XML document, or null if there were some error.
	 * */
	public Document getDocumentFromFile(final String file) {
		final LSInput lsInput = domImplLS.createLSInput();
		Document document = null;
		try {
			final FileInputStream istream = new FileInputStream(file);
			lsInput.setByteStream(istream);
			document = parser.parse(lsInput);
			istream.close();
		} catch (Exception e) {
			ErrorReporter.logExceptionStackTrace("While getting the document from `" + file + "'", e);
		}

		return document;
	}

	/**
	 * Creating a new project.
	 *
	 * @return the new project created.
	 */
	IProject createNewProject(final IProject newProjectHandle, final String name) {
		IProject newProject;

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description = workspace.newProjectDescription(name);

		TITANNature.addTITANNatureToProject(description);

		if (headless) {
			try {
				createProject(description, newProjectHandle, new NullProgressMonitor());
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("While creating project `" + newProjectHandle.getName() + "'", e);
			}
		} else {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					try {
						final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
							@Override
							protected void execute(final IProgressMonitor monitor) throws CoreException {
								createProject(description, newProjectHandle, monitor);
							}
						};
						new ProgressMonitorDialog(null).run(true, true, op);
					} catch (InterruptedException e) {
						return;
					} catch (final InvocationTargetException e) {
						displayError(CREATION_FAILED, e.getMessage());
						ErrorReporter.logExceptionStackTrace("While creating project `" + newProjectHandle.getName() + "'", e);
						return;
					}
				}
			});
		}

		newProject = newProjectHandle;

		return newProject;
	}

	/**
	 * Creating a new project.
	 *
	 * @param description
	 *            - IProjectDescription that belongs to the newly created
	 *            project.
	 * @param projectHandle
	 *            - a project handle that is used to create the new project.
	 * @param monitor
	 *            - reference to the monitor object
	 * @exception CoreException
	 *                thrown if access to the resources throws a CoreException.
	 * @exception OperationCanceledException
	 *                if the operation was canceled by the user.
	 */
	protected void createProject(final IProjectDescription description, final IProject projectHandle, final IProgressMonitor monitor)
			throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 3);

		try {
			progress.setTaskName(CREATING_PROJECT);

			projectHandle.create(description, progress.newChild(1));

			if (progress.isCanceled()) {
				throw new OperationCanceledException();
			}

			projectHandle.open(IResource.BACKGROUND_REFRESH, progress.newChild(1));

			projectHandle.refreshLocal(IResource.DEPTH_ONE, progress.newChild(1));
		} finally {
			progress.done();
		}
	}
}
