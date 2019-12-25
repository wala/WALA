package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;
import java.util.Iterator;

public class CreationSiteVertex extends Vertex implements ObjectVertex {
  private final IMethod node;
  private final int instructionIndex;
  private final TypeReference createdType;

  public CreationSiteVertex(IMethod node, int instructionIndex, TypeReference createdType) {
    super();
    this.node = node;
    this.instructionIndex = instructionIndex;
    this.createdType = createdType;
  }

  @Override
  public IClass getConcreteType() {
    return node.getClassHierarchy().lookupClass(createdType);
  }

  public IMethod getMethod() {
    return node;
  }

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    CGNode cgn = CG.getNode(node, Everywhere.EVERYWHERE);
    assert cgn != null : node;
    return NonNullSingletonIterator.make(
        Pair.make(cgn, NewSiteReference.make(instructionIndex, createdType)));
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitCreationSiteVertex(this);
  }
}
