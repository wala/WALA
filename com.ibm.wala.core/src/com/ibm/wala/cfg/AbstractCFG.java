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
package com.ibm.wala.cfg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.util.CompoundIterator;
import com.ibm.wala.util.IteratorPlusOne;
import com.ibm.wala.util.IteratorPlusTwo;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager;
import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.FixedSizeBitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * 
 * A graph of basic blocks.
 * 
 * @author sfink
 */
public abstract class AbstractCFG implements ControlFlowGraph, Constants {

  /**
   * The method this AbstractCFG represents
   */
  private final IMethod method;

  /**
   * An object to track nodes in this cfg
   */
  private DelegatingNumberedNodeManager<IBasicBlock> nodeManager = new DelegatingNumberedNodeManager<IBasicBlock>();

  /**
   * An object to track most normal edges in this cfg
   */
  private SparseNumberedEdgeManager<IBasicBlock> normalEdgeManager = new SparseNumberedEdgeManager<IBasicBlock>(nodeManager, 2,
      BasicNaturalRelation.SIMPLE);

  /**
   * An object to track not-to-exit exceptional edges in this cfg
   */
  private SparseNumberedEdgeManager<IBasicBlock> exceptionalEdgeManager = new SparseNumberedEdgeManager<IBasicBlock>(nodeManager,
      0, BasicNaturalRelation.SIMPLE);

  /**
   * Which basic blocks have a normal edge to exit()?
   */
  private FixedSizeBitVector normalToExit;

  /**
   * Which basic blocks have an exceptional edge to exit()?
   */
  private FixedSizeBitVector exceptionalToExit;

  /**
   * Which basic blocks have a fall-through?
   */
  private FixedSizeBitVector fallThru;

  /**
   * Which basic blocks are catch blocks?
   */
  private BitVector catchBlocks;

  /**
   * Cache here for efficiency
   */
  private IBasicBlock exit;

  /**
   * @param method
   */
  protected AbstractCFG(IMethod method) {
    this.method = method;
    this.catchBlocks = new BitVector(10);
  }

  /**
   * subclasses must call this before calling addEdge, but after creating the
   * nodes
   */
  protected void init() {
    normalToExit = new FixedSizeBitVector(getMaxNumber() + 1);
    exceptionalToExit = new FixedSizeBitVector(getMaxNumber() + 1);
    fallThru = new FixedSizeBitVector(getMaxNumber() + 1);
    exit = (IBasicBlock) getNode(getMaxNumber());
  }

  public abstract boolean equals(Object o);

  public abstract int hashCode();

  /**
   * Return the entry basic block for the CFG.
   * 
   * @return the entry basic block for the CFG.
   */
  public IBasicBlock entry() {
    return (IBasicBlock) getNode(0);
  }

  /**
   * Return the exit basic block for the CFG.
   * 
   * @return the exit basic block for the CFG.
   */
  public IBasicBlock exit() {
    return exit;
  }

  /*
   * (non-Javadoc)
   * 
   */
  public int getPredNodeCount(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (N.equals(exit())) {
      // TODO: cache if necessary
      FixedSizeBitVector x = FixedSizeBitVector.or(normalToExit, exceptionalToExit);
      return x.populationCount();
    } else {
      boolean normalIn = getNumberOfNormalIn(N) > 0;
      boolean exceptionalIn = getNumberOfExceptionalIn(N) > 0;
      if (normalIn) {
        if (exceptionalIn) {
          return new Iterator2Collection<IBasicBlock>(getPredNodes(N)).size();
        } else {
          return getNumberOfNormalIn(N);
        }
      } else {
        return getNumberOfExceptionalIn(N);
      }
    }
  }

