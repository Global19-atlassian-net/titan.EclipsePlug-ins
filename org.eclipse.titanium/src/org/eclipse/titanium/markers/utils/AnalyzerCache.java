/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.markers.utils;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.titanium.Activator;
import org.eclipse.titanium.markers.types.CodeSmellType;
import org.eclipse.titanium.preferences.ProblemTypePreference;
import org.eclipse.titanium.preferences.pages.MarkersPreferencePage;

public final class AnalyzerCache {
	private static final AtomicReference<Analyzer> PREFERENCE_BASED = new AtomicReference<Analyzer>();
	// TODO: we should only rebuild when a smell parameter value is changed
	private static final AtomicReference<Analyzer> ALL_BASED = new AtomicReference<Analyzer>();

	static {
		PREFERENCE_BASED.set(Analyzer.builder().adaptPreferences().build());
		ALL_BASED.set(buildAllBased());
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				if (event.getProperty().startsWith(ProblemTypePreference.PREFIX)) {
					PREFERENCE_BASED.set(Analyzer.builder().adaptPreferences().build());
					ALL_BASED.set(buildAllBased());
				}
			}
		});
	}

	/**
	 * Private constructor.
	 * */
	private AnalyzerCache(){
		//Do nothing
	}

	/**
	 * Obtain the cached analyzer configured according to the current
	 * preferences.
	 * <p>
	 * Note that the <code>Analyzer</code> one acquires via this method mirrors
	 * the code smell preferences of the moment of the method call, but is not
	 * synchronized for further preference changes. The preferences are read
	 * from the plugin preference store, configured by the user on
	 * {@link MarkersPreferencePage}.
	 *
	 * @return the <code>Analyzer</code> that uses code smells as the user
	 *         prefers
	 */
	public static Analyzer withPreference() {
		return PREFERENCE_BASED.get();
	}

	/**
	 * Obtain the cached analyzer configured for using all code smells.
	 * <p>
	 * The same rules apply as in {@link #withPreference()}: settings of code
	 * smells mirrors settings at the time of the method call. Here it is not a
	 * question which code smells are used, but any code smells can have some
	 * own setting.
	 *
	 * @return the <code>Analyzer</code> that uses all code smells
	 */
	public static Analyzer withAll() {
		return ALL_BASED.get();
	}

	private static Analyzer buildAllBased() {
		final AnalyzerBuilder allBasedBuilder = Analyzer.builder();
		for (final CodeSmellType type : CodeSmellType.values()) {
			allBasedBuilder.addProblem(type);
		}
		return allBasedBuilder.build();
	}
}
