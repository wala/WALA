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
package com.ibm.wala.cfg.cdg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;

/**
 * Control Dependence Graph
 * 
 * TODO: document me!
 * 
 * @author Mangala Gowri Nanda
 * 
 */
public class BVControlDependenceGraph<T extends IBasicBlock> extends AbstractNumberedGraph<T> {

  /**
   * Governing control flow-graph. The control dependence graph is computed from
   * this cfg.
   */
  private final ControlFlowGraph<T> cfg;

  /**
   * the EdgeManager for the CDG. It implements the edge part of the standard
   * Graph abstraction, using the control-dependence edges of the cdg.
   */
  private final EdgeManager<T> edgeManager;

  private final boolean ignoreUnreachableCode;

  private final HashMap<T, BasicBlock<T>> bbMap = HashMapFactory.make();

  final private Vector<BasicBlock<T>> seen = new Vector<BasicBlock<T>>();

  private BasicBlock<T> entry;

  final private Vector<BasicBlock<T>> entryBlocks = new Vector<BasicBlock<T>>();

  private BasicBlock<T> exitnode;

  private int count = 0;

  private int cfgCount = 0;

  private final int isz = 32;

  private final int BitMasks[] = { 0x00000001, 0x00000002, 0x00000004, 0x00000008, 0x00000010, 0x00000020, 0x00000040, 0x00000080,
      0x00000100, 0x00000200, 0x00000400, 0x00000800, 0x00001000, 0x00002000, 0x00004000, 0x00008000, 0x00010000, 0x00020000,
      0x00040000, 0x00080000, 0x00100000, 0x00200000, 0x00400000, 0x00800000, 0x01000000, 0x02000000, 0x04000000, 0x08000000,
      0x10000000, 0x20000000, 0x40000000, 0x80000000 };

  private int BitvectorSize = 0;

  private int bitvectors[][];

  /**
   * @param cfg
   *            governing control flow graph wantEdgeLabels is always true
   */
  public BVControlDependenceGraph(ControlFlowGraph<T> cfg) {
    this(cfg, false);
  }

  /**
   * @param cfg
   *            governing control flow graph wantEdgeLabels is always true
   */
  public BVControlDependenceGraph(ControlFlowGraph<T> cfg, boolean ignoreUnreachableCode) {
    this.cfg = cfg;
    this.ignoreUnreachableCode = ignoreUnreachableCode;
    buildParallelGraph();
    buildCDG();
    this.edgeManager = constructGraphEdges();
  }

  public ControlFlowGraph getUnderlyingCFG() {
    return cfg;
  }

  /**
   * Return the set of edge labels for the control flow edges that cause the
   * given edge in the CDG.
   */
  public Set<T> getEdgeLabels(Object src, Object dst) {
    BasicBlock<T> csrc = bbMap.get(src);
    if (csrc == null) {
      return Collections.emptySet();
    }
    BasicBlock<T> cdst = bbMap.get(dst);
    if (cdst == null) {
      return Collections.emptySet();
    }
    return csrc.getLabels(cdst);
  }

  @Override
  public NodeManager<T> getNodeManager() {
    return cfg;
  }

  @Override
  public EdgeManager<T> getEdgeManager() {
    return edgeManager;
  }

  private void buildParallelGraph() {
    for (Iterator<? extends T> it = cfg.iterator(); it.hasNext();) {
      T bb = it.next();
      BasicBlock<T> cdgbb = new BasicBlock<T>(bb);
      bbMap.put(bb, cdgbb);
      cfgCount++;
    }

    Object entryBB = cfg.entry(); // original entry node
    entry = new BasicBlock<T>(null); // parallel entry BasicBlock
    exitnode = bbMap.get(cfg.exit());
    entryBlocks.add(entry);

    for (Iterator<? extends T> it = cfg.iterator(); it.hasNext();) {
      T bb = it.next();
      BasicBlock<T> cdgbb = bbMap.get(bb);

      // kludge for handling multi-entry CFGs
      if (!ignoreUnreachableCode && bb != entryBB) {
        if (cfg.getPredNodeCount(bb) == 0) {
          entryBlocks.add(cdgbb);
        }
      }

      // build cfg edges for the parallel graph
      for (Iterator succ = cfg.getSuccNodes(bb); succ.hasNext();) {
        Object sbb = succ.next();
        BasicBlock<T> cdgsbb = bbMap.get(sbb);
        cfgEdge(cdgbb, cdgsbb);
      }
    }

    // link parallel entry to original entry
    cfgEdge(entry, bbMap.get(cfg.entry()));
    // link parallel entry to exit
    cfgEdge(entry, exitnode);
  }

