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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorMinusVector;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionVector;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapReturnCaller;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * Computation of reaching definitions for heap locations, relying on pointer analysis
 */
public class HeapReachingDefs<T extends InstanceKey> {

  private static final boolean DEBUG = false;

  private static final boolean VERBOSE = false;

  private final ModRef<T> modRef;

  private final ExtendedHeapModel heapModel;
  
  public HeapReachingDefs(ModRef<T> modRef, ExtendedHeapModel heapModel) {
    this.modRef = modRef;
    this.heapModel = heapModel;
  }

  /**
   * For each statement s, return the set of statements that may def the heap value read by s.
   * 
   * @param node the node we are computing heap reaching defs for
   * @param ir IR for the node
   * @param pa governing pointer analysis
   * @param mod the set of heap locations which may be written (transitively) by this node. These are logically return values in the
   *          SDG.
   * @param statements the statements whose def-use are considered interesting
   * @param exclusions heap locations that should be excluded from data dependence tracking
   * 
   * @throws IllegalArgumentException if pa is null
   * @throws IllegalArgumentException if statements is null
   */
  @SuppressWarnings("unused")
  public Map<Statement, OrdinalSet<Statement>> computeReachingDefs(CGNode node, IR ir, PointerAnalysis<T> pa,
      Map<CGNode, OrdinalSet<PointerKey>> mod, Collection<Statement> statements, HeapExclusions exclusions, CallGraph cg) {

    if (statements == null) {
      throw new IllegalArgumentException("statements is null");
    }
    if (pa == null) {
      throw new IllegalArgumentException("pa is null");
    }
    if (VERBOSE | DEBUG) {
      System.err.println("Reaching Defs " + node);
      System.err.println(statements.size());
    }

    if (DEBUG) {
      System.err.println(ir);
    }

    // create a control flow graph with one instruction per basic block.
    ExplodedControlFlowGraph cfg = ExplodedControlFlowGraph.make(ir);

    // create a mapping between statements and integers, used in bit vectors
    // shortly
    OrdinalSetMapping<Statement> domain = createStatementDomain(statements);

    // map SSAInstruction indices to statements
    Map<Integer, NormalStatement> ssaInstructionIndex2Statement = mapInstructionsToStatements(domain);

    // solve reaching definitions as a dataflow problem
    BitVectorFramework<IExplodedBasicBlock, Statement> rd = new BitVectorFramework<>(cfg, new RD(node,
        cfg, pa, domain, ssaInstructionIndex2Statement, exclusions), domain);
    if (VERBOSE) {
      System.err.println("Solve ");
    }
    BitVectorSolver<? extends ISSABasicBlock> solver = new BitVectorSolver<>(rd);
    try {
      solver.solve(null);
    } catch (CancelException e) {
      throw new CancelRuntimeException(e);
    }
    if (VERBOSE) {
      System.err.println("Solved. ");
    }
    return makeResult(solver, domain, node, heapModel, pa, mod, cfg,
        ssaInstructionIndex2Statement, exclusions, cg);
  }

  private class RDMap implements Map<Statement, OrdinalSet<Statement>> {
    final Map<Statement, OrdinalSet<Statement>> delegate = HashMapFactory.make();

    private final HeapExclusions exclusions;

    private final CallGraph cg;

    RDMap(BitVectorSolver<? extends ISSABasicBlock> solver, OrdinalSetMapping<Statement> domain, CGNode node, ExtendedHeapModel h,
        PointerAnalysis<T> pa, Map<CGNode, OrdinalSet<PointerKey>> mod, ExplodedControlFlowGraph cfg,
        Map<Integer, NormalStatement> ssaInstructionIndex2Statement, HeapExclusions exclusions, CallGraph cg) {
      if (VERBOSE) {
        System.err.println("Init pointer Key mod ");
      }
      this.exclusions = exclusions;
      this.cg = cg;
      Map<PointerKey, MutableIntSet> pointerKeyMod = initPointerKeyMod(domain, node, h, pa);
      if (VERBOSE) {
        System.err.println("Eager populate");
      }
      eagerPopulate(pointerKeyMod, solver, domain, node, h, pa, mod, cfg, ssaInstructionIndex2Statement);
      if (VERBOSE) {
        System.err.println("Done populate");
      }
    }

