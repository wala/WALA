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
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * generates instructions to simulate the semantics of JS constructor invocations
 *
 */
public class JavaScriptConstructTargetSelector implements MethodTargetSelector {
  private final MethodTargetSelector base;

  private final JavaScriptConstructorFunctions constructors;
  
  public JavaScriptConstructTargetSelector(IClassHierarchy cha, MethodTargetSelector base) {
    this.constructors = new JavaScriptConstructorFunctions(cha);
    this.base = base;
  }

  public JavaScriptConstructTargetSelector(JavaScriptConstructorFunctions constructors, MethodTargetSelector base) {
    this.constructors = constructors;
    this.base = base;
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (site.getDeclaredTarget().equals(JavaScriptMethods.ctorReference)) {
      IR callerIR = caller.getIR();
      SSAAbstractInvokeInstruction callStmts[] = callerIR.getCalls(site);
      assert callStmts.length == 1;
      int nargs = callStmts[0].getNumberOfParameters();
      return constructors.findOrCreateConstructorMethod(callerIR, callStmts[0], receiver, nargs - 1);
    } else {
      return base.getCalleeTarget(caller, site, receiver);
    }
  }

  public boolean mightReturnSyntheticMethod() {
    return true;
  }
}
