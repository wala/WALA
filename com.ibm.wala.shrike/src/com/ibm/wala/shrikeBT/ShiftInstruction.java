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
 * ShiftInstructions are distinguished from BinaryOpInstructions because most
 * binary operations in the JVM require both parameters to be the same type, but
 * shifts always take one int parameter.
 */
public final class ShiftInstruction extends Instruction {
  public enum Operator implements BinaryOpInstruction.IOperator {
    SHL, SHR, USHR;
  }

  protected ShiftInstruction(short opcode) {
    this.opcode = opcode;
  }

  private static final ShiftInstruction[] preallocated = preallocate();

  private static ShiftInstruction[] preallocate() {
    ShiftInstruction[] r = new ShiftInstruction[OP_lushr - OP_ishl + 1];
    for (int i = 0; i < r.length; i++) {
      r[i] = new ShiftInstruction((short) (i + OP_ishl));
    }
    return r;
  }

  public static ShiftInstruction make(String type, Operator operator) throws IllegalArgumentException {
    if (operator == null) {
      throw new IllegalArgumentException("operator is null");
    }
    int t = Util.getTypeIndex(type);
    if (t < 0 || t > TYPE_long_index) {
      throw new IllegalArgumentException("Cannot apply shift to type " + type);
    }

    return preallocated[(operator.ordinal() - Operator.SHL.ordinal()) * 2 + t];
  }

  public boolean equals(Object o) {
    if (o instanceof ShiftInstruction) {
      ShiftInstruction i = (ShiftInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  public Operator getOperator() {
    return Operator.values()[(opcode - OP_ishl) / 2];
  }

  public int hashCode() {
    return opcode;
  }

  public int getPoppedCount() {
    return 2;
  }

  public String getPushedType(String[] types) {
    return getType();
  }

  public byte getPushedWordSize() {
    return Util.getWordSize(getType());
  }

  public String getType() {
    return indexedTypes[(opcode - OP_ishl) & 1];
  }

  public void visit(Visitor v) throws NullPointerException {
    v.visitShift(this);
  }

  public String toString() {
    return "Shift(" + getType() + "," + getOperator() + ")";
  }
    /* (non-Javadoc)
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
    return false;
  }
}