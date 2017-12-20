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
 * This class represents dup instructions. There are two kinds of dup instructions, dup and dup_x1:
 * 
 * dup: a::rest =&gt; a::a::rest dup_x1: a::b::rest =&gt; a::b::a::rest
 */
public final class DupInstruction extends Instruction {
  final private int size;

  final private byte delta;

  protected DupInstruction(byte size, byte delta) {
    super((short) -1);
    this.size = size;
    this.delta = delta;
  }

  private final static DupInstruction[] preallocated = preallocate();

  private static DupInstruction[] preallocate() {
    DupInstruction[] r = new DupInstruction[9];

    for (int i = 0; i < r.length; i++) {
      r[i] = new DupInstruction((byte) (i / 3), (byte) (i % 3));
    }
    return r;
  }

  /**
   * DupInstructions with size or delta 2 might cause code generation failures when the working stack contains long or double
   * values, when these DupInstructions cannot be easily translated into Java bytecode instructions. For safety, avoid using
   * DupInstructions with size or delta 2.
   * 
   * Don't create these outside the shrikeBT decoder.
   */
  static DupInstruction make(int size, int delta) {
    if (size < 0 || size > 2) {
      throw new IllegalArgumentException("Invalid dup size: " + size);
    }
    if (delta < 0 || delta > 2) {
      throw new IllegalArgumentException("Invalid dup delta: " + delta);
    }
    return preallocated[size * 3 + delta];
  }

  /**
   * @param delta 0 for dup, 1 for dup_x1
   */
  public static DupInstruction make(int delta) {
    if (delta < 0 || delta > 1) {
      throw new IllegalArgumentException("Invalid dup delta: " + delta);
    }
    return make(1, delta);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DupInstruction) {
      DupInstruction i = (DupInstruction) o;
      return i.size == size && i.delta == delta;
    } else {
      return false;
    }
  }

  public int getSize() {
    return size;
  }

  public int getDelta() {
    return delta;
  }

  @Override
  public int hashCode() {
    return size + 8431890 + 10 * delta;
  }

  @Override
  public int getPoppedCount() {
    return size + delta;
  }

  @Override
  public String toString() {
    return "Dup(" + size + "," + delta + ")";
  }

  @Override
  public void visit(IInstruction.Visitor v) {
    if (v == null) {
      throw new IllegalArgumentException("illegal null visitor");
    }
    v.visitDup(this);
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
