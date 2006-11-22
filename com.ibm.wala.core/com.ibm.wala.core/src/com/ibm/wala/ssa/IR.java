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

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CompoundIterator;
import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BasicNonNegativeIntRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * 
 * An SSA IR
 * 
 * @author sfink
 */
public abstract class IR {

  /**
   * The method that defined this IR's bytecodes
   */
  private IMethod method;

  /**
   * Governing SSA construction options
   */
  private final SSAOptions options;

  /**
   * Control-flow graph
   */
  private SSACFG cfg;

  /**
   * SSA instructions
   */
  private SSAInstruction[] instructions;

  /**
   * Symbol table
   */
  private SymbolTable symbolTable;

  /**
   * Mapping from CallSiteReference program counters to instruction[] indices
   */
  private final BasicNonNegativeIntRelation callSiteMapping = new BasicNonNegativeIntRelation();

  /**
   * Mapping from NewSiteReference program counters to instruction[] indices
   */
  private final Map<NewSiteReference, Integer> newSiteMapping = HashMapFactory.make();

  /**
   * Mapping from PEI program counters to instruction[] indices
   */
  private Map<ProgramCounter, Integer> peiMapping = HashMapFactory.make();

  /**
   * subclasses must provide a source name mapping, if they want one
   * (or null otherwise)
   */
  protected abstract SSA2LocalMap getLocalMap();

  /**
   * Create an SSA form from a method created by the AstTranslator front end
   * 
   * This entrypoint is used by the JavaScript -> WALA conversion. It performs
   * traditional SSA conversion, introducing phi nodes, etc.
   * 
   * keep this package private: all calls should be through SSACache
   * 
   * @param method
   *          the method to construct SSA form for
   * @param options
   *          governing ssa construction options
   */
  protected IR(IMethod method, SSAInstruction[] instructions, SymbolTable symbolTable, SSACFG cfg, SSAOptions options) {
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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return toString(null);
  }

