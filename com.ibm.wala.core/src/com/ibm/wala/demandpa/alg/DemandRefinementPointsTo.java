/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.demandpa.alg;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.demandpa.alg.refinepolicy.NeverRefineCGPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.NeverRefineFieldsPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.RefinementPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.RefinementPolicyFactory;
import com.ibm.wala.demandpa.alg.refinepolicy.SinglePassRefinementPolicy;
import com.ibm.wala.demandpa.alg.statemachine.StateMachine;
import com.ibm.wala.demandpa.alg.statemachine.StateMachine.State;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.alg.statemachine.StatesMergedException;
import com.ibm.wala.demandpa.flowgraph.AbstractFlowGraph;
import com.ibm.wala.demandpa.flowgraph.AbstractFlowLabelVisitor;
import com.ibm.wala.demandpa.flowgraph.AssignBarLabel;
import com.ibm.wala.demandpa.flowgraph.AssignGlobalBarLabel;
import com.ibm.wala.demandpa.flowgraph.AssignGlobalLabel;
import com.ibm.wala.demandpa.flowgraph.AssignLabel;
import com.ibm.wala.demandpa.flowgraph.DemandPointerFlowGraph;
import com.ibm.wala.demandpa.flowgraph.GetFieldLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowGraph;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.demandpa.flowgraph.IFlowLabelWithFilter;
import com.ibm.wala.demandpa.flowgraph.MatchBarLabel;
import com.ibm.wala.demandpa.flowgraph.MatchLabel;
import com.ibm.wala.demandpa.flowgraph.NewLabel;
import com.ibm.wala.demandpa.flowgraph.ParamBarLabel;
import com.ibm.wala.demandpa.flowgraph.ParamLabel;
import com.ibm.wala.demandpa.flowgraph.PutFieldLabel;
import com.ibm.wala.demandpa.flowgraph.ReturnBarLabel;
import com.ibm.wala.demandpa.flowgraph.ReturnLabel;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.demandpa.util.MemoryAccess;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.PointerParamValueNumIterator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractLocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.MultipleClassesFilter;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.SingleClassFilter;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.SingleInstanceFilter;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.TypeFilter;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ExceptionReturnValueKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.collections.ArraySet;
import com.ibm.wala.util.collections.ArraySetMultiMap;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.HashSetMultiMap;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.MultiMap;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.collections.Util;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableIntSetFactory;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Demand-driven refinement-based points-to analysis.
 */
public class DemandRefinementPointsTo extends AbstractDemandPointsTo {

  private static final boolean DEBUG = false;

  private static final boolean DEBUG_TOPLEVEL = false;

  private static final boolean PARANOID = false;

  private static final boolean MEASURE_MEMORY_USAGE = false;

  protected final IFlowGraph g;

  private StateMachineFactory<IFlowLabel> stateMachineFactory;

  /**
   * the state machine for additional filtering of paths
   */
  private StateMachine<IFlowLabel> stateMachine;

  protected RefinementPolicy refinementPolicy;

  private RefinementPolicyFactory refinementPolicyFactory;

  public RefinementPolicy getRefinementPolicy() {
    return refinementPolicy;
  }

  private DemandRefinementPointsTo(CallGraph cg, ThisFilteringHeapModel model, MemoryAccessMap fam, IClassHierarchy cha,
      AnalysisOptions options, StateMachineFactory<IFlowLabel> stateMachineFactory, IFlowGraph flowGraph) {
    super(cg, model, fam, cha, options);
    this.stateMachineFactory = stateMachineFactory;
    g = flowGraph;
    this.refinementPolicyFactory = new SinglePassRefinementPolicy.Factory(new NeverRefineFieldsPolicy(), new NeverRefineCGPolicy());
    sanityCheckCG();
  }

  private void sanityCheckCG() {
    if (PARANOID) {
      for (CGNode callee : cg) {
        for (CGNode caller : Iterator2Iterable.make(cg.getPredNodes(callee))) {
          for (CallSiteReference site : Iterator2Iterable.make(cg.getPossibleSites(caller, callee))) {
            try {
              caller.getIR().getCalls(site);
            } catch (IllegalArgumentException e) {
              System.err.println(caller + " is pred of " + callee);
              System.err.println("no calls at site " + site);
              System.err.println(caller.getIR());
              if (caller.getMethod() instanceof ShrikeBTMethod) {
                try {
                  IInstruction[] instructions = ((ShrikeBTMethod) caller.getMethod()).getInstructions();
                  for (int i = 0; i < instructions.length; i++) {
                    System.err.println(i + ": " + instructions[i]);
                  }
                } catch (InvalidClassFileException e1) {
                  // TODO Auto-generated catch block
                  e1.printStackTrace();
                }
              }
              Assertions.UNREACHABLE();
            }
          }
        }
      }
    }
  }

  /**
   * Possible results of a query.
   * 
   * @see DemandRefinementPointsTo#getPointsTo(PointerKey, Predicate)
   * @author manu
   * 
   */
  public static enum PointsToResult {
    /**
     * The points-to set result satisfies the supplied {@link Predicate}
     */
    SUCCESS,
    /**
     * The {@link RefinementPolicy} indicated that no more refinement was possible, <em>and</em> on at least one refinement pass the
     * budget was not exhausted
     */
    NOMOREREFINE,
    /**
     * The budget specified in the {@link RefinementPolicy} was exceeded on all refinement passes
     */
    BUDGETEXCEEDED
  }

  /**
   * re-initialize state for a new query
   */
  protected void startNewQuery() {
    // re-init the refinement policy
    refinementPolicy = refinementPolicyFactory.make();
    // re-init the state machine
    stateMachine = stateMachineFactory.make();
  }

  /**
   * compute a points-to set for a pointer key, aiming to satisfy some predicate
   * 
   * @param pk the pointer key
   * @param ikeyPred the desired predicate that each instance key in the points-to set should ideally satisfy
   * @return a pair consisting of (1) a {@link PointsToResult} indicating whether a points-to set satisfying the predicate was
   *         computed, and (2) the last computed points-to set for the variable (possibly <code>null</code> if no points-to set
   *         could be computed in the budget)
   * @throws IllegalArgumentException if <code>pk</code> is not a {@link LocalPointerKey}; to eventually be fixed
   */
  public Pair<PointsToResult, Collection<InstanceKey>> getPointsTo(PointerKey pk, Predicate<InstanceKey> ikeyPred)
      throws IllegalArgumentException {
    Pair<PointsToResult, Collection<InstanceKeyAndState>> p = getPointsToWithStates(pk, ikeyPred);
    final Collection<InstanceKeyAndState> p2SetWithStates = p.snd;
    Collection<InstanceKey> finalP2Set = p2SetWithStates != null ? removeStates(p2SetWithStates) : Collections.<InstanceKey>emptySet();
    return Pair.make(p.fst, finalP2Set);
  }

  /**
   * @param pk
   * @param ikeyPred
   */
  private Pair<PointsToResult, Collection<InstanceKeyAndState>> getPointsToWithStates(PointerKey pk, Predicate<InstanceKey> ikeyPred) {
    if (!(pk instanceof com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)) {
      throw new IllegalArgumentException("only locals for now");
    }
    LocalPointerKey queriedPk = (LocalPointerKey) pk;
    if (DEBUG) {
      System.err.println("answering query for " + pk);
    }
    startNewQuery();
    Pair<PointsToResult, Collection<InstanceKeyAndState>> p = outerRefinementLoop(new PointerKeyAndState(queriedPk, stateMachine
        .getStartState()), ikeyPred);
    return p;
  }

  /**
   * Unwrap a Collection of WithState<T> objects, returning a Collection containing the wrapped objects
   */
  private static <T> Collection<T> removeStates(final Collection<? extends WithState<T>> p2SetWithStates) {
    if (p2SetWithStates == null) {
      throw new IllegalArgumentException("p2SetWithStates == null");
    }
    Collection<T> finalP2Set = Iterator2Collection.toSet(new MapIterator<WithState<T>, T>(p2SetWithStates.iterator(),
        WithState::getWrapped));
    return finalP2Set;
  }

  /**
   * create a demand points-to analysis runner
   * 
   * @param cg the underlying call graph for the analysis
   * @param model the heap model to be used for the analysis
   * @param mam indicates what code reads or writes each field
   * @param cha
   * @param options
   * @param stateMachineFactory factory for state machines to track additional properties like calling context
   */
  public static DemandRefinementPointsTo makeWithDefaultFlowGraph(CallGraph cg, HeapModel model, MemoryAccessMap mam,
      IClassHierarchy cha, AnalysisOptions options, StateMachineFactory<IFlowLabel> stateMachineFactory) {
    final ThisFilteringHeapModel thisFilteringHeapModel = new ThisFilteringHeapModel(model, cha);
    return new DemandRefinementPointsTo(cg, thisFilteringHeapModel, mam, cha, options, stateMachineFactory,
        new DemandPointerFlowGraph(cg, thisFilteringHeapModel, mam, cha));
  }

  private Pair<PointsToResult, Collection<InstanceKeyAndState>> outerRefinementLoop(PointerKeyAndState queried,
      Predicate<InstanceKey> ikeyPred) {
    Collection<InstanceKeyAndState> lastP2Set = null;
    boolean succeeded = false;
    int numPasses = refinementPolicy.getNumPasses();
    int passNum = 0;
    for (; passNum < numPasses; passNum++) {
      setNumNodesTraversed(0);
      setTraversalBudget(refinementPolicy.getBudgetForPass(passNum));
      Collection<InstanceKeyAndState> curP2Set = null;
      PointsToComputer computer = null;
      boolean completedPassInBudget = false;
      try {
        while (true) {
          try {
            computer = new PointsToComputer(queried);
            computer.compute();
            curP2Set = computer.getComputedP2Set(queried);
            // System.err.println("completed pass");
            if (DEBUG) {
              System.err.println("traversed " + getNumNodesTraversed() + " nodes");
              System.err.println("POINTS-TO SET " + curP2Set);
            }
            completedPassInBudget = true;
            break;
          } catch (StatesMergedException e) {
            if (DEBUG) {
              System.err.println("restarting...");
            }
          }
        }
      } catch (BudgetExceededException e) {

      }
      if (curP2Set != null) {
        if (lastP2Set == null) {
          lastP2Set = curP2Set;
        } else if (lastP2Set.size() > curP2Set.size()) {
          // got a more precise set
          assert removeStates(lastP2Set).containsAll(removeStates(curP2Set));
          lastP2Set = curP2Set;
        } else {
          // new set size is >= lastP2Set, so don't update
          assert removeStates(curP2Set).containsAll(removeStates(lastP2Set));
        }
        if (curP2Set.isEmpty() || passesPred(curP2Set, ikeyPred)) {
          // we did it!
          // if (curP2Set.isEmpty()) {
          // System.err.println("EMPTY PTO SET");
          // }
          succeeded = true;
          break;
        } else if (completedPassInBudget) {
        }
      }
      // if we get here, means either budget for pass was exceeded,
      // or points-to set wasn't good enough
      // so, start new pass, if more refinement to do
      if (!refinementPolicy.nextPass()) {
        break;
      }
    }
    PointsToResult result = null;
    if (succeeded) {
      result = PointsToResult.SUCCESS;
    } else if (passNum == numPasses) {
      // we ran all the passes without succeeding and
      // without the refinement policy giving up
      result = PointsToResult.BUDGETEXCEEDED;
    } else {
      if (lastP2Set != null) {
        result = PointsToResult.NOMOREREFINE;
      } else {
        // we stopped before the maximum number of passes, but we never
        // actually finished a pass, so we count this as BUDGETEXCEEDED
        result = PointsToResult.BUDGETEXCEEDED;
      }
    }
    return Pair.make(result, lastP2Set);
  }

