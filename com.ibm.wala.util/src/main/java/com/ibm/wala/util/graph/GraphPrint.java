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

import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.Util;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Simple graph printing utility */
public class GraphPrint {

  public static <T> String genericToString(Graph<T> G) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    SlowSparseNumberedGraph<T> sg = SlowSparseNumberedGraph.make();
    for (T name : G) {
      sg.addNode(name);
    }
    for (T n : G) {
      for (T d : Iterator2Iterable.make(G.getSuccNodes(n))) {
        sg.addEdge(n, d);
      }
    }
    return sg.toString();
  }

  public static <T> String graphToJSON(Graph<T> g) {
    Map<String, Set<String>> edges = new LinkedHashMap<>();
    for (T node : g) {
      String nodeStr = node.toString();
      Set<String> succs = MapUtil.findOrCreateSet(edges, nodeStr);
      for (T succ : Iterator2Iterable.make(g.getSuccNodes(node))) {
        succs.add(succ.toString());
      }
    }
    return toJSON(edges);
  }

  public static String toJSON(Map<String, Set<String>> map) {
    StringBuilder res = new StringBuilder();
    res.append("{\n");
    res.append(
        joinWith(
            Util.mapToSet(
                map.entrySet(),
                e -> {
                  StringBuilder res1 = new StringBuilder();
                  if (e.getValue().size() > 0) {
                    res1.append("    \"").append(e.getKey()).append("\": [\n");
                    res1.append(
                        joinWith(
                            Util.mapToSet(e.getValue(), str -> "        \"" + str + '"'), ",\n"));
                    res1.append("\n    ]");
                  }
                  return res1.length() == 0 ? null : res1.toString();
                }),
            ",\n"));
    res.append("\n}");
    return res.toString();
  }

  private static String joinWith(Iterable<String> lst, String sep) {
    StringBuilder res = new StringBuilder();
    ArrayList<String> strings = new ArrayList<>();
    for (String s : lst) if (s != null) strings.add(s);

    boolean fst = true;
    for (String s : strings) {
      if (fst) fst = false;
      else res.append(sep);
      res.append(s);
    }
    return res.toString();
  }
}
