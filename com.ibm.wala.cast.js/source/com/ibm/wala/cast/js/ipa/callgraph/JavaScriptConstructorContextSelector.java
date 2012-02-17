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
import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneLevelSiteContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

public class JavaScriptConstructorContextSelector implements ContextSelector {
  private nCFAContextSelector oneLevelCallStrings;
  private OneLevelSiteContextSelector oneLevelCallerSite;

  public JavaScriptConstructorContextSelector() {
    final ContextInsensitiveSelector dummyBase = new ContextInsensitiveSelector();
    this.oneLevelCallStrings = new nCFAContextSelector(1, dummyBase);
    this.oneLevelCallerSite = new OneLevelSiteContextSelector(dummyBase);
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return EmptyIntSet.instance;
  }

  public Context getCalleeTarget(final CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee instanceof JavaScriptConstructor) {
      final Context oneLevelCallStringContext = oneLevelCallStrings.getCalleeTarget(caller, site, callee, receiver);
      final Context callerContext = caller.getContext();
      if (!AstTranslator.NEW_LEXICAL && callerContext instanceof ScopeMappingContext) {
        return new DelegatingContext(callerContext, oneLevelCallStringContext);
      } else if (AstTranslator.NEW_LEXICAL && LexicalScopingResolverContexts.hasExposedUses(caller, site)) {
        // use a caller-site context, to enable lexical scoping lookups (via
        // caller CGNode)
        return oneLevelCallerSite.getCalleeTarget(caller, site, callee, receiver);
      } else {
        // use at least one-level of call-string sensitivity for constructors
        // always
        return oneLevelCallStringContext;
      }
    } else {
      return null;
    }
  }

}
