/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.util.ssa;

import com.ibm.wala.ssa.SSAInstruction;

public record InstructionByIIndexWrapper<T extends SSAInstruction>(T instruction) {
  // intentional: uses iIndex() instead of the instruction component so two
  // distinct SSAInstruction objects with the same index are treated as equal
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + instruction().iIndex();
    return result;
  }

  // intentional: uses iIndex() instead of the instruction component
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    InstructionByIIndexWrapper<?> other = (InstructionByIIndexWrapper<?>) obj;
    if (instruction().iIndex() != other.instruction().iIndex()) return false;
    return true;
  }

  /**
   * @deprecated Use {@link #instruction()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public T getInstruction() {
    return instruction();
  }

  public InstructionByIIndexWrapper {
    if (instruction.iIndex() < 0) {
      throw new IllegalArgumentException("The given instruction, can not be identified by iindex.");
    }
  }
}
