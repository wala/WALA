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

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

public abstract class MultiReturnValueInvokeInstruction extends SSAAbstractInvokeInstruction {
  protected final int results[];

  protected MultiReturnValueInvokeInstruction(
      int iindex, int results[], int exception, CallSiteReference site) {
    super(iindex, exception, site);
    this.results = results;
  }

  @Override
  public int getNumberOfReturnValues() {
    return results == null ? 0 : results.length;
  }

  @Override
  public int getReturnValue(int i) {
    return results == null ? -1 : results[i];
  }
}
