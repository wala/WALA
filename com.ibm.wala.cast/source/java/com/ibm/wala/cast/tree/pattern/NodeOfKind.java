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
 * A node pattern that matches an AST node of a certain kind; additionally, the node's children
 * have to match the pattern's child patterns.
 * 
 * @author mschaefer
 *
 */
public class NodeOfKind implements NodePattern {
	protected int kind;
	protected NodePattern[] children;
	
	public NodeOfKind(int kind, NodePattern... children) {
		this.kind = kind;
		this.children = new NodePattern[children.length];
		for(int i=0;i<children.length;++i)
			this.children[i] = children[i];
	}
	
	/* (non-Javadoc)
	 * @see pattern.NodePattern#matches(com.ibm.wala.cast.tree.CAstNode)
	 */
	@Override
  public boolean matches(CAstNode node) {
		if(node == null || node.getKind() != kind || node.getChildCount() != children.length)
			return false;
		for(int i=0;i<children.length;++i)
			if(!children[i].matches(node.getChild(i)))
				return false;
		return true;
	}
}