  /**
   * to measure memory usage
   */
  public long lastQueryMemoryUse;

  /**
   * check if the points-to set of a variable passes some predicate, without necessarily computing the whole points-to set
   * 
   * @param pk the pointer key
   * @param ikeyPred the desired predicate that each instance key in the points-to set should ideally satisfy
   * @param pa a pre-computed points-to analysis
   * @return a {@link PointsToResult} indicating whether a points-to set satisfying the predicate was computed
   * @throws IllegalArgumentException if <code>pk</code> is not a {@link LocalPointerKey}; to eventually be fixed
   */
  public PointsToResult pointsToPassesPred(PointerKey pk, Predicate<InstanceKey> ikeyPred, PointerAnalysis<InstanceKey> pa)
      throws IllegalArgumentException {
    if (!(pk instanceof com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)) {
      throw new IllegalArgumentException("only locals for now");
    }
    LocalPointerKey queriedPk = (LocalPointerKey) pk;
    if (DEBUG) {
      System.err.println("answering query for " + pk);
    }
    boolean succeeded = false;
    startNewQuery();
    int numPasses = refinementPolicy.getNumPasses();
    int passNum = 0;
    boolean completedSomePass = false;
    if (MEASURE_MEMORY_USAGE) {
      lastQueryMemoryUse = -1;
    }
    for (; passNum < numPasses; passNum++) {
      setNumNodesTraversed(0);
      setTraversalBudget(refinementPolicy.getBudgetForPass(passNum));
      boolean completedPassInBudget = false;
      boolean passed = false;
      long initialMemory = 0;
      try {
        while (true) {
          try {
            if (MEASURE_MEMORY_USAGE) {
              initialMemory = Util.getUsedMemory();
            }
            PointsToComputer computer = new PointsToComputer(queriedPk);
            passed = doTopLevelTraversal(queriedPk, ikeyPred, computer, pa);
            // System.err.println("completed pass");
            if (DEBUG) {
              System.err.println("traversed " + getNumNodesTraversed() + " nodes");
            }
            completedPassInBudget = true;
            completedSomePass = true;
            break;
          } catch (StatesMergedException e) {
            if (DEBUG) {
              System.err.println("restarting...");
            }
          } finally {
            if (MEASURE_MEMORY_USAGE) {
              long memoryAfterPass = Util.getUsedMemory();
              assert initialMemory != 0;
              long usedByPass = memoryAfterPass - initialMemory;
              if (usedByPass > lastQueryMemoryUse) {
                lastQueryMemoryUse = usedByPass;
                if (usedByPass > 20000000) {
                  System.err.println("DOH!");
                  System.exit(1);
                }
              }
            }

          }
        }
      } catch (BudgetExceededException e) {

      }
      if (completedPassInBudget) {
        if (passed) {
          succeeded = true;
          break;
        }
      }
      // if we get here, means either budget for pass was exceeded,
      // or points-to set wasn't good enough
      // so, start new pass, if more refinement to do
      if (!refinementPolicy.nextPass()) {
        break;
      }
    }
    PointsToResult result = null;
    if (succeeded) {
      result = PointsToResult.SUCCESS;
    } else if (passNum == numPasses) {
      // we ran all the passes without succeeding and
      // without the refinement policy giving up
      result = PointsToResult.BUDGETEXCEEDED;
    } else {
      result = completedSomePass ? PointsToResult.NOMOREREFINE : PointsToResult.BUDGETEXCEEDED;
    }
    if (MEASURE_MEMORY_USAGE) {
      System.err.println("memory " + lastQueryMemoryUse);
    }
    return result;
  }

  /**
   * do all instance keys in p2set pass ikeyPred?
   */
  private static boolean passesPred(Collection<InstanceKeyAndState> curP2Set, final Predicate<InstanceKey> ikeyPred) {
    return Util.forAll(curP2Set, t -> ikeyPred.test(t.getInstanceKey()));
  }

  /**
   * @return the points-to set of <code>pk</code>, or <code>null</code> if the points-to set can't be computed in the allocated
   *         budget
   */
  @Override
  public Collection<InstanceKey> getPointsTo(PointerKey pk) {
    return getPointsTo(pk, k -> { return false; }).snd;
  }

  /**
   * @return the points-to set of <code>pk</code>, including the {@link State}s attached to the {@link InstanceKey}s, or
   *         <code>null</code> if the points-to set can't be computed in the allocated budget
   */
  public Collection<InstanceKeyAndState> getPointsToWithStates(PointerKey pk) {
    return getPointsToWithStates(pk, k -> { return false; }).snd;
  }

  /**
   * get all the pointer keys that some instance key can flow to
   * 
   * @return a pair consisting of (1) a {@link PointsToResult} indicating whether a flows-to set was computed, and (2) the last
   *         computed flows-to set for the instance key (possibly <code>null</code> if no flows-to set could be computed in the
   *         budget)
   */
  public Pair<PointsToResult, Collection<PointerKey>> getFlowsTo(InstanceKey ik) {
    startNewQuery();
    return getFlowsToInternal(new InstanceKeyAndState(ik, stateMachine.getStartState()));
  }

  /**
   * get all the pointer keys that some instance key with state can flow to
   * 
   * @return a pair consisting of (1) a {@link PointsToResult} indicating whether a flows-to set was computed, and (2) the last
   *         computed flows-to set for the instance key (possibly <code>null</code> if no flows-to set could be computed in the
   *         budget)
   */
  public Pair<PointsToResult, Collection<PointerKey>> getFlowsTo(InstanceKeyAndState ikAndState) {
    startNewQuery();
    return getFlowsToInternal(ikAndState);
  }

  /**
   * @param ik
   */
  private Pair<PointsToResult, Collection<PointerKey>> getFlowsToInternal(InstanceKeyAndState ikAndState) {
    InstanceKey ik = ikAndState.getInstanceKey();
    if (!(ik instanceof InstanceKeyWithNode)) {
      assert false : "TODO: handle " + ik.getClass();
    }
    if (DEBUG) {
      System.err.println("answering flows-to query for " + ikAndState);
    }
    Collection<PointerKeyAndState> lastFlowsToSet = null;
    boolean succeeded = false;
    int numPasses = refinementPolicy.getNumPasses();
    int passNum = 0;
    for (; passNum < numPasses; passNum++) {
      setNumNodesTraversed(0);
      setTraversalBudget(refinementPolicy.getBudgetForPass(passNum));
      Collection<PointerKeyAndState> curFlowsToSet = null;
      FlowsToComputer computer = null;
      try {
        while (true) {
          try {
            computer = new FlowsToComputer(ikAndState);
            computer.compute();
            curFlowsToSet = computer.getComputedFlowsToSet();
            // System.err.println("completed pass");
            if (DEBUG) {
              System.err.println("traversed " + getNumNodesTraversed() + " nodes");
              System.err.println("FLOWS-TO SET " + curFlowsToSet);
            }
            break;
          } catch (StatesMergedException e) {
            if (DEBUG) {
              System.err.println("restarting...");
            }
          }
        }
      } catch (BudgetExceededException e) {

      }
      if (curFlowsToSet != null) {
        if (lastFlowsToSet == null) {
          lastFlowsToSet = curFlowsToSet;
        } else if (lastFlowsToSet.size() > curFlowsToSet.size()) {
          // got a more precise set
          assert removeStates(lastFlowsToSet).containsAll(removeStates(curFlowsToSet));
          lastFlowsToSet = curFlowsToSet;
        } else {
          // new set size is >= lastP2Set, so don't update
          // TODO what is wrong with this assertion?!? --MS
          // assert removeStates(curFlowsToSet).containsAll(removeStates(lastFlowsToSet));
        }
        // TODO add predicate support
        if (curFlowsToSet.isEmpty() /* || passesPred(curFlowsToSet, ikeyPred) */) {
          succeeded = true;
          break;
        }
      }
      // if we get here, means either budget for pass was exceeded,
      // or points-to set wasn't good enough
      // so, start new pass, if more refinement to do
      if (!refinementPolicy.nextPass()) {
        break;
      }
    }
    PointsToResult result = null;
    if (succeeded) {
      result = PointsToResult.SUCCESS;
    } else if (passNum == numPasses) {
      // we ran all the passes without succeeding and
      // without the refinement policy giving up
      result = PointsToResult.BUDGETEXCEEDED;
    } else {
      if (lastFlowsToSet != null) {
        result = PointsToResult.NOMOREREFINE;
      } else {
        // we stopped before the maximum number of passes, but we never
        // actually finished a pass, so we count this as BUDGETEXCEEDED
        result = PointsToResult.BUDGETEXCEEDED;
      }
    }
    return Pair.make(result, lastFlowsToSet == null ? null : removeStates(lastFlowsToSet));
  }

  /**
   * Closure indicating how to handle copies between {@link PointerKey}s.
   * 
   * @author Manu Sridharan
   * 
   */
  private static abstract class CopyHandler {

    abstract void handle(PointerKeyAndState src, PointerKey dst, IFlowLabel label);
  }

  /**
   * Representation of a statement storing a value into a field.
   * 
   * @author Manu Sridharan
   * 
   */
  private static final class StoreEdge {
    // 
    // Represents statement of the form base.field = val

    final PointerKeyAndState base;

    final IField field;

    final PointerKeyAndState val;

    @Override
    public int hashCode() {
      final int PRIME = 31;
      int result = 1;
      result = PRIME * result + val.hashCode();
      result = PRIME * result + field.hashCode();
      result = PRIME * result + base.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final StoreEdge other = (StoreEdge) obj;
      if (!val.equals(other.val))
        return false;
      if (!field.equals(other.field))
        return false;
      if (!base.equals(other.base))
        return false;
      return true;
    }

    public StoreEdge(final PointerKeyAndState base, final IField field, final PointerKeyAndState val) {
      this.base = base;
      this.field = field;
      this.val = val;
    }

  }

  /**
   * Representation of a field read.
   * 
   * @author Manu Sridharan
   * 
   */
  private static final class LoadEdge {
    // Represents statements of the form val = base.field
    final PointerKeyAndState base;

    final IField field;

    final PointerKeyAndState val;

    @Override
    public String toString() {
      return val + " := " + base + ", field " + field;
    }

    @Override
    public int hashCode() {
      final int PRIME = 31;
      int result = 1;
      result = PRIME * result + val.hashCode();
      result = PRIME * result + field.hashCode();
      result = PRIME * result + base.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final LoadEdge other = (LoadEdge) obj;
      if (!val.equals(other.val))
        return false;
      if (!field.equals(other.field))
        return false;
      if (!base.equals(other.base))
        return false;
      return true;
    }

    public LoadEdge(final PointerKeyAndState base, final IField field, final PointerKeyAndState val) {
      this.base = base;
      this.field = field;
      this.val = val;
    }

  }

  /**
   * Points-to analysis algorithm code.
   * 
   * Pseudocode in Chapter 5 of Manu Sridharan's dissertation.
   * 
   * @author Manu Sridharan
   * 
   */
  protected class PointsToComputer {

    protected final PointerKeyAndState queriedPkAndState;

    /**
     * map from pointer key to states in which the key's points-to set was queried
     */
    private final MultiMap<PointerKey, State> pointsToQueried = HashSetMultiMap.make();

    /**
     * map from pointer key to states in which a tracked points-to set for the key was computed
     */
    private final MultiMap<PointerKey, State> trackedQueried = HashSetMultiMap.make();

    /**
     * forward worklist: for initially processing points-to queries
     */
    private final Collection<PointerKeyAndState> initWorklist = new LinkedHashSet<>();

    /**
     * worklist for variables whose points-to set has been updated
     */
    private final Collection<PointerKeyAndState> pointsToWorklist = new LinkedHashSet<>();