    private void eagerPopulate(Map<PointerKey, MutableIntSet> pointerKeyMod, BitVectorSolver<? extends ISSABasicBlock> solver,
        OrdinalSetMapping<Statement> domain, CGNode node, ExtendedHeapModel h, PointerAnalysis<T> pa,
        Map<CGNode, OrdinalSet<PointerKey>> mod, ExplodedControlFlowGraph cfg,
        Map<Integer, NormalStatement> ssaInstruction2Statement) {
      for (Statement s : domain) {
        delegate.put(s, computeResult(s, pointerKeyMod, solver, domain, node, h, pa, mod, cfg, ssaInstruction2Statement));
      }
    }

    /**
     * For each pointerKey, which statements may def it
     */
    private Map<PointerKey, MutableIntSet> initPointerKeyMod(OrdinalSetMapping<Statement> domain, CGNode node, ExtendedHeapModel h,
        PointerAnalysis<T> pa) {
      Map<PointerKey, MutableIntSet> pointerKeyMod = HashMapFactory.make();
      for (Statement s : domain) {
        switch (s.getKind()) {
        case HEAP_PARAM_CALLEE:
        case HEAP_RET_CALLER: {
          HeapStatement hs = (HeapStatement) s;
          MutableIntSet set = findOrCreateIntSet(pointerKeyMod, hs.getLocation());
          set.add(domain.getMappedIndex(s));
          break;
        }
        default: {
          Collection<PointerKey> m = getMod(s, node, h, pa, exclusions);
          for (PointerKey p : m) {
            MutableIntSet set = findOrCreateIntSet(pointerKeyMod, p);
            set.add(domain.getMappedIndex(s));
          }
          break;
        }
        }
      }
      return pointerKeyMod;
    }

    private MutableIntSet findOrCreateIntSet(Map<PointerKey, MutableIntSet> map, PointerKey key) {
      MutableIntSet result = map.get(key);
      if (result == null) {
        result = MutableSparseIntSet.makeEmpty();
        map.put(key, result);
      }
      return result;
    }

    @Override
    public String toString() {
      return delegate.toString();
    }

    @Override
    public void clear() {
      Assertions.UNREACHABLE();
      delegate.clear();
    }

    @Override
    public boolean containsKey(Object key) {
      Assertions.UNREACHABLE();
      return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      Assertions.UNREACHABLE();
      return delegate.containsValue(value);
    }

    @Override
    public Set<Entry<Statement, OrdinalSet<Statement>>> entrySet() {
      Assertions.UNREACHABLE();
      return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
      Assertions.UNREACHABLE();
      return delegate.equals(o);
    }

    @Override
    public OrdinalSet<Statement> get(Object key) {
      return delegate.get(key);
    }

    @Override
    public int hashCode() {
      Assertions.UNREACHABLE();
      return delegate.hashCode();
    }

    @Override
    public boolean isEmpty() {
      Assertions.UNREACHABLE();
      return delegate.isEmpty();
    }

    @Override
    public Set<Statement> keySet() {
      return delegate.keySet();
    }

    @Override
    public OrdinalSet<Statement> put(Statement key, OrdinalSet<Statement> value) {
      Assertions.UNREACHABLE();
      return delegate.put(key, value);
    }

    @Override
    public void putAll(Map<? extends Statement, ? extends OrdinalSet<Statement>> t) {
      Assertions.UNREACHABLE();
      delegate.putAll(t);
    }

    @Override
    public OrdinalSet<Statement> remove(Object key) {
      Assertions.UNREACHABLE();
      return delegate.remove(key);
    }

    @Override
    public int size() {
      Assertions.UNREACHABLE();
      return delegate.size();
    }

    @Override
    public Collection<OrdinalSet<Statement>> values() {
      Assertions.UNREACHABLE();
      return delegate.values();
    }

