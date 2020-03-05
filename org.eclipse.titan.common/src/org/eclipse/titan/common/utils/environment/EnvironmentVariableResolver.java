/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.common.utils.environment;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvironmentVariableResolver {

	private final Pattern pattern;
	private final String variableStart;
	private final String variableEnd;

	private EnvironmentVariableResolver(final Pattern pattern, final String variableStart, final String variableEnd) {
		this.pattern = pattern;
		this.variableStart = variableStart;
		this.variableEnd = variableEnd;
	}

	public static EnvironmentVariableResolver eclipseStyle() {
		return new EnvironmentVariableResolver(Pattern.compile("\\[.*?\\]"), "[", "]");
	}

	public static EnvironmentVariableResolver unixStyle() {
		return new EnvironmentVariableResolver(Pattern.compile("\\$\\{.*?\\}"), "${", "}");
	}

	public String resolve(final String original, final Map<?, ?> envVariables) throws VariableNotFoundException {
		try {
			return resolve(original, envVariables, false);
		} catch (IllegalArgumentException e) {
			throw new VariableNotFoundException(e.getMessage());
		}
	}

	public String resolveIgnoreErrors(final String original, final Map<?, ?> envVariables) {
		return resolve(original, envVariables, true);
	}

	private String resolve(final String original, final Map<?, ?> envVariables, final boolean ignoreErrors) {
		final Matcher matcher = pattern.matcher(original);
		final StringBuffer builder = new StringBuffer(original.length());
		boolean result2 = matcher.find();
		while (result2) {
			final String keyWithStartEnd = matcher.group();
			final String key = keyWithStartEnd.substring(variableStart.length(), keyWithStartEnd.length() - 1);
			if (envVariables.containsKey(key)) {
				final String result3 = (String) envVariables.get(key);
				matcher.appendReplacement(builder, result3.replace("\\", "\\\\").replace("$", "\\$"));
			} else {
				if (ignoreErrors) {
					matcher.appendReplacement(builder, Matcher.quoteReplacement(variableStart) + key + Matcher.quoteReplacement(variableEnd));
				} else {
					throw new IllegalArgumentException(keyWithStartEnd);
				}
			}
			result2 = matcher.find();
		}
		matcher.appendTail(builder);
		return builder.toString();
	}

	/**
	 * Replaces the environment variable/path variables, whose pattern is defined by the constructor input parameter, with the form of ${VAR}
	 * Prerequisite: eclipseStyle resolver is created with the appropriate pattern
	 * Usage: String retval = EnvironmentVariableResolver.eclipseStyle().replaceEnvVarsWithUnixEnvVars(pathToBeResolved);
	 * @param original
	 * @return
	 */
	public String replaceEnvVarsWithUnixEnvVars(final String original) {

		final Matcher matcher = pattern.matcher(original);
		final StringBuffer builder = new StringBuffer(original.length());
		boolean result2 = matcher.find();
		while (result2) {
			final String keyWithStartEnd = matcher.group();
			final String key = keyWithStartEnd.substring(variableStart.length(), keyWithStartEnd.length() - 1);
			final String result3 = "\\$\\{" + key + "\\}";
			matcher.appendReplacement(builder, result3);
			result2 = matcher.find();
		}
		matcher.appendTail(builder);
		return builder.toString();
	}

	public static class VariableNotFoundException extends Exception {
		public VariableNotFoundException(final String variableName) {
			super("Variable " + variableName + " cannot be resolved.");
		}
	}
}
