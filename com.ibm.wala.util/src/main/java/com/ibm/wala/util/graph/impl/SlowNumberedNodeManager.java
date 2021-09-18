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
package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableMapping;
import java.io.Serializable;
import java.util.Iterator;
import java.util.stream.Stream;

/** An object which manages node numbers via a mapping. */
public class SlowNumberedNodeManager<T> implements NumberedNodeManager<T>, Serializable {

  private static final long serialVersionUID = 8956107128389624337L;
  /** A bijection between integer &lt;-&gt; node */
  private final MutableMapping<T> map = MutableMapping.make();

  @Override
  public int getNumber(T obj) {
    return map.getMappedIndex(obj);
  }

  @Override
  public T getNode(int number) {
    if (number < 0) {
      throw new IllegalArgumentException("number must be >= 0");
    }
    T result = map.getMappedObject(number);
    return result;
  }

  @Override
  public int getMaxNumber() {
    return map.getMaximumIndex();
  }

  @Override
  public Iterator<T> iterator() {
    return map.iterator();
  }

  @Override
  public Stream<T> stream() {
    return map.stream();
  }

  @Override
  public int getNumberOfNodes() {
    return map.getSize();
  }

  @Override
  public void addNode(T n) {
    if (n == null) {
      throw new IllegalArgumentException("n is null");
    }
    map.add(n);
  }

  /** @see com.ibm.wala.util.graph.NodeManager#removeNode(Object) */
  @Override
  public void removeNode(T n) {
    map.deleteMappedObject(n);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("Nodes:\n");
    for (int i = 0; i <= getMaxNumber(); i++) {
      result.append(i).append("  ");
      result.append(map.getMappedObject(i));
      result.append('\n');
    }
    return result.toString();
  }

  /** @see com.ibm.wala.util.graph.NodeManager#containsNode(Object) */
  @Override
  public boolean containsNode(T N) {
    return getNumber(N) != -1;
  }

  @Override
  public Iterator<T> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<>(s, this);
  }
}
