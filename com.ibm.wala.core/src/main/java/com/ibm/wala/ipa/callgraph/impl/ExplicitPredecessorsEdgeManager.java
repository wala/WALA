package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.IntMapIterator;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntSet;
import java.util.Iterator;
import java.util.function.IntFunction;

public abstract class ExplicitPredecessorsEdgeManager implements NumberedEdgeManager<CGNode> {

  private final NumberedNodeManager<CGNode> nodeManager;

  protected final IntFunction<CGNode> toNode;

  /** for each y, the {x | (x,y) is an edge) */
  protected final IBinaryNaturalRelation predecessors =
      new BasicNaturalRelation(
          new byte[] {BasicNaturalRelation.SIMPLE_SPACE_STINGY}, BasicNaturalRelation.SIMPLE);

  protected ExplicitPredecessorsEdgeManager(NumberedNodeManager<CGNode> nodeManager) {
    this.nodeManager = nodeManager;
    toNode =
        i -> {
          CGNode result = nodeManager.getNode(i);
          // if (Assertions.verifyAssertions && result == null) {
          // Assertions.UNREACHABLE("uh oh " + i);
          // }
          return result;
        };
  }

  @Override
  public IntSet getPredNodeNumbers(CGNode node) {
    int y = nodeManager.getNumber(node);
    return predecessors.getRelated(y);
  }

  @Override
  public Iterator<CGNode> getPredNodes(CGNode N) {
    IntSet s = getPredNodeNumbers(N);
    if (s == null) {
      return EmptyIterator.instance();
    } else {
      return new IntMapIterator<>(s.intIterator(), toNode);
    }
  }

  @Override
  public int getPredNodeCount(CGNode node) {
    int y = nodeManager.getNumber(node);
    return predecessors.getRelatedCount(y);
  }
}
