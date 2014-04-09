package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Pair;

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

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    return EmptyIterator.instance();
  }
}
