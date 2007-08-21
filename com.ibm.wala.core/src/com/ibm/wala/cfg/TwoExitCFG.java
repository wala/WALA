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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.util.IteratorPlusOne;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.FixedSizeBitVector;
import com.ibm.wala.util.intset.IntSet;

/**
 *
 * This provides a view of a control flow graph with two exits, one for normal
 * returns and one for exceptional exits.
 * 
 * @author sfink
 */
public class TwoExitCFG implements ControlFlowGraph {

  /**
   * DEBUG_LEVEL: 0 No output 1 Print some simple stats and warning information
   * 2 Detailed debugging
   */
  static final int DEBUG_LEVEL = 0;

  /**
   * A "normal" cfg with one exit node
   */
  private final ControlFlowGraph delegate;

  /**
   * A distinguished basic block representing the exceptional exit.
   */
  private final IBasicBlock exceptionalExit = new ExceptionalExitBlock();

  /**
   * Numbers of the "normal" predecessors of the delegate's exit() node
   */
  private FixedSizeBitVector normalPred;

  /**
   * Numbers of the "exceptional" predecessors of the delegate's exit() node
   */
  private FixedSizeBitVector exceptionalPred;

  /**
   * Cached here for efficiency: the "number" of the delegate's exit() node
   */
  private final int delegateExitNumber;
  
  /**
   * compute edges lazily
   */
  private boolean edgesAreComputed = false;

