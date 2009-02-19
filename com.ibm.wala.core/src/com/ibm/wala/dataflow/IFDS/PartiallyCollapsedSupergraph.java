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
package com.ibm.wala.dataflow.IFDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Filtersection;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.IndiscriminateFilter;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.intset.BimodalMutableIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * 
 * A Supergraph customized for the case when some nodes are "collapsible",
 * meaning the result for every basic block in a collapsible node is identical.
 * This graph is an InterproceduralCFG for uncollapsible nodes, hooked up to
 * collapsed nodes.
 * 
 * This class is deprecated.  It's quite a kludge, and likely not worth the constant-time
 * speedup for most applications.  I don't have any tests exercising this right now, so
 * it may be rotting.   Clients that use this should probably migrate to a simpler
 * supergraph.
 * 
 * @author sfink
 */
@Deprecated
public class PartiallyCollapsedSupergraph extends AbstractGraph<Object> implements ISupergraph<Object, CGNode> {

  /**
   * DEBUG_LEVEL:
   * <ul>
   * <li>0 No output
   * <li>1 Print some simple stats and warning information
   * <li>2 Detailed debugging
   * </ul>
   */
  static final int DEBUG_LEVEL = 0;

  private final NodeManager nodeManager;

  private final EdgeManager edgeManager;

  /**
   * governing call graph
   */
  private final CallGraph cg;

  /**
   * partially built interprocedural control flow graph
   */
  private final InterproceduralCFG partialIPFG;

  /**
   * set of nodes which cannot be collapsed
   */
  private final Collection<CGNode> noCollapse;

  private final Filter isEntry = new Filter() {
    public boolean accepts(Object o) {
      return isEntry(o);
    }
  };
  
  public Graph<CGNode> getProcedureGraph() {
    return cg;
  }

  /**
   * @param cg
   *            Governing call graph
   * @param noCollapse
   *            set of nodes in the call graph which cannot be collapsed
   */
  public PartiallyCollapsedSupergraph(CallGraph cg, Collection<CGNode> noCollapse) {
    this(cg, noCollapse, IndiscriminateFilter.<CGNode>singleton());
  }

  /**
   * @param cg
   *            Governing call graph
   * @param noCollapse
   *            set of nodes in the call graph which cannot be collapsed
   * @param relevant
   *            set of nodes which are relevant and should be included in the
   *            supergraph
   */
  public PartiallyCollapsedSupergraph(CallGraph cg, Collection<CGNode> noCollapse, Filter<CGNode> relevant) {

    this.cg = cg;
    if (DEBUG_LEVEL > 0) {
      Trace.println("Call graph \n" + cg.toString());
      try {
        GraphIntegrity.check(cg);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        Assertions._assert(false, "call graph failed graph integrity");
      }
    }
    this.noCollapse = noCollapse;
    this.partialIPFG = new InterproceduralCFG(cg, new Filtersection<CGNode>(relevant, new CollectionFilter<CGNode>(noCollapse)));
    if (DEBUG_LEVEL > 0) {
      Trace.println("IPFG \n" + partialIPFG.toString());
    }
    this.nodeManager = new NodeManager();
    this.edgeManager = new EdgeManager();
  }

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getNodeManager()
   */
  @Override
  protected com.ibm.wala.util.graph.NodeManager<Object> getNodeManager() {
    return nodeManager;
  }

  /*
   * @see com.ibm.wala.util.graph.AbstractGraph#getEdgeManager()
   */
  @Override
  protected com.ibm.wala.util.graph.EdgeManager<Object> getEdgeManager() {
    return edgeManager;
  }

  /*
   * @see com.ibm.wala.j2ee.transactions.ISupergraph#getFakeRootNode()
   */
  public CGNode getMain() {
    return cg.getFakeRootNode();
  }

