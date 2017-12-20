/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dataflow.IFDS;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Heap;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.ToStringComparator;
import com.ibm.wala.util.heapTrace.HeapTracer;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * A precise interprocedural tabulation solver.
 * <p>
 * See Reps, Horwitz, Sagiv POPL 95.
 * <p>
 * This version differs in some ways from the POPL algorithm. In particular ...
 * <ul>
 * <li>to support exceptional control flow ... there may be several return sites for each call site.
 * <li>it supports an optional merge operator, useful for non-IFDS problems and widening.
 * <li>it stores summary edges at each callee instead of at each call site.
 * </ul>
 * <p>
 *
 * @param <T> type of node in the supergraph
 * @param <P> type of a procedure (like a box in an RSM)
 * @param <F> type of factoids propagated when solving this problem
 */
public class TabulationSolver<T, P, F> {

  /**
   * DEBUG_LEVEL:
   * <ul>
   * <li>0 No output
   * <li>1 Print some simple stats and warning information
   * <li>2 Detailed debugging
   * <li>3 Also print worklists
   * </ul>
   */
  protected static final int DEBUG_LEVEL = 0;

  static protected final boolean verbose = true && ("true".equals(System.getProperty("com.ibm.wala.fixedpoint.impl.verbose")) ? true
      : false);

  static final int VERBOSE_INTERVAL = 1000;

  static final boolean VERBOSE_TRACE_MEMORY = false;

  private static int verboseCounter = 0;

  /**
   * Should we periodically clear out soft reference caches in an attempt to help the GC?
   */
  final protected static boolean PERIODIC_WIPE_SOFT_CACHES = true;

  /**
   * Interval which defines the period to clear soft reference caches
   */
  private final static int WIPE_SOFT_CACHE_INTERVAL = 1000000;

  /**
   * Counter for wiping soft caches
   */
  private static int wipeCount = WIPE_SOFT_CACHE_INTERVAL;

  /**
   * The supergraph which induces this dataflow problem
   */
  protected final ISupergraph<T, P> supergraph;

  /**
   * A map from an edge in a supergraph to a flow function
   */
  protected final IFlowFunctionMap<T> flowFunctionMap;

  /**
   * The problem being solved.
   */
  private final TabulationProblem<T, P, F> problem;

  /**
   * A map from Object (entry node in supergraph) -&gt; LocalPathEdges.
   *
   * Logically, this represents a set of edges (s_p,d_i) -&gt; (n, d_j). The data structure is chosen to attempt to save space over
   * representing each edge explicitly.
   */
  final private Map<T, LocalPathEdges> pathEdges = HashMapFactory.make();

  /**
   * A map from Object (entry node in supergraph) -&gt; CallFlowEdges.
   *
   * Logically, this represents a set of edges (c,d_i) -&gt; (s_p, d_j). The data structure is chosen to attempt to save space over
   * representing each edge explicitly.
   */
  final private Map<T, CallFlowEdges> callFlowEdges = HashMapFactory.make();

  /**
   * A map from Object (procedure) -&gt; LocalSummaryEdges.
   *
   */
  final protected Map<P, LocalSummaryEdges> summaryEdges = HashMapFactory.make();

  /**
   * the set of all {@link PathEdge}s that were used as seeds during the tabulation, grouped by procedure.
   */
  private final Map<P, Set<PathEdge<T>>> seeds = HashMapFactory.make();

  /**
   * All seeds, stored redundantly for quick access.
   */
  private final Set<PathEdge<T>> allSeeds = HashSetFactory.make();

  /**
   * The worklist
   */
  private ITabulationWorklist<T> worklist;

  /**
   * A progress monitor. can be null.
   */
  protected final IProgressMonitor progressMonitor;

  /**
   * the path edge currently being processed in the main loop of {@link #forwardTabulateSLRPs()}; <code>null</code> if
   * {@link #forwardTabulateSLRPs()} is not currently running. Note that if we are applying a summary edge in
   * {@link #processExit(PathEdge)}, curPathEdge is modified to be the path edge terminating at the call node in the caller, to
   * match the behavior in {@link #processCall(PathEdge)}.
   */
  private PathEdge<T> curPathEdge;

  /**
   * the summary edge currently being applied in {@link #processCall(PathEdge)} or {@link #processExit(PathEdge)}, or
   * <code>null</code> if summary edges are not currently being processed.
   */
  private PathEdge<T> curSummaryEdge;

  /**
   * @param p a description of the dataflow problem to solve
   * @throws IllegalArgumentException if p is null
   */
  protected TabulationSolver(TabulationProblem<T, P, F> p, IProgressMonitor monitor) {
    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    this.supergraph = p.getSupergraph();
    this.flowFunctionMap = p.getFunctionMap();
    this.problem = p;
    this.progressMonitor = monitor;
  }

