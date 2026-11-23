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

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.graph.traverse.BFSIterator;
import com.ibm.wala.util.graph.traverse.DFS;
import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/** Utility class to check integrity of a graph data structure. */
public class GraphIntegrity {

  /**
   * DEBUG_LEVEL:
   *
   * <ul>
   *   <li>0 No output
   *   <li>1 Print some simple stats and warning information
   *   <li>2 Detailed debugging
   * </ul>
   */
  static final int DEBUG_LEVEL = 0;

  public static <T> void check(Graph<T> G) throws UnsoundGraphException {
    checkArgument(G != null, "G is null");
    checkNodeCount(G);
    checkEdges(G);
  }

  private static <T> void checkEdges(Graph<T> graph) throws UnsoundGraphException {
    // Transpose the successor relation once. Each entry records whether the corresponding
    // predecessor has been seen. Discard each incoming adjacency map after checking it.
    Map<T, Map<T, Boolean>> incoming = HashMapFactory.make();
    for (T node : graph) {
      int count = 0;
      for (T succ : Iterator2Iterable.make(graph.getSuccNodes(node))) {
        if (!graph.containsNode(succ)) {
          throw new UnsoundGraphException(
              succ + " is a successor of " + node + " but is not contained in the graph");
        }
        incoming.computeIfAbsent(succ, unused -> HashMapFactory.make()).put(node, false);
        count++;
      }
      int reportedCount = graph.getSuccNodeCount(node);
      if (count != reportedCount) {
        throw new UnsoundGraphException(
            "getSuccNodeCount " + reportedCount + " is wrong for node " + node);
      }
    }

    for (T node : graph) {
      if (!graph.containsNode(node)) {
        throw new UnsoundGraphException(node + " is not contained in the graph");
      }
      Map<T, Boolean> expected = incoming.remove(node);
      int count = 0;
      int matched = 0;
      for (T pred : Iterator2Iterable.make(graph.getPredNodes(node))) {
        if (!graph.containsNode(pred)) {
          throw new UnsoundGraphException(
              pred + " is a predecessor of " + node + " but is not contained in the graph");
        }
        Boolean seen = expected == null ? null : expected.replace(pred, true);
        if (seen == null) {
          throw new UnsoundGraphException(
              pred + " is a predecessor of " + node + " but inverse doesn't hold");
        }
        if (!seen) {
          matched++;
        }
        count++;
      }
      if (expected != null && matched != expected.size()) {
        for (var edge :
            expected.entrySet()) { // diff-coverage: ignore -- an unmatched edge must exist
          if (!edge.getValue()) {
            throw new UnsoundGraphException(
                node + " is a successor of " + edge.getKey() + " but inverse doesn't hold");
          }
        }
      }
      int reportedCount = graph.getPredNodeCount(node);
      if (count != reportedCount) {
        throw new UnsoundGraphException(
            "getPredNodeCount " + reportedCount + " is wrong for node " + node);
      }
    }

    if (!incoming.isEmpty()) {
      throw new UnsoundGraphException(
          incoming.keySet().iterator().next() + " is a successor missing from the graph iterator");
    }
  }

  @SuppressWarnings("unused")
  private static <T> void checkNodeCount(Graph<T> G) throws UnsoundGraphException {
    final int n1;
    int n2;
    final int n3;
    final int n4;
    final int n5;
    try {
      n1 = G.getNumberOfNodes();
      n2 = 0;
      for (T t : G) {
        if (DEBUG_LEVEL > 1) {
          System.err.println(("n2 loop: " + t));
        }
        n2++;
      }
      n3 = IteratorUtil.count(new BFSIterator<>(G));
      n4 = IteratorUtil.count(DFS.iterateDiscoverTime(G));
      n5 = IteratorUtil.count(DFS.iterateFinishTime(G));
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw new UnsoundGraphException(e.toString());
    }
    if (n1 != n2) {
      throw new UnsoundGraphException("n1: " + n1 + " n2: " + n2);
    }
    if (n1 != n3) {
      throw setDiffException("n1: " + n1 + " n3: " + n3, G.iterator(), new BFSIterator<>(G));
    }
    if (n1 != n4) {
      throw new UnsoundGraphException("n1: " + n1 + " n4: " + n3);
    }
    if (n1 != n5) {
      throw new UnsoundGraphException("n1: " + n1 + " n5: " + n3);
    }
  }

  private static <T> UnsoundGraphException setDiffException(
      String msg, Iterator<? extends T> i1, Iterator<T> i2) {
    HashSet<T> set1 = HashSetFactory.make();
    while (i1.hasNext()) {
      T o1 = i1.next();
      boolean b = set1.add(o1);
      if (!b) {
        return new UnsoundGraphException("set1 already contained " + o1);
      }
    }
    HashSet<T> set2 = HashSetFactory.make();
    while (i2.hasNext()) {
      T o2 = i2.next();
      boolean b = set2.add(o2);
      if (!b) {
        return new UnsoundGraphException("set2 already contained " + o2);
      }
    }
    GraphIntegrity.printCollection("set 1 ", set1);
    GraphIntegrity.printCollection("set 2 ", set2);
    @SuppressWarnings("unchecked")
    HashSet<T> s1clone = (HashSet<T>) set1.clone();
    set1.removeAll(set2);
    if (!set1.isEmpty()) {
      Object first = set1.iterator().next();
      msg = msg + ", first iterator contained " + first;
    } else {
      set2.removeAll(s1clone);
      if (!set2.isEmpty()) {
        Object first = set2.iterator().next();
        msg = msg + ", 2nd iterator contained " + first;
      } else {
        msg += "set2.size = 0";
      }
    }
    return new UnsoundGraphException(msg);
  }

  public static class UnsoundGraphException extends Exception {

    @Serial private static final long serialVersionUID = 1503478788521696930L;

    public UnsoundGraphException() {}

    public UnsoundGraphException(String s) {
      super(s);
    }
  }

  /**
   * @throws IllegalArgumentException if c is null
   */
  public static void printCollection(String string, Collection<?> c) {
    checkArgument(c != null, "c is null");
    System.err.println(string);
    if (c.isEmpty()) {
      System.err.println("none\n");
    } else {
      for (Object name : c) {
        System.err.println(name);
      }
      System.err.println("\n");
    }
  }
}
