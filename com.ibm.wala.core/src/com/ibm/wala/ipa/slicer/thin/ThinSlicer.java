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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.ISDG;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.IntSet;

/**
 * A cheap, context-insensitive thin slicer based on reachability over a custom SDG.
 * 
 * This is a prototype implementation; not tuned.
 * 
 * Currently supports backward slices only.
 * 
 * TODO: Introduce a slicer interface common between this and the CS slicer.
 * TODO: This hasn't been tested much.   Need regression tests.
 * 
 * @author sjfink
 *
 */
public class ThinSlicer {
  
  /**
   * the dependence graph used for thin slicing
   */
  private final Graph<Statement> depGraph;

  public ThinSlicer(CallGraph cg, PointerAnalysis pa) {
    SDG sdg = new SDG(cg, pa, DataDependenceOptions.NO_BASE_NO_HEAP, ControlDependenceOptions.NONE, null);

    Map<Statement, Set<PointerKey>> mod = scanForMod(sdg, pa);
    Map<Statement, Set<PointerKey>> ref = scanForRef(sdg, pa);

    depGraph = GraphInverter.invert(new CISDG(sdg, mod, ref));
    
  }
  
  public Collection<Statement> computeBackwardThinSlice(Statement seed) {
    Collection<Statement> slice = DFS.getReachableNodes(depGraph, Collections.singleton(seed));
    return slice;
  }

  /**
   * A context-insensitive SDG.   This class computes a normal NO_BASE_NO_HEAP SDG, then adds
   * directly from heap stores to corresponding loads, based on an underlying pointer analysis.
   *
   */
  private static class CISDG implements ISDG {
    /**
     * the basic SDG, without interprocedural heap edges
     */
    SDG noHeap;

    /**
     * What pointer keys does each statement ref?
     */
    private final Map<Statement, Set<PointerKey>> ref;

    /**
     * What statmements write each pointer key?
     */
    final Map<PointerKey, Set<Statement>> invMod;

    /**
     * What statements ref each pointer key?
     */
    final Map<PointerKey, Set<Statement>> invRef;

    CISDG(SDG noHeap, Map<Statement, Set<PointerKey>> mod, Map<Statement, Set<PointerKey>> ref) {
      this.noHeap = noHeap;
      this.ref = ref;
      invMod = MapUtil.inverseMap(mod);
      invRef = MapUtil.inverseMap(ref);
    }

    public void addEdge(Statement src, Statement dst) {
      Assertions.UNREACHABLE();
      noHeap.addEdge(src, dst);
    }

    public void addNode(Statement n) {
      Assertions.UNREACHABLE();
      noHeap.addNode(n);
    }

    public boolean containsNode(Statement N) {
      Assertions.UNREACHABLE();
      return noHeap.containsNode(N);
    }

    @Override
    public boolean equals(Object obj) {
      Assertions.UNREACHABLE();
      return noHeap.equals(obj);
    }

    public ControlDependenceOptions getCOptions() {
      Assertions.UNREACHABLE();
      return noHeap.getCOptions();
    }

    public int getMaxNumber() {
      return noHeap.getMaxNumber();
    }

    public Statement getNode(int number) {
      Assertions.UNREACHABLE();
      return noHeap.getNode(number);
    }

    public int getNumber(Statement N) {
      return noHeap.getNumber(N);
    }

    public int getNumberOfNodes() {
      return noHeap.getNumberOfNodes();
    }

    public PDG getPDG(CGNode node) {
      Assertions.UNREACHABLE();
      return noHeap.getPDG(node);
    }

    public int getPredNodeCount(Statement N) {
      Assertions.UNREACHABLE();
      return noHeap.getPredNodeCount(N);
    }

    public IntSet getPredNodeNumbers(Statement node) {
      Assertions.UNREACHABLE();
      return noHeap.getPredNodeNumbers(node);
    }

