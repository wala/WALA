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

import com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.cfg.ShrikeCFG.BasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrikeBT.ArrayLoadInstruction;
import com.ibm.wala.shrikeBT.ArrayStoreInstruction;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.CheckCastInstruction;
import com.ibm.wala.shrikeBT.ComparisonInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.ConversionInstruction;
import com.ibm.wala.shrikeBT.GetInstruction;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.InstanceofInstruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.ShiftInstruction;
import com.ibm.wala.shrikeBT.StoreInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ShrikeUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntPair;

/**
 * This class constructs an SSA IR from a backing ShrikeBT instruction stream.
 * 
 * @author sfink
 */
public class SSABuilder extends AbstractIntStackMachine {

  /**
   * A wrapper around the method being analyzed.
   */
  final private ShrikeCTMethod method;

  /**
   * Governing symbol table
   */
  final private SymbolTable symbolTable;

  /**
   * A logical mapping from <bcIndex, valueNumber> -> local number if null,
   * don't build it.
   */
  private final SSA2LocalMap localMap;

  public SSABuilder(ShrikeCTMethod method, SSACFG cfg, ShrikeCFG scfg, SSAInstruction[] instructions,
      SymbolTable symbolTable, boolean buildLocalMap, boolean addPiNodes) {
    super(scfg);
    localMap = buildLocalMap ? new SSA2LocalMap(scfg, instructions.length, cfg.getNumberOfNodes(), maxLocals) : null;
    init(new SymbolTableMeeter(symbolTable, cfg, instructions, scfg), new SymbolicPropagator(scfg, instructions, symbolTable,
        localMap, cfg, addPiNodes, addPiNodes, addPiNodes, addPiNodes));
    this.method = method;
    this.symbolTable = symbolTable;
    if (Assertions.verifyAssertions) {
      Assertions._assert(cfg != null, "Null CFG");
    }
  }

  private static class SymbolTableMeeter implements Meeter {

    final SSACFG cfg;

    final SSAInstruction[] instructions;

    final SymbolTable symbolTable;

    final ShrikeCFG shrikeCFG;

    SymbolTableMeeter(SymbolTable symbolTable, SSACFG cfg, SSAInstruction[] instructions, ShrikeCFG shrikeCFG) {
      this.cfg = cfg;
      this.instructions = instructions;
      this.symbolTable = symbolTable;
      this.shrikeCFG = shrikeCFG;
    }

    public int meetStack(int slot, int[] rhs, BasicBlock bb) {

      if (Assertions.verifyAssertions) {
        Assertions._assert(bb != null, "null basic block");
      }

      if (bb.isExitBlock()) {
        return TOP;
      }

      if (allTheSame(rhs)) {
        for (int i = 0; i < rhs.length; i++) {
          if (rhs[i] != TOP) {
            return rhs[i];
          }
        }
        // didn't find anything but TOP
        return TOP;
      } else {
        SSACFG.BasicBlock newBB = (com.ibm.wala.ssa.SSACFG.BasicBlock) cfg.getNode(shrikeCFG.getNumber(bb));
        // if we already have a phi for this stack location
        SSAPhiInstruction phi = newBB.getPhiForStackSlot(slot);
        int result;
        if (phi == null) {
          // no phi already exists. create one.
          result = symbolTable.newPhi(rhs);
          PhiValue v = symbolTable.getPhiValue(result);
          phi = v.getPhiInstruction();
          newBB.addPhiForStackSlot(slot, phi);
        } else {
          // already created a phi. update it to account for the
          // new merge.
          result = phi.getDef();
          phi.setValues(rhs.clone());
        }
        return result;
      }
    }