    /**
     * For a statement s, compute the set of statements that may def the heap value read by s.
     */
    OrdinalSet<Statement> computeResult(Statement s, Map<PointerKey, MutableIntSet> pointerKeyMod,
        BitVectorSolver<? extends ISSABasicBlock> solver, OrdinalSetMapping<Statement> domain, CGNode node, ExtendedHeapModel h,
        PointerAnalysis<T> pa, Map<CGNode, OrdinalSet<PointerKey>> mod, ExplodedControlFlowGraph cfg,
        Map<Integer, NormalStatement> ssaInstructionIndex2Statement) {
      switch (s.getKind()) {
      case NORMAL:
        NormalStatement n = (NormalStatement) s;
        Collection<PointerKey> ref = modRef.getRef(node, h, pa, n.getInstruction(), exclusions);
        if (!ref.isEmpty()) {
          ISSABasicBlock bb = cfg.getBlockForInstruction(n.getInstructionIndex());
          BitVectorVariable v = solver.getIn(bb);
          MutableSparseIntSet defs = MutableSparseIntSet.makeEmpty();
          if (v.getValue() != null) {
            for (PointerKey p : ref) {
              if (pointerKeyMod.get(p) != null) {
                defs.addAll(pointerKeyMod.get(p).intersection(v.getValue()));
              }
            }
          }
          return new OrdinalSet<>(defs, domain);
        } else {
          return OrdinalSet.empty();
        }
      case HEAP_RET_CALLEE: {
        HeapStatement.HeapReturnCallee r = (HeapStatement.HeapReturnCallee) s;
        PointerKey p = r.getLocation();
        BitVectorVariable v = solver.getIn(cfg.exit());
        if (DEBUG) {
          System.err.println("computeResult " + cfg.exit() + " " + s + " " + pointerKeyMod.get(p) + " " + v);
        }
        if (pointerKeyMod.get(p) == null) {
          return OrdinalSet.empty();
        }
        return new OrdinalSet<>(pointerKeyMod.get(p).intersection(v.getValue()), domain);
      }
      case HEAP_RET_CALLER: {
        HeapStatement.HeapReturnCaller r = (HeapStatement.HeapReturnCaller) s;
        ISSABasicBlock bb = cfg.getBlockForInstruction(r.getCallIndex());
        BitVectorVariable v = solver.getIn(bb);
        if (allCalleesMod(cg, r, mod) || pointerKeyMod.get(r.getLocation()) == null || v.getValue() == null) {
          // do nothing ... force flow into and out of the callees
          return OrdinalSet.empty();
        } else {
          // the defs that flow to the call may flow to this return, since
          // the callees may have no relevant effect.
          return new OrdinalSet<>(pointerKeyMod.get(r.getLocation()).intersection(v.getValue()), domain);
        }
      }
      case HEAP_PARAM_CALLER: {
        HeapStatement.HeapParamCaller r = (HeapStatement.HeapParamCaller) s;
        NormalStatement call = ssaInstructionIndex2Statement.get(r.getCallIndex());
        ISSABasicBlock callBlock = cfg.getBlockForInstruction(call.getInstructionIndex());
        if (callBlock.isEntryBlock()) {
          int x = domain.getMappedIndex(new HeapStatement.HeapParamCallee(node, r.getLocation()));
          assert x >= 0;
          IntSet xset = SparseIntSet.singleton(x);
          return new OrdinalSet<>(xset, domain);
        }
        BitVectorVariable v = solver.getIn(callBlock);
        if (pointerKeyMod.get(r.getLocation()) == null || v.getValue() == null) {
          // do nothing ... force flow into and out of the callees
          return OrdinalSet.empty();
        } else {
          return new OrdinalSet<>(pointerKeyMod.get(r.getLocation()).intersection(v.getValue()), domain);
        }
      }
      case NORMAL_RET_CALLEE:
      case NORMAL_RET_CALLER:
      case PARAM_CALLEE:
      case PARAM_CALLER:
      case EXC_RET_CALLEE:
      case EXC_RET_CALLER:
      case PHI:
      case PI:
      case CATCH:
      case METHOD_ENTRY:
      case METHOD_EXIT:
        return OrdinalSet.empty();
      case HEAP_PARAM_CALLEE:
        // no statements in this method will def the heap being passed in
        return OrdinalSet.empty();
      default:
        Assertions.UNREACHABLE(s.getKind().toString());
        return null;
      }
    }

  }

  /**
   * For each statement s, compute the set of statements that may def the heap value read by s.
   */
  private Map<Statement, OrdinalSet<Statement>> makeResult(BitVectorSolver<? extends ISSABasicBlock> solver,
      OrdinalSetMapping<Statement> domain, CGNode node, ExtendedHeapModel h, PointerAnalysis<T> pa,
      Map<CGNode, OrdinalSet<PointerKey>> mod, ExplodedControlFlowGraph cfg,
      Map<Integer, NormalStatement> ssaInstructionIndex2Statement, HeapExclusions exclusions, CallGraph cg) {

    return new RDMap(solver, domain, node, h, pa, mod, cfg, ssaInstructionIndex2Statement, exclusions, cg);
  }