  /**
   * Subclasses can override this to plug in a different worklist implementation.
   */
  protected ITabulationWorklist<T> makeWorklist() {
    return new Worklist();
  }

  /**
   * @param p a description of the dataflow problem to solve
   * @throws IllegalArgumentException if p is null
   */
  public static <T, P, F> TabulationSolver<T, P, F> make(TabulationProblem<T, P, F> p) {
    return new TabulationSolver<>(p, null);
  }

  /**
   * Solve the dataflow problem.
   *
   * @return a representation of the result
   */
  public TabulationResult<T, P, F> solve() throws CancelException {

    try {
      initialize();
      forwardTabulateSLRPs();
      Result r = new Result();
      return r;
    } catch (CancelException e) {
      // store a partially-tabulated result in the thrown exception.
      Result r = new Result();
      throw new TabulationCancelException(e, r);
    } catch (CancelRuntimeException e) {
      // store a partially-tabulated result in the thrown exception.
      Result r = new Result();
      throw new TabulationCancelException(e, r);
    }
  }

  /**
   * Start tabulation with the initial seeds.
   */
  protected void initialize() {
    for (PathEdge<T> seed : problem.initialSeeds()) {
      addSeed(seed);
    }
  }

  /**
   * Restart tabulation from a particular path edge. Use with care.
   */
  public void addSeed(PathEdge<T> seed) {
    Set<PathEdge<T>> s = MapUtil.findOrCreateSet(seeds, supergraph.getProcOf(seed.entry));
    s.add(seed);
    allSeeds.add(seed);
    propagate(seed.entry, seed.d1, seed.target, seed.d2);
  }

  /**
   * See POPL 95 paper for this algorithm, Figure 3
   *
   * @throws CancelException
   */
  @SuppressWarnings("unused")
  private void forwardTabulateSLRPs() throws CancelException {
    assert curPathEdge == null : "curPathEdge should not be non-null here";
    if (worklist == null) {
      worklist = makeWorklist();
    }
    while (worklist.size() > 0) {
      MonitorUtil.throwExceptionIfCanceled(progressMonitor);
      if (verbose) {
        performVerboseAction();
      }
      if (PERIODIC_WIPE_SOFT_CACHES) {
        tendToSoftCaches();
      }

      final PathEdge<T> edge = popFromWorkList();
      if (DEBUG_LEVEL > 0) {
        System.err.println("TABULATE " + edge);
      }
      curPathEdge = edge;
      int j = merge(edge.entry, edge.d1, edge.target, edge.d2);
      if (j == -1 && DEBUG_LEVEL > 0) {
        System.err.println("merge -1: DROPPING");
      }
      if (j != -1) {
        if (j != edge.d2) {
          // this means that we don't want to push the edge. instead,
          // we'll push the merged fact. a little tricky, but i think should
          // work.
          if (DEBUG_LEVEL > 0) {
            System.err.println("propagating merged fact " + j);
          }
          propagate(edge.entry, edge.d1, edge.target, j);
        } else {
          if (supergraph.isCall(edge.target)) {
            // [13]
            processCall(edge);
          } else if (supergraph.isExit(edge.target)) {
            // [21]
            processExit(edge);
          } else {
            // [33]
            processNormal(edge);
          }
        }
      }
    }
    curPathEdge = null;
  }

  /**
   * For some reason (either a bug in our code that defeats soft references, or a bad policy in the GC), leaving soft reference
   * caches to clear themselves out doesn't work. Help it out.
   *
   * It's unfortunate that this method exits.
   */
  protected void tendToSoftCaches() {
    wipeCount++;
    if (wipeCount > WIPE_SOFT_CACHE_INTERVAL) {
      wipeCount = 0;
      ReferenceCleanser.clearSoftCaches();
    }
  }

  /**
   *
   */
  protected final void performVerboseAction() {
    verboseCounter++;
    if (verboseCounter % VERBOSE_INTERVAL == 0) {
      System.err.println("Tabulation Solver " + verboseCounter);
      System.err.println("  " + peekFromWorkList());
      if (VERBOSE_TRACE_MEMORY) {
        ReferenceCleanser.clearSoftCaches();
        System.err.println("Analyze leaks..");
        HeapTracer.traceHeap(Collections.singleton(this), true);
        System.err.println("done analyzing leaks");
      }
    }
  }

