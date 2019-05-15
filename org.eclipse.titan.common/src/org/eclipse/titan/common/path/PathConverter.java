/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.common.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.utils.Cygwin;
import org.eclipse.titan.common.utils.IOUtils;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * This is a simple utility to convert a Windows path to a path the build system
 * requires.
 * <ul>
 * <li>On Windows this is a conversion to cygwin path done with the cygpath
 * tool.
 * <li>Other operating system's paths seems to be OK
 * </ul>
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class PathConverter {
	private static final String EXECUTION_FAILED = "execution failed";
	private static final String INTERRUPTION = "execution failed beacuse of interrupion";

	private static final ConcurrentHashMap<String, String> CYGWINPATHMAP = new ConcurrentHashMap<String, String>();

	/** private constructor to disable instantiation */
	private PathConverter() {
	}

	/**
	 * Converts a win32 path into a cygwin one.
	 *
	 * @param path
	 *                the path as win32 reports it
	 * @param reportDebugInformation
	 *                tells whether debug information be reported to the
	 *                console, or not
	 * @param outputConsole
	 *                the console to write the output to
	 * @return the path in cygwin style or the value of the path parameter.
	 * */
	public static String convert(final String path, final boolean reportDebugInformation, final MessageConsole outputConsole) {

		if("".equals(path.trim())){
			ErrorReporter.logWarning("The empty path could not be converted");
			return path;
		}

		if (!Platform.OS_WIN32.equals(Platform.getOS())) {
			return path;
		}

		// If we are on win32 and we do not have cygwin -> cancel
		if (!Cygwin.isInstalled()) {
			return path;
		}

		if (CYGWINPATHMAP.containsKey(path)) {
			return CYGWINPATHMAP.get(path);
		}

		final List<String> finalCommand = Arrays.asList("sh", "-c", "cygpath -u " + '\'' + path + '\'');

		final MessageConsoleStream stream = printCommandToDebugConsole(reportDebugInformation, outputConsole, finalCommand);
		final ProcessBuilder pb = new ProcessBuilder();
		pb.redirectErrorStream(true);
		pb.command(finalCommand);
		Process proc;
		try {
			proc = pb.start();
		} catch (IOException e) {
			ErrorReporter.logExceptionStackTrace("The process could not be started", e);
			return path;
		}

		return processOutput(path, stream, proc);

	}

	private static String processOutput(final String path, final MessageConsoleStream stream, final Process proc) {
		final BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream(), Charset.defaultCharset()));
		try {
			final StringBuilder solution = new StringBuilder();
			String line = stdout.readLine();
			while (line != null) {
				printDebug(stream, line);
				solution.append(line);
				line = stdout.readLine();
			}
			final int exitValue = proc.waitFor();
			if (exitValue != 0) {
				ErrorReporter.logError("The path:`" + path + "' could not be converted");
				return path;
			}

			line = stdout.readLine();//TODO check should have reached end by now.
			while (line != null) {
				printDebug(stream, line);
				solution.append(line);
				line = stdout.readLine();
			}

			final String temp = solution.toString();
			CYGWINPATHMAP.put(path, temp);
			return temp;
		} catch (IOException e) {
			printDebug(stream, EXECUTION_FAILED);
			ErrorReporter.logExceptionStackTrace("Cygwin conversion result could not be read", e);
		} catch (InterruptedException e) {
			printDebug(stream, INTERRUPTION);
			ErrorReporter.logExceptionStackTrace("Conversion of " + path + " interrupted", e);
		} finally {
			IOUtils.closeQuietly(stdout);
		}
		return path;
	}

	private static void printDebug(final MessageConsoleStream stream, final String line) {
		if (stream != null) {
			stream.println(line);
		}
	}

	/**
	 * Returns the operating system dependent absolute path of a file
	 * relative to the position of a base file.
	 *
	 * @param baseFile
	 *                the file to be used as the base of the full path
	 * @param file
	 *                the file whose absolute path we wish to find
	 * @return the absolute path of the file or null if not possible
	 */
	public static String getAbsolutePath(final String baseFile, final String file) {
		final IPath filePath = new Path(file);
		if (filePath.isAbsolute()) {
			return file;
		}
		// absolute path of the base file
		final IPath baseFilePath = new Path(baseFile);
		// absolute path of the base dir
		final IPath baseDirPath = baseFilePath.makeAbsolute().removeLastSegments(1);
		return baseDirPath.append(filePath).makeAbsolute().toOSString();
	}

	/**
	 * Returns the project relative path of a file
	 * which is given relative to the position of a base file.
	 *
	 * @param aBaseFile the file to be used as the base of the path<br>
	 *         example value: L/hw/src/MyExample.cfg
	 * @param aFile the file name, whose RELATIVE path we wish to find, this must be relative to the base<br>
	 *         example value: MyExample2.cfg
	 * @return the RELATIVE path of the file<br>
	 *         example return value: src/MyExample2.cfg
	 */
	public static IPath getProjectRelativePath( final IFile aBaseFile, final String aFile ) {
		// relative (to the base) path of the file
		final IPath filePath = new Path( aFile );

		// relative (to the project) path of the base file
		final IPath baseFilePath = aBaseFile.getProjectRelativePath();
		// example value: baseFilePath == src/MyExample.cfg

		// relative (to the project) path of the base dir
		final IPath baseDirPath = baseFilePath.removeLastSegments( 1 );
		// example value: baseFilePath == src

		// relative (to the project) path of the file
		return baseDirPath.append( filePath );
	}

	private static MessageConsoleStream printCommandToDebugConsole(final boolean reportDebugInformation, final MessageConsole outputConsole, final List<String> command) {
		MessageConsoleStream stream = null;
		if (reportDebugInformation) {
			stream = outputConsole.newMessageStream();

			for (final String c : command) {
				stream.print(c + " ");
			}
			stream.println();
		}
		return stream;
	}
}
