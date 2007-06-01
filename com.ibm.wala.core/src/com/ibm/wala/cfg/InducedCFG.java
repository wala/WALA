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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.util.collections.ArrayIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.impl.NodeWithNumber;

/**
 * A ControlFlowGraph computed from a set of SSA instructions
 * 
 * This is a funny CFG ... we assume that there are always fallthru edges, even
 * from throws and returns.
 */
public class InducedCFG extends AbstractCFG {

  private static final boolean DEBUG = false;
  /**
   * A partial map from Instruction -> BasicBlock
   */
  private final BasicBlock[] i2block;

  private final Context context;

  private final IInstruction[] instructions;

  /**
   * TODO: we do not yet support induced CFGS with exception handlers.
   * 
   * @param instructions
   * @throws IllegalArgumentException  if instructions is null
   */
  public InducedCFG(SSAInstruction[] instructions, IMethod method, Context context) {

    super(method);
    if (instructions == null) {
      throw new IllegalArgumentException("instructions is null");
    }
    this.context = context;
    this.instructions = instructions;
    if (DEBUG) {
      Trace.println("compute InducedCFG: " + method);
    }
    i2block = new BasicBlock[instructions.length];
    if (instructions.length == 0) {
      makeEmptyBlocks();
    } else {
      makeBasicBlocks();
    }
    init();
    computeEdges();

    if (DEBUG) {
      try {
        GraphIntegrity.check(this);
      } catch (UnsoundGraphException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }
  }

  @Override
  public int hashCode() {
    return context.hashCode() ^ getMethod().hashCode();
  }

  @Override
  public boolean equals(Object o) {
      return (o instanceof InducedCFG) &&
	  getMethod().equals(((InducedCFG)o).getMethod()) &&
	  context.equals(((InducedCFG)o).context);
  }

  public IInstruction[] getInstructions() {
    return instructions;
  }

  /**
   * Compute outgoing edges in the control flow graph.
   */
  private void computeEdges() {
    for (Iterator it = iterator(); it.hasNext();) {
      BasicBlock b = (BasicBlock) it.next();
      if (b.equals(exit()))
        continue;
      b.computeOutgoingEdges();
    }
  }

  /**
   * Create basic blocks for an empty method
   */
  private void makeEmptyBlocks() {
    BasicBlock b = new BasicBlock(-1);
    addNode(b);
  }

  protected BranchVisitor makeBranchVisitor(boolean[] r) {
    return new BranchVisitor(r);
  }

  protected PEIVisitor makePEIVisitor(boolean[] r) {
    return new PEIVisitor(r);
  }

  /**
   * Walk through the instructions and compute basic block boundaries.
   */
  private void makeBasicBlocks() {
    SSAInstruction[] instructions = (SSAInstruction[]) getInstructions();
    final boolean[] r = new boolean[instructions.length];

    // Compute r so r[i] == true iff instruction i begins a basic block.
    // While doing so count the number of blocks.
    r[0] = true;
    BranchVisitor branchVisitor = makeBranchVisitor(r);
    PEIVisitor peiVisitor = makePEIVisitor(r);
    for (int i = 0; i < instructions.length; i++) {
      if (instructions[i] != null) {
        branchVisitor.setIndex(i);
        instructions[i].visit(branchVisitor);
        // TODO: deal with exception handlers
        peiVisitor.setIndex(i);
        instructions[i].visit(peiVisitor);
      }
    }

    BasicBlock b = null;
    for (int i = 0; i < r.length; i++) {
      if (r[i]) {
        b = new BasicBlock(i);
        addNode(b);

        if (DEBUG) {
          Trace.println("Add basic block " + b);
        }
      }
      i2block[i] = b;
    }
    // allocate the exit block
    BasicBlock exit = new BasicBlock(-1);
    if (DEBUG) {
      Trace.println("Add exit block " + exit);
    }
    addNode(exit);
  }

  /**
   * @author sfink
   * 
   * This visitor identifies basic block boundaries induced by branch
   * instructions.
   */
  public class BranchVisitor extends SSAInstruction.Visitor {
    private boolean[] r;

    protected BranchVisitor(boolean[] r) {
      this.r = r;
    }
    int index = 0;

    void setIndex(int i) {
      index = i;
    }

    @Override
    public void visitGoto(SSAGotoInstruction instruction) {
      Assertions.UNREACHABLE("haven't implemented logic for goto yet.");
      breakBasicBlock();
    }

    @Override
    public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
      Assertions.UNREACHABLE("haven't implemented logic for cbranch yet.");
      breakBasicBlock();
    }

    @Override
    public void visitSwitch(SSASwitchInstruction instruction) {
      Assertions.UNREACHABLE("haven't implemented logic for switch yet.");
//      breakBasicBlock();
//      int[] targets = instruction.getTargets();
//      for (int i = 0; i < targets.length; i++) {
//        r[targets[i]] = true;
//      }
    }

    @Override
    public void visitReturn(SSAReturnInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitThrow(SSAThrowInstruction instruction) {
      breakBasicBlock();
    }

    protected void breakBasicBlock() {
      if (index + 1 < getInstructions().length && !r[index + 1]) {
        r[index + 1] = true;
      }
    }
  }
  // TODO: extend the following to deal with catch blocks. Right now
  // it simply breaks basic blocks at PEIs.
  public class PEIVisitor extends SSAInstruction.Visitor {
    final private boolean[] r;

    protected PEIVisitor(boolean[] r) {
      this.r = r;
    }
    int index = 0;

    void setIndex(int i) {
      index = i;
    }

    protected void breakBasicBlock() {
      if (index + 1 < getInstructions().length && !r[index + 1]) {
        r[index + 1] = true;
      }
    }

    @Override
    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitCheckCast(SSACheckCastInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitGet(SSAGetInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitInvoke(SSAInvokeInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitMonitor(SSAMonitorInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitPut(SSAPutInstruction instruction) {
      breakBasicBlock();
    }

    @Override
    public void visitThrow(com.ibm.wala.ssa.SSAThrowInstruction instruction) {
      breakBasicBlock();
    }
  }

  public IBasicBlock getBlockForInstruction(int index) {
    if (i2block[index] == null) {
      Assertions.productionAssertion(false, "unexpected null for " + index);
    }
    return i2block[index];
  }

  public Set getExceptionHandlers() {
    // TODO: support exception handlers
    return Collections.EMPTY_SET;
  }

  // TODO: share some common parts of this implementation with the ShrikeCFG
  // implementation! right now it's clone-and-owned :(
  private class BasicBlock extends NodeWithNumber implements IBasicBlock {

    @Override
    public boolean equals(Object arg0) {
      if (getClass().equals(arg0.getClass())) {
        BasicBlock other = (BasicBlock) arg0;
        return start == other.start && getMethod().equals(other.getMethod());
      } else {
        return false;
      }
    }
    private final int start;

    BasicBlock(int start) {
      this.start = start;
    }

    /**
     * Add any exceptional edges generated by the last instruction in a basic
     * block.
     * 
     * @param last
     *          the last instruction in a basic block.
     */
    private void addExceptionalEdges(SSAInstruction last) {
      if (last.isPEI()) {
        // we don't currently model catch blocks here ... instead just link
        // to the exit block
        addExceptionalEdgeTo((BasicBlock) exit());
      }
    }

    /**
     * @param b
     */
    private void addNormalEdgeTo(BasicBlock b) {
      addNormalEdge(this, b);
    }

    /**
     * @param b
     */
    private void addExceptionalEdgeTo(BasicBlock b) {
      addExceptionalEdge(this, b);
    }

    /**
     * Method computeOutgoingEdges.
     */
    private void computeOutgoingEdges() {

      if (DEBUG) {
        Trace.println("Block " + this + ": computeOutgoingEdges()");
      }
      // TODO: we don't currently model branches

      SSAInstruction last = (SSAInstruction) getInstructions()[getLastInstructionIndex()];
      addExceptionalEdges(last);
      // this CFG is odd in that we assume fallthru might always
      // happen .. this is because I'm too lazy to code control
      // flow in all method summaries yet.
      if (true) {
        //      if (last.isFallThrough()) {
        if (DEBUG) {
          Trace.println("Add fallthru to " + getNode(getGraphNodeId() + 1));
        }
        addNormalEdgeTo((BasicBlock) getNode(getGraphNodeId() + 1));
      }
      if (last instanceof SSAReturnInstruction) {
        // link each return instrution to the exit block.
        BasicBlock exit = (BasicBlock) exit();
        addNormalEdgeTo(exit);
      }
    }

    public int getFirstInstructionIndex() {
      return start;
    }

    /**
     * Method getLastInstructionIndex.
     * 
     * @return int
     */
    public int getLastInstructionIndex() {
      int exitNumber = InducedCFG.this.getNumber(exit());
      if (getGraphNodeId() == exitNumber) {
        // this is the exit block
        return -2;
      }
      if (getGraphNodeId() == (exitNumber - 1)) {
        // this is the last non-exit block
        return getInstructions().length - 1;
      } else {
        BasicBlock next = (BasicBlock) getNode(getGraphNodeId() + 1);
        return next.getFirstInstructionIndex() - 1;
      }
    }

    public boolean isCatchBlock() {
      // TODO: support induced CFG with catch blocks.
      return false;
    }

    @Override
    public int hashCode() {
      return 1153 * getGraphNodeId() + getMethod().hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "BB[Induced]" + getNumber() + " - " + getMethod().getSignature();
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#isExitBlock()
     */
    public boolean isExitBlock() {
      return getLastInstructionIndex() == -2;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock()
     */
    public boolean isEntryBlock() {
      return getNumber() == 0;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#getMethod()
     */
    public IMethod getMethod() {
      return InducedCFG.this.getMethod();
    }

    public boolean endsInPEI() {
      return getInstructions()[getLastInstructionIndex()].isPEI();
    }

    public boolean endsInReturn() {
      return getInstructions()[getLastInstructionIndex()] instanceof SSAReturnInstruction;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#getNumber()
     */
    public int getNumber() {
      return InducedCFG.this.getNumber(this);
    }
    
    public Iterator<IInstruction> iterator() {
      return new ArrayIterator<IInstruction>(getInstructions(),getFirstInstructionIndex(),getLastInstructionIndex());
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (Iterator it = iterator(); it.hasNext();) {
      BasicBlock bb = (BasicBlock) it.next();
      s.append("BB").append(getNumber(bb)).append("\n");
      for (int j = bb.getFirstInstructionIndex(); j <= bb.getLastInstructionIndex(); j++) {
        s.append("  ").append(j).append("  ").append(getInstructions()[j]).append("\n");
      }

      Iterator<IBasicBlock> succNodes = getSuccNodes(bb);
      while (succNodes.hasNext()) {
        s.append("    -> BB").append(getNumber(succNodes.next())).append("\n");
      }
    }
    return s.toString();
  }

  /**
   * Since this CFG is synthetic, for now we assume the instruction index is the
   * same as the program counter
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getProgramCounter(int)
   */
  public int getProgramCounter(int index) {
    if (getInstructions()[index] instanceof SSAInvokeInstruction) {
      return ((SSAInvokeInstruction) getInstructions()[index]).getCallSite().getProgramCounter();
    } else {
      return index;
    }
  }
  
  
}
