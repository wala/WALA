/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cfg.exc.inter;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.InterprocAnalysisResult;
import com.ibm.wala.cfg.exc.NullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.cfg.exc.intra.NullPointerState;
import com.ibm.wala.cfg.exc.intra.NullPointerState.State;
import com.ibm.wala.cfg.exc.intra.ParameterState;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Interprocedural NullPointer Analysis.
 *
 * <p>The interprocedural NullPointer analysis builds an implicit ICFG, visits all CFGs in reverse
 * invocation order recursively and propagates all parameter states.
 *
 * <p>1st run: collect and propagate all parameters on ENTRY nodes. 2nd run: collect the results on
 * the ENTRY nodes.
 *
 * <p>This class has been developed as part of a student project "Studienarbeit" by Markus
 * Herhoffer. It has been adapted and integrated into the WALA project by Juergen Graf.
 *
 * @author Markus Herhoffer &lt;markus.herhoffer@student.kit.edu&gt;
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 */
public final class InterprocNullPointerAnalysis {

  private CallGraph cgFiltered = null;
  private final TypeReference[] ignoredExceptions;
  private final MethodState defaultMethodState;
  private final Map<CGNode, IntraprocAnalysisState> states;
  private final boolean optHasExceptions;
  private CallGraph cg;

  public static InterprocNullPointerAnalysis compute(
      final TypeReference[] ignoredExceptions,
      final CallGraph cg,
      final MethodState defaultMethodState,
      final IProgressMonitor progress,
      boolean optHasExceptions)
      throws WalaException, UnsoundGraphException, CancelException {
    final InterprocNullPointerAnalysis inpa =
        new InterprocNullPointerAnalysis(ignoredExceptions, defaultMethodState, optHasExceptions);
    inpa.run(cg, progress);

    return inpa;
  }

  private InterprocNullPointerAnalysis(
      final TypeReference[] ignoredExceptions,
      final MethodState defaultMethodState,
      boolean optHasExceptions) {
    this.ignoredExceptions = ignoredExceptions;
    this.defaultMethodState = defaultMethodState;
    this.states = new HashMap<>();
    this.optHasExceptions = optHasExceptions;
  }

  private void run(final CallGraph cg, final IProgressMonitor progress)
      throws WalaException, UnsoundGraphException, CancelException {
    if (this.cgFiltered != null) {
      throw new IllegalStateException("This analysis has already been computed.");
    }

    // we filter out everything we do not need now
    this.cg = cg;
    this.cgFiltered = computeFilteredCallgraph(cg);

    // we start with the first node
    final CGNode firstNode = cgFiltered.getNode(0);
    findAndInjectInvokes(firstNode, new ParameterState(), new HashSet<>(), progress);
  }

  /**
   * Finds all invokes in a given {@code startNode} and traverses als successors recursively.
   *
   * @param startNode The node to start
   * @param paramState The parameter states of the {@code startNode}. May be {@code null}
   */
  private void findAndInjectInvokes(
      final CGNode startNode,
      final ParameterState paramState,
      final Set<CGNode> visited,
      final IProgressMonitor progress)
      throws UnsoundGraphException, CancelException, WalaException {
    assert paramState != null;

    if (!visited.add(startNode)) {
      return;
    }

    MonitorUtil.throwExceptionIfCanceled(progress);

    final Map<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> firstPass =
        analysisFirstPass(startNode, paramState, progress);

    // visit every invoked invoke
    for (final Entry<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> nodeEntry :
        firstPass.entrySet()) {
      MonitorUtil.throwExceptionIfCanceled(progress);

      final CGNode node = nodeEntry.getKey();
      final Map<SSAAbstractInvokeInstruction, ParameterState> invokes = nodeEntry.getValue();

      for (final Entry<SSAAbstractInvokeInstruction, ParameterState> instructionEntry :
          invokes.entrySet()) {
        findAndInjectInvokes(node, instructionEntry.getValue(), visited, progress);
      }
    }

    MonitorUtil.throwExceptionIfCanceled(progress);

    analysisSecondPass(startNode, paramState, progress);
  }

  private void analysisSecondPass(
      final CGNode startNode, final ParameterState paramState, final IProgressMonitor progress)
      throws UnsoundGraphException, CancelException {
    final IR ir = startNode.getIR();
    if (!AnalysisUtil.isFakeRoot(cg, startNode) && !(ir == null || ir.isEmptyIR())) {
      final MethodState ims = new InterprocMethodState(startNode, cgFiltered, states);
      final MethodState mState =
          (defaultMethodState != null ? new DelegatingMethodState(defaultMethodState, ims) : ims);

      // run intraprocedural part again with invoke exception info
      final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intra2 =
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(
              ignoredExceptions, ir, paramState, mState, optHasExceptions);
      final int deletedEdges2 = intra2.compute(progress);
      final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg2 = intra2.getCFG();
      final IntraprocAnalysisState singleState1 = states.get(startNode);
      final int deletedEdges1 = singleState1.compute(progress);
      final IntraprocAnalysisState singleState2 =
          new IntraprocAnalysisState(intra2, startNode, cfg2, deletedEdges2 + deletedEdges1);
      singleState2.setHasExceptions(intra2.hasExceptions());
      states.put(startNode, singleState2);
    }
  }

