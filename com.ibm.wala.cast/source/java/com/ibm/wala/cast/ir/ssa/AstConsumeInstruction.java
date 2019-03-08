/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import java.util.Collection;
import java.util.Collections;

public abstract class AstConsumeInstruction extends SSAInstruction {
  protected final int[] rvals;

  public AstConsumeInstruction(int iindex, int[] rvals) {
    super(iindex);
    this.rvals = rvals;
  }

  @Override
  public int getNumberOfDefs() {
    return 0;
  }

  @Override
  public int getDef(int i) {
    Assertions.UNREACHABLE();
    return -1;
  }

  @Override
  public int getNumberOfUses() {
    return rvals.length;
  }

  @Override
  public int getUse(int i) {
    return rvals[i];
  }

  @Override
  public int hashCode() {
    int v = 1;
    for (int rval : rvals) {
      v *= rval;
    }

    return v;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.emptySet();
  }
}
