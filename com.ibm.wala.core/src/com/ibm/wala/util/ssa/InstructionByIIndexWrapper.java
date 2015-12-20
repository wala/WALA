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
package com.ibm.wala.util.ssa;

import com.ibm.wala.ssa.SSAInstruction;

public class InstructionByIIndexWrapper<T extends SSAInstruction> {
  private T instruction;
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getInstruction().iindex;
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
    InstructionByIIndexWrapper other = (InstructionByIIndexWrapper) obj;
    if (getInstruction().iindex != other.getInstruction().iindex)
      return false;
    return true;
  }

  public T getInstruction() {
    return instruction;
  }
    
  public InstructionByIIndexWrapper(T instruction) {
    if (instruction.iindex < 0) {
      throw new IllegalArgumentException("The given instruction, can not be identified by iindex.");
    }
    this.instruction = instruction;    
  }


}
