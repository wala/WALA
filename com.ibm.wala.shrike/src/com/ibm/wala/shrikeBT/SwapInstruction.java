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
 * This instruction represents the swap instruction, which swaps the two values on top of the working stack.
 */
public final class SwapInstruction extends Instruction {
  protected SwapInstruction() {
    super((short) -1);
  }

  private final static SwapInstruction preallocated = new SwapInstruction();

  public static SwapInstruction make() {
    return preallocated;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SwapInstruction) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 84323111;
  }

  @Override
  public int getPoppedCount() {
    return 2;
  }

  @Override
  public String toString() {
    return "Swap()";
  }

  @Override
  public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitSwap(this);
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