  /**
   * Create a string representation, with decoration for each variable
   * 
   * @param d
   *          an object which provides string decorators for variables in the IR
   */
  public String toString(ValueDecorator d) {
    StringBuffer result = new StringBuffer(method.toString());
    result.append("\nCFG:\n");
    result.append(cfg.toString());
    result.append("Instructions:\n");
    for (int i = 0; i <= cfg.getMaxNumber(); i++) {
      BasicBlock bb = (BasicBlock) cfg.getNode(i);
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
          result.append("           " + phi.toString(symbolTable, d)).append("\n");
        }
      }
      if (bb instanceof ExceptionHandlerBasicBlock) {
        ExceptionHandlerBasicBlock ebb = (ExceptionHandlerBasicBlock) bb;
        SSAGetCaughtExceptionInstruction s = ebb.getCatchInstruction();
        if (s != null) {
          result.append("           " + s.toString(symbolTable, d)).append("\n");
        } else {
          result.append("           " + " No catch instruction. Unreachable?\n");
        }
      }
      for (int j = start; j <= end; j++) {
        if (instructions[j] != null) {
          StringBuffer x = new StringBuffer(j + "   " + instructions[j].toString(symbolTable, d));
          StringStuff.padWithSpaces(x, 35);
          result.append(x);
          result.append("\n");
        }
      }
      for (Iterator it = bb.iteratePis(); it.hasNext();) {
        SSAPiInstruction pi = (SSAPiInstruction) it.next();
        if (pi != null) {
          result.append("           " + pi.toString(symbolTable, d)).append("\n");
        }
      }
    }
    return result.toString();
  }

  /**
   * Returns the instructions.
   * 
   * @return Instruction[]
   */
  public SSAInstruction[] getInstructions() {
    return instructions;
  }

  /**
   * Returns the symbolTable.
   * 
   * @return SymbolTable
   */
  public SymbolTable getSymbolTable() {
    return symbolTable;
  }

  /**
   * Return the CFG for the method.
   * 
   * @return the CFG for the method.
   */
  public SSACFG getControlFlowGraph() {
    return cfg;
  }

  /**
   * Return an iterator of all phis for this IR.
   */
  public Iterator<? extends SSAInstruction> iteratePhis() {
    return new DerivedNodeIterator() {
      Iterator<? extends SSAInstruction> getBlockIterator(BasicBlock b) {
        return b.iteratePhis();
      }
    };
  }

  /**
   * Return an iterator of all pis for this IR.
   */
  public Iterator<? extends SSAInstruction> iteratePis() {
    return new DerivedNodeIterator() {
      Iterator<? extends SSAInstruction> getBlockIterator(BasicBlock b) {
        return b.iteratePis();
      }
    };
  }

  abstract private class DerivedNodeIterator implements Iterator<SSAInstruction> {
    // invariant: if currentBlockIndex != -1, then
    // currentBlockIterator.hasNext()
    private Iterator<? extends SSAInstruction> currentBlockIterator;

    private int currentBlockIndex;

    abstract Iterator<? extends SSAInstruction> getBlockIterator(BasicBlock b);

    @SuppressWarnings("unchecked")
    DerivedNodeIterator() {
      currentBlockIndex = 0;
      currentBlockIterator = (Iterator<SSAInstruction>) ((BasicBlock) cfg.getNode(0)).iteratePhis();
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
        Iterator<? extends SSAInstruction> it = getBlockIterator((BasicBlock) cfg.getNode(i));
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
   * Method getParameterValueNumbers.
   * 
   * @return array of value numbers representing parameters to this method
   */
  public int[] getParameterValueNumbers() {
    return symbolTable.getParameterValueNumbers();
  }

  /**
   * @param i
   * @return the value number of the ith parameter
   */
  public int getParameter(int i) {
    return symbolTable.getParameter(i);
  }

  /**
   * Get the type reference that describes the ith parameter to this method. By
   * convention, for a non-static method, the 0th parameter is "this".
   * 
   * @param i
   * @return TypeReference
   */
  public TypeReference getParameterType(int i) {
    return method.getParameterType(i);
  }

  /**
   * Method getNumberOfParameters.
   * 
   * @return int
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
   * 
   * @param v
   *          a visitor
   */
  public void visitNormalInstructions(SSAInstruction.Visitor v) {
    for (Iterator i = iterateNormalInstructions(); i.hasNext();) {
      ((SSAInstruction) i.next()).visit(v);
    }
  }

  /**
   * visit each instruction in this IR
   * 
   * @param v
   *          a visitor
   */
  public void visitAllInstructions(SSAInstruction.Visitor v) {
    for (Iterator i = iterateAllInstructions(); i.hasNext();) {
      ((SSAInstruction) i.next()).visit(v);
    }
  }

  /**
   * @return an iterator of all "normal" instructions on this IR
   */
  public Iterator<SSAInstruction> iterateNormalInstructions() {
    return new NormalIterator();
  }

  private class NormalIterator implements Iterator<SSAInstruction> {
    int nextIndex = -1;

    SSAInstruction[] instructions = getInstructions();

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
   * @return an Iterator of allinstructions (Normal, Phi, and Catch)
   */
  public Iterator<SSAInstruction> iterateAllInstructions() {
    return new CompoundIterator<SSAInstruction>(iterateNormalInstructions(), new CompoundIterator<SSAInstruction>(
        iterateCatchInstructions(), new CompoundIterator<SSAInstruction>(iteratePhis(), iteratePis())));
  }

  /**
   * @return the exit basic block
   */
  public BasicBlock getExitBlock() {
    return (BasicBlock) cfg.exit();
  }

  /**
   * @param site
   * @return the invoke instructions corresponding to this call site
   */
  public SSAAbstractInvokeInstruction[] getCalls(CallSiteReference site) {
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
   * @param site
   * @return the instruction indices corresponding to this call site
   */
  public IntSet getCallInstructionIndices(CallSiteReference site) {
    return callSiteMapping.getRelated(site.getProgramCounter());
  }

  /**
   * @param site
   * @return the new instruction corresponding to this site
   */
  public SSANewInstruction getNew(NewSiteReference site) {
    Integer i = newSiteMapping.get(site);
    return (SSANewInstruction) instructions[i.intValue()];
  }

  /**
   * @param site
   * @return the instruction index corresponding to this site.
   */
  public int getNewInstructionIndex(NewSiteReference site) {
    Integer i = newSiteMapping.get(site);
    return i.intValue();
  }

  /**
   * @param pc
   *          a program counter
   * @return the instruction (a PEI) at this program counter
   */
  public SSAInstruction getPEI(ProgramCounter pc) {
    Integer i = peiMapping.get(pc);
    return instructions[i.intValue()];
  }

  public Iterator iterateCallSites() {
    return new Iterator() {
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

      public Object next() {
        int index = callSiteMapping.getRelated(i).max();
        advance();
        return ((SSAAbstractInvokeInstruction) instructions[index]).getSite();
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * @param site
   *          a call site in this method
   * @return the basic block corresponding to this instruction
   */
  public IBasicBlock[] getBasicBlocksForCall(CallSiteReference site) {
    IntSet s = callSiteMapping.getRelated(site.getProgramCounter());
    IBasicBlock[] result = new IBasicBlock[s.size()];
    int index = 0;
    for (IntIterator it = s.intIterator(); it.hasNext();) {
      int i = it.next();
      result[index++] = getControlFlowGraph().getBlockForInstruction(i);
    }
    return result;
  }

  /**
   * TODO: why do we need this? We should enforce instructions == null if
   * necessary, I think.
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
   * @param index
   *          an index into the IR instruction array
   * @param vn
   *          a value number
   * @return if we know that immediately after the given program counter, v_vn
   *         corresponds to one or more locals and local variable names are
   *         available, the name of the locals which v_vn represents. Otherwise,
   *         null.
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
     * @param index
     *          an index into the IR instruction array
     * @param vn
     *          a value number
     * @return if we know that immediately after the given program counter, v_vn
     *         corresponds to one or more locals and local variable names are
     *         available, the name of the locals which v_vn represents.
     *         Otherwise, null.
     */
    String[] getLocalNames(int index, int vn);

  }

  public IBasicBlock getBasicBlockForCatch(SSAGetCaughtExceptionInstruction instruction) {
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
