/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.demandpa.alg;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.TypeFilter;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import java.util.Iterator;

/**
 * a {@link HeapModel} that delegates to another except for pointer keys representing {@code this}
 * parameters of methods, for which it returns a {@link FilteredPointerKey} for the type of the
 * parameter
 *
 * @see DemandRefinementPointsTo
 * @author manu
 */
class ThisFilteringHeapModel implements HeapModel {

  private final HeapModel delegate;

  private final IClassHierarchy cha;

  @Override
  public IClassHierarchy getClassHierarchy() {
    return delegate.getClassHierarchy();
  }

  @Override
  public FilteredPointerKey getFilteredPointerKeyForLocal(
      CGNode node, int valueNumber, TypeFilter filter) {
    return delegate.getFilteredPointerKeyForLocal(node, valueNumber, filter);
  }

  @Override
  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    return delegate.getInstanceKeyForAllocation(node, allocation);
  }

  @Override
  public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
    return delegate.getInstanceKeyForMetadataObject(obj, objType);
  }

  @Override
  public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
    return delegate.getInstanceKeyForConstant(type, S);
  }

  @Override
  public InstanceKey getInstanceKeyForMultiNewArray(
      CGNode node, NewSiteReference allocation, int dim) {
    return delegate.getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  @Override
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
    return delegate.getInstanceKeyForPEI(node, instr, type);
  }

  @Override
  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    return delegate.getPointerKeyForArrayContents(I);
  }

  @Override
  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return delegate.getPointerKeyForExceptionalReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
    return delegate.getPointerKeyForInstanceField(I, field);
  }

  @Override
  public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    if (!node.getMethod().isStatic() && valueNumber == 1) {
      return delegate.getFilteredPointerKeyForLocal(node, valueNumber, getFilter(node));
    } else {
      return delegate.getPointerKeyForLocal(node, valueNumber);
    }
  }

  private FilteredPointerKey.TypeFilter getFilter(CGNode target) {
    FilteredPointerKey.TypeFilter filter =
        (FilteredPointerKey.TypeFilter) target.getContext().get(ContextKey.PARAMETERS[0]);

    if (filter != null) {
      return filter;
    } else {
      // the context does not select a particular concrete type for the
      // receiver.
      IClass C = getReceiverClass(target.getMethod());
      return new FilteredPointerKey.SingleClassFilter(C);
    }
  }

  /** @return the receiver class for this method. */
  private IClass getReceiverClass(IMethod method) {
    TypeReference formalType = method.getParameterType(0);
    IClass C = cha.lookupClass(formalType);
    if (method.isStatic()) {
      Assertions.UNREACHABLE("asked for receiver of static method " + method);
    }
    if (C == null) {
      Assertions.UNREACHABLE("no class found for " + formalType + " recv of " + method);
    }
    return C;
  }

  @Override
  public PointerKey getPointerKeyForReturnValue(CGNode node) {
    return delegate.getPointerKeyForReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForStaticField(IField f) {
    return delegate.getPointerKeyForStaticField(f);
  }

  @Override
  public Iterator<PointerKey> iteratePointerKeys() {
    return delegate.iteratePointerKeys();
  }

  public ThisFilteringHeapModel(HeapModel delegate, IClassHierarchy cha) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate null");
    }
    this.delegate = delegate;
    this.cha = cha;
  }
}
