/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import com.ibm.wala.cast.tree.CAstNode;

/**
 * A {@link NodePos} for a non-root node; includes information about the parent node, the child
 * index, and the position of the parent node.
 *
 * @author mschaefer
 */
public class ChildPos extends NodePos {
  private final CAstNode parent;
  private final int index;
  private final NodePos parent_pos;

  public ChildPos(CAstNode parent, int index, NodePos parent_pos) {
    this.parent = parent;
    this.index = index;
    this.parent_pos = parent_pos;
  }

  public CAstNode getParent() {
    return parent;
  }

  public int getIndex() {
    return index;
  }

  public NodePos getParentPos() {
    return parent_pos;
  }

  public CAstNode getChild() {
    return parent.getChild(index);
  }

  public ChildPos getChildPos(int index) {
    return new ChildPos(this.getChild(), index, this);
  }

  @Override
  public <A> A accept(PosSwitch<A> ps) {
    return ps.caseChildPos(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + index;
    result = prime * result + ((parent == null) ? 0 : parent.hashCode());
    result = prime * result + ((parent_pos == null) ? 0 : parent_pos.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ChildPos other = (ChildPos) obj;
    if (index != other.index) return false;
    if (parent == null) {
      if (other.parent != null) return false;
    } else if (!parent.equals(other.parent)) return false;
    if (parent_pos == null) {
      if (other.parent_pos != null) return false;
    } else if (!parent_pos.equals(other.parent_pos)) return false;
    return true;
  }
}
