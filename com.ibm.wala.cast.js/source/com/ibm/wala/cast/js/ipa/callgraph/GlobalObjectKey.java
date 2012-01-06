package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

/**
 * represents the JavaScript global object
 */
public class GlobalObjectKey implements InstanceKey {

  private final IClass concreteType;
  
  public GlobalObjectKey(IClass concreteType) {
    this.concreteType = concreteType;
  }
  
  @Override
  public IClass getConcreteType() {
    return concreteType;
  }

  public String toString() {
    return "JS Global Object";
  }
}
