/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntSet;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A "reversed" supergraph for backwards analysis.
 *
 * <p>In this view, a return is treated like a call, and vice-versa. All normal edges are reversed.
 */
public class BackwardsSupergraph<T, P> implements ISupergraph<T, P> {

  /**
   * DEBUG_LEVEL:
   *
   * <ul>
   *   <li>0 No output
   *   <li>1 Print some simple stats and warning information
   *   <li>2 Detailed debugging
   * </ul>
   */
  static final int DEBUG_LEVEL = 0;

  private final ISupergraph<T, P> delegate;

  private final ExitFilter exitFilter = new ExitFilter();

  /** @param forwardGraph the graph to ``reverse'' */
  protected BackwardsSupergraph(ISupergraph<T, P> forwardGraph) {
    if (forwardGraph == null) {
      throw new IllegalArgumentException("null forwardGraph");
    }
    this.delegate = forwardGraph;
  }

  public static <T, P> BackwardsSupergraph<T, P> make(ISupergraph<T, P> forwardGraph) {
    return new BackwardsSupergraph<>(forwardGraph);
  }

  /** TODO: for now, this is not inverted. */
  @Override
  public Graph<P> getProcedureGraph() {
    return delegate.getProcedureGraph();
  }

  @Override
  public boolean isCall(T n) {
    return delegate.isReturn(n);
  }

  /** a filter that accepts only exit nodes from the original graph. */
  private class ExitFilter implements Predicate<T> {
    @Override
    public boolean test(T o) {
      return delegate.isExit(o);
    }
  }

  /**
   * get the "called" (sic) nodes for a return site; i.e., the exit nodes that flow directly to this
   * return site.
   */
  @SuppressWarnings("unused")
  @Override
  public Iterator<T> getCalledNodes(T ret) {
    if (DEBUG_LEVEL > 1) {
      System.err.println(getClass() + " getCalledNodes " + ret);
      System.err.println(
          "called nodes: "
              + Iterator2Collection.toSet(new FilterIterator<>(getSuccNodes(ret), exitFilter)));
    }
    return new FilterIterator<>(getSuccNodes(ret), exitFilter);
  }

  /**
   * get the "normal" successors (sic) for a return site; i.e., the "normal" CFG predecessors that
   * are not call nodes.
   *
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCalledNodes(java.lang.Object)
   */
  @Override
  public Iterator<T> getNormalSuccessors(final T ret) {
    Iterator<T> allPreds = delegate.getPredNodes(ret);
    Predicate<T> sameProc = o -> getProcOf(ret).equals(getProcOf(o)) && !delegate.isExit(o);
    Iterator<T> sameProcPreds = new FilterIterator<>(allPreds, sameProc);
    Predicate<T> notCall = o -> !delegate.isCall(o);
    return new FilterIterator<>(sameProcPreds, notCall);
  }

  @Override
  public Iterator<? extends T> getReturnSites(T c, P callee) {
    return delegate.getCallSites(c, callee);
  }

  @Override
  public boolean isExit(T n) {
    return delegate.isEntry(n);
  }

  @Override
  public P getProcOf(T n) {
    return delegate.getProcOf(n);
  }

  /** @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object) */
  @Override
  public void removeNodeAndEdges(Object N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<T> iterator() {
    return delegate.iterator();
  }

  @Override
  public Stream<T> stream() {
    return delegate.stream();
  }

  /** @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes() */
  @Override
  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes();
  }

  /** @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object) */
  @Override
  public void addNode(Object n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /** @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object) */
  @Override
  public void removeNode(Object n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /** @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object) */
  @Override
  public boolean containsNode(T N) {
    return delegate.containsNode(N);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object) */
  @Override
  public Iterator<T> getPredNodes(T N) {
    return delegate.getSuccNodes(N);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object) */
  @Override
  public int getPredNodeCount(T N) {
    return delegate.getSuccNodeCount(N);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object) */
  @Override
  public Iterator<T> getSuccNodes(T N) {
    return delegate.getPredNodes(N);
  }

  @Override
  public boolean hasEdge(T src, T dst) {
    return delegate.hasEdge(dst, src);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object) */
  @Override
  public int getSuccNodeCount(T N) {
    return delegate.getPredNodeCount(N);
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object, java.lang.Object) */
  @Override
  public void addEdge(Object src, Object dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEdge(Object src, Object dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /** @see com.ibm.wala.util.graph.EdgeManager#removeAllIncidentEdges(Object) */
  @Override
  public void removeAllIncidentEdges(Object node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public T[] getEntriesForProcedure(P object) {
    return delegate.getExitsForProcedure(object);
  }

  /** @see com.ibm.wala.dataflow.IFDS.ISupergraph#getEntriesForProcedure(java.lang.Object) */
  @Override
  public T[] getExitsForProcedure(P object) {
    return delegate.getEntriesForProcedure(object);
  }

  @Override
  public boolean isReturn(T n) throws UnimplementedError {
    return delegate.isCall(n);
  }

  @Override
  public Iterator<? extends T> getCallSites(T r, P callee) {
    return delegate.getReturnSites(r, callee);
  }

  @Override
  public boolean isEntry(T n) {
    return delegate.isExit(n);
  }

  @Override
  public byte classifyEdge(T src, T dest) {
    byte d = delegate.classifyEdge(dest, src);
    switch (d) {
      case CALL_EDGE:
        return RETURN_EDGE;
      case RETURN_EDGE:
        return CALL_EDGE;
      case OTHER:
        return OTHER;
      case CALL_TO_RETURN_EDGE:
        return CALL_TO_RETURN_EDGE;
      default:
        Assertions.UNREACHABLE();
        return -1;
    }
  }

  @Override
  public String toString() {
    return "Backwards of delegate\n" + delegate;
  }

  @Override
  public void removeIncomingEdges(Object node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getNumberOfBlocks(P procedure) {
    return delegate.getNumberOfBlocks(procedure);
  }

  @Override
  public int getLocalBlockNumber(T n) {
    return delegate.getLocalBlockNumber(n);
  }

  @Override
  public T getLocalBlock(P procedure, int i) {
    return delegate.getLocalBlock(procedure, i);
  }

  @Override
  public int getNumber(T N) {
    return delegate.getNumber(N);
  }

  @Override
  public T getNode(int number) {
    return delegate.getNode(number);
  }

  @Override
  public int getMaxNumber() {
    return delegate.getMaxNumber();
  }

  @Override
  public Iterator<T> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public IntSet getSuccNodeNumbers(T node) {
    return delegate.getPredNodeNumbers(node);
  }

  @Override
  public IntSet getPredNodeNumbers(Object node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }
}
