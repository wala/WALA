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
package com.ibm.wala.cast.ir.ssa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAOptions.DefaultValues;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.collections.ArrayIterator;
import com.ibm.wala.util.collections.IntStack;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.dominators.DominanceFrontiers;

/**
 * Abstract core of traditional SSA conversion (Cytron et al.).
 * 
 * This implementation is abstract in the sense that it is designed to work over
 * the instructions and CFG of a Domo IR, but it is abstract with respect to
 * several integral portions of the traditional algorithm:
 * <UL>
 * <LI> The notion of uses and defs of a given instruction.
 * <LI> Assignments (<def> := <use>) that are be copy-propagated away
 * <LI> Which values are constants---i.e. have no definition.
 * <LI> Any value numbers to be skipped during SSA construction
 * <LI> Special initialization and exit block processing.
 * </UL>
 * 
 * @author Julian dolby (dolby@us.ibm.com)
 * 
 */
public abstract class AbstractSSAConversion {

  protected abstract int getNumberOfDefs(SSAInstruction inst);

  protected abstract int getDef(SSAInstruction inst, int index);

  protected abstract int getNumberOfUses(SSAInstruction inst);

  protected abstract int getUse(SSAInstruction inst, int index);

  protected abstract boolean isAssignInstruction(SSAInstruction inst);

  protected abstract int getMaxValueNumber();

  protected abstract boolean isLive(SSACFG.BasicBlock Y, int V);

  protected abstract boolean skip(int vn);

  protected abstract boolean isConstant(int valueNumber);

  protected abstract int getNextNewValueNumber();

  protected abstract void initializeVariables();

  protected abstract void repairExit();

  protected abstract void placeNewPhiAt(int value, SSACFG.BasicBlock Y);

  protected abstract SSAPhiInstruction getPhi(SSACFG.BasicBlock B, int index);

  protected abstract void setPhi(SSACFG.BasicBlock B, int index, SSAPhiInstruction inst);

  protected abstract SSAPhiInstruction repairPhiDefs(SSAPhiInstruction phi, int[] newDefs);

  protected abstract void repairPhiUse(SSACFG.BasicBlock BB, int phiIndex, int rvalIndex, int newRval);

  protected abstract void repairInstructionUses(SSAInstruction inst, int index, int[] newUses);

  protected abstract void repairInstructionDefs(SSAInstruction inst, int index, int[] newDefs, int[] newUses);

  protected abstract void pushAssignment(SSAInstruction inst, int index, int newRhs);

  protected abstract void popAssignment(SSAInstruction inst, int index);

  protected final SSACFG CFG;

  protected final DominanceFrontiers<ISSABasicBlock> DF;

  private final Graph<ISSABasicBlock> dominatorTree;

  protected final int[] phiCounts;

  protected final SSAInstruction[] instructions;

  private final int flags[];

  protected final SymbolTable symbolTable;

  protected final DefaultValues defaultValues;

  protected IntStack S[];

  protected int C[];

  protected int valueMap[];

  private Set<SSACFG.BasicBlock>[] assignmentMap;

  protected AbstractSSAConversion(IR ir, SSAOptions options) {
    this.CFG = ir.getControlFlowGraph();
    this.DF = new DominanceFrontiers<>(ir.getControlFlowGraph(), ir.getControlFlowGraph().entry());
    this.dominatorTree = DF.dominatorTree();
    this.flags = new int[2 * ir.getControlFlowGraph().getNumberOfNodes()];
    this.instructions = getInstructions(ir);
    this.phiCounts = new int[CFG.getNumberOfNodes()];
    this.symbolTable = ir.getSymbolTable();
    this.defaultValues = options.getDefaultValues();
  }

  //
  // top-level control
  //  
  protected void perform() {
    init();
    placePhiNodes();
    renameVariables();
  }

  // 
  // initialization
  //
  protected SSAInstruction[] getInstructions(IR ir) {
    return ir.getInstructions();
  }

  protected final Iterator<SSAInstruction> iterateInstructions(IR ir) {
    return new ArrayIterator<>(getInstructions(ir));
  }
  
  protected void init() {
    this.S = new IntStack[getMaxValueNumber() + 1];
    this.C = new int[getMaxValueNumber() + 1];
    this.valueMap = new int[getMaxValueNumber() + 1];
    makeAssignmentMap();
  }

  @SuppressWarnings("unchecked")
  private void makeAssignmentMap() {
    this.assignmentMap = new Set[getMaxValueNumber() + 1];
    for (ISSABasicBlock issaBasicBlock : CFG) {
      SSACFG.BasicBlock BB = (SSACFG.BasicBlock) issaBasicBlock;
      if (BB.getFirstInstructionIndex() >= 0) {
        for (SSAInstruction inst : BB) {
          if (inst != null) {
            for (int j = 0; j < getNumberOfDefs(inst); j++) {
              addDefiningBlock(assignmentMap, BB, getDef(inst, j));
            }
          }
        }
      }
    }
  }