  /**
   * This is the heart of the CDG computation. A simple bitvector
   * implementation.
   */
  private void buildCDG() {
    // count and index the nodes
    count(exitnode);
    for (int i = 0; i < entryBlocks.size(); i++) {
      BasicBlock<T> en = entryBlocks.get(i);
      count(en);
    }

    // initialize bitvectors
    count = seen.size();
    if (!ignoreUnreachableCode && count != cfgCount + 1) {
      System.out.println("Strange! count=" + count + ", cfgCount=" + cfgCount);
    }

    BitvectorSize = count / isz + ((count % isz != 0) ? 1 : 0);
    bitvectors = new int[count][BitvectorSize];

    // initialize the exitnode first
    exitnode.index = 0;
    ClearVector(0);
    SetBitInVector(0, 0);

    // initialize the rest of the blocks
    for (int i = 1; i < count; i++) {
      BasicBlock<T> bb = seen.get(i);
      bb.index = i;
      SetVector(i);
    }

    // iterate till all post dominators are done
    boolean change = true;
    while (change) {
      change = false;
      for (int i = 1; i < count; i++) {
        BasicBlock sb = seen.get(i);
        Vector succ = sb.getSuccessors();
        for (int j = 0; j < succ.size(); j++) {
          if (intersect(sb.index, ((BasicBlock) succ.get(j)).index)) {
            change = true;
          }
        }
      }
    }

    // to find the control dependence
    for (int n = count - 1; n >= 0; n--) {
      int i, j, k, tx;
      BasicBlock<T> bb = seen.get(n);
      Vector<BasicBlock<T>> succ = bb.getSuccessors();
      if (succ.size() > 1) {
        for (int m = 0; m < succ.size(); m++) {
          BasicBlock<T> sb = succ.get(m);
          for (i = 0, k = 0; i < BitvectorSize; i++) {
            // postdominates sb but does not postdominate bb
            tx = bitvectors[sb.index][i] & ~bitvectors[bb.index][i];
            for (j = 0; j < isz; j++, k++) {
              if (k >= count)
                break;

              if (GetBit(tx, BitMasks[j]) != 0) {
                cdEdge(bb, seen.get(i * isz + j), sb.item);
              }
            }
          }
        }
        // If bb postdominates one of its successors sb, it is control dependent
        // on itself with label sb
        for (int m = 0; m < succ.size(); m++) {
          BasicBlock<T> sb = succ.get(m);
          if (postdominates(sb.index, bb.index)) {
            cdEdge(bb, bb, sb.item);
          }
        }
      }
    }
  }

  /**
   * EdgeManager
   */
  private EdgeManager<T> constructGraphEdges() {
    return new EdgeManager<T>() {
      public Iterator<T> getPredNodes(T N) {
        BasicBlock<T> cbb = bbMap.get(N);
        if (cbb == null) {
          return EmptyIterator.instance();
        }
        return cbb.getPredNodes();
      }

      public int getPredNodeCount(T N) {
        BasicBlock cbb = bbMap.get(N);
        if (cbb == null)
          return 0;
        return cbb.getPredNodeCount();
      }

      public Iterator<T> getSuccNodes(T N) {
        BasicBlock<T> cbb = bbMap.get(N);
        if (cbb == null) {
          return EmptyIterator.instance();
        }
        return cbb.getSuccNodes();
      }

      public int getSuccNodeCount(IBasicBlock N) {
        BasicBlock cbb = bbMap.get(N);
        if (cbb == null)
          return 0;
        return cbb.getSuccNodeCount();
      }

      public boolean hasEdge(IBasicBlock src, IBasicBlock dst) {
        BasicBlock csrc = bbMap.get(src);
        if (csrc == null)
          return false;
        BasicBlock cdst = bbMap.get(dst);
        if (cdst == null)
          return false;
        return csrc.hasCDSuccessor(cdst);
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
    };
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Iterator<T> ns = iterator(); ns.hasNext();) {
      T n = ns.next();
      sb.append(n.toString()).append("\n");
      for (Iterator<? extends T> ss = getSuccNodes(n); ss.hasNext();) {
        Object s = ss.next();
        sb.append("  --> ").append(s);
        for (Iterator labels = getEdgeLabels(n, s).iterator(); labels.hasNext();)
          sb.append("\n   label: ").append(labels.next());
        sb.append("\n");
      }
    }

    return sb.toString();
  }

  private void count(BasicBlock<T> bb) {
    // postorder
    if (bb.mark)
      return;
    bb.mark = true;

    Vector<BasicBlock<T>> succ = bb.getSuccessors();
    for (int i = 0; i < succ.size(); i++) {
      BasicBlock<T> bs = succ.get(i);
      count(bs);
    }
    seen.add(bb);
  }

  private void cfgEdge(BasicBlock<T> b1, BasicBlock<T> b2) {
    b2.linkPredecessor(b1);
    b1.linkSuccessor(b2);
  }

