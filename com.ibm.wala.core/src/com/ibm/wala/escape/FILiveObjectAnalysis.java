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
package com.ibm.wala.escape;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractLocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder.TypedPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * A simple liveness analysis based on flow-insensitive pointer analysis.
 */
public class FILiveObjectAnalysis implements ILiveObjectAnalysis {

  /**
   * governing call graph
   */
  private final CallGraph callGraph;

  /**
   * Graph view of pointer analysis results
   */
  private final HeapGraph<?> heapGraph;

  /**
   * Cached map from InstanceKey -&gt; Set&lt;CGNode&gt;
   */
  final private Map<InstanceKey, Set<CGNode>> liveNodes = HashMapFactory.make();

  /**
   * Set of instanceKeys which are live everywhere
   */
  final private Set<InstanceKey> liveEverywhere = HashSetFactory.make();

  /**
   * A hack for now .. since right now the intraprocedural analysis is expensive
   */
  private final boolean expensiveIntraproceduralAnalysis;

  /**
   * 
   */
  public FILiveObjectAnalysis(CallGraph callGraph, HeapGraph<?> heapGraph, boolean expensiveIntraproceduralAnalysis) {
    super();
    this.callGraph = callGraph;
    this.heapGraph = heapGraph;
    this.expensiveIntraproceduralAnalysis = expensiveIntraproceduralAnalysis;
  }

  @Override
  public boolean mayBeLive(CGNode allocMethod, int allocPC, CGNode m, int instructionIndex) throws IllegalArgumentException,
      WalaException {
    if (allocMethod == null) {
      throw new IllegalArgumentException("allocMethod == null");
    }
    NewSiteReference site = TrivialMethodEscape.findAlloc(allocMethod, allocPC);
    InstanceKey ik = heapGraph.getHeapModel().getInstanceKeyForAllocation(allocMethod, site);
    return mayBeLive(ik, m, instructionIndex);
  }

  /**
   * @param instructionIndex index of an SSA instruction
   */
  @Override
  public boolean mayBeLive(InstanceKey ik, CGNode m, int instructionIndex) {
    if (liveEverywhere.contains(ik)) {
      return true;
    } else {
      Set<CGNode> live = liveNodes.get(ik);
      if (live != null) {
        if (live.contains(m)) {
          if (instructionIndex == -1) {
            return true;
          } else {
            if (mayBeLiveInSomeCaller(ik, m)) {
              // a hack. if it's live in some caller of m, assume it's live
              // throughout m. this may be imprecise.
              return true;
            } else {
              if (expensiveIntraproceduralAnalysis) {
                return mayBeLiveIntraprocedural(ik, m, instructionIndex);
              } else {
                // be conservative
                return true;
              }
            }
          }
        } else {
          return false;
        }
      } else {
        live = computeLiveNodes(ik);
        liveNodes.put(ik, live);
        return mayBeLive(ik, m, instructionIndex);
      }
    }
  }

  private boolean mayBeLiveInSomeCaller(InstanceKey ik, CGNode m) {
    for (CGNode n : Iterator2Iterable.make(callGraph.getPredNodes(m))) {
      if (mayBeLive(ik, n, -1)) {
        return true;
      }
    }
    return false;
  }

  /**
   * precondition: !mayBeLiveInSomeCaller(ik, m)
   * 
   * @param instructionIndex index of an SSA instruction
   */
  private boolean mayBeLiveIntraprocedural(InstanceKey ik, CGNode m, int instructionIndex) {

    IR ir = m.getIR();
    DefUse du = m.getDU();

    for (Object p : Iterator2Iterable.make(DFS.iterateDiscoverTime(GraphInverter.invert(heapGraph), ik))) {
      if (p instanceof LocalPointerKey) {
        LocalPointerKey lpk = (LocalPointerKey) p;
        if (lpk.getNode().equals(m)) {
          if (LocalLiveRangeAnalysis.isLive(lpk.getValueNumber(), instructionIndex, ir, du)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Compute the set of nodes in which ik may be live. If it's live everywhere, return an EMPTY_SET, but also record this fact in
   * the "liveEverywhere" set as a side effect
   */
  private Set<CGNode> computeLiveNodes(InstanceKey ik) {
    Set<CGNode> localRootNodes = HashSetFactory.make();
    for (Object node : Iterator2Iterable.make(DFS.iterateDiscoverTime(GraphInverter.invert(heapGraph), ik))) {
      if (node instanceof StaticFieldKey) {
        liveEverywhere.add(ik);
        return Collections.emptySet();
      } else {
        if (node instanceof AbstractLocalPointerKey) {
          AbstractLocalPointerKey local = (AbstractLocalPointerKey) node;
          localRootNodes.add(local.getNode());
        } else if (node instanceof TypedPointerKey) {
          TypedPointerKey t = (TypedPointerKey) node;
          node = t.getBase();
          if (node instanceof AbstractLocalPointerKey) {
            AbstractLocalPointerKey local = (AbstractLocalPointerKey) node;
            localRootNodes.add(local.getNode());
          } else {
            Assertions.UNREACHABLE("unexpected base of TypedPointerKey: " + node.getClass() + " " + node);
          }
        }
      }
    }
    return DFS.getReachableNodes(callGraph, localRootNodes);
  }

  @Override
  public boolean mayBeLive(InstanceKey ik, CGNode m, IntSet instructionIndices) {
    if (instructionIndices == null) {
      throw new IllegalArgumentException("instructionIndices is null");
    }
    // TODO this sucks
    for (IntIterator it = instructionIndices.intIterator(); it.hasNext();) {
      int i = it.next();
      if (mayBeLive(ik, m, i)) {
        return true;
      }
    }
    return false;
  }

}
