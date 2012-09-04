/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.ipa.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * A pruned view of a {@link ControlFlowGraph}. Use this class along with an {@link EdgeFilter} to produce a custom view of a CFG.
 * 
 * For example, you can use this class to produce a CFG view that ignores certain types of exceptional edges.
 */
public class PrunedCFG<I, T extends IBasicBlock<I>> extends AbstractNumberedGraph<T> implements ControlFlowGraph<I, T> {

  /**
   * @param cfg the original CFG that you want a view of
   * @param filter an object that selectively filters edges in the original CFG
   * @return a view of cfg that includes only edges accepted by the filter.
   * @throws IllegalArgumentException if cfg is null
   */
  public static <I, T extends IBasicBlock<I>> PrunedCFG<I, T> make(final ControlFlowGraph<I, T> cfg, final EdgeFilter<T> filter) {
    if (cfg == null) {
      throw new IllegalArgumentException("cfg is null");
    }
    return new PrunedCFG<I, T>(cfg, filter);
  }

  private static class FilteredCFGEdges<I, T extends IBasicBlock<I>> implements NumberedEdgeManager<T> {
    private final ControlFlowGraph<I, T> cfg;

    private final NumberedNodeManager<T> currentCFGNodes;

    private final EdgeFilter<T> filter;

    FilteredCFGEdges(ControlFlowGraph<I, T> cfg, NumberedNodeManager<T> currentCFGNodes, EdgeFilter<T> filter) {
      this.cfg = cfg;
      this.filter = filter;
      this.currentCFGNodes = currentCFGNodes;
    }

    public Iterator<T> getExceptionalSuccessors(final T N) {
      return new FilterIterator<T>(cfg.getExceptionalSuccessors(N).iterator(), new Filter<T>() {
        public boolean accepts(T o) {
          return currentCFGNodes.containsNode(o) && filter.hasExceptionalEdge(N, o);
        }
      });
    }

    public Iterator<T> getNormalSuccessors(final T N) {
      return new FilterIterator<T>(cfg.getNormalSuccessors(N).iterator(), new Filter<T>() {
        public boolean accepts(T o) {
          return currentCFGNodes.containsNode(o) && filter.hasNormalEdge(N, o);
        }
      });
    }

    public Iterator<T> getExceptionalPredecessors(final T N) {
      return new FilterIterator<T>(cfg.getExceptionalPredecessors(N).iterator(), new Filter<T>() {
        public boolean accepts(T o) {
          return currentCFGNodes.containsNode(o) && filter.hasExceptionalEdge(o, N);
        }
      });
    }

    public Iterator<T> getNormalPredecessors(final T N) {
      return new FilterIterator<T>(cfg.getNormalPredecessors(N).iterator(), new Filter<T>() {
        public boolean accepts(T o) {
          return currentCFGNodes.containsNode(o) && filter.hasNormalEdge(o, N);
        }
      });
    }

    public Iterator<T> getSuccNodes(final T N) {
      return new FilterIterator<T>(cfg.getSuccNodes(N), new Filter<T>() {
        public boolean accepts(T o) {
          return currentCFGNodes.containsNode(o) && (filter.hasNormalEdge(N, o) || filter.hasExceptionalEdge(N, o));
        }
      });
    }

    public int getSuccNodeCount(T N) {
      return Iterator2Collection.toSet(getSuccNodes(N)).size();
    }

    public IntSet getSuccNodeNumbers(T N) {
      MutableIntSet bits = IntSetUtil.make();
      for (Iterator<T> EE = getSuccNodes(N); EE.hasNext();) {
        bits.add(EE.next().getNumber());
      }

      return bits;
    }

    public Iterator<T> getPredNodes(final T N) {
      return new FilterIterator<T>(cfg.getPredNodes(N), new Filter<T>() {
        public boolean accepts(T o) {
          return currentCFGNodes.containsNode(o) && (filter.hasNormalEdge(o, N) || filter.hasExceptionalEdge(o, N));
        }
      });
    }

    public int getPredNodeCount(T N) {
      return Iterator2Collection.toSet(getPredNodes(N)).size();
    }

    public IntSet getPredNodeNumbers(T N) {
      MutableIntSet bits = IntSetUtil.make();
      for (Iterator<T> EE = getPredNodes(N); EE.hasNext();) {
        bits.add(EE.next().getNumber());
      }

      return bits;
    }

    public boolean hasEdge(T src, T dst) {
      for (Iterator EE = getSuccNodes(src); EE.hasNext();) {
        if (EE.next().equals(dst)) {
          return true;
        }
      }

      return false;
    }

    public void addEdge(T src, T dst) {
      throw new UnsupportedOperationException();
    }

    public void removeEdge(T src, T dst) {
      throw new UnsupportedOperationException();
    }

    public void removeAllIncidentEdges(T node) {
      throw new UnsupportedOperationException();
    }

    public void removeIncomingEdges(T node) {
      throw new UnsupportedOperationException();
    }