  public int getNumberOfNormalIn(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!N.equals(exit()));
    }
    int number = getNumber(N);
    int xtra = 0;
    if (number > 0) {
      if (fallThru.get(number - 1)) {
        xtra++;
      }
    }
    return normalEdgeManager.getPredNodeCount(N) + xtra;
  }

  public int getNumberOfExceptionalIn(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!N.equals(exit()));
    }
    return exceptionalEdgeManager.getPredNodeCount(N);
  }

  /**
   * @param number
   *          number of a basic block in this cfg
   */
  boolean hasAnyNormalOut(int number) {
    return (fallThru.get(number) || normalEdgeManager.getSuccNodeCount(number) > 0 || normalToExit.get(number));
  }

  /**
   * @param number
   *          number of a basic block in this cfg
   */
  private int getNumberOfNormalOut(int number) {
    int xtra = 0;
    if (fallThru.get(number)) {
      xtra++;
    }
    if (normalToExit.get(number)) {
      xtra++;
    }
    return normalEdgeManager.getSuccNodeCount(number) + xtra;
  }

  /**
   * @param number
   *          number of a basic block in this cfg
   */
  public int getNumberOfExceptionalOut(int number) {
    int xtra = 0;
    if (exceptionalToExit.get(number)) {
      xtra++;
    }
    return exceptionalEdgeManager.getSuccNodeCount(number) + xtra;
  }

  public int getNumberOfNormalOut(IBasicBlock N) {
    return getNumberOfNormalOut(getNumber(N));
  }

  public int getNumberOfExceptionalOut(final IBasicBlock N) {
    return getNumberOfExceptionalOut(getNumber(N));
  }

  public Iterator<IBasicBlock> getPredNodes(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (N.equals(exit())) {
      return new FilterIterator<IBasicBlock>(iterator(), new Filter() {
        public boolean accepts(Object o) {
          int i = getNumber((IBasicBlock) o);
          return normalToExit.get(i) || exceptionalToExit.get(i);
        }
      });
    } else {
      int number = getNumber(N);
      boolean normalIn = getNumberOfNormalIn(N) > 0;
      boolean exceptionalIn = getNumberOfExceptionalIn(N) > 0;
      if (normalIn) {
        if (exceptionalIn) {
          HashSet<IBasicBlock> result = new HashSet<IBasicBlock>(getNumberOfNormalIn(N) + getNumberOfExceptionalIn(N));
          result.addAll(new Iterator2Collection<IBasicBlock>(normalEdgeManager.getPredNodes(N)));
          result.addAll(new Iterator2Collection<IBasicBlock>(exceptionalEdgeManager.getPredNodes(N)));
          if (fallThru.get(number - 1)) {
            result.add(getNode(number - 1));
          }
          return result.iterator();
        } else {
          if (number > 0 && fallThru.get(number - 1)) {
            return new IteratorPlusOne<IBasicBlock>(normalEdgeManager.getPredNodes(N), getNode(number - 1));
          } else {
            return normalEdgeManager.getPredNodes(N);
          }
        }
      } else {
        // !normalIn
        if (exceptionalIn) {
          return exceptionalEdgeManager.getPredNodes(N);
        } else {
          return EmptyIterator.instance();
        }
      }
    }
  }

  public int getSuccNodeCount(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (N.equals(exit())) {
      return 0;
    }
    int nNormal = getNumberOfNormalOut(N);
    int nExc = getNumberOfExceptionalOut(N);
    if (nNormal > 0) {
      if (nExc > 0) {
        if (nExc == 1) {
          int number = getNumber(N);
          if (exceptionalToExit.get(number)) {
            if (normalToExit.get(number)) {
              return nNormal + nExc - 1;
            } else {
              return nNormal + nExc;
            }
          } else {
            return slowCountSuccNodes(N);
          }
        } else {
          return slowCountSuccNodes(N);
        }
      } else {
        return nNormal;
      }
    } else {
      // nNormal == 0
      return nExc;
    }
  }

  private int slowCountSuccNodes(IBasicBlock N) {
    return new Iterator2Collection<IBasicBlock>(getSuccNodes(N)).size();
  }

  public Iterator<IBasicBlock> getSuccNodes(IBasicBlock N) {
    int number = getNumber(N);
    if (normalToExit.get(number) && exceptionalToExit.get(number)) {
      return new CompoundIterator<IBasicBlock>(iterateNormalSuccessorsWithoutExit(number), iterateExceptionalSuccessors(number));
    } else {
      return new CompoundIterator<IBasicBlock>(iterateNormalSuccessors(number), iterateExceptionalSuccessors(number));
    }
  }

  private Iterator<IBasicBlock> iterateExceptionalSuccessors(int number) {
    if (exceptionalEdgeManager.hasAnySuccessor(number)) {
      if (exceptionalToExit.get(number)) {
        return new IteratorPlusOne<IBasicBlock>(exceptionalEdgeManager.getSuccNodes(number), exit());
      } else {
        return exceptionalEdgeManager.getSuccNodes(number);
      }
    } else {
      if (exceptionalToExit.get(number)) {
        return new NonNullSingletonIterator<IBasicBlock>(exit());
      } else {
        return EmptyIterator.instance();
      }
    }
  }

  Iterator<IBasicBlock> iterateExceptionalPredecessors(IBasicBlock N) {
    if (N.equals(exit())) {
      return new FilterIterator<IBasicBlock>(iterator(), new Filter() {
        public boolean accepts(Object o) {
          int i = getNumber((IBasicBlock) o);
          return exceptionalToExit.get(i);
        }
      });
    } else {
      return exceptionalEdgeManager.getPredNodes(N);
    }
  }

  Iterator<IBasicBlock> iterateNormalPredecessors(IBasicBlock N) {
    if (N.equals(exit())) {
      return new FilterIterator<IBasicBlock>(iterator(), new Filter() {
        public boolean accepts(Object o) {
          int i = getNumber((IBasicBlock) o);
          return normalToExit.get(i);
        }
      });
    } else {
      int number = getNumber(N);
      if (number > 0 && fallThru.get(number - 1)) {
        return new IteratorPlusOne<IBasicBlock>(normalEdgeManager.getPredNodes(N), getNode(number - 1));
      } else {
        return normalEdgeManager.getPredNodes(N);
      }
    }
  }

  private Iterator<IBasicBlock> iterateNormalSuccessors(int number) {
    if (fallThru.get(number)) {
      if (normalToExit.get(number)) {
        return new IteratorPlusTwo<IBasicBlock>(normalEdgeManager.getSuccNodes(number), getNode(number + 1), exit());
      } else {
        return new IteratorPlusOne<IBasicBlock>(normalEdgeManager.getSuccNodes(number), getNode(number + 1));
      }
    } else {
      if (normalToExit.get(number)) {
        return new IteratorPlusOne<IBasicBlock>(normalEdgeManager.getSuccNodes(number), exit());
      } else {
        return normalEdgeManager.getSuccNodes(number);
      }
    }
  }

  private Iterator<IBasicBlock> iterateNormalSuccessorsWithoutExit(int number) {
    if (fallThru.get(number)) {
      return new IteratorPlusOne<IBasicBlock>(normalEdgeManager.getSuccNodes(number), getNode(number + 1));
    } else {
      return normalEdgeManager.getSuccNodes(number);
    }
  }

  /**
   * @param n
   */
  public void addNode(IBasicBlock n) {
    nodeManager.addNode(n);
  }

  public int getMaxNumber() {
    return nodeManager.getMaxNumber();
  }

  public IBasicBlock getNode(int number) {
    return nodeManager.getNode(number);
  }

  public int getNumber(IBasicBlock N) {
    return nodeManager.getNumber(N);
  }

  public int getNumberOfNodes() {
    return nodeManager.getNumberOfNodes();
  }

  public Iterator<IBasicBlock> iterator() {
    return nodeManager.iterator();
  }

  /**
   * @param src
   * @param dst
   */
  public void addEdge(IBasicBlock src, IBasicBlock dst) {
    Assertions.UNREACHABLE("Don't call me .. use addNormalEdge or addExceptionalEdge");
  }

  public void removeEdge(IBasicBlock src, IBasicBlock dst) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   */
  public boolean hasEdge(IBasicBlock src, IBasicBlock dst) {
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    int x = getNumber(src);
    if (dst.equals(exit())) {
      return normalToExit.get(x) || exceptionalToExit.get(x);
    } else if (getNumber(dst) == (x + 1) && fallThru.get(x)) {
      return true;
    }
    return normalEdgeManager.hasEdge(src, dst) || exceptionalEdgeManager.hasEdge(src, dst);
  }

  public boolean hasExceptionalEdge(IBasicBlock src, IBasicBlock dst) {
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    int x = getNumber(src);
    if (dst.equals(exit())) {
      return exceptionalToExit.get(x);
    }
    return exceptionalEdgeManager.hasEdge(src, dst);
  }

  public boolean hasNormalEdge(IBasicBlock src, IBasicBlock dst) {
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    int x = getNumber(src);
    if (dst.equals(exit())) {
      return normalToExit.get(x);
    } else if (getNumber(dst) == (x + 1) && fallThru.get(x)) {
      return true;
    }
    return normalEdgeManager.hasEdge(src, dst);
  }

  /**
   * @param src
   * @param dst
   * @throws IllegalArgumentException
   *           if dst is null
   */
  public void addNormalEdge(IBasicBlock src, IBasicBlock dst) {
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    if (dst.equals(exit())) {
      normalToExit.set(getNumber(src));
    } else if (getNumber(dst) == (getNumber(src) + 1)) {
      fallThru.set(getNumber(src));
    } else {
      normalEdgeManager.addEdge(src, dst);
    }
  }

  /**
   * @param src
   * @param dst
   * @throws IllegalArgumentException
   *           if dst is null
   */
  public void addExceptionalEdge(IBasicBlock src, IBasicBlock dst) {
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    if (dst.equals(exit())) {
      exceptionalToExit.set(getNumber(src));
    } else {
      exceptionalEdgeManager.addEdge(src, dst);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#removeNode(com.ibm.wala.util.graph.Node)
   */
  public void removeNodeAndEdges(IBasicBlock N) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  public void removeNode(IBasicBlock n) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(com.ibm.wala.util.graph.Node)
   */
  public boolean containsNode(IBasicBlock N) {
    return nodeManager.containsNode(N);
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (Iterator it = iterator(); it.hasNext();) {
      IBasicBlock bb = (IBasicBlock) it.next();
      s.append("BB").append(getNumber(bb)).append("\n");

      Iterator<IBasicBlock> succNodes = getSuccNodes(bb);
      while (succNodes.hasNext()) {
        s.append("    -> BB").append(getNumber(succNodes.next())).append("\n");
      }
    }
    return s.toString();
  }

  /**
   * record that basic block i is a catch block
   * 
   * @param i
   */
  protected void setCatchBlock(int i) {
    catchBlocks.set(i);
  }

  /**
   * @param i
   * @return true iff block i is a catch block
   */
  public boolean isCatchBlock(int i) {
    return catchBlocks.get(i);
  }

  /**
   * Returns the catchBlocks.
   * 
   * @return BitVector
   */
  public BitVector getCatchBlocks() {
    return catchBlocks;
  }

  public IMethod getMethod() {
    return method;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(java.lang.Object)
   */
  public void removeAllIncidentEdges(IBasicBlock node) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getExceptionalSuccessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return new Iterator2Collection<IBasicBlock>(iterateExceptionalSuccessors(b.getNumber()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getNormalSuccessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return new Iterator2Collection<IBasicBlock>(iterateNormalSuccessors(b.getNumber()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  public Iterator<IBasicBlock> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<IBasicBlock>(s, this);
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void removeIncomingEdges(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void removeOutgoingEdges(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }

  public FixedSizeBitVector getExceptionalToExit() {
    return exceptionalToExit;
  }

  public FixedSizeBitVector getNormalToExit() {
    return normalToExit;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalPredecessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getExceptionalPredecessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return new Iterator2Collection<IBasicBlock>(iterateExceptionalPredecessors(b));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalPredecessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getNormalPredecessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return new Iterator2Collection<IBasicBlock>(iterateNormalPredecessors(b));
  }

  /*
   * (non-Javadoc)
   * 
   */
  public IntSet getPredNodeNumbers(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * TODO: optimize this. (non-Javadoc)
   * 
   */
  public IntSet getSuccNodeNumbers(IBasicBlock node) {
    int number = getNumber(node);
    IntSet s = normalEdgeManager.getSuccNodeNumbers(node);
    MutableSparseIntSet result = s == null ? new MutableSparseIntSet() : new MutableSparseIntSet(s);
    s = exceptionalEdgeManager.getSuccNodeNumbers(node);
    if (s != null) {
      result.addAll(s);
    }
    if (normalToExit.get(number) || exceptionalToExit.get(number)) {
      result.add(exit.getNumber());
    }
    if (fallThru.get(number)) {
      result.add(number + 1);
    }
    return result;
  }
}
