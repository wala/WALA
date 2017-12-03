/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.graph.traverse;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;

/**
 * Extends {@link DFSPathFinder} to discover all paths from a set of root nodes
 * to nodes passing some {@link Predicate}.
 * 
 * Note that this code performs work that is potentially exponential in the size
 * of the underlying graph, using exponential space. It most likely won't work
 * even for graphs of moderate size.
 */
public class DFSAllPathsFinder<T> extends DFSPathFinder<T> {

  private static final long serialVersionUID = 5413569289853649240L;

  public DFSAllPathsFinder(Graph<T> G, Iterator<T> nodes, Predicate<T> f) {
    super(G, nodes, f);
  }

  public DFSAllPathsFinder(Graph<T> G, T N, Predicate<T> f) throws IllegalArgumentException {
    super(G, N, f);
  }

  @Override
  protected Iterator<? extends T> getConnected(T n) {
    final List<T> cp = currentPath();
    return new FilterIterator<>(G.getSuccNodes(n), o -> ! cp.contains(o));
  }

  @Override
  protected Iterator<? extends T> getPendingChildren(T n) {
    Pair<List<T>,T> key = Pair.make(currentPath(), n);
    return pendingChildren.get(key);
  }

  @Override
  protected void setPendingChildren(T v, Iterator<? extends T> iterator) {
    Pair<List<T>,T> key = Pair.make(currentPath(), v);
    pendingChildren.put(key, iterator);
  }

  
}
