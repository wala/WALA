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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.IteratorPlusOne;
import com.ibm.wala.util.collections.IteratorPlusTwo;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager;
import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.FixedSizeBitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.SimpleIntVector;

/**
 * Common functionality for {@link ControlFlowGraph} implementations.
 */
public abstract class AbstractCFG<I, T extends IBasicBlock<I>> implements ControlFlowGraph<I, T>, MinimalCFG<T>, Constants {

  /**
   * The method this AbstractCFG represents
   */
  private final IMethod method;

  /**
   * An object to track nodes in this cfg
   */
  final private DelegatingNumberedNodeManager<T> nodeManager = new DelegatingNumberedNodeManager<>();

  /**
   * An object to track most normal edges in this cfg
   */
  final private SparseNumberedEdgeManager<T> normalEdgeManager = new SparseNumberedEdgeManager<>(nodeManager, 2,
      BasicNaturalRelation.SIMPLE);

  /**
   * An object to track not-to-exit exceptional edges in this cfg
   */
  final private SparseNumberedEdgeManager<T> exceptionalEdgeManager = new SparseNumberedEdgeManager<>(nodeManager, 0,
      BasicNaturalRelation.SIMPLE);

  /**
   * An object to track not-to-exit exceptional edges in this cfg, indexed by block number. exceptionalEdges[i] is a list of block
   * numbers that are non-exit exceptional successors of block i, in order of increasing "catch scope".
   */
  final private SimpleVector<SimpleIntVector> exceptionalSuccessors = new SimpleVector<>();

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
  final private BitVector catchBlocks;

  /**
   * Cache here for efficiency
   */
  private T exit;

  protected AbstractCFG(IMethod method) {
    this.method = method;
    this.catchBlocks = new BitVector(10);
  }

