/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NonCopyingContext;
import com.ibm.wala.util.collections.Pair;
import java.util.List;
import java.util.Map;

public abstract class ConstantFoldingRewriter extends CAstBasicRewriter<NonCopyingContext> {

  protected ConstantFoldingRewriter(CAst Ast) {
    super(Ast, new NonCopyingContext(), true);
  }

  protected abstract Object eval(CAstOperator op, Object lhs, Object rhs);

  @Override
  protected CAstNode copyNodes(
      CAstNode root,
      CAstControlFlowMap cfg,
      NonCopyingContext context,
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    CAstNode result;
    if (root.getKind() == CAstNode.BINARY_EXPR) {
      CAstNode left = copyNodes(root.getChild(1), cfg, context, nodeMap);
      CAstNode right = copyNodes(root.getChild(2), cfg, context, nodeMap);
      Object v;
      if (left.getKind() == CAstNode.CONSTANT
          && right.getKind() == CAstNode.CONSTANT
          && (v = eval((CAstOperator) root.getChild(0), left.getValue(), right.getValue()))
              != null) {
        result = Ast.makeConstant(v);
      } else {
        result = Ast.makeNode(CAstNode.BINARY_EXPR, root.getChild(0), left, right);
      }

    } else if (root.getKind() == CAstNode.IF_EXPR || root.getKind() == CAstNode.IF_STMT) {
      CAstNode expr = copyNodes(root.getChild(0), cfg, context, nodeMap);
      if (expr.getKind() == CAstNode.CONSTANT && expr.getValue() == Boolean.TRUE) {
        result = copyNodes(root.getChild(1), cfg, context, nodeMap);
      } else if (expr.getKind() == CAstNode.CONSTANT
          && root.getChildCount() > 2
          && expr.getValue() == Boolean.FALSE) {
        result = copyNodes(root.getChild(2), cfg, context, nodeMap);
      } else {
        CAstNode then = copyNodes(root.getChild(1), cfg, context, nodeMap);
        if (root.getChildCount() == 3) {
          result =
              Ast.makeNode(
                  root.getKind(), expr, then, copyNodes(root.getChild(2), cfg, context, nodeMap));
        } else {
          result = Ast.makeNode(root.getKind(), expr, then);
        }
      }

    } else if (root.getKind() == CAstNode.CONSTANT) {
      result = Ast.makeConstant(root.getValue());

    } else if (root.getKind() == CAstNode.OPERATOR) {
      result = root;

    } else {
      List<CAstNode> children = copyChildrenArrayAndTargets(root, cfg, context, nodeMap);
      CAstNode copy = Ast.makeNode(root.getKind(), children);
      result = copy;
    }

    nodeMap.put(Pair.make(root, context.key()), result);
    return result;
  }
}
