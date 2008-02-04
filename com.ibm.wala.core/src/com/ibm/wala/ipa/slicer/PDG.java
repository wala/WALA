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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.shrikeBT.IInstruction;
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
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Program dependence graph for a single call graph node
 * 
 * @author sjfink
 * 
 */
public class PDG implements NumberedGraph<Statement> {

  private final SlowSparseNumberedGraph<Statement> delegate = SlowSparseNumberedGraph.make();

  private final static boolean VERBOSE = false;

  private final CGNode node;

  private Map<SSAInstruction, Integer> instructionIndices;

  private Statement[] paramCalleeStatements;

  private Statement[] returnStatements;

  /**
   * TODO: using CallSiteReference is sloppy. clean it up.
   */
  private final Map<CallSiteReference, Set<Statement>> callerParamStatements = HashMapFactory.make();

  private final Map<CallSiteReference, Set<Statement>> callerReturnStatements = HashMapFactory.make();

  private final HeapExclusions exclusions;

  private final Collection<PointerKey> locationsHandled = HashSetFactory.make();

  private final PointerAnalysis pa;

  private final ExtendedHeapModel heapModel;

  private final Map<CGNode, OrdinalSet<PointerKey>> mod;

  private final DataDependenceOptions dOptions;

  private final ControlDependenceOptions cOptions;

  private final CallGraph cg;

  private final ModRef modRef;

  private final Map<CGNode, OrdinalSet<PointerKey>> ref;

  private final boolean ignoreAllocHeapDefs;

  private boolean isPopulated = false;

  /**
   * @param mod the set of heap locations which may be written (transitively) by this node. These are logically return
   *        values in the SDG.
   * @param ref the set of heap locations which may be read (transitively) by this node. These are logically parameters
   *        in the SDG.
   * @throws IllegalArgumentException if node is null
   */
  public PDG(final CGNode node, PointerAnalysis pa, Map<CGNode, OrdinalSet<PointerKey>> mod,
      Map<CGNode, OrdinalSet<PointerKey>> ref, DataDependenceOptions dOptions, ControlDependenceOptions cOptions,
      HeapExclusions exclusions, CallGraph cg, ModRef modRef) {
    this(node, pa, mod, ref, dOptions, cOptions, exclusions, cg, modRef, false);
  }

  /**
   * @param mod the set of heap locations which may be written (transitively) by this node. These are logically return
   *        values in the SDG.
   * @param ref the set of heap locations which may be read (transitively) by this node. These are logically parameters
   *        in the SDG.
   * @throws IllegalArgumentException if node is null
   */
  public PDG(final CGNode node, PointerAnalysis pa, Map<CGNode, OrdinalSet<PointerKey>> mod,
      Map<CGNode, OrdinalSet<PointerKey>> ref, DataDependenceOptions dOptions, ControlDependenceOptions cOptions,
      HeapExclusions exclusions, CallGraph cg, ModRef modRef, boolean ignoreAllocHeapDefs) {

    super();
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    this.cg = cg;
    this.node = node;
    this.heapModel = pa == null ? null : new DelegatingExtendedHeapModel(pa.getHeapModel());
    this.pa = pa;
    this.dOptions = dOptions;
    this.cOptions = cOptions;
    this.mod = mod;
    this.exclusions = exclusions;
    this.modRef = modRef;
    this.ref = ref;
    this.ignoreAllocHeapDefs = ignoreAllocHeapDefs;
  }

  private void populate() {
    if (!isPopulated) {
      isPopulated = true;
      instructionIndices = computeInstructionIndices(node.getIR());
      createNodes(ref, cOptions);
      createScalarEdges(cOptions);
    }
  }

  private void createScalarEdges(ControlDependenceOptions cOptions) {
    createScalarDataDependenceEdges();
    createControlDependenceEdges(cOptions);
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
  private void createControlDependenceEdges(ControlDependenceOptions cOptions) {
    if (cOptions.equals(ControlDependenceOptions.NONE)) {
      return;
    }
    IR ir;
    ir = node.getIR();
    if (ir == null) {
      return;
    }
    ControlFlowGraph<ISSABasicBlock> controlFlowGraph = ir.getControlFlowGraph();
    if (cOptions.equals(ControlDependenceOptions.NO_EXCEPTIONAL_EDGES)) {
      controlFlowGraph = ExceptionPrunedCFG.make(controlFlowGraph);
      // In case the CFG has no nodes left because the only control dependencies
      // were
      // exceptional, simply return because at this point there are no nodes.
      // Otherwise, later this may raise an Exception.
      if (controlFlowGraph.getNumberOfNodes() == 0) {
        return;
      }
    } else {
      Assertions.productionAssertion(cOptions.equals(ControlDependenceOptions.FULL));
    }

    ControlDependenceGraph<ISSABasicBlock> cdg = new ControlDependenceGraph<ISSABasicBlock>(controlFlowGraph);
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
          src = ssaInstruction2Statement(s);
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
        for (Iterator<? extends IBasicBlock> succ = cdg.getSuccNodes(bb); succ.hasNext();) {
          IBasicBlock bb2 = succ.next();
          for (Iterator<? extends IInstruction> it2 = bb2.iterator(); it2.hasNext();) {
            SSAInstruction st = (SSAInstruction) it2.next();
            if (st != null) {
              Statement dest = ssaInstruction2Statement(st);
              assert src != null;
              delegate.addEdge(src, dest);
            }
          }
        }
      }
    }

