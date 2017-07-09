/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.java.examples.ast;

import java.util.Map;

import com.ibm.wala.cast.java.types.JavaPrimitiveTypeMap;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.util.collections.Pair;

/**
 * transforms each synchronized block to execute under a conditional test
 * calling some method m(), where the block is duplicated in both the if and
 * else branches. The transformation enables a static analysis to separately
 * analyze the synchronized block for true and false return values from m().
 * 
 * See "Finding Concurrency-Related Bugs using Random Isolation," Kidd et al.,
 * VMCAI'09, Section 3
 */
public class SynchronizedBlockDuplicator extends
    CAstRewriter<CAstRewriter.RewriteContext<SynchronizedBlockDuplicator.UnwindKey>, SynchronizedBlockDuplicator.UnwindKey> {

  /**
   * key type used for cloning the synchronized blocks and the true and false
   * branches of the introduced conditional
   */
  class UnwindKey implements CAstRewriter.CopyKey<UnwindKey> {
    /**
     * are we on the true or false branch?
     */
    private boolean testDirection;

    /**
     * the AST node representing the synchronized block
     */
    private CAstNode syncNode;

    /**
     * key associated with the {@link RewriteContext context} of the parent AST
     * node of the synchronized block
     */
    private UnwindKey rest;

    private UnwindKey(boolean testDirection, CAstNode syncNode, UnwindKey rest) {
      this.rest = rest;
      this.syncNode = syncNode;
      this.testDirection = testDirection;
    }

    @Override
    public int hashCode() {
      return (testDirection ? 1 : -1) * System.identityHashCode(syncNode) * (rest == null ? 1 : rest.hashCode());
    }

    @Override
    public UnwindKey parent() {
      return rest;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof UnwindKey) && ((UnwindKey) o).testDirection == testDirection && ((UnwindKey) o).syncNode == syncNode
          && (rest == null ? ((UnwindKey) o).rest == null : rest.equals(((UnwindKey) o).rest));
    }

    @Override
    public String toString() {
      return "#" + testDirection + ((rest == null) ? "" : rest.toString());
    }
  }

  // private static final boolean DEBUG = false;

  /**
   * method to be invoked in the conditional test (program counter is ignored?
   * --MS)
   */
  private final CallSiteReference f;

  public SynchronizedBlockDuplicator(CAst Ast, boolean recursive, CallSiteReference f) {
    super(Ast, recursive, new RootContext());
    this.f = f;
  }

  public CAstEntity translate(CAstEntity original) {
    return rewrite(original);
  }

  /**
   * context used for nodes not contained in a synchronized block
   */
  private static class RootContext implements RewriteContext<UnwindKey> {

    @Override
    public UnwindKey key() {
      return null;
    }
  }

  /**
   * context used within synchronized blocks
   */
  class SyncContext implements RewriteContext<UnwindKey> {
    /**
     * context used for the parent AST node of the synchronized block
     */
    private final CAstRewriter.RewriteContext<UnwindKey> parent;

    /**
     * are we on the true or false branch of the introduced conditional?
     */
    private final boolean testDirection;

    /**
     * the AST node representing the synchronized block
     */
    private final CAstNode syncNode;

    private SyncContext(boolean testDirection, CAstNode syncNode, RewriteContext<UnwindKey> parent) {
      this.testDirection = testDirection;
      this.syncNode = syncNode;
      this.parent = parent;
    }

    @Override
    public UnwindKey key() {
      return new UnwindKey(testDirection, syncNode, parent.key());
    }

    /**
     * is n our synchronized block node or the synchronized block node of a
     * parent?
     */
    private boolean containsNode(CAstNode n) {
      if (n == syncNode) {
        return true;
      } else if (parent != null) {
        return contains(parent, n);
      } else {
        return false;
      }
    }
  }

  @Override
  protected CAstNode flowOutTo(Map<Pair<CAstNode, UnwindKey>, CAstNode> nodeMap, CAstNode oldSource, Object label, CAstNode oldTarget, CAstControlFlowMap orig,
      CAstSourcePositionMap src) {
    assert oldTarget == CAstControlFlowMap.EXCEPTION_TO_EXIT;
    return oldTarget;
  }

  private static boolean contains(RewriteContext<UnwindKey> c, CAstNode n) {
    if (c instanceof SyncContext) {
      return ((SyncContext) c).containsNode(n);
    } else {
      return false;
    }
  }

  /**
   * does root represent a synchronized block? if so, return the variable whose
   * lock is acquired. otherwise, return <code>null</code>
   */
  private static String isSynchronizedOnVar(CAstNode root) {
    if (root.getKind() == CAstNode.UNWIND) {
      CAstNode unwindBody = root.getChild(0);
      if (unwindBody.getKind() == CAstNode.BLOCK_STMT) {
        CAstNode firstStmt = unwindBody.getChild(0);
        if (firstStmt.getKind() == CAstNode.MONITOR_ENTER) {
          CAstNode expr = firstStmt.getChild(0);
          if (expr.getKind() == CAstNode.VAR) {
            String varName = (String) expr.getChild(0).getValue();

            CAstNode protectBody = root.getChild(1);
            if (protectBody.getKind() == CAstNode.MONITOR_EXIT) {
              CAstNode expr2 = protectBody.getChild(0);
              if (expr2.getKind() == CAstNode.VAR) {
                String varName2 = (String) expr2.getChild(0).getValue();
                if (varName.equals(varName2)) {
                  return varName;
                }
              }
            }
          }
        }
      }
    }

    return null;
  }

  @Override
  protected CAstNode copyNodes(CAstNode n, final CAstControlFlowMap cfg, RewriteContext<UnwindKey> c, Map<Pair<CAstNode, UnwindKey>, CAstNode> nodeMap) {
    String varName;
    // don't copy operators or constants (presumably since they are immutable?)
    if (n instanceof CAstOperator) {
      return n;

    } else if (n.getValue() != null) {
      return Ast.makeConstant(n.getValue());
    } else if (!contains(c, n) && (varName = isSynchronizedOnVar(n)) != null) {
      // we call contains() above since we pass n to copyNodes() below for the
      // true and false branches of the conditional, and in those recursive
      // calls we want n to be copied normally

      // the conditional test
      CAstNode test = Ast.makeNode(CAstNode.CALL, Ast.makeNode(CAstNode.VOID), Ast.makeConstant(f),
          Ast.makeNode(CAstNode.VAR, Ast.makeConstant(varName), Ast.makeConstant(JavaPrimitiveTypeMap.lookupType("boolean"))));

      // the new if conditional
      return Ast.makeNode(CAstNode.IF_STMT, test, copyNodes(n, cfg, new SyncContext(true, n, c), nodeMap),
          copyNodes(n, cfg, new SyncContext(false, n, c), nodeMap));

    } else {
      // invoke copyNodes() on the children with context c, ensuring, e.g., that
      // the body of a synchronized block gets cloned
      CAstNode[] newChildren = new CAstNode[n.getChildCount()];
      for (int i = 0; i < newChildren.length; i++)
        newChildren[i] = copyNodes(n.getChild(i), cfg, c, nodeMap);

      CAstNode newN = Ast.makeNode(n.getKind(), newChildren);

      nodeMap.put(Pair.make(n, c.key()), newN);

      return newN;
    }
  }
}
