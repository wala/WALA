/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKeyWithFilter;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;

/** Default implementation of {@link PointerKeyFactory} */
public class DefaultPointerKeyFactory implements PointerKeyFactory {

  public DefaultPointerKeyFactory() {}

  @Override
  public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    if (valueNumber <= 0) {
      throw new IllegalArgumentException("illegal value number: " + valueNumber + " in " + node);
    }
    return new LocalPointerKey(node, valueNumber);
  }

  @Override
  public FilteredPointerKey getFilteredPointerKeyForLocal(
      CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter) {
    if (filter == null) {
      throw new IllegalArgumentException("null filter");
    }
    assert valueNumber > 0 : "illegal value number: " + valueNumber + " in " + node;
    // TODO: add type filters!
    return new LocalPointerKeyWithFilter(node, valueNumber, filter);
  }

  @Override
  public PointerKey getPointerKeyForReturnValue(CGNode node) {
    return new ReturnValueKey(node);
  }

  @Override
  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return new ExceptionReturnValueKey(node);
  }

  @Override
  public PointerKey getPointerKeyForStaticField(IField f) {
    if (f == null) {
      throw new IllegalArgumentException("null f");
    }
    return new StaticFieldKey(f);
  }

  @Override
  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
    if (field == null) {
      throw new IllegalArgumentException("field is null");
    }

    IField resolveAgain =
        I.getConcreteType().getField(field.getName(), field.getFieldTypeReference().getName());
    if (resolveAgain != null) {
      field = resolveAgain;
    }

    return new InstanceFieldKey(I, field);
  }

  @Override
  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    return new ArrayContentsKey(I);
  }
}
