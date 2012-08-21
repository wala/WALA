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

    public void addEdge(T src, T dst) {
      Assertions.UNREACHABLE();
    }

    public int getPredNodeCount(T N) {
      Assertions.UNREACHABLE();
      return 0;
    }

    public Iterator<T> getPredNodes(T N) {
      Assertions.UNREACHABLE();
      return null;
    }

    public int getSuccNodeCount(T N) {
      Assertions.UNREACHABLE();
      return 0;
    }

    public Iterator<T> getSuccNodes(T N) {
      Assertions.UNREACHABLE();
      return null;
    }

    public boolean hasEdge(T src, T dst) {
      Assertions.UNREACHABLE();
      return false;
    }

    public void removeAllIncidentEdges(T node) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }

    public void removeEdge(T src, T dst) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }

    public void removeIncomingEdges(T node) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }

    public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
      Assertions.UNREACHABLE();
    }

    public IntSet getPredNodeNumbers(T node) {
      IntSet s = delegate.getPredNodeNumbers(node);
      MutableIntSet result = MutableSparseIntSet.makeEmpty();
      for (IntIterator it = s.intIterator(); it.hasNext();) {
        int y = it.next();
        if (!ignoreEdges.contains(y, getNumber(node))) {
          result.add(y);
        }
      }
      return result;
    }

    public IntSet getSuccNodeNumbers(T node) {
      Assertions.UNREACHABLE();
      return null;
    }

  }

  @Override
  protected NumberedNodeManager<T> getNodeManager() {
    return delegate;
  }
}