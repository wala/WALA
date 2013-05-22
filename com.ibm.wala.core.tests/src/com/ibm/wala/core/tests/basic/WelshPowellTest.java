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
package com.ibm.wala.core.tests.basic;

import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.WelshPowell;

public class WelshPowellTest {

  private <T> void assertColoring(Graph<T> G, Map<T,Integer> colors) {
    for(T n : G) {
      for(Iterator<T> ss = G.getSuccNodes(n); ss.hasNext(); ) {
        Assert.assertTrue(colors.get(n).intValue() != colors.get(ss.next()).intValue()); 
      }
      for(Iterator<T> ps = G.getPredNodes(n); ps.hasNext(); ) {
        Assert.assertTrue(colors.get(n).intValue() != colors.get(ps.next()).intValue()); 
      }
    }
  }
  
  private <T> Graph<T> buildGraph(T[][] data) {
    SlowSparseNumberedGraph<T> G = SlowSparseNumberedGraph.make();
    for(int i = 0; i < data.length; i++) {
      G.addNode(data[i][0]);
    }
    for(int i = 0; i < data.length; i++) {
      for(int j = 1; j < data[i].length; j++) {
        G.addEdge(data[i][0], data[i][j]);
      }
    }
    
    return G;
  }
  
    @Test
    public void testOne() {
      Graph<Integer> G = 
        buildGraph(new Integer[][]{
            new Integer[]{1, 6, 7, 8},
            new Integer[]{2, 5, 7, 8},
            new Integer[]{3, 5, 6, 8},
            new Integer[]{4, 7, 6, 5},
            new Integer[]{5, 2, 4, 3},
            new Integer[]{6, 3, 1, 4},
            new Integer[]{7, 1, 2, 4},
            new Integer[]{8, 1, 2, 3}});
      Pair<Map<Integer, Integer>,Integer> colors = new WelshPowell<Integer>().color(G);
      System.err.println(colors);
      assertColoring(G, colors.fst);
      Assert.assertTrue(colors.snd.intValue() <= 4);
    }
    
    @Test
    public void testTwo() {
      Graph<String> G =
        buildGraph(new String[][] {
           new String[]{"poly1", "poly2", "star1", "poly5"},
           new String[]{"poly2", "poly1", "star2", "poly3"},
           new String[]{"poly3", "poly2", "star3", "poly4"},
           new String[]{"poly4", "poly3", "star4", "poly5"},
           new String[]{"poly5", "poly4", "star5", "poly1"},
           new String[]{"star1", "poly1", "star3", "star4"},
           new String[]{"star2", "poly2", "star4", "star5"},
           new String[]{"star3", "poly3", "star1", "star5"},
           new String[]{"star4", "poly4", "star1", "star2"},
           new String[]{"star5", "poly5", "star2", "star3"}});
      Pair<Map<String, Integer>,Integer> colors = new WelshPowell<String>().color(G);
      System.err.println(colors);
      assertColoring(G, colors.fst);
      Assert.assertTrue(colors.snd.intValue() == 3);       
    }
}
