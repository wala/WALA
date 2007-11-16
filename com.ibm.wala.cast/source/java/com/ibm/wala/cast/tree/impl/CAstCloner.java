/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.tree.impl;

import java.util.Collection;
import java.util.Map;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

public class CAstCloner extends CAstBasicRewriter {

  public CAstCloner(CAst Ast) {
    super(Ast, false);
  }

  protected CAstNode copyNodes(CAstNode root, 
			       NonCopyingContext c,
			       Map nodeMap) 
  {
    if (root instanceof CAstOperator) {
      nodeMap.put(Pair.make(root, c.key()), root);
      return root;
    } else if (root.getValue() != null) {
      CAstNode copy = Ast.makeConstant( root.getValue() );
      Assertions._assert(! nodeMap.containsKey(root));
      nodeMap.put(Pair.make(root, c.key()), copy);
      return copy;
    } else {
      CAstNode newChildren[] = new CAstNode[ root.getChildCount() ];

      for(int i = 0; i < root.getChildCount(); i++) {
	newChildren[i] = copyNodes(root.getChild(i), c, nodeMap);
      }

      CAstNode copy = Ast.makeNode(root.getKind(), newChildren);
      Assertions._assert(! nodeMap.containsKey(root));
      nodeMap.put(Pair.make(root, c.key()), copy);
      return copy;
    }
  }

  public Rewrite copy(CAstNode root, 
		      final CAstControlFlowMap cfg,
		      final CAstSourcePositionMap pos,
		      final CAstNodeTypeMap types,
		      final Map<CAstNode,Collection<CAstEntity>> children) 
  {
    return rewrite(root, cfg, pos, types, children);
  }
}
