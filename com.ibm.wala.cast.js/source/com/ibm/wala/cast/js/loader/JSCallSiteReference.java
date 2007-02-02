/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.loader;

import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.cast.types.*;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.shrikeBT.IInvokeInstruction;

public class JSCallSiteReference extends CallSiteReference {

  // this must be distinct from java invoke codes.
  // see com.ibm.shrikeBT.BytecodeConstants
  public static enum Dispatch implements IInvokeInstruction.IDispatch {
    JS_CALL;
  }

  public JSCallSiteReference(MethodReference ref, int pc) {
    super(pc, ref);
  }

  public JSCallSiteReference(int pc) {
    this(AstMethodReference.fnReference(JavaScriptTypes.CodeBody), pc);
  }

  public IInvokeInstruction.IDispatch getInvocationCode() {
    return Dispatch.JS_CALL;
  }

  public String toString() {
    return "JSCall@" + getProgramCounter();
  }

}
