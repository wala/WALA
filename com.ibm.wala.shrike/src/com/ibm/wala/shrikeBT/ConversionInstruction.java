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
 * This class represents instructions that convert from one primitive type to another.
 */
public final class ConversionInstruction extends Instruction implements IConversionInstruction {
  final private String fromType;

  final private String toType;

  protected ConversionInstruction(short opcode) {
    super(opcode);

    if (opcode < OP_i2b) {
      int k = opcode - OP_i2l;
      toType = indexedTypes[skip(k % 3, k / 3)];
    } else {
      toType = indexedTypes[(opcode - OP_i2b) + TYPE_byte_index];
    }

    if (opcode < OP_i2b) {
      fromType = indexedTypes[(opcode - OP_i2l) / 3];
    } else {
      fromType = TYPE_int;
    }
  }

  private final static ConversionInstruction[] preallocated = preallocate();

  private static ConversionInstruction[] preallocate() {
    ConversionInstruction[] r = new ConversionInstruction[OP_i2s - OP_i2l + 1];
    for (short i = OP_i2l; i <= OP_i2s; i++) {
      r[i - OP_i2l] = new ConversionInstruction(i);
    }
    return r;
  }

  public static ConversionInstruction make(String fromType, String toType) throws IllegalArgumentException {
    int from = Util.getTypeIndex(fromType);
    int to = Util.getTypeIndex(toType);
    if (from < 0 || from > TYPE_double_index) {
      throw new IllegalArgumentException("Cannot convert from type " + fromType);
    }
    if (from == TYPE_int_index && (to >= TYPE_byte_index && to <= TYPE_short_index)) {
      return preallocated[(OP_i2b - OP_i2l) + (to - TYPE_byte_index)];
    } else {
      if (to < 0 || to > TYPE_double_index) {
        throw new IllegalArgumentException("Cannot convert from type " + fromType + " to type " + toType);
      }
      if (to == from) {
        throw new IllegalArgumentException("Cannot convert from type " + fromType + " to same type");
      }
      return preallocated[from * 3 + (to > from ? to - 1 : to)];
    }
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  @Override
  public String getFromType() {
    return fromType;
  }

  private static int skip(int a, int b) {
    return a < b ? a : a + 1;
  }

  @Override
  public String getToType() {
    return toType;
  }

  @Override
  public String getPushedType(String[] types) {
    return getToType();
  }

  @Override
  public byte getPushedWordSize() {
    return Util.getWordSize(getToType());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ConversionInstruction) {
      ConversionInstruction i = (ConversionInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return opcode * 143111;
  }

  @Override
  public String toString() {
    return "Conversion(" + getFromType() + "," + getToType() + ")";
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitConversion(this);
  }

  @Override
  public boolean isPEI() {
    return false;
  }

  @Override
  public boolean throwsExceptionOnOverflow() {
    return false;
  }
}
