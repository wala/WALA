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
package com.ibm.wala.util.graph;

import java.util.Collection;
import java.util.HashSet;

import com.ibm.wala.util.collections.HashSetFactory;

/**
 *  TODO: Move this somewhere.
 */
public class InferGraphRoots {

  public static <T> Collection<T> inferRoots(Graph<T> g){
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    HashSet<T> s = HashSetFactory.make();
    for (T node : g) {
      if (g.getPredNodeCount(node) == 0) {
        s.add(node);
      }
    }
    return s;
  }
}
