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
package com.ibm.wala.ssa;

import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.StringStuff;

/**
 * An SSA IR
 * 
 * @author sfink
 */
public abstract class IR {

  /**
   * The method that defined this IR's bytecodes
   */
  final private IMethod method;

  /**
   * Governing SSA construction options
   */
  private final SSAOptions options;

  /**
   * Control-flow graph
   */
  final private SSACFG cfg;

  /**
   * SSA instructions
   */
  final private SSAInstruction[] instructions;

  /**
   * Symbol table
   */
  final private SymbolTable symbolTable;

  /**
   * Mapping from CallSiteReference program counters to instruction[] indices
   */
  private final BasicNaturalRelation callSiteMapping = new BasicNaturalRelation();

  /**
   * Mapping from NewSiteReference program counters to instruction[] indices
   */
  private final Map<NewSiteReference, Integer> newSiteMapping = HashMapFactory.make();

  /**
   * Mapping from PEI program counters to instruction[] indices
   */
  final private Map<ProgramCounter, Integer> peiMapping = HashMapFactory.make();

  /**
   * Mapping from SSAInstruction to Basic Block, computed lazily
   */
  private Map<SSAInstruction, ISSABasicBlock> instruction2Block;

  /**
   * subclasses must provide a source name mapping, if they want one (or null otherwise)
   */
  protected abstract SSA2LocalMap getLocalMap();

  /**
   * Simple constructor when someone else has already computed the symbol table and cfg.
   */
  protected IR(IMethod method, SSAInstruction[] instructions, SymbolTable symbolTable, SSACFG cfg, SSAOptions options) {
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    this.method = method;
    this.instructions = instructions;
    this.symbolTable = symbolTable;
    this.cfg = cfg;
    this.options = options;
  }

