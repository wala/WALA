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

import java.util.Set;

import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.InterproceduralExceptionFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.InvertedGraph;
import com.ibm.wala.util.graph.impl.SelfLoopAddedGraph;

public class ExceptionAnalysis {
  private BitVectorSolver<CGNode> solver;
  private ExceptionToBitvectorTransformer transformer;

  public ExceptionAnalysis(CallGraph callgraph, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha) {
    this(callgraph, pointerAnalysis, cha, null);
  }

  public ExceptionAnalysis(CallGraph callgraph, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha,
      InterproceduralExceptionFilter<SSAInstruction> filter) {
    IntraproceduralResult intraResult = new IntraproceduralResult(callgraph, pointerAnalysis, cha, filter);
    transformer = new ExceptionToBitvectorTransformer(intraResult.getExceptions());
    ExceptionTransferFunctionProvider transferFunctionProvider = new ExceptionTransferFunctionProvider(intraResult, callgraph,
        transformer);

    Graph<CGNode> graph = new SelfLoopAddedGraph<>(new InvertedGraph<CGNode>(callgraph));
    BitVectorFramework<CGNode, TypeReference> problem = new BitVectorFramework<>(graph, transferFunctionProvider,
        transformer.getValues());

    solver = new ExceptionFlowSolver(problem, intraResult, transformer);
    solver.initForFirstSolve();
  }

  public void solve() {
    try {
      solver.solve(null);
    } catch (CancelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public Set<TypeReference> getCGNodeExceptions(CGNode node) {
    BitVectorVariable nodeResult = solver.getIn(node);
    if (nodeResult != null) {
      return transformer.computeExceptions(nodeResult);
    } else {
      return null;
    }
  }
}