  /**
   * subclasses must call this before calling addEdge, but after creating the nodes
   */
  protected void init() {
    normalToExit = new FixedSizeBitVector(getMaxNumber() + 1);
    exceptionalToExit = new FixedSizeBitVector(getMaxNumber() + 1);
    fallThru = new FixedSizeBitVector(getMaxNumber() + 1);
    exit = getNode(getMaxNumber());
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  /**
   * Return the entry basic block for the CFG.
   */
  @Override
  public T entry() {
    return getNode(0);
  }

  /**
   * Return the exit basic block for the CFG.
   */
  @Override
  public T exit() {
    return exit;
  }

  @Override
  public int getPredNodeCount(T N) {
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
          return Iterator2Collection.toSet(getPredNodes(N)).size();
        } else {
          return getNumberOfNormalIn(N);
        }
      } else {
        return getNumberOfExceptionalIn(N);
      }
    }
  }

  public int getNumberOfNormalIn(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    assert !N.equals(exit());
    int number = getNumber(N);
    int xtra = 0;
    if (number > 0) {
      if (fallThru.get(number - 1)) {
        xtra++;
      }
    }
    return normalEdgeManager.getPredNodeCount(N) + xtra;
  }

  public int getNumberOfExceptionalIn(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    assert !N.equals(exit());
    return exceptionalEdgeManager.getPredNodeCount(N);
  }

  /**
   * @param number number of a basic block in this cfg
   */
  boolean hasAnyNormalOut(int number) {
    return (fallThru.get(number) || normalEdgeManager.getSuccNodeCount(number) > 0 || normalToExit.get(number));
  }

  /**
   * @param number number of a basic block in this cfg
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
   * @param number number of a basic block in this cfg
   */
  public int getNumberOfExceptionalOut(int number) {
    int xtra = 0;
    if (exceptionalToExit.get(number)) {
      xtra++;
    }
    return exceptionalEdgeManager.getSuccNodeCount(number) + xtra;
  }

  public int getNumberOfNormalOut(T N) {
    return getNumberOfNormalOut(getNumber(N));
  }

  public int getNumberOfExceptionalOut(final T N) {
    return getNumberOfExceptionalOut(getNumber(N));
  }

  @Override
  public Iterator<T> getPredNodes(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (N.equals(exit())) {
      return new FilterIterator<>(iterator(), o -> {
        int i = getNumber(o);
        return normalToExit.get(i) || exceptionalToExit.get(i);
      });
    } else {
      int number = getNumber(N);
      boolean normalIn = getNumberOfNormalIn(N) > 0;
      boolean exceptionalIn = getNumberOfExceptionalIn(N) > 0;
      if (normalIn) {
        if (exceptionalIn) {
          HashSet<T> result = HashSetFactory.make(getNumberOfNormalIn(N) + getNumberOfExceptionalIn(N));
          result.addAll(Iterator2Collection.toSet(normalEdgeManager.getPredNodes(N)));
          result.addAll(Iterator2Collection.toSet(exceptionalEdgeManager.getPredNodes(N)));
          if (fallThru.get(number - 1)) {
            result.add(getNode(number - 1));
          }
          return result.iterator();
        } else {
          if (number > 0 && fallThru.get(number - 1)) {
            return IteratorPlusOne.make(normalEdgeManager.getPredNodes(N), getNode(number - 1));
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

  @Override
  public int getSuccNodeCount(T N) {
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

  private int slowCountSuccNodes(T N) {
    return Iterator2Collection.toSet(getSuccNodes(N)).size();
  }

  @Override
  public Iterator<T> getSuccNodes(T N) {
    int number = getNumber(N);
    if (normalToExit.get(number) && exceptionalToExit.get(number)) {
      return new CompoundIterator<>(iterateNormalSuccessorsWithoutExit(number), iterateExceptionalSuccessors(number));
    } else {
      return new CompoundIterator<>(iterateNormalSuccessors(number), iterateExceptionalSuccessors(number));
    }
  }

  /**
   * @param number of a basic block
   * @return the exceptional successors of the basic block, in order of increasing catch scope.
   */
  private Iterator<T> iterateExceptionalSuccessors(int number) {
    if (exceptionalEdgeManager.hasAnySuccessor(number)) {
      List<T> result = new ArrayList<>();
      SimpleIntVector v = exceptionalSuccessors.get(number);
      for (int i = 0; i <= v.getMaxIndex(); i++) {
        result.add(getNode(v.get(i)));
      }
      if (exceptionalToExit.get(number)) {
        result.add(exit);
      }
      return result.iterator();
    } else {
      if (exceptionalToExit.get(number)) {
        return new NonNullSingletonIterator<>(exit());
      } else {
        return EmptyIterator.instance();
      }
    }
  }

  Iterator<T> iterateExceptionalPredecessors(T N) {
    if (N.equals(exit())) {
      return new FilterIterator<>(iterator(), o -> {
        int i = getNumber(o);
        return exceptionalToExit.get(i);
      });
    } else {
      return exceptionalEdgeManager.getPredNodes(N);
    }
  }

  Iterator<T> iterateNormalPredecessors(T N) {
    if (N.equals(exit())) {
      return new FilterIterator<>(iterator(), o -> {
        int i = getNumber(o);
        return normalToExit.get(i);
      });
    } else {
      int number = getNumber(N);
      if (number > 0 && fallThru.get(number - 1)) {
        return IteratorPlusOne.make(normalEdgeManager.getPredNodes(N), getNode(number - 1));
      } else {
        return normalEdgeManager.getPredNodes(N);
      }
    }
  }

  private Iterator<T> iterateNormalSuccessors(int number) {
    if (fallThru.get(number)) {
      if (normalToExit.get(number)) {
        return new IteratorPlusTwo<>(normalEdgeManager.getSuccNodes(number), getNode(number + 1), exit());
      } else {
        return IteratorPlusOne.make(normalEdgeManager.getSuccNodes(number), getNode(number + 1));
      }
    } else {
      if (normalToExit.get(number)) {
        return IteratorPlusOne.make(normalEdgeManager.getSuccNodes(number), exit());
      } else {
        return normalEdgeManager.getSuccNodes(number);
      }
    }
  }

  private Iterator<T> iterateNormalSuccessorsWithoutExit(int number) {
    if (fallThru.get(number)) {
      return IteratorPlusOne.make(normalEdgeManager.getSuccNodes(number), getNode(number + 1));
    } else {
      return normalEdgeManager.getSuccNodes(number);
    }
  }

  /**
   * @param n
   */
  @Override
  public void addNode(T n) {
    nodeManager.addNode(n);
  }

  @Override
  public int getMaxNumber() {
    return nodeManager.getMaxNumber();
  }

  @Override
  public T getNode(int number) {
    return nodeManager.getNode(number);
  }

  @Override
  public int getNumber(T N) {
    return nodeManager.getNumber(N);
  }

  @Override
  public int getNumberOfNodes() {
    return nodeManager.getNumberOfNodes();
  }

  @Override
  public Iterator<T> iterator() {
    return nodeManager.iterator();
  }

  @Override
  public void addEdge(T src, T dst) throws UnimplementedError {
    Assertions.UNREACHABLE("Don't call me .. use addNormalEdge or addExceptionalEdge");
  }

  @Override
  public void removeEdge(T src, T dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEdge(T src, T dst) {
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

  public boolean hasExceptionalEdge(T src, T dst) {
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    int x = getNumber(src);
    if (dst.equals(exit())) {
      return exceptionalToExit.get(x);
    }
    return exceptionalEdgeManager.hasEdge(src, dst);
  }

  public boolean hasNormalEdge(T src, T dst) {
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
   * @throws IllegalArgumentException if src or dst is null
   */
  public void addNormalEdge(T src, T dst) {
    if (src == null) {
      throw new IllegalArgumentException("src is null");
    }
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
   * @throws IllegalArgumentException if dst is null
   */
  public void addExceptionalEdge(T src, T dst) {
    if (dst == null) {
      throw new IllegalArgumentException("dst is null");
    }
    if (dst.equals(exit())) {
      exceptionalToExit.set(getNumber(src));
    } else {
      exceptionalEdgeManager.addEdge(src, dst);
      SimpleIntVector v = exceptionalSuccessors.get(getNumber(src));
      if (v == null) {
        v = new SimpleIntVector(-1);
        exceptionalSuccessors.set(getNumber(src), v);
        v.set(0, getNumber(dst));
        return;
      }
      if (v.get(v.getMaxIndex()) != getNumber(dst)) {
        v.set(v.getMaxIndex() + 1, getNumber(dst));
      }
    }
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#removeNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNodeAndEdges(T N) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNode(T n) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public boolean containsNode(T N) {
    return nodeManager.containsNode(N);
  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (T bb : this) {
      s.append("BB").append(getNumber(bb)).append("\n");

      Iterator<T> succNodes = getSuccNodes(bb);
      while (succNodes.hasNext()) {
        s.append("    -> BB").append(getNumber(succNodes.next())).append("\n");
      }
    }
    return s.toString();
  }

  /**
   * record that basic block i is a catch block
   */
  protected void setCatchBlock(int i) {
    catchBlocks.set(i);
  }

  /**
   * @return true iff block i is a catch block
   */
  public boolean isCatchBlock(int i) {
    return catchBlocks.get(i);
  }

  /**
   * Returns the catchBlocks.
   */
  @Override
  public BitVector getCatchBlocks() {
    return catchBlocks;
  }

  @Override
  public IMethod getMethod() {
    return method;
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(java.lang.Object)
   */
  @Override
  public void removeAllIncidentEdges(T node) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalSuccessors(com.ibm.wala.cfg.T)
   */
  @Override
  public List<T> getExceptionalSuccessors(T b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    List<T> result = new ArrayList<>();
    for (T s : Iterator2Iterable.make(iterateExceptionalSuccessors(b.getNumber()))) {
      result.add(s);
    }
    return result;
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.T)
   */
  @Override
  public Collection<T> getNormalSuccessors(T b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return Iterator2Collection.toSet(iterateNormalSuccessors(b.getNumber()));
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public Iterator<T> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<>(s, this);
  }

  @Override
  public void removeIncomingEdges(T node) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeOutgoingEdges(T node) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  public FixedSizeBitVector getExceptionalToExit() {
    return exceptionalToExit;
  }

  public FixedSizeBitVector getNormalToExit() {
    return normalToExit;
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalPredecessors(com.ibm.wala.cfg.T)
   */
  @Override
  public Collection<T> getExceptionalPredecessors(T b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return Iterator2Collection.toSet(iterateExceptionalPredecessors(b));
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalPredecessors(com.ibm.wala.cfg.T)
   */
  @Override
  public Collection<T> getNormalPredecessors(T b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return Iterator2Collection.toSet(iterateNormalPredecessors(b));
  }

  @Override
  public IntSet getPredNodeNumbers(T node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * TODO: optimize this.
   */
  @Override
  public IntSet getSuccNodeNumbers(T node) {
    int number = getNumber(node);
    IntSet s = normalEdgeManager.getSuccNodeNumbers(node);
    MutableSparseIntSet result = s == null ? MutableSparseIntSet.makeEmpty() : MutableSparseIntSet.make(s);
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
