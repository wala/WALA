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

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.NodeManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Stream;

/** Simple implementation of a {@link NodeManager}. */
public class BasicNodeManager<T> implements NodeManager<T> {

  private final HashSet<T> nodes = HashSetFactory.make();

  @Override
  public Stream<T> stream() {
    return nodes.stream();
  }

  @Override
  public Iterator<T> iterator() {
    return nodes.iterator();
  }

  @Override
  public int getNumberOfNodes() {
    return nodes.size();
  }

  @Override
  public void addNode(T n) {
    nodes.add(n);
  }

  @Override
  public void removeNode(T n) {
    nodes.remove(n);
  }

  @Override
  public boolean containsNode(T N) {
    return nodes.contains(N);
  }
}
