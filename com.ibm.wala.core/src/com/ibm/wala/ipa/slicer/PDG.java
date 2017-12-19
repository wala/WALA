/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.GraphUtil;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Program dependence graph for a single call graph node
 */
public class PDG<T extends InstanceKey> implements NumberedGraph<Statement> {

/** BEGIN Custom change: control deps */                
  public enum Dependency {CONTROL_DEP, DATA_AND_CONTROL_DEP}
  
  private final SlowSparseNumberedLabeledGraph<Statement, Dependency> delegate =
    new SlowSparseNumberedLabeledGraph<>(Dependency.DATA_AND_CONTROL_DEP);
/** END Custom change: control deps */                

  private final static boolean VERBOSE = false;

  private final CGNode node;

  private Statement[] paramCalleeStatements;

  private Statement[] returnStatements;

  /**
   * TODO: using CallSiteReference is sloppy. clean it up.
   */

  private final Map<CallSiteReference, Statement> callSite2Statement = HashMapFactory.make();

  private final Map<CallSiteReference, Set<Statement>> callerParamStatements = HashMapFactory.make();

  private final Map<CallSiteReference, Set<Statement>> callerReturnStatements = HashMapFactory.make();

  private final HeapExclusions exclusions;

  private final Collection<PointerKey> locationsHandled = HashSetFactory.make();

  private final PointerAnalysis<T> pa;

  private final ExtendedHeapModel heapModel;

  private final Map<CGNode, OrdinalSet<PointerKey>> mod;

  private final DataDependenceOptions dOptions;

  private final ControlDependenceOptions cOptions;

  private final CallGraph cg;

  private final ModRef<T> modRef;

  private final Map<CGNode, OrdinalSet<PointerKey>> ref;

  private final boolean ignoreAllocHeapDefs;

  private boolean isPopulated = false;

  /**
   * @param mod the set of heap locations which may be written (transitively) by this node. These are logically return values in the
   *          SDG.
   * @param ref the set of heap locations which may be read (transitively) by this node. These are logically parameters in the SDG.
   * @throws IllegalArgumentException if node is null
   */
  public PDG(final CGNode node, PointerAnalysis<T> pa, Map<CGNode, OrdinalSet<PointerKey>> mod,
      Map<CGNode, OrdinalSet<PointerKey>> ref, DataDependenceOptions dOptions, ControlDependenceOptions cOptions,
      HeapExclusions exclusions, CallGraph cg, ModRef<T> modRef) {
    this(node, pa, mod, ref, dOptions, cOptions, exclusions, cg, modRef, false);
  }

  /**
   * @param mod the set of heap locations which may be written (transitively) by this node. These are logically return values in the
   *          SDG.
   * @param ref the set of heap locations which may be read (transitively) by this node. These are logically parameters in the SDG.
   * @throws IllegalArgumentException if node is null
   */
  public PDG(final CGNode node, PointerAnalysis<T> pa, Map<CGNode, OrdinalSet<PointerKey>> mod,
      Map<CGNode, OrdinalSet<PointerKey>> ref, DataDependenceOptions dOptions, ControlDependenceOptions cOptions,
      HeapExclusions exclusions, CallGraph cg, ModRef<T> modRef, boolean ignoreAllocHeapDefs) {

    super();
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    this.cg = cg;
    this.node = node;
    this.heapModel = pa != null? modRef.makeHeapModel(pa): null;
    this.pa = pa;
    this.dOptions = dOptions;
    this.cOptions = cOptions;
    this.mod = mod;
    this.exclusions = exclusions;
    this.modRef = modRef;
    this.ref = ref;
    this.ignoreAllocHeapDefs = ignoreAllocHeapDefs;
  }

  /**
   * WARNING: Since we're using a {@link HashMap} of {@link SSAInstruction}s, and equals() of {@link SSAInstruction} assumes a
   * canonical representative for each instruction, we <b>must</b> ensure that we use the same IR object throughout
   * initialization!!
   */
  private void populate() {
    if (!isPopulated) {
      // ensure that we keep the single, canonical IR live throughout initialization, while the instructionIndices map
      // is live.
      IR ir = node.getIR();
      isPopulated = true;

      Map<SSAInstruction, Integer> instructionIndices = computeInstructionIndices(ir);
      createNodes(ref, ir);
      createScalarEdges(cOptions, ir, instructionIndices);
    }
  }

  private void createScalarEdges(ControlDependenceOptions cOptions, IR ir, Map<SSAInstruction, Integer> instructionIndices) {
    createScalarDataDependenceEdges(ir, instructionIndices);
    createControlDependenceEdges(cOptions, ir, instructionIndices);
  }

  /**
   * return the set of all PARAM_CALLER and HEAP_PARAM_CALLER statements associated with a given call
   */
  public Set<Statement> getCallerParamStatements(SSAAbstractInvokeInstruction call) throws IllegalArgumentException {
    if (call == null) {
      throw new IllegalArgumentException("call == null");
    }
    populate();
    return callerParamStatements.get(call.getCallSite());
  }

