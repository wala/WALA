package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.HashMap;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

public class ObjectSensitivityContextSelector implements ContextSelector {

  private final HashMap<MethodReference, Boolean> returnsThis_cache = HashMapFactory.make();

  // determine whether the method returns "this"
  private boolean returnsThis(IMethod method) {
    MethodReference mref = method.getReference();
    if (method.getNumberOfParameters() < 1)
      return false;
    Boolean b = returnsThis_cache.get(mref);
    if (b != null)
      return b;
    for (SSAInstruction inst : ForInContextSelector.factory.makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions())
        .getInstructions()) {
      if (inst instanceof SSAReturnInstruction) {
        SSAReturnInstruction ret = (SSAReturnInstruction) inst;
        if (ret.getResult() == 2) {
          returnsThis_cache.put(mref, true);
          return true;
        }
      }
    }
    returnsThis_cache.put(mref, false);
    return false;
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] arguments) {
    if (returnsThis(callee)) {
      if (arguments.length > 1 && arguments[1] != null) {
        return new ArgumentInstanceContext(1, arguments[1]);
      }
    }
    return null;
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (caller.getIR().getCalls(site)[0].getNumberOfUses() > 1) {
      return IntSetUtil.make(new int[] { 1 });
    } else {
      return EmptyIntSet.instance;
    }
  }

}

class ArgumentInstanceContext implements Context {
  private final int index;
  private final InstanceKey instanceKey;

  public ArgumentInstanceContext(int index, InstanceKey instanceKey) {
    this.index = index;
    this.instanceKey = instanceKey;
  }

  public ContextItem get(ContextKey name) {
    /*
     * if(name == ContextKey.RECEIVER && index == 1) return instanceKey;
     */
    if (name == ContextKey.PARAMETERS[index])
      return new FilteredPointerKey.SingleInstanceFilter(instanceKey);
    return null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + index;
    result = prime * result + ((instanceKey == null) ? 0 : instanceKey.hashCode());
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
    ArgumentInstanceContext other = (ArgumentInstanceContext) obj;
    if (index != other.index)
      return false;
    if (instanceKey == null) {
      if (other.instanceKey != null)
        return false;
    } else if (!instanceKey.equals(other.instanceKey))
      return false;
    return true;
  }

}