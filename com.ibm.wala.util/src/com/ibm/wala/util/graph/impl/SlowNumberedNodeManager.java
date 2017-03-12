/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.graph.impl;

import java.io.Serializable;
import java.util.Iterator;

import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableMapping;

/**
 * An object which manages node numbers via a mapping.
 */
public class SlowNumberedNodeManager<T> implements NumberedNodeManager<T>, Serializable {

  /**
   * A bijection between integer &lt;-&gt; node
   */
  final private MutableMapping<T> map = MutableMapping.make();


  @Override
  public int getNumber(T obj) {
    return map.getMappedIndex(obj);
  }


  @Override
  public T getNode(int number)  {
    if (number < 0) {
      throw new IllegalArgumentException("number must be >= 0");
    }
    T result = map.getMappedObject(number);
    return result;
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedGraph#getMaxNumber()
   */
  @Override
  public int getMaxNumber() {
    return map.getMaximumIndex();
  }


  @Override
  public Iterator<T> iterator() {
    return map.iterator();
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

  /*
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNode(T n) {
    map.deleteMappedObject(n);
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("Nodes:\n");
    for (int i = 0; i <= getMaxNumber(); i++) {
      result.append(i).append("  ");
      result.append(map.getMappedObject(i));
      result.append("\n");
    }
    return result.toString();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public boolean containsNode(T N) {
    return getNumber(N) != -1;
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public Iterator<T> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<>(s, this);
  }

}
