package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingKeysContextSelector.ScopeMappingContext;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector.JavaScriptConstructor;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneLevelSiteContextSelector;
import com.ibm.wala.util.intset.IntSet;

public class JavaScriptConstructorContextSelector implements ContextSelector {
  private final ContextSelector base;
  
  private final ContextSelector oneLevel;
  
  public JavaScriptConstructorContextSelector(ContextSelector base) {
    this.base = base;
    this.oneLevel = new OneLevelSiteContextSelector(base);
  }
  
  
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }


  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {   
    if (callee instanceof JavaScriptConstructor) {
      if (caller.getContext() instanceof ScopeMappingContext) {
        return caller.getContext();        
      } else {
        // use one-level of call-site sensitivity for constructors always
        return oneLevel.getCalleeTarget(caller, site, callee, receiver);
      }
    } else {
      return base.getCalleeTarget(caller, site, callee, receiver);
    }
  }

}
