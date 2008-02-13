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
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * A view of a control flow graph where each basic block corresponds to exactly
 * one SSA instruction index.
 * 
 * Prototype: Not terribly efficient.
 */
public class ExplodedControlFlowGraph implements ControlFlowGraph<ExplodedControlFlowGraph.ExplodedBasicBlock> {

  private final IR ir;

  /**
   * The ith element of this vector is the basic block holding instruction i.
   * this basic block has number i+1.
   */
  private final SimpleVector<ExplodedBasicBlock> normalNodes = new SimpleVector<ExplodedBasicBlock>();

  private final Collection<ExplodedBasicBlock> allNodes = HashSetFactory.make();

  private final ExplodedBasicBlock entry;

  private final ExplodedBasicBlock exit;

  private ExplodedControlFlowGraph(IR ir) {
    assert ir != null;
    this.ir = ir;
    this.entry = new ExplodedBasicBlock(-2, ir.getControlFlowGraph().entry());
    this.exit = new ExplodedBasicBlock(-2, ir.getControlFlowGraph().exit());
    createNodes();
  }

  private void createNodes() {
    allNodes.add(entry);
    allNodes.add(exit);
    for (ISSABasicBlock b : ir.getControlFlowGraph()) {
      for (int i = b.getFirstInstructionIndex(); i <= b.getLastInstructionIndex(); i++) {
        ExplodedBasicBlock bb = new ExplodedBasicBlock(i, b);
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

  public ExplodedBasicBlock entry() {
    return entry;
  }

  public ExplodedBasicBlock exit() {
    return exit;
  }

  public ExplodedBasicBlock getBlockForInstruction(int index) {
    return normalNodes.get(index);
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getCatchBlocks()
   */
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

  public Collection<ExplodedBasicBlock> getExceptionalPredecessors(ExplodedBasicBlock eb) {
    assert eb != null;
    if (eb.equals(entry)) {
      return Collections.emptySet();
    }
    if (eb.isExitBlock() || eb.instructionIndex == eb.original.getFirstInstructionIndex()) {
      List<ExplodedBasicBlock> result = new ArrayList<ExplodedBasicBlock>();
      for (ISSABasicBlock s : ir.getControlFlowGraph().getExceptionalPredecessors(eb.original)) {
        assert normalNodes.get(s.getLastInstructionIndex()) != null;
        result.add(normalNodes.get(s.getLastInstructionIndex()));
      }
      return result;
    } else {
      return Collections.emptySet();
    }
  }

  public List<ExplodedBasicBlock> getExceptionalSuccessors(ExplodedBasicBlock eb) {
    assert eb != null;
    if (eb.equals(exit)) {
      return Collections.emptyList();
    }
    if (eb.isEntryBlock() || eb.instructionIndex == eb.original.getLastInstructionIndex()) {
      List<ExplodedBasicBlock> result = new ArrayList<ExplodedBasicBlock>();
      for (ISSABasicBlock s : ir.getControlFlowGraph().getExceptionalSuccessors(eb.original)) {
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

  public IInstruction[] getInstructions() {
    return ir.getInstructions();
  }

  public IMethod getMethod() throws UnimplementedError {
    return ir.getMethod();
  }

  public Collection<ExplodedBasicBlock> getNormalPredecessors(ExplodedBasicBlock eb) {
    assert eb != null;
    if (eb.equals(entry)) {
      return Collections.emptySet();
    }
    if (eb.isExitBlock() || eb.instructionIndex == eb.original.getFirstInstructionIndex()) {
      List<ExplodedBasicBlock> result = new ArrayList<ExplodedBasicBlock>();
      for (ISSABasicBlock s : ir.getControlFlowGraph().getNormalPredecessors(eb.original)) {
        if (s.equals(ir.getControlFlowGraph().entry())) {
          result.add(entry());
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

  public Collection<ExplodedBasicBlock> getNormalSuccessors(ExplodedBasicBlock eb) {
    assert eb != null;
    if (eb.equals(exit)) {
      return Collections.emptySet();
    }
    if (eb.isEntryBlock() || eb.instructionIndex == eb.original.getLastInstructionIndex()) {
      List<ExplodedBasicBlock> result = new ArrayList<ExplodedBasicBlock>();
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
  public int getProgramCounter(int index) throws UnimplementedError {
    return ir.getControlFlowGraph().getProgramCounter(index);
  }

  public void removeNodeAndEdges(ExplodedBasicBlock N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void addNode(ExplodedBasicBlock n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public boolean containsNode(ExplodedBasicBlock N) {
    return allNodes.contains(N);
  }

  public int getNumberOfNodes() {
    return allNodes.size();
  }

  public Iterator<ExplodedBasicBlock> iterator() {
    return allNodes.iterator();
  }

  public void removeNode(ExplodedBasicBlock n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void addEdge(ExplodedBasicBlock src, ExplodedBasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public int getPredNodeCount(ExplodedBasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    if (b.isEntryBlock()) {
      return 0;
    }
    if (b.equals(exit) || b.instructionIndex == b.original.getFirstInstructionIndex()) {
      return ir.getControlFlowGraph().getPredNodeCount(b.original);
    } else {
      return 1;
    }
  }

  public Iterator<? extends ExplodedBasicBlock> getPredNodes(ExplodedBasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    if (b.isEntryBlock()) {
      return EmptyIterator.instance();
    }
    if (b.equals(exit) || b.instructionIndex == b.original.getFirstInstructionIndex()) {
      List<ExplodedBasicBlock> result = new ArrayList<ExplodedBasicBlock>();
      for (Iterator<ISSABasicBlock> it = ir.getControlFlowGraph().getPredNodes(b.original); it.hasNext();) {
        ISSABasicBlock s = it.next();
        if (s.isEntryBlock()) {
          result.add(entry);
        } else {
          assert normalNodes.get(s.getLastInstructionIndex()) != null;
          result.add(normalNodes.get(s.getLastInstructionIndex()));
        }
      }
      return result.iterator();
    } else {
      assert normalNodes.get(b.instructionIndex - 1) != null;
      return NonNullSingletonIterator.make(normalNodes.get(b.instructionIndex - 1));
    }
  }

  public int getSuccNodeCount(ExplodedBasicBlock N) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return 0;
  }

  public Iterator<? extends ExplodedBasicBlock> getSuccNodes(ExplodedBasicBlock b) {
    assert b != null;
    if (b.equals(exit)) {
      return EmptyIterator.instance();
    }
    if (b.isEntryBlock() || b.instructionIndex == b.original.getLastInstructionIndex()) {
      List<ExplodedBasicBlock> result = new ArrayList<ExplodedBasicBlock>();
      for (Iterator<ISSABasicBlock> it = ir.getControlFlowGraph().getSuccNodes(b.original); it.hasNext();) {
        ISSABasicBlock s = it.next();
        if (s.equals(ir.getControlFlowGraph().exit())) {
          result.add(exit());
        } else {
          assert normalNodes.get(s.getFirstInstructionIndex()) != null;
          result.add(normalNodes.get(s.getFirstInstructionIndex()));
        }
      }
      return result.iterator();
    } else {
      assert normalNodes.get(b.instructionIndex + 1) != null;
      return NonNullSingletonIterator.make(normalNodes.get(b.instructionIndex + 1));
    }
  }

  public boolean hasEdge(ExplodedBasicBlock src, ExplodedBasicBlock dst) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  public void removeAllIncidentEdges(ExplodedBasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();

  }

  public void removeEdge(ExplodedBasicBlock src, ExplodedBasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeIncomingEdges(ExplodedBasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public void removeOutgoingEdges(ExplodedBasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public int getMaxNumber() {
    return getNumberOfNodes() - 1;
  }

  public ExplodedBasicBlock getNode(int number) {
    if (number == 0) {
      return entry();
    } else if (number == getNumberOfNodes() - 1) {
      return exit();
    } else {
      return normalNodes.get(number - 1);
    }
  }

  public int getNumber(ExplodedBasicBlock n) throws IllegalArgumentException {
    if (n == null) {
      throw new IllegalArgumentException("n == null");
    }
    return n.getNumber();
  }

  public Iterator<ExplodedBasicBlock> iterateNodes(IntSet s) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getPredNodeNumbers(ExplodedBasicBlock node) {
    MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
    for (Iterator<? extends ExplodedBasicBlock> it = getPredNodes(node); it.hasNext();) {
      result.add(getNumber(it.next()));
    }
    return result;
  }

  public IntSet getSuccNodeNumbers(ExplodedBasicBlock node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * A basic block with exactly one normal instruction (which may be null),
   * corresponding to a single instruction index in the SSA instruction array.
   * 
   * The block may also have phis.
   */
  public class ExplodedBasicBlock implements ISSABasicBlock {

    private final int instructionIndex;

    private final ISSABasicBlock original;

    public ExplodedBasicBlock(int instructionIndex, ISSABasicBlock original) {
      this.instructionIndex = instructionIndex;
      this.original = original;
      assert original != null;
    }
    
    public ExplodedControlFlowGraph getExplodedCFG() {
      return ExplodedControlFlowGraph.this;
    }
    
    public Iterator<TypeReference> getCaughtExceptionTypes() {
      if (original instanceof ExceptionHandlerBasicBlock) {
        ExceptionHandlerBasicBlock eb = (ExceptionHandlerBasicBlock)original;
        return eb.getCaughtExceptionTypes();
      } else {
        return EmptyIterator.instance();
      }
    }

    public int getFirstInstructionIndex() {
      return instructionIndex;
    }

    public int getLastInstructionIndex() {
      return instructionIndex;
    }
    
    public IMethod getMethod() {
      return ExplodedControlFlowGraph.this.getMethod();
    }

    public int getNumber() {
      if (isEntryBlock()) {
        return 0;
      } else if (isExitBlock()) {
        return getNumberOfNodes() - 1;
      } else {
        return instructionIndex + 1;
      }
    }

    public boolean isCatchBlock() {
      return (original.isCatchBlock() && instructionIndex == original.getFirstInstructionIndex());
    }

    public SSAGetCaughtExceptionInstruction getCatchInstruction() {
      assert (original instanceof ExceptionHandlerBasicBlock);
      ExceptionHandlerBasicBlock e = (ExceptionHandlerBasicBlock) original;
      return e.getCatchInstruction();
    }

    public boolean isEntryBlock() {
      return original.isEntryBlock();
    }

    public boolean isExitBlock() {
      return original.isExitBlock();
    }

    public int getGraphNodeId() {
      Assertions.UNREACHABLE();
      return 0;
    }

    public void setGraphNodeId(int number) {
      Assertions.UNREACHABLE();

    }

    public Iterator<IInstruction> iterator() {
      if (isEntryBlock() || isExitBlock() || getInstruction() == null) {
        return EmptyIterator.instance();
      } else {
        return NonNullSingletonIterator.make((IInstruction) getInstruction());
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

    public SSAInstruction getInstruction() {
      if (isEntryBlock() || isExitBlock()) {
        return null;
      } else {
        return ir.getInstructions()[instructionIndex];
      }
    }

    public SSAInstruction getLastInstruction() {
      if (getLastInstructionIndex() < 0) {
        return null;
      } else {
        return ir.getInstructions()[getLastInstructionIndex()];
      }
    }

    public Iterator<SSAPhiInstruction> iteratePhis() {
      if (isEntryBlock() || isExitBlock() || instructionIndex != original.getFirstInstructionIndex()) {
        return EmptyIterator.instance();
      } else {
        return original.iteratePhis();
      }
    }

    public Iterator<SSAPiInstruction> iteratePis() {
      if (isEntryBlock() || isExitBlock() || instructionIndex != original.getLastInstructionIndex()) {
        return EmptyIterator.instance();
      } else {
        return original.iteratePis();
      }
    }

    @Override
    public String toString() {
      return "ExplodedBlock[" + getNumber() + "](original:" + original.toString() + ")";
    }
  }
  
  @Override
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (Iterator<ExplodedBasicBlock> it = iterator(); it.hasNext();) {
      ExplodedBasicBlock bb = it.next();
      s.append("BB").append(getNumber(bb)).append("\n");

      Iterator<? extends ExplodedBasicBlock> succNodes = getSuccNodes(bb);
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