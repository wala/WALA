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
 * This class represents binary operator instructions for which the operands and the result all have the same type.
 */
final public class BinaryOpInstruction extends Instruction implements IBinaryOpInstruction {
  protected BinaryOpInstruction(short opcode) {
    super(opcode);
  }

  private final static BinaryOpInstruction[] arithmeticOps = preallocateArithmeticOps();

  private final static BinaryOpInstruction[] logicalOps = preallocateLogicalOps();

  private static BinaryOpInstruction[] preallocateArithmeticOps() {
    BinaryOpInstruction[] r = new BinaryOpInstruction[OP_drem - OP_iadd + 1];
    for (short i = OP_iadd; i <= OP_drem; i++) {
      r[i - OP_iadd] = new BinaryOpInstruction(i);
    }
    return r;
  }

  private static BinaryOpInstruction[] preallocateLogicalOps() {
    BinaryOpInstruction[] r = new BinaryOpInstruction[OP_lxor - OP_iand + 1];
    for (short i = OP_iand; i <= OP_lxor; i++) {
      r[i - OP_iand] = new BinaryOpInstruction(i);
    }
    return r;
  }

  public static BinaryOpInstruction make(String type, Operator operator) throws IllegalArgumentException {
    if (operator == null) {
      throw new IllegalArgumentException("operator is null");
    }
    int t = Util.getTypeIndex(type);
    if (t < 0) {
      throw new IllegalArgumentException("Invalid type for BinaryOp: " + type);
    }

    if (operator.compareTo(Operator.REM) <= 0) {
      if (t > TYPE_double_index) {
        throw new IllegalArgumentException("Invalid type for BinaryOp: " + type);
      }
      return arithmeticOps[(operator.ordinal() - Operator.ADD.ordinal()) * 4 + t];
    } else {
      if (t > TYPE_long_index) {
        throw new IllegalArgumentException("Cannot use logical binaryOps on floating point type: " + type);
      }
      return logicalOps[(operator.ordinal() - Operator.AND.ordinal()) * 2 + t];
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof BinaryOpInstruction) {
      BinaryOpInstruction i = (BinaryOpInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public Operator getOperator() {
    if (opcode < OP_iand) {
      // For these opcodes, there are 4 variants (i,l,f,d)
      return Operator.values()[(opcode - OP_iadd) / 4];
    } else {
      // For these opcodes there are 2 variants (i,l)
      // Note that AND is values()[5]
      return Operator.values()[5 + (opcode - OP_iand) / 2];
    }
  }

  @Override
  public int hashCode() {
    return opcode + 13901901;
  }

  @Override
  public int getPoppedCount() {
    return 2;
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
    int t;
    if (opcode < OP_iand) {
      t = (opcode - OP_iadd) & 3;
    } else {
      t = (opcode - OP_iand) & 1;
    }
    return indexedTypes[t];
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitBinaryOp(this);
  }

  @Override
  public String toString() {
    return "BinaryOp(" + getType() + "," + getOperator() + ")";
  }

  @Override
  public boolean isPEI() {
    return opcode == Constants.OP_idiv || opcode == Constants.OP_ldiv || opcode == Constants.OP_irem || opcode == Constants.OP_lrem;
  }

  @Override
  public boolean throwsExceptionOnOverflow() {
    return false;
  }

  @Override
  public boolean isUnsigned() {
    return false;
  }
}
