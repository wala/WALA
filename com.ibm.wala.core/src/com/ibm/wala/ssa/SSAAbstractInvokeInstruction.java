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
import com.ibm.wala.util.debug.Assertions;

/**
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 */
public abstract class SSAAbstractInvokeInstruction extends SSAInstruction implements IInvokeInstruction {

  /**
   * The value number of the return value of this call, or -1 if the method
   * returns void
   */
  protected final int result;

  /**
   * The value number which represents the exception object which the call may
   * throw.
   */
  protected final int exception;

  /**
   * The call site, containing the program counter location and the method being
   * called.
   */
  protected final CallSiteReference site;

  protected SSAAbstractInvokeInstruction(int result, int exception, CallSiteReference site) {
    this.result = result;
    this.exception = exception;
    this.site = site;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public boolean hasDef() {
    return result != -1;
  }

  public CallSiteReference getCallSite() {
    return site;
  }

  public CallSiteReference getSite() {
    return site;
  }

  public boolean isStatic() {
    return getSite().isStatic();
  }

  public boolean isDispatch() {
    return getSite().isDispatch();
  }
  
  public boolean isSpecial() {
	return getSite().isSpecial();
  }

  /**
   * @return the value number of the receiver of a virtual call
   */
  public int getReceiver() {
    if (Assertions.verifyAssertions) {
      IInvokeInstruction.IDispatch code = site.getInvocationCode();
      Assertions._assert(code!=IInvokeInstruction.Dispatch.STATIC, toString());
    }
    return getUse(0);
  }

  public int getProgramCounter() {
    return site.getProgramCounter();
  }

  @Override
  public int getDef(int i) {
    Assertions._assert(i < 2);
    return (i == 0 && result != -1) ? result : exception;
  }

  public int getException() {
    return exception;
  }

  @Override
  public int getNumberOfDefs() {
    return (result == -1) ? 1 : 2;
  }

  public abstract int getNumberOfParameters();

  /**
   * Method getDeclaredResultType. TODO: push this logic into shrike.
   * 
   * @return TypeReference
   */
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
  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    String code = site.getInvocationString();
    StringBuffer s = new StringBuffer();
    if (result != -1) {
      s.append(getValueString(symbolTable, d, result)).append(" = ");
    }
    s.append("invoke").append(code);
    s.append(" ");
    s.append(site.getDeclaredTarget().toString());

    if (getNumberOfParameters() > 0) {
      s.append(" ").append(getValueString(symbolTable, d, getUse(0)));
      for (int i = 1; i < getNumberOfParameters(); i++) {
        s.append(",").append(getValueString(symbolTable, d, getUse(i)));
      }
    }

    s.append(" @");
    s.append(site.getProgramCounter());

    if (exception == -1) {
      s.append(" exception: NOT MODELED");
    } else {
      s.append(" exception:").append(getValueString(symbolTable, d, exception));
    }

    return s.toString();
  }

}
