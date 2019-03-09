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
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.NodeKey;

public class JSSyntheticParameterKey extends NodeKey {
  public static int lengthOffset = -1;

  private final int param;

  public JSSyntheticParameterKey(CGNode node, int param) {
    super(node);
    this.param = param;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof JSSyntheticParameterKey)
        && ((JSSyntheticParameterKey) o).param == param
        && internalEquals(o);
  }

  @Override
  public int hashCode() {
    return param * getNode().hashCode();
  }

  @Override
  public String toString() {
    return "p" + param + ':' + getNode().getMethod().getName();
  }
}
