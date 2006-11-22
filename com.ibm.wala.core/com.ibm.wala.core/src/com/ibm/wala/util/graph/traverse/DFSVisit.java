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
package com.ibm.wala.util.graph.traverse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * Depth first search of a graph using a stack instead of recursive method
 * calls. It allows each vertice of the graph to be visited when its is first
 * discovered and when it is "finished" (i.e. completely processed).
 * 
 * @author <a href="mailto:achille@us.ibm.com">Achille Fokoue </a>
 * @version
 */
public class DFSVisit {

  /**
   * the empty iterator constant
   */
  public final static Iterator EMPTY_ITERATOR = Collections.EMPTY_LIST.iterator();

  /**
   * A simple map interface. It is used during DFS to associate a node in a
   * graph to its its traversal state.
   */
  public interface SimpleMap<K,V> {
    /**
     * returns the state associated with a given node
     * 
     * @param node
     */
    public V get(Object node);

    /**
     * sets the state associated with a given node, and returns the previous
     * associated states
     * 
     * @param key
     */
    public V put(K key, V value);

    /**
     * clear this {@link SimpleMap}
     * 
     */
    public void clear();

  }

  /**
   * A {@link SimpleMap}that maps
   * {@link com.ibm.wala.util.graph.INodeWithNumber}to their traversal state.
   * <p>
   * NOTE: the capacity of this map does not grow!
   * 
   * @version
   */
  public static class NumberedSimpleMap<K,V> implements SimpleMap<K,V> {
    Object[] table;

    public NumberedSimpleMap(int capacity) {
      table = new Object[capacity];
    }

    @SuppressWarnings("unchecked")
    public V get(Object n) {
      int id = ((INodeWithNumber) n).getGraphNodeId();
      V ret = (V) table[id];
      return ret;
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
      int id = ((INodeWithNumber) key).getGraphNodeId();
      V prev = (V) table[id];
      table[id] = value;
      return prev;
    }

    public void clear() {
      Arrays.fill(table, null);
    }
  }

  /**
   * 
   * Default implementation of {@link SimpleMap}based on a {@link HashMap}
   * 
   * @version
   */
  public static class DefaultSimpleMap<K,V> extends HashMap<K,V> implements SimpleMap<K,V> {

    private static final long serialVersionUID = 8314610925208365087L;

    /**
     * Constructs an empty DefaultSimpleMap with the specified initial capacity.
     */
    public DefaultSimpleMap(int initialCapacity) {
      super(initialCapacity);
    }

    /**
     */
    public DefaultSimpleMap() {
      super();
    }

    /**
     * Constructs an empty DefaultSimpleMap with the specified initial capacity
     * and load factor.
     */
    public DefaultSimpleMap(int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor);
    }
  }

  /**
   * A visitor that visits Nodes in the DFS2 of a graph
   * 
   * @author <a href="mailto:achille@us.ibm.com">Achille Fokoue </a>
   * @version
   */
  public static abstract class Visitor {
    public abstract void visit(Object node, Object parent);

    public abstract void leave(Object node);

    public void visitEdge(Object source, Object target) {
    }
  }

  /**
   * Depth first search of a node of graph using a stack instead of recursive
   * method calls. This is necessary in order to avoid
   * java.lang.StackOverflowError for big graphs.
   */
  private static <T >void DFS(Graph<T> g, T node, SimpleMap<T, Iterator<? extends T>> states, Visitor visitor) {
    int leave = 0;
    int found = 0;
    Stack<T> pending = new Stack<T>();
    found++;
    visitor.visit(node, null);
    states.put(node, getConnectedTo(g, node));
    pending.push(node);
    cont: while (!pending.isEmpty()) {
      T current = pending.peek();
      Iterator<? extends T> currentChildren =  states.get(current);
      for (Iterator<? extends T> e = currentChildren; e.hasNext();) {
        T currentChild = e.next();
        // visit edge
        visitor.visitEdge(current, currentChild);
        Iterator currentGrandChildren = (Iterator) states.get(currentChild);
        if (currentGrandChildren == null) {
          // new child discovered
          found++;
          visitor.visit(currentChild, current);
          states.put(currentChild, getConnectedTo(g, currentChild));
          pending.push(currentChild);
          // recursion continue
          continue cont;
        }
      }
      // all children of current have been processed
      // current is finished
      Object ret = pending.pop();
      if (Assertions.verifyAssertions) {
        Assertions._assert(ret == current);
      }
      leave++;
      visitor.leave(current);
      // allow the garbage collector to do its job
      Iterator<? extends T> empty = EmptyIterator.instance();
      states.put(current, empty);

    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(found == leave, found + " nodes discovered, but " + leave + " finished!");
    }
  }

  protected static <T> Iterator<? extends T> getConnectedTo(Graph<T> g, T node) {
    return g.getSuccNodes(node);
  }

  /**
   * Depth first search of a graph using a stack instead of recursive method
   * calls. This is necessary in order to avoid java.lang.StackOverflowError for
   * big graphs
   * 
   * @param g
   *          the graph to traverse
   * @param nodes
   *          an iterator over the nodes where the traversal should start
   * @param visitor
   *          the visitor to notify when nodes are discovered and finished
   * @param states
   *          the map to use to store and retrieve the state of nodes
   */
  public static <T> void DFS(Graph<T> g, Iterator<? extends T> nodes, Visitor visitor, SimpleMap<T,Iterator<? extends T>> states) {
    for (Iterator<? extends T> it = nodes; it.hasNext();) {
      T node = it.next();
      if (node != null && states.get(node) == null) {
        DFS(g, node, states, visitor);
      }
    }
  }

  /**
   * Depth first search of a graph using a stack instead of recursive method
   * calls. This is necessary in order to avoid java.lang.StackOverflowError for
   * big graphs
   * 
   * @param g
   *          the graph to traverse
   * @param nodes
   *          an iterator over the nodes where the traversal should start
   * @param visitor
   *          the visitor to notify when nodes are discovered and finished
   */
  public static <T> void DFS(Graph<T> g, Iterator<? extends T> nodes, Visitor visitor) {
    if (g instanceof NumberedGraph) {
      DFS(g, nodes, visitor, new NumberedSimpleMap<T, Iterator<? extends T>>(g.getNumberOfNodes()));
    } else {
      DFS(g, nodes, visitor, new DefaultSimpleMap<T,Iterator<? extends T>>());
    }
  }

  /**
   * 
   * Depth first search of a graph using a stack instead of recursive method
   * calls. This is necessary in order to avoid java.lang.StackOverflowError for
   * big graphs
   * 
   * @param g
   *          the graph to traverse
   * @param visitor
   *          the visitor to notify when nodes are discovered and finished
   */
  public static <T> void DFS(Graph<T> g, Visitor visitor) {
    DFS(g, g.iterateNodes(), visitor);
  }

}