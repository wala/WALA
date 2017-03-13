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
package com.ibm.wala.util.graph;

import com.ibm.wala.util.collections.SimpleVector;

/**
 * A simple, extremely inefficient tree implementation
 */
public class BasicTree<T> {

  private final T value;
  final private SimpleVector<BasicTree<T>> children = new SimpleVector<>();

  protected BasicTree(T value) {
    this.value = value;
  }
  
  public static <T> BasicTree<T> make(T value) {
    if (value == null) {
      throw new IllegalArgumentException("null value");
    }
    return new BasicTree<>(value);
  }
  
  public T getRootValue() {
    return value;
  }

  public T getChildValue(int i) {
    if (children.get(i) == null) {
      return null;
    } else {
      return children.get(i).getRootValue();
    }
  }
  
  public BasicTree<T> getChild(int i) {
    return children.get(i);
  }

  public void setChild(int i, BasicTree<T> tree) {
    children.set(i, tree);
  }
  
  public int getMaxChildIndex() {
    return children.getMaxIndex();
  }
  
}
