package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;

public class SelfLoopAddedGraph <T> extends AbstractGraph<T> {

  final private NodeManager<T> nodes;

  @Override
  protected NodeManager<T> getNodeManager() {
    return nodes;
  }

  final private EdgeManager<T> edges;

  @Override
  protected EdgeManager<T> getEdgeManager() {
    return edges;
  }

  public SelfLoopAddedGraph(Graph<T> G) {
    nodes = G;
    edges = new SelfLoopAddedEdgeManager<>(G);
  }

}