    /**
     * worklist for variables whose tracked points-to set has been updated
     */
    private final Collection<PointerKeyAndState> trackedPointsToWorklist = new LinkedHashSet<>();

    /**
     * maps a pointer key to those on-the-fly virtual calls for which it is the receiver
     */
    private final MultiMap<PointerKeyAndState, CallerSiteContext> pkToOTFCalls = HashSetMultiMap.make();

    /**
     * cache of the targets discovered for a call site during on-the-fly call graph construction
     */
    private final MultiMap<CallerSiteContext, IMethod> callToOTFTargets = ArraySetMultiMap.make();

    // alloc nodes to the fields we're looking to match on them,
    // matching getfield with putfield
    private final MultiMap<InstanceKeyAndState, IField> forwInstKeyToFields = HashSetMultiMap.make();

    // matching putfield_bar with getfield_bar
    private final MultiMap<InstanceKeyAndState, IField> backInstKeyToFields = HashSetMultiMap.make();

    // points-to sets and tracked points-to sets
    protected final Map<PointerKeyAndState, MutableIntSet> pkToP2Set = HashMapFactory.make();

    protected final Map<PointerKeyAndState, MutableIntSet> pkToTrackedSet = HashMapFactory.make();

    private final Map<InstanceFieldKeyAndState, MutableIntSet> instFieldKeyToP2Set = HashMapFactory.make();

    private final Map<InstanceFieldKeyAndState, MutableIntSet> instFieldKeyToTrackedSet = HashMapFactory.make();

    /**
     * for numbering {@link InstanceKey}, {@link State} pairs
     */
    protected final OrdinalSetMapping<InstanceKeyAndState> ikAndStates = MutableMapping.make();

    private final MutableIntSetFactory intSetFactory = new MutableSparseIntSetFactory(); // new

    // BitVectorIntSetFactory();

    /**
     * tracks all field stores encountered during traversal
     */
    private final HashSet<StoreEdge> encounteredStores = HashSetFactory.make();

    /**
     * tracks all field loads encountered during traversal
     */
    private final HashSet<LoadEdge> encounteredLoads = HashSetFactory.make();

    /**
     * use this with care! only for subclasses that aren't computing points-to information exactly (e.g., {@link FlowsToComputer})
     */
    protected PointsToComputer() {
      queriedPkAndState = null;
    }

    protected PointsToComputer(PointerKey pk) {
      queriedPkAndState = new PointerKeyAndState(pk, stateMachine.getStartState());
    }

    protected PointsToComputer(PointerKeyAndState pkAndState) {
      this.queriedPkAndState = pkAndState;
    }

    private OrdinalSet<InstanceKeyAndState> makeOrdinalSet(IntSet intSet) {
      // make a copy here, to avoid comodification during iteration
      // TODO remove the copying, do it only at necessary call sites
      return new OrdinalSet<>(intSetFactory.makeCopy(intSet), ikAndStates);
    }

    /**
     * get a points-to set that has already been computed via some previous call to {@link #compute()}; does _not_ do any fresh
     * demand-driven computation.
     */
    public Collection<InstanceKeyAndState> getComputedP2Set(PointerKeyAndState queried) {
      return Iterator2Collection.toSet(makeOrdinalSet(find(pkToP2Set, queried)).iterator());
      // return Iterator2Collection.toSet(new MapIterator<InstanceKeyAndState, InstanceKey>(makeOrdinalSet(
      // find(pkToP2Set, new PointerKeyAndState(lpk, stateMachine.getStartState()))).iterator(),
      // new Function<InstanceKeyAndState, InstanceKey>() {
      //
      // public InstanceKey apply(InstanceKeyAndState object) {
      // return object.getInstanceKey();
      // }
      //
      // }));
    }

    @SuppressWarnings("unused")
    protected boolean addAllToP2Set(Map<PointerKeyAndState, MutableIntSet> p2setMap, PointerKeyAndState pkAndState, IntSet vals,
        IFlowLabel label) {
      final PointerKey pk = pkAndState.getPointerKey();
      if (pk instanceof FilteredPointerKey) {
        if (DEBUG) {
          System.err.println("handling filtered pointer key " + pk);
        }
        final TypeFilter typeFilter = ((FilteredPointerKey) pk).getTypeFilter();
        vals = updateValsForFilter(vals, typeFilter);
      }
      if (label instanceof IFlowLabelWithFilter) {
        TypeFilter typeFilter = ((IFlowLabelWithFilter) label).getFilter();
        if (typeFilter != null) {
          vals = updateValsForFilter(vals, typeFilter);
        }
      }
      boolean added = findOrCreate(p2setMap, pkAndState).addAll(vals);
      // final boolean added = p2setMap.putAll(pkAndState, vals);
      if (DEBUG && added) {
        System.err.println("POINTS-TO ADDITION TO PK " + pkAndState + ":");
        for (InstanceKeyAndState ikAndState : makeOrdinalSet(vals)) {
          System.err.println(ikAndState);
        }
        System.err.println("*************");
      }
      return added;

    }

    private IntSet updateValsForFilter(IntSet vals, final TypeFilter typeFilter) {
      if (typeFilter instanceof SingleClassFilter) {
        final IClass concreteType = ((SingleClassFilter) typeFilter).getConcreteType();
        final MutableIntSet tmp = intSetFactory.make();
        vals.foreach(x -> {
          InstanceKeyAndState ikAndState = ikAndStates.getMappedObject(x);
          if (cha.isAssignableFrom(concreteType, ikAndState.getInstanceKey().getConcreteType())) {
            tmp.add(x);
          }
        });
        vals = tmp;
      } else if (typeFilter instanceof MultipleClassesFilter) {
        final MutableIntSet tmp = intSetFactory.make();
        vals.foreach(x -> {
          InstanceKeyAndState ikAndState = ikAndStates.getMappedObject(x);
          for (IClass t : ((MultipleClassesFilter) typeFilter).getConcreteTypes()) {
            if (cha.isAssignableFrom(t, ikAndState.getInstanceKey().getConcreteType())) {
              tmp.add(x);
            }
          }
        });
        vals = tmp;
      } else if (typeFilter instanceof SingleInstanceFilter) {
        final InstanceKey theOnlyInstanceKey = ((SingleInstanceFilter) typeFilter).getInstance();
        final MutableIntSet tmp = intSetFactory.make();
        vals.foreach(x -> {
          InstanceKeyAndState ikAndState = ikAndStates.getMappedObject(x);
          if (ikAndState.getInstanceKey().equals(theOnlyInstanceKey)) {
            tmp.add(x);
          }
        });
        vals = tmp;
      } else {
        Assertions.UNREACHABLE();
      }
      return vals;
    }

    protected void compute() {
      final CGNode node = ((LocalPointerKey) queriedPkAndState.getPointerKey()).getNode();
      if (hasNullIR(node)) {
        return;
      }
      g.addSubgraphForNode(node);
      addToInitWorklist(queriedPkAndState);
      worklistLoop();
    }

    protected void worklistLoop() {
      do {
        while (!initWorklist.isEmpty() || !pointsToWorklist.isEmpty() || !trackedPointsToWorklist.isEmpty()) {
          handleInitWorklist();
          handlePointsToWorklist();
          handleTrackedPointsToWorklist();
        }
        makePassOverFieldStmts();
      } while (!initWorklist.isEmpty() || !pointsToWorklist.isEmpty() || !trackedPointsToWorklist.isEmpty());
    }

    void handleCopy(final PointerKeyAndState curPkAndState, final PointerKey succPk, final IFlowLabel label) {
      assert !label.isBarred();
      State curState = curPkAndState.getState();
      doTransition(curState, label, nextState -> {
        PointerKeyAndState succPkAndState = new PointerKeyAndState(succPk, nextState);
        handleCopy(curPkAndState, succPkAndState, label);
        return null;
      });
    }

    void handleCopy(PointerKeyAndState curPkAndState, PointerKeyAndState succPkAndState, IFlowLabel label) {
      if (!addToInitWorklist(succPkAndState)) {
        // handle like x = y with Y updated
        if (addAllToP2Set(pkToP2Set, curPkAndState, find(pkToP2Set, succPkAndState), label)) {
          addToPToWorklist(curPkAndState);
        }
      }

    }

    void handleAllCopies(PointerKeyAndState curPk, Iterator<? extends Object> succNodes, IFlowLabel label) {
      while (succNodes.hasNext()) {
        handleCopy(curPk, (PointerKey) succNodes.next(), label);
      }
    }

    /**
     * @param label the label of the edge from curPk to predPk (must be barred)
     * @return those {@link PointerKeyAndState}s whose points-to sets have been queried, such that the {@link PointerKey} is predPk,
     *         and transitioning from its state on <code>label.bar()</code> yields the state of <code>curPkAndState</code>
     */
    protected Collection<PointerKeyAndState> matchingPToQueried(PointerKeyAndState curPkAndState, PointerKey predPk,
        IFlowLabel label) {
      Collection<PointerKeyAndState> ret = ArraySet.make();
      assert label.isBarred();
      IFlowLabel unbarredLabel = label.bar();
      final State curState = curPkAndState.getState();
      Set<State> predPkStates = pointsToQueried.get(predPk);
      for (State predState : predPkStates) {
        State transState = stateMachine.transition(predState, unbarredLabel);
        if (transState.equals(curState)) {
          // we have a winner!
          ret.add(new PointerKeyAndState(predPk, predState));
        }
      }
      return ret;
    }

    Collection<PointerKeyAndState> matchingTrackedQueried(PointerKeyAndState curPkAndState, PointerKey succPk, IFlowLabel label) {
      Collection<PointerKeyAndState> ret = ArraySet.make();
      assert label.isBarred();
      final State curState = curPkAndState.getState();
      Set<State> succPkStates = trackedQueried.get(succPk);
      for (State succState : succPkStates) {
        State transState = stateMachine.transition(succState, label);
        if (transState.equals(curState)) {
          ret.add(new PointerKeyAndState(succPk, succState));
        }
      }
      return ret;
    }

    protected void handleBackCopy(PointerKeyAndState curPkAndState, PointerKey predPk, IFlowLabel label) {
      for (PointerKeyAndState predPkAndState : matchingPToQueried(curPkAndState, predPk, label)) {
        if (addAllToP2Set(pkToP2Set, predPkAndState, find(pkToP2Set, curPkAndState), label)) {
          addToPToWorklist(predPkAndState);
        }
      }
    }

    void handleAllBackCopies(PointerKeyAndState curPkAndState, Iterator<? extends Object> predNodes, IFlowLabel label) {
      while (predNodes.hasNext()) {
        handleBackCopy(curPkAndState, (PointerKey) predNodes.next(), label);
      }
    }

    /**
     * should only be called when pk's points-to set has just been updated. add pk to the points-to worklist, and re-propagate and
     * calls that had pk as the receiver.
     */
    void addToPToWorklist(PointerKeyAndState pkAndState) {
      pointsToWorklist.add(pkAndState);
      Set<CallerSiteContext> otfCalls = pkToOTFCalls.get(pkAndState);
      for (CallerSiteContext callSiteAndCGNode : otfCalls) {
        propTargets(pkAndState, callSiteAndCGNode);
      }
    }

    boolean addToInitWorklist(PointerKeyAndState pkAndState) {
      if (pointsToQueried.put(pkAndState.getPointerKey(), pkAndState.getState())) {
        if (pkAndState.getPointerKey() instanceof AbstractLocalPointerKey) {
          CGNode node = ((AbstractLocalPointerKey) pkAndState.getPointerKey()).getNode();
          if (!g.hasSubgraphForNode(node)) {
            assert false : "missing constraints for " + node;
          }
        }
        if (DEBUG) {
          // System.err.println("adding to init_ " + pkAndState);
        }
        initWorklist.add(pkAndState);
        // if (pkAndStates.getMappedIndex(pkAndState) == -1) {
        // pkAndStates.add(pkAndState);
        // }
        return true;
      }
      return false;
    }