  /**
   * Do all callees corresponding to the given call site def the pointer key being tracked by r?
   */
  private static boolean allCalleesMod(CallGraph cg, HeapReturnCaller r, Map<CGNode, OrdinalSet<PointerKey>> mod) {
    Collection<CGNode> targets = cg.getPossibleTargets(r.getNode(), r.getCall().getCallSite());
    if (targets.isEmpty()) {
      return false;
    }
    for (CGNode t : targets) {
      if (!mod.get(t).contains(r.getLocation())) {
        return false;
      }
    }
    return true;
  }

  private Collection<PointerKey> getMod(Statement s, CGNode n, ExtendedHeapModel h, PointerAnalysis<T> pa, HeapExclusions exclusions) {
    switch (s.getKind()) {
    case NORMAL:
      NormalStatement ns = (NormalStatement) s;
      return modRef.getMod(n, h, pa, ns.getInstruction(), exclusions);
    case HEAP_PARAM_CALLEE:
    case HEAP_RET_CALLER:
      HeapStatement hs = (HeapStatement) s;
      return Collections.singleton(hs.getLocation());
    case HEAP_RET_CALLEE:
    case HEAP_PARAM_CALLER:
    case EXC_RET_CALLEE:
    case EXC_RET_CALLER:
    case NORMAL_RET_CALLEE:
    case NORMAL_RET_CALLER:
    case PARAM_CALLEE:
    case PARAM_CALLER:
    case PHI:
    case PI:
    case METHOD_ENTRY:
    case METHOD_EXIT:
    case CATCH:
      // doesn't mod anything in the heap.
      return Collections.emptySet();
    default:
      Assertions.UNREACHABLE(s.getKind() + " " + s.toString());
      return null;
    }
  }

  /**
   * map each SSAInstruction index to the NormalStatement which represents it.
   */
  private static Map<Integer, NormalStatement> mapInstructionsToStatements(OrdinalSetMapping<Statement> domain) {
    Map<Integer, NormalStatement> result = HashMapFactory.make();
    for (Statement s : domain) {
      if (s.getKind().equals(Kind.NORMAL)) {
        NormalStatement n = (NormalStatement) s;
        result.put(n.getInstructionIndex(), n);
      }
    }
    return result;
  }

  private static OrdinalSetMapping<Statement> createStatementDomain(Collection<Statement> statements) {
    Statement[] arr = new Statement[statements.size()];
    OrdinalSetMapping<Statement> domain = new ObjectArrayMapping<>(statements.toArray(arr));
    return domain;
  }

  /**
   * Reaching def flow functions
   */
  private class RD implements ITransferFunctionProvider<IExplodedBasicBlock, BitVectorVariable> {

    private final CGNode node;

    private final ExplodedControlFlowGraph cfg;

    private final OrdinalSetMapping<Statement> domain;

    private final PointerAnalysis<T> pa;

    private final Map<Integer, NormalStatement> ssaInstructionIndex2Statement;

    private final HeapExclusions exclusions;

    /**
     * if (i,j) \in heapReturnCaller, then statement j is a HeapStatement.ReturnCaller for statement i, a NormalStatement
     * representing an invoke
     */
    private final IBinaryNaturalRelation heapReturnCaller = new BasicNaturalRelation();

    public RD(CGNode node, ExplodedControlFlowGraph cfg, PointerAnalysis<T> pa2, OrdinalSetMapping<Statement> domain,
        Map<Integer, NormalStatement> ssaInstructionIndex2Statement, HeapExclusions exclusions) {
      this.node = node;
      this.cfg = cfg;
      this.domain = domain;
      this.pa = pa2;
      this.ssaInstructionIndex2Statement = ssaInstructionIndex2Statement;
      this.exclusions = exclusions;
      initHeapReturnCaller();
    }

    private void initHeapReturnCaller() {
      for (Statement s : domain) {
        if (s.getKind().equals(Kind.HEAP_RET_CALLER)) {
          if (DEBUG) {
            System.err.println("initHeapReturnCaller " + s);
          }
          HeapStatement.HeapReturnCaller r = (HeapReturnCaller) s;
          NormalStatement call = ssaInstructionIndex2Statement.get(r.getCallIndex());
          int i = domain.getMappedIndex(call);
          int j = domain.getMappedIndex(r);
          heapReturnCaller.add(i, j);
        }
      }
    }

