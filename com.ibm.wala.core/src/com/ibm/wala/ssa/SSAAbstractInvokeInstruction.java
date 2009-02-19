/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public abstract class SSAAbstractInvokeInstruction extends SSAInstruction {

  /**
   * The value number which represents the exception object which the call may throw.
   */
  protected final int exception;

  /**
   * The call site, containing the program counter location and the method being called.
   */
  protected final CallSiteReference site;

  protected SSAAbstractInvokeInstruction(int exception, CallSiteReference site) {
    this.exception = exception;
    this.site = site;
  }

  public CallSiteReference getCallSite() {
    return site;
  }

  public boolean isStatic() {
    return getCallSite().isStatic();
  }

  public boolean isDispatch() {
    return getCallSite().isDispatch();
  }

  public boolean isSpecial() {
    return getCallSite().isSpecial();
  }

  /**
   * @return the value number of the receiver of a virtual call
   */
  public int getReceiver() {
    assert site.getInvocationCode() != IInvokeInstruction.Dispatch.STATIC : toString();
    return getUse(0);
  }

  public int getProgramCounter() {
    return site.getProgramCounter();
  }

  @Override
  public int getNumberOfDefs() {
    return getNumberOfReturnValues() + 1;
  }

  @Override
  public int getDef(int i) {
    if (getNumberOfReturnValues() == 0) {
      assert i == 0;
      return exception;
    } else {
      if (i == 0) {
        return getReturnValue(0);
      } else if (i == 1) {
        return exception;
      } else {
        return getReturnValue(i - 1);
      }
    }
  }

  public int getException() {
    return exception;
  }

  @Override
  public boolean hasDef() {
    return getNumberOfReturnValues() > 0;
  }

  @Override
  public int getDef() {
    return getReturnValue(0);
  }

  public abstract int getNumberOfParameters();

  public abstract int getNumberOfReturnValues();

  public abstract int getReturnValue(int i);

  public TypeReference getDeclaredResultType() {
    return site.getDeclaredTarget().getReturnType();
  }

  /**
   * @see com.ibm.wala.classLoader.CallSiteReference#getDeclaredTarget()
   */
  public MethodReference getDeclaredTarget() {
    return site.getDeclaredTarget();
  }

  /**
   * @see com.ibm.wala.classLoader.CallSiteReference#getInvocationCode()
   */
  public IInvokeInstruction.IDispatch getInvocationCode() {
    return site.getInvocationCode();
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return true;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    String code = site.getInvocationString();
    StringBuffer s = new StringBuffer();
    if (hasDef()) {
      s.append(getValueString(symbolTable, getDef())).append(" = ");
    }
    s.append("invoke").append(code);
    s.append(" ");
    s.append(site.getDeclaredTarget().toString());

    if (getNumberOfParameters() > 0) {
      s.append(" ").append(getValueString(symbolTable, getUse(0)));
      for (int i = 1; i < getNumberOfParameters(); i++) {
        s.append(",").append(getValueString(symbolTable, getUse(i)));
      }
    }

    s.append(" @");
    s.append(site.getProgramCounter());

    if (exception == -1) {
      s.append(" exception: NOT MODELED");
    } else {
      s.append(" exception:").append(getValueString(symbolTable, exception));
    }

    return s.toString();
  }

}
