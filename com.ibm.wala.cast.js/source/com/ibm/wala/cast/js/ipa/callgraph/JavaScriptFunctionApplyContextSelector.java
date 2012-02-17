package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.DelegatingContext;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * 
 * @see <a
 *      href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Apply">MDN
 *      Function.prototype.apply() docs</a>
 */
public class JavaScriptFunctionApplyContextSelector implements ContextSelector {
  /* whether to use a one-level callstring context in addition to the apply context */
  private static final boolean USE_ONE_LEVEL_CALLSTRING = true; 

  public static final ContextKey APPLY_NON_NULL_ARGS = new ContextKey() {
  };

  public static class BooleanContextItem implements ContextItem {
    final boolean val;

    BooleanContextItem(boolean val) {
      this.val = val;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (val ? 1231 : 1237);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      BooleanContextItem other = (BooleanContextItem) obj;
      if (val != other.val)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "BooleanContextItem [val=" + val + "]";
    }

  }

  private final ContextSelector base;
  private nCFAContextSelector oneLevel;

  public JavaScriptFunctionApplyContextSelector(ContextSelector base) {
    this.base = base;
    this.oneLevel = new nCFAContextSelector(1, base);
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
     * was the argsList argument a non-null Array?
     */
    private final BooleanContextItem isNonNullArray;

    ApplyContext(Context delegate, boolean isNonNullArray) {
      this.delegate = delegate;
      this.isNonNullArray = new BooleanContextItem(isNonNullArray);
    }

    public ContextItem get(ContextKey name) {
      if (APPLY_NON_NULL_ARGS.equals(name)) {
        return isNonNullArray;
      } else {
        return delegate.get(name);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + delegate.hashCode();
      result = prime * result + isNonNullArray.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ApplyContext other = (ApplyContext) obj;
      if (!delegate.equals(other.delegate))
        return false;
      if (!isNonNullArray.equals(other.isNonNullArray))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "ApplyContext [delegate=" + delegate + ", isNonNullArray=" + isNonNullArray + "]";
    }
    
    

  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    IClass declaringClass = callee.getDeclaringClass();
    IMethod method = declaringClass.getMethod(AstMethodReference.fnSelector);
    Context baseCtxt = base.getCalleeTarget(caller, site, callee, receiver);
    if(USE_ONE_LEVEL_CALLSTRING)
      baseCtxt = new DelegatingContext(oneLevel.getCalleeTarget(caller, site, callee, receiver), baseCtxt);
    if (method != null) {
      String s = method.getReference().getDeclaringClass().getName().toString();
      if (s.equals("Lprologue.js/functionApply")) {
        boolean isNonNullArray = false;
        if (receiver.length >= 4) {
          InstanceKey argsList = receiver[3];
          if (argsList != null && argsList.getConcreteType().equals(caller.getClassHierarchy().lookupClass(JavaScriptTypes.Array))) {
            isNonNullArray = true;
          }
        }
        return new ApplyContext(baseCtxt, isNonNullArray);
      }
    }
    return baseCtxt;
  }

}
