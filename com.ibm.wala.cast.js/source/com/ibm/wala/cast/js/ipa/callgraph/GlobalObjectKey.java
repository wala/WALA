package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

/**
 * Represents the JavaScript global object.  
 * 
 * @see JSSSAPropagationCallGraphBuilder
 */
public class GlobalObjectKey implements InstanceKey {

  private final IClass concreteType;
  
  public GlobalObjectKey(IClass concreteType) {
    this.concreteType = concreteType;
  }
  
  public IClass getConcreteType() {
    return concreteType;
  }

  public String toString() {
    return "JS Global Object";
  }
}
