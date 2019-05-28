/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.applications;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.titanium.markers.export.ExportedProblemMerger;

/**
 * @author Gobor Daniel
 *
 *         An application to merge the exported code smell tables. It generates
 *         an excel file given in the arguments after the -o flag or a default
 *         one. Accepts the .xls files as arguments, where the exported code
 *         smells can be found.
 */
public class MergeExportedMarkerTables implements IApplication {

	@Override
	public Object start(final IApplicationContext context) throws Exception {

		final String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		if (args.length == 0) {
			System.out.println("Valid options: file_name(s).xls [-o filename]");
			return Integer.valueOf(-1);
		}

		final List<File> files = new LinkedList<File>();
		File file;
		String name = null;

		for (int i = 0; i < args.length; ++i) {
			if (args[i].endsWith(".xls")) {
				file = new File(args[i]);
				files.add(file);
			} else if ("-o".equals(args[i])) {
				i += 1;
				if (i < args.length) {
					name = args[i];
				} else {
					System.out.println("File name required after -o");
					return Integer.valueOf(-1);
				}
			} else {
				System.out.println("Valid options: file_name(s).xls [-o filename]");
				return Integer.valueOf(-1);
			}
		}

		final boolean result = merge(files, name);

		if (result) {
			return EXIT_OK;
		} else {
			return Integer.valueOf(-1);
		}
	}

	@Override
	public void stop() {
		// nothing to be done
	}

	/**
	 * Merges the given excel files containing the exported code smells.
	 *
	 * @param files
	 *            A list of File objects
	 * @param outfileName
	 *            The name of the output file, if null "Summary" will be the
	 *            filename
	 */
	private boolean merge(final List<File> files, final String outfileName) {
		File outfile = null;
		if (outfileName != null) {
			outfile = new File(outfileName + ".xls");
		} else {
			outfile = new File("Summary.xls");
		}
		int counter = 1;
		while (outfile.exists()) {
			if (outfileName != null) {
				outfile = new File(outfileName + counter + ".xls");
			} else {
				outfile = new File("Summary" + counter + ".xls");
			}
			counter += 1;
		}

		final ExportedProblemMerger epm = new ExportedProblemMerger(files, outfile);
		return epm.run();
	}
}
