/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.graph;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

/** View of a {@link NumberedGraph} in which some edges have been filtered out */
public class EdgeFilteredNumberedGraph<T> extends AbstractNumberedGraph<T> {

  private final NumberedGraph<T> delegate;

  private final IBinaryNaturalRelation ignoreEdges;

  private final Edges edges;

  /**
   * @param delegate the underlying graph
   * @param ignoreEdges relation specifying which edges should be filtered out
   */
  public EdgeFilteredNumberedGraph(NumberedGraph<T> delegate, IBinaryNaturalRelation ignoreEdges) {
    this.delegate = delegate;
    this.ignoreEdges = ignoreEdges;
    this.edges = new Edges();
  }

  @Override
  protected NumberedEdgeManager<T> getEdgeManager() {
    return edges;
  }

  private final class Edges implements NumberedEdgeManager<T> {

    private final class NodeIterator implements Iterator<T> {
      private final IntIterator nodeNumbers;

      private NodeIterator(IntSet nodeNumbers) {
        this.nodeNumbers = nodeNumbers.intIterator();
      }

      @Override
      public boolean hasNext() {
        return nodeNumbers.hasNext();
      }

      @Override
      public T next() {
        return getNode(nodeNumbers.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

    @Override
    public int getPredNodeCount(T N) {
      return getPredNodeNumbers(N).size();
    }

    @Override
    public Iterator<T> getPredNodes(@Nullable T N) {
      return new NodeIterator(getPredNodeNumbers(N));
    }

    @Override
    public int getSuccNodeCount(T N) {
      return getSuccNodeNumbers(N).size();
    }

    @Override
    public Iterator<T> getSuccNodes(@Nullable T N) {
      return new NodeIterator(getSuccNodeNumbers(N));
    }

    @Override
    public boolean hasEdge(@Nullable T src, @Nullable T dst) {
      return delegate.hasEdge(src, dst) && !ignoreEdges.contains(getNumber(src), getNumber(dst));
    }

    @Override
    public IntSet getPredNodeNumbers(@Nullable T node) {
      return getFilteredNodeNumbers(
          node, delegate.getPredNodeNumbers(node), Direction.PREDECESSORS);
    }

    /** The kind of neighbors whose edges {@link #getFilteredNodeNumbers} filters. */
    private enum Direction {
      /** Successors, whose edges run from the node to the neighbor. */
      SUCCESSORS,
      /** Predecessors, whose edges run from the neighbor to the node. */
      PREDECESSORS
    }

    /**
     * Returns the neighbors in {@code s} whose edge with {@code node} is not ignored.
     *
     * <p>An edge's direction depends on the kind of neighbor: it runs from {@code node} to a
     * successor, and from a predecessor to {@code node}. The ignored-edge relation is checked in
     * that direction.
     *
     * @param node the node whose neighbors are filtered
     * @param s the numbers of {@code node}'s neighbors in the delegate graph
     * @param direction whether {@code s} holds successors or predecessors of {@code node}
     * @return the numbers of the neighbors whose edge with {@code node} is not ignored
     */
    private IntSet getFilteredNodeNumbers(@Nullable T node, IntSet s, Direction direction) {
      int x = getNumber(node);
      MutableIntSet result = MutableSparseIntSet.makeEmpty();
      for (IntIterator it = s.intIterator(); it.hasNext(); ) {
        int y = it.next();
        boolean ignored =
            switch (direction) {
              case SUCCESSORS -> ignoreEdges.contains(x, y);
              case PREDECESSORS -> ignoreEdges.contains(y, x);
            };
        if (!ignored) {
          result.add(y);
        }
      }
      return result;
    }

    @Override
    public IntSet getSuccNodeNumbers(@Nullable T node) {
      return getFilteredNodeNumbers(node, delegate.getSuccNodeNumbers(node), Direction.SUCCESSORS);
    }

    @Override
    public void addEdge(T src, T dst) {
      Assertions.UNREACHABLE();
    }

    @Override
    public void removeAllIncidentEdges(T node) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }

    @Override
    public void removeEdge(T src, T dst) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }

    @Override
    public void removeIncomingEdges(T node) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }

    @Override
    public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }
  }

  @Override
  protected NumberedNodeManager<T> getNodeManager() {
    return delegate;
  }
}
