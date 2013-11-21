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
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NumberedGraph;

public class WelshPowell<T extends INodeWithNumber> {

  public static <T> Comparator<T> defaultComparator(final NumberedGraph<T> G) {
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
  
  public Pair<Map<T,Integer>, Integer>  color(final NumberedGraph<T> G) {
    return color(G, defaultComparator(G));
  }
  
  public Pair<Map<T,Integer>, Integer>  color(final NumberedGraph<T> G, Comparator<T> order) {
    int[] colors = new int[ G.getNumberOfNodes() ];
    for(int i = 0; i < colors.length; i++) {
      colors[i] = -1;
    }
    
    SortedSet<T> vertices = new TreeSet<T>(order);
    
    for(T n : G) {
      vertices.add(n);
    }
    
    int currentColor = 0;
    int colored = 0;
    
    while(colored < G.getNumberOfNodes()) {
      for(T n : vertices) {
        int id = n.getGraphNodeId();
        if (colors[id] == -1) {
          colors[id] = currentColor;
          colored++;
          
          for(T m : vertices) {
            if (colors[m.getGraphNodeId()] == -1) {
              color_me: {
                for(Iterator<T> ps = G.getPredNodes(m); ps.hasNext(); ) {
                  T p = ps.next();
                  if (colors[ p.getGraphNodeId() ] == currentColor) {
                    break color_me;
                  }
                }
                
                for(Iterator<T> ss = G.getSuccNodes(m); ss.hasNext(); ) {
                  T s = ss.next();
                  if (colors[s.getGraphNodeId()] == currentColor) {
                    break color_me;
                  }
                }
                
                colors[m.getGraphNodeId()] = currentColor;
                colored++;
              }
            }
          }
          
          currentColor++;
        }
      }
    }
    
    Map<T,Integer> colorMap = HashMapFactory.make();
    for(int i = 0; i < colors.length; i++) {
      colorMap.put(G.getNode(i), colors[i]);
    }
    return Pair.make(colorMap, currentColor);
  }

}
