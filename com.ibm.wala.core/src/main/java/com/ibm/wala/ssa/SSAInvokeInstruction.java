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

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.TypeReference;

/** */
public abstract class SSAInvokeInstruction extends SSAAbstractInvokeInstruction {

  protected final int result;

  /**
   * The value numbers of the arguments passed to the call. For non-static methods, params[0] ==
   * this. If params == null, this should be a static method with no parameters.
   */
  protected final int[] params;

  protected SSAInvokeInstruction(
      int iindex, int result, int[] params, int exception, CallSiteReference site) {
    super(iindex, exception, site);
    this.result = result;
    this.params = params;
    assertParamsKosher(result, params, site);
  }

  /** Constructor InvokeInstruction. This case for void return values */
  protected SSAInvokeInstruction(int iindex, int[] params, int exception, CallSiteReference site) {
    this(iindex, -1, params, exception, site);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    // result == -1 for void-returning methods, which are the only calls
    // that have a single value def.
    return insts.InvokeInstruction(
        iIndex(),
        defs == null || result == -1 ? result : defs[0],
        uses == null ? params : uses,
        defs == null ? exception : defs[result == -1 ? 0 : 1],
        site,
        null);
  }

  public static void assertParamsKosher(int result, int[] params, CallSiteReference site)
      throws IllegalArgumentException {
    if (site == null) {
      throw new IllegalArgumentException("site cannot be null");
    }
    if (site.getDeclaredTarget().getReturnType().equals(TypeReference.Void)) {
      if (result != -1) {
        assert result == -1 : "bogus call to " + site;
      }
    }

    int nExpected = 0;
    if (!site.isStatic()) {
      nExpected++;
    }

    nExpected += site.getDeclaredTarget().getNumberOfParameters();
    if (nExpected > 0) {
      assert params != null : "null params for " + site;
      if (params.length != nExpected) {
        assert params.length == nExpected
            : "wrong number of params for "
                + site
                + " Expected "
                + nExpected
                + " got "
                + params.length;
      }
    }
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitInvoke(this);
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getNumberOfUses() */
  @Override
  public int getNumberOfUses() {
    if (params == null) {
      assert site.getInvocationCode() == IInvokeInstruction.Dispatch.STATIC
          || site.getInvocationCode() == IInvokeInstruction.Dispatch.SPECIAL;
      assert site.getDeclaredTarget().getNumberOfParameters() == 0;
      return 0;
    } else {
      return params.length;
    }
  }

  @Override
  public int getNumberOfPositionalParameters() {
    return getNumberOfUses();
  }

  @Override
  public int getNumberOfReturnValues() {
    return (result == -1) ? 0 : 1;
  }

  @Override
  public int getReturnValue(int i) {
    assert i == 0 : "i != 0";
    assert (result != -1) : "SSA-Result is -1";
    return result;
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#getUse(int) */
  @Override
  public int getUse(int j) {
    if (params == null) {
      assert false : "Invalid getUse: " + j + " , null params " + this;
    }
    if (j >= params.length) {
      throw new ArrayIndexOutOfBoundsException(
          "Invalid getUse: " + this + ", index " + j + ", params.length " + params.length);
    }
    if (j < 0) {
      throw new ArrayIndexOutOfBoundsException(
          "j may not be negative! In getUse "
              + this
              + ", index "
              + j
              + ", params.length "
              + params.length);
    }
    return params[j];
  }

  @Override
  public int hashCode() {
    return (site.hashCode() * 7529) + (exception * 9823);
  }
}
