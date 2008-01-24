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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ArrayLoadInstruction;
import com.ibm.wala.shrikeBT.ArrayStoreInstruction;
import com.ibm.wala.shrikeBT.GetInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.PutInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.shrike.ShrikeUtil;

/**
 * @author sfink
 * 
 */
public class MemoryAccessMap {

  /**
   * Map: IField -> Set<MemoryAccess>
   */
  final private Map<IField, Set<MemoryAccess>> readMap = HashMapFactory.make();

  /**
   * Map: IField -> Set<MemoryAccess>
   */
  final private Map<IField, Set<MemoryAccess>> writeMap = HashMapFactory.make();

  // TODO allow for more precise handling of arrays

  final private Set<MemoryAccess> arrayReads = HashSetFactory.make();

  final private Set<MemoryAccess> arrayWrites = HashSetFactory.make();

  private final IClassHierarchy cha;

  private final boolean includePrimOps;

  public MemoryAccessMap(CallGraph cg, boolean includePrimOps) {
    this.cha = cg.getClassHierarchy();
    this.includePrimOps = includePrimOps;
    populate(cg);
  }

  private void populate(CallGraph cg) {
    for (Iterator<CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode n = it.next();
      populate(n);
    }
  }

  private void populate(CGNode n) {
    if (n.getMethod().isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) n.getMethod();
      SSAInstruction[] statements = sm.getStatements();
      SSAMemoryAccessVisitor v = new SSAMemoryAccessVisitor(n);
      for (int i = 0; i < statements.length; i++) {
        SSAInstruction s = statements[i];
        if (s != null) {
          v.setInstructionIndex(i);
          s.visit(v);
        }
      }

    } else {
      ShrikeCTMethod sm = (ShrikeCTMethod) n.getMethod();
      MemoryAccessVisitor v = new MemoryAccessVisitor(n.getMethod().getReference().getDeclaringClass().getClassLoader(), n);
      try {
        Instruction[] statements = sm.getInstructions();
        if (statements == null) {
          // System.err.println("no statements for " + n.getMethod());
          return;
        }
        for (int i = 0; i < statements.length; i++) {
          Instruction s = statements[i];
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

  private class MemoryAccessVisitor extends Instruction.Visitor {
    int instructionIndex;

    final ClassLoaderReference loader;

    final CGNode node;

    public MemoryAccessVisitor(ClassLoaderReference loader, CGNode node) {
      super();
      this.loader = loader;
      this.node = node;
    }

    protected int getInstructionIndex() {
      return instructionIndex;
    }

    protected void setInstructionIndex(int instructionIndex) {
      this.instructionIndex = instructionIndex;
    }

    @Override
    public void visitNew(NewInstruction instruction) {
      TypeReference tr = ShrikeUtil.makeTypeReference(loader, instruction.getType());
      // chekc for multi-dimensional array allocation
      if (tr.isArrayType() && tr.getArrayElementType().isArrayType()) {
        arrayWrites.add(new MemoryAccess(instructionIndex, node));
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.shrikeBT.Instruction.Visitor#visitArrayLoad(com.ibm.shrikeBT.ArrayLoadInstruction)
     */
    @Override
    public void visitArrayLoad(ArrayLoadInstruction instruction) {
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
    public void visitArrayStore(ArrayStoreInstruction instruction) {
      if (!includePrimOps) {
        TypeReference tr = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        if (tr.isPrimitiveType()) {
          return;
        }
      }
      arrayWrites.add(new MemoryAccess(instructionIndex, node));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.shrikeBT.Instruction.Visitor#visitGet(com.ibm.shrikeBT.GetInstruction)
     */
    @Override
    public void visitGet(GetInstruction instruction) {
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
    public void visitPut(PutInstruction instruction) {
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

  /**
   * @return Collection<FieldAccess>
   */
  public Collection<MemoryAccess> getFieldReads(IField field) {
    Collection<MemoryAccess> result = readMap.get(field);
    if (result == null) {
      return Collections.emptySet();
    } else {
      return result;
    }
  }

  /**
   * @return Collection<FieldAccess>
   */
  public Collection<MemoryAccess> getFieldWrites(IField field) {
    Collection<MemoryAccess> result = writeMap.get(field);
    if (result == null) {
      return Collections.emptySet();
    } else {
      return result;
    }
  }

  public Collection<MemoryAccess> getArrayReads() {
    return arrayReads;
  }

  public Collection<MemoryAccess> getArrayWrites() {
    return arrayWrites;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();

    Collection<IField> allFields = new HashSet<IField>();
    allFields.addAll(readMap.keySet());
    allFields.addAll(writeMap.keySet());

    for (Iterator<IField> it = allFields.iterator(); it.hasNext();) {
      IField f = it.next();
      result.append("FIELD ").append(f).append(":\n");
      Collection<MemoryAccess> reads = getFieldReads(f);
      if (!reads.isEmpty()) {
        result.append("  reads:\n");
        for (Iterator<MemoryAccess> it2 = reads.iterator(); it2.hasNext();) {
          result.append("  ").append(it2.next()).append("\n");
        }
      }
      Collection<MemoryAccess> writes = getFieldWrites(f);
      if (!writes.isEmpty()) {
        result.append("  writes:\n");
        for (Iterator<MemoryAccess> it2 = writes.iterator(); it2.hasNext();) {
          result.append("  ").append(it2.next()).append("\n");
        }
      }
    }

    // arrays
    result.append("ARRAY CONTENTS:\n");
    if (!arrayReads.isEmpty()) {
      result.append("  reads:\n");
      for (Iterator<MemoryAccess> it2 = arrayReads.iterator(); it2.hasNext();) {
        result.append("  ").append(it2.next()).append("\n");
      }
    }
    if (!arrayWrites.isEmpty()) {
      result.append("  writes:\n");
      for (Iterator<MemoryAccess> it2 = arrayWrites.iterator(); it2.hasNext();) {
        result.append("  ").append(it2.next()).append("\n");
      }
    }
    return result.toString();
  }
}