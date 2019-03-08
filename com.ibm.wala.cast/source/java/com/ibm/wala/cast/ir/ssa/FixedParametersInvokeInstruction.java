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
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;

/**
 * This abstract instruction extends the abstract invoke with functionality to support invocations
 * with a fixed number of arguments---the only case in some languages and a common case even in
 * scripting languages.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public abstract class FixedParametersInvokeInstruction extends MultiReturnValueInvokeInstruction {

  /**
   * The value numbers of the arguments passed to the call. For non-static methods, params[0] ==
   * this. If params == null, this should be a static method with no parameters.
   */
  private final int[] params;

  public FixedParametersInvokeInstruction(
      int iindex, int results[], int[] params, int exception, CallSiteReference site) {
    super(iindex, results, exception, site);
    this.params = params;
  }

  public FixedParametersInvokeInstruction(
      int iindex, int result, int[] params, int exception, CallSiteReference site) {
    this(iindex, new int[] {result}, params, exception, site);
  }

  /** Constructor InvokeInstruction. This case for void return values */
  public FixedParametersInvokeInstruction(
      int iindex, int[] params, int exception, CallSiteReference site) {
    this(iindex, null, params, exception, site);
  }

  protected abstract SSAInstruction copyInstruction(
      SSAInstructionFactory insts, int result[], int[] params, int exception);

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    int newParams[] = params;

    if (uses != null) {
      int i = 0;

      newParams = new int[params.length];
      for (int j = 0; j < newParams.length; j++) newParams[j] = uses[i++];
    }

    int newLvals[] = null;
    if (getNumberOfReturnValues() > 0) {
      newLvals = results.clone();
    }
    int newExp = exception;

    if (defs != null) {
      int i = 0;
      if (getNumberOfReturnValues() > 0) {
        newLvals[0] = defs[i++];
      }
      newExp = defs[i++];
      for (int j = 1; j < getNumberOfReturnValues(); j++) {
        newLvals[j] = defs[i++];
      }
    }

    return copyInstruction(insts, newLvals, newParams, newExp);
  }

  @Override
  public int getNumberOfPositionalParameters() {
    if (params == null) {
      return 0;
    } else {
      return params.length;
    }
  }

  @Override
  public void visit(IVisitor v) {
    // TODO Auto-generated method stub
    assert false;
  }

  @Override
  public int getNumberOfUses() {
    return getNumberOfPositionalParameters();
  }

  @Override
  public int hashCode() {
    // TODO Auto-generated method stub
    assert false;
    return 0;
  }

  @Override
  public int getUse(int j) {
    if (j < getNumberOfPositionalParameters()) return params[j];
    else {
      return super.getUse(j);
    }
  }
}
