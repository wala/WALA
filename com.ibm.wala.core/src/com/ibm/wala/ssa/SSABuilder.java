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

import com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.cfg.ShrikeCFG.BasicBlock;
import com.ibm.wala.classLoader.BytecodeLanguage;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IConversionInstruction;
import com.ibm.wala.shrikeBT.IGetInstruction;
import com.ibm.wala.shrikeBT.IInstanceofInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.ILoadIndirectInstruction;
import com.ibm.wala.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrikeBT.IPutInstruction;
import com.ibm.wala.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrikeBT.IStoreIndirectInstruction;
import com.ibm.wala.shrikeBT.IStoreInstruction;
import com.ibm.wala.shrikeBT.ITypeTestInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.IndirectionData;
import com.ibm.wala.shrikeBT.InvokeDynamicInstruction;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.ShrikeIndirectionData.ShrikeLocalName;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.shrike.ShrikeUtil;

/**
 * This class constructs an SSA {@link IR} from a backing ShrikeBT instruction stream.
 * 
 * The basic algorithm here is an abstract interpretation over the Java bytecode to determine types of stack locations and local
 * variables. As a side effect, the flow functions of the abstract interpretation emit instructions, eliminating the stack
 * abstraction and moving to a register-transfer language in SSA form.
 */
public class SSABuilder extends AbstractIntStackMachine {

  public static SSABuilder make(IBytecodeMethod method, SSACFG cfg, ShrikeCFG scfg, SSAInstruction[] instructions,
      SymbolTable symbolTable, boolean buildLocalMap, SSAPiNodePolicy piNodePolicy) throws IllegalArgumentException {
    if (scfg == null) {
      throw new IllegalArgumentException("scfg == null");
    }
    return new SSABuilder(method, cfg, scfg, instructions, symbolTable, buildLocalMap, piNodePolicy);
  }

  /**
   * A wrapper around the method being analyzed.
   */
  final private IBytecodeMethod method;

  /**
   * Governing symbol table
   */
  final private SymbolTable symbolTable;

  /**
   * A logical mapping from &lt;bcIndex, valueNumber&gt; -&gt; local number if null, don't build it.
   */
  private final SSA2LocalMap localMap;

  /**
   * a factory to create concrete instructions
   */
  private final SSAInstructionFactory insts;

  /**
   * information about indirect use of local variables in the bytecode
   */
  private final IndirectionData bytecodeIndirections;
  
  private final ShrikeIndirectionData ssaIndirections;
  
  private SSABuilder(IBytecodeMethod method, SSACFG cfg, ShrikeCFG scfg, SSAInstruction[] instructions, SymbolTable symbolTable,
      boolean buildLocalMap, SSAPiNodePolicy piNodePolicy) {
    super(scfg);
    localMap = buildLocalMap ? new SSA2LocalMap(scfg, instructions.length, cfg.getNumberOfNodes()) : null;
    init(new SymbolTableMeeter(symbolTable, cfg, scfg), new SymbolicPropagator(scfg, instructions, symbolTable,
        localMap, cfg, piNodePolicy));
    this.method = method;
    this.symbolTable = symbolTable;
    this.insts = method.getDeclaringClass().getClassLoader().getInstructionFactory();
    this.bytecodeIndirections = method.getIndirectionData();
    this.ssaIndirections = new ShrikeIndirectionData(instructions.length);
    assert cfg != null : "Null CFG";
  }

  private class SymbolTableMeeter implements Meeter {

    final SSACFG cfg;


    final SymbolTable symbolTable;

    final ShrikeCFG shrikeCFG;

    SymbolTableMeeter(SymbolTable symbolTable, SSACFG cfg, ShrikeCFG shrikeCFG) {
      this.cfg = cfg;
      this.symbolTable = symbolTable;
      this.shrikeCFG = shrikeCFG;
    }

