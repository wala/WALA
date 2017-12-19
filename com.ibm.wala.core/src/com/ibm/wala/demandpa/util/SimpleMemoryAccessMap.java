/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.demandpa.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrikeBT.IGetInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IPutInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.shrike.ShrikeUtil;

/**
 * @author sfink
 * 
 */
public class SimpleMemoryAccessMap implements MemoryAccessMap {

  private static final boolean DEBUG = false;

  /**
   * if true, always use IR from CGNode when reasoning about method statements; otherwise, try to use bytecodes when possible. Note
   * that current code may not work if set to false.
   */
  private static final boolean ALWAYS_BUILD_IR = true;

  /**
   * Map: IField -&gt; Set&lt;MemoryAccess&gt;
   */
  final private Map<IField, Set<MemoryAccess>> readMap = HashMapFactory.make();

  /**
   * Map: IField -&gt; Set&lt;MemoryAccess&gt;
   */
  final private Map<IField, Set<MemoryAccess>> writeMap = HashMapFactory.make();

  final private Set<MemoryAccess> arrayReads = HashSetFactory.make();

  final private Set<MemoryAccess> arrayWrites = HashSetFactory.make();

  private final IClassHierarchy cha;

  private final boolean includePrimOps;

  private final HeapModel heapModel;

  public SimpleMemoryAccessMap(CallGraph cg, HeapModel heapModel, boolean includePrimOps) {
    if (cg == null) {
      throw new IllegalArgumentException("null cg");
    }
    this.cha = cg.getClassHierarchy();
    this.heapModel = heapModel;
    this.includePrimOps = includePrimOps;
    populate(cg);
  }

  private void populate(CallGraph cg) {
    for (CGNode n : cg) {
      populate(n);
    }
  }

  @SuppressWarnings("unused")
  private void populate(CGNode n) {
    // we analyze bytecodes to avoid the cost of IR construction, except
    // for synthetic methods, where we must use the synthetic IR
    if (ALWAYS_BUILD_IR || n.getMethod().isSynthetic()) {
      if (DEBUG) {
        System.err.println("synthetic method");
      }
      IR ir = n.getIR();
      if (ir == null) {
        return;
      }
      SSAInstruction[] statements = ir.getInstructions();
      SSAMemoryAccessVisitor v = new SSAMemoryAccessVisitor(n);
      for (int i = 0; i < statements.length; i++) {
        SSAInstruction s = statements[i];
        if (s != null) {
          v.setInstructionIndex(i);
          s.visit(v);
        }
      }

    } else {
      if (DEBUG) {
        System.err.println("Shrike method");
      }
      ShrikeCTMethod sm = (ShrikeCTMethod) n.getMethod();
      MemoryAccessVisitor v = new MemoryAccessVisitor(n.getMethod().getReference().getDeclaringClass().getClassLoader(), n);
      try {
        IInstruction[] statements = sm.getInstructions();
        if (statements == null) {
          // System.err.println("no statements for " + n.getMethod());
          return;
        }
        if (DEBUG) {
          for (int i = 0; i < statements.length; i++) {
            System.err.println(i + ": " + statements[i]);
          }
        }
        for (int i = 0; i < statements.length; i++) {
          IInstruction s = statements[i];
          if (s != null) {
            v.setInstructionIndex(i);
            s.visit(v);
          }
        }
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }

  }

  private class SSAMemoryAccessVisitor extends SSAInstruction.Visitor {

    private final CGNode node;

    private int instructionIndex;

    public SSAMemoryAccessVisitor(CGNode n) {
      this.node = n;
    }

    public void setInstructionIndex(int i) {
      this.instructionIndex = i;
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
      TypeReference declaredType = instruction.getNewSite().getDeclaredType();
      // check for multidimensional array
      if (declaredType.isArrayType() && declaredType.getArrayElementType().isArrayType()) {
        arrayWrites.add(new MemoryAccess(instructionIndex, node));
      }

    }

    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      if (!includePrimOps && instruction.typeIsPrimitive()) {
        return;
      }
      arrayReads.add(new MemoryAccess(instructionIndex, node));
    }

    @Override
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
      if (!includePrimOps && instruction.typeIsPrimitive()) {
        return;
      }
      arrayWrites.add(new MemoryAccess(instructionIndex, node));
    }

