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
 * This class represents instructions that store to local variables.
 */
public final class StoreInstruction extends Instruction implements IStoreInstruction {
  final private int index;

  protected StoreInstruction(short opcode, int index) {
    super(opcode);
    this.index = index;
  }

  private final static StoreInstruction[] preallocated = preallocate();

  private static StoreInstruction[] preallocate() {
    StoreInstruction[] r = new StoreInstruction[5 * 16];
    for (int p = 0; p < 5; p++) {
      for (int i = 0; i < 4; i++) {
        r[p * 16 + i] = new StoreInstruction((short) (OP_istore_0 + i + p * 4), i);
      }
      for (int i = 4; i < 16; i++) {
        r[p * 16 + i] = new StoreInstruction((short) (OP_istore + p), i);
      }
    }
    return r;
  }

  public static StoreInstruction make(String type, int index) throws IllegalArgumentException {
    int t = Util.getTypeIndex(type);
    if (t < 0 || t > TYPE_Object_index) {
      throw new IllegalArgumentException("Cannot store local of type " + type);
    }
    if (index < 16) {
      return preallocated[t * 16 + index];
    } else {
      return new StoreInstruction((short) (OP_istore + t), index);
    }
  }

  /**
   * @return the index of the local variable stored
   */
  @Override
  public int getVarIndex() {
    return index;
  }

  @Override
  public String getType() {
    if (opcode < OP_istore_0) {
      return indexedTypes[opcode - OP_istore];
    } else {
      return indexedTypes[(opcode - OP_istore_0) / 4];
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof StoreInstruction) {
      StoreInstruction i = (StoreInstruction) o;
      return i.index == index && i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return opcode + index * 148091891;
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  @Override
  public String toString() {
    return "LocalStore(" + getType() + "," + index + ")";
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitLocalStore(this);
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
