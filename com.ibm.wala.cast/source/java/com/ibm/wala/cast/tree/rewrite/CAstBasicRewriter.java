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
package com.ibm.wala.cast.tree.rewrite;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import java.util.HashMap;
import java.util.Map;

/** abstract base class for {@link CAstRewriter}s that do no cloning of nodes */
public abstract class CAstBasicRewriter<T extends CAstBasicRewriter.NonCopyingContext>
    extends CAstRewriter<T, CAstBasicRewriter.NoKey> {

  /** context indicating that no cloning is being performed */
  public static class NonCopyingContext implements CAstRewriter.RewriteContext<NoKey> {
    private final Map<Object, Object> nodeMap = new HashMap<>();

    public Map<Object, Object> nodeMap() {
      return nodeMap;
    }

    @Override
    public NoKey key() {
      return null;
    }
  }

  /** key indicating that no duplication is being performed */
  public static class NoKey implements CAstRewriter.CopyKey<NoKey> {
    private NoKey() {
      Assertions.UNREACHABLE();
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
      return o == this;
    }

    @Override
    public NoKey parent() {
      return null;
    }
  }

  protected CAstBasicRewriter(CAst Ast, T context, boolean recursive) {
    super(Ast, recursive, context);
  }

  @Override
  protected abstract CAstNode copyNodes(
      CAstNode root,
      final CAstControlFlowMap cfg,
      T context,
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap);
}