  private Map<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> analysisFirstPass(
      final CGNode startNode, final ParameterState paramState, final IProgressMonitor progress)
      throws UnsoundGraphException, CancelException {
    final Map<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> result = new HashMap<>();
    final IR ir = startNode.getIR();

    if (!startNode.getMethod().isStatic()) {
      // this pointer is never null
      paramState.setState(0, State.NOT_NULL);
    }

    if (ir == null || ir.isEmptyIR()) {
      // we have nothing to tell about the empty IR
      states.put(startNode, new IntraprocAnalysisState());
    } else {
      final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intra =
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(
              ignoredExceptions, ir, paramState, defaultMethodState, optHasExceptions);
      final int deletedEdges = intra.compute(progress);
      // Analyze the method with intraprocedural scope
      final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg = intra.getCFG();
      final IntraprocAnalysisState info =
          new IntraprocAnalysisState(intra, startNode, cfg, deletedEdges);
      info.setHasExceptions(intra.hasExceptions());
      states.put(startNode, info);

      // get the parameter's state out of the invoke block and collect them
      final Set<IExplodedBasicBlock> invokeBlocks = AnalysisUtil.extractInvokeBlocks(cfg);

      for (final IExplodedBasicBlock invokeBlock : invokeBlocks) {
        final NullPointerState state = intra.getState(invokeBlock);
        final SSAAbstractInvokeInstruction invokeInstruction =
            (SSAAbstractInvokeInstruction) invokeBlock.getInstruction();
        final int[] parameterNumbers = AnalysisUtil.getParameterNumbers(invokeInstruction);
        final ParameterState paramStateOfInvokeBlock = new ParameterState(state, parameterNumbers);
        final Set<CGNode> targets =
            cgFiltered.getPossibleTargets(startNode, invokeInstruction.getCallSite());

        for (final CGNode target : targets) {
          final HashMap<SSAAbstractInvokeInstruction, ParameterState> stateMap = new HashMap<>();
          stateMap.put(invokeInstruction, paramStateOfInvokeBlock);
          result.put(target, stateMap);
        }
      }
    }

    return result;
  }

  /**
   * Returns the result of the interprocedural analysis.
   *
   * @return Result of the interprocedural analysis.
   */
  public InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> getResult() {
    return new InterprocAnalysisResultWrapper(states);
  }

  /** Reduces the Callgraph to only the nodes that we need */
  private static CallGraph computeFilteredCallgraph(final CallGraph cg) {
    final HashSet<Atom> filterSet = new HashSet<>();
    final Atom worldClinit = Atom.findOrCreateAsciiAtom("fakeWorldClinit");
    filterSet.add(worldClinit);
    filterSet.add(MethodReference.initAtom);
    final CallGraphFilter filter = new CallGraphFilter(filterSet);

    return filter.filter(cg);
  }

  /**
   * Filter for CallGraphs
   *
   * @author Markus Herhoffer &lt;markus.herhoffer@student.kit.edu&gt;
   */
  private static class CallGraphFilter {
    private final Set<Atom> filter;

    /**
     * Filter for CallGraphs
     *
     * @param filterSet the MethodReferences to be filtered out
     */
    private CallGraphFilter(HashSet<Atom> filterSet) {
      this.filter = filterSet;
    }

    /**
     * filters a CallGraph
     *
     * @param fullCG the original unfiltered CallGraph
     * @return the filtered CallGraph
     */
    private CallGraph filter(final CallGraph fullCG) {

      // collect nodes not named for exclusion
      final HashSet<CGNode> nodes = new HashSet<>();
      fullCG.forEach(
          node -> {
            if (!filter.contains(node.getMethod().getName())) nodes.add(node);
          });

      final Set<CGNode> partialRoots = Collections.singleton(fullCG.getFakeRootNode());

      // delete the nodes
      final PartialCallGraph partialCG1 = PartialCallGraph.make(fullCG, partialRoots, nodes);

      // delete the nodes not reachable by root (consider the different implementations of "make")
      final PartialCallGraph partialCG2 = PartialCallGraph.make(partialCG1, partialRoots);

      return partialCG2;
    }
  }
}
