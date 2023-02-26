/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.ssa;

/** */
public abstract class SSAAbstractUnaryInstruction extends SSAInstruction {

  protected final int result;

  protected final int val;

  protected SSAAbstractUnaryInstruction(int iindex, int result, int val) {
    super(iindex);
    this.result = result;
    this.val = val;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getDef() */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getDef(int i) {
    assert i == 0;
    return result;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses() */
  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int j) {
    assert j == 0;
    return val;
  }

  @Override
  public int hashCode() {
    return val * 1663 ^ result * 4027;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