    /**
     * @see com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine.Meeter#meetLocal(int,
     *      int[], BasicBlock)
     */
    public int meetLocal(int n, int[] rhs, BasicBlock bb) {
      if (allTheSame(rhs)) {
        for (int i = 0; i < rhs.length; i++) {
          if (rhs[i] != TOP) {
            return rhs[i];
          }
        }
        // didn't find anything but TOP
        return TOP;
      } else {
        SSACFG.BasicBlock newBB = (com.ibm.wala.ssa.SSACFG.BasicBlock) cfg.getNode(shrikeCFG.getNumber(bb));
        if (bb.isExitBlock()) {
          // no phis in exit block please
          return TOP;
        }
        // if we already have a phi for this local
        SSAPhiInstruction phi = newBB.getPhiForLocal(n);
        int result;
        if (phi == null) {
          // no phi already exists. create one.
          result = symbolTable.newPhi(rhs);
          PhiValue v = symbolTable.getPhiValue(result);
          phi = v.getPhiInstruction();
          newBB.addPhiForLocal(n, phi);
        } else {
          // already created a phi. update it to account for the
          // new merge.
          result = phi.getDef();
          phi.setValues(rhs.clone());
        }
        return result;
      }
    }

    /**
     * Are all rhs values all the same? Note, we consider TOP (-1) to be same as
     * everything else.
     * 
     * @param rhs
     * @return boolean
     */
    private boolean allTheSame(int[] rhs) {
      int x = -1;
      // set x := the first non-TOP value
      int i = 0;
      for (i = 0; i < rhs.length; i++) {
        if (rhs[i] != TOP) {
          x = rhs[i];
          break;
        }
      }
      // check the remaining values
      for (i = i + 1; i < rhs.length; i++) {
        if (rhs[i] != x && rhs[i] != TOP)
          return false;
      }
      return true;
    }

    /**
     * @see com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine.Meeter#meetStackAtCatchBlock(BasicBlock)
     */
    public int meetStackAtCatchBlock(BasicBlock bb) {
      int bbNumber = shrikeCFG.getNumber(bb);
      SSACFG.ExceptionHandlerBasicBlock newBB = (SSACFG.ExceptionHandlerBasicBlock) cfg.getNode(bbNumber);
      SSAGetCaughtExceptionInstruction s = newBB.getCatchInstruction();
      int exceptionValue;
      if (s == null) {
        exceptionValue = symbolTable.newSymbol();
        s = new SSAGetCaughtExceptionInstruction(bbNumber, exceptionValue);
        newBB.setCatchInstruction(s);
      } else {
        exceptionValue = s.getException();
      }
      return exceptionValue;

    }
  }

  @Override
  protected void initializeVariables() {
    MachineState entryState = getEntryState();
    int parameterNumber = 0;
    int local = -1;
    for (int i = 0; i < method.getNumberOfParameters(); i++) {
      local++;
      TypeReference t = method.getParameterType(i);
      if (t != null) {
        int symbol = symbolTable.getParameter(parameterNumber++);
        entryState.setLocal(local, symbol);
        if (t.equals(TypeReference.Double) || t.equals(TypeReference.Long)) {
          local++;
        }
      }
    }

    // This useless value ensures that the state cannot be empty, even
    // for a static method with no arguments in blocks with an empty stack
    // and no locals being used. This ensures that propagation of the
    // state thru the CFGSystem will always show changes the first time
    // it reaches a block, and thus no piece of the CFG will be skipped.
    //
    // (note that this bizarre state really happened, in java_cup)
    //
    // SJF: I don't understand how this is supposed to work. It
    // causes a bug right now in normal cases, so I'm commenting it out
    // for now. If there's a problem, let's add a regression test
    // to catch it.
    //
    entryState.push(symbolTable.newSymbol());
  }

  /**
   * This class defines the type abstractions for this analysis and the flow
   * function for each instruction in the ShrikeBT IR.
   */
  private static class SymbolicPropagator extends BasicStackFlowProvider {
    private final boolean addPiForInstanceOf;

    private final boolean addPiForNullCheck;

    private final boolean addPiForFieldSelect;

    private final boolean addPiForDispatchSelect;

    final SSAInstruction[] instructions;

    final SymbolTable symbolTable;

    final ShrikeCFG shrikeCFG;

    final SSACFG cfg;

    final ClassLoaderReference loader;

    private SSAInstruction[] creators;

    final SSA2LocalMap localMap;