    @Override
    public int meetStack(int slot, int[] rhs, BasicBlock bb) {

      assert bb != null : "null basic block";

      if (bb.isExitBlock()) {
        return TOP;
      }

      if (allTheSame(rhs)) {
        for (int rh : rhs) {
          if (rh != TOP) {
            return rh;
          }
        }
        // didn't find anything but TOP
        return TOP;
      } else {
        SSACFG.BasicBlock newBB = cfg.getNode(shrikeCFG.getNumber(bb));
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
     * @see com.ibm.wala.analysis.stackMachine.AbstractIntStackMachine.Meeter#meetLocal(int, int[], BasicBlock)
     */
    @Override
    public int meetLocal(int n, int[] rhs, BasicBlock bb) {
      if (allTheSame(rhs)) {
        for (int rh : rhs) {
          if (rh != TOP) {
            return rh;
          }
        }
        // didn't find anything but TOP
        return TOP;
      } else {
        SSACFG.BasicBlock newBB = cfg.getNode(shrikeCFG.getNumber(bb));
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
     * Are all rhs values all the same? Note, we consider TOP (-1) to be same as everything else.
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
    @Override
    public int meetStackAtCatchBlock(BasicBlock bb) {
      int bbNumber = shrikeCFG.getNumber(bb);
      SSACFG.ExceptionHandlerBasicBlock newBB = (SSACFG.ExceptionHandlerBasicBlock) cfg.getNode(bbNumber);
      SSAGetCaughtExceptionInstruction s = newBB.getCatchInstruction();
      int exceptionValue;
      if (s == null) {
        exceptionValue = symbolTable.newSymbol();
        s = insts.GetCaughtExceptionInstruction(SSAInstruction.NO_INDEX, bbNumber, exceptionValue);
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
    entryState.push(symbolTable.newSymbol());
  }

  /**
   * This class defines the type abstractions for this analysis and the flow function for each instruction in the ShrikeBT IR.
   */
  private class SymbolicPropagator extends BasicStackFlowProvider {

    final SSAInstruction[] instructions;

    final SymbolTable symbolTable;

    final ShrikeCFG shrikeCFG;

    final SSACFG cfg;

    final ClassLoaderReference loader;

    /**
     * creators[i] holds the instruction that defs value number i
     */
    private SSAInstruction[] creators;

    final SSA2LocalMap localMap;

    final SSAPiNodePolicy piNodePolicy;

    public SymbolicPropagator(ShrikeCFG shrikeCFG, SSAInstruction[] instructions, SymbolTable symbolTable, SSA2LocalMap localMap,
        SSACFG cfg, SSAPiNodePolicy piNodePolicy) {
      super(shrikeCFG);
      this.piNodePolicy = piNodePolicy;
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
      return piNodePolicy != null;
    }

    private void emitInstruction(SSAInstruction s) {
      if (s != null) {
        instructions[getCurrentInstructionIndex()] = s;
        for (int i = 0; i < s.getNumberOfDefs(); i++) {
          if (creators.length < (s.getDef(i) + 1)) {
            SSAInstruction[] arr = new SSAInstruction[2 * s.getDef(i)];
            System.arraycopy(creators, 0, arr, 0, creators.length);
            creators = arr;
          }

          assert s.getDef(i) != -1 : "invalid def " + i + " for " + s;
        
          creators[s.getDef(i)] = s;
        }
      }
    }

    private SSAInstruction getCurrentInstruction() {
      return instructions[getCurrentInstructionIndex()];
    }

    /**
     * If we've already created the current instruction, return the value number def'ed by the current instruction. Else, create a
     * new symbol.
     */
    private int reuseOrCreateDef() {
      if (getCurrentInstruction() == null) {
        return symbolTable.newSymbol();
      } else {
        return getCurrentInstruction().getDef();
      }
    }

    /**
     * If we've already created the current instruction, return the value number representing the exception the instruction may
     * throw. Else, create a new symbol
     */
    private int reuseOrCreateException() {

      if (getCurrentInstruction() != null) {
        assert getCurrentInstruction() instanceof SSAInvokeInstruction;
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
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayLength(ArrayLengthInstruction)
       */
      @Override
      public void visitArrayLength(com.ibm.wala.shrikeBT.ArrayLengthInstruction instruction) {

        int arrayRef = workingState.pop();
        int length = reuseOrCreateDef();

        workingState.push(length);
        emitInstruction(insts.ArrayLengthInstruction(getCurrentInstructionIndex(), length, arrayRef));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayLoad(IArrayLoadInstruction)
       */
      @Override
      public void visitArrayLoad(IArrayLoadInstruction instruction) {
        int index = workingState.pop();
        int arrayRef = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        if (instruction.isAddressOf()) {
          emitInstruction(insts.AddressOfInstruction(getCurrentInstructionIndex(), result, arrayRef, index, t));
        } else {
           emitInstruction(insts.ArrayLoadInstruction(getCurrentInstructionIndex(), result, arrayRef, index, t));
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayStore(IArrayStoreInstruction)
       */
      @Override
      public void visitArrayStore(IArrayStoreInstruction instruction) {

        int value = workingState.pop();
        int index = workingState.pop();
        int arrayRef = workingState.pop();
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(insts.ArrayStoreInstruction(getCurrentInstructionIndex(), arrayRef, index, value, t));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitBinaryOp(IBinaryOpInstruction)
       */
      @Override
      public void visitBinaryOp(IBinaryOpInstruction instruction) {
        int val2 = workingState.pop();
        int val1 = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        boolean isFloat = instruction.getType().equals(TYPE_double) || instruction.getType().equals(TYPE_float);
        emitInstruction(insts.BinaryOpInstruction(getCurrentInstructionIndex(), instruction.getOperator(), instruction.throwsExceptionOnOverflow(), instruction
            .isUnsigned(), result, val1, val2, !isFloat));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitCheckCast(ITypeTestInstruction)
       */
      @Override
      public void visitCheckCast(ITypeTestInstruction instruction) {
        int val = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        if (! instruction.firstClassTypes()) {
          String[] typeNames = instruction.getTypes();
          TypeReference[] t = new TypeReference[ typeNames.length ];
          for(int i = 0; i < typeNames.length; i++) {
            t[i] = ShrikeUtil.makeTypeReference(loader, typeNames[i]);
          }
          emitInstruction(insts.CheckCastInstruction(getCurrentInstructionIndex(), result, val, t, instruction.isPEI()));
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitComparison(IComparisonInstruction)
       */
      @Override
      public void visitComparison(IComparisonInstruction instruction) {

        int val2 = workingState.pop();
        int val1 = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        emitInstruction(insts.ComparisonInstruction(getCurrentInstructionIndex(), instruction.getOperator(), result, val1, val2));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConditionalBranch(IConditionalBranchInstruction)
       */
      @Override
      public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
        int val2 = workingState.pop();
        int val1 = workingState.pop();

        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(insts.ConditionalBranchInstruction(getCurrentInstructionIndex(), instruction.getOperator(), t, val1, val2, instruction.getTarget()));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConstant(ConstantInstruction)
       */
      @Override
      public void visitConstant(com.ibm.wala.shrikeBT.ConstantInstruction instruction) {
        Language l = cfg.getMethod().getDeclaringClass().getClassLoader().getLanguage();
        
        TypeReference type = l.getConstantType(instruction.getValue());
        int symbol = 0;
        if (l.isNullType(type)) {
          symbol = symbolTable.getNullConstant();
        } else if (l.isIntType(type)) {
          Integer value = (Integer) instruction.getValue();
          symbol = symbolTable.getConstant(value.intValue());
        } else if (l.isLongType(type)) {
          Long value = (Long) instruction.getValue();
          symbol = symbolTable.getConstant(value.longValue());
        } else if (l.isFloatType(type)) {
          Float value = (Float) instruction.getValue();
          symbol = symbolTable.getConstant(value.floatValue());
        } else if (l.isDoubleType(type)) {
          Double value = (Double) instruction.getValue();
          symbol = symbolTable.getConstant(value.doubleValue());
        } else if (l.isStringType(type)) {
          String value = (String) instruction.getValue();
          symbol = symbolTable.getConstant(value);
        } else if (l.isMetadataType(type)) {
          Object rval = l.getMetadataToken(instruction.getValue());
          symbol = reuseOrCreateDef();
          emitInstruction(insts.LoadMetadataInstruction(getCurrentInstructionIndex(), symbol, type, rval));
        } else {
          Assertions.UNREACHABLE("unexpected " + type);
        }
        workingState.push(symbol);
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConversion(IConversionInstruction)
       */
      @Override
      public void visitConversion(IConversionInstruction instruction) {

        int val = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);

        TypeReference fromType = ShrikeUtil.makeTypeReference(loader, instruction.getFromType());
        TypeReference toType = ShrikeUtil.makeTypeReference(loader, instruction.getToType());

        emitInstruction(insts.ConversionInstruction(getCurrentInstructionIndex(), result, val, fromType, toType, instruction.throwsExceptionOnOverflow()));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitGet(IGetInstruction)
       */
      @Override
      public void visitGet(IGetInstruction instruction) {
        int result = reuseOrCreateDef();
        FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(),
            instruction.getFieldType());
        if (instruction.isAddressOf()) {
          int ref = instruction.isStatic()? -1: workingState.pop();
          emitInstruction(insts.AddressOfInstruction(getCurrentInstructionIndex(), result, ref, f, f.getFieldType()));
        } else if (instruction.isStatic()) {
          emitInstruction(insts.GetInstruction(getCurrentInstructionIndex(), result, f));
        } else {
          int ref = workingState.pop();
          emitInstruction(insts.GetInstruction(getCurrentInstructionIndex(), result, ref, f));
        }
        assert result != -1;
        workingState.push(result);
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitGoto(GotoInstruction)
       */
      @Override
      public void visitGoto(com.ibm.wala.shrikeBT.GotoInstruction instruction) {
        emitInstruction(insts.GotoInstruction(getCurrentInstructionIndex(), instruction.getLabel()));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitInstanceof(IInstanceofInstruction)
       */
      @Override
      public void visitInstanceof(IInstanceofInstruction instruction) {

        int ref = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(insts.InstanceofInstruction(getCurrentInstructionIndex(), result, ref, t));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitInvoke(IInvokeInstruction)
       */
      @Override
      public void visitInvoke(IInvokeInstruction instruction) {
        doIndirectReads(bytecodeIndirections.indirectlyReadLocals(getCurrentInstructionIndex()));
        int n = instruction.getPoppedCount();
        int[] params = new int[n];
        for (int i = n - 1; i >= 0; i--) {
          params[i] = workingState.pop();
        }
        Language lang = shrikeCFG.getMethod().getDeclaringClass().getClassLoader().getLanguage();
        MethodReference m = ((BytecodeLanguage)lang).getInvokeMethodReference(loader, instruction);
        IInvokeInstruction.IDispatch code = instruction.getInvocationCode();
        CallSiteReference site = CallSiteReference.make(getCurrentProgramCounter(), m, code);
        int exc = reuseOrCreateException();
        
        BootstrapMethod bootstrap = null;
        if (instruction instanceof InvokeDynamicInstruction) {
          bootstrap = ((InvokeDynamicInstruction)instruction).getBootstrap();
        }
        
        if (instruction.getPushedWordSize() > 0) {
          int result = reuseOrCreateDef();
          workingState.push(result);
          emitInstruction(insts.InvokeInstruction(getCurrentInstructionIndex(), result, params, exc, site, bootstrap));
        } else {
          emitInstruction(insts.InvokeInstruction(getCurrentInstructionIndex(), params, exc, site, bootstrap));
        }
        doIndirectWrites(bytecodeIndirections.indirectlyWrittenLocals(getCurrentInstructionIndex()), -1);
      }

      @Override
      public void visitLocalLoad(ILoadInstruction instruction) {
        if (instruction.isAddressOf()) {
          int result = reuseOrCreateDef();
 
          int t = workingState.getLocal(instruction.getVarIndex()); 
          if (t == -1) {
            doIndirectWrites(new int[]{instruction.getVarIndex()}, -1);
            t = workingState.getLocal(instruction.getVarIndex());
          }
          
          TypeReference type = ShrikeUtil.makeTypeReference(loader, instruction.getType());
          emitInstruction(insts.AddressOfInstruction(getCurrentInstructionIndex(), result, t, type));
          workingState.push(result);
        } else {
          super.visitLocalLoad(instruction);
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitLocalStore(IStoreInstruction)
       */
      @Override
      public void visitLocalStore(IStoreInstruction instruction) {
        if (localMap != null) {
          localMap.startRange(getCurrentInstructionIndex(), instruction.getVarIndex(), workingState.peek());
        }
        super.visitLocalStore(instruction);
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitMonitor(MonitorInstruction)
       */
      @Override
      public void visitMonitor(com.ibm.wala.shrikeBT.MonitorInstruction instruction) {

        int ref = workingState.pop();
        emitInstruction(insts.MonitorInstruction(getCurrentInstructionIndex(), ref, instruction.isEnter()));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitNew(NewInstruction)
       */
      @Override
      public void visitNew(com.ibm.wala.shrikeBT.NewInstruction instruction) {
        int result = reuseOrCreateDef();
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        NewSiteReference ref = NewSiteReference.make(getCurrentProgramCounter(), t);
        if (t.isArrayType()) {
          int[] sizes = new int[instruction.getArrayBoundsCount()];
          for (int i = 0; i < instruction.getArrayBoundsCount(); i++) {
            sizes[instruction.getArrayBoundsCount() - 1 - i] = workingState.pop();
          }
          emitInstruction(insts.NewInstruction(getCurrentInstructionIndex(), result, ref, sizes));
        } else {
          emitInstruction(insts.NewInstruction(getCurrentInstructionIndex(), result, ref));
          popN(instruction);
        }
        workingState.push(result);
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitGet(IGetInstruction)
       */
      @Override
      public void visitPut(IPutInstruction instruction) {
        int value = workingState.pop();
        if (instruction.isStatic()) {
          FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(),
              instruction.getFieldType());
          emitInstruction(insts.PutInstruction(getCurrentInstructionIndex(), value, f));
        } else {
          int ref = workingState.pop();
          FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(),
              instruction.getFieldType());
          emitInstruction(insts.PutInstruction(getCurrentInstructionIndex(), ref, value, f));
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitReturn(ReturnInstruction)
       */
      @Override
      public void visitReturn(com.ibm.wala.shrikeBT.ReturnInstruction instruction) {
        if (instruction.getPoppedCount() == 1) {
          int result = workingState.pop();
          TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
          emitInstruction(insts.ReturnInstruction(getCurrentInstructionIndex(), result, t.isPrimitiveType()));
        } else {
          emitInstruction(insts.ReturnInstruction(getCurrentInstructionIndex()));
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitShift(IShiftInstruction)
       */
      @Override
      public void visitShift(IShiftInstruction instruction) {
        int val2 = workingState.pop();
        int val1 = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        emitInstruction(insts.BinaryOpInstruction(getCurrentInstructionIndex(), instruction.getOperator(), false, instruction.isUnsigned(), result, val1, val2,
            true));
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitSwitch(SwitchInstruction)
       */
      @Override
      public void visitSwitch(com.ibm.wala.shrikeBT.SwitchInstruction instruction) {
        int val = workingState.pop();
        emitInstruction(insts.SwitchInstruction(getCurrentInstructionIndex(), val, instruction.getDefaultLabel(), instruction.getCasesAndLabels()));
      }

      private Dominators<ISSABasicBlock> dom = null;

      private int findRethrowException() {
        int index = getCurrentInstructionIndex();
        SSACFG.BasicBlock bb = cfg.getBlockForInstruction(index);
        if (bb.isCatchBlock()) {
          SSACFG.ExceptionHandlerBasicBlock newBB = (SSACFG.ExceptionHandlerBasicBlock) bb;
          SSAGetCaughtExceptionInstruction s = newBB.getCatchInstruction();
          return s.getDef();
        } else {
          // TODO: should we really use dominators here? maybe it would be cleaner to propagate
          // the notion of 'current exception to rethrow' using the abstract interpreter.
          if (dom == null) {
            dom = Dominators.make(cfg, cfg.entry());
          }

          ISSABasicBlock x = bb;
          while (x != null) {
            if (x.isCatchBlock()) {
              SSACFG.ExceptionHandlerBasicBlock newBB = (SSACFG.ExceptionHandlerBasicBlock) x;
              SSAGetCaughtExceptionInstruction s = newBB.getCatchInstruction();
              return s.getDef();
            } else {
              x = dom.getIdom(x);
            }
          }

          // assert false;
          return -1;
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitThrow(ThrowInstruction)
       */
      @Override
      public void visitThrow(com.ibm.wala.shrikeBT.ThrowInstruction instruction) {
        if (instruction.isRethrow()) {
          workingState.clearStack();
          emitInstruction(insts.ThrowInstruction(getCurrentInstructionIndex(), findRethrowException()));
        } else {
          int exception = workingState.pop();
          workingState.clearStack();
          workingState.push(exception);
          emitInstruction(insts.ThrowInstruction(getCurrentInstructionIndex(), exception));
        }
      }

      /**
       * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitUnaryOp(IUnaryOpInstruction)
       */
      @Override
      public void visitUnaryOp(IUnaryOpInstruction instruction) {
        int val = workingState.pop();
        int result = reuseOrCreateDef();
        workingState.push(result);
        emitInstruction(insts.UnaryOpInstruction(getCurrentInstructionIndex(), instruction.getOperator(), result, val));
      }

      private void doIndirectReads(int[] locals) {
        for (int local : locals) {
          ssaIndirections.setUse(getCurrentInstructionIndex(), new ShrikeLocalName(local), workingState.getLocal(local));
        }
      }
      
      @Override
      public void visitLoadIndirect(ILoadIndirectInstruction instruction) { 
        int addressVal = workingState.pop();
        int result = reuseOrCreateDef();
        doIndirectReads(bytecodeIndirections.indirectlyReadLocals(getCurrentInstructionIndex()));
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getPushedType(null));
        emitInstruction(insts.LoadIndirectInstruction(getCurrentInstructionIndex(), result, t, addressVal));
        workingState.push(result);
      }

      private void doIndirectWrites(int[] locals, int rval) {
        for (int local : locals) {
          ShrikeLocalName name = new ShrikeLocalName(local);
          int idx = getCurrentInstructionIndex();
          if (ssaIndirections.getDef(idx, name) == -1) {
            ssaIndirections.setDef(idx, name, rval==-1? symbolTable.newSymbol(): rval);
          }
          workingState.setLocal(local, ssaIndirections.getDef(idx, name));
        }        
      }
      
      @Override
      public void visitStoreIndirect(IStoreIndirectInstruction instruction) {  
        int val = workingState.pop();        
        int addressVal = workingState.pop();
        doIndirectWrites(bytecodeIndirections.indirectlyWrittenLocals(getCurrentInstructionIndex()), val);     
        TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
        emitInstruction(insts.StoreIndirectInstruction(getCurrentInstructionIndex(), addressVal, val, t));
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
        pi = insts.PiInstruction(SSAInstruction.NO_INDEX, symbolTable.newSymbol(), ref, bb.getNumber(), outNum, piCause);
        bb.addPiForRefAndPath(ref, path, pi);
      }

      workingState.replaceValue(ref, pi.getDef());
    }

    // private void maybeInsertPi(int val) {
    // if ((addPiForFieldSelect) && (creators.length > val) && (creators[val] instanceof SSAGetInstruction)
    // && !((SSAGetInstruction) creators[val]).isStatic()) {
    // reuseOrCreatePi(creators[val], val);
    // } else if ((addPiForDispatchSelect)
    // && (creators.length > val)
    // && (creators[val] instanceof SSAInvokeInstruction)
    // && (((SSAInvokeInstruction) creators[val]).getInvocationCode() == IInvokeInstruction.Dispatch.VIRTUAL ||
    // ((SSAInvokeInstruction) creators[val])
    // .getInvocationCode() == IInvokeInstruction.Dispatch.INTERFACE)) {
    // reuseOrCreatePi(creators[val], val);
    // }
    // }

    private void maybeInsertPi(SSAAbstractInvokeInstruction call) {
      if (piNodePolicy != null) {
        Pair<Integer, SSAInstruction> pi = piNodePolicy.getPi(call, symbolTable);
        if (pi != null) {
          reuseOrCreatePi(pi.snd, pi.fst);
        }
      }
    }

    private void maybeInsertPi(SSAConditionalBranchInstruction cond) {
      if (piNodePolicy != null) {
        for (Pair<Integer, SSAInstruction> pi : piNodePolicy.getPis(cond, getDef(cond.getUse(0)), getDef(cond.getUse(1)),
            symbolTable)) {
          if (pi != null) {
            reuseOrCreatePi(pi.snd, pi.fst);
          }
        }
      }
    }

    private SSAInstruction getDef(int vn) {
      if (vn < creators.length) {
        return creators[vn];
      } else {
        return null;
      }
    }

    class EdgeVisitor extends com.ibm.wala.shrikeBT.IInstruction.Visitor {

      @Override
      public void visitInvoke(IInvokeInstruction instruction) {
        maybeInsertPi((SSAAbstractInvokeInstruction) getCurrentInstruction());
      }

      @Override
      public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
        maybeInsertPi((SSAConditionalBranchInstruction) getCurrentInstruction());
      }
    }

    @Override
    public com.ibm.wala.shrikeBT.IInstruction[] getInstructions() {
      try {
        return shrikeCFG.getMethod().getInstructions();
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

  public ShrikeIndirectionData getIndirectionData() {
    return ssaIndirections;
  }
  
  /**
   * A logical mapping from &lt;pc, valueNumber&gt; -&gt; local number Note: make sure this class remains static: this persists as part of
   * the IR!!
   */
  private static class SSA2LocalMap implements com.ibm.wala.ssa.IR.SSA2LocalMap {

    private final ShrikeCFG shrikeCFG;

    /**
     * Mapping Integer -&gt; IntPair where p maps to (vn,L) iff we've started a range at pc p where value number vn corresponds to
     * local L
     */
    private final IntPair[] localStoreMap;

    /**
     * For each basic block i and local j, block2LocalState[i][j] gives the contents of local j at the start of block i
     */
    private final int[][] block2LocalState;

    /**
     * @param nInstructions number of instructions in the bytecode for this method
     * @param nBlocks number of basic blocks in the CFG
     */
    SSA2LocalMap(ShrikeCFG shrikeCfg, int nInstructions, int nBlocks) {
      shrikeCFG = shrikeCfg;
      localStoreMap = new IntPair[nInstructions];
      block2LocalState = new int[nBlocks][];
    }

    /**
     * Record the beginning of a new range, starting at the given program counter, in which a particular value number corresponds to
     * a particular local number
     */
    void startRange(int pc, int localNumber, int valueNumber) {
      localStoreMap[pc] = new IntPair(valueNumber, localNumber);
    }

    /**
     * Finish populating the map of local variable information
     */
    private void finishLocalMap(SSABuilder builder) {
      for (BasicBlock bb : shrikeCFG) {
        MachineState S = builder.getIn(bb);
        int number = bb.getNumber();
        block2LocalState[number] = S.getLocals();
      }
    }

    /**
     * @param index - index into IR instruction array
     * @param vn - value number
     */
    @Override
    public String[] getLocalNames(int index, int vn) {
      try {
        if (!shrikeCFG.getMethod().hasLocalVariableTable()) {
          return null;
        } else {
          int[] localNumbers = findLocalsForValueNumber(index, vn);
          if (localNumbers == null) {
            return null;
          } else {
            IBytecodeMethod m = shrikeCFG.getMethod();
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

    public int[] allocateNewLocalsArray(int maxLocals) {
      int[] result = new int[maxLocals];
      for (int i = 0; i < maxLocals; i++) {
        result[i] = OPTIMISTIC ? TOP : BOTTOM;
      }
      return result;
    }
    
    private int[] setLocal(int[] locals, int localNumber, int valueNumber) {
      if (locals == null) {
        locals = allocateNewLocalsArray(localNumber + 1);
      } else if (locals.length <= localNumber) {
        int[] newLocals = allocateNewLocalsArray(2 * Math.max(locals.length, localNumber) + 1);
        System.arraycopy(locals, 0, newLocals, 0, locals.length);
        locals = newLocals;
      }
      
      locals[localNumber] = valueNumber;
      
      return locals;
    }
    /**
     * @param pc a program counter (index into ShrikeBT instruction array)
     * @param vn a value number
     * @return if we know that immediately after the given program counter, v_vn corresponds to some set of locals, then return an
     *         array of the local numbers. else return null.
     */
    private int[] findLocalsForValueNumber(int pc, int vn) {
      if (vn < 0) {
        return null;
      }
      IBasicBlock bb = shrikeCFG.getBlockForInstruction(pc);
      int firstInstruction = bb.getFirstInstructionIndex();
      // walk forward from the first instruction to reconstruct the
      // state of the locals at this pc
      int[] locals = block2LocalState[bb.getNumber()];
      for (int i = firstInstruction; i <= pc; i++) {
        if (localStoreMap[i] != null) {
          IntPair p = localStoreMap[i];
          locals = setLocal(locals, p.getY(), p.getX());
        }
      }
      return locals == null ? null : extractIndices(locals, vn);
    }

    /**
     * @return the indices i s.t. x[i] == y, or null if none found.
     */
    private static int[] extractIndices(int[] x, int y) {
      assert x != null;
      int count = 0;
      for (int element : x) {
        if (element == y) {
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
