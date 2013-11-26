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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;

public class WelshPowell<T> {

  public static class ColoredVertices<T> {
    private final boolean fullColoring;
    private final Map<T, Integer> colors;
    private final int numColors;
    
    public boolean isFullColoring() {
      return fullColoring;
    }

    public Map<T, Integer> getColors() {
      return colors;
    }

    public int getNumColors() {
      return numColors;
    }

    public ColoredVertices(boolean fullColoring, Map<T, Integer> colors, int numColors) {
      this.fullColoring = fullColoring;
      this.colors = colors;
      this.numColors = numColors;
    }

  }

  public static <T> Comparator<T> defaultComparator(final Graph<T> G) {
    return new Comparator<T>() {

      @Override
      public int compare(T o1, T o2) {
        int o1edges = G.getSuccNodeCount(o1) + G.getPredNodeCount(o1);
        int o2edges = G.getSuccNodeCount(o2) + G.getPredNodeCount(o2);
        if (o1edges != o2edges) {
          return o1edges - o2edges;
        } else {
          return o1.toString().compareTo(o2.toString());
        }
      }
    };
  }

  public ColoredVertices<T> color(final Graph<T> G) {
    return color(G, defaultComparator(G), Integer.MAX_VALUE);
  }
  
  public ColoredVertices<T> color(final Graph<T> G, int maxColors) {
    return color(G, defaultComparator(G), maxColors);
  }

  public ColoredVertices<T> color(final Graph<T> G, Comparator<T> order, int maxColors) {
    Map<T, Integer> colors = HashMapFactory.make();

    SortedSet<T> vertices = new TreeSet<T>(order);

    for (T n : G) {
      vertices.add(n);
    }

    int currentColor = 0;
    for (T n : vertices) {
      if (!colors.containsKey(n)) {
        colors.put(n, currentColor);

        for (T m : vertices) {
          if (!colors.containsKey(m)) {
            color_me: {
            for (Iterator<T> ps = G.getPredNodes(m); ps.hasNext();) {
              T p = ps.next();
              if (colors.containsKey(p) && colors.get(p) == currentColor) {
                break color_me;
              }
            }

            for (Iterator<T> ss = G.getSuccNodes(m); ss.hasNext();) {
              T s = ss.next();
              if (colors.containsKey(s) && colors.get(s) == currentColor) {
                break color_me;
              }
            }

            colors.put(m, currentColor);
          }
          }
        }
        if (currentColor == maxColors - 1) {
          return new ColoredVertices<T>(false, colors, currentColor);
        }
        currentColor++;
      }
    }

    assert colors.size() == G.getNumberOfNodes();
    return new ColoredVertices<T>(true, colors, currentColor);
  }

}