    public SymbolicPropagator(ShrikeCFG shrikeCFG, SSAInstruction[] instructions, SymbolTable symbolTable, SSA2LocalMap localMap,
        SSACFG cfg, boolean addPiForInstanceOf, boolean addPiForNullCheck, boolean addPiForFieldSelect,
        boolean addPiForDispatchSelect) {
      super(shrikeCFG);
      this.addPiForInstanceOf = addPiForInstanceOf;
      this.addPiForNullCheck = addPiForNullCheck;
      this.addPiForFieldSelect = addPiForFieldSelect;
      this.addPiForDispatchSelect = addPiForDispatchSelect;
      this.cfg = cfg;
      this.creators = new SSAInstruction[0];
      this.shrikeCFG = shrikeCFG;
      this.instructions = instructions;
      this.symbolTable = symbolTable;
      this.loader = shrikeCFG.getMethod().getDeclaringClass().getClassLoader().getReference();
      this.localMap = localMap;
      init(this.new NodeVisitor(), this.new EdgeVisitor());
    }

    @Override
    public boolean needsEdgeFlow() {
      return addPiForInstanceOf || addPiForNullCheck || addPiForFieldSelect || addPiForDispatchSelect;
    }

    private void emitInstruction(SSAInstruction s) {
      instructions[getCurrentInstructionIndex()] = s;
      for (int i = 0; i < s.getNumberOfDefs(); i++) {
        if (creators.length < (s.getDef(i) + 1)) {
          SSAInstruction[] arr = new SSAInstruction[2 * s.getDef(i)];
          System.arraycopy(creators, 0, arr, 0, creators.length);
          creators = arr;
        }

        creators[s.getDef(i)] = s;
      }
    }

    private SSAInstruction getCurrentInstruction() {
      return instructions[getCurrentInstructionIndex()];
    }

    /**
     * If we've already created the current instruction, return the value number
     * def'ed by the current instruction. Else, create a new symbol.
     */
    private int reuseOrCreateDef() {
      if (getCurrentInstruction() == null) {
        return symbolTable.newSymbol();
      } else {
        return getCurrentInstruction().getDef();
      }
    }

    /**
     * If we've already created the current instruction, return the value number
     * representing the exception the instruction may throw. Else, create a new
     * symbol
     */
    private int reuseOrCreateException() {

      if (Assertions.verifyAssertions) {
        if (getCurrentInstruction() != null) {
          Assertions._assert(getCurrentInstruction() instanceof SSAInvokeInstruction);
        }
      }
      if (getCurrentInstruction() == null) {
        return symbolTable.newSymbol();
      } else {
        SSAInvokeInstruction s = (SSAInvokeInstruction) getCurrentInstruction();
        return s.getException();
      }
    }