    public Iterator<? extends Statement> getPredNodes(Statement N) {
      if (ref.get(N) == null) {
        return noHeap.getPredNodes(N);
      } else {
        Collection<Statement> pred = HashSetFactory.make();
        for (PointerKey p : ref.get(N)) {
          if (invMod.get(p) != null) {
            // TODO: WTF? HOW CAN IT BE NULL?
            pred.addAll(invMod.get(p));
          }
        }
        pred.addAll(Iterator2Collection.toCollection(noHeap.getPredNodes(N)));
        return pred.iterator();
      }
    }

    public int getSuccNodeCount(Statement N) {
      Assertions.UNREACHABLE();
      return noHeap.getSuccNodeCount(N);
    }

    public IntSet getSuccNodeNumbers(Statement node) {
      Assertions.UNREACHABLE();
      return noHeap.getSuccNodeNumbers(node);
    }

    public Iterator<? extends Statement> getSuccNodes(Statement N) {
      Assertions.UNREACHABLE();
      return noHeap.getSuccNodes(N);
    }

    public boolean hasEdge(Statement src, Statement dst) {
      Assertions.UNREACHABLE();
      return noHeap.hasEdge(src, dst);
    }

    @Override
    public int hashCode() {
      Assertions.UNREACHABLE();
      return noHeap.hashCode();

    }

    public Iterator<? extends Statement> iterateLazyNodes() {
      Assertions.UNREACHABLE();
      return noHeap.iterateLazyNodes();
    }

    public Iterator<Statement> iterator() {
      return noHeap.iterator();
    }

    public Iterator<Statement> iterateNodes(IntSet s) {
      Assertions.UNREACHABLE();
      return noHeap.iterateNodes(s);
    }

    public void removeAllIncidentEdges(Statement node) {
      Assertions.UNREACHABLE();
      noHeap.removeAllIncidentEdges(node);
    }

    public void removeEdge(Statement src, Statement dst) {
      Assertions.UNREACHABLE();
      noHeap.removeEdge(src, dst);
    }

    public void removeIncomingEdges(Statement node) {
      Assertions.UNREACHABLE();
      noHeap.removeIncomingEdges(node);
    }

    public void removeNode(Statement n) {
      Assertions.UNREACHABLE();
      noHeap.removeNode(n);
    }

    public void removeNodeAndEdges(Statement N) {
      Assertions.UNREACHABLE();
      noHeap.removeNodeAndEdges(N);
    }

    public void removeOutgoingEdges(Statement node) {
      Assertions.UNREACHABLE();
      noHeap.removeOutgoingEdges(node);
    }

    @Override
    public String toString() {
      Assertions.UNREACHABLE();
      return noHeap.toString();
    }
  }

  /**
   * Compute the set of pointer keys each statement mods
   */
  private static Map<Statement, Set<PointerKey>> scanForMod(SDG sdg, PointerAnalysis pa) {
    ExtendedHeapModel h = new DelegatingExtendedHeapModel(pa.getHeapModel());
    Map<Statement, Set<PointerKey>> result = HashMapFactory.make();
    for (Iterator<? extends Statement> it = sdg.iterator(); it.hasNext();) {
      Statement st = it.next();
      switch (st.getKind()) {
      case NORMAL:
        Set<PointerKey> c = HashSetFactory.make(ModRef.getMod(st.getNode(), h, pa, ((NormalStatement) st).getInstruction(),
            null));
        result.put(st, c);
        break;
      }

    }
    return result;
  }

  /**
   * Compute the set of PointerKeys each statement refs.
   */
  private static Map<Statement, Set<PointerKey>> scanForRef(SDG sdg, PointerAnalysis pa) {
    ExtendedHeapModel h = new DelegatingExtendedHeapModel(pa.getHeapModel());
    Map<Statement, Set<PointerKey>> result = HashMapFactory.make();
    for (Iterator<? extends Statement> it = sdg.iterator(); it.hasNext();) {
      Statement st = it.next();
      switch (st.getKind()) {
      case NORMAL:
        Set<PointerKey> c = HashSetFactory.make(ModRef.getRef(st.getNode(), h, pa, ((NormalStatement) st).getInstruction(),
            null));
        result.put(st, c);
        break;
      }

    }
    return result;
  }

}
