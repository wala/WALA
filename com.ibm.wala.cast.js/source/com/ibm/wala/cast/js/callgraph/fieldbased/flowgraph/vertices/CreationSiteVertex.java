package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import java.util.Iterator;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;

public class CreationSiteVertex extends Vertex implements ObjectVertex {
  private final IMethod node;
  private final int instructionIndex;
 
  public CreationSiteVertex(IMethod node, int instructionIndex) {
    super();
    this.node = node;
    this.instructionIndex = instructionIndex;
  }

  @Override
  public IClass getConcreteType() {
    return node.getClassHierarchy().lookupClass(JavaScriptTypes.Object);
  }

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    CGNode cgn = CG.getNode(node, Everywhere.EVERYWHERE);
    assert cgn != null : node;
    SSAInstruction inst = cgn.getIR().getInstructions()[instructionIndex];
    TypeReference type = inst instanceof SSANewInstruction? ((SSANewInstruction)inst).getConcreteType(): JavaScriptTypes.Object;
     
    return NonNullSingletonIterator.make(Pair.make(cgn, NewSiteReference.make(instructionIndex, type)));
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitCreationSiteVertex(this);
  }

}