    // the CDG does not represent control dependences from the entry node.
    // add these manually
    Statement methodEntry = new MethodEntryStatement(node);
    for (Iterator<? extends ISSABasicBlock> it = cdg.iterator(); it.hasNext();) {
      ISSABasicBlock bb = it.next();
      if (cdg.getPredNodeCount(bb) == 0) {
        // this is control dependent on the method entry.
        for (IInstruction s : bb) {
          SSAInstruction st = (SSAInstruction) s;
          Statement dest = ssaInstruction2Statement(st);
          delegate.addEdge(methodEntry, dest);
        }
      }
    }
    // add CD from method entry to all callee parameter assignments
    // SJF: Alexey and I think that we should just define ParamStatements as
    // being control dependent on nothing ... they only represent pure
    // data dependence. So, I'm commenting out the following.
    // for (int i = 0; i < paramCalleeStatements.length; i++) {
    // addEdge(methodEntry, paramCalleeStatements[i]);
    // }
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
  private void createScalarDataDependenceEdges() {
    if (dOptions.equals(DataDependenceOptions.NONE)) {
      return;
    }

    IR ir = node.getIR();
    if (ir == null) {
      return;
    }

    DefUse DU = node.getDU();
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
            Statement c = ssaInstruction2Statement(ehbb.getCatchInstruction());

            for (ISSABasicBlock pb : ir.getControlFlowGraph().getExceptionalPredecessors(ehbb)) {
              SSAInstruction pi = instructions[pb.getLastInstructionIndex()];
              assert pi != null;

              if (pi instanceof SSAAbstractInvokeInstruction) {
                delegate.addEdge(new ExceptionalReturnCaller(node, pb.getLastInstructionIndex()), c);
              } else if (pi instanceof SSAAbstractThrowInstruction) {
                delegate.addEdge(ssaInstruction2Statement(pi), c);
              }
            }
          }
        }
      }
    }

    for (Iterator<? extends Statement> it = iterator(); it.hasNext();) {
      Statement s = it.next();
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
            for (Iterator<SSAInstruction> it2 = DU.getUses(def); it2.hasNext();) {
              SSAInstruction use = it2.next();
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
              Statement u = ssaInstruction2Statement(use);
              delegate.addEdge(s, u);
            }
          }
        }
        break;
      }
      case EXC_RET_CALLER:
      case NORMAL_RET_CALLER:
      case PARAM_CALLEE: {
        if (Assertions.verifyAssertions && dOptions.isIgnoreExceptions()) {
          Assertions._assert(!s.getKind().equals(Kind.EXC_RET_CALLER));
        }
        ValueNumberCarrier a = (ValueNumberCarrier) s;
        for (Iterator<SSAInstruction> it2 = DU.getUses(a.getValueNumber()); it2.hasNext();) {
          SSAInstruction use = it2.next();
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
          Statement u = ssaInstruction2Statement(use);
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
        if (Assertions.verifyAssertions && dOptions.isIgnoreExceptions()) {
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
            Statement st = new ExceptionalReturnCaller(node, index);
            delegate.addEdge(st, s);
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
                  Statement st = new ExceptionalReturnCaller(node, instructionIndices.get(d));
                  delegate.addEdge(st, pac);
                } else {
                  Statement st = new NormalReturnCaller(node, instructionIndices.get(d));
                  delegate.addEdge(st, pac);
                }
              } else {
                Statement ds = ssaInstruction2Statement(d);
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
        // do nothing
        break;
      default:
        Assertions.UNREACHABLE(s.toString());
        break;
      }
    }
  }

  private static class SingletonSet extends SetOfClasses {

    private final TypeReference t;

    SingletonSet(TypeReference t) {
      this.t = t;
    }

    @Override
    public void add(IClass klass) {
      Assertions.UNREACHABLE();
    }

    @Override
    public boolean contains(String klassName) {
      Assertions.UNREACHABLE();
      return false;
    }

    @Override
    public boolean contains(TypeReference klass) {
      return t.equals(klass);
    }
  }

  private static class SetComplement extends SetOfClasses {
    private final SetOfClasses set;

    SetComplement(SetOfClasses set) {
      this.set = set;
    }

    static SetComplement complement(SetOfClasses set) {
      return new SetComplement(set);
    }

    @Override
    public void add(IClass klass) {
      Assertions.UNREACHABLE();
    }

    @Override
    public boolean contains(String klassName) {
      Assertions.UNREACHABLE();
      return false;
    }

    @Override
    public boolean contains(TypeReference klass) {
      return !set.contains(klass);
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

    IR ir = node.getIR();
    if (ir == null) {
      return;
    }

    if (VERBOSE) {
      System.err.println("Location " + pk);
    }

    // in reaching defs calculation, exclude heap statements that are
    // irrelevant.
    Filter f = new Filter() {
      public boolean accepts(Object o) {
        if (o instanceof HeapStatement) {
          HeapStatement h = (HeapStatement) o;
          return h.getLocation().equals(pk);
        } else {
          return true;
        }
      }
    };
    Collection<Statement> relevantStatements = Iterator2Collection.toCollection(new FilterIterator<Statement>(iterator(), f));

    Map<Statement, OrdinalSet<Statement>> heapReachingDefs = dOptions.isIgnoreHeap() ? null : (new HeapReachingDefs(modRef))
        .computeReachingDefs(node, ir, pa, mod, relevantStatements, new HeapExclusions(SetComplement
            .complement(new SingletonSet(t))), cg);

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
        // do nothing .. there are no incoming edges
        break;
      default:
        Assertions.UNREACHABLE(st.toString());
        break;
      }
    }
  }

  private boolean hasBasePointer(SSAInstruction use) {
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

  private int getBasePointer(SSAInstruction use) {
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
    Filter filter = new Filter() {
      public boolean accepts(Object o) {
        if (o instanceof NormalStatement) {
          NormalStatement s = (NormalStatement) o;
          SSAInstruction st = ir.getInstructions()[s.getInstructionIndex()];
          return st instanceof SSAReturnInstruction;
        } else {
          return false;
        }
      }
    };
    return Iterator2Collection.toCollection(new FilterIterator<NormalStatement>(iterator(), filter));
  }

  /**
   * @return {@link IntSet} representing instruction indices of each PEI in the ir
   */
  private IntSet getPEIs(final IR ir) {
    BitVectorIntSet result = new BitVectorIntSet();
    for (int i = 0; i < ir.getInstructions().length; i++) {
      if (ir.getInstructions()[i] != null && ir.getInstructions()[i].isPEI()) {
        result.add(i);
      }
    }
    return result;
  }

  /**
   * Wrap an SSAInstruction in a Statement
   */
  private Statement ssaInstruction2Statement(SSAInstruction s) {
    return ssaInstruction2Statement(node, s, instructionIndices);
  }

  public static synchronized Statement ssaInstruction2Statement(CGNode node, SSAInstruction s,
      Map<SSAInstruction, Integer> instructionIndices) {
    assert s != null;
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
        Assertions.UNREACHABLE(s.toString() + "\nnot found in map of\n" + node.getIR());
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
  private SSAInstruction statement2SSAInstruction(SSAInstruction[] instructions, Statement s) {
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
   * 
   * @param dOptions
   */
  private void createNodes(Map<CGNode, OrdinalSet<PointerKey>> ref, ControlDependenceOptions cOptions) {
    IR ir = node.getIR();

    if (ir != null) {
      Collection<SSAInstruction> visited = createNormalStatements(ir, ref);
      createSpecialStatements(ir, visited);
    }

    createCalleeParams(ref);
    createReturnStatements();

    delegate.addNode(new MethodEntryStatement(node));
  }

  /**
   * create nodes representing defs of the return values
   * 
   * @param mod the set of heap locations which may be written (transitively) by this node. These are logically
   *        parameters in the SDG.
   * @param dOptions
   */
  private void createReturnStatements() {
    ArrayList<Statement> list = new ArrayList<Statement>();
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
   * @param ref the set of heap locations which may be read (transitively) by this node. These are logically parameters
   *        in the SDG.
   */
  private void createCalleeParams(Map<CGNode, OrdinalSet<PointerKey>> ref) {
    ArrayList<Statement> list = new ArrayList<Statement>();
    for (int i = 1; i <= node.getMethod().getNumberOfParameters(); i++) {
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

  /**
   * Create nodes corresponding to
   * <ul>
   * <li> phi instructions
   * <li> getCaughtExceptions
   * </ul>
   */
  private void createSpecialStatements(IR ir, Collection<SSAInstruction> visited) {
    // create a node for instructions which do not correspond to bytecode
    for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
      SSAInstruction s = it.next();
      if (s != null && !visited.contains(s)) {
        visited.add(s);
        if (s instanceof SSAPhiInstruction) {
          delegate.addNode(new PhiStatement(node, (SSAPhiInstruction) s));
        } else if (s instanceof SSAGetCaughtExceptionInstruction) {
          delegate.addNode(new GetCaughtExceptionStatement(node, (SSAGetCaughtExceptionInstruction) s));
        } else if (s instanceof SSAPiInstruction) {
          delegate.addNode(new PiStatement(node, (SSAPiInstruction) s));
        } else {
          Assertions.UNREACHABLE(s.toString());
        }
      }
    }
  }

  /**
   * Create nodes in the graph corresponding to "normal" (bytecode) instructions
   */
  private Collection<SSAInstruction> createNormalStatements(IR ir, Map<CGNode, OrdinalSet<PointerKey>> ref) {
    Collection<SSAInstruction> visited = HashSetFactory.make();
    // create a node for every normal instruction in the IR
    SSAInstruction[] instructions = ir.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction s = instructions[i];

      if (s instanceof SSAGetCaughtExceptionInstruction) {
        continue;
      }

      if (s != null) {
        delegate.addNode(new NormalStatement(node, i));
        visited.add(s);
      }
      if (s instanceof SSAAbstractInvokeInstruction) {
        addParamPassingStatements(i, ref);
      }
    }
    return visited;
  }

  /**
   * Create nodes in the graph corresponding to in/out parameter passing for a call instruction
   */
  private void addParamPassingStatements(int callIndex, Map<CGNode, OrdinalSet<PointerKey>> ref) {

    SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) node.getIR().getInstructions()[callIndex];
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
  private OrdinalSet<PointerKey> unionHeapLocations(CallGraph cg, CGNode n, SSAAbstractInvokeInstruction call,
      Map<CGNode, OrdinalSet<PointerKey>> loc) {
    BitVectorIntSet bv = new BitVectorIntSet();
    for (CGNode t : cg.getPossibleTargets(n, call.getCallSite())) {
      bv.addAll(loc.get(t).getBackingSet());
    }
    return new OrdinalSet<PointerKey>(bv, loc.get(n).getMapping());
  }

  @Override
  public String toString() {
    populate();
    StringBuffer result = new StringBuffer("PDG for " + node + ":\n");
    result.append(super.toString());
    return result.toString();
  }

  public Statement[] getParamCalleeStatements() {
    populate();
    return paramCalleeStatements.clone();
  }

  public Statement[] getReturnStatements() {
    populate();
    return returnStatements.clone();
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

  public int getPredNodeCount(Statement N) throws UnimplementedError {
    populate();
    Assertions.UNREACHABLE();
    return delegate.getPredNodeCount(N);
  }

  public Iterator<? extends Statement> getPredNodes(Statement N) {
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
    }
  }

  public int getSuccNodeCount(Statement N) throws UnimplementedError {
    populate();
    Assertions.UNREACHABLE();
    return delegate.getSuccNodeCount(N);
  }

  public Iterator<? extends Statement> getSuccNodes(Statement N) {
    populate();
    if (!dOptions.isIgnoreHeap()) {
      computeOutgoingHeapDependencies(N);
    }
    return delegate.getSuccNodes(N);
  }

  public boolean hasEdge(Statement src, Statement dst) throws UnimplementedError {
    populate();
    return delegate.hasEdge(src, dst);
  }

  public void removeNodeAndEdges(Statement N) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  public void addNode(Statement n) {
    Assertions.UNREACHABLE();
  }

  public boolean containsNode(Statement N) {
    populate();
    return delegate.containsNode(N);
  }

  public int getNumberOfNodes() {
    populate();
    return delegate.getNumberOfNodes();
  }

  public Iterator<Statement> iterator() {
    populate();
    return delegate.iterator();
  }

  public void removeNode(Statement n) {
    Assertions.UNREACHABLE();
  }

  public void addEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
  }

  public void removeAllIncidentEdges(Statement node) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  public void removeEdge(Statement src, Statement dst) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  public void removeIncomingEdges(Statement node) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  public void removeOutgoingEdges(Statement node) throws UnsupportedOperationException {
    Assertions.UNREACHABLE();
  }

  public int getMaxNumber() {
    populate();
    return delegate.getMaxNumber();
  }

  public Statement getNode(int number) {
    populate();
    return delegate.getNode(number);
  }

  public int getNumber(Statement N) {
    populate();
    return delegate.getNumber(N);
  }

  public Iterator<Statement> iterateNodes(IntSet s) {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getPredNodeNumbers(Statement node) {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getSuccNodeNumbers(Statement node) {
    Assertions.UNREACHABLE();
    return null;
  }
}
