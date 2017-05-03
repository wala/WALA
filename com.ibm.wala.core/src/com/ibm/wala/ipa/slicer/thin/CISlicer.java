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
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * A cheap, context-insensitive slicer based on reachability over a custom SDG.
 * 
 * This is a prototype implementation; not tuned.
 * 
 * Currently supports backward slices only.
 * 
 * TODO: Introduce a slicer interface common between this and the CS slicer. TODO: This hasn't been tested much. Need regression
 * tests.
 */
public class CISlicer {

  /**
   * the dependence graph used for context-insensitive slicing
   */
  private final Graph<Statement> depGraph;

  public CISlicer(CallGraph cg, PointerAnalysis<InstanceKey> pa, DataDependenceOptions dOptions, ControlDependenceOptions cOptions) {
    this(cg, pa, ModRef.make(), dOptions, cOptions);
  }

  public CISlicer(CallGraph cg, PointerAnalysis<InstanceKey> pa, ModRef<InstanceKey> modRef, DataDependenceOptions dOptions, ControlDependenceOptions cOptions)
      throws IllegalArgumentException {
    if (dOptions == null) {
      throw new IllegalArgumentException("dOptions == null");
    }
    if (dOptions.equals(DataDependenceOptions.NO_BASE_PTRS) || dOptions.equals(DataDependenceOptions.FULL)) {
      throw new IllegalArgumentException("Heap data dependences requested in CISlicer!");
    }

    SDG<InstanceKey> sdg = new SDG<>(cg, pa, modRef, dOptions, cOptions, null);

    Map<Statement, Set<PointerKey>> mod = scanForMod(sdg, pa, modRef);
    Map<Statement, Set<PointerKey>> ref = scanForRef(sdg, pa, modRef);

    depGraph = GraphInverter.invert(new CISDG(sdg, mod, ref));

  }

  public CISlicer(final SDG<InstanceKey> sdg, final PointerAnalysis<InstanceKey> pa, final ModRef<InstanceKey> modRef) {
    Map<Statement, Set<PointerKey>> mod = scanForMod(sdg, pa, modRef);
    Map<Statement, Set<PointerKey>> ref = scanForRef(sdg, pa, modRef);

    depGraph = GraphInverter.invert(new CISDG(sdg, mod, ref));
  }

  public Collection<Statement> computeBackwardThinSlice(Statement seed) {
    Collection<Statement> slice = DFS.getReachableNodes(depGraph, Collections.singleton(seed));
    return slice;
  }

  public Collection<Statement> computeBackwardThinSlice(Collection<Statement> seeds) {
    Collection<Statement> slice = DFS.getReachableNodes(depGraph, seeds);
    return slice;
  }

  /**
   * Compute the set of pointer keys each statement mods
   */
  public static Map<Statement, Set<PointerKey>> scanForMod(SDG<InstanceKey> sdg, PointerAnalysis<InstanceKey> pa) {
    return scanForMod(sdg, pa, false, ModRef.make());
  }

  /**
   * Compute the set of pointer keys each statement refs
   */
  public static Map<Statement, Set<PointerKey>> scanForRef(SDG<InstanceKey> sdg, PointerAnalysis<InstanceKey> pa) {
    if (sdg == null) {
      throw new IllegalArgumentException("null sdg");
    }
    return scanForRef(sdg, pa, ModRef.make());
  }

  /**
   * Compute the set of pointer keys each statement mods
   */
  public static Map<Statement, Set<PointerKey>> scanForMod(SDG<InstanceKey> sdg, PointerAnalysis<InstanceKey> pa, ModRef<InstanceKey> modRef) {
    return scanForMod(sdg, pa, false, modRef);
  }

  /**
   * Compute the set of pointer keys each statement mods. Be careful to avoid eager PDG construction here! That means .. don't
   * iterate over SDG statements!
   */
  public static Map<Statement, Set<PointerKey>> scanForMod(SDG<InstanceKey> sdg, PointerAnalysis<InstanceKey> pa, boolean ignoreAllocHeapDefs, ModRef<InstanceKey> modRef) {
    if (pa == null) {
      throw new IllegalArgumentException("null pa");
    }
    ExtendedHeapModel h = new DelegatingExtendedHeapModel(pa.getHeapModel());
    Map<Statement, Set<PointerKey>> result = HashMapFactory.make();
    for (CGNode n : sdg.getCallGraph()) {
      IR ir = n.getIR();
      if (ir != null) {
        for (int i = 0; i < ir.getInstructions().length; i++) {
          SSAInstruction st = ir.getInstructions()[i];
          if (st != null) {
            Set<PointerKey> mod = modRef.getMod(n, h, pa, st, null, ignoreAllocHeapDefs);
            if (!mod.isEmpty()) {
              NormalStatement normal = new NormalStatement(n, i);
              result.put(normal, mod);
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Compute the set of PointerKeys each statement refs.Be careful to avoid eager PDG construction here! That means .. don't iterate
   * over SDG statements!
   */
  public static Map<Statement, Set<PointerKey>> scanForRef(SDG<InstanceKey> sdg, PointerAnalysis<InstanceKey> pa, ModRef<InstanceKey> modRef) {
    if (pa == null) {
      throw new IllegalArgumentException("null pa");
    }
    ExtendedHeapModel h = new DelegatingExtendedHeapModel(pa.getHeapModel());
    Map<Statement, Set<PointerKey>> result = HashMapFactory.make();
    for (CGNode n : sdg.getCallGraph()) {
      IR ir = n.getIR();
      if (ir != null) {
        for (int i = 0; i < ir.getInstructions().length; i++) {
          SSAInstruction st = ir.getInstructions()[i];
          if (st != null) {
            Set<PointerKey> mod = modRef.getRef(n, h, pa, st, null);
            if (!mod.isEmpty()) {
              NormalStatement normal = new NormalStatement(n, i);
              result.put(normal, mod);
            }
          }
        }
      }
    }
    return result;
  }

}