  /**
   * return the set of all PARAM_CALLER, HEAP_PARAM_CALLER, and NORMAL statements (i.e., the actual call statement) associated with
   * a given call
   */
  public Set<Statement> getCallStatements(SSAAbstractInvokeInstruction call) throws IllegalArgumentException {
    Set<Statement> callerParamStatements = getCallerParamStatements(call);
    Set<Statement> result = HashSetFactory.make(callerParamStatements.size() + 1);
    result.addAll(callerParamStatements);
    result.add(callSite2Statement.get(call.getCallSite()));
    return result;
  }

  /**
   * return the set of all NORMAL_RETURN_CALLER and HEAP_RETURN_CALLER statements associated with a given call.
   */
  public Set<Statement> getCallerReturnStatements(SSAAbstractInvokeInstruction call) throws IllegalArgumentException {
    if (call == null) {
      throw new IllegalArgumentException("call == null");
    }
    populate();
    return callerReturnStatements.get(call.getCallSite());
  }

  /**
   * Create all control dependence edges in this PDG.
   */
  private void createControlDependenceEdges(ControlDependenceOptions cOptions, IR ir,
      Map<SSAInstruction, Integer> instructionIndices) {
    if (cOptions.equals(ControlDependenceOptions.NONE)) {
      return;
    }
    if (ir == null) {
      return;
    }
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> controlFlowGraph = ir.getControlFlowGraph();
    if (cOptions.isIgnoreExceptions()) {
      PrunedCFG<SSAInstruction, ISSABasicBlock> prunedCFG = ExceptionPrunedCFG.make(controlFlowGraph);
      // In case the CFG has only the entry and exit nodes left 
      // and no edges because the only control dependencies
      // were exceptional, simply return because at this point there are no nodes.
      // Otherwise, later this may raise an Exception.
      if (prunedCFG.getNumberOfNodes() == 2 
          && prunedCFG.containsNode(controlFlowGraph.entry()) 
          && prunedCFG.containsNode(controlFlowGraph.exit())
          && GraphUtil.countEdges(prunedCFG) == 0) {
        return;
      }
      controlFlowGraph = prunedCFG;
    } else {
      Assertions.productionAssertion(cOptions.equals(ControlDependenceOptions.FULL));
    }

    ControlDependenceGraph<ISSABasicBlock> cdg = new ControlDependenceGraph<>(
        controlFlowGraph);
    for (ISSABasicBlock bb : cdg) {
      if (bb.isExitBlock()) {
        // nothing should be control-dependent on the exit block.
        continue;
      }

      Statement src = null;
      if (bb.isEntryBlock()) {
        src = new MethodEntryStatement(node);
      } else {
        SSAInstruction s = ir.getInstructions()[bb.getLastInstructionIndex()];
        if (s == null) {
          // should have no control dependent successors.
          // leave src null.
        } else {
          src = ssaInstruction2Statement(s, ir, instructionIndices);
          // add edges from call statements to parameter passing and return
          // SJF: Alexey and I think that we should just define ParamStatements
          // as
          // being control dependent on nothing ... they only represent pure
          // data dependence. So, I'm commenting out the following.
          // if (s instanceof SSAAbstractInvokeInstruction) {
          // SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction)
          // s;
          // for (Statement st : callerParamStatements.get(call.getCallSite()))
          // {
          // addEdge(src, st);
          // }
          // for (Statement st : callerReturnStatements.get(call.getCallSite()))
          // {
          // addEdge(src, st);
          // }
          // }
        }
      }
      // add edges for every control-dependent statement in the IR, if there are
      // any
      // control-dependent successors
      if (src != null) {
        for (ISSABasicBlock bb2 : Iterator2Iterable.make(cdg.getSuccNodes(bb))) {
          for (SSAInstruction st : bb2) {
            if (st != null) {
              Statement dest = ssaInstruction2Statement(st, ir, instructionIndices);
              assert src != null;
              delegate.addEdge(src, dest);
/** BEGIN Custom change: control deps */                
              delegate.addEdge(src, dest, Dependency.CONTROL_DEP);
/** END Custom change: control deps */                
            }
          }
        }
      }
    }

    // the CDG does not represent control dependences from the entry node.
    // add these manually
    // We add control dependences to all instructions in all basic blocks B that _must_ execute.
    // B is the set of blocks that dominate the exit basic block
    Statement methodEntry = new MethodEntryStatement(node);
    Dominators<ISSABasicBlock> dom = Dominators.make(controlFlowGraph, controlFlowGraph.entry());
    for (ISSABasicBlock exitDom : Iterator2Iterable.make(dom.dominators(controlFlowGraph.exit()))) {
      for (SSAInstruction st : exitDom) {
        Statement dest = ssaInstruction2Statement(st, ir, instructionIndices);
        delegate.addEdge(methodEntry, dest);
/** BEGIN Custom change: control deps */                
        delegate.addEdge(methodEntry, dest, Dependency.CONTROL_DEP);
/** END Custom change: control deps */                
      }
    }
    // add CD from method entry to all callee parameter assignments
    // SJF: Alexey and I think that we should just define ParamStatements as
    // being control dependent on nothing ... they only represent pure
    // data dependence. So, I'm commenting out the following.
    // for (int i = 0; i < paramCalleeStatements.length; i++) {
    // addEdge(methodEntry, paramCalleeStatements[i]);
    // }

    /**
     * JTD: While phi nodes live in a particular basic block, they represent a meet of values from multiple blocks. Hence, they are
     * really like multiple statements that are control dependent in the manner of the predecessor blocks. When the slicer is
     * following both data and control dependences, it therefore seems right to add control dependence edges to represent how a phi
     * node depends on predecessor blocks.
     */
    if (!dOptions.equals(DataDependenceOptions.NONE)) {
      for (ISSABasicBlock bb : cdg) {
        for (SSAPhiInstruction phi : Iterator2Iterable.make(bb.iteratePhis())) {
          Statement phiSt = ssaInstruction2Statement(phi, ir, instructionIndices);
          int phiUseIndex = 0;
          for (ISSABasicBlock pb : Iterator2Iterable.make(controlFlowGraph.getPredNodes(bb))) {
            int use = phi.getUse(phiUseIndex);
            if (use == AbstractIntStackMachine.TOP) {
              // the predecessor is part of some infeasible bytecode. we probably don't want slices to include such code, so ignore.
              continue;
            }
            if (controlFlowGraph.getSuccNodeCount(pb) > 1) {
              // in this case, there is more than one edge from the
              // predecessor block, hence the phi node actually
              // depends on the last instruction in the previous
              // block, rather than having the same dependences as
              // statements in that block.
              SSAInstruction pss = ir.getInstructions()[pb.getLastInstructionIndex()];
              assert pss != null;
              Statement pst = ssaInstruction2Statement(pss, ir, instructionIndices);
              delegate.addEdge(pst, phiSt);
/** BEGIN Custom change: control deps */                
              delegate.addEdge(pst, phiSt, Dependency.CONTROL_DEP);
/** END Custom change: control deps */                
            } else {
              for (ISSABasicBlock cpb : Iterator2Iterable.make(cdg.getPredNodes(pb))) {
/** BEGIN Custom change: control deps */                
                if (cpb.getLastInstructionIndex() < 0) {
                  continue;
                }
/** END Custom change: control deps */                
                SSAInstruction cps = ir.getInstructions()[cpb.getLastInstructionIndex()];
                assert cps != null : "unexpected null final instruction for CDG predecessor " + cpb + " in node " + node;
                Statement cpst = ssaInstruction2Statement(cps, ir, instructionIndices);
                delegate.addEdge(cpst, phiSt);
/** BEGIN Custom change: control deps */                
                delegate.addEdge(cpst, phiSt, Dependency.CONTROL_DEP);
/** END Custom change: control deps */                
              }
            }
            phiUseIndex++;
          }
        }
      }
    }
  }

