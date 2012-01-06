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
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * Special-purpose context selector which adds object sensitivity on uses of <code>Function.prototype.call</code>.
 *  
 * @see <a
 *      href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Call">MDN
 *      Function.prototype.call() docs</a>
 *      
 * @author mschaefer
 */
public class JavaScriptFunctionDotCallContextSelector implements ContextSelector {

  private final ContextSelector base;

  public JavaScriptFunctionDotCallContextSelector(ContextSelector base) {
    this.base = base;
  }

  // parameter 0 is the call function itself, parameter 1 is the function to call, parameter 2 its receiver
  private final static int PARAMETER_INDEX = 2;
  
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (caller.getIR().getCalls(site)[0].getNumberOfUses() > PARAMETER_INDEX) {
      return IntSetUtil.make(new int[] { PARAMETER_INDEX }).union(base.getRelevantParameters(caller, site));
    } else {
      return base.getRelevantParameters(caller, site);
    }
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    String calleeName = callee.getName().toString();
    Context baseContext = base.getCalleeTarget(caller, site, callee, receiver);
    // TODO: use a better test here
    if (calleeName.contains(JavaScriptFunctionDotCallTargetSelector.SYNTHETIC_CALL_METHOD_PREFIX)) {
      if (receiver.length > PARAMETER_INDEX && receiver[PARAMETER_INDEX] != null) {
        return new ParameterInstanceContext(baseContext, PARAMETER_INDEX, receiver[PARAMETER_INDEX]);
      }
    }
    return baseContext;
  }
  
  public class ParameterInstanceContext implements Context {
    private final Context baseContext;
    private final int index;
    private final InstanceKey key;

    public ParameterInstanceContext(Context baseContext, int index, InstanceKey key) {
      if(key == null)
        throw new IllegalArgumentException("Null key provided for parameter.");
      if(index <= 0 || index > ContextKey.PARAMETERS.length)
        throw new IllegalArgumentException("Parameter index out of range.");
      this.baseContext = baseContext;
      this.index = index;
      this.key = key;
    }

    public ContextItem get(ContextKey name) {
      if(name == ContextKey.PARAMETERS[index])
        return new FilteredPointerKey.SingleInstanceFilter(key);
      else
        return baseContext.get(name);
    }
  }
}
