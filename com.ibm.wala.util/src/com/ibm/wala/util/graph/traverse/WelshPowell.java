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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.NumberedGraph;

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

    public ColoredVertices(boolean fullColoring, NumberedGraph<T> G, int colors[], int numColors) {
      this(fullColoring, makeMap(G, colors), numColors);
    }

    private static <T> Map<T, Integer> makeMap(NumberedGraph<T> G, int[] colors) {
      Map<T,Integer> colorMap = HashMapFactory.make();
      for(int i = 0; i < colors.length; i++) {
        if (colors[i] != -1) {
          colorMap.put(G.getNode(i), colors[i]);
        }
      }
      return colorMap;
    }
    
    public ColoredVertices(boolean fullColoring, Map<T, Integer> colors, int numColors) {
      this.fullColoring = fullColoring;
      this.colors = colors;
      this.numColors = numColors;
    }

    @Override
    public String toString() {
      return colors.toString();
    }
  }

  public static <T> Comparator<T> defaultComparator(final NumberedGraph<T> G) {
    return (o1, o2) -> {
      int o1edges = G.getSuccNodeCount(o1) + G.getPredNodeCount(o1);
      int o2edges = G.getSuccNodeCount(o2) + G.getPredNodeCount(o2);
      if (o1edges != o2edges) {
        return o2edges - o1edges;
      } else {
        return o2.toString().compareTo(o1.toString());
      }
    };
  }
  
  public ColoredVertices<T> color(final NumberedGraph<T> G) {
    return color(G, defaultComparator(G), Integer.MAX_VALUE);
  }
  
  public ColoredVertices<T> color(final NumberedGraph<T> G, int maxColors) {
    return color(G, defaultComparator(G), maxColors);
  }

  public ColoredVertices<T> color(final NumberedGraph<T> G, Comparator<T> order, int maxColors) {
    int[] colors = new int[ G.getMaxNumber() + 1];
    for(int i = 0; i < colors.length; i++) {
      colors[i] = -1;
    }
    
    SortedSet<T> vertices = new TreeSet<>(order);

    for (T n : G) {
      vertices.add(n);
    }

    int currentColor = 0;
    int colored = 0;

    for(T n : vertices) {
      int id = G.getNumber(n);
      if (colors[id] == -1) {
        colors[id] = currentColor;
        colored++;

        for(T m : vertices) {
          if (colors[G.getNumber(m)] == -1) {
            color_me: {
              for(T p : Iterator2Iterable.make(G.getPredNodes(m)) ) {
                if (colors[ G.getNumber(p) ] == currentColor) {
                  break color_me;
                }
              }
  
              for(T s : Iterator2Iterable.make(G.getSuccNodes(m))) {
                if (colors[G.getNumber(s)] == currentColor) {
                  break color_me;
                }
              }
  
              colors[G.getNumber(m)] = currentColor;
              colored++;
              
              if (currentColor == maxColors - 1) {
                return new ColoredVertices<>(false, G, colors, currentColor);
              }

            }

          }
        }

        currentColor++;

        if (currentColor == maxColors - 1) {
          return new ColoredVertices<>(false, G, colors, currentColor);
        }
      }
    }
    
    assert colored == G.getNumberOfNodes();

    return new ColoredVertices<>(true, G, colors, currentColor);
  }

}
