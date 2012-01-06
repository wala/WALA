package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
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

/**
 * 
 * @see <a
 *      href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Apply">MDN
 *      Function.apply() docs</a>
 */
public class JavaScriptFunctionApplyContextSelector implements ContextSelector {

  private final ContextSelector base;

  public JavaScriptFunctionApplyContextSelector(ContextSelector base) {
    this.base = base;
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

    private final CallSiteReference site;
    
    /**
     * was the argsList argument a non-null Array?
     */
    private final boolean isNonNullArray;

    ApplyContext(Context delegate, CallSiteReference site, boolean isNonNullArray) {
      this.delegate = delegate;
      this.site = site;
      this.isNonNullArray = isNonNullArray;
    }

    @Override
    public ContextItem get(ContextKey name) {
      return delegate.get(name);
    }

    public boolean isNonNullArray() {
      return isNonNullArray;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
      result = prime * result + (isNonNullArray ? 1231 : 1237);
      result = prime * result + ((site == null) ? 0 : site.hashCode());
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
      if (delegate == null) {
        if (other.delegate != null)
          return false;
      } else if (!delegate.equals(other.delegate))
        return false;
      if (isNonNullArray != other.isNonNullArray)
        return false;
      if (site == null) {
        if (other.site != null)
          return false;
      } else if (!site.equals(other.site))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "ApplyContext [delegate=" + delegate + ", site=" + site + ", isNonNullArray=" + isNonNullArray + "]";
    }



  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee.toString().equals("<Code body of function Lprologue.js/functionApply>")) {
      boolean isNonNullArray = false;
      if (receiver.length >= 4) {
        InstanceKey argsList = receiver[3];
        if (argsList != null && argsList.getConcreteType().equals(caller.getClassHierarchy().lookupClass(JavaScriptTypes.Array))) {
          isNonNullArray = true;
        }
      }
      return new ApplyContext(base.getCalleeTarget(caller, site, callee, receiver), site, isNonNullArray);
    }
    return base.getCalleeTarget(caller, site, callee, receiver);
  }

}