  /**
   * @param delegate
   *          A "normal" cfg with one exit node
   * @throws IllegalArgumentException  if delegate is null
   */
  public TwoExitCFG(ControlFlowGraph delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!(delegate instanceof TwoExitCFG), "bad recursion");
    }
    this.delegate = delegate;
    this.delegateExitNumber = delegate.getNumber(delegate.exit());
  }
  
  private void ensureEdgesReady() {
    if (!edgesAreComputed) {
      computeEdges(delegate);
      edgesAreComputed = true;
    }
  }

  /**
   * @param delegate
   */
  private void computeEdges(ControlFlowGraph delegate) {
    normalPred = (delegate instanceof AbstractCFG) ? ((AbstractCFG) delegate).getNormalToExit() : new FixedSizeBitVector(delegate
        .getMaxNumber() + 1);
    exceptionalPred = (delegate instanceof AbstractCFG) ? ((AbstractCFG) delegate).getExceptionalToExit() : new FixedSizeBitVector(
        delegate.getMaxNumber() + 1);
    if (!(delegate instanceof AbstractCFG)) {
      IInstruction[] instructions = delegate.getInstructions();
      for (Iterator it = delegate.getPredNodes(delegate.exit()); it.hasNext();) {
        IBasicBlock b = (IBasicBlock) it.next();
        if (b.getLastInstructionIndex() >= 0) {
          IInstruction last = instructions[b.getLastInstructionIndex()];
          if (last != null && last.isPEI()) {
            exceptionalPred.set(b.getNumber());
            // occasionally for weird CFGs we may actually fall
            // thru to the exit. TODO: perhaps enforce an invariant
            // that all "normal" predecessors of exit end in return.
            if (!(last instanceof SSAThrowInstruction)) {
              if (b.getLastInstructionIndex() == instructions.length - 1) {
                normalPred.set(b.getNumber());
              }
            }
          } else {
            normalPred.set(b.getNumber());
          }
        }
      }
    }
  }

  public IBasicBlock entry() {
    return delegate.entry();
  }

  public IBasicBlock exit() {
    throw new UnsupportedOperationException("don't call this");
  }

  public BitVector getCatchBlocks() {
    return delegate.getCatchBlocks();
  }

  public IBasicBlock getBlockForInstruction(int index) {
    return delegate.getBlockForInstruction(index);
  }

  public IInstruction[] getInstructions() {
    return delegate.getInstructions();
  }

  public int getProgramCounter(int index) {
    return delegate.getProgramCounter(index);
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
   */
  public void removeNodeAndEdges(IBasicBlock N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public int getNumber(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (N.equals(exceptionalExit)) {
      return getMaxNumber();
    } else {
      return delegate.getNumber(N);
    }
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNode(int)
   */
  public IBasicBlock getNode(int number) {
    return (number == getMaxNumber()) ? exceptionalExit : delegate.getNode(number);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getMaxNumber()
   */
  public int getMaxNumber() {
    return delegate.getMaxNumber() + 1;
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  public Iterator<IBasicBlock> iterator() {
    return new IteratorPlusOne<IBasicBlock>(delegate.iterator(), exceptionalExit);
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes() + 1;
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
   */
  public void addNode(IBasicBlock n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
   */
  public void removeNode(IBasicBlock n) {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
   */
  public boolean containsNode(IBasicBlock N) {
    return delegate.containsNode(N) || N.equals(exceptionalExit);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(java.lang.Object)
   */
  public Iterator<? extends IBasicBlock> getPredNodes(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (N.equals(exceptionalExit)) {
      return delegate.getExceptionalPredecessors(delegate.exit()).iterator();
    } else if (N.equals(delegate.exit())) {
      return delegate.getNormalPredecessors(delegate.exit()).iterator();
    } else {
      return delegate.getPredNodes(N);
    }
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
   */
  public int getPredNodeCount(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    ensureEdgesReady();
    if (N.equals(delegate.exit())) {
      return normalPred.populationCount();
    } else if (N.equals(exceptionalExit)) {
      return exceptionalPred.populationCount();
    } else {
      return delegate.getPredNodeCount(N);
    }
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
   */
  public Iterator<? extends IBasicBlock> getSuccNodes(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (DEBUG_LEVEL > 1) {
      Trace.println("TwoExitCFG: getSuccNodes " + N);
    }
    ensureEdgesReady();
    IBasicBlock bb = N;
    if (N.equals(exceptionalExit)) {
      return EmptyIterator.instance();
    } else if (exceptionalPred.get(bb.getNumber())) {
      if (normalPred.get(bb.getNumber())) {
        return new IteratorPlusOne<IBasicBlock>(delegate.getSuccNodes(N), exceptionalExit);
      } else {
        return new SubstitutionIterator(delegate.getSuccNodes(N));
      }
    } else {
      return delegate.getSuccNodes(N);
    }
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
   */
  public int getSuccNodeCount(IBasicBlock N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    if (N.equals(exceptionalExit)) {
      return 0;
    } else {
      ensureEdgesReady();
      int result = delegate.getSuccNodeCount(N);
      IBasicBlock bb = N;
      if (exceptionalPred.get(bb.getNumber()) && normalPred.get(bb.getNumber())) {
        result++;
      }
      return result;
    }
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object,
   *      java.lang.Object)
   */
  public void addEdge(IBasicBlock src, IBasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  
  public void removeEdge(IBasicBlock src, IBasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public boolean hasEdge(IBasicBlock src, IBasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeAllIncidentEdges(IBasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /**
   * An additional basic block to model exceptional exits
   */
  public final class ExceptionalExitBlock implements ISSABasicBlock {

    public ControlFlowGraph getDelegate() {
      return delegate;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#getFirstInstructionIndex()
     */
    public int getFirstInstructionIndex() {
      Assertions.UNREACHABLE();
      return 0;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#getLastInstructionIndex()
     */
    public int getLastInstructionIndex() {
      return -2;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#isCatchBlock()
     */
    public boolean isCatchBlock() {
      Assertions.UNREACHABLE();
      return false;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#isExitBlock()
     */
    public boolean isExitBlock() {
      return true;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock()
     */
    public boolean isEntryBlock() {
      return false;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#getMethod()
     */
    public IMethod getMethod() {
      return delegate.getMethod();
    }

    /*
     * @see com.ibm.wala.util.graph.INodeWithNumber#getGraphNodeId()
     */
    public int getGraphNodeId() {
      Assertions.UNREACHABLE();
      return 0;
    }

    /*
     * @see com.ibm.wala.util.graph.INodeWithNumber#setGraphNodeId(int)
     */
    public void setGraphNodeId(int number) {
      Assertions.UNREACHABLE();
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object arg0) {
      if (arg0 instanceof ExceptionalExitBlock) {
        ExceptionalExitBlock other = (ExceptionalExitBlock) arg0;
        return delegate.exit().equals(other.getDelegate().exit());
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return delegate.exit().hashCode() * 8467;
    }

    @Override
    public String toString() {
      return "Exceptional Exit[ " + getMethod() + "]";
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#getNumber()
     */
    public int getNumber() {
      return getMaxNumber();
    }

    public Iterator<SSAPhiInstruction> iteratePhis() {
      return EmptyIterator.instance();
    }

    public Iterator<SSAPiInstruction> iteratePis() {
      return EmptyIterator.instance();
    }

    public Iterator<IInstruction> iterator() {
      return EmptyIterator.instance();
    }

    public SSAInstruction getLastInstruction() {
      Assertions.UNREACHABLE();
      return null;
    }

  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getMethod()
   */
  public IMethod getMethod() {
    return delegate.getMethod();
  }

  /**
   * An iterator that substitutes exceptionalExit for exit()
   */
  private class SubstitutionIterator implements Iterator<IBasicBlock> {
    private final Iterator it;

    SubstitutionIterator(Iterator it) {
      this.it = it;
    }

    public void remove() {
      Assertions.UNREACHABLE();
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public IBasicBlock next() {
      IBasicBlock n = (IBasicBlock) it.next();
      if (n.getNumber() == delegateExitNumber) {
        return exceptionalExit;
      } else {
        return n;
      }
    }
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getExceptionalSuccessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (b.equals(exceptionalExit)) {
      return Collections.emptySet();
    } else {
      HashSet<IBasicBlock> c = HashSetFactory.make(getSuccNodeCount(b));
      for (Iterator<IBasicBlock> it = delegate.getExceptionalSuccessors(b).iterator(); it.hasNext(); ) {
        IBasicBlock o = it.next();
        if (o.equals(delegate.exit())) {
          c.add(exceptionalExit);
        } else {
          c.add(o);
        }
      }
      if (DEBUG_LEVEL > 1) {
        Trace.println("Used delegate " + delegate.getClass());
        Trace.println("Exceptional succ of " + b + " " + c);
      }
      return c;
    }
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getNormalSuccessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (b.equals(exceptionalExit)) {
      return Collections.emptySet();
    } else {
      return delegate.getNormalSuccessors(b);
    }
  }

  /**
   * @return A distinguished basic block representing the normal exit
   */
  public IBasicBlock getNormalExit() {
    return delegate.exit();
  }

  /**
   * @return A distinguished basic block representing the exceptional exit
   */
  public IBasicBlock getExceptionalExit() {
    return exceptionalExit;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("Two-Exit CFG");
    result.append("\ndelegate\n" + delegate);
    return result.toString();
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  public Iterator<IBasicBlock> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<IBasicBlock>(s, this);
  }

  public void removeIncomingEdges(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  public void removeOutgoingEdges(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  public Collection<IBasicBlock> getExceptionalPredecessors(IBasicBlock b) {
    Assertions.UNREACHABLE();
    return null;
  }

  public Collection<IBasicBlock> getNormalPredecessors(IBasicBlock b) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getSuccNodeNumbers(IBasicBlock node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getPredNodeNumbers(IBasicBlock node) {
    Assertions.UNREACHABLE();
    return null;
  }

  public ControlFlowGraph getDelegate() {
    return delegate;
  }
}
