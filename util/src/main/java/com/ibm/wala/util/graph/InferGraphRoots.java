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

import static com.google.common.base.Preconditions.checkArgument;

import com.ibm.wala.util.collections.HashSetFactory;
import java.util.HashSet;
import java.util.Set;

/** TODO: Move this somewhere. */
public class InferGraphRoots {

  public static <T> Set<T> inferRoots(Graph<T> g) {
    checkArgument(g != null, "g is null");
    HashSet<T> s = HashSetFactory.make();
    for (T node : g) {
      if (g.getPredNodeCount(node) == 0) {
        s.add(node);
      }
    }
    return s;
  }
}
