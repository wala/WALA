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
package com.ibm.wala.util.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.graph.traverse.BFSIterator;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * Utility class to check integrity of a graph data structure.
 */
public class GraphIntegrity {

  /**
   * DEBUG_LEVEL:
   * <ul>
   * <li>0 No output
   * <li>1 Print some simple stats and warning information
   * <li>2 Detailed debugging
   * </ul>
   */
  static final int DEBUG_LEVEL = 0;

  public static <T> void check(Graph<T> G) throws UnsoundGraphException {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    checkNodeCount(G);
    checkEdges(G);
    checkEdgeCounts(G);
  }

  private static <T> void checkEdgeCounts(Graph<T> G) throws UnsoundGraphException {
    for (T N : G) {
      int count1 = G.getSuccNodeCount(N);
      int count2 = IteratorUtil.count(G.getSuccNodes(N));
      if (count1 != count2) {
        throw new UnsoundGraphException("getSuccNodeCount " + count1 + " is wrong for node " + N);
      }

      int count3 = G.getPredNodeCount(N);
      int count4 = IteratorUtil.count(G.getPredNodes(N));
      if (count3 != count4) {
        throw new UnsoundGraphException("getPredNodeCount " + count1 + " is wrong for node " + N);
      }
    }
  }

  private static <T> void checkEdges(Graph<T> G) throws UnsoundGraphException {
    for (T N : G) {
      if (!G.containsNode(N)) {
        throw new UnsoundGraphException(N + " is not contained in the the graph " + G.containsNode(N));
      }
      PRED: for (T pred : Iterator2Iterable.make(G.getPredNodes(N))) {
        if (!G.containsNode(pred)) {
          throw new UnsoundGraphException(pred + " is a predecessor of " + N + " but is not contained in the graph");
        }
        for (Object succ : Iterator2Iterable.make(G.getSuccNodes(pred))) {
          if (succ.equals(N)) {
            continue PRED;
          }
        }
        // didn't find N
        G.getPredNodes(N);
        G.getSuccNodes(pred);
        throw new UnsoundGraphException(pred + " is a predecessor of " + N + " but inverse doesn't hold");
      }
      SUCC: for (T succ : Iterator2Iterable.make(G.getSuccNodes(N))) {
        if (!G.containsNode(succ)) {
          throw new UnsoundGraphException(succ + " is a successor of " + N + " but is not contained in the graph");
        }
        for (Object pred : Iterator2Iterable.make(G.getPredNodes(succ))) {
          if (pred.equals(N)) {
            continue SUCC;
          }
        }
        // didn't find N
        throw new UnsoundGraphException(succ + " is a successor of " + N + " but inverse doesn't hold");
      }
    }

  }

  @SuppressWarnings("unused")
  private static <T> void checkNodeCount(Graph<T> G) throws UnsoundGraphException {
    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;
    int n5 = 0;
    try {
      n1 = G.getNumberOfNodes();
      n2 = 0;
      for (T t : G) {
        Object n = t;
        if (DEBUG_LEVEL > 1) {
          System.err.println(("n2 loop: " + n));
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

  private static <T> UnsoundGraphException setDiffException(String msg, Iterator<? extends T> i1, Iterator<T> i2) {
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
    if (set1.size() > 0) {
      Object first = set1.iterator().next();
      msg = msg + ", first iterator contained " + first;
      return new UnsoundGraphException(msg);
    } else {
      set2.removeAll(s1clone);
      if (set2.size() > 0) {
        Object first = set2.iterator().next();
        msg = msg + ", 2nd iterator contained " + first;
        return new UnsoundGraphException(msg);
      } else {
        msg = msg + "set2.size = 0";
        return new UnsoundGraphException(msg);
      }
    }
  }

  public static class UnsoundGraphException extends Exception {

    private static final long serialVersionUID = 1503478788521696930L;

    public UnsoundGraphException() {
      super();

    }

    public UnsoundGraphException(String s) {
      super(s);
    }

  }

  /**
   * @throws IllegalArgumentException
   *           if c is null
   */
  public static void printCollection(String string, Collection<?> c) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    System.err.println(string);
    if (c.isEmpty()) {
      System.err.println("none\n");
    } else {
      for (Object name : c) {
        System.err.println(name.toString());
      }
      System.err.println("\n");
    }
  }

}
