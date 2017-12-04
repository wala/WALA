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
package com.ibm.wala.ipa.callgraph;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.modref.GenReach;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Utility class for computing an analysis result for call graph nodes and their
 * transitive callees, given the results for individual nodes.
 * 
 */
public class CallGraphTransitiveClosure {

  
  /**
   * Compute the transitive closure of an analysis result over all callees.
   * 
   * @param cg the call graph
   * @param nodeResults analysis result for each individual node
   * @return a map from each node to the analysis result for the node and its transitive callees
   */
  public static <T> Map<CGNode, OrdinalSet<T>> transitiveClosure(CallGraph cg, Map<CGNode, Collection<T>> nodeResults) {
    try {
      // invert the call graph, to compute the bottom-up result
      GenReach<CGNode, T> gr = new GenReach<>(GraphInverter.invert(cg), nodeResults);
      BitVectorSolver<CGNode> solver = new BitVectorSolver<>(gr);
      solver.solve(null);
      Map<CGNode, OrdinalSet<T>> result = HashMapFactory.make();
      for (CGNode n : cg) {
        BitVectorVariable bv = solver.getOut(n);
        result.put(n, new OrdinalSet<>(bv.getValue(), gr.getLatticeValues()));
      }
      return result;
    } catch (CancelException e) {
      throw new CancelRuntimeException(e);
    }
  }
  
  /**
   * Collect analysis result for each {@link CGNode} in a {@link Map}.
   */
  public static <T> Map<CGNode, Collection<T>> collectNodeResults(CallGraph cg, Function<CGNode, Collection<T>> nodeResultComputer) {
    Map<CGNode, Collection<T>> result = HashMapFactory.make();
    for (CGNode n : cg) {
      result.put(n, nodeResultComputer.apply(n));
    }
    return result;
    
  }
}
