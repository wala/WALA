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
 * This class represents conditional branches. A conditional branch tests two integers or two object references for some
 * relationship, and takes the branch if the relationship holds.
 */
public final class ConditionalBranchInstruction extends Instruction implements IConditionalBranchInstruction {

  final private int label;

  protected ConditionalBranchInstruction(short opcode, int label) {
    super(opcode);
    this.label = label;
  }

  public static ConditionalBranchInstruction make(String type, Operator operator, int label) throws IllegalArgumentException {
    int t = Util.getTypeIndex(type);
    short opcode;

    switch (t) {
    case TYPE_int_index:
      opcode = (short) (OP_if_icmpeq + (operator.ordinal() - Operator.EQ.ordinal()));
      break;
    case TYPE_Object_index:
      if (operator != Operator.EQ && operator != Operator.NE) {
        throw new IllegalArgumentException("Cannot test for condition " + operator + " on a reference");
      }
      opcode = (short) (OP_if_acmpeq + (operator.ordinal() - Operator.EQ.ordinal()));
      break;
    default:
      throw new IllegalArgumentException("Cannot conditionally branch on a value of type " + type);
    }

    return make(opcode, label);
  }

  // Relax from private to public by Xiangyu, to create ifeq
  public static ConditionalBranchInstruction make(short opcode, int label) throws IllegalArgumentException {
    if (opcode < OP_ifeq || opcode > OP_if_acmpne) {
      throw new IllegalArgumentException("Illegal opcode: " + opcode);
    }
    return new ConditionalBranchInstruction(opcode, label);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ConditionalBranchInstruction) {
      ConditionalBranchInstruction i = (ConditionalBranchInstruction) o;
      return i.opcode == opcode && i.label == label;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "ConditionalBranch(" + getType() + "," + getOperator() + "," + label + ")";
  }

  @Override
  public int[] getBranchTargets() {
    int[] r = { label };
    return r;
  }

  @Override
  public int getTarget() {
    return label;
  }

  @Override
  public IInstruction redirectTargets(int[] targetMap) throws IllegalArgumentException {
    if (targetMap == null) {
      throw new IllegalArgumentException("targetMap is null");
    }
    try {
      return make(opcode, targetMap[label]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("bad target map", e);
    }
  }

  @Override
  public Operator getOperator() {
    if (opcode < OP_if_acmpeq) {
      return Operator.values()[opcode - OP_if_icmpeq];
    } else {
      return Operator.values()[opcode - OP_if_acmpeq];
    }
  }

  @Override
  public String getType() {
    return opcode < OP_if_acmpeq ? TYPE_int : TYPE_Object;
  }

  @Override
  public int hashCode() {
    return 30190 * opcode + 384101 * label;
  }

  @Override
  public int getPoppedCount() {
    // Xiangyu, to support if_eq (if_ne)...
    if (opcode >= Constants.OP_ifeq && opcode <= Constants.OP_ifle)
      return 1;
    return 2;
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitConditionalBranch(this);
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
