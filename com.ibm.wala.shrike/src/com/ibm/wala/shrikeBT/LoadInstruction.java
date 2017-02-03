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
 * This class represents local variable load instructions.
 */
public final class LoadInstruction extends Instruction implements ILoadInstruction {
  private final int index;

  protected LoadInstruction(short opcode, int index) {
    super(opcode);
    this.index = index;
  }

  private final static LoadInstruction[] preallocated = preallocate();

  private static LoadInstruction[] preallocate() {
    LoadInstruction[] r = new LoadInstruction[5 * 16];
    for (int p = 0; p < 5; p++) {
      for (int i = 0; i < 4; i++) {
        r[p * 16 + i] = new LoadInstruction((short) (OP_iload_0 + i + p * 4), i);
      }
      for (int i = 4; i < 16; i++) {
        r[p * 16 + i] = new LoadInstruction((short) (OP_iload + p), i);
      }
    }
    return r;
  }

  public static LoadInstruction make(String type, int index) throws IllegalArgumentException {
    int t = Util.getTypeIndex(type);
    if (t < 0 || t > TYPE_Object_index) {
      throw new IllegalArgumentException("Cannot load local of type " + type);
    }
    if (index < 16) {
      return preallocated[t * 16 + index];
    } else {
      return new LoadInstruction((short) (OP_iload + t), index);
    }
  }

  /**
   * @return the index of the local variable loaded
   */
  @Override
  public int getVarIndex() {
    return index;
  }

  @Override
  public String getType() {
    if (opcode < OP_iload_0) {
      return indexedTypes[opcode - OP_iload];
    } else {
      return indexedTypes[(opcode - OP_iload_0) / 4];
    }
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
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitLocalLoad(this);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof LoadInstruction) {
      LoadInstruction i = (LoadInstruction) o;
      return i.index == index && i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return opcode + index * 19801901;
  }

  @Override
  public String toString() {
    return "LocalLoad(" + getType() + "," + index + ")";
  }

  @Override
  public boolean isPEI() {
    return false;
  }

  /**
   * Java does not permit this.
   * @see com.ibm.wala.shrikeBT.IMemoryOperation#isAddressOf()
   */
  @Override
  public boolean isAddressOf() {
    return false;
  }

 }
