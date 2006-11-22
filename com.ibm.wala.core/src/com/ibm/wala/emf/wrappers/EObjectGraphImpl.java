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
package com.ibm.wala.emf.wrappers;

import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.ECollection;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.graph.EGraph;
import com.ibm.wala.ecore.graph.GraphFactory;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.IntSet;

/**
 *
 * An in-memory representation of a graph of EObjects.
 * This class is often more convenient for client-side programming than
 * the "raw" EMF-generated graph implementation.
 * 
 * @author sfink
 */
public class EObjectGraphImpl implements EObjectGraph {

  private NumberedGraph<EObject> delegate = new SlowSparseNumberedGraph<EObject>();


  /* (non-Javadoc)
   */
  public void addEdge(EObject src, EObject dst) {
    delegate.addEdge(src, dst);
  }
  
  public void removeEdge(EObject src, EObject dst) {
    delegate.removeEdge(src, dst);
  }

  /* (non-Javadoc)
   */
  public void addNode(EObject n) {
    delegate.addNode(n);
  }
 
  /* (non-Javadoc)
   */
  public boolean containsNode(EObject N) {
    return delegate.containsNode(N);
  }
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    return this == obj;
  }

  /* (non-Javadoc)
   */
  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes();
  }

  /* (non-Javadoc)
   */
  public int getPredNodeCount(EObject N) {
    return delegate.getPredNodeCount(N);
  }

  /* (non-Javadoc)
   */
  public Iterator<? extends EObject> getPredNodes(EObject N) {
    return delegate.getPredNodes(N);
  }

  /* (non-Javadoc)
   */
  public int getSuccNodeCount(EObject N) {
    return delegate.getSuccNodeCount(N);
  }

  /* (non-Javadoc)
   */
  public Iterator<? extends EObject> getSuccNodes(EObject N) {
    return delegate.getSuccNodes(N);
  }
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return delegate.hashCode();
  }

  /* (non-Javadoc)
   */
  public Iterator<? extends EObject> iterateNodes() {
    return delegate.iterateNodes();
  }

  /* (non-Javadoc)
   */
  public void removeAllIncidentEdges(EObject node) {
    delegate.removeAllIncidentEdges(node);
  }

  /* (non-Javadoc)
   */
  public void removeNode(EObject n) {
    delegate.removeNode(n);
  }

  /* (non-Javadoc)
   */
  public void removeNodeAndEdges(EObject N) {
    delegate.removeNodeAndEdges(N);
  }
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return delegate.toString();
  }
 
  /* (non-Javadoc)
   */
  public void removeIncomingEdges(EObject node) {
    delegate.removeIncomingEdges(node);
  }
  /**
   * @param node
   */
  public void removeOutgoingEdges(EObject node) {
    delegate.removeOutgoingEdges(node);
  }
  /* (non-Javadoc)
   */
  public int getMaxNumber() {
    return delegate.getMaxNumber();
  }
  /* (non-Javadoc)
   */
  public EObject getNode(int number) {
    return delegate.getNode(number);
  }
  /* (non-Javadoc)
   */
  public int getNumber(EObject N) {
    return delegate.getNumber(N);
  }
  /* (non-Javadoc)
   */
  public Iterator<EObject> iterateNodes(IntSet s) {
    return delegate.iterateNodes(s);
  }
  /**
   * @param g an EMF implementation of a graph
   * @return an EObjectGraph with the same nodes and edges of g
   */
  @SuppressWarnings("unchecked")
  public static EObjectGraph fromEMF(EGraph g) {
    EObjectGraphImpl result = new EObjectGraphImpl();
    for (Iterator<EObject> it = g.getNodes().getContents().iterator(); it.hasNext();) {
      result.addNode(it.next());
    }
    for (Iterator it = g.getEdges().getContents().iterator(); it.hasNext(); ) {
      EPair e = (EPair)it.next();
      result.addEdge(e.getX(),e.getY());
    }
    return result;
  }

  /**
   * @return an EGraph representing the contents of this EObjectGraph
   */
  @SuppressWarnings("unchecked")
  public EObject export() {
    EGraph result = GraphFactory.eINSTANCE.createEGraph();
    ECollection nodes = CommonFactory.eINSTANCE.createEContainer();
    for (Iterator it = iterateNodes(); it.hasNext(); ) {
      nodes.getContents().add(it.next());
    }
    ERelation edges = CommonFactory.eINSTANCE.createERelation();
    for (Iterator it = iterateNodes(); it.hasNext(); ) {
      EObject x = (EObject)it.next();
      for (Iterator it2 = getSuccNodes(x); it2.hasNext(); ) {
        EObject y = (EObject)it2.next();
        EPair p = CommonFactory.eINSTANCE.createEPair();
        p.setX(x);
        p.setY(y);
        edges.getContents().add(p);
      }
    }
    result.setNodes(nodes);
    result.setEdges(edges);
    return result;
  }

  /* (non-Javadoc)
   */
  public boolean hasEdge(EObject src, EObject dst) {
    return delegate.hasEdge(src,dst);
  }

  /* (non-Javadoc)
   */
  public IntSet getSuccNodeNumbers(EObject node) {
    return delegate.getSuccNodeNumbers(node);
  }

  /* (non-Javadoc)
   */
  public IntSet getPredNodeNumbers(EObject node) {
    return delegate.getPredNodeNumbers(node);
  }
  
}
