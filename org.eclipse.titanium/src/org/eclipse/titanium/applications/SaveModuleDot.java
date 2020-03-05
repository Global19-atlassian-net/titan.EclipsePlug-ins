/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.applications;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.titanium.error.ConsoleErrorHandler;
import org.eclipse.titanium.error.ErrorHandler;
import org.eclipse.titanium.graph.clustering.BaseCluster;
import org.eclipse.titanium.graph.clustering.ClustererBuilder;
import org.eclipse.titanium.graph.components.EdgeDescriptor;
import org.eclipse.titanium.graph.components.NodeDescriptor;
import org.eclipse.titanium.graph.generators.ModuleGraphGenerator;
import org.eclipse.titanium.graph.visualization.GraphHandler;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

/**
 * This class <code>implements {@link IApplication}</code>, it is used to export
 * pajek graph without loading the visual features.
 *
 * @author Gabor Jenei
 */
public class SaveModuleDot extends InformationExporter {
	private DirectedSparseGraph<NodeDescriptor, EdgeDescriptor> graph=null;

	@Override
	protected boolean checkParameters(final String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: <output path> [-c<clustering algorithm name>]");
			System.out.println("The possible clustering algorithms are: ");
			System.out.println("\tModuleLocation\n\tFolderName\n\tLinkedLocation\n\tRegularExpression\n\tModuleName\n");
			return false;
		}

		return true;
	}

	@Override
	protected void exportInformationForProject(final String[] args, final IProject project, final IProgressMonitor monitor) {
		final ErrorHandler errorHandler = new ConsoleErrorHandler();
		final ModuleGraphGenerator generator = new ModuleGraphGenerator(project, errorHandler);

		try {
			generator.generateGraph();

			String clusterName="";
			for (int i = 1;i < args.length;++i) {
				if (args[i].startsWith("-c")) {
					clusterName=args[i].substring(2);
				}
			}

			if(clusterName.isEmpty()){
				graph = generator.getGraph();
			} else {
				final BaseCluster clusterer = new ClustererBuilder().
						setAlgorithm(clusterName).setGraph(generator.getGraph()).setProject(project).build();
				clusterer.run(monitor, false);
				graph = clusterer.getGraph();
			}

			final String fileName=args[0] + project.getName() + ".dot";
			GraphHandler.saveGraphToDot(graph, fileName, project.getName());
			errorHandler.reportInformation("The graphs have been successfully saved. See results at "+
					new File(fileName).getAbsolutePath());
		} catch (Exception e) {
			errorHandler.reportException("Error while exporting", e);
		}
	}
}