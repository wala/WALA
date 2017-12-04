/*******************************************************************************
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.demandpa.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.CISlicer;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;

/**
 * A {@link MemoryAccessMap} that makes use of a pre-computed
 * {@link PointerAnalysis} to reduce the number of considered accesses.
 * 
 * @author manu
 * 
 */
public class PABasedMemoryAccessMap implements MemoryAccessMap {

  private static final boolean DEBUG = false;

  private final PointerAnalysis<InstanceKey> pa;

  private final HeapModel heapModel;

  private final Map<PointerKey, Set<Statement>> invMod;

  private final Map<PointerKey, Set<Statement>> invRef;

  public PABasedMemoryAccessMap(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
    this(pa, new SDG<>(cg, pa, DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, ControlDependenceOptions.NONE));
  }

  public PABasedMemoryAccessMap(PointerAnalysis<InstanceKey> pa, SDG<InstanceKey> sdg) {
    this(pa, CISlicer.scanForMod(sdg, pa, true, ModRef.make()), CISlicer.scanForRef(sdg, pa));
  }
  
  public PABasedMemoryAccessMap(PointerAnalysis<InstanceKey> pa, Map<Statement, Set<PointerKey>> mod, Map<Statement, Set<PointerKey>> ref) {
    if (pa == null) {
      throw new IllegalArgumentException("null pa");
    }
    this.pa = pa;
    this.heapModel = pa.getHeapModel();
    invMod = MapUtil.inverseMap(mod);
    invRef = MapUtil.inverseMap(ref);
  }

  @Override
  public Collection<MemoryAccess> getArrayReads(PointerKey arrayRef) {
    Collection<MemoryAccess> memAccesses = new ArrayList<>();
    if (DEBUG) {
      System.err.println(("looking at reads of array ref " + arrayRef));
    }
    for (InstanceKey ik : pa.getPointsToSet(arrayRef)) {
      PointerKey ack = heapModel.getPointerKeyForArrayContents(ik);
      convertStmtsToMemoryAccess(invRef.get(ack), memAccesses);
    }
    return memAccesses;
  }

  @Override
  public Collection<MemoryAccess> getArrayWrites(PointerKey arrayRef) {
    Collection<MemoryAccess> memAccesses = new ArrayList<>();
    if (DEBUG) {
      System.err.println(("looking at writes to array ref " + arrayRef));
    }
    for (InstanceKey ik : pa.getPointsToSet(arrayRef)) {
      if (DEBUG) {
        System.err.println(("instance key " + ik + " class " + ik.getClass()));
      }
      PointerKey ack = heapModel.getPointerKeyForArrayContents(ik);
      convertStmtsToMemoryAccess(invMod.get(ack), memAccesses);
    }
    return memAccesses;
  }

  @Override
  public Collection<MemoryAccess> getFieldReads(PointerKey baseRef, IField field) {
    Collection<MemoryAccess> memAccesses = new ArrayList<>();
    for (InstanceKey ik : pa.getPointsToSet(baseRef)) {
      PointerKey ifk = heapModel.getPointerKeyForInstanceField(ik, field);
      convertStmtsToMemoryAccess(invRef.get(ifk), memAccesses);
    }
    return memAccesses;
  }

  @Override
  public Collection<MemoryAccess> getFieldWrites(PointerKey baseRef, IField field) {
    Collection<MemoryAccess> memAccesses = new ArrayList<>();
    for (InstanceKey ik : pa.getPointsToSet(baseRef)) {
      PointerKey ifk = heapModel.getPointerKeyForInstanceField(ik, field);
      convertStmtsToMemoryAccess(invMod.get(ifk), memAccesses);
    }
    return memAccesses;
  }

  @Override
  public Collection<MemoryAccess> getStaticFieldReads(IField field) {
    Collection<MemoryAccess> result = new ArrayList<>();
    convertStmtsToMemoryAccess(invRef.get(heapModel.getPointerKeyForStaticField(field)), result);
    return result;
  }

  @Override
  public Collection<MemoryAccess> getStaticFieldWrites(IField field) {
    Collection<MemoryAccess> result = new ArrayList<>();
    convertStmtsToMemoryAccess(invMod.get(heapModel.getPointerKeyForStaticField(field)), result);
    return result;
  }

  private static void convertStmtsToMemoryAccess(Collection<Statement> stmts, Collection<MemoryAccess> result) {
    if (stmts == null) {
      return;
    }
    if (DEBUG) {
      System.err.println(("statements: " + stmts));
    }
    for (Statement s : stmts) {
      switch (s.getKind()) {
      case NORMAL:
        NormalStatement normStmt = (NormalStatement) s;
        result.add(new MemoryAccess(normStmt.getInstructionIndex(), normStmt.getNode()));
        break;
      default:
        Assertions.UNREACHABLE();
      }
    }
  }

  @Override
  public HeapModel getHeapModel() {
    return heapModel;
  }

}