  /**
   * Handle lines [33-37] of the algorithm
   *
   * @param edge
   */
  @SuppressWarnings("unused")
  private void processNormal(final PathEdge<T> edge) {
    if (DEBUG_LEVEL > 0) {
      System.err.println("process normal: " + edge);
    }
    for (T m : Iterator2Iterable.make(supergraph.getSuccNodes(edge.target))) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("normal successor: " + m);
      }
      IUnaryFlowFunction f = flowFunctionMap.getNormalFlowFunction(edge.target, m);
      IntSet D3 = computeFlow(edge.d2, f);
      if (DEBUG_LEVEL > 0) {
        System.err.println(" reached: " + D3);
      }
      if (D3 != null) {
        D3.foreach(d3 -> {
          newNormalExplodedEdge(edge, m, d3);
          propagate(edge.entry, edge.d1, m, d3);
        });
      }
    }
  }

  /**
   * Handle lines [21 - 32] of the algorithm, propagating information from an exit node.
   *
   * Note that we've changed the way we record summary edges. Summary edges are now associated with a callee (s_p,exit), where the
   * original algorithm used a call, return pair in the caller.
   */
  @SuppressWarnings("unused")
  protected void processExit(final PathEdge<T> edge) {
    if (DEBUG_LEVEL > 0) {
      System.err.println("process exit: " + edge);
    }

    final LocalSummaryEdges summaries = findOrCreateLocalSummaryEdges(supergraph.getProcOf(edge.target));
    int s_p_n = supergraph.getLocalBlockNumber(edge.entry);
    int x = supergraph.getLocalBlockNumber(edge.target);
    if (!summaries.contains(s_p_n, x, edge.d1, edge.d2)) {
      summaries.insertSummaryEdge(s_p_n, x, edge.d1, edge.d2);
    }
    assert curSummaryEdge == null : "curSummaryEdge should be null here";
    curSummaryEdge = edge;

    final CallFlowEdges callFlow = findOrCreateCallFlowEdges(edge.entry);

    // [22] for each c /in callers(p)
    IntSet callFlowSourceNodes = callFlow.getCallFlowSourceNodes(edge.d1);
    if (callFlowSourceNodes != null) {
      for (IntIterator it = callFlowSourceNodes.intIterator(); it.hasNext();) {
        // [23] for each d4 s.t. <c,d4> -> <s_p,d1> occurred earlier
        int globalC = it.next();
        final IntSet D4 = callFlow.getCallFlowSources(globalC, edge.d1);

        // [23] for each d5 s.t. <e_p,d2> -> <returnSite(c),d5> ...
        propagateToReturnSites(edge, supergraph.getNode(globalC), D4);
      }
    }
    curSummaryEdge = null;
  }

  /**
   * Propagate information for an "exit" edge to the appropriate return sites
   *
   * [23] for each d5 s.t. {@literal <s_p,d2> -> <returnSite(c),d5>} ..
   *
   * @param edge the edge being processed
   * @param succ numbers of the nodes that are successors of edge.n (the return block in the callee) in the call graph.
   * @param c a call site of edge.s_p
   * @param D4 set of d1 s.t. {@literal <c, d1> -> <edge.s_p, edge.d2>} was recorded as call flow
   */
  @SuppressWarnings("unused")
  private void propagateToReturnSites(final PathEdge<T> edge, final T c, final IntSet D4) {
    P proc = supergraph.getProcOf(c);
    final T[] entries = supergraph.getEntriesForProcedure(proc);

    // we iterate over each potential return site;
    // we might have multiple return sites due to exceptions
    // note that we might have different summary edges for each
    // potential return site, and different flow functions from this
    // exit block to each return site.
    for (T retSite : Iterator2Iterable.make(supergraph.getReturnSites(c, supergraph.getProcOf(edge.target)))) {
      if (DEBUG_LEVEL > 1) {
        System.err.println("candidate return site: " + retSite + " " + supergraph.getNumber(retSite));
      }
      // note: since we might have multiple exit nodes for the callee, (to handle exceptional returns)
      // not every return site might be valid for this exit node (edge.n).
      // so, we'll filter the logic by checking that we only process reachable return sites.
      // the supergraph carries the information regarding the legal successors
      // of the exit node
      if (!supergraph.hasEdge(edge.target, retSite)) {
        continue;
      }
      if (DEBUG_LEVEL > 1) {
        System.err.println("feasible return site: " + retSite);
      }
      final IFlowFunction retf = flowFunctionMap.getReturnFlowFunction(c, edge.target, retSite);
      if (retf instanceof IBinaryReturnFlowFunction) {
        propagateToReturnSiteWithBinaryFlowFunction(edge, c, D4, entries, retSite, retf);
      } else {
        final IntSet D5 = computeFlow(edge.d2, (IUnaryFlowFunction) retf);
        if (DEBUG_LEVEL > 1) {
          System.err.println("D4" + D4);
          System.err.println("D5 " + D5);
        }
        IntSetAction action = d4 -> propToReturnSite(c, entries, retSite, d4, D5, edge);
        D4.foreach(action);
      }
    }
  }

  /**
   * Propagate information for an "exit" edge to a caller return site
   *
   * [23] for each d5 s.t. {@literal <s_p,d2> -> <returnSite(c),d5>} ..
   *
   * @param edge the edge being processed
   * @param c a call site of edge.s_p
   * @param D4 set of d1 s.t. {@literal <c, d1> -> <edge.s_p, edge.d2>} was recorded as call flow
   * @param entries the blocks in the supergraph that are entries for the procedure of c
   * @param retSite the return site being propagated to
   * @param retf the flow function
   */
  private void propagateToReturnSiteWithBinaryFlowFunction(final PathEdge<T> edge, final T c, final IntSet D4, final T[] entries,
      final T retSite, final IFlowFunction retf) {
    D4.foreach(d4 -> {
      final IntSet D5 = computeBinaryFlow(d4, edge.d2, (IBinaryReturnFlowFunction) retf);
      propToReturnSite(c, entries, retSite, d4, D5, edge);
    });
  }

  /**
   * Propagate information to a particular return site.
   *
   * @param c the corresponding call site
   * @param entries entry nodes in the caller
   * @param retSite the return site
   * @param d4 a fact s.t. {@literal <c, d4> -> <callee, d2>} was
   *          recorded as call flow and {@literal <callee, d2>} is the
   *          source of the summary edge being applied
   * @param D5 facts to propagate to return site
   * @param edge the path edge ending at the exit site of the callee
   */
  private void propToReturnSite(final T c, final T[] entries, final T retSite, final int d4, final IntSet D5, final PathEdge<T> edge) {
    if (D5 != null) {
      D5.foreach(new IntSetAction() {
        @SuppressWarnings("unused")
        @Override
        public void act(final int d5) {
          // [26 - 28]
          // note that we've modified the algorithm here to account
          // for potential
          // multiple entry nodes. Instead of propagating the new
          // summary edge
          // with respect to one s_profOf(c), we have to propagate
          // for each
          // potential entry node s_p /in s_procof(c)
          for (final T s_p : entries) {
            if (DEBUG_LEVEL > 1) {
              System.err.println(" do entry " + s_p);
            }
            IntSet D3 = getInversePathEdges(s_p, c, d4);
            if (DEBUG_LEVEL > 1) {
              System.err.println("D3" + D3);
            }
            if (D3 != null) {
              D3.foreach(d3 -> {
                // set curPathEdge to be consistent with its setting in processCall() when applying a summary edge
                curPathEdge = PathEdge.createPathEdge(s_p, d3, c, d4);
                newSummaryEdge(curPathEdge, edge, retSite, d5);
                propagate(s_p, d3, retSite, d5);
              });
            }
          }
        }
      });
    }
  }

  /**
   * @param s_p
   * @param n
   * @param d2 note that s_p must be an entry for procof(n)
   * @return set of d1 s.t. {@literal <s_p, d1> -> <n, d2>} is a path edge, or null if none found
   */
  protected IntSet getInversePathEdges(T s_p, T n, int d2) {
    int number = supergraph.getLocalBlockNumber(n);
    LocalPathEdges lp = pathEdges.get(s_p);
    if (lp == null) {
      return null;
    }
    return lp.getInverse(number, d2);
  }

  /**
   * Handle lines [14 - 19] of the algorithm, propagating information into and across a call site.
   */
  @SuppressWarnings("unused")
  protected void processCall(final PathEdge<T> edge) {
    if (DEBUG_LEVEL > 0) {
      System.err.println("process call: " + edge);
    }

    // c:= number of the call node
    final int c = supergraph.getNumber(edge.target);

    Collection<T> allReturnSites = HashSetFactory.make();
    // populate allReturnSites with return sites for missing calls.
    for (T retSite : Iterator2Iterable.make(supergraph.getReturnSites(edge.target, null))) {
      allReturnSites.add(retSite);
    }
    // [14 - 16]
    boolean hasCallee = false;
    for (T callee : Iterator2Iterable.make(supergraph.getCalledNodes(edge.target))) {
      hasCallee = true;
      processParticularCallee(edge, c, allReturnSites, callee);
    }
    // special logic: in backwards problems, a "call" node can have
    // "normal" successors as well. deal with these.
    for (T m : Iterator2Iterable.make(supergraph.getNormalSuccessors(edge.target))) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("normal successor: " + m);
      }
      IUnaryFlowFunction f = flowFunctionMap.getNormalFlowFunction(edge.target, m);
      IntSet D3 = computeFlow(edge.d2, f);
      if (DEBUG_LEVEL > 0) {
        System.err.println("normal successor reached: " + D3);
      }
      if (D3 != null) {
        D3.foreach(d3 -> {
          newNormalExplodedEdge(edge, m, d3);
          propagate(edge.entry, edge.d1, m, d3);
        });
      }
    }

    // [17 - 19]
    // we modify this to handle each return site individually
    for (final T returnSite : allReturnSites) {
      if (DEBUG_LEVEL > 0) {
        System.err.println(" process return site: " + returnSite);
      }
      IUnaryFlowFunction f = null;
      if (hasCallee) {
        f = flowFunctionMap.getCallToReturnFlowFunction(edge.target, returnSite);
      } else {
        f = flowFunctionMap.getCallNoneToReturnFlowFunction(edge.target, returnSite);
      }
      IntSet reached = computeFlow(edge.d2, f);
      if (DEBUG_LEVEL > 0) {
        System.err.println("reached: " + reached);
      }
      if (reached != null) {
        reached.foreach(x -> {
          assert x >= 0;
          assert edge.d1 >= 0;
          newNormalExplodedEdge(edge, returnSite, x);
          propagate(edge.entry, edge.d1, returnSite, x);
        });
      }
    }
  }

  /**
   * handle a particular callee for some call node.
   *
   * @param edge the path edge being processed
   * @param callNodeNum the number of the call node in the supergraph
   * @param allReturnSites a set collecting return sites for the call. This set is mutated with the return sites for this callee.
   * @param calleeEntry the entry node of the callee in question
   */
  @SuppressWarnings("unused")
  protected void processParticularCallee(final PathEdge<T> edge, final int callNodeNum, Collection<T> allReturnSites, final T calleeEntry) {
    if (DEBUG_LEVEL > 0) {
      System.err.println(" process callee: " + calleeEntry);
    }
    // reached := {d1} that reach the callee
    MutableSparseIntSet reached = MutableSparseIntSet.makeEmpty();
    final Collection<T> returnSitesForCallee = Iterator2Collection.toSet(supergraph.getReturnSites(edge.target, supergraph
        .getProcOf(calleeEntry)));
    allReturnSites.addAll(returnSitesForCallee);
    // we modify this to handle each return site individually. Some types of problems
    // compute different flow functions for each return site.
    for (final T returnSite : returnSitesForCallee) {
      IUnaryFlowFunction f = flowFunctionMap.getCallFlowFunction(edge.target, calleeEntry, returnSite);
      IntSet r = computeFlow(edge.d2, f);
      if (r != null) {
        reached.addAll(r);
      }
    }
    // in some problems, we also want to consider flow into a callee that can never flow out
    // via a return. in this case, the return site is null.
    IUnaryFlowFunction f = flowFunctionMap.getCallFlowFunction(edge.target, calleeEntry, null);
    IntSet r = computeFlow(edge.d2, f);
    if (r != null) {
      reached.addAll(r);
    }
    if (DEBUG_LEVEL > 0) {
      System.err.println(" reached: " + reached);
    }
    if (reached != null) {
      final LocalSummaryEdges summaries = summaryEdges.get(supergraph.getProcOf(calleeEntry));
      final CallFlowEdges callFlow = findOrCreateCallFlowEdges(calleeEntry);
      final int s_p_num = supergraph.getLocalBlockNumber(calleeEntry);

      reached.foreach(d1 -> {
        // we get reuse if we _don't_ propagate a new fact to the callee entry
        final boolean gotReuse = !propagate(calleeEntry, d1, calleeEntry, d1);
        recordCall(edge.target, calleeEntry, d1, gotReuse);
        newCallExplodedEdge(edge, calleeEntry, d1);
        // cache the fact that we've flowed <c, d2> -> <callee, d1> by a
        // call flow
        callFlow.addCallEdge(callNodeNum, edge.d2, d1);
        // handle summary edges now as well. this is different from the PoPL
        // 95 paper.
        if (summaries != null) {
          // for each exit from the callee
          P p = supergraph.getProcOf(calleeEntry);
          T[] exits = supergraph.getExitsForProcedure(p);
          for (final T exit : exits) {
            if (DEBUG_LEVEL > 0) {
              assert supergraph.containsNode(exit);
            }
            int x_num = supergraph.getLocalBlockNumber(exit);
            // reachedBySummary := {d2} s.t. <callee,d1> -> <exit,d2>
            // was recorded as a summary edge
            IntSet reachedBySummary = summaries.getSummaryEdges(s_p_num, x_num, d1);
            if (reachedBySummary != null) {
              for (final T returnSite : returnSitesForCallee) {
                // if "exit" is a valid exit from the callee to the return
                // site being processed
                if (supergraph.hasEdge(exit, returnSite)) {
                  final IFlowFunction retf = flowFunctionMap.getReturnFlowFunction(edge.target, exit, returnSite);
                  reachedBySummary.foreach(d2 -> {
                    assert curSummaryEdge == null : "curSummaryEdge should be null here";
                    curSummaryEdge = PathEdge.createPathEdge(calleeEntry, d1, exit, d2);
                    if (retf instanceof IBinaryReturnFlowFunction) {
                      final IntSet D51 = computeBinaryFlow(edge.d2, d2, (IBinaryReturnFlowFunction) retf);
                      if (D51 != null) {
                        D51.foreach(d5 -> {
                          newSummaryEdge(edge, curSummaryEdge, returnSite, d5);
                          propagate(edge.entry, edge.d1, returnSite, d5);
                        });
                      }
                    } else {
                      final IntSet D52 = computeFlow(d2, (IUnaryFlowFunction) retf);
                      if (D52 != null) {
                        D52.foreach(d5 -> {
                          newSummaryEdge(edge, curSummaryEdge, returnSite, d5);
                          propagate(edge.entry, edge.d1, returnSite, d5);
                        });
                      }
                    }
                    curSummaryEdge = null;
                  });
                }
              }
            }
          }
        }
      });
    }
  }

  /**
   * invoked when a callee is processed with a particular entry fact
   *
   * @param callNode
   * @param callee
   * @param d1 the entry fact
   * @param gotReuse whether existing summary edges were applied
   */
  protected void recordCall(T callNode, T callee, int d1, boolean gotReuse) {
  }

  /**
   * @return f(call_d, exit_d);
   *
   */
  @SuppressWarnings("unused")
  protected IntSet computeBinaryFlow(int call_d, int exit_d, IBinaryReturnFlowFunction f) {
    if (DEBUG_LEVEL > 0) {
      System.err.println("got binary flow function " + f);
    }
    IntSet result = f.getTargets(call_d, exit_d);
    return result;
  }

  /**
   * @return f(d1)
   *
   */
  @SuppressWarnings("unused")
  protected IntSet computeFlow(int d1, IUnaryFlowFunction f) {
    if (DEBUG_LEVEL > 0) {
      System.err.println("got flow function " + f);
    }
    IntSet result = f.getTargets(d1);

    if (result == null) {
      return null;
    } else {
      return result;
    }
  }

  /**
   * @return f^{-1}(d2)
   */
  protected IntSet computeInverseFlow(int d2, IReversibleFlowFunction f) {
    return f.getSources(d2);
  }

  protected PathEdge<T> popFromWorkList() {
    assert worklist != null;
    return worklist.take();
  }

  private PathEdge<T> peekFromWorkList() {
    // horrible. don't use in performance-critical
    assert worklist != null;
    PathEdge<T> result = worklist.take();
    worklist.insert(result);
    return result;
  }

  /**
   * Propagate the fact &lt;s_p,i&gt; -&gt; &lt;n, j&gt; has arisen as a path edge. Returns &lt;code&gt;true&lt;/code&gt; iff the path edge was not previously
   * observed.
   *
   * @param s_p entry block
   * @param i dataflow fact on entry
   * @param n reached block
   * @param j dataflow fact reached
   */
  @SuppressWarnings("unused")
  protected boolean propagate(T s_p, int i, T n, int j) {
    int number = supergraph.getLocalBlockNumber(n);
    if (number < 0) {
      System.err.println("BOOM " + n);
      supergraph.getLocalBlockNumber(n);
    }
    assert number >= 0;

    LocalPathEdges pLocal = findOrCreateLocalPathEdges(s_p);

    assert j >= 0;

    if (!pLocal.contains(i, number, j)) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("propagate " + s_p + "  " + i + " " + number + " " + j);
      }
      pLocal.addPathEdge(i, number, j);
      addToWorkList(s_p, i, n, j);
      return true;
    }
    return false;
  }

  public LocalPathEdges getLocalPathEdges(T s_p) {
    return pathEdges.get(s_p);
  }

  /**
   * Merging: suppose we're doing propagate &lt;s_p,i&gt; -&gt; &lt;n,j&gt; but we already have path edges &lt;s_p,i&gt; -&gt; &lt;n, x&gt;, &lt;s_p,i&gt; -&gt; &lt;n,y&gt;, and
   * &lt;s_p,i&gt; -&gt;&lt;n, z&gt;.
   *
   * let \alpha be the merge function. then instead of &lt;s_p,i&gt; -&gt; &lt;n,j&gt;, we propagate &lt;s_p,i&gt; -&gt; &lt;n, \alpha(j,x,y,z) &gt; !!!
   *
   * return -1 if no fact should be propagated
   */
  private int merge(T s_p, int i, T n, int j) {
    assert j >= 0;
    IMergeFunction alpha = problem.getMergeFunction();
    if (alpha != null) {
      LocalPathEdges lp = pathEdges.get(s_p);
      IntSet preExistFacts = lp.getReachable(supergraph.getLocalBlockNumber(n), i);
      if (preExistFacts == null) {
        return j;
      } else {
        int size = preExistFacts.size();
        if ((size == 0) || ((size == 1) && preExistFacts.contains(j))) {
          return j;
        } else {
          int result = alpha.merge(preExistFacts, j);
          return result;
        }
      }
    } else {
      return j;
    }
  }

  @SuppressWarnings("unused")
  protected void addToWorkList(T s_p, int i, T n, int j) {
    if (worklist == null) {
      worklist = makeWorklist();
    }
    worklist.insert(PathEdge.createPathEdge(s_p, i, n, j));
    if (DEBUG_LEVEL >= 3) {
      System.err.println("WORKLIST: " + worklist);
    }
  }

  protected LocalPathEdges findOrCreateLocalPathEdges(T s_p) {
    LocalPathEdges result = pathEdges.get(s_p);
    if (result == null) {
      result = makeLocalPathEdges();
      pathEdges.put(s_p, result);
    }
    return result;
  }

  private LocalPathEdges makeLocalPathEdges() {
    return problem.getMergeFunction() == null ? new LocalPathEdges(false) : new LocalPathEdges(true);
  }

  protected LocalSummaryEdges findOrCreateLocalSummaryEdges(P proc) {
    LocalSummaryEdges result = summaryEdges.get(proc);
    if (result == null) {
      result = new LocalSummaryEdges();
      summaryEdges.put(proc, result);
    }
    return result;
  }

  protected CallFlowEdges findOrCreateCallFlowEdges(T s_p) {
    CallFlowEdges result = callFlowEdges.get(s_p);
    if (result == null) {
      result = new CallFlowEdges();
      callFlowEdges.put(s_p, result);
    }
    return result;
  }

  /**
   * get the bitvector of facts that hold at the entry to a given node
   *
   * @return IntSet representing the bitvector
   */
  public IntSet getResult(T node) {
    P proc = supergraph.getProcOf(node);
    int n = supergraph.getLocalBlockNumber(node);
    T[] entries = supergraph.getEntriesForProcedure(proc);
    MutableIntSet result = MutableSparseIntSet.makeEmpty();

    Set<T> allEntries = HashSetFactory.make(Arrays.asList(entries));
    Set<PathEdge<T>> pSeeds = seeds.get(proc);
    if (pSeeds != null) {
    	for (PathEdge<T> seed : pSeeds) {
    		allEntries.add(seed.entry);
    	}
    }

    for (T entry : allEntries){
    	LocalPathEdges lp = pathEdges.get(entry);
    	if (lp != null) {
    		result.addAll(lp.getReachable(n));
    	}
    }

    return result;
  }

  public class Result implements TabulationResult<T, P, F> {

    /**
     * get the bitvector of facts that hold at the entry to a given node
     *
     * @return IntSet representing the bitvector
     */
    @Override
    public IntSet getResult(T node) {
      return TabulationSolver.this.getResult(node);
    }

    @Override
    public String toString() {

      StringBuffer result = new StringBuffer();
      TreeMap<Object, TreeSet<T>> map = new TreeMap<>(ToStringComparator.instance());

      Comparator<Object> c = (o1, o2) -> {
        if (!(o1 instanceof IBasicBlock)) {
          return -1;
        }
        IBasicBlock bb1 = (IBasicBlock) o1;
        IBasicBlock bb2 = (IBasicBlock) o2;
        return bb1.getNumber() - bb2.getNumber();
      };
      for (T n : supergraph) {
        P proc = supergraph.getProcOf(n);
        TreeSet<T> s = map.get(proc);
        if (s == null) {
          s = new TreeSet<>(c);
          map.put(proc, s);
        }
        s.add(n);
      }

      for (Entry<Object, TreeSet<T>> e : map.entrySet()) {
        Set<T> s = e.getValue();
        for (T o : s) {
          result.append(o + " : " + getResult(o) + "\n");
        }
      }
      return result.toString();
    }

    /*
     * @see com.ibm.wala.dataflow.IFDS.TabulationResult#getProblem()
     */
    @Override
    public TabulationProblem<T, P, F> getProblem() {
      return problem;
    }

    /*
     * @see com.ibm.wala.dataflow.IFDS.TabulationResult#getSupergraphNodesReached()
     */
    @Override
    public Collection<T> getSupergraphNodesReached() {
      Collection<T> result = HashSetFactory.make();
      for (Entry<T, LocalPathEdges> e : pathEdges.entrySet()) {
        T key = e.getKey();
        P proc = supergraph.getProcOf(key);
        IntSet reached = e.getValue().getReachedNodeNumbers();
        for (IntIterator ii = reached.intIterator(); ii.hasNext();) {
          result.add(supergraph.getLocalBlock(proc, ii.next()));
        }
      }

      return result;
    }

    /**
     * @param n1
     * @param d1
     * @param n2
     * @return set of d2 s.t. (n1,d1) -&gt; (n2,d2) is recorded as a summary edge, or null if none found
     */
    @Override
    public IntSet getSummaryTargets(T n1, int d1, T n2) {
      LocalSummaryEdges summaries = summaryEdges.get(supergraph.getProcOf(n1));
      if (summaries == null) {
        return null;
      }
      int num1 = supergraph.getLocalBlockNumber(n1);
      int num2 = supergraph.getLocalBlockNumber(n2);
      return summaries.getSummaryEdges(num1, num2, d1);
    }

    @Override
    public Collection<PathEdge<T>> getSeeds() {
      return TabulationSolver.this.getSeeds();
    }
  }

  /**
   * @return Returns the supergraph.
   */
  public ISupergraph<T, P> getSupergraph() {
    return supergraph;
  }

  protected class Worklist extends Heap<PathEdge<T>> implements ITabulationWorklist<T> {

    Worklist() {
      super(100);
    }

    @Override
    protected boolean compareElements(PathEdge<T> p1, PathEdge<T> p2) {
      return problem.getDomain().hasPriorityOver(p1, p2);
    }

  }

  /**
   * @return set of d1 s.t. (n1,d1) -&gt; (n2,d2) is recorded as a summary edge, or null if none found
   * @throws UnsupportedOperationException unconditionally
   */
  @SuppressWarnings("unused")
  public IntSet getSummarySources(T n2, int d2, T n1) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("not currently supported.  be careful");
    // LocalSummaryEdges summaries = summaryEdges.get(supergraph.getProcOf(n1));
    // if (summaries == null) {
    // return null;
    // }
    // int num1 = supergraph.getLocalBlockNumber(n1);
    // int num2 = supergraph.getLocalBlockNumber(n2);
    // return summaries.getInvertedSummaryEdgesForTarget(num1, num2, d2);
  }

  public TabulationProblem<T, P, F> getProblem() {
    return problem;
  }

  public Collection<PathEdge<T>> getSeeds() {
    return Collections.unmodifiableCollection(allSeeds);
  }

  public IProgressMonitor getProgressMonitor() {
    return progressMonitor;
  }

  protected PathEdge<T> getCurPathEdge() {
    return curPathEdge;
  }

  protected PathEdge<T> getCurSummaryEdge() {
    return curSummaryEdge;
  }

  /**
   * Indicates that due to a path edge &lt;s_p, d1&gt; -&gt; &lt;n, d2&gt; (the 'edge'
   * parameter) and a normal flow function application, a new path edge &lt;s_p,
   * d1&gt; -&gt; &lt;m, d3&gt; was created. To be overridden in subclasses. We also use
   * this function to record call-to-return flow.
   *
   */
  @SuppressWarnings("unused")
  protected void newNormalExplodedEdge(PathEdge<T> edge, T m, int d3) {

  }

  /**
   * Indicates that due to a path edge 'edge' &lt;s_p, d1&gt; -&gt; &lt;n, d2&gt; and
   * application of a call flow function, a new path edge &lt;calleeEntry, d3&gt; -&gt;
   * &lt;calleeEntry, d3&gt; was created. To be overridden in subclasses.
   *
   */
  @SuppressWarnings("unused")
  protected void newCallExplodedEdge(PathEdge<T> edge, T calleeEntry, int d3) {

  }

  /**
   * Combines [25] and [26-28]. In the caller we have a path edge
   * 'edgeToCallSite' &lt;s_c, d3&gt; -&gt; &lt;c, d4&gt;, where c is the call site. In the
   * callee, we have path edge 'calleeSummaryEdge' &lt;s_p, d1&gt; -&gt; &lt;e_p, d2&gt;. Of
   * course, there is a call edge &lt;c, d4&gt; -&gt; &lt;s_p, d1&gt;. Finally, we have a
   * return edge &lt;e_p, d2&gt; -&gt; &lt;returnSite, d5&gt;.
   */
  @SuppressWarnings("unused")
  protected void newSummaryEdge(PathEdge<T> edgeToCallSite, PathEdge<T> calleeSummaryEdge, T returnSite, int d5) {

  }


}
