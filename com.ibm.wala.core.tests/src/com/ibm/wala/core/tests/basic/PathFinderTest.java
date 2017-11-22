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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.DFSAllPathsFinder;

public class PathFinderTest {
  
  private static Graph<String> createGraph(String edges) {
    Graph<String> g = SlowSparseNumberedGraph.make();
    for(int i = 0; i < edges.length(); i+= 2) {
      String from = edges.substring(i, i+1);
      if (! g.containsNode(from)) {
        g.addNode(from);
      }
      
      String to = edges.substring(i+1, i+2);
      if (! g.containsNode(to)) {
        g.addNode(to);
      }
      
      g.addEdge(from, to);
    }
    return g;
  }

  private static DFSAllPathsFinder<String> makeFinder(Graph<String> g, String start, final String end) {
    return new DFSAllPathsFinder<>(g, start, end::equals);
  }

  private static void checkPaths(DFSAllPathsFinder<String> paths, int expectedCount) {
    int count = 0;
    List<String> path;
    while ((path = paths.find()) != null) {  
      System.err.println(path);
      count++;
    }
    Assert.assertEquals(expectedCount, count);
  }
  
  private static final String edges1 = "ABBCBDCECFDGDHEIFIGJHJJKIKKL";

  @Test
  public void testPaths1() {
    Graph<String> g = createGraph(edges1);
    DFSAllPathsFinder<String> paths = makeFinder(g, "A", "L");
    checkPaths(paths, 4);
  }

  private static final String edges2 = "ABBCBDCECFDGDHEIFIGJHJJKIKKCKL";

  @Test
  public void testPaths2() {
    Graph<String> g = createGraph(edges2);
    DFSAllPathsFinder<String> paths = makeFinder(g, "A", "L");
    checkPaths(paths, 4);
  }

  private static final String edges3 = "ABBHBCBDCECFDGDHEIFIGJHJJKIKKCKL";

  @Test
  public void testPaths3() {
    Graph<String> g = createGraph(edges3);
    DFSAllPathsFinder<String> paths = makeFinder(g, "A", "L");
    checkPaths(paths, 5);
  }

  private static final String edges4 = "ABACADAEBABCBDBECACBCDCEDADBDCDEEAEBECED";

  @Test
  public void testPaths4() {
    Graph<String> g = createGraph(edges4);
    DFSAllPathsFinder<String> paths = makeFinder(g, "A", "E");
    checkPaths(paths, 1 + 3 + 6 + 6);
  }
}
