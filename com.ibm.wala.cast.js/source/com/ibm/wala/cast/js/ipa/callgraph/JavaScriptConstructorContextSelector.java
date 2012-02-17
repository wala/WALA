package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.LexicalScopingResolverContexts;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingKeysContextSelector.ScopeMappingContext;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector.JavaScriptConstructor;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.DelegatingContext;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneLevelSiteContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.util.intset.IntSet;

public class JavaScriptConstructorContextSelector implements ContextSelector {
  private final ContextSelector base;

  /**
   * for generating contexts with one-level of call strings, to match standard
   * Andersen's heap abstraction
   */
  private final nCFAContextSelector oneLevelCallStrings;

  private final OneLevelSiteContextSelector oneLevelCallerSite;
  
  private final boolean usePreciseLexical;
  
  public JavaScriptConstructorContextSelector(ContextSelector base, boolean usePreciseLexical) {
    this.base = base;
    this.oneLevelCallStrings = new nCFAContextSelector(1, base);
    this.oneLevelCallerSite = new OneLevelSiteContextSelector(base);
    this.usePreciseLexical = usePreciseLexical;
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }

  public Context getCalleeTarget(final CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee instanceof JavaScriptConstructor) {
      final Context oneLevelCallStringContext = oneLevelCallStrings.getCalleeTarget(caller, site, callee, receiver);
      final Context callerContext = caller.getContext();
      if (!AstTranslator.NEW_LEXICAL && callerContext instanceof ScopeMappingContext) {
        return new DelegatingContext(callerContext, oneLevelCallStringContext);
      } else if (AstTranslator.NEW_LEXICAL && usePreciseLexical && LexicalScopingResolverContexts.hasExposedUses(caller, site)) {
        // use a caller-site context, to enable lexical scoping lookups (via caller CGNode)
        return oneLevelCallerSite.getCalleeTarget(caller, site, callee, receiver);
      } else {
        // use at least one-level of call-string sensitivity for constructors
        // always
        return oneLevelCallStringContext;
      }
    } else {
      return base.getCalleeTarget(caller, site, callee, receiver);
    }
  }

}
