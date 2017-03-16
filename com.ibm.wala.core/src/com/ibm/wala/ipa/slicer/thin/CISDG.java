/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer.thin;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.ISDG;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;

/**
 * A context-insensitive SDG. This class assumes that it is given a normal NO_HEAP SDG. It adds context-insensitive heap information
 * directly from heap stores to corresponding loads, based on an underlying pointer analysis.
 */
public class CISDG implements ISDG {

  private final static boolean DEBUG = false;

  /**
   * the basic SDG, without interprocedural heap edges
   */
  final SDG<InstanceKey> noHeap;

  /**
   * What pointer keys does each statement mod?
   */
  private final Map<Statement, Set<PointerKey>> ref;

  /**
   * What pointer keys does each statement ref?
   */
  private final Map<Statement, Set<PointerKey>> mod;

  /**
   * What statements write each pointer key?
   */
  final Map<PointerKey, Set<Statement>> invMod;

  /**
   * What statements ref each pointer key?
   */
  final Map<PointerKey, Set<Statement>> invRef;

  protected CISDG(SDG<InstanceKey> noHeap, Map<Statement, Set<PointerKey>> mod, Map<Statement, Set<PointerKey>> ref) {
    this.noHeap = noHeap;
    this.mod = mod;
    this.ref = ref;
    invMod = MapUtil.inverseMap(mod);
    invRef = MapUtil.inverseMap(ref);
  }

  @Override
  public void addEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
    noHeap.addEdge(src, dst);
  }

  @Override
  public void addNode(Statement n) {
    Assertions.UNREACHABLE();
    noHeap.addNode(n);
  }

  @Override
  public boolean containsNode(Statement N) {
    return noHeap.containsNode(N);
  }

  @Override
  public boolean equals(Object obj) {
    Assertions.UNREACHABLE();
    return noHeap.equals(obj);
  }

  @Override
  public ControlDependenceOptions getCOptions() {
    Assertions.UNREACHABLE();
    return noHeap.getCOptions();
  }

  @Override
  public int getMaxNumber() {
    return noHeap.getMaxNumber();
  }

  @Override
  public Statement getNode(int number) {
    Assertions.UNREACHABLE();
    return noHeap.getNode(number);
  }

  @Override
  public int getNumber(Statement N) {
    return noHeap.getNumber(N);
  }

  @Override
  public int getNumberOfNodes() {
    return noHeap.getNumberOfNodes();
  }

  @Override
  public PDG<InstanceKey> getPDG(CGNode node) {
    Assertions.UNREACHABLE();
    return noHeap.getPDG(node);
  }

  @Override
  public int getPredNodeCount(Statement N) {
    return IteratorUtil.count(getPredNodes(N));
  }

  @Override
  public IntSet getPredNodeNumbers(Statement node) {
    Assertions.UNREACHABLE();
    return noHeap.getPredNodeNumbers(node);
  }

  @Override
  public Iterator<Statement> getPredNodes(Statement N) {
    if (DEBUG) {
      System.err.println("getPredNodes " + N);
    }
    if (ref.get(N) == null) {
      return noHeap.getPredNodes(N);
    } else {
      Collection<Statement> pred = HashSetFactory.make();
      for (PointerKey p : ref.get(N)) {
        if (invMod.get(p) != null) {
          pred.addAll(invMod.get(p));
        }
      }
      pred.addAll(Iterator2Collection.toSet(noHeap.getPredNodes(N)));
      return pred.iterator();
    }
  }

  @Override
  public int getSuccNodeCount(Statement N) {
    return IteratorUtil.count(getSuccNodes(N));
  }

  @Override
  public IntSet getSuccNodeNumbers(Statement node) {
    Assertions.UNREACHABLE();
    return noHeap.getSuccNodeNumbers(node);
  }

  @Override
  public Iterator<Statement> getSuccNodes(Statement N) {
    if (DEBUG) {
      System.err.println("getSuccNodes " + N);
    }
    if (mod.get(N) == null) {
      return noHeap.getSuccNodes(N);
    } else {
      Collection<Statement> succ = HashSetFactory.make();
      for (PointerKey p : mod.get(N)) {
        if (invRef.get(p) != null) {
          succ.addAll(invRef.get(p));
        }
      }
      succ.addAll(Iterator2Collection.toSet(noHeap.getSuccNodes(N)));
      return succ.iterator();
    }
  }

  @Override
  public boolean hasEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
    return noHeap.hasEdge(src, dst);
  }

  @Override
  public int hashCode() {
    Assertions.UNREACHABLE();
    return noHeap.hashCode();

  }

  @Override
  public Iterator<? extends Statement> iterateLazyNodes() {
    Assertions.UNREACHABLE();
    return noHeap.iterateLazyNodes();
  }

  @Override
  public Iterator<Statement> iterator() {
    return noHeap.iterator();
  }

  @Override
  public Iterator<Statement> iterateNodes(IntSet s) {
    Assertions.UNREACHABLE();
    return noHeap.iterateNodes(s);
  }

  @Override
  public void removeAllIncidentEdges(Statement node) {
    Assertions.UNREACHABLE();
    noHeap.removeAllIncidentEdges(node);
  }

  @Override
  public void removeEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
    noHeap.removeEdge(src, dst);
  }

  @Override
  public void removeIncomingEdges(Statement node) {
    Assertions.UNREACHABLE();
    noHeap.removeIncomingEdges(node);
  }

  @Override
  public void removeNode(Statement n) {
    Assertions.UNREACHABLE();
    noHeap.removeNode(n);
  }

  @Override
  public void removeNodeAndEdges(Statement N) {
    Assertions.UNREACHABLE();
    noHeap.removeNodeAndEdges(N);
  }

  @Override
  public void removeOutgoingEdges(Statement node) {
    Assertions.UNREACHABLE();
    noHeap.removeOutgoingEdges(node);
  }

  @Override
  public String toString() {
    Assertions.UNREACHABLE();
    return noHeap.toString();
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return noHeap.getClassHierarchy();
  }
}
