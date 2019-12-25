/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.ipa.callgraph.CGNode;

/** A pointer key which provides a unique set for each local in each call graph node. */
public class LocalPointerKey extends AbstractLocalPointerKey {
  private final CGNode node;

  private final int valueNumber;

  public LocalPointerKey(CGNode node, int valueNumber) {
    super();
    this.node = node;
    this.valueNumber = valueNumber;
    if (valueNumber <= 0) {
      throw new IllegalArgumentException("illegal valueNumber: " + valueNumber);
    }
    if (node == null) {
      throw new IllegalArgumentException("null node");
    }
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof LocalPointerKey) {
      LocalPointerKey other = (LocalPointerKey) obj;
      return node.equals(other.node) && valueNumber == other.valueNumber;
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    return node.hashCode() * 23 + valueNumber;
  }

  @Override
  public String toString() {
    return "[" + node + ", v" + valueNumber + ']';
  }

  @Override
  public final CGNode getNode() {
    return node;
  }

  public final int getValueNumber() {
    return valueNumber;
  }

  public final boolean isParameter() {
    return valueNumber <= node.getMethod().getNumberOfParameters();
  }
}
