package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.collections.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RandomGraph<T> extends SlowSparseNumberedGraph<T> {

  /** */
  private static final long serialVersionUID = 5950736619507540953L;

  protected abstract T makeNode(int i);

  public RandomGraph(int nodes, int edges) {
    for (int i = 0; i < nodes; i++) {
      addNode(makeNode(i));
    }

    List<Pair<Integer, Integer>> allEdges = new ArrayList<>(nodes * nodes);
    for (int i = 0; i < nodes; i++) {
      for (int j = 0; j < nodes; j++) {
        allEdges.add(Pair.make(i, j));
      }
    }

    Collections.shuffle(allEdges);

    for (int i = 0; i < edges; i++) {
      addEdge(getNode(allEdges.get(i).fst), getNode(allEdges.get(i).snd));
    }
  }

  public static class IntegerRandomGraph extends RandomGraph<Integer> {

    /** */
    private static final long serialVersionUID = -4216451570756483022L;

    @Override
    protected Integer makeNode(int i) {
      return i;
    }

    public IntegerRandomGraph(int nodes, int edges) {
      super(nodes, edges);
    }
  }
}