    /**
     * Update the machine state to account for an instruction
     */
    class NodeVisitor extends BasicStackMachineVisitor {

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitArrayLength(ArrayLengthInstruction)
       */
      @Override
      public void visitArrayLength(com.ibm.wala.shrikeBT.ArrayLengthInstruction instruction) {

        int arrayRef = workingState.pop();
        int length = reuseOrCreateDef();

        workingState.push(length);
        emitInstruction(new SSAArrayLengthInstruction(length, arrayRef));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitArrayLoad(ArrayLoadInstruction)
       */
      @Override
      public void visitArrayLoad(com.ibm.wala.shrikeBT.ArrayLoadInstruction instruction) {

        int index = workingState.pop();
        int arrayRef = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(new SSAArrayLoadInstruction(result, arrayRef, index, t));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitArrayStore(ArrayStoreInstruction)
       */
      @Override
      public void visitArrayStore(com.ibm.wala.shrikeBT.ArrayStoreInstruction instruction) {

        int value = workingState.pop();
        int index = workingState.pop();
        int arrayRef = workingState.pop();
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(new SSAArrayStoreInstruction(arrayRef, index, value, t));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitBinaryOp(BinaryOpInstruction)
       */
      @Override
      public void visitBinaryOp(com.ibm.wala.shrikeBT.BinaryOpInstruction instruction) {

        int val2 = workingState.pop();
        int val1 = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        emitInstruction(new SSABinaryOpInstruction(instruction.getOperator(), result, val1, val2));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitCheckCast(CheckCastInstruction)
       */
      @Override
      public void visitCheckCast(com.ibm.wala.shrikeBT.CheckCastInstruction instruction) {

        int val = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(new SSACheckCastInstruction(result, val, t));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitComparison(ComparisonInstruction)
       */
      @Override
      public void visitComparison(com.ibm.wala.shrikeBT.ComparisonInstruction instruction) {

        int val2 = workingState.pop();
        int val1 = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        emitInstruction(new SSAComparisonInstruction(instruction.getOpcode(), result, val1, val2));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitConditionalBranch(ConditionalBranchInstruction)
       */
      @Override
      public void visitConditionalBranch(com.ibm.wala.shrikeBT.ConditionalBranchInstruction instruction) {
        int val2 = workingState.pop();
        int val1 = workingState.pop();

        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(new SSAConditionalBranchInstruction(instruction.getOperator(), t, val1, val2));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitConstant(ConstantInstruction)
       */
      @Override
      public void visitConstant(com.ibm.wala.shrikeBT.ConstantInstruction instruction) {

        TypeReference type = ShrikeUtil.makeTypeReference(ClassLoaderReference.Primordial, instruction.getType());
        int symbol = 0;
        if (type == TypeReference.Null) {
          symbol = symbolTable.getNullConstant();
        } else if (type == TypeReference.Int) {
          Integer value = (Integer) instruction.getValue();
          symbol = symbolTable.getConstant(value.intValue());
        } else if (type == TypeReference.Long) {
          Long value = (Long) instruction.getValue();
          symbol = symbolTable.getConstant(value.longValue());
        } else if (type == TypeReference.Float) {
          Float value = (Float) instruction.getValue();
          symbol = symbolTable.getConstant(value.floatValue());
        } else if (type == TypeReference.Double) {
          Double value = (Double) instruction.getValue();
          symbol = symbolTable.getConstant(value.doubleValue());
        } else if (type == TypeReference.JavaLangString) {
          String value = (String) instruction.getValue();
          symbol = symbolTable.getConstant(value);
        } else if (type == TypeReference.JavaLangClass) {
          TypeReference rval = ShrikeUtil.makeTypeReference(ClassLoaderReference.Primordial, (String) instruction.getValue());
          symbol = reuseOrCreateDef();
          emitInstruction(new SSALoadClassInstruction(symbol, rval));
        } else {
          Assertions.UNREACHABLE("unexpected " + type);
        }
        workingState.push(symbol);
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitConversion(ConversionInstruction)
       */
      @Override
      public void visitConversion(com.ibm.wala.shrikeBT.ConversionInstruction instruction) {

        int val = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);

        TypeReference fromType = ShrikeUtil.makeTypeReference(ClassLoaderReference.Primordial, instruction.getFromType());
        TypeReference toType = ShrikeUtil.makeTypeReference(ClassLoaderReference.Primordial, instruction.getToType());

        emitInstruction(new SSAConversionInstruction(result, val, fromType, toType));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitGet(GetInstruction)
       */
      @Override
      public void visitGet(com.ibm.wala.shrikeBT.GetInstruction instruction) {
        int result = reuseOrCreateDef();
        if (instruction.isStatic()) {
          FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(),
              instruction.getFieldType());
          emitInstruction(new SSAGetInstruction(result, f));
        } else {
          int ref = workingState.pop();
          FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(),
              instruction.getFieldType());
          emitInstruction(new SSAGetInstruction(result, ref, f));
        }
        workingState.push(result);
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitGoto(GotoInstruction)
       */
      @Override
      public void visitGoto(com.ibm.wala.shrikeBT.GotoInstruction instruction) {
        emitInstruction(new SSAGotoInstruction());
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitInstanceof(InstanceofInstruction)
       */
      @Override
      public void visitInstanceof(com.ibm.wala.shrikeBT.InstanceofInstruction instruction) {

        int ref = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(new SSAInstanceofInstruction(result, ref, t));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitInvoke(InvokeInstruction)
       */
      @Override
      public void visitInvoke(com.ibm.wala.shrikeBT.InvokeInstruction instruction) {
        int n = instruction.getPoppedCount();
        int[] params = new int[n];
        for (int i = n - 1; i >= 0; i--) {
          params[i] = workingState.pop();
        }
        MethodReference m = MethodReference.findOrCreate(loader, instruction.getClassType(), instruction.getMethodName(),
            instruction.getMethodSignature());
        IInvokeInstruction.Dispatch code = ShrikeUtil.getInvocationCode(instruction);
        CallSiteReference site = CallSiteReference.make(getCurrentProgramCounter(), m, code);
        int exc = reuseOrCreateException();
        if (instruction.getPushedWordSize() > 0) {
          int result = reuseOrCreateDef();
          workingState.push(result);
          emitInstruction(new SSAInvokeInstruction(result, params, exc, site));
        } else {
          emitInstruction(new SSAInvokeInstruction(params, exc, site));
        }
      }

      /*
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitLocalStore(com.ibm.wala.shrikeBT.StoreInstruction)
       */
      @Override
      public void visitLocalStore(StoreInstruction instruction) {
        if (localMap != null) {
          localMap.startRange(getCurrentInstructionIndex(), instruction.getVarIndex(), workingState.peek());
        }
        super.visitLocalStore(instruction);
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitMonitor(MonitorInstruction)
       */
      @Override
      public void visitMonitor(com.ibm.wala.shrikeBT.MonitorInstruction instruction) {

        int ref = workingState.pop();
        emitInstruction(new SSAMonitorInstruction(ref, instruction.isEnter()));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitNew(NewInstruction)
       */
      @Override
      public void visitNew(com.ibm.wala.shrikeBT.NewInstruction instruction) {
        int result = reuseOrCreateDef();
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        NewSiteReference ref = NewSiteReference.make(getCurrentProgramCounter(), t);
        if (t.isArrayType()) {
          int[] sizes = new int[t.getDimensionality()];
          for (int i = 0; i<instruction.getArrayBoundsCount(); i++) {
            sizes[i] = workingState.pop();
          }
          for (int i = instruction.getArrayBoundsCount(); i< sizes.length; i++) {
            sizes[i] = symbolTable.getConstant(0);
          }
          emitInstruction(new SSANewInstruction(result, ref, sizes));
        } else {
          emitInstruction(new SSANewInstruction(result, ref));
          popN(instruction);
        }
        workingState.push(result);
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitGet(GetInstruction)
       */
      @Override
      public void visitPut(com.ibm.wala.shrikeBT.PutInstruction instruction) {
        int value = workingState.pop();
        if (instruction.isStatic()) {
          FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(),
              instruction.getFieldType());
          emitInstruction(new SSAPutInstruction(value, f));
        } else {
          int ref = workingState.pop();
          FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(),
              instruction.getFieldType());
          emitInstruction(new SSAPutInstruction(ref, value, f));
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitReturn(ReturnInstruction)
       */
      @Override
      public void visitReturn(com.ibm.wala.shrikeBT.ReturnInstruction instruction) {
        if (instruction.getPoppedCount() == 1) {
          int result = workingState.pop();
          TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
          emitInstruction(new SSAReturnInstruction(result, t.isPrimitiveType()));
        } else {
          emitInstruction(new SSAReturnInstruction());
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitShift(ShiftInstruction)
       */
      @Override
      public void visitShift(com.ibm.wala.shrikeBT.ShiftInstruction instruction) {
        int val2 = workingState.pop();
        int val1 = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        emitInstruction(new SSABinaryOpInstruction(instruction.getOperator(), result, val1, val2));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitSwitch(SwitchInstruction)
       */
      @Override
      public void visitSwitch(com.ibm.wala.shrikeBT.SwitchInstruction instruction) {

        int val = workingState.pop();

        emitInstruction(new SSASwitchInstruction(val, instruction.getDefaultLabel(), instruction.getCasesAndLabels()));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitThrow(ThrowInstruction)
       */
      @Override
      public void visitThrow(com.ibm.wala.shrikeBT.ThrowInstruction instruction) {

        int exception = workingState.pop();
        workingState.clearStack();
        workingState.push(exception);
        emitInstruction(new SSAThrowInstruction(exception));
      }

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitUnaryOp(UnaryOpInstruction)
       */
      @Override
      public void visitUnaryOp(com.ibm.wala.shrikeBT.UnaryOpInstruction instruction) {
        int val = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        emitInstruction(new SSAUnaryOpInstruction(instruction.getOperator(), result, val));
      }

    }

    /**
     * @param piCause
     * @param ref
     */
    private void reuseOrCreatePi(SSAInstruction piCause, int ref) {
      int n = getCurrentInstructionIndex();
      SSACFG.BasicBlock bb = cfg.getBlockForInstruction(n);

      BasicBlock path = getCurrentSuccessor();
      int outNum = shrikeCFG.getNumber(path);

      SSAPiInstruction pi = bb.getPiForRefAndPath(ref, path);
      if (pi == null) {
        pi = new SSAPiInstruction(symbolTable.newSymbol(), ref, outNum, piCause);
        bb.addPiForRefAndPath(ref, path, pi);
      }

      workingState.replaceValue(ref, pi.getDef());
    }

    private void maybeInsertPi(int val) {
      if ((addPiForFieldSelect) && (creators.length > val) && (creators[val] instanceof SSAGetInstruction)
          && !((SSAGetInstruction) creators[val]).isStatic()) {
        reuseOrCreatePi(creators[val], val);
      } else if ((addPiForDispatchSelect)
          && (creators.length > val)
          && (creators[val] instanceof SSAInvokeInstruction)
          && (((SSAInvokeInstruction) creators[val]).getInvocationCode() == IInvokeInstruction.Dispatch.VIRTUAL || ((SSAInvokeInstruction) creators[val])
              .getInvocationCode() == IInvokeInstruction.Dispatch.INTERFACE)) {
        reuseOrCreatePi(creators[val], val);
      }
    }

    private void maybeInsertPi(SSAInstruction cond, int val1, int val2) {
      if ((addPiForInstanceOf) && (creators.length > val1) && (creators[val1] instanceof SSAInstanceofInstruction)
          && (symbolTable.isBooleanOrZeroOneConstant(val2))) {
        reuseOrCreatePi(creators[val1], creators[val1].getUse(0));
      } else if ((addPiForNullCheck) && (symbolTable.isNullConstant(val2))) {
        reuseOrCreatePi(cond, val1);
      } else if (symbolTable.isIntegerConstant(val2)) {
        maybeInsertPi(val1);
      }
    }

    class EdgeVisitor extends com.ibm.wala.shrikeBT.Instruction.Visitor {

      /**
       * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitSwitch(SwitchInstruction)
       */
      @Override
      public void visitSwitch(com.ibm.wala.shrikeBT.SwitchInstruction instruction) {
        int val = getCurrentInstruction().getUse(0);
        maybeInsertPi(val);
      }

      @Override
      public void visitConditionalBranch(com.ibm.wala.shrikeBT.ConditionalBranchInstruction instruction) {
        int val1 = getCurrentInstruction().getUse(0);
        int val2 = getCurrentInstruction().getUse(1);
        maybeInsertPi(getCurrentInstruction(), val1, val2);
        maybeInsertPi(getCurrentInstruction(), val2, val1);
      }
    }

    @Override
    public com.ibm.wala.shrikeBT.Instruction[] getInstructions() {
      try {
        return ((ShrikeCTMethod) shrikeCFG.getMethod()).getInstructions();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
        return null;
      }
    }
  }

  /**
   * Build the IR
   */
  public void build() {
    solve();
    if (localMap != null) {
      localMap.finishLocalMap(this);
    }
  }

  public SSA2LocalMap getLocalMap() {
    return localMap;
  }

  /**
   * A logical mapping from <pc, valueNumber> -> local number Note: make sure
   * this class remains static: this persists as part of the IR!!
   */
  private static class SSA2LocalMap implements com.ibm.wala.ssa.IR.SSA2LocalMap {

    private final ShrikeCFG shrikeCFG;

    /**
     * Mapping Integer -> IntPair where p maps to (vn,L) iff we've started a
     * range at pc p where value number vn corresponds to local L
     */
    private final IntPair[] localStoreMap;

    /**
     * For each basic block i and local j, block2LocalState[i][j] gives the
     * contents of local j at the start of block i
     */
    private final int[][] block2LocalState;

    /**
     * maximum number of locals used at any program point
     */
    private final int maxLocals;

    /**
     * @param nInstructions
     *          number of instructions in the bytecode for this method
     * @param nBlocks
     *          number of basic blocks in the CFG
     */
    SSA2LocalMap(ShrikeCFG shrikeCfg, int nInstructions, int nBlocks, int maxLocals) {
      shrikeCFG = shrikeCfg;
      localStoreMap = new IntPair[nInstructions];
      block2LocalState = new int[nBlocks][];
      this.maxLocals = maxLocals;
    }

    /**
     * Record the beginning of a new range, starting at the given program
     * counter, in which a particular value number corresponds to a particular
     * local number
     * 
     * @param pc
     * @param valueNumber
     * @param localNumber
     */
    void startRange(int pc, int localNumber, int valueNumber) {
      if (Assertions.verifyAssertions) {
        int max = shrikeCFG.getMethod().getMaxLocals();
        if (localNumber >= max) {
          Assertions._assert(false, "invalid local " + localNumber + ">" + max);
        }
      }

      localStoreMap[pc] = new IntPair(valueNumber, localNumber);
    }

    /**
     * Finish populating the map of local variable information
     * 
     * @param builder
     */
    private void finishLocalMap(SSABuilder builder) {
      for (Iterator it = shrikeCFG.iterator(); it.hasNext();) {
        ShrikeCFG.BasicBlock bb = (ShrikeCFG.BasicBlock) it.next();
        MachineState S = builder.getIn(bb);
        int number = bb.getNumber();
        block2LocalState[number] = S.getLocals();
      }
    }

    /**
     * @param index -
     *          index into IR instruction array
     * @param vn -
     *          value number
     */
    public String[] getLocalNames(int index, int vn) {
      try {
        if (!shrikeCFG.getMethod().hasLocalVariableTable()) {
          return null;
        } else {
          int[] localNumbers = findLocalsForValueNumber(index, vn);
          if (localNumbers == null) {
            return null;
          } else {
            ShrikeCTMethod m = (ShrikeCTMethod) shrikeCFG.getMethod();
            String[] result = new String[localNumbers.length];
            for (int i = 0; i < localNumbers.length; i++) {
              result[i] = m.getLocalVariableName(m.getBytecodeIndex(index), localNumbers[i]);
            }
            return result;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
        return null;
      }
    }

    /**
     * @param pc
     *          a program counter (index into ShrikeBT instruction array)
     * @param vn
     *          a value number
     * @return if we know that immediately after the given program counter, v_vn
     *         corresponds to some set of locals, then return an array of the
     *         local numbers. else return null.
     */
    private int[] findLocalsForValueNumber(int pc, int vn) {

      IBasicBlock bb = shrikeCFG.getBlockForInstruction(pc);
      int firstInstruction = bb.getFirstInstructionIndex();
      // walk forward from the first instruction to reconstruct the
      // state of the locals at this pc
      int[] locals = block2LocalState[bb.getNumber()];
      if (locals == null) {
        locals = allocateNewLocalsArray();
      }
      for (int i = firstInstruction; i <= pc; i++) {
        if (localStoreMap[i] != null) {
          IntPair p = localStoreMap[i];
          locals[p.getY()] = p.getX();
        }
      }
      return extractIndices(locals, vn);
    }

    public int[] allocateNewLocalsArray() {
      int[] result = new int[maxLocals];
      for (int i = 0; i < maxLocals; i++) {
        result[i] = OPTIMISTIC ? TOP : BOTTOM;
      }
      return result;
    }

    /**
     * @return the indices i s.t. x[i] == y, or null if none found.
     */
    private int[] extractIndices(int[] x, int y) {
      int count = 0;
      for (int i = 0; i < x.length; i++) {
        if (x[i] == y) {
          count++;
        }
      }
      if (count == 0) {
        return null;
      } else {
        int[] result = new int[count];
        int j = 0;
        for (int i = 0; i < x.length; i++) {
          if (x[i] == y) {
            result[j++] = i;
          }
        }
        return result;
      }
    }
  }
}
