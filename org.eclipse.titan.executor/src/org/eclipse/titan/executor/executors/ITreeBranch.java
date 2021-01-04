/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.executor.executors;

import java.util.List;

/**
 * This interface represents a tree branch, that can have leafs under itself.
 * <p>
 * Extends tree leaf, allowing to store tree branches in tree branches.
 *
 * @author Kristof Szabados
 * */
public interface ITreeBranch extends ITreeLeaf {

	@Override
	void dispose();

	List<ITreeLeaf> children();
}
