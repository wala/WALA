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

package com.ibm.wala.ssa.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * A view of a control flow graph where each basic block corresponds to exactly one SSA instruction index.
 *
 * Prototype: Not terribly efficient.
 */
public class ExplodedControlFlowGraph implements ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> {

  private final static int ENTRY_INDEX = -1;

  private final static int EXIT_INDEX = -2;

  private final IR ir;

  /**
   * The ith element of this vector is the basic block holding instruction i. this basic block has number i+1.
   */
  private final SimpleVector<IExplodedBasicBlock> normalNodes = new SimpleVector<>();

  private final Collection<IExplodedBasicBlock> allNodes = HashSetFactory.make();

  private final IExplodedBasicBlock entry;

  private final IExplodedBasicBlock exit;

  private ExplodedControlFlowGraph(IR ir) {
    assert ir != null;
    this.ir = ir;
    this.entry = new ExplodedBasicBlock(ENTRY_INDEX, null);
    this.exit = new ExplodedBasicBlock(EXIT_INDEX, null);
    createNodes();
  }

  private void createNodes() {
    allNodes.add(entry);
    allNodes.add(exit);
    for (ISSABasicBlock b : ir.getControlFlowGraph()) {
      for (int i = b.getFirstInstructionIndex(); i <= b.getLastInstructionIndex(); i++) {
        IExplodedBasicBlock bb = new ExplodedBasicBlock(i, b);
        normalNodes.set(i, bb);
        allNodes.add(bb);
      }
    }
  }

  public static ExplodedControlFlowGraph make(IR ir) {
    if (ir == null) {
      throw new IllegalArgumentException("ir == null");
    }
    return new ExplodedControlFlowGraph(ir);
  }

  @Override
  public IExplodedBasicBlock entry() {
    return entry;
  }

  @Override
  public IExplodedBasicBlock exit() {
    return exit;
  }

