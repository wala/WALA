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
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * A Call instruction.
 *
 * <p>Note that different languages have different notions of what a call is. This is an abstract
 * superclass which encapsulates the common functionality that all languages share, so far.
 */
public abstract class SSAAbstractInvokeInstruction extends SSAInstruction {

  /** The value number which represents the exception object which the call may throw. */
  protected final int exception;

  /** The call site, containing the program counter location and the method being called. */
  protected final CallSiteReference site;

  /**
   * @param exception The value number which represents the exception object which the call may
   *     throw.
   * @param site The call site, containing the program counter location and the method being called.
   */
  protected SSAAbstractInvokeInstruction(int iindex, int exception, CallSiteReference site) {
    super(iindex);
    this.exception = exception;
    this.site = site;
  }

  /** @return The call site, containing the program counter location and the method being called. */
  public CallSiteReference getCallSite() {
    return site;
  }

  /** Is this a 'static' call? (invokestatic in Java) */
  public boolean isStatic() {
    return getCallSite().isStatic();
  }

  /**
   * Might this call dispatch to one of several possible methods? i.e., in Java, is it an
   * invokeinterface or invokevirtual
   */
  public boolean isDispatch() {
    return getCallSite().isDispatch();
  }

  /** Is this a 'special' call? (invokespecial in Java) */
  public boolean isSpecial() {
    return getCallSite().isSpecial();
  }

  /** @return the value number of the receiver of a virtual call */
  public int getReceiver() {
    assert site.getInvocationCode() != IInvokeInstruction.Dispatch.STATIC : toString();
    return getUse(0);
  }

  /** @return the program counter (index into the method's bytecode) for this call site. */
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
      switch (i) {
        case 0:
          return getReturnValue(0);
        case 1:
          return exception;
        default:
          return getReturnValue(i - 1);
      }
    }
  }

  /**
   * Return the value number which is def'fed by this call instruction if the call returns
   * exceptionally.
   */
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

  /** How many parameters does this call specify? */
  public abstract int getNumberOfPositionalParameters();

  /** How many distinct values does this call return? */
  public abstract int getNumberOfReturnValues();

  /** What is the the value number of the ith value returned by this call */
  public abstract int getReturnValue(int i);

  /** What is the declared return type of the called method */
  public TypeReference getDeclaredResultType() {
    return site.getDeclaredTarget().getReturnType();
  }

  /** What method is the declared callee? */
  public MethodReference getDeclaredTarget() {
    return site.getDeclaredTarget();
  }

  /** @see com.ibm.wala.classLoader.CallSiteReference#getInvocationCode() */
  public IInvokeInstruction.IDispatch getInvocationCode() {
    return site.getInvocationCode();
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    String code = site.getInvocationString();
    StringBuilder s = new StringBuilder();
    if (hasDef()) {
      s.append(getValueString(symbolTable, getDef())).append(" = ");
    }
    s.append("invoke").append(code);
    s.append(' ');
    s.append(site.getDeclaredTarget().toString());

    if (getNumberOfPositionalParameters() > 0) {
      s.append(' ').append(getValueString(symbolTable, getUse(0)));
      for (int i = 1; i < getNumberOfPositionalParameters(); i++) {
        s.append(',').append(getValueString(symbolTable, getUse(i)));
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
