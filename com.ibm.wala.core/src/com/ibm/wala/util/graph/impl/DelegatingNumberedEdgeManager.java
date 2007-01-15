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
package com.ibm.wala.util.graph.impl;

import java.util.Iterator;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.INodeWithNumberedEdges;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * 
 * An object that delegates edge management to the nodes, INodeWithNumberedEdges
 * 
 * @author sfink
 * 
 */
public class DelegatingNumberedEdgeManager<T extends INodeWithNumber> implements EdgeManager<T> {

  private final DelegatingNumberedNodeManager<T> nodeManager;

  /**
   * 
   */
  public DelegatingNumberedEdgeManager(DelegatingNumberedNodeManager<T> nodeManager) {
    this.nodeManager = nodeManager;
  }

  // TODO: optimization is possible
  private class IntSetNodeIterator implements Iterator<T> {

    private final IntIterator delegate;

    IntSetNodeIterator(IntIterator delegate) {
      this.delegate = delegate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return delegate.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    public T next() {
      return nodeManager.getNode(delegate.next());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<T> getPredNodes(T N) {
    INodeWithNumberedEdges en = (INodeWithNumberedEdges) N;
    IntSet pred = en.getPredNumbers();
    Iterator<T> empty = EmptyIterator.instance();
    return (pred == null) ? empty : (Iterator<T>) new IntSetNodeIterator(pred.intIterator());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getPredNodeCount(T N) {
    INodeWithNumberedEdges en = (INodeWithNumberedEdges) N;
    return en.getPredNumbers().size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<T> getSuccNodes(T N) {
    INodeWithNumberedEdges en = (INodeWithNumberedEdges) N;
    IntSet succ = en.getSuccNumbers();
    Iterator<T> empty = EmptyIterator.instance();
    return (succ == null) ? empty: (Iterator<T>) new IntSetNodeIterator(succ.intIterator());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getSuccNodeCount(T N) {
    INodeWithNumberedEdges en = (INodeWithNumberedEdges) N;
    return en.getSuccNumbers().size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(com.ibm.wala.util.graph.Node,
   *      com.ibm.wala.util.graph.Node)
   */
  public void addEdge(T src, T dst) {
    Assertions.UNREACHABLE();
  }

  public void removeEdge(T src, T dst) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeAllIncidentEdges(T node) {
    INodeWithNumberedEdges n = (INodeWithNumberedEdges) node;
    n.removeAllIncidentEdges();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeIncomingEdges(T node) {
    INodeWithNumberedEdges n = (INodeWithNumberedEdges) node;
    n.removeIncomingEdges();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeOutgoingEdges(T node) {
    INodeWithNumberedEdges n = (INodeWithNumberedEdges) node;
    n.removeOutgoingEdges();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return super.toString();
  }

  /*
   * (non-Javadoc)
   */
  public boolean hasEdge(T src, T dst) {
    Assertions.UNREACHABLE("implement me");
    return false;
  }

}