  private void cdEdge(BasicBlock<T> b1, BasicBlock<T> b2, T label) {
    if (b1 == entry)
      return;
    b2.linkCDpredecessor(b1);
    b1.linkCDsuccessor(b2, label);
  }

  private void ClearVector(int bb) {
    for (int i = 0; i < BitvectorSize; i++) {
      bitvectors[bb][i] = ClearAllBits();
    }
  }

  private void SetVector(int bb) {
    for (int i = 0; i < BitvectorSize; i++) {
      bitvectors[bb][i] = SetAllBits();
    }
  }

  private void SetBitInVector(int bb, int index) {
    int div = index / isz;
    int mod = index % isz;
    bitvectors[bb][div] = SetBit(bitvectors[bb][div], BitMasks[mod]);
  }

  private boolean intersect(int b1, int b2) {
    int div = b1 / isz;
    int mod = b1 % isz;
    boolean change = false;
    for (int i = 0; i < BitvectorSize; i++) {
      int save = bitvectors[b1][i];
      bitvectors[b1][i] &= bitvectors[b2][i];
      if (i == div)
        bitvectors[b1][i] = SetBit(bitvectors[b1][i], BitMasks[mod]);
      if (save != bitvectors[b1][i])
        change = true;
    }
    return change;
  }

  private boolean postdominates(int b1, int b2) {
    int div = b2 / isz;
    int mod = b2 % isz;
    return (GetBit(bitvectors[b1][div], BitMasks[mod]) != 0);
  }

  private int SetBit(int flg, int msk) {
    return flg |= msk;
  }

  /*
   * private int ClearBit(int flg, int msk){ return flg &= ~msk; }
   */

  private int GetBit(int flg, int msk) {
    return flg & msk;
  }

  private int ClearAllBits() {
    return 0x00000000;
  }

  private int SetAllBits() {
    return 0xffffffff;
  }

  public static class BasicBlock<T extends IBasicBlock> {

    protected final T item;

    protected boolean mark = false;

    protected int index;

    final private Vector<BasicBlock<T>> predecessors = new Vector<BasicBlock<T>>(2);

    final private Vector<BasicBlock<T>> successors = new Vector<BasicBlock<T>>(2);

    final private Vector<T> cdPred = new Vector<T>(2);

    final private Vector<T> cdSucc = new Vector<T>(2);

    final private HashMap<T, Set<T>> labelMap = HashMapFactory.make();

    private BasicBlock(T item) {
      this.item = item;
    }

    @Override
    public String toString() {
      return item.toString();
    }

    private void linkPredecessor(BasicBlock<T> bb) {
      Assertions._assert(bb != null);

      if (!predecessors.contains(bb))
        predecessors.add(bb);
    }

    /*
     * private int countPredecessors () { return predecessors.size(); }
     * 
     * private BasicBlock getPredecessor ( int idx ) { return (BasicBlock)
     * predecessors.get(idx); }
     * 
     * private Vector getPredecessors () { return predecessors; }
     */

    private void linkSuccessor(BasicBlock<T> bb) {
      Assertions._assert(bb != null);

      if (!successors.contains(bb))
        successors.add(bb);
    }

    /*
     * private int countSuccessors () { return successors.size(); }
     * 
     * private BasicBlock getSuccessor ( int idx ) { return (BasicBlock)
     * successors.get(idx); }
     */

    private Vector<BasicBlock<T>> getSuccessors() {
      return successors;
    }

    private void linkCDpredecessor(BasicBlock<T> bb) {
      Assertions._assert(bb != null);

      if (!cdPred.contains(bb.item))
        cdPred.add(bb.item);
    }

    private int getPredNodeCount() {
      return cdPred.size();
    }

    private Iterator<T> getPredNodes() {
      return cdPred.iterator();
    }

    private void linkCDsuccessor(BasicBlock<T> bb, T label) {
      Assertions._assert(bb != null);

      if (!cdSucc.contains(bb.item)) {
        cdSucc.add(bb.item);
      }
      Set<T> labelSet = labelMap.get(bb.item);
      if (labelSet == null) {
        labelSet = HashSetFactory.make(2);
        labelMap.put(bb.item, labelSet);
      }
      if (label != null && !labelSet.contains(label))
        labelSet.add(label);
    }

    private int getSuccNodeCount() {
      return cdSucc.size();
    }

    private Iterator<T> getSuccNodes() {
      return cdSucc.iterator();
    }

    private Set<T> getLabels(BasicBlock<T> succ) {
      Set<T> ret = labelMap.get(succ.item);
      if (ret == null) {
        return Collections.emptySet();
      }
      return ret;
    }

    private boolean hasCDSuccessor(BasicBlock succ) {
      if (cdSucc.contains(succ.item))
        return true;
      return false;
    }

  } // end of BasicBlock
}
