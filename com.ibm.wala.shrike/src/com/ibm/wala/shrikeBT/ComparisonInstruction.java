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
 * This class represents comparisons between floats, longs and doubles.
 */
final public class ComparisonInstruction extends Instruction implements IComparisonInstruction {
  protected ComparisonInstruction(short opcode) {
    super(opcode);
  }

  private final static ComparisonInstruction preallocatedLCMP = new ComparisonInstruction(OP_lcmp);

  private final static ComparisonInstruction[] preallocatedFloatingCompares = preallocateFloatingCompares();

  private static ComparisonInstruction[] preallocateFloatingCompares() {
    ComparisonInstruction[] r = new ComparisonInstruction[OP_dcmpg - OP_fcmpl + 1];
    for (short i = OP_fcmpl; i <= OP_dcmpg; i++) {
      r[i - OP_fcmpl] = new ComparisonInstruction(i);
    }
    return r;
  }

  public static ComparisonInstruction make(String type, Operator operator) throws IllegalArgumentException {
    int t = Util.getTypeIndex(type);
    switch (t) {
    case TYPE_long_index:
      if (operator != Operator.CMP) {
        throw new IllegalArgumentException("Operator " + operator + " is not a valid comparison operator for longs");
      } else {
        return preallocatedLCMP;
      }
    case TYPE_float_index:
    case TYPE_double_index:
      if (operator == Operator.CMP) {
        throw new IllegalArgumentException("Operator " + operator + " is not a valid comparison operator for floating point values");
      } else {
        return preallocatedFloatingCompares[(operator.ordinal() - Operator.CMPL.ordinal()) + (t - TYPE_float_index) * 2];
      }
    default:
      throw new IllegalArgumentException("Type " + type + " cannot be compared");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ComparisonInstruction) {
      ComparisonInstruction i = (ComparisonInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  /**
   * @return OPR_cmp (for long), OPR_cmpl, or OPR_cmpg (for float and double)
   */
  @Override
  public Operator getOperator() {
    switch (opcode) {
    case OP_lcmp:
      return Operator.CMP;
    case OP_fcmpl:
    case OP_dcmpl:
      return Operator.CMPL;
    case OP_dcmpg:
    case OP_fcmpg:
      return Operator.CMPG;
    default:
      throw new Error("Unknown opcode");
    }
  }

  @Override
  public String getType() {
    switch (opcode) {
    case OP_lcmp:
      return TYPE_long;
    case OP_fcmpg:
    case OP_fcmpl:
      return TYPE_float;
    case OP_dcmpl:
    case OP_dcmpg:
      return TYPE_double;
    default:
      throw new Error("Unknown opcode");
    }
  }

  @Override
  public int hashCode() {
    return opcode + 1391901;
  }

  @Override
  public int getPoppedCount() {
    return 2;
  }

  @Override
  public String getPushedType(String[] types) {
    return Constants.TYPE_boolean;
  }

  @Override
  public byte getPushedWordSize() {
    return 1;
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitComparison(this);
  }

  @Override
  public String toString() {
    return "Comparison(" + getType() + "," + getOperator() + ")";
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
