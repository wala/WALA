/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * A view of an SDG that excludes certain statements
 * 
 * @author sjfink
 *
 */
public class SDGView implements ISDG {
  
  private final ISDG delegate;
  
  private final Filter<Statement> notExcluded;

  public SDGView(final ISDG sdg, final Collection<Statement> exclusions) {
    super();
    this.delegate = sdg;
    this.notExcluded = new Filter<Statement>() {
      public boolean accepts(Statement o) {
        return !exclusions.contains(o);
      }
    };
  }

  public void addEdge(Statement src, Statement dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void addNode(Statement n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public boolean containsNode(Statement N) {
    return delegate.containsNode(N);
  }

  public int getMaxNumber() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return delegate.getMaxNumber();
  }

  public Statement getNode(int number) throws UnimplementedError {
    Assertions.UNREACHABLE(); 
    return delegate.getNode(number);
  }

  public int getNumber(Statement N) {
    return delegate.getNumber(N);
  }

  public int getNumberOfNodes() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return delegate.getNumberOfNodes();
  }

  public int getPredNodeCount(Statement N) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return delegate.getPredNodeCount(N);
  }

  public IntSet getPredNodeNumbers(Statement node) {
    // TODO: optimize me.
    MutableSparseIntSet result = new MutableSparseIntSet();
    for (Iterator<? extends Statement> it = getPredNodes(node); it.hasNext();) {
      Statement s = it.next();
      result.add(getNumber(s));
    }
    return result;
  }

  public Iterator<? extends Statement> getPredNodes(Statement N) {
    return new FilterIterator<Statement>(delegate.getPredNodes(N), notExcluded);
  }

  public int getSuccNodeCount(Statement N) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return delegate.getSuccNodeCount(N);
  }

  public IntSet getSuccNodeNumbers(Statement node) {
    // TODO: optimize me.
    MutableSparseIntSet result = new MutableSparseIntSet();
    for (Iterator<? extends Statement> it = getSuccNodes(node); it.hasNext();) {
      Statement s = it.next();
      result.add(getNumber(s));
    }
    return result;
  }

  public Iterator<? extends Statement> getSuccNodes(Statement N) {
    return new FilterIterator<Statement>(delegate.getSuccNodes(N), notExcluded);
  }

  public boolean hasEdge(Statement src, Statement dst) {
    // an optimization: rule out some heap-heap edges up front, before consulting hash sets
    if (src instanceof HeapStatement) {
      if (dst instanceof HeapStatement) {
        HeapStatement hs = (HeapStatement)src;
        HeapStatement hd = (HeapStatement)dst;
        if (!hs.getLocation().equals(hd.getLocation())) {
          return false;
        }
      }
    }
    
    
    return notExcluded.accepts(dst) && notExcluded.accepts(src) && delegate.hasEdge(src, dst);
  }

  public Iterator<Statement> iterator() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return delegate.iterator();
  }

  public Iterator<Statement> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return delegate.iterateNodes(s);
  }

  public void removeAllIncidentEdges(Statement node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeEdge(Statement src, Statement dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeIncomingEdges(Statement node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeNode(Statement n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeNodeAndEdges(Statement N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeOutgoingEdges(Statement node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public ControlDependenceOptions getCOptions() {
    return delegate.getCOptions();
  }

  public PDG getPDG(CGNode node) {
    return delegate.getPDG(node);
  }

  public Iterator<? extends Statement> iterateLazyNodes() {
    return delegate.iterateLazyNodes();
  }

}