  private void addDefiningBlock(Set<SSACFG.BasicBlock>[] A, SSACFG.BasicBlock BB, int i) {
    if (!skip(i)) {
      if (A[i] == null) {
        A[i] = new LinkedHashSet<>(2);
      }
      A[i].add(BB);
    }
  }

  //
  // place phi nodes phase of traditional algorithm
  //
  protected void placePhiNodes() {
    int IterCount = 0;

    for (ISSABasicBlock issaBasicBlock : CFG) {
      SSACFG.BasicBlock X = (SSACFG.BasicBlock) issaBasicBlock;
      setHasAlready(X, 0);
      setWork(X, 0);
    }

    Set<BasicBlock> W = new LinkedHashSet<>();
    for (int V = 0; V < assignmentMap.length; V++) {

      // some things (e.g. constants) have no defs at all
      if (assignmentMap[V] == null)
        continue;

      // ignore values as requested
      if (skip(V))
        continue;

      IterCount++;

      for (BasicBlock X : assignmentMap[V]) {
        setWork(X, IterCount);
        W.add(X);
      }

      while (!W.isEmpty()) {
        SSACFG.BasicBlock X = W.iterator().next();
        W.remove(X);
        for (ISSABasicBlock IY : Iterator2Iterable.make(DF.getDominanceFrontier(X))) {
          SSACFG.BasicBlock Y = (SSACFG.BasicBlock) IY;
          if (getHasAlready(Y) < IterCount) {
            if (isLive(Y, V)) {
              placeNewPhiAt(V, Y);
              phiCounts[Y.getGraphNodeId()]++;
            }
            setHasAlready(Y, IterCount);
            if (getWork(Y) < IterCount) {
              setWork(Y, IterCount);
              W.add(Y);
            }
          }
        }
      }
    }
  }

  private int getWork(SSACFG.BasicBlock BB) {
    return flags[BB.getGraphNodeId() * 2 + 1];
  }

  private void setWork(SSACFG.BasicBlock BB, int v) {
    flags[BB.getGraphNodeId() * 2 + 1] = v;
  }

  private int getHasAlready(SSACFG.BasicBlock BB) {
    return flags[BB.getGraphNodeId() * 2];
  }

  private void setHasAlready(SSACFG.BasicBlock BB, int v) {
    flags[BB.getGraphNodeId() * 2] = v;
  }

  //
  // rename variables phase of traditional algorithm
  //
  private void renameVariables() {
    for (int V = 1; V <= getMaxValueNumber(); V++) {
      if (!skip(V)) {
        C[V] = 0;
        S[V] = new IntStack();
      }
    }

    initializeVariables();

    SEARCH(CFG.entry());
  }

  
  /**
   * Stack frames for the SEARCH recursion.
   * Used for converting the recursion to an iteration and avoiding stack overflow. 
   * @author yinnonh
   *
   */
  private static class Frame{
    public final SSACFG.BasicBlock X;
    public final Iterator<ISSABasicBlock> i; // iterator o
    public Frame(SSACFG.BasicBlock X, Iterator<ISSABasicBlock> i) {
      this.X = X;
      this.i = i;
    }
  }
  private void SEARCH(SSACFG.BasicBlock X) {
    // original method was recursive:
    //   SearchPreRec(X)
    //   for (BasicBlock Y: childs(X))
    //     SEARCH(Y)
    //   SearchPostRec(X)
    
    ArrayList<Frame> stack = new ArrayList<>();
    
    SearchPreRec(X);
    push(stack, new Frame(X, dominatorTree.getSuccNodes(X)));
    
    // invariant: pre-rec phase was performed for elements in the queue. 
    while (!stack.isEmpty()){
      Frame f = peek(stack);
      if (f.i.hasNext()){
        // iterate next child
        BasicBlock next = (BasicBlock) f.i.next();
        SearchPreRec(next);
        push(stack, new Frame(next, dominatorTree.getSuccNodes(next)));
      } else {
        // finished iterating children, time to "return"
        SearchPostRec(f.X);
        pop(stack);
      }
    }
  }

   private static <T> void push(ArrayList<T> stack, T elt) {
    stack.add(elt);
  }
  
  private static <T> T peek(ArrayList<T> stack) {
    return stack.get(stack.size()-1); 
  }
  
  private static <T> T pop(ArrayList<T> stack) {
    T e = stack.get(stack.size()-1);
    stack.remove(stack.size()-1);
    return e;
  }

