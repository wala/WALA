/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NoKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;

/**
 * Representation of a node's position in a CAst entity's syntax tree. The position is stored as a zipper
 * data structure.
 * 
 * @author mschaefer
 *
 */
public abstract class NodePos implements CAstRewriter.RewriteContext<CAstBasicRewriter.NoKey> {
	public abstract <A> A accept(PosSwitch<A> ps);
	
	@Override
  public NoKey key() {
		return null;
	}

	/**
	 * Determines whether a node is inside the subtree rooted at some other node.
	 * 
	 * @param node the node
	 * @param tree the subtree
	 * @return {@literal true} if {@code node} is a descendant of {@code tree}, {@literal false} otherwise
	 */
	public static boolean inSubtree(CAstNode node, CAstNode tree) {
		if(node == tree)
			return true;
		if(tree == null)
			return false;
		for(int i=0;i<tree.getChildCount();++i)
			if(inSubtree(node, tree.getChild(i)))
				return true;
		return false;
	}
}