    public void removeOutgoingEdges(T node) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FilteredNodes<T extends IBasicBlock> implements NumberedNodeManager<T> {
    private final NumberedNodeManager<T> nodes;

    private final Set subset;

    FilteredNodes(NumberedNodeManager<T> nodes, Set subset) {
      this.nodes = nodes;
      this.subset = subset;
    }

    public int getNumber(T N) {
      if (subset.contains(N))
        return nodes.getNumber(N);
      else
        return -1;
    }

    public T getNode(int number) {
      T N = nodes.getNode(number);
      if (subset.contains(N))
        return N;
      else
        throw new NoSuchElementException();
    }

    public int getMaxNumber() {
      int max = -1;
      for (Iterator<? extends T> NS = nodes.iterator(); NS.hasNext();) {
        T N = NS.next();
        if (subset.contains(N) && getNumber(N) > max) {
          max = getNumber(N);
        }
      }

      return max;
    }

    private Iterator<T> filterNodes(Iterator nodeIterator) {
      return new FilterIterator<T>(nodeIterator, new Filter() {
        public boolean accepts(Object o) {
          return subset.contains(o);
        }
      });
    }

    public Iterator<T> iterateNodes(IntSet s) {
      return filterNodes(nodes.iterateNodes(s));
    }

    public Iterator<T> iterator() {
      return filterNodes(nodes.iterator());
    }

    public int getNumberOfNodes() {
      return subset.size();
    }

    public void addNode(T n) {
      throw new UnsupportedOperationException();
    }

    public void removeNode(T n) {
      throw new UnsupportedOperationException();
    }

    public boolean containsNode(T N) {
      return subset.contains(N);
    }
  }

  private final ControlFlowGraph<I, T> cfg;

  private final FilteredNodes<T> nodes;

  private final FilteredCFGEdges<I, T> edges;

  private PrunedCFG(final ControlFlowGraph<I, T> cfg, final EdgeFilter<T> filter) {
    this.cfg = cfg;
    Graph<T> temp = new AbstractNumberedGraph<T>() {
      private final NumberedEdgeManager<T> edges = new FilteredCFGEdges<I, T>(cfg, cfg, filter);

      @Override
      protected NumberedNodeManager<T> getNodeManager() {
        return cfg;
      }

      @Override
      protected NumberedEdgeManager<T> getEdgeManager() {
        return edges;
      }
    };

    Set<T> reachable = DFS.getReachableNodes(temp, Collections.singleton(cfg.entry()));
    Set<T> back = DFS.getReachableNodes(GraphInverter.invert(temp), Collections.singleton(cfg.exit()));
    reachable.retainAll(back);

    this.nodes = new FilteredNodes<T>(cfg, reachable);
    this.edges = new FilteredCFGEdges<I, T>(cfg, nodes, filter);
  }

  @Override
  protected NumberedNodeManager<T> getNodeManager() {
    return nodes;
  }

  @Override
  protected NumberedEdgeManager<T> getEdgeManager() {
    return edges;
  }

  public List<T> getExceptionalSuccessors(final T N) {
    ArrayList<T> result = new ArrayList<T>();
    for (Iterator<T> it = edges.getExceptionalSuccessors(N); it.hasNext();) {
      result.add(it.next());
    }
    return result;
  }

  public Collection<T> getNormalSuccessors(final T N) {
    return Iterator2Collection.toSet(edges.getNormalSuccessors(N));
  }

  public Collection<T> getExceptionalPredecessors(final T N) {
    return Iterator2Collection.toSet(edges.getExceptionalPredecessors(N));
  }

  public Collection<T> getNormalPredecessors(final T N) {
    return Iterator2Collection.toSet(edges.getNormalPredecessors(N));
  }

  public T entry() {
    return cfg.entry();
  }

  public T exit() {
    return cfg.exit();
  }

  public T getBlockForInstruction(int index) {
    return cfg.getBlockForInstruction(index);
  }

  public I[] getInstructions() {
    return cfg.getInstructions();
  }

  public int getProgramCounter(int index) {
    return cfg.getProgramCounter(index);
  }

  public IMethod getMethod() {
    return cfg.getMethod();
  }

  public BitVector getCatchBlocks() {
    BitVector result = new BitVector();
    BitVector blocks = cfg.getCatchBlocks();
    int i = 0;
    while ((i = blocks.nextSetBit(i)) != -1) {
      if (nodes.containsNode(getNode(i))) {
        result.set(i);
      }
    }

    return result;
  }

  public IntSet getPhiIndices(T bb) {
    assert containsNode(bb);
    assert cfg.containsNode(bb);

    int i = 0;
    MutableIntSet valid = IntSetUtil.make();
    for (Iterator<? extends T> pbs = cfg.getPredNodes(bb); pbs.hasNext(); i++) {
      if (nodes.containsNode(pbs.next())) {
        valid.add(i);
      }
    }

    return valid;
  }

}
