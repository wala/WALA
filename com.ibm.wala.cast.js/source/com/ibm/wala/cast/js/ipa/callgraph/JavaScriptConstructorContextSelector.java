package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingKeysContextSelector.ScopeMappingContext;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector.JavaScriptConstructor;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.DelegatingContext;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.util.intset.IntSet;

public class JavaScriptConstructorContextSelector implements ContextSelector {
  private final ContextSelector base;
  
  private final ContextSelector oneLevel;
  
  public JavaScriptConstructorContextSelector(ContextSelector base) {
    this.base = base;
    this.oneLevel = new nCFAContextSelector(1, base);
  }
  
  
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }


  public Context getCalleeTarget(final CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {   
    final Context baseCtxt = base.getCalleeTarget(caller, site, callee, receiver);
    if (callee instanceof JavaScriptConstructor) {
      final Context oneLevelContext = oneLevel.getCalleeTarget(caller, site, callee, receiver);
      final Context callerContext = caller.getContext();
      if (callerContext instanceof ScopeMappingContext) {
        return new DelegatingContext(callerContext, new DelegatingContext(oneLevelContext, baseCtxt));
      } else {
        // use at least one-level of call-string sensitivity for constructors always
        return oneLevelContext;
      }
    } else {
      return baseCtxt;
    }
  }

}