  /**
   * create mappings from callsites, new sites, and PEIs to instruction index
   */
  protected void setupLocationMap() {
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction x = instructions[i];
      if (x != null) {
        if (x instanceof SSAAbstractInvokeInstruction) {
          callSiteMapping.add(((SSAAbstractInvokeInstruction) x).getCallSite().getProgramCounter(), i);
        }
        if (x instanceof SSANewInstruction) {
          newSiteMapping.put(((SSANewInstruction) x).getNewSite(), new Integer(i));
        }
        if (x.isPEI()) {
          peiMapping.put(new ProgramCounter(cfg.getProgramCounter(i)), new Integer(i));
        }
      }
    }
  }

  protected abstract String instructionPosition(int instructionIndex);

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(method.toString());
    result.append("\nCFG:\n");
    result.append(cfg.toString());
    result.append("Instructions:\n");
    for (int i = 0; i <= cfg.getMaxNumber(); i++) {
      BasicBlock bb = cfg.getNode(i);
      int start = bb.getFirstInstructionIndex();
      int end = bb.getLastInstructionIndex();
      result.append("BB").append(bb.getNumber());
      if (bb instanceof ExceptionHandlerBasicBlock) {
        result.append("<Handler>");
      }
      result.append("\n");
      for (Iterator it = bb.iteratePhis(); it.hasNext();) {
        SSAPhiInstruction phi = (SSAPhiInstruction) it.next();
        if (phi != null) {
          result.append("           " + phi.toString(symbolTable)).append("\n");
        }
      }
      if (bb instanceof ExceptionHandlerBasicBlock) {
        ExceptionHandlerBasicBlock ebb = (ExceptionHandlerBasicBlock) bb;
        SSAGetCaughtExceptionInstruction s = ebb.getCatchInstruction();
        if (s != null) {
          result.append("           " + s.toString(symbolTable)).append("\n");
        } else {
          result.append("           " + " No catch instruction. Unreachable?\n");
        }
      }
      for (int j = start; j <= end; j++) {
        if (instructions[j] != null) {
          StringBuffer x = new StringBuffer(j + "   " + instructions[j].toString(symbolTable));
          StringStuff.padWithSpaces(x, 45);
          result.append(x);
          result.append(instructionPosition(j));
          result.append("\n");
        }
      }
      for (Iterator it = bb.iteratePis(); it.hasNext();) {
        SSAPiInstruction pi = (SSAPiInstruction) it.next();
        if (pi != null) {
          result.append("           " + pi.toString(symbolTable)).append("\n");
        }
      }
    }
    return result.toString();
  }

  /**
   * Returns the normal instructions. Does not include {@link SSAPhiInstruction}, {@link SSAPiInstruction}, or
   * {@link SSAGetCaughtExceptionInstruction}s, which are currently managed by {@link BasicBlock}. Entries in the
   * returned array might be null.
   * 
   * This may go away someday.
   */
  public SSAInstruction[] getInstructions() {
    return instructions;
  }

  /**
   * @return the {@link SymbolTable} managing attributes for values in this method
   */
  public SymbolTable getSymbolTable() {
    return symbolTable;
  }

  /**
   * @return the underlying {@link ControlFlowGraph} which defines this IR.
   */
  public SSACFG getControlFlowGraph() {
    return cfg;
  }

  /**
   * Return an iterator of all {@link SSAPhiInstruction}s for this IR.
   */
  public Iterator<? extends SSAInstruction> iteratePhis() {
    return new TwoLevelIterator() {
      @Override
      Iterator<? extends SSAInstruction> getBlockIterator(BasicBlock b) {
        return b.iteratePhis();
      }
    };
  }

  /**
   * Return an iterator of all {@link SSAPiInstruction}s for this IR.
   */
  public Iterator<? extends SSAInstruction> iteratePis() {
    return new TwoLevelIterator() {
      @Override
      Iterator<? extends SSAInstruction> getBlockIterator(BasicBlock b) {
        return b.iteratePis();
      }
    };
  }

  /**
   * An {@link Iterator} over all {@link SSAInstruction}s of a certain type, retrieved by iterating over the
   * {@link BasicBlock}s, one at a time.
   * 
   * TODO: this looks buggy to me .. looks like it's hardcoded for Phis. Does it work for Pis?
   */
  abstract private class TwoLevelIterator implements Iterator<SSAInstruction> {
    // invariant: if currentBlockIndex != -1, then
    // currentBlockIterator.hasNext()
    private Iterator<? extends SSAInstruction> currentBlockIterator;

    private int currentBlockIndex;

    abstract Iterator<? extends SSAInstruction> getBlockIterator(BasicBlock b);

    @SuppressWarnings("unchecked")
    TwoLevelIterator() {
      currentBlockIndex = 0;
      currentBlockIterator = cfg.getNode(0).iteratePhis();
      if (!currentBlockIterator.hasNext()) {
        advanceBlock();
      }
    }

    public boolean hasNext() {
      return currentBlockIndex != -1;
    }

    public SSAInstruction next() {
      SSAInstruction result = currentBlockIterator.next();
      if (!currentBlockIterator.hasNext()) {
        advanceBlock();
      }
      return result;
    }

    public void remove() {
      Assertions.UNREACHABLE();
    }

    private void advanceBlock() {
      for (int i = currentBlockIndex + 1; i <= cfg.getMaxNumber(); i++) {
        Iterator<? extends SSAInstruction> it = getBlockIterator(cfg.getNode(i));
        if (it.hasNext()) {
          currentBlockIndex = i;
          currentBlockIterator = it;
          return;
        }
      }
      currentBlockIterator = null;
      currentBlockIndex = -1;
    }
  }

  /**
   * @return array of value numbers representing parameters to this method
   */
  public int[] getParameterValueNumbers() {
    return symbolTable.getParameterValueNumbers();
  }

  /**
   * @return the value number of the ith parameter
   */
  public int getParameter(int i) {
    return symbolTable.getParameter(i);
  }

  /**
   * Get the {@link TypeReference} that describes the ith parameter to this method. By convention, for a non-static
   * method, the 0th parameter is "this".
   */
  public TypeReference getParameterType(int i) {
    return method.getParameterType(i);
  }

  /**
   * @return number of parameters to this method, including "this"
   */
  public int getNumberOfParameters() {
    return method.getNumberOfParameters();
  }

  /**
   * @return the method this IR represents
   */
  public IMethod getMethod() {
    return method;
  }

  /**
   * @return iterator of the catch instructions in this IR
   */
  public Iterator<SSAInstruction> iterateCatchInstructions() {
    return new CatchIterator();
  }

  /**
   * TODO: looks like this should be merged into {@link TwoLevelIterator}, above?
   */
  private class CatchIterator implements Iterator<SSAInstruction> {
    // invariant: if currentBlockIndex != -1, then
    // then block[currentBlockIndex] is a handler block
    private int currentBlockIndex;

    private boolean hasCatch(Object x) {
      return (x instanceof ExceptionHandlerBasicBlock) && (((ExceptionHandlerBasicBlock) x).getCatchInstruction() != null);
    }

    CatchIterator() {
      currentBlockIndex = 0;
      if (!hasCatch(cfg.getNode(0))) {
        advanceBlock();
      }
    }

    public boolean hasNext() {
      return currentBlockIndex != -1;
    }

    public SSAInstruction next() {
      ExceptionHandlerBasicBlock bb = (ExceptionHandlerBasicBlock) cfg.getNode(currentBlockIndex);
      SSAInstruction result = bb.getCatchInstruction();
      advanceBlock();
      return result;
    }

    public void remove() {
      Assertions.UNREACHABLE();
    }

    private void advanceBlock() {
      for (int i = currentBlockIndex + 1; i < cfg.getMaxNumber(); i++) {
        if (hasCatch(cfg.getNode(i))) {
          currentBlockIndex = i;
          return;
        }
      }
      currentBlockIndex = -1;
    }
  }

  /**
   * visit each normal (non-phi, non-pi, non-catch) instruction in this IR
   */
  public void visitNormalInstructions(SSAInstruction.Visitor v) {
    for (Iterator i = iterateNormalInstructions(); i.hasNext();) {
      ((SSAInstruction) i.next()).visit(v);
    }
  }

  /**
   * visit each instruction in this IR
   */
  public void visitAllInstructions(SSAInstruction.Visitor v) {
    for (Iterator i = iterateAllInstructions(); i.hasNext();) {
      ((SSAInstruction) i.next()).visit(v);
    }
  }

  /**
   * @return an {@link Iterator} of all "normal" instructions on this IR
   */
  public Iterator<SSAInstruction> iterateNormalInstructions() {
    return new NormalIterator();
  }

  private class NormalIterator implements Iterator<SSAInstruction> {
    int nextIndex = -1;

    final SSAInstruction[] instructions = getInstructions();

    NormalIterator() {
      advanceIndex(0);
    }

    private void advanceIndex(int start) {
      for (int i = start; i < instructions.length; i++) {
        if (instructions[i] != null) {
          nextIndex = i;
          return;
        }
      }
      nextIndex = -1;
    }

    public boolean hasNext() {
      return nextIndex != -1;
    }

    public void remove() {
      Assertions.UNREACHABLE();
    }

    public SSAInstruction next() {
      SSAInstruction result = instructions[nextIndex];
      advanceIndex(nextIndex + 1);
      return result;
    }
  }

  /**
   * @return an {@link Iterator} of all instructions (Normal, Phi, and Catch)
   */
  public Iterator<SSAInstruction> iterateAllInstructions() {
    return new CompoundIterator<SSAInstruction>(iterateNormalInstructions(), new CompoundIterator<SSAInstruction>(
        iterateCatchInstructions(), new CompoundIterator<SSAInstruction>(iteratePhis(), iteratePis())));
  }

  /**
   * @return the exit basic block
   */
  public BasicBlock getExitBlock() {
    return cfg.exit();
  }

  /**
   * Return the invoke instructions corresponding to a call site
   */
  public SSAAbstractInvokeInstruction[] getCalls(CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    IntSet s = callSiteMapping.getRelated(site.getProgramCounter());
    SSAAbstractInvokeInstruction[] result = new SSAAbstractInvokeInstruction[s.size()];
    int index = 0;
    for (IntIterator it = s.intIterator(); it.hasNext();) {
      int i = it.next();
      result[index++] = (SSAAbstractInvokeInstruction) instructions[i];
    }
    return result;
  }

  /**
   * Return the instruction indices corresponding to a call site
   */
  public IntSet getCallInstructionIndices(CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    return callSiteMapping.getRelated(site.getProgramCounter());
  }

  /**
   * Return the new instruction corresponding to an allocation site
   */
  public SSANewInstruction getNew(NewSiteReference site) {
    Integer i = newSiteMapping.get(site);
    return (SSANewInstruction) instructions[i.intValue()];
  }

  /**
   * Return the instruction index corresponding to an allocation site
   */
  public int getNewInstructionIndex(NewSiteReference site) {
    Integer i = newSiteMapping.get(site);
    return i.intValue();
  }

  /**
   * @param pc a program counter
   * @return the instruction (a PEI) at this program counter
   */
  public SSAInstruction getPEI(ProgramCounter pc) {
    Integer i = peiMapping.get(pc);
    return instructions[i.intValue()];
  }

  public Iterator<NewSiteReference> iterateNewSites() {
    return newSiteMapping.keySet().iterator();
  }

  public Iterator<CallSiteReference> iterateCallSites() {
    return new Iterator<CallSiteReference>() {
      private final int limit = callSiteMapping.maxKeyValue();

      private int i = -1;

      {
        advance();
      }

      private void advance() {
        while (callSiteMapping.getRelatedCount(++i) == 0 && i <= limit)
          ;
      }

      public boolean hasNext() {
        return i <= limit;
      }

      public CallSiteReference next() {
        int index = callSiteMapping.getRelated(i).max();
        advance();
        return ((SSAAbstractInvokeInstruction) instructions[index]).getCallSite();
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * @param site a call site in this method
   * @return the basic block corresponding to this instruction
   * @throws IllegalArgumentException if site is null
   */
  public ISSABasicBlock[] getBasicBlocksForCall(final CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    final IntSet s = callSiteMapping.getRelated(site.getProgramCounter());
    final ISSABasicBlock[] result = new ISSABasicBlock[s.size()];
    int index = 0;
    for (final IntIterator it = s.intIterator(); it.hasNext();) {
      final int i = it.next();
      result[index++] = getControlFlowGraph().getBlockForInstruction(i);
    }
    return result;
  }

  /**
   * This is space-inefficient. Use with care.
   * 
   * Be very careful; note the strange identity semantics of SSAInstruction, using ==. You can't mix SSAInstructions and
   * IRs freely.
   */
  public ISSABasicBlock getBasicBlockForInstruction(SSAInstruction s) {
    if (instruction2Block == null) {
      mapInstructions2Blocks();
    }
    return instruction2Block.get(s);
  }

  private void mapInstructions2Blocks() {
    instruction2Block = HashMapFactory.make();
    for (ISSABasicBlock b : cfg) {
      for (IInstruction s : b) {
        instruction2Block.put((SSAInstruction) s, b);
      }
    }
  }

  /**
   * TODO: why do we need this? We should enforce instructions == null if necessary, I think.
   * 
   * @return true iff every instruction is null
   */
  public boolean isEmptyIR() {
    if (instructions == null)
      return true;

    for (int i = 0; i < instructions.length; i++)
      if (instructions[i] != null)
        return false;

    return true;
  }

  /**
   * @param index an index into the IR instruction array
   * @param vn a value number
   * @return if we know that immediately after the given program counter, v_vn corresponds to one or more locals and
   *         local variable names are available, the name of the locals which v_vn represents. Otherwise, null.
   */
  public String[] getLocalNames(int index, int vn) {
    if (getLocalMap() == null) {
      return new String[0];
    } else {
      return getLocalMap().getLocalNames(index, vn);
    }
  }

  /**
   * 
   * 
   */
  public interface SSA2LocalMap {
    /**
     * @param index an index into the IR instruction array
     * @param vn a value number
     * @return if we know that immediately after the given program counter, v_vn corresponds to one or more locals and
     *         local variable names are available, the name of the locals which v_vn represents. Otherwise, null.
     */
    String[] getLocalNames(int index, int vn);

  }

  public ISSABasicBlock getBasicBlockForCatch(SSAGetCaughtExceptionInstruction instruction) {
    if (instruction == null) {
      throw new IllegalArgumentException("instruction is null");
    }
    int bb = instruction.getBasicBlockNumber();
    return cfg.getBasicBlock(bb);
  }

  /**
   * @return Returns the options.
   */
  public SSAOptions getOptions() {
    return options;
  };
}
