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

/**
 * A {@link Statement} representing method exit used as a dummy exit for starting propagation to a
 * seed statement in backwards slicing.
 */
public class MethodExitStatement extends Statement {

  public MethodExitStatement(CGNode node) {
    super(node);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      MethodExitStatement other = (MethodExitStatement) obj;
      return getNode().equals(other.getNode());
    } else {
      return false;
    }
  }

  @Override
  public Kind getKind() {
    return Kind.METHOD_EXIT;
  }

  @Override
  public int hashCode() {
    return getKind().hashCode() + 9901 * getNode().hashCode();
  }

  @Override
  public String toString() {
    return getKind() + ":" + getNode();
  }
}
