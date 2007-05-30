/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dataflow.IFDS;

import java.util.Iterator;

import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.IntSet;

/**
 * 
 * A "reversed" supergraph for backwards analysis.
 * 
 * In this view, a return is treated like a call, and vice-versa. All normal
 * edges are reversed.
 * 
 * @author sfink
 */
public class BackwardsSupergraph<T,P> implements ISupergraph<T,P> {

  /**
   * DEBUG_LEVEL:
   * <ul>
   * <li>0 No output
   * <li>1 Print some simple stats and warning information
   * <li>2 Detailed debugging
   * </ul>
   */
  static final int DEBUG_LEVEL = 0;
  private final ISupergraph<T,P> delegate;
  private final ExitFilter exitFilter = new ExitFilter();

  /**
   * @param forwardGraph
   *          the graph to ``reverse''
   */
  public BackwardsSupergraph(ISupergraph<T,P> forwardGraph) {
    this.delegate = forwardGraph;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMain()
   */
  public P getMain() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#isCall(java.lang.Object)
   */
  public boolean isCall(T n) {
    return delegate.isReturn(n);
  }

  /**
   * a filter that accepts only exit nodes from the original graph.
   */
  private class ExitFilter implements Filter {
    /*
     * @see com.ibm.wala.util.Filter#accepts(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public boolean accepts(Object o) {
      return delegate.isExit((T) o);
    }
  }

  /**
   * get the "called" (sic) nodes for a return site; i.e., the exit nodes that
   * flow directly to this return site.
   * 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCalledNodes(java.lang.Object)
   */
  public Iterator<T> getCalledNodes(T ret) {
    if (DEBUG_LEVEL > 1) {
      Trace.println(getClass() + " getCalledNodes " + ret);
      Trace.printCollection("called nodes ", new Iterator2Collection<Object>(new FilterIterator<Object>(getSuccNodes(ret), exitFilter)));
    }
    return new FilterIterator<T>(getSuccNodes(ret), exitFilter);
  }
  
  /**
   * get the "normal" successors (sic) for a return site; i.e., the "normal" CFG predecessors
   * that are not call nodes.
   * 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCalledNodes(java.lang.Object)
   */
  public Iterator<T> getNormalSuccessors(final T ret) {
    Iterator<? extends Object> allPreds = delegate.getPredNodes(ret);
    Filter sameProc = new Filter() {
      @SuppressWarnings("unchecked")
      public boolean accepts(Object o) {
        return getProcOf(ret).equals(getProcOf((T) o));
      }
    };
    Iterator<Object> sameProcPreds = new FilterIterator<Object>(allPreds,sameProc);
    Filter notCall = new Filter() {
      @SuppressWarnings("unchecked")
      public boolean accepts(Object o) {
        return !delegate.isCall((T) o);
      }
    };
    return new FilterIterator<T>(sameProcPreds,notCall);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getReturnSites(java.lang.Object)
   */
  public Iterator<? extends T> getReturnSites(T c) {
    return delegate.getCallSites(c);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#isExit(java.lang.Object)
   */
  public boolean isExit(T n) {
    return delegate.isEntry(n);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getProcOf(java.lang.Object)
   */
  public P getProcOf(T n) {
    return delegate.getProcOf(n);
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
   */
  public void removeNodeAndEdges(Object N) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  public Iterator<T> iterator() {
    return delegate.iterator();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
   */
  public void addNode(Object n) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
   */
  public void removeNode(Object n) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
   */
  public boolean containsNode(T N) {
    return delegate.containsNode(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
   */
  public Iterator<? extends T> getPredNodes(T N) {
    return delegate.getSuccNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
   */
  public int getPredNodeCount(T N) {
    return delegate.getSuccNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
   */
  public Iterator<? extends T> getSuccNodes(T N) {
    return delegate.getPredNodes(N);
  }
  
  
  public boolean hasEdge(T src, T dst) {
    return delegate.hasEdge(dst,src);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
   */
  public int getSuccNodeCount(T N) {
    return delegate.getPredNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object,
   *      java.lang.Object)
   */
  public void addEdge(Object src, Object dst) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }
  
  public void removeEdge(Object src, Object dst) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(java.lang.Object)
   */
  public void removeAllIncidentEdges(Object node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getEntriesForProcedure(java.lang.Object)
   */
  public T[] getEntriesForProcedure(P object) {
    return delegate.getExitsForProcedure(object);
  }
  
  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getEntriesForProcedure(java.lang.Object)
   */
  public T[] getExitsForProcedure(P object) {
    return delegate.getEntriesForProcedure(object);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMainEntry()
   */
  public T getMainEntry() {
    return delegate.getMainExit();
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMainExit()
   */
  public T getMainExit() {
    return delegate.getMainEntry();
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#isReturn(java.lang.Object)
   */
  public boolean isReturn(T n) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return false;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCallSites(java.lang.Object)
   */
  public Iterator<? extends T> getCallSites(T r) {
    return delegate.getReturnSites(r);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#isEntry(java.lang.Object)
   */
  public boolean isEntry(T n) {
    return delegate.isExit(n);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#classifyEdge(java.lang.Object,
   *      java.lang.Object)
   */
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

  public void removeIncomingEdges(Object node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    
  }

  public void removeOutgoingEdges(T node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    
  }

  /* 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getNumberOfBlocks(java.lang.Object)
   */
  public int getNumberOfBlocks(P procedure) {
    return delegate.getNumberOfBlocks(procedure);
  }

  /* 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlockNumber(java.lang.Object)
   */
  public int getLocalBlockNumber(T n) {
    return delegate.getLocalBlockNumber(n);
  }

  /* 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlock(java.lang.Object, int)
   */
  public T getLocalBlock(P procedure, int i) {
    return delegate.getLocalBlock(procedure,i);
  }

  public int getNumber(T N) {
    return delegate.getNumber(N);
  }

  public T getNode(int number) {
    return delegate.getNode(number);
  }

  public int getMaxNumber() {
    return delegate.getMaxNumber();
  }

  public Iterator<T> iterateNodes(IntSet s) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getSuccNodeNumbers(T node) {
    return delegate.getPredNodeNumbers(node);
  }

  public IntSet getPredNodeNumbers(Object node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }
}