  /**
   * Create all data dependence edges in this PDG.
   * 
   * Scalar dependences are taken from SSA def-use information.
   * 
   * Heap dependences are computed by a reaching defs analysis.
   * 
   * @param pa
   * @param mod
   */
  private void createScalarDataDependenceEdges(IR ir, Map<SSAInstruction, Integer> instructionIndices) {
    if (dOptions.equals(DataDependenceOptions.NONE)) {
      return;
    }

    if (ir == null) {
      return;
    }

    // this is tricky .. I'm explicitly creating a new DefUse to make sure it refers to the instructions we need from
    // the "one true" ir of the moment.
    DefUse DU = new DefUse(ir);
    SSAInstruction[] instructions = ir.getInstructions();

    //
    // TODO: teach some other bit of code about the uses of
    // GetCaughtException, and then delete this code.
    //
    if (!dOptions.isIgnoreExceptions()) {
      for (ISSABasicBlock bb : ir.getControlFlowGraph()) {
        if (bb.isCatchBlock()) {
          SSACFG.ExceptionHandlerBasicBlock ehbb = (SSACFG.ExceptionHandlerBasicBlock) bb;

          if (ehbb.getCatchInstruction() != null) {
            Statement c = ssaInstruction2Statement(ehbb.getCatchInstruction(), ir, instructionIndices);

            for (ISSABasicBlock pb : ir.getControlFlowGraph().getExceptionalPredecessors(ehbb)) {
              SSAInstruction st = instructions[pb.getLastInstructionIndex()];

              if (st instanceof SSAAbstractInvokeInstruction) {
                delegate.addEdge(new ExceptionalReturnCaller(node, pb.getLastInstructionIndex()), c);
              } else if (st instanceof SSAAbstractThrowInstruction) {
                delegate.addEdge(ssaInstruction2Statement(st, ir, instructionIndices), c);
              }
            }
          }
        }
      }
    }

    for (Statement s : this) {
      switch (s.getKind()) {
      case NORMAL:
      case CATCH:
      case PI:
      case PHI: {
        SSAInstruction statement = statement2SSAInstruction(instructions, s);
        // note that data dependencies from invoke instructions will pass
        // interprocedurally
        if (!(statement instanceof SSAAbstractInvokeInstruction)) {
          if (dOptions.isTerminateAtCast() && (statement instanceof SSACheckCastInstruction)) {
            break;
          }
          if (dOptions.isTerminateAtCast() && (statement instanceof SSAInstanceofInstruction)) {
            break;
          }
          // add edges from this statement to every use of the def of this
          // statement
          for (int i = 0; i < statement.getNumberOfDefs(); i++) {
            int def = statement.getDef(i);
            for (SSAInstruction use : Iterator2Iterable.make(DU.getUses(def))) {
              if (dOptions.isIgnoreBasePtrs()) {
                if (use instanceof SSANewInstruction) {
                  // cut out array length parameters
                  continue;
                }
                if (hasBasePointer(use)) {
                  int base = getBasePointer(use);
                  if (def == base) {
                    // skip the edge to the base pointer
                    continue;
                  }
                  if (use instanceof SSAArrayReferenceInstruction) {
                    SSAArrayReferenceInstruction arr = (SSAArrayReferenceInstruction) use;
                    if (def == arr.getIndex()) {
                      // skip the edge to the array index
                      continue;
                    }
                  }
                }
              }
              Statement u = ssaInstruction2Statement(use, ir, instructionIndices);
              delegate.addEdge(s, u);
            }
          }
        }
        break;
      }
      case EXC_RET_CALLER:
      case NORMAL_RET_CALLER:
      case PARAM_CALLEE: {
        if (dOptions.isIgnoreExceptions()) {
          assert !s.getKind().equals(Kind.EXC_RET_CALLER);
        }

        ValueNumberCarrier a = (ValueNumberCarrier) s;
        for (SSAInstruction use : Iterator2Iterable.make(DU.getUses(a.getValueNumber()))) {
          if (dOptions.isIgnoreBasePtrs()) {
            if (use instanceof SSANewInstruction) {
              // cut out array length parameters
              continue;
            }
            if (hasBasePointer(use)) {
              int base = getBasePointer(use);
              if (a.getValueNumber() == base) {
                // skip the edge to the base pointer
                continue;
              }
              if (use instanceof SSAArrayReferenceInstruction) {
                SSAArrayReferenceInstruction arr = (SSAArrayReferenceInstruction) use;
                if (a.getValueNumber() == arr.getIndex()) {
                  // skip the edge to the array index
                  continue;
                }
              }
            }
          }
          Statement u = ssaInstruction2Statement(use, ir, instructionIndices);
          delegate.addEdge(s, u);
        }
        break;
      }
      case NORMAL_RET_CALLEE:
        for (NormalStatement ret : computeReturnStatements(ir)) {
          delegate.addEdge(ret, s);
        }
        break;
      case EXC_RET_CALLEE:
        if (dOptions.isIgnoreExceptions()) {
          Assertions.UNREACHABLE();
        }
        // TODO: this is overly conservative. deal with catch blocks?
        for (IntIterator ii = getPEIs(ir).intIterator(); ii.hasNext();) {
          int index = ii.next();
          SSAInstruction pei = ir.getInstructions()[index];
          if (dOptions.isTerminateAtCast() && (pei instanceof SSACheckCastInstruction)) {
            continue;
          }
          if (pei instanceof SSAAbstractInvokeInstruction) {
            if (! dOptions.isIgnoreExceptions()) {
              Statement st = new ExceptionalReturnCaller(node, index);
              delegate.addEdge(st, s);
            }
          } else {
            delegate.addEdge(new NormalStatement(node, index), s);
          }
        }
        break;
      case PARAM_CALLER: {
        ParamCaller pac = (ParamCaller) s;
        int vn = pac.getValueNumber();
        // note that if the caller is the fake root method and the parameter
        // type is primitive,
        // it's possible to have a value number of -1. If so, just ignore it.
        if (vn > -1) {
          if (ir.getSymbolTable().isParameter(vn)) {
            Statement a = new ParamCallee(node, vn);
            delegate.addEdge(a, pac);
          } else {
            SSAInstruction d = DU.getDef(vn);
            if (dOptions.isTerminateAtCast() && (d instanceof SSACheckCastInstruction)) {
              break;
            }
            if (d != null) {
              if (d instanceof SSAAbstractInvokeInstruction) {
                SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) d;
                if (vn == call.getException()) {
                  if (! dOptions.isIgnoreExceptions()) {
                    Statement st = new ExceptionalReturnCaller(node, instructionIndices.get(d));
                    delegate.addEdge(st, pac);
                  }
                } else {
                  Statement st = new NormalReturnCaller(node, instructionIndices.get(d));
                  delegate.addEdge(st, pac);
                }
              } else {
                Statement ds = ssaInstruction2Statement(d, ir, instructionIndices);
                delegate.addEdge(ds, pac);
              }
            }
          }
        }
      }
        break;

      case HEAP_RET_CALLEE:
      case HEAP_RET_CALLER:
      case HEAP_PARAM_CALLER:
      case HEAP_PARAM_CALLEE:
      case METHOD_ENTRY:
      case METHOD_EXIT:
        // do nothing
        break;
      default:
        Assertions.UNREACHABLE(s.toString());
        break;
      }
    }
  }

  private static class SingletonSet extends SetOfClasses implements Serializable {

    /* Serial version */
    private static final long serialVersionUID = -3256390509887654324L;

    private final TypeReference t;

    SingletonSet(TypeReference t) {
      this.t = t;
    }

    @Override
    public void add(String klass) {
      Assertions.UNREACHABLE();
    }

    @Override
    public boolean contains(String klassName) {
      return t.getName().toString().substring(1).equals(klassName);
    }
  }

  private static class SetComplement extends SetOfClasses implements Serializable {

    /* Serial version */
    private static final long serialVersionUID = -3256390509887654323L;

    private final SetOfClasses set;

    SetComplement(SetOfClasses set) {
      this.set = set;
    }

    static SetComplement complement(SetOfClasses set) {
      return new SetComplement(set);
    }

    @Override
    public void add(String klass) {
      Assertions.UNREACHABLE();
    }

    @Override
    public boolean contains(String klassName) {
      return !set.contains(klassName);
    }
  }

  /**
   * Create heap data dependence edges in this PDG relevant to a particular {@link PointerKey}.
   */
  private void createHeapDataDependenceEdges(final PointerKey pk) {

    if (locationsHandled.contains(pk)) {
      return;
    } else {
      locationsHandled.add(pk);
    }
    if (dOptions.isIgnoreHeap() || (exclusions != null && exclusions.excludes(pk))) {
      return;
    }

    TypeReference t = HeapExclusions.getType(pk);
    if (t == null) {
      return;
    }

    // It's OK to create a new IR here; we're not keeping any hashing live up to this point
    IR ir = node.getIR();
    if (ir == null) {
      return;
    }

    if (VERBOSE) {
      System.err.println("Location " + pk);
    }

    // in reaching defs calculation, exclude heap statements that are
    // irrelevant.
    Predicate<Statement> f = o -> {
      if (o instanceof HeapStatement) {
        HeapStatement h = (HeapStatement) o;
        return h.getLocation().equals(pk);
      } else {
        return true;
      }
    };
    Collection<Statement> relevantStatements = Iterator2Collection.toSet(new FilterIterator<>(iterator(), f));

    Map<Statement, OrdinalSet<Statement>> heapReachingDefs = new HeapReachingDefs<>(modRef, heapModel).computeReachingDefs(node, ir, pa, mod,
        relevantStatements, new HeapExclusions(SetComplement.complement(new SingletonSet(t))), cg);

    for (Statement st : heapReachingDefs.keySet()) {
      switch (st.getKind()) {
      case NORMAL:
      case CATCH:
      case PHI:
      case PI: {
        OrdinalSet<Statement> defs = heapReachingDefs.get(st);
        if (defs != null) {
          for (Statement def : defs) {
            delegate.addEdge(def, st);
          }
        }
      }
        break;
      case EXC_RET_CALLER:
      case NORMAL_RET_CALLER:
      case PARAM_CALLEE:
      case NORMAL_RET_CALLEE:
      case PARAM_CALLER:
      case EXC_RET_CALLEE:
        break;
      case HEAP_RET_CALLEE:
      case HEAP_RET_CALLER:
      case HEAP_PARAM_CALLER: {
        OrdinalSet<Statement> defs = heapReachingDefs.get(st);
        if (defs != null) {
          for (Statement def : defs) {
            delegate.addEdge(def, st);
          }
        }
        break;
      }
      case HEAP_PARAM_CALLEE:
      case METHOD_ENTRY:
      case METHOD_EXIT:
        // do nothing .. there are no incoming edges
        break;
      default:
        Assertions.UNREACHABLE(st.toString());
        break;
      }
    }
  }

  private static boolean hasBasePointer(SSAInstruction use) {
    if (use instanceof SSAFieldAccessInstruction) {
      SSAFieldAccessInstruction f = (SSAFieldAccessInstruction) use;
      return !f.isStatic();
    } else if (use instanceof SSAArrayReferenceInstruction) {
      return true;
    } else if (use instanceof SSAArrayLengthInstruction) {
      return true;
    } else {
      return false;
    }
  }

  private static int getBasePointer(SSAInstruction use) {
    if (use instanceof SSAFieldAccessInstruction) {
      SSAFieldAccessInstruction f = (SSAFieldAccessInstruction) use;
      return f.getRef();
    } else if (use instanceof SSAArrayReferenceInstruction) {
      SSAArrayReferenceInstruction a = (SSAArrayReferenceInstruction) use;
      return a.getArrayRef();
    } else if (use instanceof SSAArrayLengthInstruction) {
      SSAArrayLengthInstruction s = (SSAArrayLengthInstruction) use;
      return s.getArrayRef();
    } else {
      Assertions.UNREACHABLE("BOOM");
      return -1;
    }
  }

  /**
   * @return Statements representing each return instruction in the ir
   */
  private Collection<NormalStatement> computeReturnStatements(final IR ir) {
    Predicate<Statement> filter = o -> {
      if (o instanceof NormalStatement) {
        NormalStatement s = (NormalStatement) o;
        SSAInstruction st = ir.getInstructions()[s.getInstructionIndex()];
        return st instanceof SSAReturnInstruction;
      } else {
        return false;
      }
    };
    return Iterator2Collection.toSet(
        new MapIterator<>(
            new FilterIterator<>(iterator(), filter),
            NormalStatement.class::cast));
  }

  /**
   * @return {@link IntSet} representing instruction indices of each PEI in the ir
   */
  private static IntSet getPEIs(final IR ir) {
    BitVectorIntSet result = new BitVectorIntSet();
    for (int i = 0; i < ir.getInstructions().length; i++) {
      if (ir.getInstructions()[i] != null && ir.getInstructions()[i].isPEI()) {
        result.add(i);
      }
    }
    return result;
  }

  /**
   * Wrap an {@link SSAInstruction} in a {@link Statement}. WARNING: Since we're using a {@link HashMap} of {@link SSAInstruction}s,
   * and equals() of {@link SSAInstruction} assumes a canonical representative for each instruction, we <b>must</b> ensure that we
   * use the same IR object throughout initialization!!
   */
  private Statement ssaInstruction2Statement(SSAInstruction s, IR ir, Map<SSAInstruction, Integer> instructionIndices) {
    return ssaInstruction2Statement(node, s, instructionIndices, ir);
  }

  public static synchronized Statement ssaInstruction2Statement(CGNode node, SSAInstruction s,
      Map<SSAInstruction, Integer> instructionIndices, IR ir) {
    if (node == null) {
      throw new IllegalArgumentException("null node");
    }
    if (s == null) {
      throw new IllegalArgumentException("null s");
    }
    if (s instanceof SSAPhiInstruction) {
      SSAPhiInstruction phi = (SSAPhiInstruction) s;
      return new PhiStatement(node, phi);
    } else if (s instanceof SSAPiInstruction) {
      SSAPiInstruction pi = (SSAPiInstruction) s;
      return new PiStatement(node, pi);
    } else if (s instanceof SSAGetCaughtExceptionInstruction) {
      return new GetCaughtExceptionStatement(node, ((SSAGetCaughtExceptionInstruction) s));
    } else {
      Integer x = instructionIndices.get(s);
      if (x == null) {
        Assertions.UNREACHABLE(s.toString() + "\nnot found in map of\n" + ir);
      }
      return new NormalStatement(node, x.intValue());
    }
  }

  /**
   * @return for each SSAInstruction, its instruction index in the ir instruction array
   */
  public static Map<SSAInstruction, Integer> computeInstructionIndices(IR ir) {
    Map<SSAInstruction, Integer> result = HashMapFactory.make();
    if (ir != null) {
      SSAInstruction[] instructions = ir.getInstructions();
      for (int i = 0; i < instructions.length; i++) {
        SSAInstruction s = instructions[i];
        if (s != null) {
          result.put(s, new Integer(i));
        }
      }
    }
    return result;
  }

  /**
   * Convert a NORMAL or PHI Statement to an SSAInstruction
   */
  private static SSAInstruction statement2SSAInstruction(SSAInstruction[] instructions, Statement s) {
    SSAInstruction statement = null;
    switch (s.getKind()) {
    case NORMAL:
      NormalStatement n = (NormalStatement) s;
      statement = instructions[n.getInstructionIndex()];
      break;
    case PHI:
      PhiStatement p = (PhiStatement) s;
      statement = p.getPhi();
      break;
    case PI:
      PiStatement ps = (PiStatement) s;
      statement = ps.getPi();
      break;
    case CATCH:
      GetCaughtExceptionStatement g = (GetCaughtExceptionStatement) s;
      statement = g.getInstruction();
      break;
    default:
      Assertions.UNREACHABLE(s.toString());
    }
    return statement;
  }

  /**
   * Create all nodes in this PDG. Each node is a Statement.
   */
  private void createNodes(Map<CGNode, OrdinalSet<PointerKey>> ref, IR ir) {

    if (ir != null) {
      createNormalStatements(ir, ref);
      createSpecialStatements(ir);
    }

    createCalleeParams();
    createReturnStatements();

    delegate.addNode(new MethodEntryStatement(node));
    delegate.addNode(new MethodExitStatement(node));
  }

  /**
   * create nodes representing defs of the return values
   * 
   * @param mod the set of heap locations which may be written (transitively) by this node. These are logically parameters in the
   *          SDG.
   * @param dOptions
   */
  private void createReturnStatements() {
    ArrayList<Statement> list = new ArrayList<>();
    if (!node.getMethod().getReturnType().equals(TypeReference.Void)) {
      NormalReturnCallee n = new NormalReturnCallee(node);
      delegate.addNode(n);
      list.add(n);
    }
    if (!dOptions.isIgnoreExceptions()) {
      ExceptionalReturnCallee e = new ExceptionalReturnCallee(node);
      delegate.addNode(e);
      list.add(e);
    }
    if (!dOptions.isIgnoreHeap()) {
      for (PointerKey p : mod.get(node)) {
        Statement h = new HeapStatement.HeapReturnCallee(node, p);
        delegate.addNode(h);
        list.add(h);
      }
    }
    returnStatements = new Statement[list.size()];
    list.toArray(returnStatements);
  }

  /**
   * create nodes representing defs of formal parameters
   * 
   * @param ref the set of heap locations which may be read (transitively) by this node. These are logically parameters in the SDG.
   */
  private void createCalleeParams() {
    if (paramCalleeStatements == null) {
      ArrayList<Statement> list = new ArrayList<>();
      int paramCount = node.getMethod().getNumberOfParameters();
      
      for (int i = 1; i <= paramCount; i++) {
        ParamCallee s = new ParamCallee(node, i);
        delegate.addNode(s);
        list.add(s);
      }
      if (!dOptions.isIgnoreHeap()) {
        for (PointerKey p : ref.get(node)) {
          Statement h = new HeapStatement.HeapParamCallee(node, p);
          delegate.addNode(h);
          list.add(h);
        }
      }
      paramCalleeStatements = new Statement[list.size()];
      list.toArray(paramCalleeStatements);
    }
  }

  /**
   * Create nodes corresponding to
   * <ul>
   * <li>phi instructions
   * <li>getCaughtExceptions
   * </ul>
   */
  private void createSpecialStatements(IR ir) {
    // create a node for instructions which do not correspond to bytecode
    for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
      if (s instanceof SSAPhiInstruction) {
        delegate.addNode(new PhiStatement(node, (SSAPhiInstruction) s));
      } else if (s instanceof SSAGetCaughtExceptionInstruction) {
        delegate.addNode(new GetCaughtExceptionStatement(node, (SSAGetCaughtExceptionInstruction) s));
      } else if (s instanceof SSAPiInstruction) {
        delegate.addNode(new PiStatement(node, (SSAPiInstruction) s));
      }
    }
  }

  /**
   * Create nodes in the graph corresponding to "normal" (bytecode) instructions
   */
  private void createNormalStatements(IR ir, Map<CGNode, OrdinalSet<PointerKey>> ref) {
    // create a node for every normal instruction in the IR
    SSAInstruction[] instructions = ir.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction s = instructions[i];

      if (s instanceof SSAGetCaughtExceptionInstruction) {
        continue;
      }

      if (s != null) {
        final NormalStatement statement = new NormalStatement(node, i);
        delegate.addNode(statement);
        if (s instanceof SSAAbstractInvokeInstruction) {
          callSite2Statement.put(((SSAAbstractInvokeInstruction) s).getCallSite(), statement);
          addParamPassingStatements(i, ref, ir);
        }
      }
    }
  }

  /**
   * Create nodes in the graph corresponding to in/out parameter passing for a call instruction
   */
  private void addParamPassingStatements(int callIndex, Map<CGNode, OrdinalSet<PointerKey>> ref, IR ir) {
    SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) ir.getInstructions()[callIndex];
    Collection<Statement> params = MapUtil.findOrCreateSet(callerParamStatements, call.getCallSite());
    Collection<Statement> rets = MapUtil.findOrCreateSet(callerReturnStatements, call.getCallSite());
    for (int j = 0; j < call.getNumberOfUses(); j++) {
      Statement st = new ParamCaller(node, callIndex, call.getUse(j));
      delegate.addNode(st);
      params.add(st);
    }
    if (!call.getDeclaredResultType().equals(TypeReference.Void)) {
      Statement st = new NormalReturnCaller(node, callIndex);
      delegate.addNode(st);
      rets.add(st);
    }
    {
      if (!dOptions.isIgnoreExceptions()) {
        Statement st = new ExceptionalReturnCaller(node, callIndex);
        delegate.addNode(st);
        rets.add(st);
      }
    }

    if (!dOptions.isIgnoreHeap()) {
      OrdinalSet<PointerKey> uref = unionHeapLocations(cg, node, call, ref);
      for (PointerKey p : uref) {
        Statement st = new HeapStatement.HeapParamCaller(node, callIndex, p);
        delegate.addNode(st);
        params.add(st);
      }
      OrdinalSet<PointerKey> umod = unionHeapLocations(cg, node, call, mod);
      for (PointerKey p : umod) {
        Statement st = new HeapStatement.HeapReturnCaller(node, callIndex, p);
        delegate.addNode(st);
        rets.add(st);
      }
    }
  }

  /**
   * @return the set of all locations read by any callee at a call site.
   */
  private static OrdinalSet<PointerKey> unionHeapLocations(CallGraph cg, CGNode n, SSAAbstractInvokeInstruction call,
      Map<CGNode, OrdinalSet<PointerKey>> loc) {
    BitVectorIntSet bv = new BitVectorIntSet();
    for (CGNode t : cg.getPossibleTargets(n, call.getCallSite())) {
      bv.addAll(loc.get(t).getBackingSet());
    }
    return new OrdinalSet<>(bv, loc.get(n).getMapping());
  }

  @Override
  public String toString() {
    populate();
    StringBuffer result = new StringBuffer("PDG for " + node + ":\n");
    result.append(super.toString());
    return result.toString();
  }

  public Statement[] getParamCalleeStatements() {
    if (paramCalleeStatements == null) {
      createCalleeParams();
    }
    Statement[] result = new Statement[paramCalleeStatements.length];
    System.arraycopy(paramCalleeStatements, 0, result, 0, result.length);
    return result;
  }

  public Statement[] getReturnStatements() {
    populate();
    Statement[] result = new Statement[returnStatements.length];
    System.arraycopy(returnStatements, 0, result, 0, result.length);
    return result;
  }

  public CGNode getCallGraphNode() {
    return node;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      return node.equals(((PDG) obj).node);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 103 * node.hashCode();
  }

  @Override
  public int getPredNodeCount(Statement N) throws UnimplementedError {
    populate();
    Assertions.UNREACHABLE();
    return delegate.getPredNodeCount(N);
  }

  @Override
  public Iterator<Statement> getPredNodes(Statement N) {
    populate();
    if (!dOptions.isIgnoreHeap()) {
      computeIncomingHeapDependencies(N);
    }
    return delegate.getPredNodes(N);
  }

  private void computeIncomingHeapDependencies(Statement N) {
    switch (N.getKind()) {
    case NORMAL:
      NormalStatement st = (NormalStatement) N;
      if (!(ignoreAllocHeapDefs && st.getInstruction() instanceof SSANewInstruction)) {
        Collection<PointerKey> ref = modRef.getRef(node, heapModel, pa, st.getInstruction(), exclusions);
        for (PointerKey pk : ref) {
          createHeapDataDependenceEdges(pk);
        }
      }
      break;
    case HEAP_PARAM_CALLEE:
    case HEAP_PARAM_CALLER:
    case HEAP_RET_CALLEE:
    case HEAP_RET_CALLER:
      HeapStatement h = (HeapStatement) N;
      createHeapDataDependenceEdges(h.getLocation());
      break;
    default:
      // do nothing
    }
  }

  private void computeOutgoingHeapDependencies(Statement N) {
    switch (N.getKind()) {
    case NORMAL:
      NormalStatement st = (NormalStatement) N;
      if (!(ignoreAllocHeapDefs && st.getInstruction() instanceof SSANewInstruction)) {
        Collection<PointerKey> mod = modRef.getMod(node, heapModel, pa, st.getInstruction(), exclusions);
        for (PointerKey pk : mod) {
          createHeapDataDependenceEdges(pk);
        }
      }
      break;
    case HEAP_PARAM_CALLEE:
    case HEAP_PARAM_CALLER:
    case HEAP_RET_CALLEE:
    case HEAP_RET_CALLER:
      HeapStatement h = (HeapStatement) N;
      createHeapDataDependenceEdges(h.getLocation());
      break;
    default:
      // do nothing
    }
  }

  @Override
  public int getSuccNodeCount(Statement N) throws UnimplementedError {
    populate();
    Assertions.UNREACHABLE();
    return delegate.getSuccNodeCount(N);
  }

  @Override
  public Iterator<Statement> getSuccNodes(Statement N) {
    populate();
    if (!dOptions.isIgnoreHeap()) {
      computeOutgoingHeapDependencies(N);
    }
    return delegate.getSuccNodes(N);
  }

  @Override
  public boolean hasEdge(Statement src, Statement dst) throws UnimplementedError {
    populate();
    return delegate.hasEdge(src, dst);
  }

  @Override
  public void removeNodeAndEdges(Statement N) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  @Override
  public void addNode(Statement n) {
    Assertions.UNREACHABLE();
  }

  @Override
  public boolean containsNode(Statement N) {
    populate();
    return delegate.containsNode(N);
  }

  @Override
  public int getNumberOfNodes() {
    populate();
    return delegate.getNumberOfNodes();
  }

  @Override
  public Iterator<Statement> iterator() {
    populate();
    return delegate.iterator();
  }

  @Override
  public void removeNode(Statement n) {
    Assertions.UNREACHABLE();
  }

  @Override
  public void addEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeAllIncidentEdges(Statement node) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeEdge(Statement src, Statement dst) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeIncomingEdges(Statement node) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeOutgoingEdges(Statement node) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  @Override
  public int getMaxNumber() {
    populate();
    return delegate.getMaxNumber();
  }

  @Override
  public Statement getNode(int number) {
    populate();
    return delegate.getNode(number);
  }

  @Override
  public int getNumber(Statement N) {
    populate();
    return delegate.getNumber(N);
  }

  @Override
  public Iterator<Statement> iterateNodes(IntSet s) {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public IntSet getPredNodeNumbers(Statement node) {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public IntSet getSuccNodeNumbers(Statement node) {
    Assertions.UNREACHABLE();
    return null;
  }
/** BEGIN Custom change: control deps */
  public boolean isControlDependend(Statement from, Statement to) {
    return delegate.hasEdge(from, to, Dependency.CONTROL_DEP);
  }
/** END Custom change: control deps */
}
