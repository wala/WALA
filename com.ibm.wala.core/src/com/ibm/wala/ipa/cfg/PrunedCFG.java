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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.util.CompoundIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * A pruned view of a {@link ControlFlowGraph}.   Use this class along with
 * an {@link EdgeFilter} to produce a custom view of a CFG.
 * 
 * For example, you can use this class to produce a CFG view that ignores certain
 * types of exceptional edges.
 */
public class PrunedCFG extends AbstractNumberedGraph<IBasicBlock> implements ControlFlowGraph {

  /**
   * @param cfg the original CFG that you want a view of
   * @param filter an object that selectively filters edges in the original CFG
   * @return a view of cfg that includes only edges accepted by the filter.
   * @throws IllegalArgumentException if cfg is null
   */
  public static PrunedCFG make(final ControlFlowGraph cfg, final EdgeFilter filter) {
    if (cfg == null) {
      throw new IllegalArgumentException("cfg is null");
    }
    return new PrunedCFG(cfg, filter);
  }

  private static class FilteredCFGEdges implements NumberedEdgeManager<IBasicBlock> {
    private final ControlFlowGraph cfg;

    private final NumberedNodeManager<IBasicBlock> currentCFGNodes;

    private final EdgeFilter filter;

    FilteredCFGEdges(ControlFlowGraph cfg, NumberedNodeManager<IBasicBlock> currentCFGNodes, EdgeFilter filter) {
      this.cfg = cfg;
      this.filter = filter;
      this.currentCFGNodes = currentCFGNodes;
    }

