/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT;

/**
 * This class represents the athrow instruction.
 */
public final class ThrowInstruction extends Instruction {
  private static final ThrowInstruction preallocated = new ThrowInstruction();

  protected ThrowInstruction() {
    this.opcode = OP_athrow;
  }

  public static ThrowInstruction make() {
    return preallocated;
  }

  public boolean equals(Object o) {
    return o instanceof ThrowInstruction;
  }

  public boolean isFallThrough() {
    return false;
  }

  public int hashCode() {
    return 99651;
  }

  public int getPoppedCount() {
    return 1;
  }

  public void visit(Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitThrow(this);
  }

  public String toString() {
    return "Throw()";
  }
    /* (non-Javadoc)
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
    return true;
  }
}