    protected void addToTrackedPToWorklist(PointerKeyAndState pkAndState) {
      if (pkAndState.getPointerKey() instanceof AbstractLocalPointerKey) {
        CGNode node = ((AbstractLocalPointerKey) pkAndState.getPointerKey()).getNode();
        if (!g.hasSubgraphForNode(node)) {
          assert false : "missing constraints for " + node;
        }
      }
      if (DEBUG) {
        // System.err.println("adding to tracked points-to " + pkAndState);
      }
      trackedQueried.put(pkAndState.getPointerKey(), pkAndState.getState());
      trackedPointsToWorklist.add(pkAndState);
    }

    /**
     * Adds new targets for a virtual call, based on the points-to set of the receiver, and propagates values for the parameters /
     * return value of the new targets. NOTE: this method will <em>not</em> do any propagation for virtual call targets that have
     * already been discovered.
     * 
     * @param receiverAndState the receiver
     * @param callSiteAndCGNode the call
     */
    void propTargets(PointerKeyAndState receiverAndState, CallerSiteContext callSiteAndCGNode) {
      final CGNode caller = callSiteAndCGNode.getCaller();
      CallSiteReference call = callSiteAndCGNode.getCallSite();
      final State receiverState = receiverAndState.getState();
      OrdinalSet<InstanceKeyAndState> p2set = makeOrdinalSet(find(pkToP2Set, receiverAndState));
      for (InstanceKeyAndState ikAndState : p2set) {
        InstanceKey ik = ikAndState.getInstanceKey();
        IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, call, ik.getConcreteType());
        if (targetMethod == null) {
          // NOTE: target method can be null because we don't
          // always have type filters
          continue;
        }
        // if we've already handled this target, we can stop
        if (callToOTFTargets.get(callSiteAndCGNode).contains(targetMethod)) {
          continue;
        }
        callToOTFTargets.put(callSiteAndCGNode, targetMethod);
        // TODO can we just pick one of these, rather than all of them?
        // TODO handle clone() properly
        Set<CGNode> targetCGNodes = cg.getNodes(targetMethod.getReference());
        for (final CGNode targetForCall : targetCGNodes) {
          if (DEBUG) {
            System.err.println("adding target " + targetForCall + " for call " + call);
          }
          if (hasNullIR(targetForCall)) {
            continue;
          }
          g.addSubgraphForNode(targetForCall);
          // need to check flows through parameters and returns,
          // in direction of value flow and reverse
          SSAAbstractInvokeInstruction[] calls = getCallInstrs(caller, call);
          for (final SSAAbstractInvokeInstruction invokeInstr : calls) {
            final ReturnLabel returnLabel = ReturnLabel.make(new CallerSiteContext(caller, call));
            if (invokeInstr.hasDef()) {
              final PointerKeyAndState defAndState = new PointerKeyAndState(heapModel.getPointerKeyForLocal(caller, invokeInstr
                  .getDef()), receiverState);
              final PointerKey ret = heapModel.getPointerKeyForReturnValue(targetForCall);
              doTransition(receiverState, returnLabel, retState -> {
                repropCallArg(defAndState, new PointerKeyAndState(ret, retState), returnLabel.bar());
                return null;
              });
            }
            final PointerKeyAndState exc = new PointerKeyAndState(heapModel.getPointerKeyForLocal(caller, invokeInstr
                .getException()), receiverState);
            final PointerKey excRet = heapModel.getPointerKeyForExceptionalReturnValue(targetForCall);
            doTransition(receiverState, returnLabel, excRetState -> {
              repropCallArg(exc, new PointerKeyAndState(excRet, excRetState), returnLabel.bar());
              return null;
            });
            for (int formalNum : Iterator2Iterable.make(new PointerParamValueNumIterator(targetForCall))) {
              final int actualNum = formalNum - 1;
              final ParamBarLabel paramBarLabel = ParamBarLabel.make(new CallerSiteContext(caller, call));
              doTransition(receiverState, paramBarLabel, formalState -> {
                repropCallArg(
                    new PointerKeyAndState(heapModel.getPointerKeyForLocal(targetForCall, formalNum), formalState),
                    new PointerKeyAndState(heapModel.getPointerKeyForLocal(caller, invokeInstr.getUse(actualNum)), receiverState),
                    paramBarLabel);
                return null;
              });
            }
          }
        }
      }
    }

    /**
     * handle possible updated flow in both directions for a call parameter
     * 
     * @param src
     * @param dst
     */
    private void repropCallArg(PointerKeyAndState src, PointerKeyAndState dst, IFlowLabel dstToSrcLabel) {
      if (DEBUG) {
        // System.err.println("re-propping from src " + src + " to dst " + dst);
      }
      for (PointerKeyAndState srcToHandle : matchingPToQueried(dst, src.getPointerKey(), dstToSrcLabel)) {
        handleCopy(srcToHandle, dst, dstToSrcLabel.bar());
      }
      for (PointerKeyAndState dstToHandle : matchingTrackedQueried(src, dst.getPointerKey(), dstToSrcLabel)) {
        IntSet trackedSet = find(pkToTrackedSet, dstToHandle);
        if (!trackedSet.isEmpty()) {
          if (findOrCreate(pkToTrackedSet, src).addAll(trackedSet)) {
            addToTrackedPToWorklist(src);
          }
        }
      }
    }

    void handleInitWorklist() {
      while (!initWorklist.isEmpty()) {
        incrementNumNodesTraversed();
        final PointerKeyAndState curPkAndState = initWorklist.iterator().next();
        initWorklist.remove(curPkAndState);
        final PointerKey curPk = curPkAndState.getPointerKey();
        final State curState = curPkAndState.getState();
        if (DEBUG)
          System.err.println("init " + curPkAndState);
        if (curPk instanceof LocalPointerKey) {
          assert g.hasSubgraphForNode(((LocalPointerKey) curPk).getNode());
        }
        // if (curPk instanceof LocalPointerKey) {
        // Collection<InstanceKey> constantVals =
        // getConstantVals((LocalPointerKey) curPk);
        // if (constantVals != null) {
        // for (InstanceKey ik : constantVals) {
        // pkToP2Set.put(curPk, ik);
        // addToPToWorklist(curPk);
        // }
        // }
        // }
        IFlowLabelVisitor v = new AbstractFlowLabelVisitor() {

          @Override
          public void visitNew(NewLabel label, Object dst) {
            final InstanceKey ik = (InstanceKey) dst;
            if (DEBUG) {
              System.err.println("alloc " + ik + " assigned to " + curPk);
            }
            doTransition(curState, label, newState -> {
              InstanceKeyAndState ikAndState = new InstanceKeyAndState(ik, newState);
              int n = ikAndStates.add(ikAndState);
              findOrCreate(pkToP2Set, curPkAndState).add(n);
              addToPToWorklist(curPkAndState);
              return null;
            });
          }

          @Override
          public void visitGetField(GetFieldLabel label, Object dst) {
            IField field = (label).getField();
            PointerKey loadBase = (PointerKey) dst;
            if (refineFieldAccesses(field, loadBase, curPk, label, curState)) {
              // if (Assertions.verifyAssertions) {
              // Assertions._assert(stateMachine.transition(curState, label) ==
              // curState);
              // }
              PointerKeyAndState loadBaseAndState = new PointerKeyAndState(loadBase, curState);
              addEncounteredLoad(new LoadEdge(loadBaseAndState, field, curPkAndState));
              if (!addToInitWorklist(loadBaseAndState)) {
                // handle like x = y.f, with Y updated
                for (InstanceKeyAndState ikAndState : makeOrdinalSet(find(pkToP2Set, loadBaseAndState))) {
                  trackInstanceField(ikAndState, field, forwInstKeyToFields);
                }
              }
            } else {
              handleAllCopies(curPkAndState, g.getWritesToInstanceField(loadBase, field), MatchLabel.v());
            }
          }

          @Override
          public void visitAssignGlobal(AssignGlobalLabel label, Object dst) {
            handleAllCopies(curPkAndState, g.getWritesToStaticField((StaticFieldKey) dst), AssignGlobalLabel.v());
          }

          @Override
          public void visitAssign(AssignLabel label, Object dst) {
            handleCopy(curPkAndState, (PointerKey) dst, AssignLabel.noFilter());
          }

        };
        g.visitSuccs(curPk, v);
        // interprocedural edges
        handleForwInterproc(curPkAndState, new CopyHandler() {

          @Override
          void handle(PointerKeyAndState src, PointerKey dst, IFlowLabel label) {
            handleCopy(src, dst, label);
          }

        });
      }

    }

