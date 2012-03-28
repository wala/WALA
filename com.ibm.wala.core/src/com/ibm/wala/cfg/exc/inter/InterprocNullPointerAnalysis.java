package com.ibm.wala.cfg.exc.inter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.NullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.cfg.exc.intra.NullPointerState;
import com.ibm.wala.cfg.exc.intra.NullPointerState.State;
import com.ibm.wala.cfg.exc.intra.ParameterState;
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
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.strings.Atom;

/**
 * Interprocedural NullPointer Analysis.
 * 
 * The interprocedural NullPointer analysis builds an implicit ICFG, visits all
 * CFGs in reverse invocation order recursively and propagates all parameter
 * states.
 * 
 * 1st run: collect and propagate all parameters on ENTRY nodes.
 * 2nd run: collect the results on the ENTRY nodes.
 * 
 * @author Markus Herhoffer <markus.herhoffer@student.kit.edu>
 * @author Juergen Graf <graf@kit.edu>
 * 
 */
public final class InterprocNullPointerAnalysis {

  private CallGraph cgFiltered = null;
  private final TypeReference[] ignoredExceptions;
  private final Map<CGNode, IntraprocAnalysisState> states;

  public static InterprocNullPointerAnalysis compute(final CallGraph cg, final IProgressMonitor progress)
      throws WalaException, UnsoundGraphException, CancelException {
    return compute(cg, NullPointerAnalysis.DEFAULT_IGNORE_EXCEPTIONS, progress);
  }
  
  public static InterprocNullPointerAnalysis compute(final CallGraph cg, final TypeReference[] ignoredExceptions,
      final IProgressMonitor progress) throws WalaException, UnsoundGraphException, CancelException {
    final InterprocNullPointerAnalysis inpa = new InterprocNullPointerAnalysis(ignoredExceptions);
    inpa.run(cg, progress);
    
    return inpa;
  }
  
  private InterprocNullPointerAnalysis(final TypeReference[] ignoredExceptions) {
    this.ignoredExceptions = ignoredExceptions;
    this.states = new HashMap<CGNode, IntraprocAnalysisState>();
  }

  private void run(final CallGraph cg, final IProgressMonitor progress) throws WalaException, UnsoundGraphException, CancelException {
    if (this.cgFiltered != null) {
      throw new IllegalStateException("This analysis has already been computed.");
    }

    // we filter out everything we do not need now
    this.cgFiltered = computeFilteredCallgraph(cg);

    // we start with the first node
    final CGNode firstNode = cgFiltered.getNode(0);
    findAndInjectInvokes(firstNode, new ParameterState(), new HashSet<CGNode>(), progress);
  }

  /**
   * Finds all invokes in a given <code>startNode</code> and traverses als
   * successors recursively.
   * 
   * @param startNode
   *          The node to start
   * @param paramState
   *          The parameter states of the <code>startNode</code>. May be
   *          <code>null</code>
   * @throws UnsoundGraphException
   * @throws CancelException
   * @throws WalaException
   */
  private void findAndInjectInvokes(final CGNode startNode, final ParameterState paramState, final Set<CGNode> visited,
      final IProgressMonitor progress) throws UnsoundGraphException, CancelException, WalaException {
    assert paramState != null;

    if (visited.contains(startNode)) {
      return;
    }
    
    visited.add(startNode);

    final Map<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> firstPass =
        analysisFirstPass(startNode, paramState, progress);

    // visit every invoked invoke
    for (final Entry<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> nodeEntry : firstPass.entrySet()) {
      final CGNode node = nodeEntry.getKey();
      final Map<SSAAbstractInvokeInstruction, ParameterState> invokes = nodeEntry.getValue();

      for (final Entry<SSAAbstractInvokeInstruction, ParameterState> instructionEntry : invokes.entrySet()) {
        findAndInjectInvokes(node, instructionEntry.getValue(), visited, progress);
      }
    }

    analysisSecondPass(startNode, paramState, progress);
  }
  
