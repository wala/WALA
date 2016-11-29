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
package com.ibm.wala.analysis.exceptionanalysis;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.InterproceduralExceptionFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

/**
 * Wrapper to store multiple intraprocedural analysis for a call graph.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class CGIntraproceduralExceptionAnalysis {
  private Map<CGNode, IntraproceduralExceptionAnalysis> analysis;
  private Set<TypeReference> exceptions;
  private CallGraph callGraph;

  public CGIntraproceduralExceptionAnalysis(CallGraph cg, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha,
      InterproceduralExceptionFilter<SSAInstruction> filter) {
    this.callGraph = cg;
    this.exceptions = new LinkedHashSet<>();
    this.analysis = new LinkedHashMap<>();
    for (CGNode node : cg) {
      if (node.getIR() == null || node.getIR().isEmptyIR()) {
        analysis.put(node, IntraproceduralExceptionAnalysis.newDummy());
      } else {
        IntraproceduralExceptionAnalysis intraEA;
        intraEA = new IntraproceduralExceptionAnalysis(node, filter.getFilter(node), cha, pointerAnalysis);
        analysis.put(node, intraEA);
        exceptions.addAll(intraEA.getExceptions());
        exceptions.addAll(intraEA.getPossiblyCaughtExceptions());
      }
    }
  }

  /**
   * @param node
   * @return IntraproceduralExceptionAnalysis for given node.
   */
  public IntraproceduralExceptionAnalysis getAnalysis(CGNode node) {
    if (!callGraph.containsNode(node)) {
      throw new IllegalArgumentException("The given CG node has to be part " + "of the call graph given during construction.");
    }

    IntraproceduralExceptionAnalysis result = analysis.get(node);
    if (result == null) {
      throw new RuntimeException("Internal Error: No result for the given node.");
    }
    return result;
  }

  /**
   * Return a set of all Exceptions, which might occur within the given call
   * graph.
   * 
   * @return all exceptions, which might occur.
   */
  public Set<TypeReference> getExceptions() {
    return exceptions;
  }
}
