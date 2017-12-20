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
package com.ibm.wala.util.graph.dominators;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.traverse.DFSDiscoverTimeIterator;
import com.ibm.wala.util.graph.traverse.SlowDFSDiscoverTimeIterator;

/**
 * Calculate dominators using Langauer and Tarjan's fastest algorithm. TOPLAS 1(1), July 1979. This implementation uses path
 * compression and results in a O(e * alpha(e,n)) complexity, where e is the number of edges in the CFG and n is the number of
 * nodes.
 * 
 * Sources: TOPLAS article, Muchnick book
 */

public abstract class Dominators<T> {
  static final boolean DEBUG = false;

  /**
   * a mapping from DFS number to node
   */
  private final T[] vertex;

  /**
   * a convenient place to locate the graph to avoid passing it internally
   */
  protected final Graph<T> G;

  /**
   * the root node from which to build dominators
   */
  protected final T root;

  /**
   * the number of nodes reachable from the root
   */
  protected int reachableNodeCount = 0;

  /**
   * @param G The graph
   * @param root The root from which to compute dominators
   * @throws IllegalArgumentException if G is null
   */
  @SuppressWarnings("unchecked")
  public Dominators(Graph<T> G, T root) throws IllegalArgumentException {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    this.G = G;
    this.root = root;
    if (G.getNumberOfNodes() == 0) {
      throw new IllegalArgumentException("G has no nodes");
    }
    this.vertex = (T[]) new Object[G.getNumberOfNodes() + 1];
  }

  public static <T> Dominators<T> make(Graph<T> G, T root) {
    if (G instanceof NumberedGraph) {
      return new NumberedDominators<>((NumberedGraph<T>) G, root);
    } else {
      return new GenericDominators<>(G, root);
    }
  }

  /**
   * is node dominated by master?
   */
  public boolean isDominatedBy(T node, T master) {
    for (T ptr = node; ptr != null; ptr = getIdom(ptr))
      // use equals() since sometimes the CFGs get
      // reconstructed --MS
      if (ptr.equals(master))
        return true;

    return false;
  }

  /**
   * return the immediate dominator of node
   */
  public T getIdom(T node) {
    return getInfo(node).dominator;
  }

  /**
   * return an Iterator over all nodes that dominate node
   */
  public Iterator<T> dominators(final T node) {
    return new Iterator<T>() {
      private T current = node;

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean hasNext() {
        return current != null;
      }

      @Override
      public T next() {
        if (current == null)
          throw new NoSuchElementException();
        T nextNode = current;
        current = getIdom(current);
        return nextNode;
      }
    };
  }

