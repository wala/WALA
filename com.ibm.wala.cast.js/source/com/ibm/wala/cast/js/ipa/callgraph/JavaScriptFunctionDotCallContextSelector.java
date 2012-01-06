package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
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

  // parameter 0 is the function being invoked, parameter 1 its receiver
  private final static int PARAMETER_INDEX = 1;
  
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
      if (receiver.length > PARAMETER_INDEX && receiver[PARAMETER_INDEX] != null)
        // TODO: don't use ForInContext here, introduce new kind of context for specialising on parameter position
        return new ForInContextSelector.ForInContext(base.getCalleeTarget(caller, site, callee, receiver), receiver[PARAMETER_INDEX]);
    }
    return baseContext;
  }
}
