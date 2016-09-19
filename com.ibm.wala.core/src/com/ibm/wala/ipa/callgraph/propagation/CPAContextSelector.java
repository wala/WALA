/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

public class CPAContextSelector implements ContextSelector {

  private final ContextSelector base;
  
  public CPAContextSelector(ContextSelector base) {
    this.base = base;
  }

  public static class CPAContext extends SelectiveCPAContext {

    public CPAContext(Context base, InstanceKey[] x) {
      super(base, x);
    }
    
  }
  
  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
     Context target = base.getCalleeTarget(caller, site, callee, actualParameters);
     if (actualParameters != null && actualParameters.length > 0) {
    return new CPAContext(target, actualParameters);
     } else {
       return target;
     }
  }

  private static boolean dispatchIndex(CallSiteReference ref, int i) {
    if (ref.isStatic()) {
      return ! ref.getDeclaredTarget().getParameterType(i).isPrimitiveType();
    } else {
      return i==0 || ! ref.getDeclaredTarget().getParameterType(i-1).isPrimitiveType();
    }
  }
  
  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    MutableIntSet s = IntSetUtil.make();
    for(int i = 0; i < caller.getIR().getCalls(site)[0].getNumberOfUses(); i++) {
      if (!caller.getMethod().getDeclaringClass().getClassLoader().getLanguage().methodsHaveDeclaredParameterTypes() || dispatchIndex(site, i)) {
        s.add(i);
      }
    }
    return s;
  }

}
