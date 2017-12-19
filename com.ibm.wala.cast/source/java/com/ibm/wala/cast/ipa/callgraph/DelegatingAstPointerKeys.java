/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.strings.Atom;

public class DelegatingAstPointerKeys implements AstPointerKeyFactory {
  private final PointerKeyFactory base;

  public DelegatingAstPointerKeys(PointerKeyFactory base) {
    this.base = base;
  }

  @Override
  public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    return base.getPointerKeyForLocal(node, valueNumber);
  }

  @Override
  public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter) {
    return base.getFilteredPointerKeyForLocal(node, valueNumber, filter);
  }

  @Override
  public PointerKey getPointerKeyForReturnValue(CGNode node) {
    return base.getPointerKeyForReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return base.getPointerKeyForExceptionalReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForStaticField(IField f) {
    return base.getPointerKeyForStaticField(f);
  }

  @Override
  public PointerKey getPointerKeyForObjectCatalog(InstanceKey I) {
    return new ObjectPropertyCatalogKey(I);
  }

  @Override
  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField f) {
    return base.getPointerKeyForInstanceField(I, f);
  }

  @Override
  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    return base.getPointerKeyForArrayContents(I);
  }

  @Override
  public Iterator<PointerKey> getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F) {
    List<PointerKey> result = new LinkedList<>();

    if (F instanceof ConstantKey) {
      PointerKey ifk = getInstanceFieldPointerKeyForConstant(I, (ConstantKey<?>) F);
      if (ifk != null) {
        result.add(ifk);
      }
    }

    result.add(ReflectedFieldPointerKey.mapped(new ConcreteTypeKey(getFieldNameType(F)), I));

    return result.iterator();
  }

  /**
   * get type for F appropriate for use in a field name.
   * 
   * @param F
   */
  protected IClass getFieldNameType(InstanceKey F) {
    return F.getConcreteType();
  }

  /**
   * if F is a supported constant representing a field, return the corresponding {@link InstanceFieldKey} for I.  Otherwise, return <code>null</code>.
   * @param F
   */
  protected PointerKey getInstanceFieldPointerKeyForConstant(InstanceKey I, ConstantKey<?> F) {
    Object v = F.getValue();
    // FIXME: current only constant string are handled
    if (v instanceof String) {
      IField f = I.getConcreteType().getField(Atom.findOrCreateUnicodeAtom((String) v));
      return getPointerKeyForInstanceField(I, f);
    }
    return null;
  }
  
  @Override
  public Iterator<PointerKey> getPointerKeysForReflectedFieldRead(InstanceKey I, InstanceKey F) {
    if (F instanceof ConstantKey) {
      PointerKey ifk = getInstanceFieldPointerKeyForConstant(I, (ConstantKey<?>) F);
      if (ifk != null) {
        return new NonNullSingletonIterator<>(ifk);
      }
    }
    PointerKey x = ReflectedFieldPointerKey.mapped(new ConcreteTypeKey(getFieldNameType(F)), I);
    return new NonNullSingletonIterator<>(x);
  }
}
