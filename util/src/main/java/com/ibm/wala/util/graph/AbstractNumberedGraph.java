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
package com.ibm.wala.util.graph;

import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.intset.IntSet;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

/**
 * Basic functionality for a graph that delegates node and edge management, and tracks node numbers
 */
public abstract class AbstractNumberedGraph<T> extends AbstractGraph<T>
    implements NumberedGraph<T> {

  /**
   * @return the object which manages nodes in the graph
   */
  @Override
  protected abstract NumberedNodeManager<T> getNodeManager();

  /**
   * @return the object which manages edges in the graph
   */
  @Override
  protected abstract NumberedEdgeManager<T> getEdgeManager();

  @Override
  public int getMaxNumber() {
    return getNodeManager().getMaxNumber();
  }

  @Override
  public T getNode(int number) {
    return getNodeManager().getNode(number);
  }

  @Override
  public int getNumber(@Nullable T N) {
    if (N == null) {
      throw new IllegalArgumentException("N cannot be null");
    }
    return getNodeManager().getNumber(N);
  }

  @Override
  public Iterator<T> iterateNodes(final IntSet s) {
    return new NumberedNodeIterator<>(s, this);
  }

  @Override
  public IntSet getPredNodeNumbers(@Nullable T node) throws IllegalArgumentException {
    assert getEdgeManager() != null;
    return getEdgeManager().getPredNodeNumbers(node);
  }

  @Override
  public IntSet getSuccNodeNumbers(@Nullable T node) throws IllegalArgumentException {
    return getEdgeManager().getSuccNodeNumbers(node);
  }
}
