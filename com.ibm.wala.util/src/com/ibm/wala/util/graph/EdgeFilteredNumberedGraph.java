/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.graph;

import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * View of a {@link NumberedGraph} in which some edges have been filtered out
 */
public class EdgeFilteredNumberedGraph<T> extends AbstractNumberedGraph<T> {

  private final NumberedGraph<T> delegate;

  private final IBinaryNaturalRelation ignoreEdges;

  private final Edges edges;

  /**
   * 
   * @param delegate the underlying graph
   * @param ignoreEdges relation specifying which edges should be filtered out
   */
  public EdgeFilteredNumberedGraph(NumberedGraph<T> delegate, IBinaryNaturalRelation ignoreEdges) {
    super();
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
    public Iterator<T> getPredNodes(T N) {
      return new NodeIterator(getPredNodeNumbers(N));
    }

    @Override
    public int getSuccNodeCount(T N) {
      return getSuccNodeNumbers(N).size();
    }

    @Override
    public Iterator<T> getSuccNodes(T N) {
      return new NodeIterator(getSuccNodeNumbers(N));
    }

    @Override
    public boolean hasEdge(T src, T dst) {
      return delegate.hasEdge(src, dst) && !ignoreEdges.contains(getNumber(src), getNumber(dst));
    }

    @Override
    public IntSet getPredNodeNumbers(T node) {
      return getFilteredNodeNumbers(node, delegate.getPredNodeNumbers(node));
    }

    private IntSet getFilteredNodeNumbers(T node, IntSet s) {
      MutableIntSet result = MutableSparseIntSet.makeEmpty();
      for (IntIterator it = s.intIterator(); it.hasNext();) {
        int y = it.next();
        if (!ignoreEdges.contains(y, getNumber(node))) {
          result.add(y);
        }
      }
      return result;
    }

    @Override
    public IntSet getSuccNodeNumbers(T node) {
      return getFilteredNodeNumbers(node, delegate.getSuccNodeNumbers(node));
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