/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import com.ibm.wala.util.collections.Pair;

/**
 * combination of 2 pi node policies
 */
public class CompoundPiPolicy implements SSAPiNodePolicy {
  
  public static CompoundPiPolicy createCompoundPiPolicy(SSAPiNodePolicy p1, SSAPiNodePolicy p2) {
    return new CompoundPiPolicy(p1, p2);
  }

  private final SSAPiNodePolicy p1;
  private final SSAPiNodePolicy p2;
  
  private CompoundPiPolicy(SSAPiNodePolicy p1, SSAPiNodePolicy p2) {
    this.p1 = p1;
    this.p2 = p2;
    if (p1 == null) {
      throw new IllegalArgumentException("p1 is null");
    }
    if (p2 == null) {
      throw new IllegalArgumentException("p2 is null");
    }
  }

  public Pair<Integer, SSAInstruction> getPi(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    Pair<Integer, SSAInstruction> result = p1.getPi(cond, def1, def2, symbolTable);
    if (result != null) {
      return result;
    }
    return p2.getPi(cond, def1, def2, symbolTable);
  }
  

  public Pair<Integer, SSAInstruction> getPi(SSAAbstractInvokeInstruction call, SymbolTable symbolTable) {
    Pair<Integer, SSAInstruction> result = p1.getPi(call, symbolTable);
    if (result != null) {
      return result;
    }
    return p2.getPi(call, symbolTable);
  }
  

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((p1 == null) ? 0 : p1.hashCode());
    result = prime * result + ((p2 == null) ? 0 : p2.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final CompoundPiPolicy other = (CompoundPiPolicy) obj;
    if (p1 == null) {
      if (other.p1 != null)
        return false;
    } else if (!p1.equals(other.p1))
      return false;
    if (p2 == null) {
      if (other.p2 != null)
        return false;
    } else if (!p2.equals(other.p2))
      return false;
    return true;
  }

  
}
