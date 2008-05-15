/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright ©2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient’s reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents’ employees.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.alg.statemachine.StatesMergedException;
import com.ibm.wala.demandpa.alg.statemachine.StateMachine.State;
import com.ibm.wala.demandpa.flowgraph.AbstractFlowLabelVisitor;
import com.ibm.wala.demandpa.flowgraph.AssignBarLabel;
import com.ibm.wala.demandpa.flowgraph.AssignGlobalBarLabel;
import com.ibm.wala.demandpa.flowgraph.AssignGlobalLabel;
import com.ibm.wala.demandpa.flowgraph.AssignLabel;
import com.ibm.wala.demandpa.flowgraph.DemandPointerFlowGraph;
import com.ibm.wala.demandpa.flowgraph.GetFieldLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowGraph;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowLabelWithFilter;
import com.ibm.wala.demandpa.flowgraph.MatchBarLabel;
import com.ibm.wala.demandpa.flowgraph.MatchLabel;
import com.ibm.wala.demandpa.flowgraph.NewLabel;
import com.ibm.wala.demandpa.flowgraph.ParamBarLabel;
import com.ibm.wala.demandpa.flowgraph.ParamLabel;
import com.ibm.wala.demandpa.flowgraph.PutFieldLabel;
import com.ibm.wala.demandpa.flowgraph.ReturnBarLabel;
import com.ibm.wala.demandpa.flowgraph.ReturnLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.demandpa.genericutil.ArraySet;
import com.ibm.wala.demandpa.genericutil.ArraySetMultiMap;
import com.ibm.wala.demandpa.genericutil.HashSetMultiMap;
import com.ibm.wala.demandpa.genericutil.MultiMap;
import com.ibm.wala.demandpa.genericutil.Predicate;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.demandpa.util.CallSiteAndCGNode;
import com.ibm.wala.demandpa.util.MemoryAccess;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.PointerParamValueNumIterator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.SingleClassFilter;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.SingleInstanceFilter;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.TypeFilter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ExceptionReturnValueKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableIntSetFactory;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Demand-driven refinement-based points-to analysis.
 * 
 * @author Manu Sridharan
 * 
 */
public class DemandRefinementPointsTo extends AbstractDemandPointsTo {

  private static final boolean DEBUG = false;

  private static final boolean PARANOID = false;

  // private static final boolean DEBUG_FULL = DEBUG && false;

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

  public DemandRefinementPointsTo(CallGraph cg, ThisFilteringHeapModel model, MemoryAccessMap mam, ClassHierarchy cha,
      AnalysisOptions options, StateMachineFactory<IFlowLabel> stateMachineFactory) {
    this(cg, model, mam, cha, options, stateMachineFactory, new DemandPointerFlowGraph(cg, model, mam, cha));
  }

