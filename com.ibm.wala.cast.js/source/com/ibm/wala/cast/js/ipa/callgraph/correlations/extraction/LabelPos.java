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
package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import com.ibm.wala.cast.tree.CAstNode;

/**
 * A {@link NodePos} for a node that labels a CFG edge; currently only seems to occur with 'switch'
 * statements.
 *
 * @author mschaefer
 */
public class LabelPos extends NodePos {
  private final CAstNode parent;
  private final NodePos parent_pos;

  public LabelPos(CAstNode node, NodePos parent_pos) {
    this.parent = node;
    this.parent_pos = parent_pos;
  }

  public CAstNode getParent() {
    return parent;
  }

  public NodePos getParentPos() {
    return parent_pos;
  }

  @Override
  public <A> A accept(PosSwitch<A> ps) {
    return ps.caseLabelPos(this);
  }
}
