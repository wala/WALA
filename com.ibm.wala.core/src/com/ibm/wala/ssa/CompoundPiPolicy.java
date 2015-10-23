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

import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.util.collections.Pair;

/**
 * A Combination of 2 {@link SSAPiNodePolicy}s.  This policy will insert Pi nodes if either of two delegate policies says to.
 */
public class CompoundPiPolicy implements SSAPiNodePolicy {
  
  /**
   * @param p1 first {@link SSAPiNodePolicy} to delegate to
   * @param p2 second {@link SSAPiNodePolicy} to delegate to
   */
  public static CompoundPiPolicy createCompoundPiPolicy(SSAPiNodePolicy p1, SSAPiNodePolicy p2) {
    return new CompoundPiPolicy(p1, p2);
  }

  private final SSAPiNodePolicy p1;
  private final SSAPiNodePolicy p2;
  
  /**
   * @param p1 first {@link SSAPiNodePolicy} to delegate to
   * @param p2 second {@link SSAPiNodePolicy} to delegate to
   */
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

  /* 
   * @see com.ibm.wala.ssa.SSAPiNodePolicy#getPi(com.ibm.wala.ssa.SSAConditionalBranchInstruction, com.ibm.wala.ssa.SSAInstruction, com.ibm.wala.ssa.SSAInstruction, com.ibm.wala.ssa.SymbolTable)
   */
  @Override
  public Pair<Integer, SSAInstruction> getPi(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    Pair<Integer, SSAInstruction> result = p1.getPi(cond, def1, def2, symbolTable);
    if (result != null) {
      return result;
    }
    return p2.getPi(cond, def1, def2, symbolTable);
  }
  

  /* 
   * @see com.ibm.wala.ssa.SSAPiNodePolicy#getPi(com.ibm.wala.ssa.SSAAbstractInvokeInstruction, com.ibm.wala.ssa.SymbolTable)
   */
  @Override
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

  @Override
  public List<Pair<Integer, SSAInstruction>> getPis(SSAConditionalBranchInstruction cond, SSAInstruction def1, SSAInstruction def2,
      SymbolTable symbolTable) {
    LinkedList<Pair<Integer, SSAInstruction>> result = new LinkedList<>();
    result.addAll(p1.getPis(cond, def1, def2, symbolTable));
    result.addAll(p2.getPis(cond, def1, def2, symbolTable));
    return result;
  }

}
