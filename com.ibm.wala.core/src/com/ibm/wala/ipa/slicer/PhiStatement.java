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
import com.ibm.wala.ssa.SSAPhiInstruction;

/** identifier of a phi instruction */
public class PhiStatement extends Statement {
  private final SSAPhiInstruction phi;

  public PhiStatement(CGNode node, SSAPhiInstruction phi) {
    super(node);
    this.phi = phi;
  }

  @Override
  public Kind getKind() {
    return Kind.PHI;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      PhiStatement other = (PhiStatement) obj;
      return getNode().equals(other.getNode()) && phi.getDef() == other.phi.getDef();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 3691 * phi.getDef() + getNode().hashCode();
  }

  @Override
  public String toString() {
    return "PHI " + getNode() + ':' + phi;
  }

  public SSAPhiInstruction getPhi() {
    return phi;
  }
}
