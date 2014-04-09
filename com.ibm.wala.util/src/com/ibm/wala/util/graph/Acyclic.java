/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
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
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;

/**
 * Utilities for dealing with acyclic subgraphs
 */
public class Acyclic {

  /*
   * prevent instantiation 
   */
  private Acyclic() {
  }
  
  /**
   * This is slow. Fix it.
   */
  public static <T> boolean isAcyclic(NumberedGraph<T> G, T root) {
    IBinaryNaturalRelation r = computeBackEdges(G, root);
    Iterator<IntPair> it = r.iterator();
    return !it.hasNext();
  }

  /**
   * Compute a relation R s.t. (i,j) \in R iff (i,j) is a backedge according to a DFS of a numbered graph starting from some root.
   * 
   * Not efficient. Recursive and uses hash sets.
   */
  public static <T> IBinaryNaturalRelation computeBackEdges(NumberedGraph<T> G, T root) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    BasicNaturalRelation result = new BasicNaturalRelation();

    Set<T> visited = HashSetFactory.make();
    Set<T> onstack = HashSetFactory.make();
    dfs(result, root, G, visited, onstack);
    return result;
  }

  private static <T> void dfs(BasicNaturalRelation result, T root, NumberedGraph<T> G, Set<T> visited, Set<T> onstack) {
    visited.add(root);
    onstack.add(root);
    for (Iterator<? extends T> it = G.getSuccNodes(root); it.hasNext();) {
      T dstNode = it.next();
      if (onstack.contains(dstNode)) {
        int src = G.getNumber(root);
        int dst = G.getNumber(dstNode);
        result.add(src, dst);
      }
      if (!visited.contains(dstNode)) {
        dfs(result, dstNode, G, visited, onstack);
      }
    }
    onstack.remove(root);
  }

  public static <T> boolean hasIncomingBackEdges(Path p, NumberedGraph<T> G, T root) {
    /*
     * TODO: pull out computeBackEdges, and pass in the backedge relation as a parameter to this call
     */
    IBinaryNaturalRelation backedges = computeBackEdges(G, root);
    for (int index = 0; index < p.size(); index++) {
      int gn = p.get(index);
      Iterator<? extends T> predIter = G.getPredNodes(G.getNode(gn));
      while (predIter.hasNext()) {
        if (backedges.contains(G.getNumber(predIter.next()), gn))
          return true;
      }
    }
    return false;
  }

  /**
   * Compute a set of acyclic paths through a graph G from a node src to a node sink.
   * 
   * This is not terribly efficient.
   * 
   * @param max the max number of paths to return.
   */
  public static <T> Collection<Path> computeAcyclicPaths(NumberedGraph<T> G, T root, T src, T sink, int max) {
    Collection<Path> result = HashSetFactory.make();
    EdgeFilteredNumberedGraph<T> acyclic = new EdgeFilteredNumberedGraph<T>(G, computeBackEdges(G, root));

    Collection<Path> worklist = HashSetFactory.make();
    Path sinkPath = Path.make(G.getNumber(sink));
    worklist.add(sinkPath);
    while (!worklist.isEmpty() && result.size() <= max) {
      Path p = worklist.iterator().next();
      worklist.remove(p);
      int first = p.get(0);
      if (first == G.getNumber(src)) {
        result.add(p);
      } else {
        for (IntIterator it = acyclic.getPredNodeNumbers(acyclic.getNode(first)).intIterator(); it.hasNext();) {
          worklist.add(Path.prepend(it.next(), p));
        }
      }
    }

    return result;
  }
}
