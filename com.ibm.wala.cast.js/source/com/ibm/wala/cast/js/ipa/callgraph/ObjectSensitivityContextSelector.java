/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.HashMap;

import com.ibm.wala.cast.ipa.callgraph.ArgumentInstanceContext;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

public class ObjectSensitivityContextSelector implements ContextSelector {
  private final ContextSelector base;
  
  public ObjectSensitivityContextSelector(ContextSelector base) {
    this.base = base;
  }
  
  private final HashMap<MethodReference, Boolean> returnsThis_cache = HashMapFactory.make();
  
  private final IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();

  // determine whether the method returns "this"
  private boolean returnsThis(IMethod method) {
    MethodReference mref = method.getReference();
    if(method.getNumberOfParameters() < 1)
      return false;
    Boolean b = returnsThis_cache.get(mref);
    if(b != null)
      return b;
    for(SSAInstruction inst : factory.makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions()).getInstructions()) {
      if(inst instanceof SSAReturnInstruction) {
        SSAReturnInstruction ret = (SSAReturnInstruction)inst;
        if(ret.getResult() == 2) {
          returnsThis_cache.put(mref, true);
          return true;
        }
      }
    }
    returnsThis_cache.put(mref, false);
    return false;
  }

  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] arguments) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, arguments);
    if(returnsThis(callee)) {
      if(arguments.length > 1 && arguments[1] != null) {
        return new ArgumentInstanceContext(baseContext, 1, arguments[1]);
      }
    }
    return baseContext;
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (caller.getIR().getCalls(site)[0].getNumberOfUses() > 1) {
      return IntSetUtil.make(new int[]{1}).union(base.getRelevantParameters(caller, site));
    } else {
      return base.getRelevantParameters(caller, site);
    }
  }

}
