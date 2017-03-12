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
 * This class represents arraylength instructions.
 */
final public class ArrayLengthInstruction extends Instruction {
  protected ArrayLengthInstruction() {
    super(OP_arraylength);
  }

  private static final ArrayLengthInstruction preallocated = new ArrayLengthInstruction();

  public static ArrayLengthInstruction make() {
    return preallocated;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ArrayLengthInstruction;
  }

  @Override
  public int hashCode() {
    return 3180901;
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  @Override
  public String getPushedType(String[] types) {
    return Constants.TYPE_int;
  }

  @Override
  public byte getPushedWordSize() {
    return 1;
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitArrayLength(this);
  }

  @Override
  public String toString() {
    return "ArrayLength()";
  }

  @Override
  public boolean isPEI() {
    return true;
  }
}