  public Object getEntryForProcedure(CGNode n) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(n != null);
    }
    if (noCollapse.contains(n)) {
      // p is cg node which is expanded in the IPFG
      return partialIPFG.getEntry(n);
    } else {
      // p is a collapsed node, for which all blocks map to the node itself;
      return nodeManager.getCollapsedEntry(n);
    }
  }

  public Object[] getEntries(Object n) {
    CGNode p = getProcOf(n);
    return new Object[] { getEntryForProcedure(p) };
  }

  public Object[] getExitsForProcedure(CGNode node) {
    if (noCollapse.contains(node)) {
      ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = partialIPFG.getCFG(node);
      return new Object[] { new BasicBlockInContext<ISSABasicBlock>(node, cfg.exit()) };
    } else {
      return new Object[] { nodeManager.getCollapsedExit(node) };
    }
  }

  @SuppressWarnings("unchecked")
  public boolean isCall(Object object) throws IllegalArgumentException {
    if (object == null) {
      throw new IllegalArgumentException("object == null");
    }
    if (object instanceof BasicBlockInContext) {
      return partialIPFG.hasCall((BasicBlockInContext<ISSABasicBlock>) object);
    } else {
      if (Assertions.verifyAssertions) {
        if (!(object instanceof CollapsedNode)) {
          Assertions._assert(false, object.getClass().toString());
        }
      }
      if (nodeManager.isCollapsedEntry(object)) {
        CGNode n = nodeManager.getProcOfCollapsedNode(object);
        return cg.getSuccNodeCount(n) > 0;
      } else {
        return false;
      }
    }
  }

  public boolean isEntry(Object object) {
    if (object instanceof IBasicBlock) {
      IBasicBlock b = (IBasicBlock) object;
      return b.isEntryBlock();
    } else {
      return nodeManager.isCollapsedEntry(object);
    }
  }

  public boolean isExit(Object object) {
    if (object instanceof IBasicBlock) {
      IBasicBlock b = (IBasicBlock) object;
      return b.isExitBlock();
    } else {
      return nodeManager.isCollapsedExit(object);
    }
  }

  public Iterator<Object> getCalledNodes(Object n) {
    return new FilterIterator<Object>(edgeManager.getSuccNodes(n), isEntry);
  }

  /* 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getReturnSites(java.lang.Object, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public Iterator<? extends Object> getReturnSites(Object object, CGNode callee) {
    if (object instanceof BasicBlockInContext) {
      return partialIPFG.getReturnSites((BasicBlockInContext) object);
    } else {
      CGNode n = nodeManager.getProcOfCollapsedNode(object);
      return new NonNullSingletonIterator<CollapsedNode>(nodeManager.getCollapsedExit(n));
    }
  }

  /* 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCallSites(java.lang.Object, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public Iterator<? extends Object> getCallSites(Object object, CGNode callee) {
    if (object instanceof BasicBlockInContext) {
      return partialIPFG.getCallSites((BasicBlockInContext<ISSABasicBlock>) object, callee);
    } else {
      CGNode n = nodeManager.getProcOfCollapsedNode(object);
      return new NonNullSingletonIterator<CollapsedNode>(nodeManager.getCollapsedEntry(n));
    }
  }

  public CGNode getProcOf(Object n) throws IllegalArgumentException {
    if (!(n instanceof com.ibm.wala.ipa.cfg.BasicBlockInContext) && n instanceof com.ibm.wala.cfg.IBasicBlock) {
      throw new IllegalArgumentException(
        "(n instanceof com.ibm.wala.cfg.IBasicBlock) and (not ( n instanceof com.ibm.wala.ipa.cfg.BasicBlockInContext ) ): " + n + ", " + n.getClass());
    }
    if (n instanceof BasicBlockInContext) {
      return partialIPFG.getCGNode((BasicBlockInContext) n);
    } else {
      return nodeManager.getProcOfCollapsedNode(n);
    }
  }

  private class EdgeManager implements NumberedEdgeManager<Object> {

    /**
     * A transverse edge is an edge from an uncollapsed node to a collapsed
     * node, or vice versa. We track these explicitly. This is a map from
     * collapsed node entry -> Set of predecessor basic blocks
     */
    private final Map<Object, Set<Object>> incomingTransverseEdges = HashMapFactory.make();

    /**
     * This is a map from basic block -> set of collapse node entry
     */
    private final Map<Object, Set<Object>> outgoingTransverseEdges = HashMapFactory.make();

    EdgeManager() {
      computeTransverseEdges();
    }

    /**
     * This could be done more efficiently.
     */
    private void computeTransverseEdges() {
      // compute transverse edges that originate from basic blocks
      for (BasicBlockInContext<ISSABasicBlock> bb : partialIPFG) {
        if (partialIPFG.hasCall(bb)) {
          Set targets = partialIPFG.getCallTargets(bb);
          for (Iterator it2 = targets.iterator(); it2.hasNext();) {
            CGNode n = (CGNode) it2.next();
            if (!noCollapse.contains(n)) {
              // add an edge from bb -> n
              Object s_n = nodeManager.getCollapsedEntry(n);
              Set<Object> incoming = MapUtil.findOrCreateSet(incomingTransverseEdges, s_n);
              incoming.add(bb);
              Set<Object> outgoing = MapUtil.findOrCreateSet(outgoingTransverseEdges, bb);
              outgoing.add(s_n);

              // add an edge from n_exit -> return sites
              Object e_n = nodeManager.getCollapsedExit(n);
              for (Iterator returnSites = getReturnSites(bb, n); returnSites.hasNext();) {
                Object ret = returnSites.next();
                Set<Object> in = MapUtil.findOrCreateSet(incomingTransverseEdges, ret);
                in.add(e_n);
                Set<Object> out = MapUtil.findOrCreateSet(outgoingTransverseEdges, e_n);
                out.add(ret);
              }
            }
          }
        }
      }
      // compute transverse edges that originate from collapsed nodes;
      // this happens for the fake root method and other dark methods.
      for (Iterator it = nodeManager.iterateCollapsedNodes(); it.hasNext();) {
        Object n = it.next();
        if (!nodeManager.isCollapsedEntry(n)) {
          continue;
        }
        CGNode node = nodeManager.getProcOfCollapsedNode(n);
        for (Iterator it2 = cg.getSuccNodes(node); it2.hasNext();) {
          CGNode outNode = (CGNode) it2.next();
          if (noCollapse.contains(outNode)) {
            ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = partialIPFG.getCFG(outNode);
            // add an edge to the entry block
            BasicBlockInContext<ISSABasicBlock> entry = new BasicBlockInContext<ISSABasicBlock>(outNode, cfg.entry());
            Set<Object> incoming = MapUtil.findOrCreateSet(incomingTransverseEdges, entry);
            incoming.add(n);
            Set<Object> outgoing = MapUtil.findOrCreateSet(outgoingTransverseEdges, n);
            outgoing.add(entry);

            // add the edge representing the return from the call.
            BasicBlockInContext<ISSABasicBlock> exit = new BasicBlockInContext<ISSABasicBlock>(outNode, cfg.exit());
            // IBasicBlock exit = cfg.exit();
            Object retSite = nodeManager.getCollapsedExit(node);
            incoming = MapUtil.findOrCreateSet(incomingTransverseEdges, retSite);
            incoming.add(exit);
            outgoing = MapUtil.findOrCreateSet(outgoingTransverseEdges, exit);
            outgoing.add(retSite);
          }
        }
      }
    }

    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append("Transverse Edges:\n");
      for (Iterator it = incomingTransverseEdges.entrySet().iterator(); it.hasNext();) {
        Map.Entry e = (Map.Entry) it.next();
        Object entryNode = e.getKey();
        Set incoming = (Set) e.getValue();
        for (Iterator it2 = incoming.iterator(); it2.hasNext();) {
          result.append(it2.next()).append("->").append(entryNode).append("\n");
        }
      }
      result.append("Partial IPFG:\n");
      result.append(partialIPFG);
      return result.toString();
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Iterator<? extends Object> getPredNodes(Object N) {
      if (N instanceof IBasicBlock) {
        Set incoming = incomingTransverseEdges.get(N);
        if (incoming == null) {
          return partialIPFG.getPredNodes((BasicBlockInContext) N);
        } else {
          return new CompoundIterator<Object>(partialIPFG.getPredNodes((BasicBlockInContext) N), incoming.iterator());
        }
      } else {
        if (isEntry(N)) {
          Set<Object> result = HashSetFactory.make(4);
          CGNode n = nodeManager.getProcOfCollapsedNode(N);
          for (Iterator it = cg.getPredNodes(n); it.hasNext();) {
            CGNode p = (CGNode) it.next();
            if (!noCollapse.contains(p)) {
              result.add(nodeManager.getCollapsedEntry(p));
            }
          }
          Set<Object> xverse = incomingTransverseEdges.get(N);
          if (xverse != null) {
            result.addAll(xverse);
          }
          return result.iterator();
        } else {
          // N is a collapsed exit
          Set<Object> result = HashSetFactory.make(4);
          CGNode n = nodeManager.getProcOfCollapsedNode(N);
          for (Iterator it = cg.getSuccNodes(n); it.hasNext();) {
            CGNode s = (CGNode) it.next();
            if (!noCollapse.contains(s)) {
              result.add(nodeManager.getCollapsedExit(s));
            }
          }
          result.add(nodeManager.getCollapsedEntry(n));
          Set<Object> xverse = incomingTransverseEdges.get(N);
          if (xverse != null) {
            result.addAll(xverse);
          }
          return result.iterator();
        }
      }
    }

    @SuppressWarnings("unchecked")
    public IntSet getPredNodeNumbers(Object node) {
      if (node instanceof IBasicBlock) {
        Set incoming = incomingTransverseEdges.get(node);
        if (incoming == null) {
          return partialIPFG.getPredNodeNumbers((BasicBlockInContext) node);
        } else {
          IntSet pred = partialIPFG.getPredNodeNumbers((BasicBlockInContext) node);
          MutableSparseIntSet result = pred == null ? MutableSparseIntSet.makeEmpty() : MutableSparseIntSet.make(pred);
          for (Iterator it = incoming.iterator(); it.hasNext();) {
            result.add(getNumber(it.next()));
          }
          return result;
        }
      } else {
        if (isEntry(node)) {
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          CGNode n = nodeManager.getProcOfCollapsedNode(node);
          for (Iterator it = cg.getPredNodes(n); it.hasNext();) {
            CGNode p = (CGNode) it.next();
            if (!noCollapse.contains(p)) {
              result.add(nodeManager.getCollapsedEntry(p).number);
            }
          }
          Set xverse = incomingTransverseEdges.get(node);
          if (xverse != null) {
            for (Iterator it = xverse.iterator(); it.hasNext();) {
              result.add(getNumber(it.next()));
            }
          }
          return result;
        } else {
          // node is a collapsed exit
          MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
          CGNode n = nodeManager.getProcOfCollapsedNode(node);
          for (Iterator it = cg.getSuccNodes(n); it.hasNext();) {
            CGNode s = (CGNode) it.next();
            if (!noCollapse.contains(s)) {
              result.add(nodeManager.getCollapsedExit(s).number);
            }
          }
          result.add(nodeManager.getCollapsedEntry(n).number);
          Set xverse = incomingTransverseEdges.get(node);
          if (xverse != null) {
            for (Iterator it = xverse.iterator(); it.hasNext();) {
              result.add(getNumber(it.next()));
            }
          }
          return result;
        }
      }
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
     */
    public int getPredNodeCount(Object N) {
      Collection c = Iterator2Collection.toCollection(getPredNodes(N));
      return c.size();
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Iterator<? extends Object> getSuccNodes(Object N) {
      if (N instanceof IBasicBlock) {
        Set<Object> xverse = outgoingTransverseEdges.get(N);
        if (xverse == null) {
          return partialIPFG.getSuccNodes((BasicBlockInContext) N);
        } else {
          return new CompoundIterator<Object>(partialIPFG.getSuccNodes((BasicBlockInContext) N), xverse.iterator());
        }
      } else {
        if (isEntry(N)) {
          Set<Object> result = HashSetFactory.make(4);
          CGNode n = nodeManager.getProcOfCollapsedNode(N);
          for (Iterator it = cg.getSuccNodes(n); it.hasNext();) {
            CGNode s = (CGNode) it.next();
            if (!noCollapse.contains(s)) {
              result.add(nodeManager.getCollapsedEntry(s));
            }
          }
          result.add(nodeManager.getCollapsedExit(n));
          Set<Object> xverse = outgoingTransverseEdges.get(N);
          if (xverse != null) {
            result.addAll(xverse);
          }
          return result.iterator();
        } else {
          // N is a collapsed exit
          CGNode n = nodeManager.getProcOfCollapsedNode(N);
          Object entry = nodeManager.getCollapsedEntry(n);
          HashSet<Object> result = HashSetFactory.make(4);
          for (Iterator it = getPredNodes(entry); it.hasNext();) {
            Object callSite = it.next();
            for (Iterator returnSites = getReturnSites(callSite, n); returnSites.hasNext();) {
              result.add(returnSites.next());
            }
          }
          return result.iterator();
        }
      }
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public IntSet getSuccNodeNumbers(Object N) {
      if (N instanceof IBasicBlock) {
        Set xverse = outgoingTransverseEdges.get(N);
        if (xverse == null) {
          return partialIPFG.getSuccNodeNumbers((BasicBlockInContext) N);
        } else {
          IntSet succ = partialIPFG.getSuccNodeNumbers((BasicBlockInContext) N);
          MutableSparseIntSet result = succ == null ? MutableSparseIntSet.makeEmpty() : MutableSparseIntSet.make(succ);
          for (Iterator it = xverse.iterator(); it.hasNext();) {
            result.add(getNumber(it.next()));
          }
          return result;
        }
      } else {
        if (isEntry(N)) {
          BimodalMutableIntSet result = new BimodalMutableIntSet();
          CGNode n = nodeManager.getProcOfCollapsedNode(N);
          for (Iterator it = cg.getSuccNodes(n); it.hasNext();) {
            CGNode s = (CGNode) it.next();
            if (!noCollapse.contains(s)) {
              result.add(nodeManager.getCollapsedEntry(s).number);
            }
          }
          result.add(nodeManager.getCollapsedExit(n).number);
          Set xverse = outgoingTransverseEdges.get(N);
          if (xverse != null) {
            for (Iterator it = xverse.iterator(); it.hasNext();) {
              result.add(getNumber(it.next()));
            }
          }
          return result;
        } else {
          // N is a collapsed exit
          CGNode n = nodeManager.getProcOfCollapsedNode(N);
          Object entry = nodeManager.getCollapsedEntry(n);
          BimodalMutableIntSet result = new BimodalMutableIntSet();
          for (Iterator it = getPredNodes(entry); it.hasNext();) {
            Object callSite = it.next();
            for (Iterator returnSites = getReturnSites(callSite, n); returnSites.hasNext();) {
              result.add(getNumber(returnSites.next()));
            }
          }
          return result;
        }
      }
    }

    /*
     * This can be really slow: this will need tuning.
     * 
     */
    @SuppressWarnings("unchecked")
    public boolean hasEdge(Object src, Object dst) {
      if (src instanceof IBasicBlock) {
        if (dst instanceof IBasicBlock) {
          return partialIPFG.hasEdge((BasicBlockInContext) src, (BasicBlockInContext) dst);
        } else {
          // TODO: optimize
          return getSuccNodeNumbers(src).contains(getNumber(dst));
        }
      } else {
        // TODO: optimize
        return getSuccNodeNumbers(src).contains(getNumber(dst));
      }
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
     */

    public int getSuccNodeCount(Object N) {
      Collection c = Iterator2Collection.toCollection(getSuccNodes(N));
      return c.size();
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object,
     *      java.lang.Object)
     */
    public void addEdge(Object src, Object dst) {
      Assertions.UNREACHABLE();
    }

    public void removeEdge(Object src, Object dst) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(java.lang.Object)
     */
    public void removeAllIncidentEdges(Object node) {
      Assertions.UNREACHABLE();

    }

    public void removeIncomingEdges(Object node) {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();

    }

    public void removeOutgoingEdges(Object node) {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
    }

  }

  /**
   * @author sfink
   * 
   */
  private class NodeManager implements NumberedNodeManager<Object> {

    /**
     * Map: CGNode -> Integer ... the index into collapsed nodes for the
     * collapsed entry of a CGNode. Note that if i is the index for the
     * collapsed entry, then i+1 is the index for the collapsed exit
     */
    private final Map<CGNode, Integer> node2EntryIndex = HashMapFactory.make();

    private final ArrayList<CollapsedNode> collapsedNodes = new ArrayList<CollapsedNode>();

    /**
     * create all the collapsed nodes
     */
    NodeManager() {
      int firstNumber = partialIPFG.getMaxNumber() + 1;
      int nextNumber = firstNumber;
      for (Iterator it = cg.iterator(); it.hasNext();) {
        CGNode n = (CGNode) it.next();
        if (!noCollapse.contains(n)) {
          node2EntryIndex.put(n, new Integer(nextNumber - firstNumber));
          collapsedNodes.add(new CollapsedNode(n, true, nextNumber++));
          collapsedNodes.add(new CollapsedNode(n, false, nextNumber++));
        }
      }
    }

    public boolean isCollapsedEntry(Object object) {
      CollapsedNode n = (CollapsedNode) object;
      return n.isEntry;
    }

    public boolean isCollapsedExit(Object object) {
      CollapsedNode n = (CollapsedNode) object;
      return !n.isEntry;
    }

    public CGNode getProcOfCollapsedNode(Object object) {
      CollapsedNode n = (CollapsedNode) object;
      return n.node;
    }

    /**
     * TODO: refactor to avoid allocation?
     * 
     * @param n
     *            a collapsible node
     * @return an object that represents entry to this node
     */
    public CollapsedNode getCollapsedEntry(CGNode n) {
      Integer index = node2EntryIndex.get(n);
      if (Assertions.verifyAssertions && index == null) {
        Assertions.UNREACHABLE("null index for " + n);
      }
      return collapsedNodes.get(index.intValue());
    }

    /**
     * TODO: refactor to avoid allocation?
     * 
     * @param n
     *            a collapsible node
     * @return an object that represents entry to this node
     */
    public CollapsedNode getCollapsedExit(CGNode n) {
      Integer index = node2EntryIndex.get(n);
      return collapsedNodes.get(index.intValue() + 1);
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
     */
    public Iterator<Object> iterator() {
      return new CompoundIterator<Object>(partialIPFG.iterator(), collapsedNodes.iterator());
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
     */
    public int getNumberOfNodes() {
      return partialIPFG.getNumberOfNodes() + collapsedNodes.size();
    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
     */
    public void addNode(Object n) {
      Assertions.UNREACHABLE();

    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
     */
    public void removeNode(Object n) {
      Assertions.UNREACHABLE();

    }

    /*
     * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public boolean containsNode(Object N) {
      if (N instanceof BasicBlockInContext) {
        return partialIPFG.containsNode((BasicBlockInContext) N);
      } else {
        return collapsedNodes.contains(N);
      }
    }

    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append("Uncollapsed nodes:\n");
      for (Iterator it = iterateUncollapsedNodes(); it.hasNext();) {
        result.append(it.next()).append("\n");
      }
      result.append("Collapsed nodes:\n");
      for (Iterator it = iterateCollapsedNodes(); it.hasNext();) {
        result.append(it.next()).append("\n");
      }
      return result.toString();
    }

    /**
     * 
     */
    private Iterator iterateCollapsedNodes() {
      return collapsedNodes.iterator();
    }

    private Iterator iterateUncollapsedNodes() {
      return partialIPFG.iterator();
    }

    @SuppressWarnings("unchecked")
    public int getNumber(Object N) throws IllegalArgumentException {
      if (!(N instanceof BasicBlockInContext) && !(N instanceof CollapsedNode)) {
        throw new IllegalArgumentException(
            "(not ( N instanceof com.ibm.wala.ipa.cfg.BasicBlockInContext ) ) and (not ( N instanceof com.ibm.wala.dataflow.IFDS.PartiallyCollapsedSupergraph$CollapsedNode ) )");
      }
      if (N instanceof CollapsedNode) {
        return ((CollapsedNode) N).number;
      } else {
        return partialIPFG.getNumber((BasicBlockInContext) N);
      }
    }

    public Object getNode(int number) {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
      return null;
    }

    public int getMaxNumber() {
      return partialIPFG.getMaxNumber() + collapsedNodes.size();
    }

    public Iterator<Object> iterateNodes(IntSet s) {
      // TODO Auto-generated method stub
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getEntriesForProcedure(java.lang.Object)
   */
  public Object[] getEntriesForProcedure(CGNode object) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(object != null);
    }
    return new Object[] { getEntryForProcedure(object) };
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMainEntry()
   */
  public Object getMainEntry() {
    return getEntryForProcedure(getMain());
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMainExit()
   */
  public Object getMainExit() {
    CGNode n = getMain();
    if (noCollapse.contains(n)) {
      // p is cg node which is expanded in the IPFG
      ControlFlowGraph cfg = partialIPFG.getCFG(n);
      return cfg.exit();
    } else {
      // p is a collapsed node, for which all blocks map to the node itself;
      return nodeManager.getCollapsedExit(n);
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#isReturn(java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public boolean isReturn(Object object) {
    if (object instanceof BasicBlockInContext) {
      return partialIPFG.isReturn((BasicBlockInContext) object);
    } else {
      if (nodeManager.isCollapsedExit(object)) {
        CGNode node = getProcOf(object);
        return cg.getSuccNodeCount(node) > 0;
      } else {
        return false;
      }
    }
  }

  /**
   * @return the portion of this graph that is a normal, uncollapsed, ICFG
   */
  public InterproceduralCFG getUncollapsedGraph() {
    return partialIPFG;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#classifyEdge(java.lang.Object,
   *      java.lang.Object)
   */
  public byte classifyEdge(Object src, Object dest) throws IllegalArgumentException {

    if (src == null) {
      throw new IllegalArgumentException("src == null");
    }
    if (isCall(src)) {
      if (isEntry(dest)) {
        return CALL_EDGE;
      } else {
        return CALL_TO_RETURN_EDGE;
      }
    } else if (isExit(src)) {
      return RETURN_EDGE;
    } else {
      return OTHER;
    }
  }

  /**
   * In forward problems, a call node will have no normal successors.
   * 
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getNormalSuccessors(java.lang.Object)
   */
  public Iterator<Object> getNormalSuccessors(Object call) {
    return EmptyIterator.instance();
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getNumberOfBlocks(java.lang.Object)
   */
  public int getNumberOfBlocks(CGNode procedure) {
    CGNode n = procedure;
    if (noCollapse.contains(n)) {
      // p is cg node which is expanded in the IPFG
      // note that we use getMaxNumber() and not getNumberOfNodes() to account
      // for CFG implementations where the basic block numbering is
      // not dense. TODO: enforce an invariant that the numbering is
      // dense?
      return partialIPFG.getCFG(n).getMaxNumber() + 1;
    } else {
      // p is a collapsed node: we generate 2 blocks.
      return 2;
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlockNumber(java.lang.Object)
   */
  public int getLocalBlockNumber(Object n) {
    if (n instanceof IBasicBlock) {
      return ((IBasicBlock) n).getNumber();
    } else {
      return isEntry(n) ? 0 : 1;
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlock(java.lang.Object,
   *      int)
   */
  public Object getLocalBlock(CGNode procedure, int i) {
    CGNode n = procedure;
    if (noCollapse.contains(n)) {
      return partialIPFG.getCFG(n).getNode(i);
    } else {
      return (i == 0) ? nodeManager.getCollapsedEntry(n) : nodeManager.getCollapsedExit(n);
    }
  }

  public int getNumber(Object N) {
    return nodeManager.getNumber(N);
  }

  public Object getNode(int number) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public int getMaxNumber() {
    return nodeManager.getMaxNumber();
  }

  public Iterator<Object> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getSuccNodeNumbers(Object node) {
    return edgeManager.getSuccNodeNumbers(node);
  }

  public IntSet getPredNodeNumbers(Object node) {
    return edgeManager.getPredNodeNumbers(node);
  }

  public CallGraph getCallGraph() {
    return cg;
  }

  /**
   * We create 2 nodes for each collapsed call graph node, an entry node and an
   * exit node.
   */
  private final static class CollapsedNode {
    final CGNode node;

    final boolean isEntry;

    final int number;

    CollapsedNode(CGNode node, boolean isEntry, int number) {
      this.node = node;
      this.isEntry = isEntry;
      this.number = number;
    }

    @Override
    public String toString() {
      return node + "," + (isEntry ? "entry" : "exit");
    }

    @Override
    public int hashCode() {
      return 8017 * node.hashCode() + (isEntry ? 1 : 0);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof CollapsedNode) {
        CollapsedNode that = (CollapsedNode) other;
        return node.equals(that.node) && isEntry == that.isEntry;
      } else {
        return false;
      }
    }
  }

}
