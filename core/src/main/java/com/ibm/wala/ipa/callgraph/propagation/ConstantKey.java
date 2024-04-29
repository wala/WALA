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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Pair;
import java.util.Iterator;
import java.util.Objects;

/** An instance key which represents a unique, constant object. */
public final class ConstantKey<T> implements InstanceKey {
  private final T value;

  private final IClass valueClass;

  public ConstantKey(T value, IClass valueClass) {
    this.value = value;
    this.valueClass = valueClass;
    assert valueClass != null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ConstantKey) {
      ConstantKey<?> other = (ConstantKey<?>) obj;
      return valueClass.equals(other.valueClass) ? Objects.equals(value, other.value) : false;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return value == null ? 65535 : 1877 * value.hashCode();
  }

  @Override
  public String toString() {
    if (value == null) return "[ConstantKey:null]";
    else return "[ConstantKey:" + value + ':' + valueClass.getReference() + ']';
  }

  @Override
  public IClass getConcreteType() {
    return valueClass;
  }

  public T getValue() {
    return value;
  }

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    return EmptyIterator.instance();
  }
}
