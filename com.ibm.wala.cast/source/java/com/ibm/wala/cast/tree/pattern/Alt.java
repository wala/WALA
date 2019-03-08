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
package com.ibm.wala.cast.tree.pattern;

import com.ibm.wala.cast.tree.CAstNode;

/**
 * Pattern to match one of two alternatives.
 *
 * @author mschaefer
 */
public class Alt implements NodePattern {
  private final NodePattern left, right;

  public Alt(NodePattern left, NodePattern right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public boolean matches(CAstNode node) {
    return left.matches(node) || right.matches(node);
  }
}
