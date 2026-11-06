/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.traverse.SCCIterator;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility class for computing an analysis result for call graph nodes and their transitive callees,
 * given the results for individual nodes.
 */
public class CallGraphTransitiveClosure {

  /**
   * Compute the transitive closure of an analysis result over all callees.
   *
   * <p>The closure is computed over the strongly connected components of {@code cg}, callees first.
   * Each component's result is the union of its members' own results and the results of the
   * components its members call, so every node in a component shares the same result.
   *
   * @param cg the call graph
   * @param nodeResults analysis result for each individual node
   * @return a map from each node to the analysis result for the node and its transitive callees
   */
  public static <T> Map<CGNode, OrdinalSet<T>> transitiveClosure(
      CallGraph cg, Map<CGNode, Collection<T>> nodeResults) {
    MutableMapping<T> domain = MutableMapping.make();
    for (Collection<T> c : nodeResults.values()) {
      for (T t : c) {
        domain.add(t);
      }
    }

    // SCCIterator enumerates components callers first, so walk them in reverse to visit each
    // component only after every component it calls.
    List<Set<CGNode>> components = new ArrayList<>();
    new SCCIterator<>(cg).forEachRemaining(components::add);
    int[] componentOf = new int[cg.getMaxNumber() + 1];
    for (int i = 0; i < components.size(); i++) {
      for (CGNode n : components.get(i)) {
        componentOf[cg.getNumber(n)] = i;
      }
    }

    BitVectorIntSet[] closure = new BitVectorIntSet[cg.getMaxNumber() + 1];
    Map<CGNode, OrdinalSet<T>> result = HashMapFactory.make();
    for (int i = components.size() - 1; i >= 0; i--) {
      Set<CGNode> component = components.get(i);
      BitVectorIntSet bits = new BitVectorIntSet();
      for (CGNode n : component) {
        Collection<T> own = nodeResults.get(n);
        if (own != null) {
          for (T t : own) {
            bits.add(domain.getMappedIndex(t));
          }
        }
        for (CGNode callee : Iterator2Iterable.make(cg.getSuccNodes(n))) {
          int calleeNumber = cg.getNumber(callee);
          // A callee in this component contributes its own results above; any other callee's
          // component has already been visited.
          if (componentOf[calleeNumber] != i) {
            bits.addAll(closure[calleeNumber]);
          }
        }
      }
      OrdinalSet<T> componentClosure = new OrdinalSet<>(bits, domain);
      for (CGNode n : component) {
        closure[cg.getNumber(n)] = bits;
        result.put(n, componentClosure);
      }
    }
    return result;
  }

  /** Collect analysis result for each {@link CGNode} in a {@link Map}. */
  public static <T> Map<CGNode, Collection<T>> collectNodeResults(
      CallGraph cg, Function<CGNode, Collection<T>> nodeResultComputer) {
    Map<CGNode, Collection<T>> result = HashMapFactory.make();
    for (CGNode n : cg) {
      result.put(n, nodeResultComputer.apply(n));
    }
    return result;
  }
}
