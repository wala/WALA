/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * A statement that has a corresponding index in the
 * bytecode (actually in the ShrikeBT representation)
 * 
 * @author sjfink
 *
 */
public class NormalStatement extends Statement {

  private final int instructionIndex;
  
  public NormalStatement(CGNode node, int instructionIndex) {
    super(node);
    this.instructionIndex = instructionIndex;
  }

  @Override
  public Kind getKind() {
    return Kind.NORMAL;
  }

  @Override
  public boolean equals(Object obj) {
    if (getClass().equals(obj.getClass())) {
      NormalStatement other = (NormalStatement) obj;
      return getNode().equals(other.getNode()) && instructionIndex == other.instructionIndex;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 3691 * instructionIndex + getNode().hashCode();
  }

  @Override
  public String toString() {
    return getNode().getMethod().getSignature() + ":" +  getInstruction().toString();
  }
  
  public SSAInstruction getInstruction() {
    return getNode().getIR(new WarningSet()).getInstructions()[instructionIndex];
  }

  public int getInstructionIndex() {
    return instructionIndex;
  }



}