    /**
     * handle flow from actuals to formals, and from returned values to variables at the caller
     * 
     * @param curPk
     * @param handler
     */
    private void handleForwInterproc(final PointerKeyAndState curPkAndState, final CopyHandler handler) {
      PointerKey curPk = curPkAndState.getPointerKey();
      if (curPk instanceof LocalPointerKey) {
        final LocalPointerKey localPk = (LocalPointerKey) curPk;
        if (g.isParam(localPk)) {
          // System.err.println("at param");
          final CGNode callee = localPk.getNode();
          final int paramPos = localPk.getValueNumber() - 1;
          for (final CallerSiteContext callSiteAndCGNode : g.getPotentialCallers(localPk)) {
            final CGNode caller = callSiteAndCGNode.getCaller();
            final CallSiteReference call = callSiteAndCGNode.getCallSite();
            // final IR ir = getIR(caller);
            if (hasNullIR(caller))
              continue;
            final ParamLabel paramLabel = ParamLabel.make(callSiteAndCGNode);
            doTransition(curPkAndState.getState(), paramLabel, new Function<State, Object>() {

              private void propagateToCallee() {
                // if (caller.getIR() == null) {
                // return;
                // }
                g.addSubgraphForNode(caller);
                SSAAbstractInvokeInstruction[] callInstrs = getCallInstrs(caller, call);
                for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
                  PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
                  assert g.containsNode(actualPk);
                  assert g.containsNode(localPk);
                  handler.handle(curPkAndState, actualPk, paramLabel);
                }
              }

              @Override
              public Object apply(State callerState) {
                // hack to get some actual parameter from call site
                // TODO do this better
                SSAAbstractInvokeInstruction[] callInstrs = getCallInstrs(caller, call);
                SSAAbstractInvokeInstruction callInstr = callInstrs[0];
                PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
                Set<CGNode> possibleTargets = g.getPossibleTargets(caller, call, (LocalPointerKey) actualPk);
                if (noOnTheFlyNeeded(callSiteAndCGNode, possibleTargets)) {
                  propagateToCallee();
                } else {
                  if (callToOTFTargets.get(callSiteAndCGNode).contains(callee.getMethod())) {
                    // already found this target as valid, so do propagation
                    propagateToCallee();
                  } else {
                    // if necessary, start a query for the call site
                    queryCallTargets(callSiteAndCGNode, callInstrs, callerState);
                  }
                }
                return null;
              }

            });

          }
        }
        SSAAbstractInvokeInstruction callInstr = g.getInstrReturningTo(localPk);
        if (callInstr != null) {
          CGNode caller = localPk.getNode();
          boolean isExceptional = localPk.getValueNumber() == callInstr.getException();

          CallSiteReference callSiteRef = callInstr.getCallSite();
          CallerSiteContext callSiteAndCGNode = new CallerSiteContext(caller, callSiteRef);
          // get call targets
          Set<CGNode> possibleCallees = g.getPossibleTargets(caller, callSiteRef, localPk);

          // cg.getPossibleTargets(caller, callSiteRef);
          // if (DEBUG &&
          // callSiteRef.getDeclaredTarget().toString().indexOf("clone()") !=
          // -1) {
          // System.err.println(possibleCallees);
          // System.err.println(Iterator2Collection.toCollection(cg.getSuccNodes(caller)));
          // System.err.println(Iterator2Collection.toCollection(cg.getPredNodes(possibleCallees.iterator().next())));
          // }
          // construct graph for each target
          if (noOnTheFlyNeeded(callSiteAndCGNode, possibleCallees)) {
            for (CGNode callee : possibleCallees) {
              if (hasNullIR(callee)) {
                continue;
              }
              g.addSubgraphForNode(callee);
              PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
                  .getPointerKeyForReturnValue(callee);
              assert g.containsNode(retVal);
              handler.handle(curPkAndState, retVal, ReturnLabel.make(callSiteAndCGNode));
            }
          } else {
            if (callToOTFTargets.containsKey(callSiteAndCGNode)) {
              // already queried this call site
              // handle existing targets
              Set<IMethod> targetMethods = callToOTFTargets.get(callSiteAndCGNode);
              for (CGNode callee : possibleCallees) {
                if (targetMethods.contains(callee.getMethod())) {
                  if (hasNullIR(callee)) {
                    continue;
                  }
                  g.addSubgraphForNode(callee);
                  PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
                      .getPointerKeyForReturnValue(callee);
                  assert g.containsNode(retVal);
                  handler.handle(curPkAndState, retVal, ReturnLabel.make(callSiteAndCGNode));
                }
              }
            } else {
              // if necessary, raise a query for the call site
              queryCallTargets(callSiteAndCGNode, getCallInstrs(caller, callSiteAndCGNode.getCallSite()), curPkAndState.getState());
            }
          }
        }
      }
    }

    /**
     * track a field of some instance key, as we are interested in statements that read or write to the field
     * 
     * @param ikAndState
     * @param field
     * @param ikToFields either {@link #forwInstKeyToFields} or {@link #backInstKeyToFields}
     */
    private void trackInstanceField(InstanceKeyAndState ikAndState, IField field, MultiMap<InstanceKeyAndState, IField> ikToFields) {
      ikToFields.put(ikAndState, field);
      addPredsOfIKeyAndStateToTrackedPointsTo(ikAndState);
    }

    private void addPredsOfIKeyAndStateToTrackedPointsTo(InstanceKeyAndState ikAndState) throws UnimplementedError {
      for (Object o : Iterator2Iterable.make(g.getPredNodes(ikAndState.getInstanceKey(), NewLabel.v()))) {
        PointerKey ikPred = (PointerKey) o;
        PointerKeyAndState ikPredAndState = new PointerKeyAndState(ikPred, ikAndState.getState());
        int mappedIndex = ikAndStates.getMappedIndex(ikAndState);
        assert mappedIndex != -1;
        if (findOrCreate(pkToTrackedSet, ikPredAndState).add(mappedIndex)) {
          addToTrackedPToWorklist(ikPredAndState);
        }
      }
    }

    /**
     * Initiates a query for the targets of some virtual call, by asking for points-to set of receiver. NOTE: if receiver has
     * already been queried, will not do any additional propagation for already-discovered virtual call targets
     */
    private void queryCallTargets(CallerSiteContext callSiteAndCGNode, SSAAbstractInvokeInstruction[] callInstrs, State callerState) {
      final CGNode caller = callSiteAndCGNode.getCaller();
      for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
        PointerKey thisArg = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(0));
        PointerKeyAndState thisArgAndState = new PointerKeyAndState(thisArg, callerState);
        if (pkToOTFCalls.put(thisArgAndState, callSiteAndCGNode)) {
          // added the call target
          final CGNode node = ((LocalPointerKey) thisArg).getNode();
          if (hasNullIR(node)) {
            return;
          }
          g.addSubgraphForNode(node);
          if (!addToInitWorklist(thisArgAndState)) {
            // need to handle pk's current values for call
            propTargets(thisArgAndState, callSiteAndCGNode);
          } else {
            if (DEBUG) {
              final CallSiteReference call = callSiteAndCGNode.getCallSite();
              System.err.println("querying for targets of call " + call + " in " + caller);
            }
          }
        } else {
          // TODO: I think we can remove this call
          propTargets(thisArgAndState, callSiteAndCGNode);
        }
      }
    }

    void handlePointsToWorklist() {
      while (!pointsToWorklist.isEmpty()) {
        incrementNumNodesTraversed();
        final PointerKeyAndState curPkAndState = pointsToWorklist.iterator().next();
        pointsToWorklist.remove(curPkAndState);
        final PointerKey curPk = curPkAndState.getPointerKey();
        final State curState = curPkAndState.getState();
        if (DEBUG) {
          System.err.println("points-to " + curPkAndState);
          System.err.println("***pto-set " + find(pkToP2Set, curPkAndState) + "***");
        }
        IFlowLabelVisitor predVisitor = new AbstractFlowLabelVisitor() {

          @Override
          public void visitPutField(PutFieldLabel label, Object dst) {
            IField field = label.getField();
            PointerKey storeBase = (PointerKey) dst;
            if (refineFieldAccesses(field, storeBase, curPk, label, curState)) {
              // statements x.f = y, Y updated (X' not empty required)
              // update Z.f for all z in X'
              PointerKeyAndState storeBaseAndState = new PointerKeyAndState(storeBase, curState);
              encounteredStores.add(new StoreEdge(storeBaseAndState, field, curPkAndState));
              for (InstanceKeyAndState ikAndState : makeOrdinalSet(find(pkToTrackedSet, storeBaseAndState))) {
                if (forwInstKeyToFields.get(ikAndState).contains(field)) {
                  InstanceFieldKeyAndState ifKeyAndState = getInstFieldKey(ikAndState, field);
                  findOrCreate(instFieldKeyToP2Set, ifKeyAndState).addAll(find(pkToP2Set, curPkAndState));

                }
              }
            } else {
              handleAllBackCopies(curPkAndState, g.getReadsOfInstanceField(storeBase, field), MatchBarLabel.v());
            }
          }

          @Override
          public void visitGetField(GetFieldLabel label, Object dst) {
            IField field = (label).getField();
            PointerKey dstPtrKey = (PointerKey) dst;
            if (refineFieldAccesses(field, curPk, dstPtrKey, label, curState)) {
              // statements x = y.f, Y updated
              // if X queried, start tracking Y.f
              PointerKeyAndState loadDefAndState = new PointerKeyAndState(dstPtrKey, curState);
              addEncounteredLoad(new LoadEdge(curPkAndState, field, loadDefAndState));
              if (pointsToQueried.get(dstPtrKey).contains(curState)) {
                for (InstanceKeyAndState ikAndState : makeOrdinalSet(find(pkToP2Set, curPkAndState))) {
                  trackInstanceField(ikAndState, field, forwInstKeyToFields);
                }
              }
            }
          }

          @Override
          public void visitAssignGlobal(AssignGlobalLabel label, Object dst) {
            handleAllBackCopies(curPkAndState, g.getReadsOfStaticField((StaticFieldKey) dst), label.bar());
          }

          @Override
          public void visitAssign(AssignLabel label, Object dst) {
            handleBackCopy(curPkAndState, (PointerKey) dst, label.bar());
          }

        };
        g.visitPreds(curPk, predVisitor);
        IFlowLabelVisitor succVisitor = new AbstractFlowLabelVisitor() {

          @Override
          public void visitPutField(PutFieldLabel label, Object dst) {
            IField field = (label).getField();
            PointerKey dstPtrKey = (PointerKey) dst;
            // pass barred label since this is for tracked points-to sets
            if (refineFieldAccesses(field, curPk, dstPtrKey, label.bar(), curState)) {
              // x.f = y, X updated
              // if Y' non-empty, then update
              // tracked set of X.f, to trace flow
              // to reads
              PointerKeyAndState storeDst = new PointerKeyAndState(dstPtrKey, curState);
              encounteredStores.add(new StoreEdge(curPkAndState, field, storeDst));
              IntSet trackedSet = find(pkToTrackedSet, storeDst);
              if (!trackedSet.isEmpty()) {
                for (InstanceKeyAndState ikAndState : makeOrdinalSet(find(pkToP2Set, curPkAndState))) {
                  InstanceFieldKeyAndState ifk = getInstFieldKey(ikAndState, field);
                  findOrCreate(instFieldKeyToTrackedSet, ifk).addAll(trackedSet);
                  trackInstanceField(ikAndState, field, backInstKeyToFields);
                }
              }
            }
          }
        };
        g.visitSuccs(curPk, succVisitor);
        handleBackInterproc(curPkAndState, new CopyHandler() {

          @Override
          void handle(PointerKeyAndState src, PointerKey dst, IFlowLabel label) {
            handleBackCopy(src, dst, label);
          }

        }, false);
      }
    }

    /**
     * handle flow from return value to callers, or from actual to formals
     * 
     * @param curPkAndState
     * @param handler
     */
    private void handleBackInterproc(final PointerKeyAndState curPkAndState, final CopyHandler handler, final boolean addGraphs) {
      final PointerKey curPk = curPkAndState.getPointerKey();
      final State curState = curPkAndState.getState();
      // interprocedural edges
      if (curPk instanceof ReturnValueKey) {
        final ReturnValueKey returnKey = (ReturnValueKey) curPk;
        if (DEBUG) {
          System.err.println("return value");
        }
        final CGNode callee = returnKey.getNode();
        if (DEBUG) {
          System.err.println("returning from " + callee);
          // System.err.println("CALL GRAPH:\n" + cg);
          // System.err.println(new
          // Iterator2Collection(cg.getPredNodes(cgNode)));
        }
        final boolean isExceptional = returnKey instanceof ExceptionReturnValueKey;
        // iterate over callers
        for (final CallerSiteContext callSiteAndCGNode : g.getPotentialCallers(returnKey)) {
          final CGNode caller = callSiteAndCGNode.getCaller();
          if (hasNullIR(caller))
            continue;
          final CallSiteReference call = callSiteAndCGNode.getCallSite();
          if (calleeSubGraphMissingAndShouldNotBeAdded(addGraphs, callee, curPkAndState)) {
            continue;
          }
          final ReturnBarLabel returnBarLabel = ReturnBarLabel.make(callSiteAndCGNode);
          doTransition(curState, returnBarLabel, new Function<State, Object>() {

            private void propagateToCaller() {
              // if (caller.getIR() == null) {
              // return;
              // }
              g.addSubgraphForNode(caller);
              SSAAbstractInvokeInstruction[] callInstrs = getCallInstrs(caller, call);
              for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
                PointerKey returnAtCallerKey = heapModel.getPointerKeyForLocal(caller, isExceptional ? callInstr.getException()
                    : callInstr.getDef());
                assert g.containsNode(returnAtCallerKey);
                assert g.containsNode(returnKey);
                handler.handle(curPkAndState, returnAtCallerKey, returnBarLabel);
              }
            }

            @Override
            public Object apply(State callerState) {
              // if (DEBUG) {
              // System.err.println("caller " + caller);
              // }
              SSAAbstractInvokeInstruction[] callInstrs = getCallInstrs(caller, call);
              SSAAbstractInvokeInstruction callInstr = callInstrs[0];
              PointerKey returnAtCallerKey = heapModel.getPointerKeyForLocal(caller, isExceptional ? callInstr.getException()
                  : callInstr.getDef());
              Set<CGNode> possibleTargets = g.getPossibleTargets(caller, call, (LocalPointerKey) returnAtCallerKey);
              if (noOnTheFlyNeeded(callSiteAndCGNode, possibleTargets)) {
                propagateToCaller();
              } else {
                if (callToOTFTargets.get(callSiteAndCGNode).contains(callee.getMethod())) {
                  // already found this target as valid, so do propagation
                  propagateToCaller();
                } else {
                  // if necessary, start a query for the call site
                  queryCallTargets(callSiteAndCGNode, callInstrs, callerState);
                }
              }
              return null;
            }

          });
        }
      }
      if (curPk instanceof LocalPointerKey) {
        LocalPointerKey localPk = (LocalPointerKey) curPk;
        CGNode caller = localPk.getNode();
        // from actual parameter to callee
        for (SSAAbstractInvokeInstruction callInstr : Iterator2Iterable.make(g.getInstrsPassingParam(localPk))) {
          for (int i = 0; i < callInstr.getNumberOfUses(); i++) {
            if (localPk.getValueNumber() != callInstr.getUse(i))
              continue;
            CallSiteReference callSiteRef = callInstr.getCallSite();
            CallerSiteContext callSiteAndCGNode = new CallerSiteContext(caller, callSiteRef);
            // get call targets
            Set<CGNode> possibleCallees = g.getPossibleTargets(caller, callSiteRef, localPk);
            // construct graph for each target
            if (noOnTheFlyNeeded(callSiteAndCGNode, possibleCallees)) {
              for (CGNode callee : possibleCallees) {
                if (calleeSubGraphMissingAndShouldNotBeAdded(addGraphs, callee, curPkAndState)) {
                  continue;
                }
                if (hasNullIR(callee)) {
                  continue;
                }
                g.addSubgraphForNode(callee);
                PointerKey paramVal = heapModel.getPointerKeyForLocal(callee, i + 1);
                assert g.containsNode(paramVal);
                handler.handle(curPkAndState, paramVal, ParamBarLabel.make(callSiteAndCGNode));
              }
            } else {
              if (callToOTFTargets.containsKey(callSiteAndCGNode)) {
                // already queried this call site
                // handle existing targets
                Set<IMethod> targetMethods = callToOTFTargets.get(callSiteAndCGNode);
                for (CGNode callee : possibleCallees) {
                  if (targetMethods.contains(callee.getMethod())) {
                    if (hasNullIR(callee)) {
                      continue;
                    }
                    g.addSubgraphForNode(callee);
                    PointerKey paramVal = heapModel.getPointerKeyForLocal(callee, i + 1);
                    assert g.containsNode(paramVal);
                    handler.handle(curPkAndState, paramVal, ParamBarLabel.make(callSiteAndCGNode));
                  }
                }
              } else {
                // if necessary, raise a query for the call site
                queryCallTargets(callSiteAndCGNode, getCallInstrs(caller, callSiteAndCGNode.getCallSite()), curState);
              }
            }
          }
        }
      }
    }

    /**
     * when doing backward interprocedural propagation, is it true that we should not add a graph representation for a callee _and_
     * that the subgraph for the callee is missing?
     * 
     * @param addGraphs whether graphs should always be added
     * @param callee
     * @param pkAndState
     */
    protected boolean calleeSubGraphMissingAndShouldNotBeAdded(boolean addGraphs, CGNode callee, PointerKeyAndState pkAndState) {
      return !addGraphs && !g.hasSubgraphForNode(callee);
    }

    public void handleTrackedPointsToWorklist() {
      // if (Assertions.verifyAssertions) {
      // Assertions._assert(trackedPointsToWorklist.isEmpty() || refineFields);
      // }
      while (!trackedPointsToWorklist.isEmpty()) {
        incrementNumNodesTraversed();
        final PointerKeyAndState curPkAndState = trackedPointsToWorklist.iterator().next();
        trackedPointsToWorklist.remove(curPkAndState);
        final PointerKey curPk = curPkAndState.getPointerKey();
        final State curState = curPkAndState.getState();
        if (DEBUG)
          System.err.println("tracked points-to " + curPkAndState);
        final MutableIntSet trackedSet = find(pkToTrackedSet, curPkAndState);
        IFlowLabelVisitor succVisitor = new AbstractFlowLabelVisitor() {

          @Override
          public void visitPutField(PutFieldLabel label, Object dst) {
            // statements x.f = y, X' updated, f in map
            // query y; if already queried, add Y to Z.f for all
            // z in X'
            IField field = label.getField();
            PointerKey dstPtrKey = (PointerKey) dst;
            if (refineFieldAccesses(field, curPk, dstPtrKey, label, curState)) {
              for (InstanceKeyAndState ikAndState : makeOrdinalSet(trackedSet)) {
                boolean needField = forwInstKeyToFields.get(ikAndState).contains(field);
                PointerKeyAndState storeDst = new PointerKeyAndState(dstPtrKey, curState);
                encounteredStores.add(new StoreEdge(curPkAndState, field, storeDst));
                if (needField) {
                  if (!addToInitWorklist(storeDst)) {
                    InstanceFieldKeyAndState ifk = getInstFieldKey(ikAndState, field);
                    findOrCreate(instFieldKeyToP2Set, ifk).addAll(find(pkToP2Set, storeDst));
                  }
                }
              }
            }
          }
        };
        g.visitSuccs(curPk, succVisitor);
        IFlowLabelVisitor predVisitor = new AbstractFlowLabelVisitor() {

          @Override
          public void visitAssignGlobal(final AssignGlobalLabel label, Object dst) {
            for (Object o : Iterator2Iterable.make(g.getReadsOfStaticField((StaticFieldKey) dst))) {
              final PointerKey predPk = (PointerKey) o;
              doTransition(curState, AssignGlobalBarLabel.v(), predPkState -> {
                PointerKeyAndState predPkAndState = new PointerKeyAndState(predPk, predPkState);
                handleTrackedPred(trackedSet, predPkAndState, AssignGlobalBarLabel.v());
                return null;
              });
            }
          }

          @Override
          public void visitPutField(PutFieldLabel label, Object dst) {
            IField field = label.getField();
            PointerKey storeBase = (PointerKey) dst;
            // bar label since this is for tracked points-to sets
            if (refineFieldAccesses(field, storeBase, curPk, label.bar(), curState)) {
              PointerKeyAndState storeBaseAndState = new PointerKeyAndState(storeBase, curState);
              encounteredStores.add(new StoreEdge(storeBaseAndState, field, curPkAndState));
              if (!addToInitWorklist(storeBaseAndState)) {
                for (InstanceKeyAndState ikAndState : makeOrdinalSet(find(pkToP2Set, storeBaseAndState))) {
                  InstanceFieldKeyAndState ifk = getInstFieldKey(ikAndState, field);
                  findOrCreate(instFieldKeyToTrackedSet, ifk).addAll(trackedSet);
                  trackInstanceField(ikAndState, field, backInstKeyToFields);
                }
              }
            } else {
              // send to all getfield sources
              for (final PointerKey predPk : Iterator2Iterable.make(g.getReadsOfInstanceField(storeBase, field))) {
                doTransition(curState, MatchBarLabel.v(), predPkState -> {
                  PointerKeyAndState predPkAndState = new PointerKeyAndState(predPk, predPkState);
                  handleTrackedPred(trackedSet, predPkAndState, MatchBarLabel.v());
                  return null;
                });
              }

            }
          }

          @Override
          public void visitGetField(GetFieldLabel label, Object dst) {
            IField field = label.getField();
            PointerKey dstPtrKey = (PointerKey) dst;
            // x = y.f, Y' updated
            // bar label since this is for tracked points-to sets
            if (refineFieldAccesses(field, curPk, dstPtrKey, label.bar(), curState)) {
              for (InstanceKeyAndState ikAndState : makeOrdinalSet(trackedSet)) {
                // tracking value written into ik.field
                boolean needField = backInstKeyToFields.get(ikAndState).contains(field);
                PointerKeyAndState loadedVal = new PointerKeyAndState(dstPtrKey, curState);
                addEncounteredLoad(new LoadEdge(curPkAndState, field, loadedVal));
                if (needField) {
                  InstanceFieldKeyAndState ifk = getInstFieldKey(ikAndState, field);
                  // use an assign bar label with no filter here, since filtering only happens at casts
                  handleTrackedPred(find(instFieldKeyToTrackedSet, ifk), loadedVal, AssignBarLabel.noFilter());
                }
              }
            }
          }

          @Override
          public void visitAssign(final AssignLabel label, Object dst) {
            final PointerKey predPk = (PointerKey) dst;
            doTransition(curState, label.bar(), predPkState -> {
              PointerKeyAndState predPkAndState = new PointerKeyAndState(predPk, predPkState);
              handleTrackedPred(trackedSet, predPkAndState, label.bar());
              return null;
            });
          }

        };
        g.visitPreds(curPk, predVisitor);
        handleBackInterproc(curPkAndState, new CopyHandler() {

          @Override
          void handle(PointerKeyAndState src, final PointerKey dst, final IFlowLabel label) {
            assert src == curPkAndState;
            doTransition(curState, label, dstState -> {
              PointerKeyAndState dstAndState = new PointerKeyAndState(dst, dstState);
              handleTrackedPred(trackedSet, dstAndState, label);
              return null;
            });
          }

        }, true);
      }
    }

    private void addEncounteredLoad(LoadEdge loadEdge) {
      if (encounteredLoads.add(loadEdge)) {
        // if (DEBUG) {
        // System.err.println("encountered load edge " + loadEdge);
        // }
      }
    }

    public void makePassOverFieldStmts() {
      for (StoreEdge storeEdge : encounteredStores) {
        PointerKeyAndState storedValAndState = storeEdge.val;
        IField field = storeEdge.field;
        PointerKeyAndState baseAndState = storeEdge.base;
        // x.f = y, X' updated
        // for each z in X' such that f in z's map,
        // add Y to Z.f
        IntSet trackedSet = find(pkToTrackedSet, baseAndState);
        for (InstanceKeyAndState ikAndState : makeOrdinalSet(trackedSet)) {
          if (forwInstKeyToFields.get(ikAndState).contains(field)) {
            if (!addToInitWorklist(storedValAndState)) {
              InstanceFieldKeyAndState ifk = getInstFieldKey(ikAndState, field);
              findOrCreate(instFieldKeyToP2Set, ifk).addAll(find(pkToP2Set, storedValAndState));
            }
          }
        }
      }
      for (LoadEdge loadEdge : encounteredLoads) {
        PointerKeyAndState loadedValAndState = loadEdge.val;
        IField field = loadEdge.field;
        PointerKey basePointerKey = loadEdge.base.getPointerKey();
        State loadDstState = loadedValAndState.getState();
        PointerKeyAndState baseAndStateToHandle = new PointerKeyAndState(basePointerKey, loadDstState);
        boolean basePointerOkay = pointsToQueried.get(basePointerKey).contains(loadDstState)
            || !pointsToQueried.get(loadedValAndState.getPointerKey()).contains(loadDstState)
            || initWorklist.contains(loadedValAndState);
        // if (!basePointerOkay) {
        // System.err.println("ASSERTION WILL FAIL");
        // System.err.println("QUERIED: " + queriedPkAndStates);
        // }
        if (!basePointerOkay) {
          // remove this assertion, since we now allow multiple queries --MS
          // Assertions._assert(false, "queried " + loadedValAndState + " but not " + baseAndStateToHandle);
        }
        final IntSet curP2Set = find(pkToP2Set, baseAndStateToHandle);
        // int startSize = curP2Set.size();
        // int curSize = -1;
        for (InstanceKeyAndState ikAndState : makeOrdinalSet(curP2Set)) {
          // curSize = curP2Set.size();
          // if (Assertions.verifyAssertions) {
          // Assertions._assert(startSize == curSize);
          // }
          InstanceFieldKeyAndState ifk = getInstFieldKey(ikAndState, field);
          // make sure we've actually queried the def'd val before adding to its points-to set
          if (pointsToQueried.get(loadedValAndState.getPointerKey()).contains(loadedValAndState.getState())) {
            // just pass no label assign filter since no type-based filtering can be
            // done here
            if (addAllToP2Set(pkToP2Set, loadedValAndState, find(instFieldKeyToP2Set, ifk), AssignLabel.noFilter())) {
              if (DEBUG) {
                System.err.println("from load edge " + loadEdge);
              }
              addToPToWorklist(loadedValAndState);
            }
          }
        }
        // }
        // x = y.f, Y' updated
        PointerKeyAndState baseAndState = loadEdge.base;
        for (InstanceKeyAndState ikAndState : makeOrdinalSet(find(pkToTrackedSet, baseAndState))) {
          if (backInstKeyToFields.get(ikAndState).contains(field)) {
            // tracking value written into ik.field
            InstanceFieldKeyAndState ifk = getInstFieldKey(ikAndState, field);
            if (findOrCreate(pkToTrackedSet, loadedValAndState).addAll(find(instFieldKeyToTrackedSet, ifk))) {
              if (DEBUG) {
                System.err.println("from load edge " + loadEdge);
              }
              addToTrackedPToWorklist(loadedValAndState);
            }
          }
        }
      }
    }

    private InstanceFieldKeyAndState getInstFieldKey(InstanceKeyAndState ikAndState, IField field) {
      return new InstanceFieldKeyAndState(new InstanceFieldKey(ikAndState.getInstanceKey(), field), ikAndState.getState());
    }

    protected <K> MutableIntSet findOrCreate(Map<K, MutableIntSet> M, K key) {
      MutableIntSet result = M.get(key);
      if (result == null) {
        result = intSetFactory.make();
        M.put(key, result);
      }
      return result;
    }

    private final MutableIntSet emptySet = intSetFactory.make();

    protected <K> MutableIntSet find(Map<K, MutableIntSet> M, K key) {
      MutableIntSet result = M.get(key);
      if (result == null) {
        result = emptySet;
      }
      return result;
    }

    /**
     * Handle a predecessor when processing some tracked locations
     * 
     * @param curTrackedSet the tracked locations
     * @param predPkAndState the predecessor
     */
    protected boolean handleTrackedPred(final MutableIntSet curTrackedSet, PointerKeyAndState predPkAndState, IFlowLabel label) {
      if (addAllToP2Set(pkToTrackedSet, predPkAndState, curTrackedSet, label)) {
        addToTrackedPToWorklist(predPkAndState);
        return true;
      }
      return false;
    }
  }

  private static SSAAbstractInvokeInstruction[] getCallInstrs(CGNode node, CallSiteReference site) {
    return node.getIR().getCalls(site);
  }

  private static boolean hasNullIR(CGNode node) {
    boolean ret = node.getMethod().isNative();
    assert node.getIR() != null || ret;
    return ret;
  }

  private Object doTransition(State curState, IFlowLabel label, Function<State, Object> func) {
    State nextState = stateMachine.transition(curState, label);
    Object ret = null;
    if (nextState != StateMachine.ERROR) {
      ret = func.apply(nextState);
    } else {
      // System.err.println("filtered at edge " + label);
    }
    return ret;
  }

  public StateMachineFactory<IFlowLabel> getStateMachineFactory() {
    return stateMachineFactory;
  }

  public void setStateMachineFactory(StateMachineFactory<IFlowLabel> stateMachineFactory) {
    this.stateMachineFactory = stateMachineFactory;
  }

  public RefinementPolicyFactory getRefinementPolicyFactory() {
    return refinementPolicyFactory;
  }

  public void setRefinementPolicyFactory(RefinementPolicyFactory refinementPolicyFactory) {
    this.refinementPolicyFactory = refinementPolicyFactory;
  }

  /**
   * we are looking for an instance key flowing to pk that violates pred.
   * 
   * @param pk
   * @param pred
   * @param pa
   */
  @SuppressWarnings("unused")
  private boolean doTopLevelTraversal(PointerKey pk, final Predicate<InstanceKey> pred, final PointsToComputer ptoComputer,
      PointerAnalysis<InstanceKey> pa) {
    final Set<PointerKeyAndState> visited = HashSetFactory.make();
    final LinkedList<PointerKeyAndState> worklist = new LinkedList<>();

    class Helper {

      /**
       * cache of the targets discovered for a call site during on-the-fly call graph construction
       */
      private final MultiMap<CallerSiteContext, IMethod> callToOTFTargets = ArraySetMultiMap.make();

      void propagate(PointerKeyAndState pkAndState) {
        if (visited.add(pkAndState)) {
          assert graphContainsNode(pkAndState.getPointerKey());
          worklist.addLast(pkAndState);
        }
      }

      private boolean graphContainsNode(PointerKey pointerKey) {
        if (pointerKey instanceof LocalPointerKey) {
          LocalPointerKey lpk = (LocalPointerKey) pointerKey;
          return g.hasSubgraphForNode(lpk.getNode());
        }
        return true;
      }

      private Collection<IMethod> getOTFTargets(CallerSiteContext callSiteAndCGNode, SSAAbstractInvokeInstruction[] callInstrs,
          State callerState) {
        if (DEBUG_TOPLEVEL) {
          System.err.println("toplevel refining call site " + callSiteAndCGNode);
        }
        final CallSiteReference call = callSiteAndCGNode.getCallSite();
        final CGNode caller = callSiteAndCGNode.getCaller();
        Collection<IMethod> result = HashSetFactory.make();
        for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
          PointerKey thisArg = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(0));
          PointerKeyAndState thisArgAndState = new PointerKeyAndState(thisArg, callerState);
          OrdinalSet<InstanceKeyAndState> thisPToSet = getPToSetFromComputer(ptoComputer, thisArgAndState);
          for (InstanceKeyAndState ikAndState : thisPToSet) {
            InstanceKey ik = ikAndState.getInstanceKey();
            IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, call, ik.getConcreteType());
            if (targetMethod == null) {
              // NOTE: target method can be null because we don't
              // always have type filters
              continue;
            }
            result.add(targetMethod);
          }
        }
        return result;
      }

      public void handleTopLevelForwInterproc(PointerKeyAndState curPkAndState) {
        PointerKey curPk = curPkAndState.getPointerKey();
        final State curState = curPkAndState.getState();
        if (curPk instanceof LocalPointerKey) {
          final LocalPointerKey localPk = (LocalPointerKey) curPk;
          if (g.isParam(localPk)) {
            // System.err.println("at param");
            final CGNode callee = localPk.getNode();
            final int paramPos = localPk.getValueNumber() - 1;
            for (final CallerSiteContext callSiteAndCGNode : g.getPotentialCallers(localPk)) {
              final CGNode caller = callSiteAndCGNode.getCaller();
              final CallSiteReference call = callSiteAndCGNode.getCallSite();
              // final IR ir = getIR(caller);
              if (hasNullIR(caller))
                continue;
              final ParamLabel paramLabel = ParamLabel.make(callSiteAndCGNode);
              doTransition(curPkAndState.getState(), paramLabel, new Function<State, Object>() {

                private void propagateToCallee() {
                  // if (caller.getIR() == null) {
                  // return;
                  // }
                  g.addSubgraphForNode(caller);
                  SSAAbstractInvokeInstruction[] callInstrs = getCallInstrs(caller, call);
                  for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
                    final PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
                    assert g.containsNode(actualPk);
                    assert g.containsNode(localPk);
                    doTransition(curState, paramLabel, nextState -> {
                      propagate(new PointerKeyAndState(actualPk, nextState));
                      return null;
                    });
                  }
                }

                @Override
                public Object apply(State callerState) {
                  // hack to get some actual parameter from call site
                  // TODO do this better
                  SSAAbstractInvokeInstruction[] callInstrs = getCallInstrs(caller, call);
                  SSAAbstractInvokeInstruction callInstr = callInstrs[0];
                  PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
                  Set<CGNode> possibleTargets = g.getPossibleTargets(caller, call, (LocalPointerKey) actualPk);
                  if (noOnTheFlyNeeded(callSiteAndCGNode, possibleTargets)) {
                    propagateToCallee();
                  } else {
                    Collection<IMethod> otfTargets = getOTFTargets(callSiteAndCGNode, callInstrs, callerState);
                    if (otfTargets.contains(callee.getMethod())) {
                      // already found this target as valid, so do propagation
                      propagateToCallee();
                    }
                  }
                  return null;
                }

              });

            }
          }
          SSAAbstractInvokeInstruction callInstr = g.getInstrReturningTo(localPk);
          if (callInstr != null) {
            CGNode caller = localPk.getNode();
            boolean isExceptional = localPk.getValueNumber() == callInstr.getException();

            CallSiteReference callSiteRef = callInstr.getCallSite();
            CallerSiteContext callSiteAndCGNode = new CallerSiteContext(caller, callSiteRef);
            // get call targets
            Set<CGNode> possibleCallees = g.getPossibleTargets(caller, callSiteRef, localPk);
            if (noOnTheFlyNeeded(callSiteAndCGNode, possibleCallees)) {
              for (CGNode callee : possibleCallees) {
                if (hasNullIR(callee)) {
                  continue;
                }
                g.addSubgraphForNode(callee);
                final PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
                    .getPointerKeyForReturnValue(callee);
                assert g.containsNode(retVal);
                doTransition(curState, ReturnLabel.make(callSiteAndCGNode), nextState -> {
                  propagate(new PointerKeyAndState(retVal, nextState));
                  return null;
                });
              }
            } else {
              Collection<IMethod> otfTargets = getOTFTargets(callSiteAndCGNode, getCallInstrs(caller, callSiteAndCGNode
                  .getCallSite()), curPkAndState.getState());
              for (CGNode callee : possibleCallees) {
                if (otfTargets.contains(callee.getMethod())) {
                  if (hasNullIR(callee)) {
                    continue;
                  }
                  g.addSubgraphForNode(callee);
                  final PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
                      .getPointerKeyForReturnValue(callee);
                  assert g.containsNode(retVal);
                  doTransition(curState, ReturnLabel.make(callSiteAndCGNode), nextState -> {
                    propagate(new PointerKeyAndState(retVal, nextState));
                    return null;
                  });
                }
              }
            }
          }
        }

      }

      private OrdinalSet<InstanceKeyAndState> getPToSetFromComputer(final PointsToComputer ptoComputer,
          PointerKeyAndState pointerKeyAndState) {
        // make sure relevant constraints have been added
        if (pointerKeyAndState.getPointerKey() instanceof LocalPointerKey) {
          LocalPointerKey lpk = (LocalPointerKey) pointerKeyAndState.getPointerKey();
          g.addSubgraphForNode(lpk.getNode());
        }
        // add pointerKeyAndState to init worklist
        ptoComputer.addToInitWorklist(pointerKeyAndState);
        // run worklist algorithm
        ptoComputer.worklistLoop();
        // suck out the points-to set
        final MutableIntSet intP2Set = ptoComputer.pkToP2Set.get(pointerKeyAndState);
        if (intP2Set == null) {
          // null if empty p2set
          return OrdinalSet.empty();
        } else {
          return ptoComputer.makeOrdinalSet(intP2Set);
        }
      }

      private void computeFlowsTo(PointsToComputer ptoComputer, OrdinalSet<InstanceKeyAndState> basePToSet) {
        for (InstanceKeyAndState ikAndState : basePToSet) {
          ptoComputer.addPredsOfIKeyAndStateToTrackedPointsTo(ikAndState);
        }
        // run worklist loop
        assert ptoComputer.initWorklist.isEmpty();
        assert ptoComputer.pointsToWorklist.isEmpty();
        ptoComputer.worklistLoop();
      }

      private Collection<State> getFlowedToStates(PointsToComputer ptoComputer, OrdinalSet<InstanceKeyAndState> basePToSet,
          PointerKey putfieldBase) {
        Collection<State> result = HashSetFactory.make();
        Set<State> trackedStates = ptoComputer.trackedQueried.get(putfieldBase);
        for (State trackedState : trackedStates) {
          PointerKeyAndState pkAndState = new PointerKeyAndState(putfieldBase, trackedState);
          if (ptoComputer.makeOrdinalSet(ptoComputer.pkToTrackedSet.get(pkAndState)).containsAny(basePToSet)) {
            result.add(trackedState);
          }
        }
        // for (PointerKeyAndState pkAndState : ptoComputer.pkToTrackedSet.keySet()) {
        // if (pkAndState.getPointerKey().equals(putfieldBase)) {
        // if (ptoComputer.makeOrdinalSet(ptoComputer.pkToTrackedSet.get(pkAndState)).containsAny(basePToSet)) {
        // result.add(pkAndState.getState());
        // }
        // }
        // }
        return result;
      }

    }
    final Helper h = new Helper();
    PointerKeyAndState initPkAndState = new PointerKeyAndState(pk, stateMachine.getStartState());
    if (pk instanceof LocalPointerKey) {
      g.addSubgraphForNode(((LocalPointerKey) pk).getNode());
    }
    h.propagate(initPkAndState);
    while (!worklist.isEmpty()) {
      incrementNumNodesTraversed();
      PointerKeyAndState curPkAndState = worklist.removeFirst();
      final PointerKey curPk = curPkAndState.getPointerKey();
      final State curState = curPkAndState.getState();
      // if predicate holds for pre-computed points-to set of curPk, we are done
      if (DEBUG_TOPLEVEL) {
        System.err.println("toplevel pkAndState " + curPkAndState);
      }
      if (predHoldsForPk(curPk, pred, pa)) {
        if (DEBUG_TOPLEVEL) {
          System.err.println("predicate holds");
        }
        continue;
      }
      // otherwise, traverse new, assign, assign global, param, return, match edges
      class MyFlowLabelVisitor extends AbstractFlowLabelVisitor {

        boolean foundBadInstanceKey;

        @Override
        public void visitNew(NewLabel label, Object dst) {
          // TODO Auto-generated method stub

          final InstanceKey ik = (InstanceKey) dst;
          if (DEBUG_TOPLEVEL) {
            System.err.println("toplevel alloc " + ik + " assigned to " + curPk);
          }
          doTransition(curState, label, newState -> {
            // just check if ik violates the pred
            if (!pred.test(ik)) {
              foundBadInstanceKey = true;
            }
            return null;
          });
        }

        @Override
        public void visitGetField(GetFieldLabel label, Object dst) {
          IField field = (label).getField();
          PointerKey loadBase = (PointerKey) dst;
          if (refineFieldAccesses(field, loadBase, curPk, label, curState)) {
            if (DEBUG_TOPLEVEL) {
              System.err.println("toplevel refining for read of " + field);
            }
            // find points-to set of base pointer
            OrdinalSet<InstanceKeyAndState> basePToSet = h.getPToSetFromComputer(ptoComputer, new PointerKeyAndState(loadBase,
                curState));
            if (DEBUG_TOPLEVEL) {
              System.err.println("toplevel base pointer p2set " + basePToSet);
            }
            // find "flows-to sets" of pointed-to instance keys
            h.computeFlowsTo(ptoComputer, basePToSet);
            if (DEBUG_TOPLEVEL) {
              System.err.println("toplevel finished computing flows to");
            }
            // for each putfield base pointer, if flowed-to, then propagate written pointer key
            for (MemoryAccess fieldWrite : getWrites(field, loadBase)) {
              Collection<Pair<PointerKey, PointerKey>> baseAndStoredPairs = getBaseAndStored(fieldWrite, field);
              if (baseAndStoredPairs == null) {
                continue;
              }
              for (Pair<PointerKey, PointerKey> p : baseAndStoredPairs) {
                PointerKey base = p.fst;
                PointerKey stored = p.snd;
                Collection<State> reachedFlowStates = h.getFlowedToStates(ptoComputer, basePToSet, base);
                for (State nextState : reachedFlowStates) {
                  if (DEBUG_TOPLEVEL) {
                    System.err.println("toplevel alias with base " + base + " in state " + nextState);
                  }
                  h.propagate(new PointerKeyAndState(stored, nextState));
                }
              }
            }
          } else { // use match edges
            for (final PointerKey writtenPk : Iterator2Iterable.make(g.getWritesToInstanceField(loadBase, field))) {
              doTransition(curState, MatchLabel.v(), nextState -> {
                h.propagate(new PointerKeyAndState(writtenPk, nextState));
                return null;
              });
            }
          }
        }

        private Collection<Pair<PointerKey, PointerKey>> getBaseAndStored(MemoryAccess fieldWrite, IField field) {
          final CGNode node = fieldWrite.getNode();
          // an optimization; if node is not represented in our constraint graph, then we could not possibly
          // have discovered flow to the base pointer
          if (!g.hasSubgraphForNode(node)) {
            return null;
          }
          IR ir = node.getIR();
          PointerKey base = null, stored = null;
          if (field == ArrayContents.v()) {
            final SSAInstruction instruction = ir.getInstructions()[fieldWrite.getInstructionIndex()];
            if (instruction == null) {
              return null;
            }
            if (instruction instanceof SSANewInstruction) {
              return DemandPointerFlowGraph.getInfoForNewMultiDim((SSANewInstruction) instruction, heapModel, fieldWrite.getNode()).arrStoreInstrs;
            }
            SSAArrayStoreInstruction s = (SSAArrayStoreInstruction) instruction;
            base = heapModel.getPointerKeyForLocal(fieldWrite.getNode(), s.getArrayRef());
            stored = heapModel.getPointerKeyForLocal(fieldWrite.getNode(), s.getValue());
          } else {
            SSAPutInstruction s = (SSAPutInstruction) ir.getInstructions()[fieldWrite.getInstructionIndex()];
            if (s == null) {
              return null;
            }
            base = heapModel.getPointerKeyForLocal(fieldWrite.getNode(), s.getRef());
            stored = heapModel.getPointerKeyForLocal(fieldWrite.getNode(), s.getVal());
          }
          return Collections.singleton(Pair.make(base, stored));
        }

        private Collection<MemoryAccess> getWrites(IField field, PointerKey loadBase) {
          final PointerKey convertedBase = convertToHeapModel(loadBase, mam.getHeapModel());
          if (field == ArrayContents.v()) {
            return mam.getArrayWrites(loadBase);
          } else {
            return mam.getFieldWrites(convertedBase, field);
          }
        }

        @Override
        public void visitAssignGlobal(AssignGlobalLabel label, Object dst) {
          for (Object writeToStaticField : Iterator2Iterable.make(g.getWritesToStaticField((StaticFieldKey) dst))) {
            final PointerKey writtenPk = (PointerKey) writeToStaticField;
            doTransition(curState, label, nextState -> {
              h.propagate(new PointerKeyAndState(writtenPk, nextState));
              return null;
            });

          }
        }

        @Override
        public void visitAssign(AssignLabel label, Object dst) {
          final PointerKey succPk = (PointerKey) dst;
          doTransition(curState, label, nextState -> {
            h.propagate(new PointerKeyAndState(succPk, nextState));
            return null;
          });
        }

      }
      MyFlowLabelVisitor v = new MyFlowLabelVisitor();
      g.visitSuccs(curPk, v);
      if (v.foundBadInstanceKey) {
        // found an instance key violating the pred
        return false;
      }
      h.handleTopLevelForwInterproc(curPkAndState);
    }
    return true;
  }

  private static boolean predHoldsForPk(PointerKey curPk, Predicate<InstanceKey> pred, PointerAnalysis<InstanceKey> pa) {
    PointerKey curPkForPAHeapModel = convertToHeapModel(curPk, pa.getHeapModel());
    OrdinalSet<InstanceKey> pointsToSet = pa.getPointsToSet(curPkForPAHeapModel);
    for (InstanceKey ik : pointsToSet) {
      if (!pred.test(ik)) {
        return false;
      }
    }
    return true;
  }

  private static PointerKey convertToHeapModel(PointerKey curPk, HeapModel heapModel) {
    return AbstractFlowGraph.convertPointerKeyToHeapModel(curPk, heapModel);
  }

  private boolean refineFieldAccesses(IField field, PointerKey basePtr, PointerKey val, IFlowLabel label, State state) {
    boolean shouldRefine = refinementPolicy.getFieldRefinePolicy().shouldRefine(field, basePtr, val, label, state);
    if (DEBUG) {
      if (shouldRefine) {
        System.err.println("refining access to " + field);
      } else {
        System.err.println("using match for access to " + field);
      }
    }
    return shouldRefine;
  }

  private boolean noOnTheFlyNeeded(CallerSiteContext call, Set<CGNode> possibleTargets) {
    // NOTE: if we want to be more precise for queries in dead code,
    // we shouldn't rely on possibleTargets here (since there may be
    // zero targets)
    if (!refinementPolicy.getCallGraphRefinePolicy().shouldRefine(call)) {
      return true;
    }
    // here we compute the number of unique *method* targets, as opposed to call graph nodes.
    // if we have a context-sensitive call graph, with many targets representing clones of
    // the same method, we don't want to count the clones twice
    Set<IMethod> methodTargets = new HashSet<>();
    for (CGNode node : possibleTargets) {
      methodTargets.add(node.getMethod());
    }
    return methodTargets.size() <= 1;
  }

  /**
   * used to compute "flows-to sets," i.e., all the pointers that can point to some instance key
   * 
   */
  protected class FlowsToComputer extends PointsToComputer {

    private final InstanceKeyAndState queriedIkAndState;

    private final int queriedIkAndStateNum;

    /**
     * holds the desired flows-to set
     */
    private final Collection<PointerKeyAndState> theFlowsToSet = HashSetFactory.make();

    public FlowsToComputer(InstanceKeyAndState ikAndState) {
      this.queriedIkAndState = ikAndState;
      this.queriedIkAndStateNum = ikAndStates.add(queriedIkAndState);
    }

    @Override
    protected void compute() {
      // seed the points-to worklist

      InstanceKey ik = queriedIkAndState.getInstanceKey();
      g.addSubgraphForNode(((InstanceKeyWithNode) ik).getNode());
      for (Object pred : Iterator2Iterable.make(g.getPredNodes(ik, NewLabel.v()))) {
        PointerKey predPk = (PointerKey) pred;
        PointerKeyAndState predPkAndState = new PointerKeyAndState(predPk, queriedIkAndState.getState());
        theFlowsToSet.add(predPkAndState);
        findOrCreate(pkToTrackedSet, predPkAndState).add(queriedIkAndStateNum);
        addToTrackedPToWorklist(predPkAndState);
      }
      worklistLoop();
    }

    public Collection<PointerKeyAndState> getComputedFlowsToSet() {
      return theFlowsToSet;
    }

    /**
     * also update the flows-to set of interest if necessary
     */
    @Override
    protected boolean handleTrackedPred(MutableIntSet curTrackedSet, PointerKeyAndState predPkAndState, IFlowLabel label) {
      boolean result = super.handleTrackedPred(curTrackedSet, predPkAndState, label);
      if (result && find(pkToTrackedSet, predPkAndState).contains(queriedIkAndStateNum)) {
        theFlowsToSet.add(predPkAndState);
      }
      return result;
    }

  }

}