    @Override
    public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(IExplodedBasicBlock src, IExplodedBasicBlock dst) {
      if (DEBUG) {
        System.err.println("getEdgeXfer: " + src + " " + dst + " " + src.isEntryBlock());
      }
      if (src.isEntryBlock()) {
        if (DEBUG) {
          System.err.println("heapEntry " + heapEntryStatements());
        }
        return new BitVectorUnionVector(new BitVectorIntSet(heapEntryStatements()).getBitVector());
      }
      if (src.getInstruction() != null && !(src.getInstruction() instanceof SSAAbstractInvokeInstruction)
          && !cfg.getNormalSuccessors(src).contains(dst)) {
        // if the edge only happens due to exceptional control flow, then no
        // heap locations
        // are def'ed or used
        if (DEBUG) {
          System.err.println("Identity");
        }
        return BitVectorIdentity.instance();
      } else {
        BitVector kill = kill(src);
        IntSet gen = gen(src);
        if (DEBUG) {
          System.err.println("gen: " + gen + " kill: " + kill);
        }
        if (kill == null) {
          if (gen == null) {
            return BitVectorIdentity.instance();
          } else {
            return new BitVectorUnionVector(new BitVectorIntSet(gen).getBitVector());
          }
        } else {
          if (gen == null) {
            return new BitVectorMinusVector(kill);
          } else {
            return new BitVectorKillGen(kill, new BitVectorIntSet(gen).getBitVector());
          }
        }
      }
    }

    @Override
    public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
      return BitVectorUnion.instance();
    }

    @Override
    public UnaryOperator<BitVectorVariable> getNodeTransferFunction(IExplodedBasicBlock node) {
      return null;
    }

    @Override
    public boolean hasEdgeTransferFunctions() {
      return true;
    }

    @Override
    public boolean hasNodeTransferFunctions() {
      return false;
    }

    /**
     * @return int set representing the heap def statements that are gen'ed by the basic block. null if none.
     */
    IntSet gen(IExplodedBasicBlock b) {

      SSAInstruction s = b.getInstruction();
      if (DEBUG) {
        System.err.println("gen " + b + " " + s);
      }
      if (s == null) {
        return null;
      } else {
        if (s instanceof SSAAbstractInvokeInstruction) {
          // it's a normal statement ... we better be able to find it in the
          // domain.
          Statement st = ssaInstructionIndex2Statement.get(b.getLastInstructionIndex());
          if (st == null) {
            System.err.println(ssaInstructionIndex2Statement);
            Assertions.UNREACHABLE("bang " + b + " " + b.getLastInstructionIndex() + " " + s);
          }
          int domainIndex = domain.getMappedIndex(st);
          assert (domainIndex != -1);
          if (DEBUG) {
            System.err.println("GEN FOR " + s + " " + heapReturnCaller.getRelated(domainIndex));
          }
          return heapReturnCaller.getRelated(domainIndex);
        } else {
          Collection<PointerKey> gen = modRef.getMod(node, heapModel, pa, s, exclusions);
          if (gen.isEmpty()) {
            return null;
          } else {
            NormalStatement n = ssaInstructionIndex2Statement.get(b.getLastInstructionIndex());
            return SparseIntSet.singleton(domain.getMappedIndex(n));
          }
        }
      }
    }

    /**
     * @return int set representing all HEAP_PARAM_CALLEE statements in the domain.
     */
    private IntSet heapEntryStatements() {
      BitVectorIntSet result = new BitVectorIntSet();
      for (Statement s : domain) {
        if (s.getKind().equals(Kind.HEAP_PARAM_CALLEE)) {
          result.add(domain.getMappedIndex(s));
        }
      }
      return result;
    }

    /**
     * @return int set representing the heap def statements that are killed by the basic block. null if none.
     */
    BitVector kill(IExplodedBasicBlock b) {
      SSAInstruction s = b.getInstruction();
      if (s == null) {
        return null;
      } else {
        Collection<PointerKey> mod = modRef.getMod(node, heapModel, pa, s, exclusions);
        if (mod.isEmpty()) {
          return null;
        } else {
          // only static fields are actually killed
          Predicate<PointerKey> staticFilter = StaticFieldKey.class::isInstance;
          final Collection<PointerKey> kill = Iterator2Collection
              .toSet(new FilterIterator<>(mod.iterator(), staticFilter));
          if (kill.isEmpty()) {
            return null;
          } else {
            Predicate<Statement> f = s1 -> {
              Collection m = getMod(s1, node, heapModel, pa, exclusions);
              for (PointerKey k : kill) {
                if (m.contains(k)) {
                  return true;
                }
              }
              return false;
            };
            BitVector result = new BitVector();
            for (Statement k : Iterator2Iterable.make(new FilterIterator<>(domain.iterator(), f))) {
              result.set(domain.getMappedIndex(k));
            }
            return result;
          }
        }
      }
    }
  }
}
