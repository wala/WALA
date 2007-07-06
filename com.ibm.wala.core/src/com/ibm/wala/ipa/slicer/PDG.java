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

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.ParamStatement.ValueNumberCarrier;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
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
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * Program dependence graph for a single call graph node
 * 
 * @author sjfink
 * 
 */
public class PDG extends SlowSparseNumberedGraph<Statement> {

  public final static boolean IGNORE_ALLOC_HEAP_DEFS = false;

  private final static boolean VERBOSE = false;

  private final CGNode node;

  private final Map<SSAInstruction, Integer> instructionIndices;

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

  /**
   * @param mod
   *            the set of heap locations which may be written (transitively) by
   *            this node. These are logically return values in the SDG.
   * @param ref
   *            the set of heap locations which may be read (transitively) by
   *            this node. These are logically parameters in the SDG.
   * @throws IllegalArgumentException
   *             if node is null
   */
  public PDG(final CGNode node, PointerAnalysis pa, Map<CGNode, OrdinalSet<PointerKey>> mod,
      Map<CGNode, OrdinalSet<PointerKey>> ref, DataDependenceOptions dOptions, ControlDependenceOptions cOptions,
      HeapExclusions exclusions) {

    super();
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    this.node = node;
    this.heapModel = pa == null ? null : new DelegatingExtendedHeapModel(pa.getHeapModel());
    this.pa = pa;
    this.dOptions = dOptions;
    this.mod = mod;
    this.exclusions = exclusions;
    instructionIndices = computeInstructionIndices(node.getIR(new WarningSet()));
    createNodes(ref, cOptions);
    createScalarEdges(cOptions);
  }

  private void createScalarEdges(ControlDependenceOptions cOptions) {
    createScalarDataDependenceEdges();
    createControlDependenceEdges(cOptions);
  }

  public Set<Statement> getCallerParamStatements(SSAAbstractInvokeInstruction call) {
    return callerParamStatements.get(call.getCallSite());
  }

  public Set<Statement> getCallerReturnStatements(SSAAbstractInvokeInstruction call) {
    return callerReturnStatements.get(call.getCallSite());
  }

