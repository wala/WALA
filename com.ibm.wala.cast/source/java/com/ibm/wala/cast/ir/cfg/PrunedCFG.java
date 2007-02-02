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
package com.ibm.wala.cast.ir.cfg;

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

public class PrunedCFG extends AbstractNumberedGraph<IBasicBlock> implements ControlFlowGraph {

  public interface EdgeFilter {

    boolean hasNormalEdge(IBasicBlock src, IBasicBlock dst);

    boolean hasExceptionalEdge(IBasicBlock src, IBasicBlock dst);

  }

  private static class FilteredCFGEdges implements NumberedEdgeManager {
    private final ControlFlowGraph cfg;

    private final NumberedNodeManager currentCFGNodes;

    private final EdgeFilter filter;

    FilteredCFGEdges(ControlFlowGraph cfg, NumberedNodeManager currentCFGNodes, EdgeFilter filter) {
      this.cfg = cfg;
      this.filter = filter;
      this.currentCFGNodes = currentCFGNodes;
    }

    public Iterator getExceptionalSuccessors(final IBasicBlock N) {
      return new FilterIterator(cfg.getExceptionalSuccessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode(o) && filter.hasExceptionalEdge((IBasicBlock) N, (IBasicBlock) o);
        }
      });
    }

    public Iterator getNormalSuccessors(final IBasicBlock N) {
      return new FilterIterator(cfg.getNormalSuccessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode(o) && filter.hasNormalEdge((IBasicBlock) N, (IBasicBlock) o);
        }
      });
    }

    public Iterator getExceptionalPredecessors(final IBasicBlock N) {
      return new FilterIterator(cfg.getExceptionalPredecessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode(o) && filter.hasExceptionalEdge((IBasicBlock) o, (IBasicBlock) N);
        }
      });
    }

    public Iterator getNormalPredecessors(final IBasicBlock N) {
      return new FilterIterator(cfg.getNormalPredecessors(N).iterator(), new Filter() {
        public boolean accepts(Object o) {
          return currentCFGNodes.containsNode(o) && filter.hasNormalEdge((IBasicBlock) o, (IBasicBlock) N);
        }
      });
    }

    public Iterator getSuccNodes(Object N) {
      return new CompoundIterator(getNormalSuccessors((IBasicBlock) N), getExceptionalSuccessors((IBasicBlock) N));
    }

    public int getSuccNodeCount(Object N) {
      return new Iterator2Collection(getSuccNodes(N)).size();
    }

    public IntSet getSuccNodeNumbers(Object N) {
      MutableIntSet bits = IntSetUtil.make();
      for (Iterator EE = getSuccNodes(N); EE.hasNext();) {
        bits.add(((IBasicBlock) EE.next()).getNumber());
      }

      return bits;
    }

    public Iterator getPredNodes(Object N) {
      return new CompoundIterator(getNormalPredecessors((IBasicBlock) N), getExceptionalPredecessors((IBasicBlock) N));
    }

    public int getPredNodeCount(Object N) {
      return new Iterator2Collection(getPredNodes(N)).size();
    }

    public IntSet getPredNodeNumbers(Object N) {
      MutableIntSet bits = IntSetUtil.make();
      for (Iterator EE = getPredNodes(N); EE.hasNext();) {
        bits.add(((IBasicBlock) EE.next()).getNumber());
      }

      return bits;
    }

    public boolean hasEdge(Object src, Object dst) {
      for (Iterator EE = getSuccNodes(src); EE.hasNext();) {
        if (EE.next().equals(dst)) {
          return true;
        }
      }

      return false;
    }

    public void addEdge(Object src, Object dst) {
      throw new UnsupportedOperationException();
    }

    public void removeEdge(Object src, Object dst) {
      throw new UnsupportedOperationException();
    }

    public void removeAllIncidentEdges(Object node) {
      throw new UnsupportedOperationException();
    }

    public void removeIncomingEdges(Object node) {
      throw new UnsupportedOperationException();
    }

    public void removeOutgoingEdges(Object node) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FilteredNodes implements NumberedNodeManager {
    private final NumberedNodeManager nodes;

    private final Set subset;

    FilteredNodes(NumberedNodeManager nodes, Set subset) {
      this.nodes = nodes;
      this.subset = subset;
    }

    public int getNumber(Object N) {
      if (subset.contains(N))
        return nodes.getNumber(N);
      else
        return -1;
    }

    public Object getNode(int number) {
      Object N = nodes.getNode(number);
      if (subset.contains(N))
        return N;
      else
        throw new NoSuchElementException();
    }

    public int getMaxNumber() {
      int max = -1;
      for (Iterator NS = nodes.iterateNodes(); NS.hasNext();) {
        Object N = NS.next();
        if (subset.contains(N) && getNumber(N) > max) {
          max = getNumber(N);
        }
      }

      return max;
    }

    private Iterator filterNodes(Iterator nodeIterator) {
      return new FilterIterator(nodeIterator, new Filter() {
        public boolean accepts(Object o) {
          return subset.contains(o);
        }
      });
    }

    public Iterator iterateNodes(IntSet s) {
      return filterNodes(nodes.iterateNodes(s));
    }

    public Iterator iterateNodes() {
      return filterNodes(nodes.iterateNodes());
    }

    public int getNumberOfNodes() {
      return subset.size();
    }

    public void addNode(Object n) {
      throw new UnsupportedOperationException();
    }

    public void removeNode(Object n) {
      throw new UnsupportedOperationException();
    }

    public boolean containsNode(Object N) {
      return subset.contains(N);
    }
  }

  private final ControlFlowGraph cfg;

  private final FilteredNodes nodes;

  private final FilteredCFGEdges edges;

  public PrunedCFG(final ControlFlowGraph cfg, final EdgeFilter filter) {
    this.cfg = cfg;
    Graph<IBasicBlock> temp = new AbstractNumberedGraph<IBasicBlock>() {
      private final EdgeManager edges = new FilteredCFGEdges(cfg, cfg, filter);

      protected NodeManager getNodeManager() {
        return cfg;
      }

      protected EdgeManager getEdgeManager() {
        return edges;
      }
    };

    Set reachable = DFS.getReachableNodes(temp, Collections.singleton(cfg.entry()));
    Set back = DFS.getReachableNodes(GraphInverter.invert(temp), Collections.singleton(cfg.exit()));
    reachable.retainAll(back);

    this.nodes = new FilteredNodes(cfg, reachable);
    this.edges = new FilteredCFGEdges(cfg, nodes, filter);
  }

  protected NodeManager getNodeManager() {
    return nodes;
  }

  protected EdgeManager getEdgeManager() {
    return edges;
  }

  public Collection getExceptionalSuccessors(final IBasicBlock N) {
    return new Iterator2Collection(edges.getExceptionalSuccessors(N));
  }

  public Collection getNormalSuccessors(final IBasicBlock N) {
    return new Iterator2Collection(edges.getNormalSuccessors(N));
  }

  public Collection getExceptionalPredecessors(final IBasicBlock N) {
    return new Iterator2Collection(edges.getExceptionalPredecessors(N));
  }

  public Collection getNormalPredecessors(final IBasicBlock N) {
    return new Iterator2Collection(edges.getNormalPredecessors(N));
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
