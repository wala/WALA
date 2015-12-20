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
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.InvertedGraph;

public class ExceptionAnalysis {
  private BitVectorSolver<CGNode> solver;
  private Exception2BitvectorTransformer transformer;

  public ExceptionAnalysis(CallGraph callgraph, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha) {
    this(callgraph, pointerAnalysis, cha, null);
  }

  public ExceptionAnalysis(CallGraph callgraph, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha,
      InterproceduralExceptionFilter<SSAInstruction> filter) {
    CGIntraproceduralExceptionAnalysis intraResult = new CGIntraproceduralExceptionAnalysis(callgraph, pointerAnalysis, cha, filter);
    transformer = new Exception2BitvectorTransformer(intraResult.getExceptions());
    ExceptionTransferFunctionProvider transferFunctionProvider = new ExceptionTransferFunctionProvider(intraResult, callgraph,
        transformer);

    Graph<CGNode> graph = new InvertedGraph<CGNode>(callgraph);
    BitVectorFramework<CGNode, TypeReference> problem = new BitVectorFramework<>(graph, transferFunctionProvider,
        transformer.getValues());

    solver = new InitializedBitVectorSolver(problem);
    solver.initForFirstSolve();
  }

  public void solve() {
    try {
      solver.solve(null);
    } catch (CancelException e) {
      throw new RuntimeException("Internal Error: Got Cancel Exception, "
          + "but didn't use Progressmonitor!", e);
    }
  }
  
  public void solve(IProgressMonitor monitor) throws CancelException {
      solver.solve(monitor);
  }  

  public Set<TypeReference> getCGNodeExceptions(CGNode node) {
    BitVectorVariable nodeResult = solver.getOut(node);
    if (nodeResult != null) {
      return transformer.computeExceptions(nodeResult);
    } else {
      return null;
    }
  }
}