    @Override
    public void visitGet(SSAGetInstruction instruction) {
      if (!includePrimOps && instruction.getDeclaredFieldType().isPrimitiveType()) {
        return;
      }
      FieldReference fr = instruction.getDeclaredField();
      IField f = cha.resolveField(fr);
      if (f == null) {
        return;
      }
      Set<MemoryAccess> s = MapUtil.findOrCreateSet(readMap, f);
      MemoryAccess fa = new MemoryAccess(instructionIndex, node);
      s.add(fa);
    }

    @Override
    public void visitPut(SSAPutInstruction instruction) {
      if (!includePrimOps && instruction.getDeclaredFieldType().isPrimitiveType()) {
        return;
      }
      FieldReference fr = instruction.getDeclaredField();
      IField f = cha.resolveField(fr);
      if (f == null) {
        return;
      }
      Set<MemoryAccess> s = MapUtil.findOrCreateSet(writeMap, f);
      MemoryAccess fa = new MemoryAccess(instructionIndex, node);
      s.add(fa);
    }

  }

  private class MemoryAccessVisitor extends IInstruction.Visitor {
    int instructionIndex;

    final ClassLoaderReference loader;

    final CGNode node;

    public MemoryAccessVisitor(ClassLoaderReference loader, CGNode node) {
      super();
      this.loader = loader;
      this.node = node;
    }

    protected void setInstructionIndex(int instructionIndex) {
      this.instructionIndex = instructionIndex;
    }

    @Override
    public void visitNew(NewInstruction instruction) {
      TypeReference tr = ShrikeUtil.makeTypeReference(loader, instruction.getType());
      // chekc for multi-dimensional array allocation
      if (tr.isArrayType() && tr.getArrayElementType().isArrayType()) {
        if (DEBUG) {
          System.err.println("found multi-dim array write at " + instructionIndex);
        }
        arrayWrites.add(new MemoryAccess(instructionIndex, node));
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.shrikeBT.Instruction.Visitor#visitArrayLoad(com.ibm.shrikeBT.ArrayLoadInstruction)
     */
    @Override
    public void visitArrayLoad(IArrayLoadInstruction instruction) {
      if (!includePrimOps) {
        TypeReference tr = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        // ISSUE is this the right check?
        if (tr.isPrimitiveType()) {
          return;
        }

      }
      arrayReads.add(new MemoryAccess(instructionIndex, node));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.shrikeBT.Instruction.Visitor#visitArrayStore(com.ibm.shrikeBT.ArrayStoreInstruction)
     */
    @Override
    public void visitArrayStore(IArrayStoreInstruction instruction) {
      if (!includePrimOps) {
        TypeReference tr = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        if (tr.isPrimitiveType()) {
          return;
        }
      }

      if (DEBUG) {
        System.err.println("found array write at " + instructionIndex);
      }
      arrayWrites.add(new MemoryAccess(instructionIndex, node));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.shrikeBT.Instruction.Visitor#visitGet(com.ibm.shrikeBT.GetInstruction)
     */
    @Override
    public void visitGet(IGetInstruction instruction) {
      FieldReference fr = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(), instruction
          .getFieldType());
      if (!includePrimOps && fr.getFieldType().isPrimitiveType()) {
        return;
      }
      IField f = cha.resolveField(fr);
      if (f == null) {
        return;
      }
      Set<MemoryAccess> s = MapUtil.findOrCreateSet(readMap, f);
      MemoryAccess fa = new MemoryAccess(instructionIndex, node);
      s.add(fa);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.shrikeBT.Instruction.Visitor#visitPut(com.ibm.shrikeBT.PutInstruction)
     */
    @Override
    public void visitPut(IPutInstruction instruction) {
      FieldReference fr = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(), instruction
          .getFieldType());
      if (!includePrimOps && fr.getFieldType().isPrimitiveType()) {
        return;
      }
      IField f = cha.resolveField(fr);
      if (f == null) {
        return;
      }
      Set<MemoryAccess> s = MapUtil.findOrCreateSet(writeMap, f);
      MemoryAccess fa = new MemoryAccess(instructionIndex, node);
      s.add(fa);
    }

  }

  /*
   * @see com.ibm.wala.demandpa.util.MemoryAccessMap#getFieldReads(com.ibm.wala.classLoader.IField)
   */
  @Override
  public Collection<MemoryAccess> getFieldReads(PointerKey pk, IField field) {
    Collection<MemoryAccess> result = readMap.get(field);
    if (result == null) {
      return Collections.emptySet();
    } else {
      return result;
    }
  }

  /*
   * @see com.ibm.wala.demandpa.util.MemoryAccessMap#getFieldWrites(com.ibm.wala.classLoader.IField)
   */
  @Override
  public Collection<MemoryAccess> getFieldWrites(PointerKey pk, IField field) {
    Collection<MemoryAccess> result = writeMap.get(field);
    if (result == null) {
      return Collections.emptySet();
    } else {
      return result;
    }
  }

  /*
   * @see com.ibm.wala.demandpa.util.MemoryAccessMap#getArrayReads()
   */
  @Override
  public Collection<MemoryAccess> getArrayReads(PointerKey pk) {
    return arrayReads;
  }

  /*
   * @see com.ibm.wala.demandpa.util.MemoryAccessMap#getArrayWrites()
   */
  @Override
  public Collection<MemoryAccess> getArrayWrites(PointerKey pk) {
    return arrayWrites;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();

    Collection<IField> allFields = HashSetFactory.make();
    allFields.addAll(readMap.keySet());
    allFields.addAll(writeMap.keySet());

    for (IField f : allFields) {
      result.append("FIELD ").append(f).append(":\n");
      Collection<MemoryAccess> reads = getFieldReads(null, f);
      if (!reads.isEmpty()) {
        result.append("  reads:\n");
        for (MemoryAccess memoryAccess : reads) {
          result.append("  ").append(memoryAccess).append("\n");
        }
      }
      Collection<MemoryAccess> writes = getFieldWrites(null, f);
      if (!writes.isEmpty()) {
        result.append("  writes:\n");
        for (MemoryAccess memoryAccess : writes) {
          result.append("  ").append(memoryAccess).append("\n");
        }
      }
    }

    // arrays
    result.append("ARRAY CONTENTS:\n");
    if (!arrayReads.isEmpty()) {
      result.append("  reads:\n");
      for (MemoryAccess memoryAccess : arrayReads) {
        result.append("  ").append(memoryAccess).append("\n");
      }
    }
    if (!arrayWrites.isEmpty()) {
      result.append("  writes:\n");
      for (MemoryAccess memoryAccess : arrayWrites) {
        result.append("  ").append(memoryAccess).append("\n");
      }
    }
    return result.toString();
  }

  @Override
  public Collection<MemoryAccess> getStaticFieldReads(IField field) {
    return getFieldReads(null, field);
  }

  @Override
  public Collection<MemoryAccess> getStaticFieldWrites(IField field) {
    return getFieldWrites(null, field);
  }

  @Override
  public HeapModel getHeapModel() {
    // NOTE: this memory access map actually makes no use of the heap model
    return heapModel;
  }

  public void repOk() {
    for (MemoryAccess m : Iterator2Iterable.make(new CompoundIterator<>(arrayReads.iterator(), arrayWrites.iterator()))) {
      CGNode node = m.getNode();
      IR ir = node.getIR();
      assert ir != null : "null IR for " + node + " but we have a memory access";
      SSAInstruction[] instructions = ir.getInstructions();
      int instructionIndex = m.getInstructionIndex();
      assert instructionIndex >= 0 && instructionIndex < instructions.length : "instruction index " + instructionIndex
          + " out of range for " + node + ", which has " + instructions.length + " instructions";
      SSAInstruction s = instructions[m.getInstructionIndex()];
      if (s == null) {
        // this is possible due to dead bytecodes
        continue;
      }
      assert s instanceof SSAArrayReferenceInstruction || s instanceof SSANewInstruction : "bad type " + s.getClass()
          + " for array access instruction";
    }
  }
}
