package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Pair;
import java.util.Iterator;

public class GlobalVertex extends Vertex implements ObjectVertex {

  private GlobalVertex() {}

  public static final GlobalVertex global = new GlobalVertex();

  public static GlobalVertex instance() {
    return global;
  }

  @Override
  public IClass getConcreteType() {
    return null;
  }

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    return EmptyIterator.instance();
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitGlobalVertex(this);
  }
}
