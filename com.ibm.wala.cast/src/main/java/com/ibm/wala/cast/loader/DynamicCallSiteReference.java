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
package com.ibm.wala.cast.loader;

import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class DynamicCallSiteReference extends CallSiteReference {

  // this must be distinct from java invoke codes.
  // see com.ibm.shrikeBT.BytecodeConstants
  public static enum Dispatch implements IInvokeInstruction.IDispatch {
    JS_CALL;

    @Override
    public boolean hasImplicitThis() {
      return false;
    }
  }

  public DynamicCallSiteReference(MethodReference ref, int pc) {
    super(pc, ref);
  }

  public DynamicCallSiteReference(TypeReference ref, int pc) {
    this(AstMethodReference.fnReference(ref), pc);
  }

  @Override
  public IInvokeInstruction.IDispatch getInvocationCode() {
    return Dispatch.JS_CALL;
  }

  @Override
  protected String getInvocationString(IInvokeInstruction.IDispatch invocationCode) {
    return "Function";
  }

  @Override
  public String toString() {
    return "JSCall@" + getProgramCounter();
  }

  public CallSiteReference cloneReference(int pc) {
    return new DynamicCallSiteReference(getDeclaredTarget(), pc);
  }

  @Override
  public boolean isDispatch() {
    return true;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isFixed() {
    return false;
  }
}