  /**
   * return the dominator tree, which has an edge from n to n' if n dominates n'
   */
  public Graph<T> dominatorTree() {
    return new AbstractGraph<T>() {
      @Override
      protected NodeManager<T> getNodeManager() {
        return G;
      }

      @Override
      protected EdgeManager<T> getEdgeManager() {
        return edges;
      }

      private final EdgeManager<T> edges = new EdgeManager<T>() {
        private final Map<T, Set<T>> nextMap = HashMapFactory.make();

        {
          for (T n : G) {
            if (n != root) {
              T prev = getIdom(n);
              Set<T> next = nextMap.get(prev);
              if (next == null)
                nextMap.put(prev, next = HashSetFactory.make(2));
              next.add(n);
            }
          }
        }

        @Override
        public Iterator<T> getPredNodes(T N) {
          if (N == root)
            return EmptyIterator.instance();
          else
            return new NonNullSingletonIterator<>(getIdom(N));
        }

        @Override
        public int getPredNodeCount(Object N) {
          return (N == root) ? 0 : 1;
        }

        @Override
        public Iterator<T> getSuccNodes(Object N) {
          if (nextMap.containsKey(N))
            return nextMap.get(N).iterator();
          else
            return EmptyIterator.instance();
        }

        @Override
        public int getSuccNodeCount(Object N) {
          if (nextMap.containsKey(N))
            return nextMap.get(N).size();
          else
            return 0;
        }

        @Override
        public void addEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
        }

        @Override
        public void removeEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
        }

        @Override
        public void removeAllIncidentEdges(Object node) {
          Assertions.UNREACHABLE();
        }

        @Override
        public void removeIncomingEdges(Object node) {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();

        }

        @Override
        public void removeOutgoingEdges(Object node) {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();

        }

        @Override
        public boolean hasEdge(Object src, Object dst) {
          // TODO Auto-generated method stub
          Assertions.UNREACHABLE();
          return false;
        }
      };
    };
  }

  //
  // IMPLEMENTATION -- MAIN ALGORITHM
  //

  /**
   * analyze dominators
   */
  protected void analyze() {
    if (DEBUG)
      System.out.println("Dominators for " + G);

    // Step 1: Perform a DFS numbering
    step1();

    // Step 2: the heart of the algorithm
    step2();

    // Step 3: adjust immediate dominators of nodes whose current version of
    // the immediate dominators differs from the nodes with the depth-first
    // number of the node's semidominator.
    step3();

    if (DEBUG)
      System.err.println(this);
  }

  /**
   * The goal of this step is to perform a DFS numbering on the CFG, starting at the root. The exit node is not included.
   */
  private void step1() {
    reachableNodeCount = 0;

    DFSDiscoverTimeIterator<T> dfs = new SlowDFSDiscoverTimeIterator<T>(G, root) {
      public static final long serialVersionUID = 88831771771711L;

      @Override
      protected void visitEdge(T from, T to) {
        if (DEBUG)
          System.out.println("visiting edge " + from + " --> " + to);
        setParent(to, from);
      }
    };

    while (dfs.hasNext()) {
      T node = dfs.next();
      assert node != null;
      vertex[++reachableNodeCount] = node;
      setSemi(node, reachableNodeCount);
      if (DEBUG)
        System.out.println(node + " is DFS number " + reachableNodeCount);
    }
  }

  /**
   * This is the heart of the algorithm. See sources for details.
   */
  private void step2() {
    if (DEBUG) {
      System.out.println(" ******* Beginning STEP 2 *******\n");
    }

    // Visit each node in reverse DFS order, except for the root, which
    // has number 1
    // for i=n downto 2
    for (int i = reachableNodeCount; i > 1; i--) {
      T node = vertex[i];

      if (DEBUG) {
        System.out.println(" Processing: " + node + "\n");
      }

      // visit each predecessor
      Iterator<? extends T> e = G.getPredNodes(node);
      while (e.hasNext()) {
        T prev = e.next();

        if (DEBUG) {
          System.out.println("    Inspecting prev: " + prev);
        }
        T u = EVAL(prev);
        // if semi(u) < semi(node) then semi(node) = semi(u)
        // u may be part of infinite loop and thus, is unreachable from the exit
        // node.
        // In this case, it will have a semi value of 0. Thus, we screen for it
        // here
        if (getSemi(u) != 0 && getSemi(u) < getSemi(node)) {
          setSemi(node, getSemi(u));
        }
      } // while prev

      // add "node" to bucket(vertex(semi(node)));
      addToBucket(vertex[getSemi(node)], node);

      // LINK(parent(node), node)
      LINK(getParent(node), node);

      // foreach node2 in bucket(parent(node)) do
      Iterator<T> bucketEnum = iterateBucket(getParent(node));
      while (bucketEnum.hasNext()) {
        T node2 = bucketEnum.next();

        // u = EVAL(node2)
        T u = EVAL(node2);

        // if semi(u) < semi(node2) then
        // dom(node2) = u
        // else
        // dom(node2) = parent(node)
        if (getSemi(u) < getSemi(node2)) {
          setDominator(node2, u);
        } else {
          setDominator(node2, getParent(node));
        }
      } // while bucket has more elements
    } // for DFSCounter .. 1
  } // method

  /**
   * This method inspects the passed node and returns the following: node, if node is a root of a tree in the forest
   * 
   * any vertex, u != r such that otherwise r is the root of the tree containing node and * semi(u) is minimum on the path r -&gt; v
   * 
   * See TOPLAS 1(1), July 1979, p 128 for details.
   * 
   * @param node the node to evaluate
   * @return the node as described above
   */
  private T EVAL(T node) {
    if (DEBUG) {
      System.out.println("  Evaling " + node);
    }
    if (getAncestor(node) == null) {
      return getLabel(node);
    } else {
      compress(node);
      if (getSemi(getLabel(getAncestor(node))) >= getSemi(getLabel(node))) {
        return getLabel(node);
      } else {
        return getLabel(getAncestor(node));
      }
    }
  }

  /**
   * This recursive method performs the path compression
   * 
   * @param node node of interest
   */
  private void compress(T node) {
    if (getAncestor(getAncestor(node)) != null) {
      compress(getAncestor(node));
      if (getSemi(getLabel(getAncestor(node))) < getSemi(getLabel(node))) {
        setLabel(node, getLabel(getAncestor(node)));
      }
      setAncestor(node, getAncestor(getAncestor(node)));
    }
  }

  /**
   * Adds edge (node1, node2) to the forest maintained as an auxiliary data structure. This implementation uses path compression and
   * results in a O(e * alpha(e,n)) complexity, where e is the number of edges in the CFG and n is the number of nodes.
   * 
   * @param node1 a basic node corresponding to the source of the new edge
   * @param node2 a basic node corresponding to the source of the new edge
   */
  private void LINK(T node1, T node2) {
    if (DEBUG) {
      System.out.println("  Linking " + node1 + " with " + node2);
    }
    T s = node2;
    while (getSemi(getLabel(node2)) < getSemi(getLabel(getChild(s)))) {
      if (getSize(s) + getSize(getChild(getChild(s))) >= 2 * getSize(getChild(s))) {
        setAncestor(getChild(s), s);
        setChild(s, getChild(getChild(s)));
      } else {
        setSize(getChild(s), getSize(s));
        setAncestor(s, getChild(s));
        s = getChild(s);
      }
    }
    setLabel(s, getLabel(node2));
    setSize(node1, getSize(node1) + getSize(node2));
    if (getSize(node1) < 2 * getSize(node2)) {
      T tmp = s;
      s = getChild(node1);
      setChild(node1, tmp);
    }
    while (s != null) {
      setAncestor(s, node1);
      s = getChild(s);
    }
    if (DEBUG) {
      System.out.println("  .... done");
    }
  }

  /**
   * This final step sets the final dominator information.
   */
  private void step3() {
    // Visit each node in DFS order, except for the root, which has number 1
    for (int i = 2; i <= reachableNodeCount; i++) {
      T node = vertex[i];
      // if dom(node) != vertex[semi(node)]
      if (getDominator(node) != vertex[getSemi(node)]) {
        // dom(node) = dom(dom(node))
        setDominator(node, getDominator(getDominator(node)));
      }
    }
  }

  /**
   * LOOK-ASIDE TABLE FOR PER-NODE STATE AND ITS ACCESSORS
   */
  protected final class DominatorInfo {
    /*
     * The result of this computation: the immediate dominator of this node
     */
    private T dominator;

    /*
     * The parent node in the DFS tree used in dominator computation
     */
    private T parent;

    /*
     * the ``semi-dominator,'' which starts as the DFS number in step 1
     */
    private int semiDominator;

    /*
     * The buckets used in step 2
     */
    final private Set<T> bucket;

    /*
     * the labels used in the fast union-find structure
     */
    private T label;

    /*
     * ancestor for fast union-find data structure
     */
    private T ancestor;

    /*
     * the size used by the fast union-find structure
     */
    private int size;

    /*
     * the child used by the fast union-find structure
     */
    private T child;

    DominatorInfo(T node) {
      semiDominator = 0;
      dominator = null;
      parent = null;
      bucket = HashSetFactory.make();
      ancestor = null;
      label = node;
      size = 1;
      child = null;
    }
  }

  /*
   * Look-aside table for DominatorInfo objects
   */
  protected abstract DominatorInfo getInfo(T node);

  private Iterator<T> iterateBucket(T node) {
    return getInfo(node).bucket.iterator();
  }

  private void addToBucket(T node, T addend) {
    getInfo(node).bucket.add(addend);
  }

  private T getDominator(T node) {
    assert node != null;
    return getInfo(node).dominator;
  }

  private void setDominator(T node, T dominator) {
    getInfo(node).dominator = dominator;
  }

  private T getParent(T node) {
    return getInfo(node).parent;
  }

  private void setParent(T node, T parent) {
    getInfo(node).parent = parent;
  }

  private T getAncestor(T node) {
    return getInfo(node).ancestor;
  }

  private void setAncestor(T node, T ancestor) {
    getInfo(node).ancestor = ancestor;
  }

  private T getLabel(T node) {
    if (node == null)
      return null;
    else
      return getInfo(node).label;
  }

  private void setLabel(T node, T label) {
    getInfo(node).label = label;
  }

  private int getSize(T node) {
    if (node == null)
      return 0;
    else
      return getInfo(node).size;
  }

  private void setSize(T node, int size) {
    getInfo(node).size = size;
  }

  private T getChild(T node) {
    return getInfo(node).child;
  }

  private void setChild(T node, T child) {
    getInfo(node).child = child;
  }

  private int getSemi(T node) {
    if (node == null)
      return 0;
    else
      return getInfo(node).semiDominator;
  }

  private void setSemi(T node, int semi) {
    getInfo(node).semiDominator = semi;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (T node : G) {
      sb.append("Dominators of " + node + ":\n");
      for (T dom : Iterator2Iterable.make(dominators(node)))
        sb.append("   " + dom + "\n");
      sb.append("\n");
    }
    return sb.toString();
  }

}
