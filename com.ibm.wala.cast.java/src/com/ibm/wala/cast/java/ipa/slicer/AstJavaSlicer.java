/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.java.ipa.slicer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.traverse.DFS;

public class AstJavaSlicer extends Slicer {

  /*
   * Use the passed-in SDG
   */
  public static Collection<Statement> computeBackwardSlice(SDG sdg, Collection<Statement> ss) throws IllegalArgumentException,
      CancelException {
    return computeSlice(sdg, ss, true);
  }

  /**
   * @param ss a collection of statements of interest
   * @throws CancelException
   */
  public static Collection<Statement> computeSlice(SDG sdg, Collection<Statement> ss, boolean backward) throws CancelException {
    return new AstJavaSlicer().slice(sdg, ss, backward);
  }

  public static Set<Statement> gatherStatements(CallGraph CG, Collection<CGNode> partialRoots, Predicate<SSAInstruction> filter) {
    Set<Statement> result = new HashSet<>();
    for (CGNode n : DFS.getReachableNodes(CG, partialRoots)) {
      IR nir = n.getIR();
      if (nir != null) {
	SSAInstruction insts[] = nir.getInstructions();
	for (int i = 0; i < insts.length; i++) {
          if (filter.test(insts[i])) {
            result.add(new NormalStatement(n, i));
	  }
	}
      }
    }

    return result;
  }

  public static Set<Statement> gatherAssertions(CallGraph CG, Collection<CGNode> partialRoots) {
    return gatherStatements(CG, partialRoots, AstAssertInstruction.class::isInstance);
  }

  public static Set<Statement> gatherMonitors(CallGraph CG, Collection<CGNode> partialRoots) {
    return gatherStatements(CG, partialRoots, SSAMonitorInstruction.class::isInstance);
  }

  public static Set<Statement> gatherWrites(CallGraph CG, Collection<CGNode> partialRoots) {
    return gatherStatements(CG, partialRoots, o -> (o instanceof SSAPutInstruction) || (o instanceof SSAArrayStoreInstruction));
  }

  public static Set<Statement> gatherReads(CallGraph CG, Collection<CGNode> partialRoots) {
    return gatherStatements(CG, partialRoots, o -> (o instanceof SSAGetInstruction) || (o instanceof SSAArrayLoadInstruction));
  }

  public static Pair<Collection<Statement>, SDG<InstanceKey>> computeAssertionSlice(CallGraph CG, PointerAnalysis<InstanceKey> pa,
      Collection<CGNode> partialRoots, boolean multiThreadedCode) throws IllegalArgumentException, CancelException {
    CallGraph pcg = PartialCallGraph.make(CG, new LinkedHashSet<>(partialRoots));
    SDG<InstanceKey> sdg = new SDG<>(pcg, pa, new AstJavaModRef<>(), DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
    //System.err.println(("SDG:\n" + sdg));
    Set<Statement> stmts = gatherAssertions(CG, partialRoots);
    if (multiThreadedCode) {
      // Grab anything that has "side effects" under JMM
      stmts.addAll(gatherReads(CG, partialRoots));
      stmts.addAll(gatherWrites(CG, partialRoots));
      stmts.addAll(gatherMonitors(CG, partialRoots));
    }
    return Pair.make(AstJavaSlicer.computeBackwardSlice(sdg, stmts), sdg);
  }

}