    public Iterator<IBasicBlock> getExceptionalSuccessors(final IBasicBlock N) {
      return new FilterIterator<IBasicBlock>(cfg.getExceptionalSuccessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode((IBasicBlock) o) && filter.hasExceptionalEdge(N, (IBasicBlock) o);
        }
      });
    }

    public Iterator<IBasicBlock> getNormalSuccessors(final IBasicBlock N) {
      return new FilterIterator<IBasicBlock>(cfg.getNormalSuccessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode((IBasicBlock) o) && filter.hasNormalEdge(N, (IBasicBlock) o);
        }
      });
    }

    public Iterator<IBasicBlock> getExceptionalPredecessors(final IBasicBlock N) {
      return new FilterIterator<IBasicBlock>(cfg.getExceptionalPredecessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode((IBasicBlock) o) && filter.hasExceptionalEdge((IBasicBlock) o, N);
        }
      });
    }

    public Iterator<IBasicBlock> getNormalPredecessors(final IBasicBlock N) {
      return new FilterIterator<IBasicBlock>(cfg.getNormalPredecessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode((IBasicBlock) o) && filter.hasNormalEdge((IBasicBlock) o, N);
        }
      });
    }

    public Iterator<IBasicBlock> getSuccNodes(IBasicBlock N) {
      return new CompoundIterator<IBasicBlock>(getNormalSuccessors(N), getExceptionalSuccessors(N));
    }

    public int getSuccNodeCount(IBasicBlock N) {
      return Iterator2Collection.toCollection(getSuccNodes(N)).size();
    }

    public IntSet getSuccNodeNumbers(IBasicBlock N) {
      MutableIntSet bits = IntSetUtil.make();
      for (Iterator EE = getSuccNodes(N); EE.hasNext();) {
        bits.add(((IBasicBlock) EE.next()).getNumber());
      }

      return bits;
    }

    public Iterator<IBasicBlock> getPredNodes(IBasicBlock N) {
      return new CompoundIterator<IBasicBlock>(getNormalPredecessors(N), getExceptionalPredecessors(N));
    }

    public int getPredNodeCount(IBasicBlock N) {
      return Iterator2Collection.toCollection(getPredNodes(N)).size();
    }

    public IntSet getPredNodeNumbers(IBasicBlock N) {
      MutableIntSet bits = IntSetUtil.make();
      for (Iterator EE = getPredNodes(N); EE.hasNext();) {
        bits.add(((IBasicBlock) EE.next()).getNumber());
      }

      return bits;
    }

    public boolean hasEdge(IBasicBlock src, IBasicBlock dst) {
      for (Iterator EE = getSuccNodes(src); EE.hasNext();) {
        if (EE.next().equals(dst)) {
          return true;
        }
      }

      return false;
    }

    public void addEdge(IBasicBlock src, IBasicBlock dst) {
      throw new UnsupportedOperationException();
    }

    public void removeEdge(IBasicBlock src, IBasicBlock dst) {
      throw new UnsupportedOperationException();
    }

    public void removeAllIncidentEdges(IBasicBlock node) {
      throw new UnsupportedOperationException();
    }

    public void removeIncomingEdges(IBasicBlock node) {
      throw new UnsupportedOperationException();
    }

    public void removeOutgoingEdges(IBasicBlock node) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FilteredNodes implements NumberedNodeManager<IBasicBlock> {
    private final NumberedNodeManager<IBasicBlock> nodes;

    private final Set subset;

    FilteredNodes(NumberedNodeManager<IBasicBlock> nodes, Set subset) {
      this.nodes = nodes;
      this.subset = subset;
    }

    public int getNumber(IBasicBlock N) {
      if (subset.contains(N))
        return nodes.getNumber(N);
      else
        return -1;
    }

    public IBasicBlock getNode(int number) {
      IBasicBlock N = nodes.getNode(number);
      if (subset.contains(N))
        return N;
      else
        throw new NoSuchElementException();
    }

    public int getMaxNumber() {
      int max = -1;
      for (Iterator<? extends IBasicBlock> NS = nodes.iterator(); NS.hasNext();) {
        IBasicBlock N = NS.next();
        if (subset.contains(N) && getNumber(N) > max) {
          max = getNumber(N);
        }
      }

      return max;
    }

    private Iterator<IBasicBlock> filterNodes(Iterator nodeIterator) {
      return new FilterIterator<IBasicBlock>(nodeIterator, new Filter() {
        public boolean accepts(Object o) {
          return subset.contains(o);
        }
      });
    }

    public Iterator<IBasicBlock> iterateNodes(IntSet s) {
      return filterNodes(nodes.iterateNodes(s));
    }

    public Iterator<IBasicBlock> iterator() {
      return filterNodes(nodes.iterator());
    }

    public int getNumberOfNodes() {
      return subset.size();
    }

    public void addNode(IBasicBlock n) {
      throw new UnsupportedOperationException();
    }

    public void removeNode(IBasicBlock n) {
      throw new UnsupportedOperationException();
    }

    public boolean containsNode(IBasicBlock N) {
      return subset.contains(N);
    }
  }

  private final ControlFlowGraph cfg;

  private final FilteredNodes nodes;

  private final FilteredCFGEdges edges;

  private PrunedCFG(final ControlFlowGraph cfg, final EdgeFilter filter) {
    this.cfg = cfg;
    Graph<IBasicBlock> temp = new AbstractNumberedGraph<IBasicBlock>() {
      private final EdgeManager<IBasicBlock> edges = new FilteredCFGEdges(cfg, cfg, filter);

      @Override
      protected NodeManager<IBasicBlock> getNodeManager() {
        return cfg;
      }

      @Override
      protected EdgeManager<IBasicBlock> getEdgeManager() {
        return edges;
      }
    };

    Set<IBasicBlock> reachable = DFS.getReachableNodes(temp, Collections.singleton(cfg.entry()));
    Set<IBasicBlock> back = DFS.getReachableNodes(GraphInverter.invert(temp), Collections.singleton(cfg.exit()));
    reachable.retainAll(back);

    this.nodes = new FilteredNodes(cfg, reachable);
    this.edges = new FilteredCFGEdges(cfg, nodes, filter);
  }

  @Override
  protected NodeManager<IBasicBlock> getNodeManager() {
    return nodes;
  }

  @Override
  protected EdgeManager<IBasicBlock> getEdgeManager() {
    return edges;
  }

  public Collection<IBasicBlock> getExceptionalSuccessors(final IBasicBlock N) {
    return Iterator2Collection.toCollection(edges.getExceptionalSuccessors(N));
  }

  public Collection<IBasicBlock> getNormalSuccessors(final IBasicBlock N) {
    return Iterator2Collection.toCollection(edges.getNormalSuccessors(N));
  }

  public Collection<IBasicBlock> getExceptionalPredecessors(final IBasicBlock N) {
    return Iterator2Collection.toCollection(edges.getExceptionalPredecessors(N));
  }

  public Collection<IBasicBlock> getNormalPredecessors(final IBasicBlock N) {
    return Iterator2Collection.toCollection(edges.getNormalPredecessors(N));
  }

  public IBasicBlock entry() {
    return cfg.entry();
  }

  public IBasicBlock exit() {
    return cfg.exit();
  }

  public IBasicBlock getBlockForInstruction(int index) {
    return cfg.getBlockForInstruction(index);
  }

  public IInstruction[] getInstructions() {
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

}
