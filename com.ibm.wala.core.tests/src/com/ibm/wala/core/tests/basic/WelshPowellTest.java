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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.DelegatingNumberedGraph;
import com.ibm.wala.util.graph.impl.NodeWithNumberedEdges;
import com.ibm.wala.util.graph.traverse.WelshPowell;
import com.ibm.wala.util.graph.traverse.WelshPowell.ColoredVertices;

public class WelshPowellTest {

  public static <T> void assertColoring(Graph<T> G, Map<T,Integer> colors, boolean fullColor) {
    for(T n : G) {
      for(T succ : Iterator2Iterable.make(G.getSuccNodes(n))) {
        if (!fullColor &&  (!colors.containsKey(n) || !colors.containsKey(succ)) ) {
          continue;
        }
        Assert.assertTrue(n + " and succ: " + succ + " have same color: " + colors.get(n).intValue(), colors.get(n).intValue() != colors.get(succ).intValue()); 
      }
      for(T pred : Iterator2Iterable.make(G.getPredNodes(n))) {
        if (!fullColor && (!colors.containsKey(n) || !colors.containsKey(pred)) ) {
          continue;
        }
        Assert.assertTrue(n + " and pred: " + pred + " have same color:" + colors.get(n).intValue(), colors.get(n).intValue() != colors.get(pred).intValue()); 
      }
    }
  }
  
  private class TypedNode<T> extends NodeWithNumberedEdges {
    private final T data;
    
    private TypedNode(T data) {
      this.data = data;
    }
    
    @Override
    public String toString() {
      return data.toString();
    }
  }
    
  private <T> NumberedGraph<TypedNode<T>> buildGraph(T[][] data) {
    DelegatingNumberedGraph<TypedNode<T>> G = new DelegatingNumberedGraph<>();
    Map<T,TypedNode<T>> nodes = HashMapFactory.make();
    for (T[] element : data) {
      TypedNode<T> n = new TypedNode<>(element[0]);
      nodes.put(element[0], n);
      G.addNode(n);
    }
    for (T[] element : data) {
      for(int j = 1; j < element.length; j++) {
        G.addEdge(nodes.get(element[0]), nodes.get(element[j]));
      }
    }
    
    return G;
  }
  
    @Test
    public void testOne() {
      NumberedGraph<TypedNode<Integer>> G = 
        buildGraph(new Integer[][]{
            new Integer[]{1, 6, 7, 8},
            new Integer[]{2, 5, 7, 8},
            new Integer[]{3, 5, 6, 8},
            new Integer[]{4, 7, 6, 5},
            new Integer[]{5, 2, 4, 3},
            new Integer[]{6, 3, 1, 4},
            new Integer[]{7, 1, 2, 4},
            new Integer[]{8, 1, 2, 3}});
      ColoredVertices<TypedNode<Integer>> colors = new WelshPowell<TypedNode<Integer>>().color(G);
      System.err.println(colors.getColors());
      assertColoring(G, colors.getColors(), true);
      Assert.assertTrue(colors.getNumColors() <= 4);
    }
    
    @Test
    public void testTwo() {
      NumberedGraph<TypedNode<String>> G =
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
      ColoredVertices<TypedNode<String>> colors = new WelshPowell<TypedNode<String>>().color(G);
      System.err.println(colors.getColors());
      assertColoring(G, colors.getColors(), true);
      Assert.assertTrue(colors.getNumColors() == 3);       
    }
 }
