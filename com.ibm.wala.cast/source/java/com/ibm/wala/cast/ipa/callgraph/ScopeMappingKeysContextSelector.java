package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

public class ScopeMappingKeysContextSelector implements ContextSelector {

  public static final ContextKey scopeKey = new ContextKey() {
    public String toString() {
      return "SCOPE KEY";
    }
  };

  public static class ScopeMappingContext implements Context {
    private final ScopeMappingInstanceKey key;

    private ScopeMappingContext(ScopeMappingInstanceKey key) {
      this.key = key;
    }

    public ContextItem get(ContextKey name) {
      if (scopeKey.equals(name)) {
        return key;
      } else {
        return null;
      }
    }

    private int hashcode = -1;

    public int hashCode() {
      if (hashcode == -1) {
        hashcode = key.hashCode();
      }
      return hashcode;
    }

    public String toString() {
      return "context for " + key;
    }

    public boolean equals(Object o) {
      return (o instanceof ScopeMappingContext) && key.equals(((ScopeMappingContext) o).key);
    }
  }


  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee instanceof SummarizedMethod) {
      final String calleeName = callee.getReference().toString();
      if (calleeName.equals("< JavaScriptLoader, LArray, ctor()LRoot; >")
          || calleeName.equals("< JavaScriptLoader, LObject, ctor()LRoot; >")) {
        return null;
      }
    }
    if (receiver[0] instanceof ScopeMappingInstanceKey) {
      final ScopeMappingInstanceKey smik = (ScopeMappingInstanceKey) receiver[0];
      if (AstTranslator.NEW_LEXICAL) {
        if (detectRecursion(smik.getCreator().getContext(), callee)) {
          return null;
        }
      }
      final ScopeMappingContext scopeMappingContext = new ScopeMappingContext(smik);
      return scopeMappingContext;
    } else {
      return null;
    }
  }

  /**
   * we need this check due a potentially nasty interaction between
   * ScopeMappingContexts and CGNode-based contexts like
   * {@link CallerSiteContext}s. It's unclear where exactly this check should
   * go; since ScopeMappingContexts are less common, putting it here for now.
   */
  private boolean detectRecursion(Context context, IMethod callee) {
    CGNode caller = (CGNode) context.get(ContextKey.CALLER);
    if (caller != null) {
      if (caller.getMethod().equals(callee)) {
        return true;
      } else {
        return detectRecursion(caller.getContext(), callee);
      }
    }
    return false;
  }

  private static final IntSet thisParameter = IntSetUtil.make(new int[] { 0 });

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return thisParameter;
  }

}
