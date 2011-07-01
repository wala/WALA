/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

public class ForInContextSelector implements ContextSelector {
  
  public static class ForInContext implements Context {
    private final InstanceKey obj;
    
    ForInContext(InstanceKey obj) {
      this.obj = obj;
    }
    public ContextItem get(ContextKey name) {
      if (name.equals(ContextKey.PARAMETERS[2])) {
        return new FilteredPointerKey.SingleInstanceFilter(obj);
      } else {
        return null;
      }
    }
    @Override
    public int hashCode() {
      return obj.hashCode();
    }
    @Override
    public boolean equals(Object other) {
      return other != null &&
          getClass().equals(other.getClass()) &&
          obj.equals(((ForInContext)other).obj);
    }     
    @Override
    public String toString() {
      return "for in hack filter for " + obj;
    }
  }
  
  public static final String HACK_METHOD_STR = "_forin_body";
  
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, final InstanceKey[] receiver) {
    if (callee.getDeclaringClass().getName().toString().contains(HACK_METHOD_STR)) {

      return new ForInContext(receiver[2]);
    } else {
      return null;
    }
    /**
     * else if (caller.getContext() instanceof ForInContext) {
      // use one level of call strings within the special method
      return new CallerSiteContext(caller, site);
    } 
     */
  }
  
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (caller.getIR().getCalls(site)[0].getNumberOfUses() > 2) {
      return IntSetUtil.make(new int[]{2});
    } else {
      return EmptyIntSet.instance;
    }
  }

}