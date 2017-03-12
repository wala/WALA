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
 * PopInstructions pop one or two elements off the working stack.
 */
public final class PopInstruction extends Instruction {
  final private byte size;

  protected PopInstruction(byte size) {
    super((short) -1);
    this.size = size;
  }

  /**
   * @param size 1 or 2, the number of elements to pop
   */
  public static PopInstruction make(int size) {
    if (size < 0 || size > 2) {
      throw new IllegalArgumentException("Invalid pop size: " + size);
    } else {
      return new PopInstruction((byte) size);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PopInstruction) {
      PopInstruction i = (PopInstruction) o;
      return i.size == size;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return size + 8431890;
  }

  @Override
  public int getPoppedCount() {
    return size;
  }

  @Override
  public void visit(IInstruction.Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitPop(this);
  }

  @Override
  public String toString() {
    return "Pop(" + size + ")";
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
