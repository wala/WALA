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
 * This class represents the ?aload instructions.
 */
final public class ArrayLoadInstruction extends Instruction implements IArrayLoadInstruction {
  protected ArrayLoadInstruction(short opcode) {
    super(opcode);
  }

  private final static ArrayLoadInstruction[] preallocated = preallocate();

  private static ArrayLoadInstruction[] preallocate() {
    ArrayLoadInstruction[] r = new ArrayLoadInstruction[OP_saload - OP_iaload + 2];
    for (short i = OP_iaload; i <= OP_saload; i++) {
      r[i - OP_iaload] = new ArrayLoadInstruction(i);
    }
    r[OP_saload - OP_iaload + 1] = r[OP_baload - OP_iaload];
    return r;
  }

  public static ArrayLoadInstruction make(String type) throws IllegalArgumentException {
    int i = Util.getTypeIndex(type);
    if (i < 0 || i > TYPE_boolean_index) {
      throw new IllegalArgumentException("Invalid type " + type + " for ArrayLoadInstruction");
    }
    return preallocated[i];
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ArrayLoadInstruction) {
      ArrayLoadInstruction i = (ArrayLoadInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return opcode + 9109101;
  }

  @Override
  public int getPoppedCount() {
    return 2;
  }

  @Override
  public String toString() {
    return "ArrayLoad(" + getType() + ")";
  }

  @Override
  public String getPushedType(String[] types) throws IllegalArgumentException {

    if (types == null) {
      return getType();
    } else {
      if (types.length <= 1) {
        throw new IllegalArgumentException("types.length <= 1");
      }
      String t = types[1];
      if (t == null) {
        throw new IllegalArgumentException("types[1] cannot be null");
      }
      if (t.startsWith("[")) {
        return t.substring(1);
      } else if (t.equals(TYPE_null)) {
        return TYPE_null;
      } else {
        return TYPE_unknown;
      }
    }
  }

  @Override
  public byte getPushedWordSize() {
    return Util.getWordSize(getType());
  }

  @Override
  public String getType() {
    return Constants.indexedTypes[opcode - OP_iaload];
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitArrayLoad(this);
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  /**
   * Java bytecode does not permit this. 
   * @see com.ibm.wala.shrikeBT.IMemoryOperation#isAddressOf()
   */
  @Override
  public boolean isAddressOf() {
    return false;
  }

}
