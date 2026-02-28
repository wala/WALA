/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;
import java.util.Objects;

/** Identifier of a statement in an SDG. */
public abstract class Statement {
  public enum Kind {
    NORMAL,
    PHI,
    PI,
    CATCH,
    PARAM_CALLER,
    PARAM_CALLEE,
    NORMAL_RET_CALLER,
    NORMAL_RET_CALLEE,
    EXC_RET_CALLER,
    EXC_RET_CALLEE,
    HEAP_PARAM_CALLER,
    HEAP_PARAM_CALLEE,
    HEAP_RET_CALLER,
    HEAP_RET_CALLEE,
    METHOD_ENTRY,
    METHOD_EXIT
  }

  private final CGNode node;

  public Statement(final CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("null node");
    }
    this.node = node;
  }

  public abstract Kind getKind();

  public CGNode getNode() {
    return node;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((node == null) ? 0 : node.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    if (!Objects.equals(node, ((Statement) obj).node)) return false;
    return true;
  }

  @Override
  public String toString() {
    return getKind().toString() + ':' + getNode();
  }
}