  public DemandRefinementPointsTo(CallGraph cg, ThisFilteringHeapModel model, MemoryAccessMap fam, ClassHierarchy cha,
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
        for (Iterator<? extends CGNode> predNodes = cg.getPredNodes(callee); predNodes.hasNext();) {
          CGNode caller = predNodes.next();
          for (Iterator<CallSiteReference> iterator = cg.getPossibleSites(caller, callee); iterator.hasNext();) {
            CallSiteReference site = iterator.next();
            try {
              caller.getIR().getCalls(site);
            } catch (IllegalArgumentException e) {
              System.err.println(caller + " is pred of " + callee);
              System.err.println("no calls at site " + site);
              System.err.println(caller.getIR());
              if (caller.getMethod() instanceof ShrikeBTMethod) {
                try {
                  Instruction[] instructions = ((ShrikeBTMethod) caller.getMethod()).getInstructions();
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
     * The {@link RefinementPolicy} indicated that no more refinement was possible
     */
    NOMOREREFINE,
    /**
     * The budget specified in the {@link RefinementPolicy} was exceeded
     */
    BUDGETEXCEEDED
  };

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
   * @return a pair consisting of (1) a {@link PointsToResult} indicating whether a points-to set satisfying the
   *         predicate was computed, and (2) the last computed points-to set for the variable (possibly
   *         <code>null</code> if no points-to set could be computed in the budget)
   * @throws IllegalArgumentException if <code>pk</code> is not a {@link LocalPointerKey}; to eventually be fixed
   */
  public Pair<PointsToResult, Collection<InstanceKey>> getPointsTo(PointerKey pk, Predicate<InstanceKey> ikeyPred)
      throws IllegalArgumentException {
    if (!(pk instanceof com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)) {
      throw new IllegalArgumentException("only locals for now");
    }
    LocalPointerKey queriedPk = (LocalPointerKey) pk;
    if (DEBUG) {
      System.err.println("answering query for " + pk);
    }
    Collection<InstanceKey> lastP2Set = null;
    boolean succeeded = false;
    startNewQuery();
    int numPasses = refinementPolicy.getNumPasses();
    int passNum = 0;
    for (; passNum < numPasses; passNum++) {
      setNumNodesTraversed(0);
      setTraversalBudget(refinementPolicy.getBudgetForPass(passNum));
      Collection<InstanceKey> curP2Set = null;
      PointsToComputer computer = null;
      boolean completedPassInBudget = false;
      try {
        while (true) {
          try {
            computer = new PointsToComputer(queriedPk);
            computer.compute();
            curP2Set = computer.getP2Set(queriedPk);
//            System.err.println("completed pass");
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
          assert lastP2Set.containsAll(curP2Set);
          lastP2Set = curP2Set;
        } else {
          // new set size is >= lastP2Set, so don't update
          assert curP2Set.containsAll(lastP2Set);
        }
        if (curP2Set.isEmpty() || passesPred(curP2Set, ikeyPred)) {
          // we did it!
//          if (curP2Set.isEmpty()) {
//            System.err.println("EMPTY PTO SET");
//          }
          succeeded = true;
          break;
        } else if (completedPassInBudget) {
//          if (computer.isHopeless(ikeyPred)) {
//            break;
//          }
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
      result = PointsToResult.NOMOREREFINE;
    }
    return Pair.make(result, lastP2Set);
  }

  private boolean passesPred(Collection<InstanceKey> p2set, Predicate<InstanceKey> ikeyPred) {
    for (InstanceKey ik : p2set) {
      if (!ikeyPred.test(ik)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return the points-to set of <code>pk</code>, or <code>null</code> if the points-to set can't be computed in
   *         the allocated budget
   */
  public Collection<InstanceKey> getPointsTo(PointerKey pk) {
    return getPointsTo(pk, Predicate.<InstanceKey> falsePred()).snd;
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
     * set of variables whose points-to sets were queried
     */
    private final MultiMap<PointerKey, State> pointsToQueried = HashSetMultiMap.make();

    /**
     * forward worklist: for initially processing points-to queries
     */
    private final Collection<PointerKeyAndState> initWorklist = new LinkedHashSet<PointerKeyAndState>();

    /**
     * worklist for variables whose points-to set has been updated
     */
    private final Collection<PointerKeyAndState> pointsToWorklist = new LinkedHashSet<PointerKeyAndState>();

    /**
     * worklist for variables whose tracked points-to set has been updated
     */
    private final Collection<PointerKeyAndState> trackedPointsToWorklist = new LinkedHashSet<PointerKeyAndState>();

    /**
     * maps a pointer key to those on-the-fly virtual calls for which it is the receiver
     */
    private final MultiMap<PointerKeyAndState, CallSiteAndCGNode> pkToOTFCalls = HashSetMultiMap.make();

    /**
     * cache of the targets discovered for a call site during on-the-fly call graph construction
     */
    private final MultiMap<CallSiteAndCGNode, IMethod> callToOTFTargets = ArraySetMultiMap.make();

    // alloc nodes to the fields we're looking to match on them,
    // matching getfield with putfield
    private final MultiMap<InstanceKeyAndState, IField> forwInstKeyToFields = HashSetMultiMap.make();

    // matching putfield_bar with getfield_bar
    private final MultiMap<InstanceKeyAndState, IField> backInstKeyToFields = HashSetMultiMap.make();

    // points-to sets and tracked points-to sets
    private final Map<PointerKeyAndState, MutableIntSet> pkToP2Set = HashMapFactory.make();

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

    protected PointsToComputer(PointerKey pk) {
      queriedPkAndState = new PointerKeyAndState(pk, stateMachine.getStartState());
    }

    /**
     * Assumes PointsToComputer has been run and it computed a points-to set within budget (i.e., without early
     * termination.) Further assuming that all variables are initialized, can further refinement possibly allow pred to
     * be satisfied? NOTE: a heuristic.
     */
    public boolean isHopeless(Predicate<InstanceKey> pred) {
      final Set<PointerKeyAndState> visited = HashSetFactory.make();
      final Collection<PointerKeyAndState> worklist = new LinkedHashSet<PointerKeyAndState>();
      visited.add(queriedPkAndState);
      worklist.add(queriedPkAndState);
      class CopyFunction implements Function<State, Object> {

        private final PointerKey dstPk;

        public Object apply(State succState) {
          PointerKeyAndState dstPkAndState = new PointerKeyAndState(dstPk, succState);
          if (visited.add(dstPkAndState)) {
            worklist.add(dstPkAndState);
          }
          return null;
        }

        public CopyFunction(PointerKey dstPk) {
          this.dstPk = dstPk;
        }

      }
      while (!worklist.isEmpty()) {
        PointerKeyAndState curPkAndState = worklist.iterator().next();
        worklist.remove(curPkAndState);
        OrdinalSet<InstanceKeyAndState> p2set = makeOrdinalSet(find(pkToP2Set, curPkAndState));
//        System.err.println("checking " + curPkAndState);
//        System.err.println("p2set " + p2set);
        // want to check if *all* instance keys in points-to set fail the pred
        boolean allFail = true;
        for (InstanceKeyAndState ikAndState : p2set) {
          if (pred.test(ikAndState.getInstanceKey())) {
            allFail = false;
            break;
          }
        }
        // TODO think about this more!!! I think it's safe; want to say hopeless to much
        if (allFail && !p2set.isEmpty()) {
          System.err.println("HOPELESS due to " + curPkAndState);
          return true;
        }
        // add successors to worklist
        final PointerKey curPk = curPkAndState.getPointerKey();
        final State curState = curPkAndState.getState();
        IFlowLabelVisitor v = new AbstractFlowLabelVisitor() {

          @Override
          public void visitGetField(GetFieldLabel label, Object dst) {
            IField field = (label).getField();
            PointerKey loadBase = (PointerKey) dst;
            if (refineFieldAccesses(field, loadBase, curPk, label, curState)) {
              MutableIntSet loadBaseP2Set = find(pkToP2Set, new PointerKeyAndState(loadBase, curState));
              // for each write to field
              for (Pair<PointerKey, PointerKey> pair : getBaseAndStoredPointersOfWrites(loadBase, field)) {
                PointerKey basePk = pair.fst;
                PointerKey storedPk = pair.snd;
                // if ikey and state is in tracked set of base pointer + some state of write,
                // continue with written var and state
                for (PointerKeyAndState pkAndState : pkToTrackedSet.keySet()) {
                  if (basePk.equals(pkAndState.getPointerKey())) {
                    if (pkToTrackedSet.get(pkAndState).containsAny(loadBaseP2Set)) {
                      // bingo! continue from stored pk
                      PointerKeyAndState storedPkAndState = new PointerKeyAndState(storedPk, pkAndState.getState());
                      if (visited.add(storedPkAndState)) {
                        worklist.add(storedPkAndState);
                      }
                    }
                  }
                }
              }
            } else {
              // can't follow match edges, since they may get filtered out with more refinement
            }

          }

          @Override
          public void visitAssignGlobal(AssignGlobalLabel label, Object dst) {
            for (Iterator<? extends Object> writeIter = g.getWritesToStaticField((StaticFieldKey) dst); writeIter.hasNext();) {
              PointerKey dstPk = (PointerKey) writeIter.next();
              doTransition(curState, label, new CopyFunction(dstPk));
            }

          }

          @Override
          public void visitAssign(AssignLabel label, Object dst) {
            final PointerKey dstPk = (PointerKey) dst;
            doTransition(curState, label, new CopyFunction(dstPk));
          }

        };
        g.visitSuccs(curPk, v);
        handleForwInterproc(curPkAndState, new CopyHandler() {

          @Override
          void handle(PointerKeyAndState src, PointerKey dst, IFlowLabel label) {
            boolean caseToAvoid = stateMachine instanceof ContextSensitiveStateMachine && label instanceof ParamLabel
                && src.getState().equals(stateMachine.getStartState());
            if (!caseToAvoid) {
              doTransition(src.getState(), label, new CopyFunction(dst));
            }
          }

        });
      }
      return false;
    }

    /**
     * return pairs (basePk,storedPk) where there exists a statement basePk.field = storedPk
     */
    private Collection<Pair<PointerKey, PointerKey>> getBaseAndStoredPointersOfWrites(PointerKey baseRef, IField field) {
      Collection<Pair<PointerKey, PointerKey>> result = HashSetFactory.make();
      if (field == ArrayContents.v()) {
        for (MemoryAccess a : mam.getArrayWrites(baseRef)) {
          final CGNode node = a.getNode();
          IR ir = node.getIR();
          SSAInstruction instruction = ir.getInstructions()[a.getInstructionIndex()];
          if (instruction == null) {
            // this means the array store found was in fact dead code
            // TODO detect this earlier and don't keep it in the MemoryAccessMap
            continue;
          }
          // TODO handle multi-dim arrays
          if (instruction instanceof SSAArrayStoreInstruction) {
            SSAArrayStoreInstruction s = (SSAArrayStoreInstruction) instruction;
            PointerKey base = heapModel.getPointerKeyForLocal(node, s.getArrayRef());
            PointerKey r = heapModel.getPointerKeyForLocal(node, s.getValue());
            // if (Assertions.verifyAssertions) {
            // Assertions._assert(containsNode(r), "missing node for " + r);
            // }
            result.add(Pair.make(base, r));
          }
        }
      } else {
        assert !field.isStatic();
        for (MemoryAccess a : mam.getFieldWrites(baseRef, field)) {
          IR ir = a.getNode().getIR();
          SSAPutInstruction s = (SSAPutInstruction) ir.getInstructions()[a.getInstructionIndex()];
          if (s == null) {
            // s can be null because the memory access map may be constructed from bytecode,
            // and the write instruction may have been eliminated from SSA because it's dead
            // TODO clean this up
            continue;
          }
          PointerKey base = heapModel.getPointerKeyForLocal(a.getNode(), s.getRef());
          PointerKey r = heapModel.getPointerKeyForLocal(a.getNode(), s.getVal());
          // if (Assertions.verifyAssertions) {
          // Assertions._assert(containsNode(r));
          // }
          result.add(Pair.make(base, r));

        }
      }
      return result;
    }

    private OrdinalSet<InstanceKeyAndState> makeOrdinalSet(IntSet intSet) {
      // make a copy here, to avoid comodification during iteration
      // TODO remove the copying, do it only at necessary call sites
      return new OrdinalSet<InstanceKeyAndState>(intSetFactory.makeCopy(intSet), ikAndStates);
    }

    public Collection<InstanceKey> getP2Set(LocalPointerKey lpk) {
      return Iterator2Collection.toCollection(new MapIterator<InstanceKeyAndState, InstanceKey>(makeOrdinalSet(
          find(pkToP2Set, new PointerKeyAndState(lpk, stateMachine.getStartState()))).iterator(),
          new Function<InstanceKeyAndState, InstanceKey>() {

            public InstanceKey apply(InstanceKeyAndState object) {
              return object.getInstanceKey();
            }

          }));
    }

    private boolean addAllToP2Set(Map<PointerKeyAndState, MutableIntSet> p2setMap, PointerKeyAndState pkAndState, IntSet vals,
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
        // System.err.println("POINTS-TO ADDITION TO PK " + pkAndState + ":");
        // for (InstanceKeyAndState ikAndState : makeOrdinalSet(vals)) {
        // System.err.println(ikAndState);
        // }
        // System.err.println("*************");
      }
      return added;

    }

    private IntSet updateValsForFilter(IntSet vals, final TypeFilter typeFilter) {
      if (typeFilter instanceof SingleClassFilter) {
        final IClass concreteType = ((SingleClassFilter) typeFilter).getConcreteType();
        final MutableIntSet tmp = intSetFactory.make();
        vals.foreach(new IntSetAction() {

          public void act(int x) {
            InstanceKeyAndState ikAndState = ikAndStates.getMappedObject(x);
            if (cha.isAssignableFrom(concreteType, ikAndState.getInstanceKey().getConcreteType())) {
              tmp.add(x);
            }
          }

        });
        vals = tmp;
      } else if (typeFilter instanceof SingleInstanceFilter) {
        final InstanceKey theOnlyInstanceKey = ((SingleInstanceFilter) typeFilter).getInstance();
        final MutableIntSet tmp = intSetFactory.make();
        vals.foreach(new IntSetAction() {

          public void act(int x) {
            InstanceKeyAndState ikAndState = ikAndStates.getMappedObject(x);
            if (ikAndState.getInstanceKey().equals(theOnlyInstanceKey)) {
              tmp.add(x);
            }
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
      if (Assertions.verifyAssertions) {
        Assertions._assert(!label.isBarred());
      }
      State curState = curPkAndState.getState();
      doTransition(curState, label, new Function<State, Object>() {

        public Object apply(State nextState) {
          PointerKeyAndState succPkAndState = new PointerKeyAndState(succPk, nextState);
          handleCopy(curPkAndState, succPkAndState, label);
          return null;
        }

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
     * 
     * @param curPkAndState
     * @param predPk
     * @param label the label of the edge from curPk to predPk (must be barred)
     * @return those {@link PointerKeyAndState}s whose points-to sets have been queried, such that the
     *         {@link PointerKey} is predPk, and transitioning from its state on <code>label.bar()</code> yields the
     *         state of <code>curPkAndState</code>
     */
    Collection<PointerKeyAndState> matchingPToQueried(PointerKeyAndState curPkAndState, PointerKey predPk, IFlowLabel label) {
      Collection<PointerKeyAndState> ret = ArraySet.make();
      if (Assertions.verifyAssertions) {
        Assertions._assert(label.isBarred());
      }
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
      if (Assertions.verifyAssertions) {
        Assertions._assert(label.isBarred());
      }
      final State curState = curPkAndState.getState();
      // TODO may need to speed this up with another data structure
      for (PointerKeyAndState pkAndState : pkToTrackedSet.keySet()) {
        if (succPk.equals(pkAndState.getPointerKey())) {
          State transState = stateMachine.transition(pkAndState.getState(), label);
          if (transState.equals(curState)) {
            ret.add(pkAndState);
          }
        }
      }
      return ret;

    }

    void handleBackCopy(PointerKeyAndState curPkAndState, PointerKey predPk, IFlowLabel label) {
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
     * should only be called when pk's points-to set has just been updated. add pk to the points-to worklist, and
     * re-propagate and calls that had pk as the receiver.
     * 
     * @param pkAndState
     */
    void addToPToWorklist(PointerKeyAndState pkAndState) {
      pointsToWorklist.add(pkAndState);
      Set<CallSiteAndCGNode> otfCalls = pkToOTFCalls.get(pkAndState);
      for (CallSiteAndCGNode callSiteAndCGNode : otfCalls) {
        propTargets(pkAndState, callSiteAndCGNode);
      }
    }

    boolean addToInitWorklist(PointerKeyAndState pkAndState) {
      if (pointsToQueried.put(pkAndState.getPointerKey(), pkAndState.getState())) {
        if (Assertions.verifyAssertions && pkAndState.getPointerKey() instanceof LocalPointerKey) {
          CGNode node = ((LocalPointerKey) pkAndState.getPointerKey()).getNode();
          if (!g.hasSubgraphForNode(node)) {
            Assertions._assert(false, "missing constraints for node of var " + pkAndState);
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
      if (Assertions.verifyAssertions && pkAndState.getPointerKey() instanceof LocalPointerKey) {
        CGNode node = ((LocalPointerKey) pkAndState.getPointerKey()).getNode();
        if (!g.hasSubgraphForNode(node)) {
          Assertions._assert(false, "missing constraints for " + node);
        }
      }
      if (DEBUG) {
        // System.err.println("adding to tracked points-to " + pkAndState);
      }
      trackedPointsToWorklist.add(pkAndState);
    }

    /**
     * Adds new targets for a virtual call, based on the points-to set of the receiver, and propagates values for the
     * parameters / return value of the new targets. NOTE: this method will <em>not</em> do any propagation for
     * virtual call targets that have already been discovered.
     * 
     * @param receiverAndState the receiver
     * @param callSiteAndCGNode the call
     */
    void propTargets(PointerKeyAndState receiverAndState, CallSiteAndCGNode callSiteAndCGNode) {
      final CGNode caller = callSiteAndCGNode.getCGNode();
      CallSiteReference call = callSiteAndCGNode.getCallSiteReference();
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
            final ReturnLabel returnLabel = ReturnLabel.make(new CallSiteAndCGNode(call, caller));
            if (invokeInstr.hasDef()) {
              final PointerKeyAndState defAndState = new PointerKeyAndState(heapModel.getPointerKeyForLocal(caller, invokeInstr
                  .getDef()), receiverState);
              final PointerKey ret = heapModel.getPointerKeyForReturnValue(targetForCall);
              doTransition(receiverState, returnLabel, new Function<State, Object>() {

                public Object apply(State retState) {
                  repropCallArg(defAndState, new PointerKeyAndState(ret, retState), returnLabel.bar());
                  return null;
                }

              });
            }
            final PointerKeyAndState exc = new PointerKeyAndState(heapModel.getPointerKeyForLocal(caller, invokeInstr
                .getException()), receiverState);
            final PointerKey excRet = heapModel.getPointerKeyForExceptionalReturnValue(targetForCall);
            doTransition(receiverState, returnLabel, new Function<State, Object>() {

              public Object apply(State excRetState) {
                repropCallArg(exc, new PointerKeyAndState(excRet, excRetState), returnLabel.bar());
                return null;
              }

            });
            for (Iterator<Integer> iter = new PointerParamValueNumIterator(targetForCall); iter.hasNext();) {
              final int formalNum = iter.next();
              final int actualNum = formalNum - 1;
              final ParamBarLabel paramBarLabel = ParamBarLabel.make(new CallSiteAndCGNode(call, caller));
              doTransition(receiverState, paramBarLabel, new Function<State, Object>() {

                public Object apply(State formalState) {
                  repropCallArg(
                      new PointerKeyAndState(heapModel.getPointerKeyForLocal(targetForCall, formalNum), formalState),
                      new PointerKeyAndState(heapModel.getPointerKeyForLocal(caller, invokeInstr.getUse(actualNum)), receiverState),
                      paramBarLabel);
                  return null;
                }

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
        if (Assertions.verifyAssertions && curPk instanceof LocalPointerKey) {
          Assertions._assert(g.hasSubgraphForNode(((LocalPointerKey) curPk).getNode()));
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
            doTransition(curState, label, new Function<State, Object>() {

              public Object apply(State newState) {
                InstanceKeyAndState ikAndState = new InstanceKeyAndState(ik, newState);
                int n = ikAndStates.add(ikAndState);
                findOrCreate(pkToP2Set, curPkAndState).add(n);
                addToPToWorklist(curPkAndState);
                return null;
              }

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
          for (final CallSiteAndCGNode callSiteAndCGNode : g.getPotentialCallers(localPk)) {
            final CGNode caller = callSiteAndCGNode.getCGNode();
            final CallSiteReference call = callSiteAndCGNode.getCallSiteReference();
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
                for (int i = 0; i < callInstrs.length; i++) {
                  SSAAbstractInvokeInstruction callInstr = callInstrs[i];
                  PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
                  if (Assertions.verifyAssertions) {
                    Assertions._assert(g.containsNode(actualPk));
                    Assertions._assert(g.containsNode(localPk));
                  }
                  handler.handle(curPkAndState, actualPk, paramLabel);
                }
              }

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
        SSAInvokeInstruction callInstr = g.getInstrReturningTo(localPk);
        if (callInstr != null) {
          CGNode caller = localPk.getNode();
          boolean isExceptional = localPk.getValueNumber() == callInstr.getException();

          CallSiteReference callSiteRef = callInstr.getCallSite();
          CallSiteAndCGNode callSiteAndCGNode = new CallSiteAndCGNode(callSiteRef, caller);
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
              if (Assertions.verifyAssertions) {
                Assertions._assert(g.containsNode(retVal));
              }
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
                  if (Assertions.verifyAssertions) {
                    Assertions._assert(g.containsNode(retVal));
                  }
                  handler.handle(curPkAndState, retVal, ReturnLabel.make(callSiteAndCGNode));
                }
              }
            } else {
              // if necessary, raise a query for the call site
              queryCallTargets(callSiteAndCGNode, getCallInstrs(caller, callSiteAndCGNode.getCallSiteReference()), curPkAndState
                  .getState());
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
      State state = ikAndState.getState();
      if (Assertions.verifyAssertions) {
        // Assertions._assert(refineFieldAccesses(field));
      }
      ikToFields.put(ikAndState, field);
      for (Iterator<? extends Object> iter = g.getPredNodes(ikAndState.getInstanceKey(), NewLabel.v()); iter.hasNext();) {
        PointerKey ikPred = (PointerKey) iter.next();
        PointerKeyAndState ikPredAndState = new PointerKeyAndState(ikPred, state);
        int mappedIndex = ikAndStates.getMappedIndex(ikAndState);
        if (Assertions.verifyAssertions) {
          Assertions._assert(mappedIndex != -1);
        }
        if (findOrCreate(pkToTrackedSet, ikPredAndState).add(mappedIndex)) {
          addToTrackedPToWorklist(ikPredAndState);
        }
      }
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

    /**
     * Initiates a query for the targets of some virtual call, by asking for points-to set of receiver. NOTE: if
     * receiver has already been queried, will not do any additional propagation for already-discovered virtual call
     * targets
     * 
     * @param caller
     * @param ir
     * @param call
     * @param callerState
     */
    private void queryCallTargets(CallSiteAndCGNode callSiteAndCGNode, SSAAbstractInvokeInstruction[] callInstrs, State callerState) {
      final CallSiteReference call = callSiteAndCGNode.getCallSiteReference();
      final CGNode caller = callSiteAndCGNode.getCGNode();
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
              System.err.println("querying for targets of call " + call + " in " + caller);
            }
          }
        } else {
          // TODO: I think we can remove this call
          propTargets(thisArgAndState, callSiteAndCGNode);
        }
      }
    }

    private boolean noOnTheFlyNeeded(CallSiteAndCGNode call, Set<CGNode> possibleTargets) {
      // NOTE: if we want to be more precise for queries in dead code,
      // we shouldn't rely on possibleTargets here (since there may be
      // zero targets)
      return !refinementPolicy.getCallGraphRefinePolicy().shouldRefine(call) || possibleTargets.size() <= 1;
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
        for (final CallSiteAndCGNode callSiteAndCGNode : g.getPotentialCallers(returnKey)) {
          final CGNode caller = callSiteAndCGNode.getCGNode();
          if (hasNullIR(caller))
            continue;
          final CallSiteReference call = callSiteAndCGNode.getCallSiteReference();
          if (!addGraphs) {
            // shouldn't need to add the graph, so check if it is present;
            // if not, terminate
            if (!g.hasSubgraphForNode(caller)) {
              continue;
            }
          }
          final ReturnBarLabel returnBarLabel = ReturnBarLabel.make(callSiteAndCGNode);
          doTransition(curState, returnBarLabel, new Function<State, Object>() {

            private void propagateToCaller() {
              // if (caller.getIR() == null) {
              // return;
              // }
              g.addSubgraphForNode(caller);
              SSAAbstractInvokeInstruction[] callInstrs = getCallInstrs(caller, call);
              for (int i = 0; i < callInstrs.length; i++) {
                SSAAbstractInvokeInstruction callInstr = callInstrs[i];
                PointerKey returnAtCallerKey = heapModel.getPointerKeyForLocal(caller, isExceptional ? callInstr.getException()
                    : callInstr.getDef());
                if (Assertions.verifyAssertions) {
                  Assertions._assert(g.containsNode(returnAtCallerKey));
                  Assertions._assert(g.containsNode(returnKey));
                }
                handler.handle(curPkAndState, returnAtCallerKey, returnBarLabel);
              }
            }

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
        for (Iterator<SSAInvokeInstruction> iter = g.getInstrsPassingParam(localPk); iter.hasNext();) {
          SSAInvokeInstruction callInstr = iter.next();
          for (int i = 0; i < callInstr.getNumberOfUses(); i++) {
            if (localPk.getValueNumber() != callInstr.getUse(i))
              continue;
            CallSiteReference callSiteRef = callInstr.getCallSite();
            CallSiteAndCGNode callSiteAndCGNode = new CallSiteAndCGNode(callSiteRef, caller);
            // get call targets
            Set<CGNode> possibleCallees = g.getPossibleTargets(caller, callSiteRef, localPk);
            // construct graph for each target
            if (noOnTheFlyNeeded(callSiteAndCGNode, possibleCallees)) {
              for (CGNode callee : possibleCallees) {
                if (!addGraphs) {
                  // shouldn't need to add the graph, so check if it is present;
                  // if not, terminate
                  if (!g.hasSubgraphForNode(callee)) {
                    continue;
                  }
                }
                if (hasNullIR(callee)) {
                  continue;
                }
                g.addSubgraphForNode(callee);
                PointerKey paramVal = heapModel.getPointerKeyForLocal(callee, i + 1);
                if (Assertions.verifyAssertions) {
                  Assertions._assert(g.containsNode(paramVal));
                }
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
                    if (Assertions.verifyAssertions) {
                      Assertions._assert(g.containsNode(paramVal));
                    }
                    handler.handle(curPkAndState, paramVal, ParamBarLabel.make(callSiteAndCGNode));
                  }
                }
              } else {
                // if necessary, raise a query for the call site
                queryCallTargets(callSiteAndCGNode, getCallInstrs(caller, callSiteAndCGNode.getCallSiteReference()), curState);
              }
            }
          }
        }
      }
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
          public void visitAssignGlobal(AssignGlobalLabel label, Object dst) {
            for (Iterator<? extends Object> readIter = g.getReadsOfStaticField((StaticFieldKey) dst); readIter.hasNext();) {
              final PointerKey predPk = (PointerKey) readIter.next();
              doTransition(curState, AssignGlobalBarLabel.v(), new Function<State, Object>() {

                public Object apply(State predPkState) {
                  PointerKeyAndState predPkAndState = new PointerKeyAndState(predPk, predPkState);
                  if (findOrCreate(pkToTrackedSet, predPkAndState).addAll(trackedSet)) {
                    addToTrackedPToWorklist(predPkAndState);
                  }
                  return null;
                }

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
              for (Iterator<PointerKey> readIter = g.getReadsOfInstanceField(storeBase, field); readIter.hasNext();) {
                final PointerKey predPk = readIter.next();
                doTransition(curState, MatchBarLabel.v(), new Function<State, Object>() {

                  public Object apply(State predPkState) {
                    PointerKeyAndState predPkAndState = new PointerKeyAndState(predPk, predPkState);
                    if (findOrCreate(pkToTrackedSet, predPkAndState).addAll(trackedSet)) {
                      addToTrackedPToWorklist(predPkAndState);
                    }
                    return null;
                  }

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
                  if (findOrCreate(pkToTrackedSet, loadedVal).addAll(find(instFieldKeyToTrackedSet, ifk))) {
                    addToTrackedPToWorklist(loadedVal);
                  }
                }
              }
            }
          }

          @Override
          public void visitAssign(AssignLabel label, Object dst) {
            final PointerKey predPk = (PointerKey) dst;
            doTransition(curState, AssignBarLabel.noFilter(), new Function<State, Object>() {

              public Object apply(State predPkState) {
                PointerKeyAndState predPkAndState = new PointerKeyAndState(predPk, predPkState);
                if (findOrCreate(pkToTrackedSet, predPkAndState).addAll(trackedSet)) {
                  addToTrackedPToWorklist(predPkAndState);
                }
                return null;
              }

            });
          }

        };
        g.visitPreds(curPk, predVisitor);
        handleBackInterproc(curPkAndState, new CopyHandler() {

          @Override
          void handle(PointerKeyAndState src, final PointerKey dst, IFlowLabel label) {
            if (Assertions.verifyAssertions) {
              Assertions._assert(src == curPkAndState);
            }
            doTransition(curState, label, new Function<State, Object>() {

              public Object apply(State dstState) {
                PointerKeyAndState dstAndState = new PointerKeyAndState(dst, dstState);
                if (findOrCreate(pkToTrackedSet, dstAndState).addAll(trackedSet)) {
                  addToTrackedPToWorklist(dstAndState);
                }
                return null;
              }

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
        if (Assertions.verifyAssertions) {
          boolean basePointerOkay = pointsToQueried.get(basePointerKey).contains(loadDstState)
              || !pointsToQueried.get(loadedValAndState.getPointerKey()).contains(loadDstState)
              || initWorklist.contains(loadedValAndState);
          // if (!basePointerOkay) {
          // System.err.println("ASSERTION WILL FAIL");
          // System.err.println("QUERIED: " + queriedPkAndStates);
          // }
          if (!basePointerOkay) {
            // TEMPORARY --MS
            // Assertions._assert(false, "queried " + loadedValAndState + " but not " + baseAndStateToHandle);
          }
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
          // just pass no label assign filter since no type-based filtering can be
          // done here
          if (addAllToP2Set(pkToP2Set, loadedValAndState, find(instFieldKeyToP2Set, ifk), AssignLabel.noFilter())) {
            if (DEBUG) {
              System.err.println("from load edge " + loadEdge);
            }
            addToPToWorklist(loadedValAndState);
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

    private <K> MutableIntSet find(Map<K, MutableIntSet> M, K key) {
      MutableIntSet result = M.get(key);
      if (result == null) {
        result = emptySet;
      }
      return result;
    }
  }

  private SSAAbstractInvokeInstruction[] getCallInstrs(CGNode node, CallSiteReference site) {
    return node.getIR().getCalls(site);
  }

  private boolean hasNullIR(CGNode node) {
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

}
