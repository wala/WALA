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
 * This instruction represents all return instructions.
 */
public final class ReturnInstruction extends Instruction {
  protected ReturnInstruction(short opcode) {
    super(opcode);
  }

  private static final ReturnInstruction[] preallocated = preallocate();

  private static final ReturnInstruction preallocatedVoid = new ReturnInstruction(OP_return);

  private static ReturnInstruction[] preallocate() {
    ReturnInstruction[] r = new ReturnInstruction[OP_areturn - OP_ireturn + 1];
    for (int i = 0; i < r.length; i++) {
      r[i] = new ReturnInstruction((short) (OP_ireturn + i));
    }
    return r;
  }

  public static ReturnInstruction make(String type) throws IllegalArgumentException {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    if (type.equals(TYPE_void)) {
      return preallocatedVoid;
    } else {
      int t = Util.getTypeIndex(type);
      if (t < 0 || t > TYPE_Object_index) {
        throw new IllegalArgumentException("Cannot return type " + type);
      }
      return preallocated[t];
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ReturnInstruction) {
      ReturnInstruction i = (ReturnInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public boolean isFallThrough() {
    return false;
  }

  @Override
  public int hashCode() {
    return opcode + 31111;
  }

  @Override
  public int getPoppedCount() {
    return opcode == OP_return ? 0 : 1;
  }

  public String getType() {
    return opcode == OP_return ? TYPE_void : indexedTypes[opcode - OP_ireturn];
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitReturn(this);
  }

  @Override
  public String toString() {
    return "Return(" + getType() + ")";
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