  /**
   * Create all control dependence edges in this PDG.
   */
  private void createControlDependenceEdges(ControlDependenceOptions cOptions) {
    if (cOptions.equals(ControlDependenceOptions.NONE)) {
      return;
    }
    Assertions.productionAssertion(cOptions.equals(ControlDependenceOptions.FULL));
    IR ir = node.getIR(new WarningSet());
    if (ir == null) {
      return;
    }
    ControlDependenceGraph cdg = new ControlDependenceGraph(ir.getControlFlowGraph());
    for (Iterator<? extends IBasicBlock> it = cdg.iterator(); it.hasNext();) {
      IBasicBlock bb = it.next();
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
          // should have no control dependence successors.
          // leave src null.
        } else {
          src = ssaInstruction2Statement(s);
          // add edges from call statements to parameter passing and return
          if (s instanceof SSAAbstractInvokeInstruction) {
            SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) s;
            for (Statement st : callerParamStatements.get(call.getCallSite())) {
              addEdge(src, st);
            }
            for (Statement st : callerReturnStatements.get(call.getCallSite())) {
              addEdge(src, st);
            }
          }
        }
      }
      // add edges for every control-dependent statement in the IR
      for (Iterator<? extends IBasicBlock> succ = cdg.getSuccNodes(bb); succ.hasNext();) {
        IBasicBlock bb2 = succ.next();
        for (Iterator<? extends IInstruction> it2 = bb2.iterator(); it2.hasNext();) {
          SSAInstruction st = (SSAInstruction) it2.next();
          if (st != null) {
            Statement dest = ssaInstruction2Statement(st);
            assert src != null;
            addEdge(src, dest);
          }
        }
      }
    }

    // the CDG does not represent control dependences from the entry node.
    // add these manually
    Statement methodEntry = new MethodEntryStatement(node);
    for (Iterator<? extends IBasicBlock> it = cdg.iterator(); it.hasNext();) {
      IBasicBlock bb = it.next();
      if (cdg.getPredNodeCount(bb) == 0) {
        // this is control dependent on the method entry.
        for (Iterator<? extends IInstruction> it2 = bb.iterator(); it2.hasNext();) {
          SSAInstruction st = (SSAInstruction) it2.next();
          Statement dest = ssaInstruction2Statement(st);
          addEdge(methodEntry, dest);
        }
      }
    }
    // add CD from method entry to all callee parameter assignments
    for (int i = 0; i < paramCalleeStatements.length; i++) {
      addEdge(methodEntry, paramCalleeStatements[i]);
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
  private void createScalarDataDependenceEdges() {
    if (dOptions.equals(DataDependenceOptions.NONE)) {
      return;
    }

    IR ir = node.getIR(new WarningSet());
    if (ir == null) {
      return;
    }

    DefUse DU = node.getDU(new WarningSet());
    SSAInstruction[] instructions = ir.getInstructions();
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
              addEdge(s, u);
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
          addEdge(s, u);
        }
        break;
      }
      case NORMAL_RET_CALLEE:
        for (NormalStatement ret : computeReturnStatements(ir)) {
          addEdge(ret, s);
        }
        break;
      case EXC_RET_CALLEE:
        if (Assertions.verifyAssertions && dOptions.isIgnoreExceptions()) {
          Assertions.UNREACHABLE();
        }
        // TODO: this is overly conservative. deal with catch blocks?
        for (NormalStatement pei : getPEIs(ir)) {
          if (dOptions.isTerminateAtCast() && (pei.getInstruction() instanceof SSACheckCastInstruction)) {
            continue;
          }
          if (pei.getInstruction() instanceof SSAAbstractInvokeInstruction) {
            SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) pei.getInstruction();
            Statement st = new ParamStatement.ExceptionalReturnCaller(node, call);
            addEdge(st, s);
          } else {
            addEdge(pei, s);
          }
        }
        break;
      case PARAM_CALLER: {
        ParamStatement.ParamCaller pac = (ParamStatement.ParamCaller) s;
        int vn = pac.getValueNumber();
        // note that if the caller is the fake root method and the parameter
        // type is primitive,
        // it's possible to have a value number of -1. If so, just ignore it.
        if (vn > -1) {
          if (ir.getSymbolTable().isParameter(vn)) {
            Statement a = new ParamStatement.ParamCallee(node, vn);
            addEdge(a, pac);
          } else {
            SSAInstruction d = DU.getDef(vn);
            if (dOptions.isTerminateAtCast() && (d instanceof SSACheckCastInstruction)) {
              break;
            }
            if (d != null) {
              if (d instanceof SSAAbstractInvokeInstruction) {
                SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) d;
                if (vn == call.getException()) {
                  Statement st = new ParamStatement.ExceptionalReturnCaller(node, call);
                  addEdge(st, pac);
                } else {
                  Statement st = new ParamStatement.NormalReturnCaller(node, call);
                  addEdge(st, pac);
                }
              } else {
                Statement ds = ssaInstruction2Statement(d);
                addEdge(ds, pac);
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
   * Create heap data dependence edges in this PDG relevant to a particular
   * statement.
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

    IR ir = node.getIR(new WarningSet());
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

    Map<Statement, OrdinalSet<Statement>> heapReachingDefs = dOptions.isIgnoreHeap() ? null : HeapReachingDefs.computeReachingDefs(
        node, ir, pa, mod, relevantStatements, new HeapExclusions(SetComplement.complement(new SingletonSet(t))));

    for (Statement st : heapReachingDefs.keySet()) {
      switch (st.getKind()) {
      case NORMAL:
      case CATCH:
      case PHI:
      case PI: {
        OrdinalSet<Statement> defs = heapReachingDefs.get(st);
        if (defs != null) {
          for (Statement def : defs) {
            addEdge(def, st);
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
            addEdge(def, st);
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
   * @return Statements representing each PEI in the ir
   */
  private Collection<NormalStatement> getPEIs(final IR ir) {
    Filter filter = new Filter() {
      public boolean accepts(Object o) {
        if (o instanceof NormalStatement) {
          NormalStatement s = (NormalStatement) o;
          SSAInstruction st = ir.getInstructions()[s.getInstructionIndex()];
          return st.isPEI();
        } else {
          return false;
        }
      }
    };
    return Iterator2Collection.toCollection(new FilterIterator<NormalStatement>(iterator(), filter));
  }

  /**
   * Wrap an SSAInstruction in a Statement
   */
  Statement ssaInstruction2Statement(SSAInstruction s) {
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
        Assertions.UNREACHABLE(s.toString());
      }
      return new NormalStatement(node, x.intValue());
    }
  }

  /**
   * @return for each SSAInstruction, its instruction index in the ir
   *         instruction array
   */
  private Map<SSAInstruction, Integer> computeInstructionIndices(IR ir) {
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
    IR ir = node.getIR(new WarningSet());

    if (ir != null) {
      Collection<SSAInstruction> visited = createNormalStatements(ir, ref);
      createSpecialStatements(ir, visited);
    }

    createCalleeParams(ref);
    createReturnStatements();

    if (!cOptions.equals(ControlDependenceOptions.NONE)) {
      addNode(new MethodEntryStatement(node));
    }

  }

  /**
   * create nodes representing defs of the return values
   * 
   * @param mod
   *            the set of heap locations which may be written (transitively) by
   *            this node. These are logically parameters in the SDG.
   * @param dOptions
   */
  private void createReturnStatements() {
    ArrayList<Statement> list = new ArrayList<Statement>();
    if (!node.getMethod().getReturnType().equals(TypeReference.Void)) {
      ParamStatement.NormalReturnCallee n = new ParamStatement.NormalReturnCallee(node);
      addNode(n);
      list.add(n);
    }
    if (!dOptions.isIgnoreExceptions()) {
      ParamStatement.ExceptionalReturnCallee e = new ParamStatement.ExceptionalReturnCallee(node);
      addNode(e);
      list.add(e);
    }
    if (!dOptions.isIgnoreHeap()) {
      for (PointerKey p : mod.get(node)) {
        Statement h = new HeapStatement.ReturnCallee(node, p);
        addNode(h);
        list.add(h);
      }
    }
    returnStatements = new Statement[list.size()];
    list.toArray(returnStatements);
  }

  /**
   * create nodes representing defs of formal parameters
   * 
   * @param ref
   *            the set of heap locations which may be read (transitively) by
   *            this node. These are logically parameters in the SDG.
   */
  private void createCalleeParams(Map<CGNode, OrdinalSet<PointerKey>> ref) {

    ArrayList<Statement> list = new ArrayList<Statement>();
    for (int i = 1; i <= node.getMethod().getNumberOfParameters(); i++) {
      ParamStatement s = new ParamStatement.ParamCallee(node, i);
      addNode(s);
      list.add(s);
    }
    if (!dOptions.isIgnoreHeap()) {
      for (PointerKey p : ref.get(node)) {
        Statement h = new HeapStatement.ParamCallee(node, p);
        addNode(h);
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
          addNode(new PhiStatement(node, (SSAPhiInstruction) s));
        } else if (s instanceof SSAGetCaughtExceptionInstruction) {
          addNode(new GetCaughtExceptionStatement(node, (SSAGetCaughtExceptionInstruction) s));
        } else if (s instanceof SSAPiInstruction) {
          addNode(new PiStatement(node, (SSAPiInstruction) s));
        } else {
          Assertions.UNREACHABLE(s.toString());
        }
      }
    }
  }

  /**
   * Create nodes in the graph corresponding to "normal" (bytecode) instructions
   * 
   * @param options
   * 
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
        addNode(new NormalStatement(node, i));
        visited.add(s);
      }
      if (s instanceof SSAAbstractInvokeInstruction) {
        addParamPassingStatements((SSAAbstractInvokeInstruction) s, ref);
      }
    }
    return visited;
  }

  /**
   * Create nodes in the graph corresponding to in/out parameter passing for a
   * call instruction
   * 
   * @param dOptions
   */
  private void addParamPassingStatements(SSAAbstractInvokeInstruction call, Map<CGNode, OrdinalSet<PointerKey>> ref) {

    Collection<Statement> params = MapUtil.findOrCreateSet(callerParamStatements, call.getCallSite());
    Collection<Statement> rets = MapUtil.findOrCreateSet(callerReturnStatements, call.getCallSite());
    for (int j = 0; j < call.getNumberOfUses(); j++) {
      Statement st = new ParamStatement.ParamCaller(node, call, call.getUse(j));
      addNode(st);
      params.add(st);
    }
    if (!call.getDeclaredResultType().equals(TypeReference.Void)) {
      Statement st = new ParamStatement.NormalReturnCaller(node, call);
      addNode(st);
      rets.add(st);
    }
    {
      if (!dOptions.isIgnoreExceptions()) {
        Statement st = new ParamStatement.ExceptionalReturnCaller(node, call);
        addNode(st);
        rets.add(st);
      }
    }

    if (!dOptions.isIgnoreHeap()) {
      OrdinalSet<PointerKey> uref = unionHeapLocations(node, call, ref);
      for (PointerKey p : uref) {
        Statement st = new HeapStatement.ParamCaller(node, call, p);
        addNode(st);
        params.add(st);
      }
      OrdinalSet<PointerKey> umod = unionHeapLocations(node, call, mod);
      for (PointerKey p : umod) {
        Statement st = new HeapStatement.ReturnCaller(node, call, p);
        addNode(st);
        rets.add(st);
      }
    }
  }

  /**
   * @return the set of all locations read by any callee at a call site.
   */
  private OrdinalSet<PointerKey> unionHeapLocations(CGNode n, SSAAbstractInvokeInstruction call,
      Map<CGNode, OrdinalSet<PointerKey>> loc) {
    BitVectorIntSet bv = new BitVectorIntSet();
    for (CGNode t : n.getPossibleTargets(call.getCallSite())) {
      bv.addAll(loc.get(t).getBackingSet());
    }
    return new OrdinalSet<PointerKey>(bv, loc.get(n).getMapping());
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("PDG for " + node + ":\n");
    result.append(super.toString());
    return result.toString();
  }

  public Statement[] getParamCalleeStatements() {
    return paramCalleeStatements.clone();
  }

  public Statement[] getReturnStatements() {
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

  @Override
  public int getPredNodeCount(Statement N) {
    Assertions.UNREACHABLE();
    return super.getPredNodeCount(N);
  }

  @Override
  public Iterator<? extends Statement> getPredNodes(Statement N) {
    if (!dOptions.isIgnoreHeap()) {
      computeIncomingHeapDependencies(N);
    }
    return super.getPredNodes(N);
  }

  private void computeIncomingHeapDependencies(Statement N) {
    switch (N.getKind()) {
    case NORMAL:
      NormalStatement st = (NormalStatement) N;
      if (!(IGNORE_ALLOC_HEAP_DEFS && st.getInstruction() instanceof SSANewInstruction)) {
        Collection<PointerKey> ref = ModRef.getRef(node, heapModel, pa, st.getInstruction(), exclusions);
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
      if (!(IGNORE_ALLOC_HEAP_DEFS && st.getInstruction() instanceof SSANewInstruction)) {
        Collection<PointerKey> ref = ModRef.getMod(node, heapModel, pa, st.getInstruction(), exclusions);
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

  @Override
  public int getSuccNodeCount(Statement N) {
    Assertions.UNREACHABLE();
    return super.getSuccNodeCount(N);
  }

  @Override
  public Iterator<? extends Statement> getSuccNodes(Statement N) {
    if (!dOptions.isIgnoreHeap()) {
      computeOutgoingHeapDependencies(N);
    }
    return super.getSuccNodes(N);
  }

  @Override
  public boolean hasEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
    return super.hasEdge(src, dst);
  }

}
