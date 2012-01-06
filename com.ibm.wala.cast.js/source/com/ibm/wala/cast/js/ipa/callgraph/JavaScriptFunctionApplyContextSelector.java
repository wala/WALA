package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

public class JavaScriptFunctionApplyContextSelector implements ContextSelector {

  private final ContextSelector base;

  private final JSSSAPropagationCallGraphBuilder builder;

  public JavaScriptFunctionApplyContextSelector(ContextSelector base, JSSSAPropagationCallGraphBuilder builder) {
    this.base = base;
    this.builder = builder;
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    // 0 for function (synthetic apply), 1 for this (function being invoked), 2
    // for this arg of function being invoked,
    // 3 for arguments array
    if (caller.getIR().getCalls(site)[0].getNumberOfUses() >= 4) {
      return IntSetUtil.make(new int[] { 3 }).union(base.getRelevantParameters(caller, site));
    } else {
      return base.getRelevantParameters(caller, site);
    }
  }

  public static class ApplyContext implements Context {
    private final Context delegate;

    /**
     * null in the case where no args array is passed
     * 
     * TODO strictly speaking, we don't care about the exact InstanceKey per se; we only care about
     * some of its properties...capture more directly?
     */
    private final InstanceKey argsList;

    ApplyContext(Context delegate, InstanceKey argsList) {
      this.delegate = delegate;
      this.argsList = argsList;
    }

    @Override
    public ContextItem get(ContextKey name) {
      return delegate.get(name);
    }

  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee.toString().equals("<Code body of function Lprologue.js/functionApply>")) {
      System.err.println("CALLEE: " + callee);
      InstanceKey argsList = null;
      if (receiver.length >= 4) {
        argsList = receiver[3];
        // System.err.println(argsList);
        // if
        // (argsList.getConcreteType().equals(caller.getClassHierarchy().lookupClass(JavaScriptTypes.Array)))
        // {
        // System.err.println("it's an array");
        // PointerKey catalog = ((AstPointerKeyFactory)
        // builder.getPointerKeyFactory()).getPointerKeyForObjectCatalog(argsList);
        // System.err.println(catalog);
        // OrdinalSet<InstanceKey> catalogp2set =
        // builder.getPointerAnalysis().getPointsToSet(catalog);
        // System.err.println(catalogp2set);
        // }
      } else {
        System.err.println("no arguments array");
      }
      return new ApplyContext(base.getCalleeTarget(caller, site, callee, receiver), argsList);
    }
    return base.getCalleeTarget(caller, site, callee, receiver);
  }

}
