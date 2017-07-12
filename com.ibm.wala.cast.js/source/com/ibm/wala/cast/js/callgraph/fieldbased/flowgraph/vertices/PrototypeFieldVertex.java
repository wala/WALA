package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

public class PrototypeFieldVertex extends Vertex implements PointerKey {

  public enum PrototypeField { __proto__, prototype }

  private final PrototypeField field;
  private final ObjectVertex type;
  
  public PrototypeFieldVertex(PrototypeField field, ObjectVertex type) {
    this.field = field;
    this.type = type;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitPrototypeVertex(this);
  }
  
  @Override
  public String toString() {
    return field + ":" + type;
  }
}
