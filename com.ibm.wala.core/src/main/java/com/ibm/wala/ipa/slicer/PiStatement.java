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
import com.ibm.wala.ssa.SSAPiInstruction;

/** identifier of a Pi instruction */
public class PiStatement extends Statement {
  private final SSAPiInstruction pi;

  public PiStatement(CGNode node, SSAPiInstruction pi) {
    super(node);
    this.pi = pi;
  }

  @Override
  public Kind getKind() {
    return Kind.PI;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      PiStatement other = (PiStatement) obj;
      return getNode().equals(other.getNode()) && pi.getDef() == other.getPi().getDef();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 3691 * pi.getDef() + getNode().hashCode();
  }

  @Override
  public String toString() {
    return getNode() + ":" + pi;
  }

  public SSAPiInstruction getPi() {
    return pi;
  }
}
