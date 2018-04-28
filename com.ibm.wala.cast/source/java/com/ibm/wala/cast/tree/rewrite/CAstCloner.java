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
package com.ibm.wala.cast.tree.rewrite;

import java.util.Collection;
import java.util.Map;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.util.collections.Pair;

public class CAstCloner extends CAstBasicRewriter<CAstBasicRewriter.NonCopyingContext> {

  public CAstCloner(CAst Ast, boolean recursive) {
    this(Ast, new NonCopyingContext(), recursive);
  }

  public CAstCloner(CAst Ast) {
    this(Ast, false);
  }

  protected CAstCloner(CAst Ast, NonCopyingContext context, boolean recursive) {
    super(Ast, context, recursive);
  }

  @Override
  protected CAstNode copyNodes(CAstNode root, final CAstControlFlowMap cfg, NonCopyingContext c, Map<Pair<CAstNode,NoKey>, CAstNode> nodeMap) {
    final Pair<CAstNode, NoKey> pairKey = Pair.make(root, c.key());
    if (root instanceof CAstOperator) {
      nodeMap.put(pairKey, root);
      return root;
    } else if (root.getValue() != null) {
      CAstNode copy = Ast.makeConstant(root.getValue());
      assert !nodeMap.containsKey(pairKey);
      nodeMap.put(pairKey, copy);
      return copy;
    } else {
      CAstNode newChildren[] = new CAstNode[root.getChildCount()];

      for (int i = 0; i < root.getChildCount(); i++) {
        newChildren[i] = copyNodes(root.getChild(i), cfg, c, nodeMap);
      }

      CAstNode copy = Ast.makeNode(root.getKind(), newChildren);
      assert !nodeMap.containsKey(pairKey);
      nodeMap.put(pairKey, copy);
      return copy;
    }
  }

  public Rewrite copy(CAstNode root, final CAstControlFlowMap cfg, final CAstSourcePositionMap pos, final CAstNodeTypeMap types,
      final Map<CAstNode, Collection<CAstEntity>> children) {
    return rewrite(root, cfg, pos, types, children);
  }
}
