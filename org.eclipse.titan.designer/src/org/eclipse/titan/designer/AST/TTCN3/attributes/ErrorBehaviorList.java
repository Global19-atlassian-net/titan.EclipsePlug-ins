/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.titan.designer.AST.ASTNode;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * Represents a list of error behavior settings in the codec API of the run-time
 * environment.
 *
 * @author Kristof Szabados
 */
public final class ErrorBehaviorList extends ASTNode implements ILocateableNode, IIncrementallyUpdateable {
	//TODO check missing NEGTEST_CONFL
	private static final String[] VALID_TYPES = { "UNBOUND", "INCOMPL_ANY", "ENC_ENUM", "INCOMPL_MSG", "LEN_FORM", "INVAL_MSG", "REPR",
		"CONSTRAINT", "TAG", "SUPERFL", "EXTENSION", "DEC_ENUM", "DEC_DUPFLD", "DEC_MISSFLD", "DEC_OPENTYPE", "DEC_UCSTR", "LEN_ERR",
		"SIGN_ERR", "INCOMP_ORDER", "TOKEN_ERR", "LOG_MATCHING", "FLOAT_TR", "FLOAT_NAN", "OMITTED_TAG", "EXTRA_DATA" };
	private static final String[] VALID_HANDLINGS = { "DEFAULT", "ERROR", "WARNING", "IGNORE" };

	// TODO could be optimized using real-life data
	private final List<ErrorBehaviorSetting> settings = new ArrayList<ErrorBehaviorSetting>(1);
	private final Map<String, ErrorBehaviorSetting> settingMap = new HashMap<String, ErrorBehaviorSetting>();
	private ErrorBehaviorSetting settingAll;

	/** the time when this error behavior list was checked the last time. */
	private CompilationTimeStamp lastTimeChecked;
	private Location location;

	public ErrorBehaviorList() {
		// do nothing
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

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (int i = 0, size = settings.size(); i < size; i++) {
			final ErrorBehaviorSetting setting = settings.get(i);
			if (setting == child) {
				return builder.append(".<setting ").append(i + 1).append('>');
			}
		}

		return builder;
	}

	public void addSetting(final ErrorBehaviorSetting setting) {
		if (setting != null) {
			settings.add(setting);
		}
	}

	public void addAllBehaviors(final ErrorBehaviorList other) {
		if (other != null) {
			settings.addAll(other.settings);
		}
	}

	public int getNofErrorBehaviors() {
		return settings.size();
	}

	public ErrorBehaviorSetting getBehaviorByIndex(final int index) {
		return settings.get(index);
	}

	public boolean hasSetting(final CompilationTimeStamp timestamp, final String errorType) {
		check(timestamp);
		return settingAll != null || settingMap.containsKey(errorType);
	}

	public String getHandling(final CompilationTimeStamp timestamp, final String errorType) {
		check(timestamp);
		if (settingMap.containsKey(errorType)) {
			return settingMap.get(errorType).getErrorHandling();
		} else if (settingAll != null) {
			return settingAll.getErrorHandling();
		}

		return "DEFAULT";
	}

	/**
	 * Does the semantic checking of the error behavior list..
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		for (int i = 0, size = settings.size(); i < size; i++) {
			final ErrorBehaviorSetting setting = settings.get(i);
			final String errorType = setting.getErrorType();
			if ("ALL".equals(errorType)) {
				if (settingAll != null) {
					setting.getLocation().reportSemanticWarning("Duplicate setting for error type `ALL'");
					settingAll.getLocation().reportSemanticWarning("The previous setting is ignored");
				}
				settingAll = setting;
				if (!settingMap.isEmpty()) {
					setting.getLocation().reportSemanticWarning("All setting before `ALL' are ignored");
					settingMap.clear();
				}
			} else {
				if (settingMap.containsKey(errorType)) {
					setting.getLocation().reportSemanticWarning(
							MessageFormat.format("Duplicate setting for error type `{0}''", errorType));
					settingMap.get(errorType).getLocation().reportSemanticWarning("The previous setting is ignored");
					settingMap.put(errorType, setting);
				} else {
					settingMap.put(errorType, setting);
				}

				boolean typeFound = false;
				for (final String validType : VALID_TYPES) {
					if (validType.equals(errorType)) {
						typeFound = true;
						break;
					}
				}
				if (!typeFound) {
					setting.getLocation().reportSemanticWarning(
							MessageFormat.format("String `{0}'' is not a valid error type", errorType));
				}
			}

			final String errorHandling = setting.getErrorHandling();
			boolean handlingFound = false;
			for (final String validHandling : VALID_HANDLINGS) {
				if (validHandling.equals(errorHandling)) {
					handlingFound = true;
					break;
				}
			}
			if (!handlingFound) {
				setting.getLocation().reportSemanticWarning(
						MessageFormat.format("String `{0}'' is not a valid error handling", errorHandling));
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

		for (final ErrorBehaviorSetting setting : settings) {
			setting.updateSyntax(reparser, false);
			reparser.updateLocation(setting.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (settings != null) {
			for (final ErrorBehaviorSetting s : settings) {
				if (!s.accept(v)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Add generated java code on this level.
	 *
	 * @param aData only used to update imports if needed
	 * @param source the source code generated
	 */
	public void generateCode( final JavaGenData aData, final StringBuilder source  ) {
		if (settingAll != null) {
			source.append(MessageFormat.format("TTCN_EncDec.set_error_behavior(TTCN_EncDec.error_type.ET_ALL, TTCN_EncDec.error_behavior_type.EB_{0});\n", settingAll.getErrorHandling()));
		} else {
			source.append( "TTCN_EncDec.set_error_behavior(TTCN_EncDec.error_type.ET_ALL, TTCN_EncDec.error_behavior_type.EB_DEFAULT);\n" );
		}
		for (final ErrorBehaviorSetting ebs : settingMap.values()) {
			source.append(MessageFormat.format("TTCN_EncDec.set_error_behavior(TTCN_EncDec.error_type.ET_{0}, TTCN_EncDec.error_behavior_type.EB_{1});\n", ebs.getErrorType(), ebs.getErrorHandling()));
		}
	}
}
