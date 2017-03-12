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
 * This class represents the ?astore instructions.
 */
final public class ArrayStoreInstruction extends Instruction implements IArrayStoreInstruction {
  protected ArrayStoreInstruction(short opcode) {
    super(opcode);
  }

  private final static ArrayStoreInstruction[] preallocated = preallocate();

  private static ArrayStoreInstruction[] preallocate() {
    ArrayStoreInstruction[] r = new ArrayStoreInstruction[OP_sastore - OP_iastore + 2];
    for (short i = OP_iastore; i <= OP_sastore; i++) {
      r[i - OP_iastore] = new ArrayStoreInstruction(i);
    }
    r[OP_sastore - OP_iastore + 1] = r[OP_baload - OP_iaload];
    return r;
  }

  public static ArrayStoreInstruction make(String type) throws IllegalArgumentException {
    int i = Util.getTypeIndex(type);
    if (i < 0 || i > TYPE_boolean_index) {
      throw new IllegalArgumentException("Invalid type " + type + " for ArrayStoreInstruction");
    }
    return preallocated[i];
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ArrayStoreInstruction) {
      ArrayStoreInstruction i = (ArrayStoreInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return opcode + 148791;
  }

  @Override
  public int getPoppedCount() {
    return 3;
  }

  @Override
  public String getType() {
    return Decoder.indexedTypes[opcode - OP_iastore];
  }

  @Override
  public String toString() {
    return "ArrayStore(" + getType() + ")";
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitArrayStore(this);
  }

  @Override
  public boolean isPEI() {
    return true;
  }
}
