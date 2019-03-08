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
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;

/** identifier of a GetCaughtException instruction */
public class GetCaughtExceptionStatement extends Statement {
  private final SSAGetCaughtExceptionInstruction st;

  public GetCaughtExceptionStatement(CGNode node, SSAGetCaughtExceptionInstruction st) {
    super(node);
    this.st = st;
  }

  @Override
  public Kind getKind() {
    return Kind.CATCH;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      GetCaughtExceptionStatement other = (GetCaughtExceptionStatement) obj;
      return getNode().equals(other.getNode()) && st.equals(other.st);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 3691 * st.hashCode() + getNode().hashCode();
  }

  @Override
  public String toString() {
    return getNode() + ":" + st;
  }

  public SSAGetCaughtExceptionInstruction getInstruction() {
    return st;
  }
}
