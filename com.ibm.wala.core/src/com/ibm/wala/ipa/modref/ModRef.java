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
package com.ibm.wala.ipa.modref;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Mod-ref analysis for heap locations.
 * 
 * For each call graph node, what heap locations (as determined by a heap model)
 * may it read or write, including it's callees transitively
 * 
 * @author sjfink
 * 
 */
public class ModRef {

  /**
   * For each call graph node, what heap locations (as determined by a heap
   * model) may it write, including its callees transitively
   * @throws IllegalArgumentException  if cg is null
   * 
   */
  public Map<CGNode, OrdinalSet<PointerKey>> computeMod(CallGraph cg, PointerAnalysis pa, HeapExclusions heapExclude) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    Map<CGNode, Collection<PointerKey>> scan = scanForMod(cg, pa, heapExclude);
    return transitiveClosure(cg, scan);
  }

  /**
   * For each call graph node, what heap locations (as determined by a heap
   * model) may it read, including its callees transitively
   * @throws IllegalArgumentException  if cg is null
   * 
   */
  public Map<CGNode, OrdinalSet<PointerKey>> computeRef(CallGraph cg, PointerAnalysis pa, HeapExclusions heapExclude) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    Map<CGNode, Collection<PointerKey>> scan = scanForRef(cg, pa, heapExclude);
    return transitiveClosure(cg, scan);
  }

  /**
   * For each call graph node, what heap locations (as determined by a heap
   * model) may it write, including its callees transitively
   * 
   */
  public Map<CGNode, OrdinalSet<PointerKey>> computeMod(CallGraph cg, PointerAnalysis pa) {
    return computeMod(cg, pa, null);
  }

  /**
   * For each call graph node, what heap locations (as determined by a heap
   * model) may it read, including its callees transitively
   * 
   */
  public Map<CGNode, OrdinalSet<PointerKey>> computeRef(CallGraph cg, PointerAnalysis pa) {
    return computeRef(cg, pa, null);
  }

  private Map<CGNode, OrdinalSet<PointerKey>> transitiveClosure(CallGraph cg, Map<CGNode, Collection<PointerKey>> scan) {
    GenReach<CGNode, PointerKey> gr = new GenReach<CGNode, PointerKey>(GraphInverter.invert(cg), scan);
    BitVectorSolver<CGNode> solver = new BitVectorSolver<CGNode>(gr);
    solver.solve();
    Map<CGNode, OrdinalSet<PointerKey>> result = HashMapFactory.make();
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode n = it.next();
      BitVectorVariable bv = (BitVectorVariable) solver.getOut(n);
      result.put(n, new OrdinalSet<PointerKey>(bv.getValue(), gr.getLatticeValues()));
    }
    return result;
  }

  /**
   * For each call graph node, what heap locations (as determined by a heap
   * model) may it write, <bf> NOT </bf> including its callees transitively
   * 
   * @param heapExclude
   */
  private Map<CGNode, Collection<PointerKey>> scanForMod(CallGraph cg, PointerAnalysis pa, HeapExclusions heapExclude) {
    Map<CGNode, Collection<PointerKey>> result = HashMapFactory.make();
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode n = it.next();
      result.put(n, scanNodeForMod(n, pa, heapExclude));
    }
    return result;
  }

  /**
   * For each call graph node, what heap locations (as determined by a heap
   * model) may it read, <bf> NOT </bf> including its callees transitively
   * 
   * @param heapExclude
   */
  private Map<CGNode, Collection<PointerKey>> scanForRef(CallGraph cg, PointerAnalysis pa, HeapExclusions heapExclude) {
    Map<CGNode, Collection<PointerKey>> result = HashMapFactory.make();
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode n = it.next();
      result.put(n, scanNodeForRef(n, pa, heapExclude));
    }
    return result;
  }

  /**
   * For a call graph node, what heap locations (as determined by a heap model)
   * may it write, <bf> NOT </bf> including it's callees transitively
   * 
   * @param heapExclude
   */
  private Collection<PointerKey> scanNodeForMod(final CGNode n, final PointerAnalysis pa, HeapExclusions heapExclude) {
    Collection<PointerKey> result = HashSetFactory.make();
    final ExtendedHeapModel h = new DelegatingExtendedHeapModel(pa.getHeapModel());
    SSAInstruction.Visitor v = makeModVisitor(n, result, pa, h);
    IR ir = n.getIR();
    if (ir != null) {
      for (Iterator<SSAInstruction> it = ir.iterateNormalInstructions(); it.hasNext();) {
        it.next().visit(v);
      }
    }
    if (heapExclude != null) {
      result = heapExclude.filter(result);
    }
    return result;
  }

  /**
   * For a call graph node, what heap locations (as determined by a heap model)
   * may it read, <bf> NOT </bf> including it's callees transitively
   */
  private Collection<PointerKey> scanNodeForRef(final CGNode n, final PointerAnalysis pa, HeapExclusions heapExclude) {
    Collection<PointerKey> result = HashSetFactory.make();
    final ExtendedHeapModel h = new DelegatingExtendedHeapModel(pa.getHeapModel());
    SSAInstruction.Visitor v = makeRefVisitor(n, result, pa, h);
    IR ir = n.getIR();
    if (ir != null) {
      for (Iterator<SSAInstruction> it = ir.iterateNormalInstructions(); it.hasNext();) {
        it.next().visit(v);
      }
    }
    if (heapExclude != null) {
      result = heapExclude.filter(result);
    }
    return result;
  }

  protected static class RefVisitor extends SSAInstruction.Visitor {
    private final CGNode n;

    private final Collection<PointerKey> result;

    private final PointerAnalysis pa;

    private final ExtendedHeapModel h;

    protected RefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
      this.n = n;
      this.result = result;
      this.pa = pa;
      this.h = h;
    }

    @Override
    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
      PointerKey ref = h.getPointerKeyForLocal(n, instruction.getArrayRef());
      for (InstanceKey i : pa.getPointsToSet(ref)) {
        result.add(h.getPointerKeyForArrayLength(i));
      }
    }

    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      PointerKey ref = h.getPointerKeyForLocal(n, instruction.getArrayRef());
      for (InstanceKey i : pa.getPointsToSet(ref)) {
        result.add(h.getPointerKeyForArrayContents(i));
      }
    }

    @Override
    public void visitGet(SSAGetInstruction instruction) {
      IField f = pa.getClassHierarchy().resolveField(instruction.getDeclaredField());
      if (f != null) {
        if (instruction.isStatic()) {
          result.add(h.getPointerKeyForStaticField(f));
        } else {
          PointerKey ref = h.getPointerKeyForLocal(n, instruction.getRef());
          for (InstanceKey i : pa.getPointsToSet(ref)) {
            result.add(h.getPointerKeyForInstanceField(i, f));
          }
        }
      }
    }
  }

  protected static class ModVisitor extends SSAInstruction.Visitor {
    private final CGNode n;

    private final Collection<PointerKey> result;

    private final ExtendedHeapModel h;

    private final PointerAnalysis pa;


    protected ModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h, PointerAnalysis pa) {
      this.n = n;
      this.result = result;
      this.h = h;
      this.pa = pa;
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
      if (instruction.getConcreteType().isArrayType()) {
        int dim = instruction.getConcreteType().getDimensionality();
        if (dim > 1) {
          for (int d = 0; d < dim - 1; d++) {
            InstanceKey i = h.getInstanceKeyForMultiNewArray(n, instruction.getNewSite(), d);
            PointerKey pk = h.getPointerKeyForArrayContents(i);
            if (pk == null) {
              h.getPointerKeyForArrayContents(i);
            }

            assert pk != null;
            result.add(pk);
            pk = h.getPointerKeyForArrayLength(i);
            assert pk != null;
            result.add(pk);
          }
        } else {
          // allocation of 1D arr "writes" the contents of the array and the
          // length field
          InstanceKey i = h.getInstanceKeyForAllocation(n, instruction.getNewSite());
          PointerKey pk = h.getPointerKeyForArrayContents(i);
          if (pk == null) {
            h.getPointerKeyForArrayContents(i);
          }
          assert pk != null;
          result.add(pk);
          pk = h.getPointerKeyForArrayLength(i);
          assert pk != null;
          result.add(pk);
        }

      } else {
        if (!PDG.IGNORE_ALLOC_HEAP_DEFS) {
          // allocation of a scalar "writes" all fields in the scalar
          InstanceKey i = h.getInstanceKeyForAllocation(n, instruction.getNewSite());
          if (i != null) {
            IClass type = i.getConcreteType();
            try {
              for (IField f : type.getAllInstanceFields()) {
                PointerKey pk = h.getPointerKeyForInstanceField(i, f);
                assert pk != null;
                result.add(pk);
              }
            } catch (ClassHierarchyException e) {
              Assertions.UNREACHABLE();
              e.printStackTrace();
            }
          }
        }
      }
    }

    @Override
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
      PointerKey ref = h.getPointerKeyForLocal(n, instruction.getArrayRef());
      for (InstanceKey i : pa.getPointsToSet(ref)) {
        result.add(h.getPointerKeyForArrayContents(i));
      }
    }

    @Override
    public void visitPut(SSAPutInstruction instruction) {
      IField f = pa.getClassHierarchy().resolveField(instruction.getDeclaredField());
      if (f != null) {
        if (instruction.isStatic()) {
          result.add(h.getPointerKeyForStaticField(f));
        } else {
          PointerKey ref = h.getPointerKeyForLocal(n, instruction.getRef());
          for (InstanceKey i : pa.getPointsToSet(ref)) {
            result.add(h.getPointerKeyForInstanceField(i, f));
          }
        }
      }
    }
  }

  protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
      return new ModVisitor(n, result, h, pa);
  }

  public Collection<PointerKey> getMod(CGNode n, ExtendedHeapModel h, PointerAnalysis pa, SSAInstruction s,
      HeapExclusions hexcl) {
    if (s == null) {
          throw new IllegalArgumentException("s is null");
        }
    Collection<PointerKey> result = HashSetFactory.make(2);
    ModVisitor v = makeModVisitor(n, result, pa, h);
    s.visit(v);
    return hexcl == null ? result : hexcl.filter(result);
  }

  protected RefVisitor makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis pa, ExtendedHeapModel h) {
    return new RefVisitor(n, result, pa, h);
  }

  public Collection<PointerKey> getRef(CGNode n, ExtendedHeapModel h, PointerAnalysis pa, SSAInstruction s,
      HeapExclusions hexcl) {
    if (s == null) {
          throw new IllegalArgumentException("s is null");
        }
    Collection<PointerKey> result = HashSetFactory.make(2);
    RefVisitor v = makeRefVisitor(n, result, pa, h);
    s.visit(v);
    return hexcl == null ? result : hexcl.filter(result);
  }

}
