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

import com.ibm.wala.cast.tree.*;

import java.util.Map;

public class CAstCloner extends CAstRewriter<Object> {

  public CAstCloner(CAst Ast) {
    super(Ast, false, null);
  }

  protected CAstNode copyNodes(CAstNode root, 
			       Object ignoredContext,
			       Map<CAstNode, CAstNode> nodeMap) 
  {
    if (root instanceof CAstOperator) {
      nodeMap.put(root, root);
      return root;
    } else if (root.getValue() != null) {
      CAstNode copy = Ast.makeConstant( root.getValue() );
      nodeMap.put(root, copy);
      return copy;
    } else {
      CAstNode newChildren[] = new CAstNode[ root.getChildCount() ];

      for(int i = 0; i < root.getChildCount(); i++) {
	newChildren[i] = copyNodes(root.getChild(i), ignoredContext, nodeMap);
      }

      CAstNode copy = Ast.makeNode(root.getKind(), newChildren);
      nodeMap.put(root, copy);
      return copy;
    }
  }

  public Rewrite copy(CAstNode root, 
		      final CAstControlFlowMap cfg,
		      final CAstSourcePositionMap pos,
		      final CAstNodeTypeMap types,
		      final Map children) 
  {
    return rewrite(root, cfg, pos, types, children);
  }
}
