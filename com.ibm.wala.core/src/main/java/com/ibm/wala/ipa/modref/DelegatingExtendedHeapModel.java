/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.modref;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import java.util.Iterator;

/** An implementation of {@link ExtendedHeapModel} based on a normal {@link HeapModel} */
public class DelegatingExtendedHeapModel implements ExtendedHeapModel {

  private final HeapModel h;

  public DelegatingExtendedHeapModel(HeapModel h) {
    if (h == null) {
      throw new IllegalArgumentException("null h");
    }
    this.h = h;
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return h.getClassHierarchy();
  }

  @Override
  public FilteredPointerKey getFilteredPointerKeyForLocal(
      CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter) {
    return h.getFilteredPointerKeyForLocal(node, valueNumber, filter);
  }

  @Override
  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    return h.getInstanceKeyForAllocation(node, allocation);
  }

  @Override
  public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
    return h.getInstanceKeyForMetadataObject(obj, objType);
  }

  @Override
  public <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S) {
    return h.getInstanceKeyForConstant(type, S);
  }

  @Override
  public InstanceKey getInstanceKeyForMultiNewArray(
      CGNode node, NewSiteReference allocation, int dim) {
    return h.getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  @Override
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
    if (node == null) {
      throw new IllegalArgumentException("null node");
    }
    return h.getInstanceKeyForPEI(node, instr, type);
  }

  @Override
  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    if (I == null) {
      throw new IllegalArgumentException("I is null");
    }
    return h.getPointerKeyForArrayContents(I);
  }

  @Override
  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return h.getPointerKeyForExceptionalReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
    if (field == null) {
      throw new IllegalArgumentException("field is null");
    }
    return h.getPointerKeyForInstanceField(I, field);
  }

  @Override
  public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    return h.getPointerKeyForLocal(node, valueNumber);
  }

  @Override
  public PointerKey getPointerKeyForReturnValue(CGNode node) {
    return h.getPointerKeyForReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForStaticField(IField f) {
    return h.getPointerKeyForStaticField(f);
  }

  @Override
  public Iterator<PointerKey> iteratePointerKeys() {
    return h.iteratePointerKeys();
  }

  @Override
  public PointerKey getPointerKeyForArrayLength(InstanceKey I) {
    return new ArrayLengthKey(I);
  }
}
