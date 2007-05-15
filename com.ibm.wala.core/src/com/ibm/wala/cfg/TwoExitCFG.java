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
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.util.IteratorPlusOne;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
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
   */
  public TwoExitCFG(ControlFlowGraph delegate) {
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

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#entry()
   */
  public IBasicBlock entry() {
    return delegate.entry();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#exit()
   */
  public IBasicBlock exit() {
    Assertions.UNREACHABLE("Don't call me");
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getCatchBlocks()
   */
  public BitVector getCatchBlocks() {
    return delegate.getCatchBlocks();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getBlockForInstruction(int)
   */
  public IBasicBlock getBlockForInstruction(int index) {
    return delegate.getBlockForInstruction(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getInstructions()
   */
  public IInstruction[] getInstructions() {
    return delegate.getInstructions();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getProgramCounter(int)
   */
  public int getProgramCounter(int index) {
    return delegate.getProgramCounter(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#removeNodeAndEdges(java.lang.Object)
   */
  public void removeNodeAndEdges(IBasicBlock N) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNumber(java.lang.Object)
   */
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
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNode(int)
   */
  public IBasicBlock getNode(int number) {
    return (number == getMaxNumber()) ? exceptionalExit : delegate.getNode(number);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getMaxNumber()
   */
  public int getMaxNumber() {
    return delegate.getMaxNumber() + 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  public Iterator<IBasicBlock> iterator() {
    return new IteratorPlusOne<IBasicBlock>(delegate.iterator(), exceptionalExit);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes() + 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
   */
  public void addNode(IBasicBlock n) {
    Assertions.UNREACHABLE();

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#removeNode(java.lang.Object)
   */
  public void removeNode(IBasicBlock n) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(java.lang.Object)
   */
  public boolean containsNode(IBasicBlock N) {
    return delegate.containsNode(N) || N.equals(exceptionalExit);
  }

  /*
   * (non-Javadoc)
   * 
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
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(java.lang.Object)
   */
  public int getPredNodeCount(IBasicBlock N) {
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
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
   */
  public Iterator<? extends IBasicBlock> getSuccNodes(IBasicBlock N) {
    if (DEBUG_LEVEL > 1) {
      Trace.println("TwoExitCFG: getSuccNodes " + N);
    }
    ensureEdgesReady();
    IBasicBlock bb = (IBasicBlock) N;
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
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(java.lang.Object)
   */
  public int getSuccNodeCount(IBasicBlock N) {
    if (N.equals(exceptionalExit)) {
      return 0;
    } else {
      ensureEdgesReady();
      int result = delegate.getSuccNodeCount(N);
      IBasicBlock bb = (IBasicBlock) N;
      if (exceptionalPred.get(bb.getNumber()) && normalPred.get(bb.getNumber())) {
        result++;
      }
      return result;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(java.lang.Object,
   *      java.lang.Object)
   */
  public void addEdge(IBasicBlock src, IBasicBlock dst) {
    Assertions.UNREACHABLE();
  }
  
  public void removeEdge(IBasicBlock src, IBasicBlock dst) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   */
  public boolean hasEdge(IBasicBlock src, IBasicBlock dst) {
    Assertions.UNREACHABLE();
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(java.lang.Object)
   */
  public void removeAllIncidentEdges(IBasicBlock node) {
    Assertions.UNREACHABLE();

  }

  /**
   * @author sfink
   * 
   * An additional basic block to model exceptional exits
   */
  public final class ExceptionalExitBlock implements ISSABasicBlock {

    public ControlFlowGraph getDelegate() {
      return delegate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#getFirstInstructionIndex()
     */
    public int getFirstInstructionIndex() {
      Assertions.UNREACHABLE();
      return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#getLastInstructionIndex()
     */
    public int getLastInstructionIndex() {
      return -2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#isCatchBlock()
     */
    public boolean isCatchBlock() {
      Assertions.UNREACHABLE();
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#isExitBlock()
     */
    public boolean isExitBlock() {
      return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock()
     */
    public boolean isEntryBlock() {
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#getMethod()
     */
    public IMethod getMethod() {
      return delegate.getMethod();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.INodeWithNumber#getGraphNodeId()
     */
    public int getGraphNodeId() {
      Assertions.UNREACHABLE();
      return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.INodeWithNumber#setGraphNodeId(int)
     */
    public void setGraphNodeId(int number) {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object arg0) {
      if (arg0 instanceof ExceptionalExitBlock) {
        ExceptionalExitBlock other = (ExceptionalExitBlock) arg0;
        return delegate.exit().equals(other.getDelegate().exit());
      } else {
        return false;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return delegate.exit().hashCode() * 8467;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return "Exceptional Exit[ " + getMethod() + "]";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#getNumber()
     */
    public int getNumber() {
      return getMaxNumber();
    }

    public Iterator iteratePhis() {
      return Collections.EMPTY_LIST.iterator();
    }

    public Iterator iteratePis() {
      return Collections.EMPTY_LIST.iterator();
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
   * (non-Javadoc)
   * 
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

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      Assertions.UNREACHABLE();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return it.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
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
   * (non-Javadoc)
   * 
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
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getNormalSuccessors(IBasicBlock b) {
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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer result = new StringBuffer("Two-Exit CFG");
    result.append("\ndelegate\n" + delegate);
    return result.toString();
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

  public Collection<IBasicBlock> getExceptionalPredecessors(IBasicBlock b) {
    Assertions.UNREACHABLE();
    return null;
  }

  public Collection<IBasicBlock> getNormalPredecessors(IBasicBlock b) {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getSuccNodeNumbers(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getPredNodeNumbers(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * @return Returns the delegate.
   */
  public ControlFlowGraph getDelegate() {
    return delegate;
  }
}