  @Override
  public IExplodedBasicBlock getBlockForInstruction(int index) {
    return normalNodes.get(index);
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getCatchBlocks()
   */
  @Override
  public BitVector getCatchBlocks() {
    BitVector original = ir.getControlFlowGraph().getCatchBlocks();
    BitVector result = new BitVector();
    for (int i = 0; i <= original.max(); i++) {
      if (original.get(i)) {
        result.set(i + 1);
      }
    }
    return result;
  }

  @Override
  public Collection<IExplodedBasicBlock> getExceptionalPredecessors(IExplodedBasicBlock bb) {
    ExplodedBasicBlock eb = (ExplodedBasicBlock) bb;
    assert eb != null;
    if (eb.equals(entry)) {
      return Collections.emptySet();
    }
    if (eb.isExitBlock() || eb.instructionIndex == eb.original.getFirstInstructionIndex()) {
      List<IExplodedBasicBlock> result = new ArrayList<>();
      for (ISSABasicBlock s : ir.getControlFlowGraph().getExceptionalPredecessors(eb.original)) {
        assert normalNodes.get(s.getLastInstructionIndex()) != null;
        result.add(normalNodes.get(s.getLastInstructionIndex()));
      }
      return result;
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public List<IExplodedBasicBlock> getExceptionalSuccessors(IExplodedBasicBlock bb) {
    ExplodedBasicBlock eb = (ExplodedBasicBlock) bb;
    assert eb != null;
    if (eb.equals(exit)) {
      return Collections.emptyList();
    }
    if (eb.isEntryBlock() || eb.instructionIndex == eb.original.getLastInstructionIndex()) {
      List<IExplodedBasicBlock> result = new ArrayList<>();
      ISSABasicBlock orig = eb.original;
      if (eb.isEntryBlock() && orig == null) {
        orig = ir.getControlFlowGraph().entry();
      }

      for (ISSABasicBlock s : ir.getControlFlowGraph().getExceptionalSuccessors(orig)) {
        if (s.equals(ir.getControlFlowGraph().exit())) {
          result.add(exit());
        } else {
          assert normalNodes.get(s.getFirstInstructionIndex()) != null;
          result.add(normalNodes.get(s.getFirstInstructionIndex()));
        }
      }
      return result;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public SSAInstruction[] getInstructions() {
    return ir.getInstructions();
  }

  @Override
  public IMethod getMethod() throws UnimplementedError {
    return ir.getMethod();
  }

  @Override
  public Collection<IExplodedBasicBlock> getNormalPredecessors(IExplodedBasicBlock bb) {
    ExplodedBasicBlock eb = (ExplodedBasicBlock) bb;
    assert eb != null;
    if (eb.equals(entry)) {
      return Collections.emptySet();
    }
    if (eb.isExitBlock() || eb.instructionIndex == eb.original.getFirstInstructionIndex()) {
      List<IExplodedBasicBlock> result = new ArrayList<>();
      for (ISSABasicBlock s : ir.getControlFlowGraph().getNormalPredecessors(eb.original)) {
        if (s.equals(ir.getControlFlowGraph().entry())) {
          if (s.getLastInstructionIndex() >= 0) {
            assert normalNodes.get(s.getLastInstructionIndex()) != null;
            result.add(normalNodes.get(s.getLastInstructionIndex()));
          } else {
            result.add(entry());
          }
        } else {
          assert normalNodes.get(s.getLastInstructionIndex()) != null;
          result.add(normalNodes.get(s.getLastInstructionIndex()));
        }
      }
      return result;
    } else {
      assert normalNodes.get(eb.instructionIndex - 1) != null;
      return Collections.singleton(normalNodes.get(eb.instructionIndex - 1));
    }
  }

  @Override
  public Collection<IExplodedBasicBlock> getNormalSuccessors(IExplodedBasicBlock bb) {
    ExplodedBasicBlock eb = (ExplodedBasicBlock) bb;
    assert eb != null;
    if (eb.equals(exit)) {
      return Collections.emptySet();
    }
    if (eb.isEntryBlock()) {
      return Collections.singleton(normalNodes.get(0));
    }
    if (eb.instructionIndex == eb.original.getLastInstructionIndex()) {
      List<IExplodedBasicBlock> result = new ArrayList<>();
      for (ISSABasicBlock s : ir.getControlFlowGraph().getNormalSuccessors(eb.original)) {
        if (s.equals(ir.getControlFlowGraph().exit())) {
          result.add(exit());
        } else {
          assert normalNodes.get(s.getFirstInstructionIndex()) != null;
          result.add(normalNodes.get(s.getFirstInstructionIndex()));
        }
      }
      return result;
    } else {
      assert normalNodes.get(eb.instructionIndex + 1) != null;
      return Collections.singleton(normalNodes.get(eb.instructionIndex + 1));
    }
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getProgramCounter(int)
   */
  @Override
  public int getProgramCounter(int index) throws UnimplementedError {
    return ir.getControlFlowGraph().getProgramCounter(index);
  }

  @Override
  public void removeNodeAndEdges(IExplodedBasicBlock N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addNode(IExplodedBasicBlock n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsNode(IExplodedBasicBlock N) {
    return allNodes.contains(N);
  }

  @Override
  public int getNumberOfNodes() {
    return allNodes.size();
  }

  @Override
  public Iterator<IExplodedBasicBlock> iterator() {
    return allNodes.iterator();
  }

  @Override
  public void removeNode(IExplodedBasicBlock n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addEdge(IExplodedBasicBlock src, IExplodedBasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getPredNodeCount(IExplodedBasicBlock bb) throws IllegalArgumentException {
    ExplodedBasicBlock eb = (ExplodedBasicBlock) bb;
    if (eb == null) {
      throw new IllegalArgumentException("eb == null");
    }
    if (eb.isEntryBlock()) {
      return 0;
    }
    if (eb.isExitBlock()) {
      return ir.getControlFlowGraph().getPredNodeCount(ir.getControlFlowGraph().exit());
    }
    if (eb.instructionIndex == eb.original.getFirstInstructionIndex()) {
      if (eb.original.isEntryBlock()) {
        return 1;
      } else {
        return ir.getControlFlowGraph().getPredNodeCount(eb.original);
      }
    } else {
      return 1;
    }
  }

  @Override
  public Iterator<IExplodedBasicBlock> getPredNodes(IExplodedBasicBlock bb) throws IllegalArgumentException {
    ExplodedBasicBlock eb = (ExplodedBasicBlock) bb;
    if (eb == null) {
      throw new IllegalArgumentException("eb == null");
    }
    if (eb.isEntryBlock()) {
      return EmptyIterator.instance();
    }
    ISSABasicBlock original = eb.isExitBlock() ? ir.getControlFlowGraph().exit() : eb.original;
    if (eb.isExitBlock() || eb.instructionIndex == eb.original.getFirstInstructionIndex()) {
      List<IExplodedBasicBlock> result = new ArrayList<>();
      if (eb.original != null && eb.original.isEntryBlock()) {
        result.add(entry);
      }
      for (ISSABasicBlock s : Iterator2Iterable.make(ir.getControlFlowGraph().getPredNodes(original))) {
        if (s.isEntryBlock()) {
          // it's possible for an entry block to have instructions; in this case, add
          // the exploded basic block for the last instruction in the entry block
          if (s.getLastInstructionIndex() >= 0) {
            result.add(normalNodes.get(s.getLastInstructionIndex()));
          } else {
            result.add(entry);
          }
        } else {
          assert normalNodes.get(s.getLastInstructionIndex()) != null;
          result.add(normalNodes.get(s.getLastInstructionIndex()));
        }
      }
      return result.iterator();
    } else {
      assert normalNodes.get(eb.instructionIndex - 1) != null;
      return NonNullSingletonIterator.make(normalNodes.get(eb.instructionIndex - 1));
    }
  }

  @Override
  public int getSuccNodeCount(IExplodedBasicBlock N) throws UnimplementedError {
    return Iterator2Collection.toSet(getSuccNodes(N)).size();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(java.lang.Object)
   */
  @Override
  public Iterator<IExplodedBasicBlock> getSuccNodes(IExplodedBasicBlock bb) {
    ExplodedBasicBlock eb = (ExplodedBasicBlock) bb;
    assert eb != null;
    if (eb.isExitBlock()) {
      return EmptyIterator.instance();
    }
    if (eb.isEntryBlock()) {
      IExplodedBasicBlock z = normalNodes.get(0);
      return z == null ? EmptyIterator.<IExplodedBasicBlock> instance() : NonNullSingletonIterator.make(z);
    }
    if (eb.instructionIndex == eb.original.getLastInstructionIndex()) {
      List<IExplodedBasicBlock> result = new ArrayList<>();
      for (ISSABasicBlock s : Iterator2Iterable.make(ir.getControlFlowGraph().getSuccNodes(eb.original))) {
        if (s.equals(ir.getControlFlowGraph().exit())) {
          result.add(exit());
        } else {
          // there can be a weird corner case where a void method without a
          // return statement
          // can have trailing empty basic blocks with no instructions. ignore
          // these.
          if (normalNodes.get(s.getFirstInstructionIndex()) != null) {
            result.add(normalNodes.get(s.getFirstInstructionIndex()));
          }
        }
      }
      return result.iterator();
    } else {
      assert normalNodes.get(eb.instructionIndex + 1) != null;
      return NonNullSingletonIterator.make(normalNodes.get(eb.instructionIndex + 1));
    }
  }

  @Override
  public boolean hasEdge(IExplodedBasicBlock src, IExplodedBasicBlock dst) throws UnimplementedError {
    for (IExplodedBasicBlock succ : Iterator2Iterable.make(getSuccNodes(src))) {
      if (succ == dst) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void removeAllIncidentEdges(IExplodedBasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void removeEdge(IExplodedBasicBlock src, IExplodedBasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeIncomingEdges(IExplodedBasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeOutgoingEdges(IExplodedBasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getMaxNumber() {
    return getNumberOfNodes() - 1;
  }

  @Override
  public IExplodedBasicBlock getNode(int number) {
    if (number == 0) {
      return entry();
    } else if (number == getNumberOfNodes() - 1) {
      return exit();
    } else {
      return normalNodes.get(number - 1);
    }
  }

  @Override
  public int getNumber(IExplodedBasicBlock n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n == null");
    }
    return n.getNumber();
  }

  @Override
  public Iterator<IExplodedBasicBlock> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public IntSet getPredNodeNumbers(IExplodedBasicBlock node) {
    MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
    for (IExplodedBasicBlock ebb : Iterator2Iterable.make(getPredNodes(node))) {
      result.add(getNumber(ebb));
    }
    return result;
  }

  @Override
  public IntSet getSuccNodeNumbers(IExplodedBasicBlock node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * A basic block with exactly one normal instruction (which may be null), corresponding to a single instruction index in the SSA
   * instruction array.
   *
   * The block may also have phis.
   */
  private class ExplodedBasicBlock implements IExplodedBasicBlock {

    private final int instructionIndex;

    private final ISSABasicBlock original;

    public ExplodedBasicBlock(int instructionIndex, ISSABasicBlock original) {
      this.instructionIndex = instructionIndex;
      this.original = original;
    }

    @SuppressWarnings("unused")
    public ExplodedControlFlowGraph getExplodedCFG() {
      return ExplodedControlFlowGraph.this;
    }

    @Override
    public Iterator<TypeReference> getCaughtExceptionTypes() {
      if (original instanceof ExceptionHandlerBasicBlock) {
        ExceptionHandlerBasicBlock eb = (ExceptionHandlerBasicBlock) original;
        return eb.getCaughtExceptionTypes();
      } else {
        return EmptyIterator.instance();
      }
    }

    @Override
    public int getFirstInstructionIndex() {
      return instructionIndex;
    }

    @Override
    public int getLastInstructionIndex() {
      return instructionIndex;
    }

    @Override
    public IMethod getMethod() {
      return ExplodedControlFlowGraph.this.getMethod();
    }

    @Override
    public int getNumber() {
      if (isEntryBlock()) {
        return 0;
      } else if (isExitBlock()) {
        return getNumberOfNodes() - 1;
      } else {
        return instructionIndex + 1;
      }
    }

    @Override
    public boolean isCatchBlock() {
      if (original == null) {
        return false;
      }
      return (original.isCatchBlock() && instructionIndex == original.getFirstInstructionIndex());
    }

    @Override
    public SSAGetCaughtExceptionInstruction getCatchInstruction() {
      if (!(original instanceof ExceptionHandlerBasicBlock)) {
        throw new IllegalArgumentException("block does not represent an exception handler");
      }
      ExceptionHandlerBasicBlock e = (ExceptionHandlerBasicBlock) original;
      return e.getCatchInstruction();
    }

    @Override
    public boolean isEntryBlock() {
      return instructionIndex == ENTRY_INDEX;
    }

    @Override
    public boolean isExitBlock() {
      return instructionIndex == EXIT_INDEX;
    }

    @Override
    public int getGraphNodeId() {
      return getNumber();
    }

    @Override
    public void setGraphNodeId(int number) {
      Assertions.UNREACHABLE();

    }

    @Override
    public Iterator<SSAInstruction> iterator() {
      if (isEntryBlock() || isExitBlock() || getInstruction() == null) {
        return EmptyIterator.instance();
      } else {
        return NonNullSingletonIterator.make(getInstruction());
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + instructionIndex;
      result = prime * result + ((original == null) ? 0 : original.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final ExplodedBasicBlock other = (ExplodedBasicBlock) obj;
      if (instructionIndex != other.instructionIndex)
        return false;
      if (original == null) {
        if (other.original != null)
          return false;
      } else if (!original.equals(other.original))
        return false;
      return true;
    }

    @Override
    public SSAInstruction getInstruction() {
      if (isEntryBlock() || isExitBlock()) {
        return null;
      } else {
        return ir.getInstructions()[instructionIndex];
      }
    }

    @Override
    public SSAInstruction getLastInstruction() {
      if (getLastInstructionIndex() < 0) {
        return null;
      } else {
        return ir.getInstructions()[getLastInstructionIndex()];
      }
    }

    @Override
    public Iterator<SSAPhiInstruction> iteratePhis() {
      if (isEntryBlock() || isExitBlock() || instructionIndex != original.getFirstInstructionIndex()) {
        return EmptyIterator.instance();
      } else {
        return original.iteratePhis();
      }
    }

    @Override
    public Iterator<SSAPiInstruction> iteratePis() {
      if (isEntryBlock() || isExitBlock() || instructionIndex != original.getLastInstructionIndex()) {
        return EmptyIterator.instance();
      } else {
        return original.iteratePis();
      }
    }

    @Override
    public String toString() {
      if (isEntryBlock()) {
        return "ExplodedBlock[" + getNumber() + "](entry:" + getMethod() + ")";
      }
      if (isExitBlock()) {
        return "ExplodedBlock[" + getNumber() + "](exit:" + getMethod() + ")";
      }
      return "ExplodedBlock[" + getNumber() + "](original:" + original + ")";
    }

    @Override
    public int getOriginalNumber() {
      return original.getNumber();
    }
  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (IExplodedBasicBlock bb : this) {
      s.append("BB").append(getNumber(bb)).append("\n");

      Iterator<? extends IExplodedBasicBlock> succNodes = getSuccNodes(bb);
      while (succNodes.hasNext()) {
        s.append("    -> BB").append(getNumber(succNodes.next())).append("\n");
      }
    }
    return s.toString();
  }

  public IR getIR() {
    return ir;
  }

}
