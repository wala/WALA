/******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cast.tree.pattern;

import com.ibm.wala.cast.tree.CAstNode;

/**
 * A node pattern matching a node of a given kind, without regard to its children.
 * 
 * @author mschaefer
 *
 */
public class SubtreeOfKind extends NodeOfKind {
	public SubtreeOfKind(int kind) {
		super(kind);
	}
	
	@Override
	public boolean matches(CAstNode node) {
		return node != null && node.getKind() == this.kind;
	}
}