  private void analysisSecondPass(final CGNode startNode, final ParameterState paramState,
      final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
    final IR ir = startNode.getIR();
    if (!AnalysisUtil.isFakeRoot(startNode) && !(ir == null || ir.isEmptyIR())) {
      final MethodState mState = new InterprocMethodState(startNode, cgFiltered, states);

      // run intraprocedural part again with invoke exception info
      final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intra2 = 
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ignoredExceptions, ir, paramState, mState);
      final int deletedEdges2 = intra2.compute(progress);
      final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg2 = intra2.getCFG();
      final IntraprocAnalysisState singleState1 = states.get(startNode);
      final int deletedEdges1 = singleState1.compute(progress);
      final IntraprocAnalysisState singleState2 = new IntraprocAnalysisState(intra2, startNode, cfg2, deletedEdges2 + deletedEdges1);
      singleState2.setHasExceptions(intra2.hasExceptions());
      states.put(startNode, singleState2);
    }
  }
  
  private Map<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> analysisFirstPass(final CGNode startNode,
      final ParameterState paramState, final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
    final Map<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>> result =
        new HashMap<CGNode, Map<SSAAbstractInvokeInstruction, ParameterState>>();
    final IR ir = startNode.getIR();

    if (!startNode.getMethod().isStatic()) {
      // this pointer is never null
      paramState.setState(0, State.NOT_NULL);
    }

    // skip the fakeRoot and memorize the successors to visit them later
    if (AnalysisUtil.isFakeRoot(startNode)) {
      for (CGNode successor : getAllSuccessors(startNode)) {
        // we neither have an instruction nor a parameter state
        final HashMap<SSAAbstractInvokeInstruction, ParameterState> invokeMap =
            new HashMap<SSAAbstractInvokeInstruction, ParameterState>();
        invokeMap.put(null, null);
        result.put(successor, invokeMap);
      }

      // we have nothing to tell about the fakeroot
      states.put(startNode, new IntraprocAnalysisState());
    } else if (ir == null || ir.isEmptyIR()) {
      // we have nothing to tell about the empty IR
      states.put(startNode, new IntraprocAnalysisState());
    } else {
      final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> intra = 
          NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis(ignoredExceptions, ir, paramState, null);
      final int deletedEdges = intra.compute(progress);
      // Analyze the method with intraprocedural scope
      final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg = intra.getCFG();
      final IntraprocAnalysisState info = new IntraprocAnalysisState(intra, startNode, cfg, deletedEdges);
      info.setHasExceptions(intra.hasExceptions());
      states.put(startNode, info);

      // get the parameter's state out of the invoke block and collect them
      final Set<IExplodedBasicBlock> invokeBlocks = AnalysisUtil.extractInvokeBlocks(cfg);

      for (final IExplodedBasicBlock invokeBlock : invokeBlocks) {
        final NullPointerState state = intra.getState(invokeBlock);
        final SSAAbstractInvokeInstruction invokeInstruction = (SSAAbstractInvokeInstruction) invokeBlock.getInstruction();
        final int[] parameterNumbers = AnalysisUtil.getParameterNumbers(invokeInstruction);
        final ParameterState paramStateOfInvokeBlock = new ParameterState(state, parameterNumbers);
        final Set<CGNode> targets = cgFiltered.getPossibleTargets(startNode, invokeInstruction.getCallSite());

        for (final CGNode target : targets) {
          final HashMap<SSAAbstractInvokeInstruction, ParameterState> stateMap = new HashMap<SSAAbstractInvokeInstruction, ParameterState>();
          stateMap.put(invokeInstruction, paramStateOfInvokeBlock);
          result.put(target, stateMap);
        }
      }
    }
    
    return result;
  }
  
  /**
   * Returns all successor of a given cg's node.
   * 
   * @param node
   *          The node of the cg
   * @return a set with alle successors of <code>node</code>
   */
  private Set<CGNode> getAllSuccessors(CGNode node) {
    final Set<CGNode> successors = new HashSet<CGNode>();
    final Iterator<CGNode> it = cgFiltered.getSuccNodes(node);

    while (it.hasNext()) {
      final CGNode successor = it.next();
      successors.add(successor);
    }

    return successors;
  }

  /**
   * Returns the result of the interprocedural analysis.
   * 
   * @return Result of the interprocedural analysis.
   */
  public Map<CGNode, IntraprocAnalysisState> getResult() {
    return states;
  }

  /**
   * Reduces the Callgraph to only the nodes that we need
   */
  private static CallGraph computeFilteredCallgraph(final CallGraph cg) {
    final HashSet<Atom> filterSet = new HashSet<Atom>();
    final Atom worldClinit = Atom.findOrCreateAsciiAtom("fakeWorldClinit");
    filterSet.add(worldClinit);
    filterSet.add(MethodReference.initAtom);
    final CallGraphFilter filter = new CallGraphFilter(filterSet);

    return filter.filter(cg);
  }

  /**
   * Filter for CallGraphs
   * 
   * @author markus
   * 
   */
  private static class CallGraphFilter {
    private Set<Atom> filter;

    /**
     * Filter for CallGraphs
     * 
     * @param filterSet
     *          the MethodReferences to be filtered out
     */
    public CallGraphFilter(HashSet<Atom> filterSet) {
      this.filter = filterSet;
    }

    /**
     * filters a CallGraph
     * 
     * @param fullCG
     *          the original unfiltered CallGraph
     * @return the filtered CallGraph
     */
    public CallGraph filter(final CallGraph fullCG) {
      final HashSet<CGNode> nodes = new HashSet<CGNode>();

      // fill all nodes into a set
      for (final CGNode n : fullCG) {
        nodes.add(n);
      }

      final HashSet<CGNode> nodesToRemove = new HashSet<CGNode>();
      // collect all nodes that we do not need
      for (final CGNode node : nodes) {
        for (final Atom method : filter) {
          if (node.getMethod().getName().equals(method)) {
            nodesToRemove.add(node);
          }
        }
      }
      nodes.removeAll(nodesToRemove);

      final HashSet<CGNode> partialRoots = new HashSet<CGNode>();
      partialRoots.add(fullCG.getFakeRootNode());

      // delete the nodes
      final PartialCallGraph partialCG1 = PartialCallGraph.make(fullCG, partialRoots, nodes);

      // delete the nodes not reachable by root (consider the different implementations of "make")
      final PartialCallGraph partialCG2 = PartialCallGraph.make(partialCG1, partialRoots);

      return partialCG2;
    }
  }
}
