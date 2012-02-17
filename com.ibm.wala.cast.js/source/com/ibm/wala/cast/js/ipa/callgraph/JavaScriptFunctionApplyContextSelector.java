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
import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * 
 * @see <a
 *      href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Apply">MDN
 *      Function.prototype.apply() docs</a>
 */
public class JavaScriptFunctionApplyContextSelector implements ContextSelector {
  /*
   * whether to use a one-level callstring context in addition to the apply
   * context
   */
  private static final boolean USE_ONE_LEVEL = true;

  private static final TypeName APPLY_TYPE_NAME = TypeName.findOrCreate("Lprologue.js/functionApply");

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

  private ContextSelector oneLevel;

  public JavaScriptFunctionApplyContextSelector() {
    this.oneLevel = new nCFAContextSelector(1, new ContextInsensitiveSelector());
    // this.oneLevel = new OneLevelSiteContextSelector(base);
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    // 0 for function (synthetic apply), 1 for this (function being invoked), 2
    // for this arg of function being invoked,
    // 3 for arguments array
    if (caller.getIR().getCalls(site)[0].getNumberOfUses() >= 4) {
      return IntSetUtil.make(new int[] { 3 });
    } else {
      return EmptyIntSet.instance;
    }
  }

  public static class ApplyContext implements Context {
    /**
     * was the argsList argument a non-null Array?
     */
    private final BooleanContextItem isNonNullArray;

    ApplyContext(boolean isNonNullArray) {
      this.isNonNullArray = new BooleanContextItem(isNonNullArray);
    }

    public ContextItem get(ContextKey name) {
      if (APPLY_NON_NULL_ARGS.equals(name)) {
        return isNonNullArray;
      } else {
        return null;
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((isNonNullArray == null) ? 0 : isNonNullArray.hashCode());
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
      if (isNonNullArray == null) {
        if (other.isNonNullArray != null)
          return false;
      } else if (!isNonNullArray.equals(other.isNonNullArray))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "ApplyContext [isNonNullArray=" + isNonNullArray + "]";
    }

  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    IClass declaringClass = callee.getDeclaringClass();
    IMethod method = declaringClass.getMethod(AstMethodReference.fnSelector);
    if (method != null) {
      TypeName tn = method.getReference().getDeclaringClass().getName();
      if (tn.equals(APPLY_TYPE_NAME)) {
        boolean isNonNullArray = false;
        if (receiver.length >= 4) {
          InstanceKey argsList = receiver[3];
          if (argsList != null && argsList.getConcreteType().equals(caller.getClassHierarchy().lookupClass(JavaScriptTypes.Array))) {
            isNonNullArray = true;
          }
        }
        Context result = new ApplyContext(isNonNullArray);
        if (USE_ONE_LEVEL)
          result = new DelegatingContext(result, oneLevel.getCalleeTarget(caller, site, callee, receiver));
        return result;
      }
    }
    return null;
  }

}
