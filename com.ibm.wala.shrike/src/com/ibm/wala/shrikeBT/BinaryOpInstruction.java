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
 * This class represents binary operator instructions for which the operands and
 * the result all have the same type.
 */
final public class BinaryOpInstruction extends Instruction {
  public interface IOperator {}
  
  public enum Operator implements IOperator {
    ADD, SUB, MUL, DIV, REM, AND, OR, XOR; 

    @Override
    public String toString() {
    	return super.toString().toLowerCase();
    }
  }

  protected BinaryOpInstruction(short opcode) {
    this.opcode = opcode;
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

  public boolean equals(Object o) {
    if (o instanceof BinaryOpInstruction) {
      BinaryOpInstruction i = (BinaryOpInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  /**
   * don't call this unless you really know what you're doing
   */
  public Operator getOperator() {
    if (opcode < OP_iand) {
      return Operator.values()[(opcode - OP_iadd) / 4];
    } else {
      return Operator.values()[(opcode - OP_iand) / 2];
    }
  }

  public int hashCode() {
    return opcode + 13901901;
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
    int t;
    if (opcode < OP_iand) {
      t = (opcode - OP_iadd) & 3;
    } else {
      t = (opcode - OP_iand) & 1;
    }
    return indexedTypes[t];
  }

  public void visit(Visitor v) {
    v.visitBinaryOp(this);
  }

  public String toString() {
    return "BinaryOp(" + getType() + "," + getOperator() + ")";
  }
    /* (non-Javadoc)
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
		return opcode == Constants.OP_idiv;
  }
}