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

  /**
   * used for resolving field references in putstatic instructions
   */
  private final IClassHierarchy cha;

  /**
   * the supergraph over which tabulation is performed
   */
  private final ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph;

  /**
   * the tabulation domain
   */
  private final ReachingDefsDomain domain = new ReachingDefsDomain();

  public ContextSensitiveReachingDefs(CallGraph cg) {
    this.cha = cg.getClassHierarchy();
    // we use an ICFGSupergraph, which basically adapts ExplodedInterproceduralCFG to the ISupergraph interface
    this.supergraph = ICFGSupergraph.make(cg);
  }

  /**
   * controls numbering of putstatic instructions for use in tabulation
   */
  private class ReachingDefsDomain extends MutableMapping<Pair<CGNode, Integer>> implements
      TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> {

    private static final long serialVersionUID = 4014252274660361965L;

    @Override
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

    /**
     * the flow function for flow from a callee to caller where there was no flow from caller to callee; just the identity function
     * 
     * @see ReachingDefsProblem
     */
    @Override
    public IFlowFunction getUnbalancedReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      return IdentityFlowFunction.identity();
    }

    /**
     * flow function from caller to callee; just the identity function
     */
    @Override
    public IUnaryFlowFunction getCallFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest, BasicBlockInContext<IExplodedBasicBlock> ret) {
      return IdentityFlowFunction.identity();
    }

    /**
     * flow function from call node to return node when there are no targets for the call site; not a case we are expecting
     */
    @Override
    public IUnaryFlowFunction getCallNoneToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      // if we're missing callees, just keep what information we have
      return IdentityFlowFunction.identity();
    }

    /**
     * flow function from call node to return node at a call site when callees exist. We kill everything; surviving facts should
     * flow out of the callee
     */
    @Override
    public IUnaryFlowFunction getCallToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      return KillEverything.singleton();
    }

    /**
     * flow function for normal intraprocedural edges
     */
    @Override
    public IUnaryFlowFunction getNormalFlowFunction(final BasicBlockInContext<IExplodedBasicBlock> src,
        BasicBlockInContext<IExplodedBasicBlock> dest) {
      final IExplodedBasicBlock ebb = src.getDelegate();
      SSAInstruction instruction = ebb.getInstruction();
      if (instruction instanceof SSAPutInstruction) {
        final SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
        if (putInstr.isStatic()) {
          return new IUnaryFlowFunction() {

            @Override
            public IntSet getTargets(int d1) {
              // first, gen this statement
              int factNum = domain.getMappedIndex(Pair.make(src.getNode(), ebb.getFirstInstructionIndex()));
              assert factNum != -1;
              MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
              result.add(factNum);
              // if incoming statement is some different statement that defs the same static field, kill it; otherwise, keep it
              if (d1 != factNum) {
                IField staticField = cha.resolveField(putInstr.getDeclaredField());
                assert staticField != null;
                Pair<CGNode, Integer> otherPutInstrAndNode = domain.getMappedObject(d1);
                SSAPutInstruction otherPutInstr = (SSAPutInstruction) otherPutInstrAndNode.fst.getIR().getInstructions()[otherPutInstrAndNode.snd];
                IField otherStaticField = cha.resolveField(otherPutInstr.getDeclaredField());
                if (!staticField.equals(otherStaticField)) {
                  result.add(d1);
                }
              }
              return result;
            }

            @Override
            public String toString() {
              return "Reaching Defs Normal Flow";
            }
          };
        }
      }
      // identity function when src block isn't for a putstatic
      return IdentityFlowFunction.identity();
    }

    /**
     * standard flow function from callee to caller; just identity
     */
    @Override
    public IFlowFunction getReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> call,
        BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dest) {
      return IdentityFlowFunction.identity();
    }

  }

  /**
   * Definition of the reaching definitions tabulation problem. Note that we choose to make the problem a <em>partially</em>
   * balanced tabulation problem, where the solver is seeded with the putstatic instructions themselves. The problem is partially
   * balanced since a definition in a callee used as a seed for the analysis may then reach a caller, yielding a "return" without a
   * corresponding "call." An alternative to this approach, used in the Reps-Horwitz-Sagiv POPL95 paper, would be to "lift" the
   * domain of putstatic instructions with a 0 (bottom) element, have a 0->0 transition in all transfer functions, and then seed the
   * analysis with the path edge (main_entry, 0) -&gt; (main_entry, 0). We choose the partially-balanced approach to avoid pollution of
   * the flow functions.
   * 
   */
  private class ReachingDefsProblem implements
      PartiallyBalancedTabulationProblem<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> {

    private ReachingDefsFlowFunctions flowFunctions = new ReachingDefsFlowFunctions(domain);

    /**
     * path edges corresponding to all putstatic instructions, used as seeds for the analysis
     */
    private Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds = collectInitialSeeds();

    /**
     * we use the entry block of the CGNode as the fake entry when propagating from callee to caller with unbalanced parens
     */
    @Override
    public BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(BasicBlockInContext<IExplodedBasicBlock> node) {
      final CGNode cgNode = node.getNode();
      return getFakeEntry(cgNode);
    }

    /**
     * we use the entry block of the CGNode as the "fake" entry when propagating from callee to caller with unbalanced parens
     */
    private BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(final CGNode cgNode) {
      BasicBlockInContext<IExplodedBasicBlock>[] entriesForProcedure = supergraph.getEntriesForProcedure(cgNode);
      assert entriesForProcedure.length == 1;
      return entriesForProcedure[0];
    }

    /**
     * collect the putstatic instructions in the call graph as {@link PathEdge} seeds for the analysis
     */
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
            // note that the fact number used for the source of this path edge doesn't really matter
            result.add(PathEdge.createPathEdge(fakeEntry, factNum, bb, factNum));

          }
        }
      }
      return result;
    }

    @Override
    public IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> getFunctionMap() {
      return flowFunctions;
    }

    @Override
    public TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
      return domain;
    }

    /**
     * we don't need a merge function; the default unioning of tabulation works fine
     */
    @Override
    public IMergeFunction getMergeFunction() {
      return null;
    }

    @Override
    public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
      return supergraph;
    }

    @Override
    public Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds() {
      return initialSeeds;
    }

  }

  /**
   * perform the tabulation analysis and return the {@link TabulationResult}
   */
  public TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> analyze() {
    PartiallyBalancedTabulationSolver<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> solver = PartiallyBalancedTabulationSolver
        .createPartiallyBalancedTabulationSolver(new ReachingDefsProblem(), null);
    TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> result = null;
    try {
      result = solver.solve();
    } catch (CancelException e) {
      // this shouldn't happen 
      assert false;
    }
    return result;

  }

  public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
    return supergraph;
  }

  public TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
    return domain;
  }
}
