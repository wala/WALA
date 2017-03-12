/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

public abstract class SSAAbstractBinaryInstruction extends SSAInstruction {

  protected final int result;
  protected final int val1;
  protected final int val2;

  public SSAAbstractBinaryInstruction(int iindex, int result, int val1, int val2) {
    super(iindex);
    this.result = result;
    assert val1 != -1;
    this.val1 = val1;
    assert val2 != -1;
    this.val2 = val2;
  }

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

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses()
   */
  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  @Override
  public int getNumberOfUses() {
    return 2;
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getUse(int)
   */
  @Override
  public int getUse(int j) {
    assert j <= 1;
    return (j == 0) ? val1 : val2;
  }

  @Override
  public int hashCode() {
    return 6311 * result ^ 2371 * val1 + val2;
  }

}
