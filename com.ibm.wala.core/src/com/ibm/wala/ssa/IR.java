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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.StringStuff;

/**
 * An SSA IR.
 * 
 * The IR (Intermediate Representation) is the central data structure that represents the instructions of a particular method.
 * The IR represents a method's instructions in a language close to JVM bytecode, but in an SSA-based register transfer language
 * which eliminates the stack abstraction, relying instead on a set of symbolic registers. The IR organizes instructions in a
 * control-flow graph of basic blocks, as typical in compiler textbooks.
 * 
 * See http://wala.sourceforge.net/wiki/index.php/UserGuide:IR for more details on the IR API.
 */
public abstract class IR implements IRView {

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
   * subclasses must provide information about indirect use of values, if appropriate, and otherwise null 
   */
  protected abstract <T extends SSAIndirectionData.Name> SSAIndirectionData<T> getIndirectionData();
  
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
   * create mappings from call sites, new sites, and PEIs to instruction index
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

  /**
   * @return a String which is a readable representation of the instruction position corresponding to an instruction index
   */
  protected abstract String instructionPosition(int instructionIndex);

  @Override
  public String toString() {
    Collection<? extends SSAIndirectionData.Name> names = null;
    if (getIndirectionData() != null) {
      names = getIndirectionData().getNames();
    }
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

        result.append("<Handler> (");
        Iterator<TypeReference> catchIter = ((ExceptionHandlerBasicBlock) bb).getCaughtExceptionTypes();
        while (catchIter.hasNext()) {
          TypeReference next = catchIter.next();
          result.append(next);
          if (catchIter.hasNext()) {
            result.append(",");
          }
        }
        result.append(")");
      }
      result.append("\n");
      for (SSAPhiInstruction phi : Iterator2Iterable.make(bb.iteratePhis())) {
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
          if (names != null) {
            boolean any = false;
            for(SSAIndirectionData.Name n : names) {
              if (getIndirectionData().getUse(j, n) != -1) {
                result.append(" " + n + " -> " + getIndirectionData().getUse(j, n));
                any = true;
              }
            }
            if (any) {
              result.append("\n");
            }
          }
          StringBuffer x = new StringBuffer(j + "   " + instructions[j].toString(symbolTable));
          StringStuff.padWithSpaces(x, 45);
          result.append(x);
          result.append(instructionPosition(j));
          
          Map<Integer,Set<String>> valNames = HashMapFactory.make();
          for(int v = 0; v < instructions[j].getNumberOfDefs(); v++) {
            int valNum = instructions[j].getDef(v);
            addNames(j, valNames, valNum);
          }
          for(int v = 0; v < instructions[j].getNumberOfUses(); v++) {
            int valNum = instructions[j].getUse(v);
            addNames(j, valNames, valNum);
          }
          if (!valNames.isEmpty()) {
            result.append(" [");
            for(Map.Entry<Integer,Set<String>> e : valNames.entrySet()) {
              result.append(e.getKey() + "=" + e.getValue());
            }
            result.append("]");
          }
 
          result.append("\n");
          
          if (names != null) {
            boolean any = false;
            for(SSAIndirectionData.Name n : names) {
              if (getIndirectionData().getDef(j, n) != -1) {
                result.append(" " + n + " <- " + getIndirectionData().getDef(j, n));
                any = true;
              }
            }
            if (any) {
              result.append("\n");
            }
          }
        }
      }
      for (SSAPiInstruction pi : Iterator2Iterable.make(bb.iteratePis())) {
        if (pi != null) {
          result.append("           " + pi.toString(symbolTable)).append("\n");
        }
      }
    }
    return result.toString();
  }

  private void addNames(int j, Map<Integer, Set<String>> valNames, int valNum) {
    if (getLocalNames(j, valNum) != null && getLocalNames(j, valNum).length > 0) {
      if (! valNames.containsKey(valNum)) {
        valNames.put(valNum, HashSetFactory.<String>make());
      }
      for(String s : getLocalNames(j, valNum)) {
        valNames.get(valNum).add(s);
      }
    }
  }

  /**
   * Returns the normal instructions. Does not include {@link SSAPhiInstruction}, {@link SSAPiInstruction}, or
   * {@link SSAGetCaughtExceptionInstruction}s, which are currently managed by {@link BasicBlock}. Entries in the returned array
   * might be null.
   * 
   * This may go away someday.
   */
  @Override
  public SSAInstruction[] getInstructions() {
    return instructions;
  }

  /**
   * @return the {@link SymbolTable} managing attributes for values in this method
   */
  @Override
  public SymbolTable getSymbolTable() {
    return symbolTable;
  }

  /**
   * @return the underlying {@link ControlFlowGraph} which defines this IR.
   */
  @Override
  public SSACFG getControlFlowGraph() {
    return cfg;
  }

  @Override
  public Iterator<ISSABasicBlock> getBlocks() {
    return getControlFlowGraph().iterator();
  }

  /**
   * Return an {@link Iterator} of all {@link SSAPhiInstruction}s for this IR.
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
   * Return an {@link Iterator} of all {@link SSAPiInstruction}s for this IR.
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
   * An {@link Iterator} over all {@link SSAInstruction}s of a certain type, retrieved by iterating over the {@link BasicBlock}s,
   * one at a time.
   * 
   * TODO: this looks buggy to me .. looks like it's hardcoded for Phis. Does it work for Pis?
   */
  abstract private class TwoLevelIterator implements Iterator<SSAInstruction> {
    // invariant: if currentBlockIndex != -1, then
    // currentBlockIterator.hasNext()
    private Iterator<? extends SSAInstruction> currentBlockIterator;

    private int currentBlockIndex;

    abstract Iterator<? extends SSAInstruction> getBlockIterator(BasicBlock b);

    TwoLevelIterator() {
      currentBlockIndex = 0;
      currentBlockIterator = cfg.getNode(0).iteratePhis();
      if (!currentBlockIterator.hasNext()) {
        advanceBlock();
      }
    }

    @Override
    public boolean hasNext() {
      return currentBlockIndex != -1;
    }

    @Override
    public SSAInstruction next() {
      SSAInstruction result = currentBlockIterator.next();
      if (!currentBlockIterator.hasNext()) {
        advanceBlock();
      }
      return result;
    }

    @Override
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
   * Get the {@link TypeReference} that describes the ith parameter to this method. By convention, for a non-static method, the 0th
   * parameter is "this".
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
  @Override
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

    @Override
    public boolean hasNext() {
      return currentBlockIndex != -1;
    }

    @Override
    public SSAInstruction next() {
      ExceptionHandlerBasicBlock bb = (ExceptionHandlerBasicBlock) cfg.getNode(currentBlockIndex);
      SSAInstruction result = bb.getCatchInstruction();
      advanceBlock();
      return result;
    }

    @Override
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
    for (SSAInstruction inst : Iterator2Iterable.make(iterateNormalInstructions())) {
      inst.visit(v);
    }
  }

  /**
   * visit each instruction in this IR
   */
  public void visitAllInstructions(SSAInstruction.Visitor v) {
    for (SSAInstruction inst : Iterator2Iterable.make(iterateAllInstructions())) {
      inst.visit(v);
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

    @Override
    public boolean hasNext() {
      return nextIndex != -1;
    }

    @Override
    public void remove() {
      Assertions.UNREACHABLE();
    }

    @Override
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
    return new CompoundIterator<>(iterateNormalInstructions(), new CompoundIterator<>(
        iterateCatchInstructions(), new CompoundIterator<>(iteratePhis(), iteratePis())));
  }

  /**
   * @return the exit basic block
   */
  @Override
  public BasicBlock getExitBlock() {
    return cfg.exit();
  }

  /**
   * Return the invoke instructions corresponding to a call site
   * 
   * Note that Shrike may inline JSRS. This can lead to multiple copies of a single bytecode instruction in a particular IR. So we
   * may have more than one instruction index for a particular call site from bytecode.
   */
  public SSAAbstractInvokeInstruction[] getCalls(CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    IntSet s = callSiteMapping.getRelated(site.getProgramCounter());
    if (s == null) {
      throw new IllegalArgumentException("no calls at site's pc");
    }
    SSAAbstractInvokeInstruction[] result = new SSAAbstractInvokeInstruction[s.size()];
    int index = 0;
    for (IntIterator it = s.intIterator(); it.hasNext();) {
      int i = it.next();
      result[index++] = (SSAAbstractInvokeInstruction) instructions[i];
    }
    return result;
  }

  /**
   * Return the instruction indices corresponding to a call site.
   * 
   * Note that Shrike may inline JSRS. This can lead to multiple copies of a single bytecode instruction in a particular IR. So we
   * may have more than one instruction index for a particular call site from bytecode.
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
  @Override
  public SSAInstruction getPEI(ProgramCounter pc) {
    Integer i = peiMapping.get(pc);
    return instructions[i.intValue()];
  }

  /**
   * @return an {@link Iterator} of all the allocation sites ( {@link NewSiteReference}s ) in this IR
   */
  @Override
  public Iterator<NewSiteReference> iterateNewSites() {
    return newSiteMapping.keySet().iterator();
  }

  /**
   * @return an {@link Iterator} of all the call sites ( {@link CallSiteReference}s ) in this IR
   */
  @Override
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

      @Override
      public boolean hasNext() {
        return i <= limit;
      }

      @Override
      public CallSiteReference next() {
        int index = callSiteMapping.getRelated(i).max();
        advance();
        return ((SSAAbstractInvokeInstruction) instructions[index]).getCallSite();
      }

      @Override
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
  @Override
  public ISSABasicBlock[] getBasicBlocksForCall(final CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    final IntSet s = callSiteMapping.getRelated(site.getProgramCounter());
    if (s == null) {
      throw new IllegalArgumentException("invalid site: " + site);
    }
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
   * Be very careful; note the strange identity semantics of SSAInstruction, using ==. You can't mix SSAInstructions and IRs freely.
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
      for (SSAInstruction s : b) {
        instruction2Block.put(s, b);
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

    for (SSAInstruction instruction : instructions)
      if (instruction != null)
        return false;

    return true;
  }

  /**
   * @param index an index into the IR instruction array
   * @param vn a value number
   * @return if we know that immediately after the given program counter, v_vn corresponds to one or more locals and local variable
   *         names are available, the name of the locals which v_vn represents. Otherwise, null.
   */
  @Override
  public String[] getLocalNames(int index, int vn) {
    if (getLocalMap() == null) {
      return new String[0];
    } else {
      return getLocalMap().getLocalNames(index, vn);
    }
  }

  /**
   * A Map that gives the names of the local variables corresponding to SSA value numbers at particular IR instruction indices, if
   * such information is available from source code mapping.
   */
  public interface SSA2LocalMap {
    /**
     * @param index an index into the IR instruction array
     * @param vn a value number
     * @return if we know that immediately after the given program counter, v_vn corresponds to one or more locals and local
     *         variable names are available, the name of the locals which v_vn represents. Otherwise, null.
     */
    String[] getLocalNames(int index, int vn);
  }

  /**
   * Return the {@link ISSABasicBlock} corresponding to a particular catch instruction
   */
  public ISSABasicBlock getBasicBlockForCatch(SSAGetCaughtExceptionInstruction instruction) {
    if (instruction == null) {
      throw new IllegalArgumentException("instruction is null");
    }
    int bb = instruction.getBasicBlockNumber();
    return cfg.getBasicBlock(bb);
  }

  /**
   * @return the {@link SSAOptions} which controlled how this {@link IR} was built
   */
  public SSAOptions getOptions() {
    return options;
  }
}
