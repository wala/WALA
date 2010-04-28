/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.examples.analysis.dataflow;

import java.util.Collection;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.IdentityFlowFunction;
import com.ibm.wala.dataflow.IFDS.KillEverything;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationSolver;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.IFDS.TabulationSolver;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * Computes interprocedural reaching definitions for static fields in a context-sensitive manner via {@link TabulationSolver
 * tabulation}.
 */
public class ContextSensitiveReachingDefs {

  private final IClassHierarchy cha;

  private final ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph; 

  private ReachingDefsDomain domain = new ReachingDefsDomain();

  public ContextSensitiveReachingDefs(CallGraph cg, AnalysisCache cache) {
    this.cha = cg.getClassHierarchy();
    this.supergraph = ICFGSupergraph.make(cg, cache);
  }

  private class ReachingDefsDomain extends MutableMapping<Pair<CGNode, Integer>> implements
      TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> {

    public boolean hasPriorityOver(PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p1,
        PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p2) {
      // don't worry about worklist priorities
      return false;
    }

  }

  private class ReachingDefsFlowFunctions implements IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> {

    private final ReachingDefsDomain domain;

    protected ReachingDefsFlowFunctions(ReachingDefsDomain domain) {
      this.domain = domain;
    }

    public IFlowFunction getUnbalancedReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      return IdentityFlowFunction.identity();
    }

    public IUnaryFlowFunction getCallFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, BasicBlockInContext<IExplodedBasicBlock> ret) {
      // just send the fact into the callee
      return IdentityFlowFunction.identity();
    }

    public IUnaryFlowFunction getCallNoneToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      // if we're missing callees, just give up and kill everything
      return KillEverything.singleton();
    }

    public IUnaryFlowFunction getCallToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      // kill everything; surviving facts should flow out of the callee
      return KillEverything.singleton();
    }

    public IUnaryFlowFunction getNormalFlowFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      final IExplodedBasicBlock ebb = src.getDelegate();
      SSAInstruction instruction = ebb.getInstruction();
      if (instruction instanceof SSAPutInstruction) {
        final SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
        if (putInstr.isStatic()) {
          return new IUnaryFlowFunction() {

            public IntSet getTargets(int d1) {
              int factNum = domain.getMappedIndex(Pair.make(src.getNode(), ebb.getFirstInstructionIndex()));
              IField staticField = cha.resolveField(putInstr.getDeclaredField());
              assert staticField != null;
              assert factNum != -1;
              MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
              result.add(factNum);
              if (d1 != factNum) {
                Pair<CGNode, Integer> otherPutInstrAndNode = domain.getMappedObject(d1);
                // if it writes the same field, kill it
                SSAPutInstruction otherPutInstr = (SSAPutInstruction) otherPutInstrAndNode.fst.getIR().getInstructions()[otherPutInstrAndNode.snd];
                IField otherStaticField = cha.resolveField(otherPutInstr.getDeclaredField());
                if (!staticField.equals(otherStaticField)) {
                  result.add(d1);
                }
              }
              return result;
            }

            public String toString() {
              return "Reaching Defs Normal Flow";
            }
          };
        }
      }
      return IdentityFlowFunction.identity();
    }

    public IFlowFunction getReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> call,
        BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dest) {
      return IdentityFlowFunction.identity();
    }

  }

  private class ReachingDefsProblem implements
      PartiallyBalancedTabulationProblem<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> {

    private ReachingDefsFlowFunctions flowFunctions = new ReachingDefsFlowFunctions(domain);

    private Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds = collectInitialSeeds();

    /**
     * we use the entry block of the CGNode as the fake entry when propagating from callee to caller with unbalanced parens
     */
    public BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(BasicBlockInContext<IExplodedBasicBlock> node) {
      final CGNode cgNode = node.getNode();
      return getFakeEntry(cgNode);
    }

    /**
     * we use the entry block of the CGNode as the fake entry when propagating from callee to caller with unbalanced parens
     */
    private BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(final CGNode cgNode) {
      BasicBlockInContext<IExplodedBasicBlock>[] entriesForProcedure = supergraph.getEntriesForProcedure(cgNode);
      assert entriesForProcedure.length == 1;
      return entriesForProcedure[0];
    }

    private Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> collectInitialSeeds() {
      Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> result = HashSetFactory.make();
      for (BasicBlockInContext<IExplodedBasicBlock> bb : supergraph) {
        IExplodedBasicBlock ebb = bb.getDelegate();
        SSAInstruction instruction = ebb.getInstruction();
        if (instruction instanceof SSAPutInstruction) {
          SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
          if (putInstr.isStatic()) {
            final CGNode cgNode = bb.getNode();
            Pair<CGNode, Integer> fact = Pair.make(cgNode, ebb.getFirstInstructionIndex());
            int factNum = domain.add(fact);
            BasicBlockInContext<IExplodedBasicBlock> fakeEntry = getFakeEntry(cgNode);
            result.add(PathEdge.createPathEdge(fakeEntry, factNum, bb, factNum));

          }
        }
      }
      return result;
    }

    public IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> getFunctionMap() {
      return flowFunctions;
    }

    public TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
      return domain;
    }

    public IMergeFunction getMergeFunction() {
      return null;
    }

    public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
      return supergraph;
    }

    public Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds() {
      return initialSeeds;
    }

  }

  public TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> analyze() throws CancelException {
    PartiallyBalancedTabulationSolver<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> solver = PartiallyBalancedTabulationSolver
        .createPartiallyBalancedTabulationSolver(new ReachingDefsProblem(), null);
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> result = solver.solve();
    return result;

  }
  
  public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
    return supergraph;
  }
  
  public TabulationDomain<Pair<CGNode,Integer>, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
    return domain;
  }
}
