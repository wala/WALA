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

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * A graph view that reverses the edges in a graph
 */
public class GraphInverter {

  public static <T> NumberedGraph<T> invert(final NumberedGraph<T> G) {
    return new InvertedNumberedGraph<>(G);
  }
  
      /**
   * @param G
   * @return A graph view that reverses the edges in G
   */
  public static <T> Graph<T> invert(final Graph<T> G) {
    if (G instanceof NumberedGraph) {
      return new InvertedNumberedGraph<>((NumberedGraph<T>) G);
    } else {
      return new InvertedGraph<>(G);
    }
  }

}
