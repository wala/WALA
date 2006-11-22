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
 * This class represents arraylength instructions.
 */
final public class ArrayLengthInstruction extends Instruction {
  protected ArrayLengthInstruction() {
    opcode = (byte) OP_arraylength;
  }

  private static ArrayLengthInstruction preallocated = new ArrayLengthInstruction();

  public static ArrayLengthInstruction make() {
    return preallocated;
  }

  public boolean equals(Object o) {
    return o instanceof ArrayLengthInstruction;
  }

  public int hashCode() {
    return 3180901;
  }

  public int getPoppedCount() {
    return 1;
  }

  public String getPushedType(String[] types) {
    return Constants.TYPE_int;
  }

  public byte getPushedWordSize() {
    return 1;
  }

  public void visit(Visitor v) {
    v.visitArrayLength(this);
  }

  public String toString() {
    return "ArrayLength()";
  }
    /* (non-Javadoc)
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
    return true;
  }
}