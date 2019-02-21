/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.Module.module_type;
import org.eclipse.titan.designer.AST.ASN1.definitions.SpecialASN1Module;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * @author Kristof Szabados
 * */
public abstract class Setting extends ASTNode implements ISetting {
	/** indicates if this setting has already been found erroneous in the actual checking cycle. */
	protected boolean isErroneous;

	/** the name of the setting to be used in the code generator */
	protected String genName;

	/** the time when this setting was check the last time. */
	protected CompilationTimeStamp lastTimeChecked;

	/**
	 * The location of the whole setting.
	 * This location encloses the setting fully, as it is used to report errors to.
	 **/
	protected Location location;

	public Setting() {
		isErroneous = false;
		location = NULL_Location.INSTANCE;
	}

	@Override
	/** {@inheritDoc} */
	public final boolean getIsErroneous(final CompilationTimeStamp timestamp) {
		return isErroneous;
	}

	@Override
	/** {@inheritDoc} */
	public final void setIsErroneous(final boolean isErroneous) {
		this.isErroneous = isErroneous;
	}

	@Override
	/** {@inheritDoc} */
	public abstract Setting_type getSettingtype();

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
	public final boolean isAsn() {
		if (myScope == null) {
			return false;
		}

		return module_type.ASN_MODULE.equals(myScope.getModuleScope().getModuletype());
	}

	/**
	 * Set the generated name for this setting.
	 *
	 * @param genName the name to set.
	 * */
	public void setGenName(final String genName) {
		this.genName = genName;
	}

	/**
	 * Set the generated name for this setting,
	 *  as a concatenation of a prefix, an underscore and a suffix,
	 * unless the prefix already ends with, or the suffix already begins with
	 * precisely one underscore.
	 *
	 * @param prefix the prefix to use
	 * @param suffix the suffix to use.
	 * */
	public void setGenName(final String prefix, final String suffix) {
		if (prefix.length() == 0 || suffix.length() == 0) {
			ErrorReporter.INTERNAL_ERROR("FATAL ERROR while seting the generated name of setting `" + getFullName() + "''");
			genName = "<FATAL ERROR>";
			return;
		}

		if((!prefix.endsWith("_") || prefix.endsWith("__")) &&
				(!suffix.startsWith("_") || suffix.startsWith("__"))) {
			genName = prefix + "_" + suffix;
		} else {
			genName = prefix + suffix;
		}
	}

	/**
	 * Returns a Java reference that points to this setting from the local module.
	 *
	 * @return The name of the Java setting in the generated code.
	 */
	public String getGenNameOwn(){
		if (genName == null) {
			ErrorReporter.INTERNAL_ERROR("Code generator reached erroneous setting `" + getFullName() + "''");
			return "FATAL_ERROR encountered while processing `" + getFullName() + "''\n";
		}

		return genName;
	}

	@Override
	/** {@inheritDoc} */
	public String getGenNameOwn(final JavaGenData aData) {
		if(myScope == null) {
			ErrorReporter.INTERNAL_ERROR("Code generator reached erroneous setting `" + getFullName() + "''");
			return "FATAL_ERROR encountered while processing `" + getFullName() + "''\n";
		}

		final StringBuilder returnValue = new StringBuilder();
		final Module myModule = myScope.getModuleScopeGen();//get_scope_mod_gen
		if(!myModule.equals(aData.getModuleScope()) && !SpecialASN1Module.isSpecAsss(myModule)) {
			//when the definition is referred from another module
			// the reference shall be qualified with the namespace of my module
			returnValue.append(myModule.getName()).append('.');
		}

		returnValue.append( getGenNameOwn());

		return returnValue.toString();
	}
}
