package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

/**
 * A selective Cartesian product context that enforces object sensitivity on some set
 * of parameter positions.
 */
public class SelectiveCPAContext implements Context {
  // base context
  protected final Context base;
  
  // maps parameters to their abstract objects
  private final Map<ContextKey, InstanceKey> parameterObjs;
  
  // cached hash code
  private final int hashCode;

  // helper method for constructing the parameterObjs map
  private static Map<ContextKey, InstanceKey> makeMap(InstanceKey[] x) {
    Map<ContextKey, InstanceKey> result = new HashMap<ContextKey, InstanceKey>();
    for(int i = 0; i < x.length; i++) {
      if (x[i] != null) {
        result.put(ContextKey.PARAMETERS[i], x[i]);
      }
    }
    return result;
  }

  public SelectiveCPAContext(Context base, InstanceKey[] x) {
    this(base, makeMap(x));
  }

  public SelectiveCPAContext(Context base, Map<ContextKey, InstanceKey> parameterObjs) {
    this.base = base;
    this.parameterObjs = parameterObjs;
    hashCode = base.hashCode() ^ parameterObjs.hashCode();
  }

  public ContextItem get(ContextKey name) {
    if (parameterObjs.containsKey(name)) {
      return new FilteredPointerKey.SingleInstanceFilter(parameterObjs.get(name));
    } else {
      return base.get(name);
    }
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other != null &&
        getClass().equals(other.getClass()) &&
        base.equals(((SelectiveCPAContext)other).base) &&
        parameterObjs.equals(((SelectiveCPAContext)other).parameterObjs);
  }     


}