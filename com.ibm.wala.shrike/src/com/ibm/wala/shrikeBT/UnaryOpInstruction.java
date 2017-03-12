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
 * This class represents unary operators where the result is the same type as the operand.
 */
public final class UnaryOpInstruction extends Instruction implements IUnaryOpInstruction {
  protected UnaryOpInstruction(short opcode) {
    super(opcode);
  }

  private final static UnaryOpInstruction[] preallocated = preallocate();

  private static UnaryOpInstruction[] preallocate() {
    UnaryOpInstruction[] r = new UnaryOpInstruction[OP_dneg - OP_ineg + 1];
    for (int i = 0; i < r.length; i++) {
      r[i] = new UnaryOpInstruction((short) (OP_ineg + i));
    }
    return r;
  }

  public static UnaryOpInstruction make(String type, IUnaryOpInstruction.Operator operator) throws IllegalArgumentException {
    int t = Util.getTypeIndex(type);
    if (t < 0 || t > TYPE_double_index) {
      throw new IllegalArgumentException("Type " + type + " cannot have a unary operator applied");
    }
    return preallocated[t];
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof UnaryOpInstruction) {
      UnaryOpInstruction i = (UnaryOpInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public IUnaryOpInstruction.Operator getOperator() {
    return IUnaryOpInstruction.Operator.NEG;
  }

  @Override
  public int hashCode() {
    return opcode;
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  @Override
  public String getPushedType(String[] types) {
    return getType();
  }

  @Override
  public byte getPushedWordSize() {
    return Util.getWordSize(getType());
  }

  @Override
  public String getType() {
    return indexedTypes[opcode - OP_ineg];
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitUnaryOp(this);
  }

  @Override
  public String toString() {
    return "UnaryOp(" + getType() + "," + getOperator() + ")";
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