  private void SearchPreRec(SSACFG.BasicBlock X) {
    int id = X.getGraphNodeId();
    int Xf = X.getFirstInstructionIndex();

    // first loop
    for (int i = 0; i < phiCounts[id]; i++) {
      SSAPhiInstruction phi = getPhi(X, i);
      if (!skipRepair(phi, -1)) {
        setPhi(X, i, repairPhiDefs(phi, makeNewDefs(phi)));
      }
    }
    for (int i = Xf; i <= X.getLastInstructionIndex(); i++) {
      SSAInstruction inst = instructions[i];
      if (isAssignInstruction(inst)) {
        int lhs = getDef(inst, 0);
        int rhs = getUse(inst, 0);
        int newRhs = skip(rhs) ? rhs : top(rhs);
        S[lhs].push(newRhs);

        pushAssignment(inst, i, newRhs);

      } else {
        if (!skipRepair(inst, i)) {
          int[] newUses = makeNewUses(inst);
          repairInstructionUses(inst, i, newUses);
          int[] newDefs = makeNewDefs(inst);
          repairInstructionDefs(inst, i, newDefs, newUses);
        }
      }
    }

    if (X.isExitBlock()) {
      repairExit();
    }

    for (ISSABasicBlock IY : Iterator2Iterable.make(CFG.getSuccNodes(X))) {
      SSACFG.BasicBlock Y = (SSACFG.BasicBlock) IY;
      int Y_id = Y.getGraphNodeId();
      int j = com.ibm.wala.cast.ir.cfg.Util.whichPred(CFG, Y, X);
      for (int i = 0; i < phiCounts[Y_id]; i++) {
        SSAPhiInstruction phi = getPhi(Y, i);
        int oldUse = getUse(phi, j);
        int newUse = skip(oldUse) ? oldUse : top(oldUse);
        repairPhiUse(Y, i, j, newUse);
      }
    }
  }

  private void SearchPostRec(SSACFG.BasicBlock X) {
    int id = X.getGraphNodeId();
    int Xf = X.getFirstInstructionIndex();

    for (int i = 0; i < phiCounts[id]; i++) {
      SSAInstruction A = getPhi(X, i);
      for (int j = 0; j < getNumberOfDefs(A); j++) {
        if (!skip(getDef(A, j))) {
          S[valueMap[getDef(A, j)]].pop();
        }
      }
    }
    for (int i = Xf; i <= X.getLastInstructionIndex(); i++) {
      SSAInstruction A = instructions[i];
      if (isAssignInstruction(A)) {
        S[getDef(A, 0)].pop();
        popAssignment(A, i);
      } else if (A != null) {
        for (int j = 0; j < getNumberOfDefs(A); j++) {
          if (!skip(getDef(A, j))) {
            S[valueMap[getDef(A, j)]].pop();
          }
        }
      }
    }
  }

  private int[] makeNewUses(SSAInstruction inst) {
    int[] newUses = new int[getNumberOfUses(inst)];
    for (int j = 0; j < getNumberOfUses(inst); j++) {
      newUses[j] = skip(getUse(inst, j)) ? getUse(inst, j) : top(getUse(inst, j));
    }

    return newUses;
  }

  private int[] makeNewDefs(SSAInstruction inst) {
    int[] newDefs = new int[getNumberOfDefs(inst)];

    for (int j = 0; j < getNumberOfDefs(inst); j++) {
      if (skip(getDef(inst, j))) {
        newDefs[j] = getDef(inst, j);
      } else {
        int ii = getNextNewValueNumber();

        if (valueMap.length <= ii) {
          int[] nvm = new int[valueMap.length * 2 + ii + 1];
          System.arraycopy(valueMap, 0, nvm, 0, valueMap.length);
          valueMap = nvm;
        }

        valueMap[ii] = getDef(inst, j);
        S[getDef(inst, j)].push(ii);
        newDefs[j] = ii;
      }
    }

    return newDefs;
  }

  protected boolean skipRepair(SSAInstruction inst, @SuppressWarnings("unused") int index) {
    if (inst == null)
      return true;
    for (int i = 0; i < getNumberOfDefs(inst); i++)
      if (!skip(getDef(inst, i)))
        return false;
    for (int i = 0; i < getNumberOfUses(inst); i++)
      if (!skip(getUse(inst, i)))
        return false;
    return true;
  }

  protected void fail(int v) {
    assert isConstant(v) || !S[v].isEmpty() : "bad stack for " + v + " while SSA converting";
  }

  protected boolean hasDefaultValue(int valueNumber) {
    return (defaultValues != null) && (defaultValues.getDefaultValue(symbolTable, valueNumber) != -1);
  }

  protected int getDefaultValue(int valueNumber) {
    return defaultValues.getDefaultValue(symbolTable, valueNumber);
  }

  protected int top(int v) {
    if (!(isConstant(v) || !S[v].isEmpty())) {
      if (hasDefaultValue(v)) {
        return getDefaultValue(v);
      } else {
        fail(v);
      }
    }

   
    return (isConstant(v)) ? v : S[v].peek();
  }

}
