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
 * This instruction represents the swap instruction, which swaps the two values
 * on top of the working stack.
 */
public final class SwapInstruction extends Instruction {
  protected SwapInstruction() {
  }

  private final static SwapInstruction preallocated = new SwapInstruction();

  public static SwapInstruction make() {
    return preallocated;
  }

  public boolean equals(Object o) {
    if (o instanceof SwapInstruction) {
      return true;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return 84323111;
  }

  public int getPoppedCount() {
    return 2;
  }

  public String toString() {
    return "Swap()";
  }

  public void visit(Visitor v) throws NullPointerException {
    v.visitSwap(this);
  }
    /* (non-Javadoc)
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
    return false;
  }
}