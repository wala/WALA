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
package com.ibm.wala.util.graph.traverse;

import com.ibm.wala.util.graph.Graph;
import java.util.Iterator;

abstract class GraphDFSDiscoverTimeIterator<T> extends DFSDiscoverTimeIterator<T> {

  private static final long serialVersionUID = -5673397879499010863L;
  /** the graph being searched */
  private Graph<T> G;

  protected void init(Graph<T> G, Iterator<? extends T> nodes) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    this.G = G;
    super.init(nodes);
  }

  @Override
  protected Iterator<? extends T> getConnected(T n) {
    return G.getSuccNodes(n);
  }
}
