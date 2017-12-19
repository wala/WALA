/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Adam Fuchs, Avik Chaudhuri, Steve Suh - Modified from stack to registers
 *******************************************************************************/
package com.ibm.wala.dalvik.ssa;

import java.util.Iterator;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.dalvik.classLoader.DexCFG;
import com.ibm.wala.dalvik.classLoader.DexCFG.BasicBlock;
import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.dalvik.classLoader.Literal;
import com.ibm.wala.dalvik.dex.instructions.ArrayFill;
import com.ibm.wala.dalvik.dex.instructions.ArrayGet;
import com.ibm.wala.dalvik.dex.instructions.ArrayLength;
import com.ibm.wala.dalvik.dex.instructions.ArrayPut;
import com.ibm.wala.dalvik.dex.instructions.BinaryLiteralOperation;
import com.ibm.wala.dalvik.dex.instructions.BinaryOperation;
import com.ibm.wala.dalvik.dex.instructions.Branch;
import com.ibm.wala.dalvik.dex.instructions.CheckCast;
import com.ibm.wala.dalvik.dex.instructions.Constant;
import com.ibm.wala.dalvik.dex.instructions.GetField;
import com.ibm.wala.dalvik.dex.instructions.Goto;
import com.ibm.wala.dalvik.dex.instructions.InstanceOf;
import com.ibm.wala.dalvik.dex.instructions.Instruction;
import com.ibm.wala.dalvik.dex.instructions.Instruction.Visitor;
import com.ibm.wala.dalvik.dex.instructions.Invoke;
import com.ibm.wala.dalvik.dex.instructions.Monitor;
import com.ibm.wala.dalvik.dex.instructions.New;
import com.ibm.wala.dalvik.dex.instructions.NewArray;
import com.ibm.wala.dalvik.dex.instructions.NewArrayFilled;
import com.ibm.wala.dalvik.dex.instructions.PutField;
import com.ibm.wala.dalvik.dex.instructions.Return;
import com.ibm.wala.dalvik.dex.instructions.Switch;
import com.ibm.wala.dalvik.dex.instructions.Throw;
import com.ibm.wala.dalvik.dex.instructions.UnaryOperation;
import com.ibm.wala.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IGetInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.PhiValue;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPiNodePolicy;
import com.ibm.wala.ssa.ShrikeIndirectionData;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.intset.IntPair;

/**
 * This class constructs an SSA {@link IR} from a backing ShrikeBT instruction stream.
 *
 * The basic algorithm here is an abstract interpretation over the Java bytecode to determine types of stack locations and local
 * variables. As a side effect, the flow functions of the abstract interpretation emit instructions, eliminating the stack
 * abstraction and moving to a register-transfer language in SSA form.
 */
public class DexSSABuilder extends AbstractIntRegisterMachine {
    public static DexSSABuilder make(DexIMethod method, SSACFG cfg, DexCFG scfg, SSAInstruction[] instructions,
            SymbolTable symbolTable, boolean buildLocalMap, SSAPiNodePolicy piNodePolicy) throws IllegalArgumentException {
        if (scfg == null) {
            throw new IllegalArgumentException("scfg == null");
        }
        return new DexSSABuilder(method, cfg, scfg, instructions, symbolTable, buildLocalMap, piNodePolicy);

    }

    /**
     * A wrapper around the method being analyzed.
     */
    final private DexIMethod method;

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
//    private final IndirectionData bytecodeIndirections;

    private final ShrikeIndirectionData shrikeIndirections;

    private DexSSABuilder(DexIMethod method, SSACFG cfg, DexCFG scfg, SSAInstruction[] instructions, SymbolTable symbolTable,
            boolean buildLocalMap, SSAPiNodePolicy piNodePolicy) {
        super(scfg);
        localMap = buildLocalMap ? new SSA2LocalMap(scfg, instructions.length, cfg.getNumberOfNodes(), method.getMaxLocals()) : null;
        init(new SymbolTableMeeter(cfg, scfg), new SymbolicPropagator(scfg, instructions,
                cfg, piNodePolicy));
        this.method = method;
        this.symbolTable = symbolTable;
        this.insts = method.getDeclaringClass().getClassLoader().getInstructionFactory();
//        this.bytecodeIndirections = method.getIndirectionData();
        this.shrikeIndirections = new ShrikeIndirectionData(instructions.length);
        assert cfg != null : "Null CFG";
    }

    private class SymbolTableMeeter implements Meeter {

        final SSACFG cfg;

        final DexCFG dexCFG;

        SymbolTableMeeter(SSACFG cfg, DexCFG dexCFG) {
            this.cfg = cfg;
//            this.instructions = instructions;
            this.dexCFG = dexCFG;
        }

//      public int meetStack(int slot, int[] rhs, IBasicBlock<Instruction> bb) {
//
//          assert bb != null : "null basic block";
//
//          if (bb.isExitBlock()) {
//              return TOP;
//          }
//
//          if (allTheSame(rhs)) {
//              for (int i = 0; i < rhs.length; i++) {
//                  if (rhs[i] != TOP) {
//                      return rhs[i];
//                  }
//              }
//              // didn't find anything but TOP
//              return TOP;
//          } else {
//              SSACFG.BasicBlock newBB = cfg.getNode(dexCFG.getNumber(bb));
//              // if we already have a phi for this stack location
//              SSAPhiInstruction phi = newBB.getPhiForStackSlot(slot);
//              int result;
//              if (phi == null) {
//                  // no phi already exists. create one.
//                  result = symbolTable.newPhi(rhs);
//                  PhiValue v = symbolTable.getPhiValue(result);
//                  phi = v.getPhiInstruction();
//                  newBB.addPhiForStackSlot(slot, phi);
//              } else {
//                  // already created a phi. update it to account for the
//                  // new merge.
//                  result = phi.getDef();
//                  phi.setValues(rhs.clone());
//              }
//              return result;
//          }
//      }

        @Override
        public int meetLocal(int n, int[] rhs, DexCFG.BasicBlock bb) {
            if (allTheSame(rhs)) {
                for (int rh : rhs) {
                    if (rh != TOP) {
                        return rh;
                    }
                }
                // didn't find anything but TOP
                return TOP;
            } else {
                SSACFG.BasicBlock newBB = cfg.getNode(dexCFG.getNumber(bb));
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
    }

    @Override
    protected void initializeVariables() {
        MachineState entryState = getEntryState();
        int parameterNumber = 0;
        // added -2 to account for the return and exception register
//      int local = method.getMaxLocals() - method.getNumberOfParameters() - 1 - 2;

        //can't use just getNumberOfParameters because it does not account for Long/Wide variables which take up 2 registers
        //as the parameter
        int local = method.getMaxLocals() - method.getNumberOfParameterRegisters() - 1 - 2;

//      MyLogger.log(LogLevel.DEBUG, "DexSSABuilder - initializeVariables() - local: " + local);


        //initialize the "this" parameter if it needs to be set.
        //the "this" parameter will be symbol number 1 in a virtual method.
//      if (method.getMaxLocals() - method.getNumberOfParameters() - 2 > 0)
//          entryState.setLocal(local, 1);

//      System.out.println("visiting initalizeVartiables()");
//      if (method.isStatic())
//          System.out.println("Static");
//      if (method.isClinit())
//          System.out.println("Clinit");
//      System.out.println("GetNumberOfParameter: " + method.getNumberOfParameterRegisters());
//      System.out.println("Total Registers: " + (method.getMaxLocals()-2));
//      System.out.println("local: " + local);

//      if (local >= 0) {
//          System.out.println("Max Registers: " + (int)(method.getMaxLocals() - 2));
//          System.out.println("Parameters: " + method.getNumberOfParameters());
//          System.out.println("Setting Entry State, local:"+ local + " with 1");
//          entryState.setLocal(local, 1);
//      }

//      System.out.println("Max Registers: " + (int)(method.getMaxLocals() - 2));
//      System.out.println("Parameters: " + method.getNumberOfParameters());

        entryState.allocateLocals();
        for (int i = 0; i < method.getNumberOfParameters(); i++) {
            local++;
            TypeReference t = method.getParameterType(i);
            if (t != null) {
                int symbol = symbolTable.getParameter(parameterNumber++);
                entryState.setLocal(local, symbol);
                if (t.equals(TypeReference.Double) || t.equals(TypeReference.Long)) {
                    local++;
                    entryState.setLocal(local,symbol);
                }
            }
        }
//      MyLogger.log(LogLevel.DEBUG, "DexSSABuilder - initializeVariables() - local: " + local);
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
        // TODO: if there's no stack do we need this?
        //entryState.push(symbolTable.newSymbol());
    }

    /**
     * This class defines the type abstractions for this analysis and the flow function for each instruction in the ShrikeBT IR.
     */
    private class SymbolicPropagator extends BasicRegisterFlowProvider {

        final SSAInstruction[] instructions;

        final DexCFG dexCFG;

        final SSACFG cfg;

        final ClassLoaderReference loader;

        /**
         * creators[i] holds the instruction that defs value number i
         */
        private SSAInstruction[] creators;

//        final SSA2LocalMap localMap;

        final SSAPiNodePolicy piNodePolicy;

        public SymbolicPropagator(DexCFG dexCFG, SSAInstruction[] instructions, SSACFG cfg,
                SSAPiNodePolicy piNodePolicy) {
            super(dexCFG);
            this.piNodePolicy = piNodePolicy;
            this.cfg = cfg;
            this.creators = new SSAInstruction[0];
            this.dexCFG = dexCFG;
            this.instructions = instructions;
            this.loader = dexCFG.getMethod().getDeclaringClass().getClassLoader().getReference();
//            this.localMap = localMap;
            init(this.new NodeVisitor(cfg), this.new EdgeVisitor());
        }

        @Override
        public boolean needsEdgeFlow() {
            return piNodePolicy != null;
        }

        private void emitInstruction(SSAInstruction s) {
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

        private SSAInstruction getCurrentInstruction() {
            return instructions[getCurrentInstructionIndex()];
        }

        /**
         * If we've already created the current instruction, return the value number def'ed by the current instruction. Else, create a
         * new symbol.
         */

        private int reuseOrCreateDef() {
            if (getCurrentInstruction() == null || !getCurrentInstruction().hasDef()) {
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
        class NodeVisitor extends BasicRegisterMachineVisitor {
        	private final SSACFG cfg;
        	
			public NodeVisitor(SSACFG cfg) {
				this.cfg = cfg;
			}

			// TODO: make sure all visit functions are overridden

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayLength(ArrayLengthInstruction)
             */
            @Override
            public void visitArrayLength(ArrayLength instruction) {
                int arrayRef = workingState.getLocal(instruction.source);
                int dest = instruction.destination;
                int length = reuseOrCreateDef();
                setLocal(dest, length);

                emitInstruction(insts.ArrayLengthInstruction(getCurrentInstructionIndex(), length, arrayRef));
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayLoad(IArrayLoadInstruction)
             */
            @Override
            public void visitArrayGet(ArrayGet instruction) {
                int index = workingState.getLocal(instruction.offset);
                int arrayRef = workingState.getLocal(instruction.array);
                int dest = instruction.destination;
//              int index = workingState.pop();
//              int arrayRef = workingState.pop();
                int result = reuseOrCreateDef();
                setLocal(dest, result);
//              workingState.push(result);
                TypeReference t = instruction.getType();
//              if (instruction.isAddressOf()) {
//                  emitInstruction(insts.AddressOfInstruction(result, arrayRef, index, t));
//              } else {
                    emitInstruction(insts.ArrayLoadInstruction(getCurrentInstructionIndex(), result, arrayRef, index, t));
//              }
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayStore(IArrayStoreInstruction)
             */
            @Override
            public void visitArrayPut(ArrayPut instruction) {

                int value = workingState.getLocal(instruction.source);
                int index = workingState.getLocal(instruction.offset);
                int arrayRef = workingState.getLocal(instruction.array);
                TypeReference t = instruction.getType();
                //System.out.println(t.getName().toString());
//              int value = workingState.pop();
//              int index = workingState.pop();
//              int arrayRef = workingState.pop();
//              TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
                emitInstruction(insts.ArrayStoreInstruction(getCurrentInstructionIndex(), arrayRef, index, value, t));
            }

            @Override
            public void visitArrayFill(ArrayFill instruction) {
                Iterator<Number> iae = instruction.getTable().getArrayElements().iterator();
                int i = 0;
                while (iae.hasNext())
                {
                    Number ae = iae.next();                    
                    int index = symbolTable.getConstant(i);
                    int arrayRef = workingState.getLocal(instruction.array);
                    TypeReference t = instruction.getType();
//                  System.out.println(t.getName().toString());
                    int value;
//
//                      System.out.println("Index: " + ae.bufferIndex + ", Width: " + ae.elementWidth + ", Value: " +  byte_buffer.getSomethingDependingonType );


                    //okay to call the getConstant(String) for a char?
                    if (t.equals(TypeReference.Char))
                        value = symbolTable.getConstant((char) ae.intValue());
                    else if (t.equals(TypeReference.Byte))
                        value = symbolTable.getConstant(ae.byteValue());
                    else if (t.equals(TypeReference.Short))
                        value = symbolTable.getConstant(ae.shortValue());
                    else if (t.equals(TypeReference.Int))
                        value = symbolTable.getConstant(ae.intValue());
                    else if (t.equals(TypeReference.Long))
                        value = symbolTable.getConstant(ae.longValue());
                    else if (t.equals(TypeReference.Float))
                        value = symbolTable.getConstant(ae.floatValue());
                    else if (t.equals(TypeReference.Double))
                        value = symbolTable.getConstant(ae.doubleValue());
                    else if (t.equals(TypeReference.Boolean))
                        value = symbolTable.getConstant(ae.intValue() != 0);
                    else
                    {
                        
                        value = 0;
                    }
                    emitInstruction(insts.ArrayStoreInstruction(getCurrentInstructionIndex(), arrayRef, index, value, t));

//                  System.out.println("Index: " + t.bufferIndex + ", Value: " +  t.buffer[t.bufferIndex]);

                    i++;
                }

            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitBinaryOp(IBinaryOpInstruction)
             */
            @Override
            public void visitBinaryOperation(BinaryOperation instruction) {
                int val2 = workingState.getLocal(instruction.oper2);
                int val1 = workingState.getLocal(instruction.oper1);
                int dest = instruction.destination;
//              int val2 = workingState.pop();
//              int val1 = workingState.pop();
                int result = reuseOrCreateDef();
                setLocal(dest, result);
//              workingState.push(result);
//              boolean isFloat = instruction.getType().equals(TYPE_double) || instruction.getType().equals(TYPE_float);
                emitInstruction(insts.BinaryOpInstruction(getCurrentInstructionIndex(), instruction.getOperator(), false, instruction.isUnsigned(), result, val1, val2, !instruction.isFloat()));
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitBinaryOp(IBinaryOpInstruction)
             */
            @Override
            public void visitBinaryLiteral(BinaryLiteralOperation instruction) {
                //int val2 = workingState.getLocal(instruction.oper2);

                Literal lit = instruction.oper2;

                int val2;
                if (lit instanceof Literal.IntLiteral)
                    val2 = symbolTable.getConstant(((Literal.IntLiteral)lit).value);
                else if (lit instanceof Literal.LongLiteral)
                    val2 = symbolTable.getConstant(((Literal.LongLiteral)lit).value);
                else if (lit instanceof Literal.DoubleLiteral)
                    val2 = symbolTable.getConstant(((Literal.DoubleLiteral)lit).value);
                else
                    val2 = symbolTable.getConstant(((Literal.FloatLiteral)lit).value);


                int val1 = workingState.getLocal(instruction.oper1);
                int dest = instruction.destination;
//              int val2 = workingState.pop();
//              int val1 = workingState.pop();
                int result = reuseOrCreateDef();
                setLocal(dest, result);
//              workingState.push(result);
//              boolean isFloat = instruction.getType().equals(TYPE_double) || instruction.getType().equals(TYPE_float);
                try {
                    if (instruction.isSub())
                        emitInstruction(insts.BinaryOpInstruction(getCurrentInstructionIndex(), instruction.getOperator(), false, instruction.isUnsigned(), result, val2, val1, !instruction.isFloat()));
                    else
                        emitInstruction(insts.BinaryOpInstruction(getCurrentInstructionIndex(), instruction.getOperator(), false, instruction.isUnsigned(), result, val1, val2, !instruction.isFloat()));
                } catch (AssertionError e) {
                    System.err.println("When visiting Instuction " + instruction);
                    throw e;
                }
            }

			protected void setLocal(int dest, int result) {
				assert result <= symbolTable.getMaxValueNumber();
				workingState.setLocal(dest, result);
			}

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitCheckCast
             */
            @Override
            public void visitCheckCast(CheckCast instruction) {
                int val = workingState.getLocal(instruction.object);
//              int val = workingState.pop();
                // dex does not use this result, but we need it for the SSA CheckCastInstruction
                int result = reuseOrCreateDef();
                workingState.setLocal(instruction.object, result);
//              workingState.push(result);
//              TypeReference t = instruction.getType();
                emitInstruction(insts.CheckCastInstruction(getCurrentInstructionIndex(), result, val, instruction.type, instruction.isPEI()));
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConditionalBranch(IConditionalBranchInstruction)
             */
            @Override
            public void visitBranch(Branch instruction) {
                if(instruction instanceof Branch.BinaryBranch)
                {
                    Branch.BinaryBranch bbranch = (Branch.BinaryBranch)instruction;
                    int val2 = workingState.getLocal(bbranch.oper2);
                    int val1 = workingState.getLocal(bbranch.oper1);
//                  int val2 = workingState.pop();
//                  int val1 = workingState.pop();

                    TypeReference t = TypeReference.Int;
                    emitInstruction(insts.ConditionalBranchInstruction(getCurrentInstructionIndex(), instruction.getOperator(), t, val1, val2, -1));
                }
                else if(instruction instanceof Branch.UnaryBranch)
                {
                    Branch.UnaryBranch ubranch = (Branch.UnaryBranch)instruction;
                    int val2 = symbolTable.getConstant(0);
                    int val1 = workingState.getLocal(ubranch.oper1);
                    TypeReference t = TypeReference.Int;
                    emitInstruction(insts.ConditionalBranchInstruction(getCurrentInstructionIndex(), instruction.getOperator(), t, val1, val2, -1));
                }
                else
                {
                    throw new IllegalArgumentException("instruction is of an unknown subtype of Branch");
                }
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConstant(ConstantInstruction)
             */
            @Override
            public void visitConstant(Constant instruction) {
                int dest = instruction.destination;
                int symbol = 0;
                if(instruction instanceof Constant.ClassConstant)
                {
                	Constant.ClassConstant constInst = (Constant.ClassConstant)instruction;
                	
                    // TODO: change to a symbol that represents the given IClass
//                  symbol = symbolTable.getConstant(((Constant.ClassConstant)instruction).value);
                    symbol = reuseOrCreateDef();

                    TypeReference typeRef = constInst.value;
					SSALoadMetadataInstruction s = 
                    		insts.LoadMetadataInstruction(getCurrentInstructionIndex(), symbol, TypeReference.JavaLangClass, typeRef);
					emitInstruction(s);
                }
                else if(instruction instanceof Constant.IntConstant)
                {
                    symbol = symbolTable.getConstant(((Constant.IntConstant)instruction).value);
                }
                else if(instruction instanceof Constant.LongConstant)
                {
                    symbol = symbolTable.getConstant(((Constant.LongConstant)instruction).value);
                }
                else if(instruction instanceof Constant.StringConstant)
                {
                    symbol = symbolTable.getConstant(((Constant.StringConstant)instruction).value);
                }
                else
                {
                    Assertions.UNREACHABLE("unexpected constant instruction " + instruction);
                }
                setLocal(dest, symbol);
//              Language l = cfg.getMethod().getDeclaringClass().getClassLoader().getLanguage();
//              TypeReference type = l.getConstantType(instruction.getValue());
//              int symbol = 0;
//              if (l.isNullType(type)) {
//                  symbol = symbolTable.getNullConstant();
//              } else if (l.isIntType(type)) {
//                  Integer value = (Integer) instruction.getValue();
//                  symbol = symbolTable.getConstant(value.intValue());
//              } else if (l.isLongType(type)) {
//                  Long value = (Long) instruction.getValue();
//                  symbol = symbolTable.getConstant(value.longValue());
//              } else if (l.isFloatType(type)) {
//                  Float value = (Float) instruction.getValue();
//                  symbol = symbolTable.getConstant(value.floatValue());
//              } else if (l.isDoubleType(type)) {
//                  Double value = (Double) instruction.getValue();
//                  symbol = symbolTable.getConstant(value.doubleValue());
//              } else if (l.isStringType(type)) {
//                  String value = (String) instruction.getValue();
//                  symbol = symbolTable.getConstant(value);
//              } else if (l.isMetadataType(type)) {
//                  Object rval = l.getMetadataToken(instruction.getValue());
//                  symbol = reuseOrCreateDef();
//                  emitInstruction(insts.LoadMetadataInstruction(symbol, type, rval));
//              } else {
//                  Assertions.UNREACHABLE("unexpected " + type);
//              }
//              workingState.push(symbol);
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConversion(IConversionInstruction)
             */
            // TODO: is this just a unary operation?
//          @Override
//          public void visitConversion(IConversionInstruction instruction) {
//
//              int val = workingState.pop();
//              int result = reuseOrCreateDef();
//              workingState.push(result);
//
//              TypeReference fromType = ShrikeUtil.makeTypeReference(loader, instruction.getFromType());
//              TypeReference toType = ShrikeUtil.makeTypeReference(loader, instruction.getToType());
//
//              emitInstruction(insts.ConversionInstruction(result, val, fromType, toType, instruction.throwsExceptionOnOverflow()));
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitGet(IGetInstruction)
             */
            @Override
            public void visitGetField(GetField instruction) {
                int dest = instruction.destination;
                int result = reuseOrCreateDef();
                FieldReference f = FieldReference.findOrCreate(loader, instruction.clazzName, instruction.fieldName, instruction.fieldType);
                // TODO: what is isAddressOf()?
                // shouldn't matter, java doesn't allow isAddressOf()
                if(instruction instanceof GetField.GetInstanceField)
                {
                    int instance = workingState.getLocal(((GetField.GetInstanceField)instruction).instance);
                    emitInstruction(insts.GetInstruction(getCurrentInstructionIndex(), result, instance, f));
                }
                else if(instruction instanceof GetField.GetStaticField)
                {
                    emitInstruction(insts.GetInstruction(getCurrentInstructionIndex(), result, f));
                }
                else
                {
                    throw new IllegalArgumentException("unknown subclass of GetField: "+instruction);
                }
//              if (instruction.isAddressOf()) {
//                  int ref = instruction.isStatic()? -1: workingState.pop();
//                  emitInstruction(insts.AddressOfInstruction(result, ref, f, f.getFieldType()));
//              } else if (instruction.isStatic()) {
//                  emitInstruction(insts.GetInstruction(result, f));
//              } else {
//                  int ref = workingState.pop();
//                  emitInstruction(insts.GetInstruction(result, ref, f));
//              }
                setLocal(dest, result);
//              workingState.push(result);
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitGoto(GotoInstruction)
             */
            @Override
            public void visitGoto(Goto instruction) {
                emitInstruction(insts.GotoInstruction(getCurrentInstructionIndex(), instruction.destination));
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitInstanceof
             */
            @Override
            public void visitInstanceof(InstanceOf instruction) {
                int ref = workingState.getLocal(instruction.source);
                int dest = instruction.destination;
//              int ref = workingState.pop();
                int result = reuseOrCreateDef();
                setLocal(dest, result);
//              workingState.push(result);
//              TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
                emitInstruction(insts.InstanceofInstruction(getCurrentInstructionIndex(), result, ref, instruction.type));
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitInvoke(IInvokeInstruction)
             */
            @Override
            public void visitInvoke(Invoke instruction) {
                // TODO: can other methods do indirect reads from a dex method?
                //              doIndirectReads(bytecodeIndirections.indirectlyReadLocals(getCurrentInstructionIndex()));
//              int n = instruction.getPoppedCount();
//              int n = instruction.args.length;
//              int[] params = new int[n];
//              for (int i = n - 1; i >= 0; i--) {
//                  params[i] = workingState.pop();
//              }


                Language lang = dexCFG.getMethod().getDeclaringClass().getClassLoader().getLanguage();
                // TODO: check that the signature needed by findOrCreate can use the descriptor
                
                MethodReference m = MethodReference.findOrCreate(lang, loader, instruction.clazzName, instruction.methodName, instruction.descriptor);


                //((DexIClass)dexCFG.getDexMethod().getDeclaringClass()).getSuperclass().get
//              MethodReference m = ((DexIClass)dexCFG.getDexMethod().getDeclaringClass()).loader.lookupClass(TypeName.findOrCreate(instruction.clazzName)).
//              this.myClass.loader.lookupClass(TypeName.findOrCreate(cname)),
//              ((Instruction21c)inst).getRegisterA()));


                
                IInvokeInstruction.IDispatch code = instruction.getInvocationCode();
                CallSiteReference site = CallSiteReference.make(getCurrentProgramCounter(), m, code);
                int exc = reuseOrCreateException();
                setLocal(dexCFG.getDexMethod().getExceptionReg(), exc);


                int n = instruction.args.length;
                for (int i = 0; i < m.getNumberOfParameters(); i++)
                    if (m.getParameterType(i) == TypeReference.Double || m.getParameterType(i) == TypeReference.Long)
                        n--;
                int[] params = new int[n];

                int arg_i = 0;
                //there is no "this" parameter when calling this invoke call
                if (n == m.getNumberOfParameters()) {
                    for (int i = 0; i < n; i++) {
                        params[i] = workingState.getLocal(instruction.args[arg_i]);
                        
                        if (m.getParameterType(i) == TypeReference.Double || m.getParameterType(i) == TypeReference.Long)
                            arg_i++;
                        arg_i++;
                    }
                }
                //there is a "this" parameter in this invoke call
                else if (n == m.getNumberOfParameters()+1) {
                    params[0] = workingState.getLocal(instruction.args[0]);
                    
                    arg_i = 1;
                    for (int i = 0; i < (n-1); i++) {
                        params[i+1] = workingState.getLocal(instruction.args[arg_i]);
                        if (m.getParameterType(i) == TypeReference.Double || m.getParameterType(i) == TypeReference.Long)
                            arg_i++;
                        arg_i++;
                    }
                }
                //this should not happen
                else
                    throw new UnsupportedOperationException("visitInvoke DexSSABuilder, error");



                if(m.getReturnType().equals(TypeReference.Void))
                {
                    SSAInstruction inst = insts.InvokeInstruction(getCurrentInstructionIndex(), params, exc, site, null);
                    //System.out.println("Emitting(1) InvokeInstruction: "+inst);
                    emitInstruction(inst);
                } else {
                    int result = reuseOrCreateDef();
                    assert result != -1;
                    // TODO: check that this return register is correct
                    //I think it be registerCount() or registerCount()+1
//                  int dest = dexCFG.getDexMethod().regBank.getReturnReg().regID;
                    int dest = dexCFG.getDexMethod().getReturnReg();

                    setLocal(dest, result);
                    SSAInstruction inst = insts.InvokeInstruction(getCurrentInstructionIndex(), result, params, exc, site, null);
                    //System.out.println("Emitting(2) InvokeInstruction: "+inst);
                    emitInstruction(inst);
                }
//              if (instruction.getPushedWordSize() > 0) {
//                  int result = reuseOrCreateDef();
//                  workingState.push(result);
//                  emitInstruction(insts.InvokeInstruction(result, params, exc, site));
//              } else {
//                  emitInstruction(insts.InvokeInstruction(params, exc, site));
//              }
                // TODO: can other methods do indirect writes to a dex method?
//              doIndirectWrites(bytecodeIndirections.indirectlyWrittenLocals(getCurrentInstructionIndex()), -1);
            }

            // Dex doesn't have local load or store
//          @Override
//          public void visitLocalLoad(ILoadInstruction instruction) {
//              if (instruction.isAddressOf()) {
//                  int result = reuseOrCreateDef();
//
//                  int t = workingState.getLocal(instruction.getVarIndex());
//                  if (t == -1) {
//                      doIndirectWrites(new int[]{instruction.getVarIndex()}, -1);
//                      t = workingState.getLocal(instruction.getVarIndex());
//                  }
//
//                  TypeReference type = ShrikeUtil.makeTypeReference(loader, instruction.getType());
//                  emitInstruction(insts.AddressOfInstruction(result, t, type));
//                  workingState.push(result);
//              } else {
//                  super.visitLocalLoad(instruction);
//              }
//          }
//
//          /*
//           * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitLocalStore(com.ibm.wala.shrikeBT.StoreInstruction)
//           */
//          @Override
//          public void visitLocalStore(IStoreInstruction instruction) {
//              if (localMap != null) {
//                  localMap.startRange(getCurrentInstructionIndex(), instruction.getVarIndex(), workingState.peek());
//              }
//              super.visitLocalStore(instruction);
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitMonitor(MonitorInstruction)
             */
            @Override
            public void visitMonitor(Monitor instruction) {
                int ref = workingState.getLocal(instruction.object);
//              int ref = workingState.pop();
                emitInstruction(insts.MonitorInstruction(getCurrentInstructionIndex(), ref, instruction.enter));
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitNew(NewInstruction)
             */
            @Override
            public void visitNew(New instruction) {
                int dest = instruction.destination;
                int result = reuseOrCreateDef();
//              TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
//              NewSiteReference ref = NewSiteReference.make(getCurrentProgramCounter(), t);

//              if (t.isArrayType()) {
//                  int[] sizes = new int[instruction.getArrayBoundsCount()];
//                  for (int i = 0; i < instruction.getArrayBoundsCount(); i++) {
//                      sizes[instruction.getArrayBoundsCount() - 1 - i] = workingState.pop();
//                  }
//                  emitInstruction(insts.NewInstruction(result, ref, sizes));
//              } else {
                    emitInstruction(insts.NewInstruction(getCurrentInstructionIndex(), result, instruction.newSiteRef));
//                  popN(instruction);
//              }
                setLocal(dest, result);
//              workingState.push(result);
            }

            @Override
            public void visitNewArray(NewArray instruction)
            {
                int dest = instruction.destination;
                int result = reuseOrCreateDef();
                int[] sizes = new int[instruction.sizes.length];
                for(int i = 0; i < instruction.sizes.length; i++)
                {
                    sizes[i] = workingState.getLocal(instruction.sizes[i]);
                }
                emitInstruction(insts.NewInstruction(getCurrentInstructionIndex(), result, instruction.newSiteRef, sizes));
                setLocal(dest, result);
            }

            @Override
            public void visitNewArrayFilled(NewArrayFilled instruction)
            {
                int dest = instruction.destination;
                int result = reuseOrCreateDef();
                int[] sizes = new int[instruction.sizes.length];
                for(int i = 0; i < instruction.sizes.length; i++)
                {
                    sizes[i] = symbolTable.getConstant(instruction.sizes[i]);
                }
                emitInstruction(insts.NewInstruction(getCurrentInstructionIndex(), result, instruction.newSiteRef, sizes));
                setLocal(dest, result);

                /*
                 * we need to emit these instructions somehow, but for now this clobbers the emitInstruction mechanism
                 * 
                for (int i = 0; i < instruction.args.length; i++)
                {
                    int value = workingState.getLocal(instruction.args[i]);
                    int index = symbolTable.getConstant(i);
                    int arrayRef = result;
                    TypeReference t = instruction.myType;
                    emitInstruction(insts.ArrayStoreInstruction(getCurrentInstructionIndex(), arrayRef, index, value, t));
                }
                */
            }


            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitGet(IGetInstruction)
             */
            @Override
            public void visitPutField(PutField instruction) {
                int value = workingState.getLocal(instruction.source);
                FieldReference f = FieldReference.findOrCreate(loader, instruction.clazzName, instruction.fieldName, instruction.fieldType);
//              int value = workingState.pop();
                if (instruction instanceof PutField.PutStaticField) {
//              if (instruction.isStatic()) {
                    emitInstruction(insts.PutInstruction(getCurrentInstructionIndex(), value, f));
                } else if (instruction instanceof PutField.PutInstanceField) {
//              } else {
                    int ref = workingState.getLocal(((PutField.PutInstanceField)instruction).instance);
//                  int ref = workingState.pop();
                    emitInstruction(insts.PutInstruction(getCurrentInstructionIndex(), ref, value, f));
                } else {
                    throw new IllegalArgumentException("Unknown subclass of PutField: "+instruction);
                }
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitReturn(ReturnInstruction)
             */
            @Override
            public void visitReturn(Return instruction) {
                if(instruction instanceof Return.ReturnDouble)
                {
                    // TODO: figure out how to return a double
                    Return.ReturnDouble retD = (Return.ReturnDouble)instruction;
                    int result = workingState.getLocal(retD.source1);
//                  boolean isPrimitive = symbolTable.isLongConstant(result) || symbolTable.isDoubleConstant(result);
                    boolean isPrimitive = true;
                    emitInstruction(insts.ReturnInstruction(getCurrentInstructionIndex(), result, isPrimitive));
//                  throw new UnsupportedOperationException("can't yet support returning doubles");
                } else if (instruction instanceof Return.ReturnSingle)
                {
                    Return.ReturnSingle retS = (Return.ReturnSingle)instruction;
                    int result = workingState.getLocal(retS.source);
                    // TODO: figure out if this is primitive or not
                    //boolean isPrimitive = false;
                    boolean isPrimitive = retS.isPrimitive();
                    emitInstruction(insts.ReturnInstruction(getCurrentInstructionIndex(), result, isPrimitive));
                } else if (instruction instanceof Return.ReturnVoid)
                {
                    emitInstruction(insts.ReturnInstruction(getCurrentInstructionIndex()));
                }
//              if (instruction.getPoppedCount() == 1) {
//                  int result = workingState.pop();
//                  TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
//                  emitInstruction(insts.ReturnInstruction(result, t.isPrimitiveType()));
//              } else {
//                  emitInstruction(insts.ReturnInstruction());
//              }
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitShift(IShiftInstruction)
             */
            // TODO: this is just a binary operation
//          @Override
//          public void visitShift(IShiftInstruction instruction) {
//              int val2 = workingState.pop();
//              int val1 = workingState.pop();
//              int result = reuseOrCreateDef();
//              workingState.push(result);
//              emitInstruction(insts.BinaryOpInstruction(instruction.getOperator(), false, instruction.isUnsigned(), result, val1, val2,
//                      true));
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitSwitch(SwitchInstruction)
             */
            @Override
            public void visitSwitch(Switch instruction) {
                int val = workingState.getLocal(instruction.regA);
//              int val = workingState.pop();
                // TODO: figure out if the switch offset should refer to a pc offset or an instruction id or what
                emitInstruction(insts.SwitchInstruction(getCurrentInstructionIndex(), val, instruction.getDefaultLabel(), instruction.getCasesAndLabels()));
            }

            private Dominators<ISSABasicBlock> dom = null;

            @SuppressWarnings("unused")
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
            public void visitThrow(Throw instruction) {
                int throwable = workingState.getLocal(instruction.throwable);
                assert symbolTable.getMaxValueNumber() >= throwable;
                emitInstruction(insts.ThrowInstruction(getCurrentInstructionIndex(), throwable));
//              if (instruction.isRethrow()) {
//                  workingState.clearStack();
//                  emitInstruction(insts.ThrowInstruction(findRethrowException()));
//              } else {
//                  int exception = workingState.pop();
//                  workingState.clearStack();
//                  workingState.push(exception);
//                  emitInstruction(insts.ThrowInstruction(exception));
//              }
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitUnaryOp(IUnaryOpInstruction)
             */
            @Override
            public void visitUnaryOperation(UnaryOperation instruction) {
            	
            	if (instruction.op == UnaryOperation.OpID.MOVE_EXCEPTION) {
            		
            		int idx = getCurrentInstructionIndex();
            		int bbidx = dexCFG.getBlockForInstruction(idx).getNumber();
            		ExceptionHandlerBasicBlock newBB = (ExceptionHandlerBasicBlock) cfg.getBasicBlock(bbidx);

            		SSAGetCaughtExceptionInstruction s = newBB.getCatchInstruction();
                    int exceptionValue;
                     if (s == null) {
                    	exceptionValue = symbolTable.newSymbol();
                        s = insts.GetCaughtExceptionInstruction(newBB.getLastInstructionIndex(), bbidx, exceptionValue);
                        newBB.setCatchInstruction(s);
                    } else {
                        exceptionValue = s.getException();
                    }

                 	setLocal(instruction.destination, exceptionValue);
                	return;
                }
            	
                //System.out.println("Instruction: " + getCurrentInstructionIndex());
                int val = workingState.getLocal(instruction.source);
//              int val = workingState.pop();
//              workingState.push(result);
                if(instruction.isConversion())
                {
                    TypeReference fromType, toType;
                    boolean overflows = false;
                    // TODO: figure out if any of these can overflow
                    switch(instruction.op)
                    {
                    case DOUBLETOLONG:
                        fromType = TypeReference.Double;
                        toType = TypeReference.Long;
                        break;
                    case DOUBLETOFLOAT:
                        fromType = TypeReference.Double;
                        toType = TypeReference.Float;
                        break;
                    case INTTOBYTE:
                        fromType = TypeReference.Int;
                        toType = TypeReference.Byte;
                        break;
                    case INTTOCHAR:
                        fromType = TypeReference.Int;
                        toType = TypeReference.Char;
                        break;
                    case INTTOSHORT:
                        fromType = TypeReference.Int;
                        toType = TypeReference.Short;
                        break;
                    case DOUBLETOINT:
                        fromType = TypeReference.Double;
                        toType = TypeReference.Int;
                        break;
                    case FLOATTODOUBLE:
                        fromType = TypeReference.Float;
                        toType = TypeReference.Double;
                        break;
                    case FLOATTOLONG:
                        fromType = TypeReference.Float;
                        toType = TypeReference.Long;
                        break;
                    case FLOATTOINT:
                        fromType = TypeReference.Float;
                        toType = TypeReference.Int;
                        break;
                    case LONGTODOUBLE:
                        fromType = TypeReference.Long;
                        toType = TypeReference.Double;
                        break;
                    case LONGTOFLOAT:
                        fromType = TypeReference.Long;
                        toType = TypeReference.Float;
                        break;
                    case LONGTOINT:
                        fromType = TypeReference.Long;
                        toType = TypeReference.Int;
                        break;
                    case INTTODOUBLE:
                        fromType = TypeReference.Int;
                        toType = TypeReference.Double;
                        break;
                    case INTTOFLOAT:
                        fromType = TypeReference.Int;
                        toType = TypeReference.Float;
                        break;
                    case INTTOLONG:
                        fromType = TypeReference.Int;
                        toType = TypeReference.Long;
                        break;
                    default:
                        throw new IllegalArgumentException("unknown conversion type "+instruction.op+" in unary instruction: "+instruction);
                    }
                    int dest = instruction.destination;
                    int result = reuseOrCreateDef();
                    setLocal(dest, result);
                    emitInstruction(insts.ConversionInstruction(getCurrentInstructionIndex(), result, val, fromType, toType, overflows));
                }        
                else
                {
                	if (instruction.op == UnaryOperation.OpID.MOVE) {
                        setLocal(instruction.destination, workingState.getLocal(instruction.source));
                    }
                    else if (instruction.op == UnaryOperation.OpID.MOVE_WIDE) {
                        setLocal(instruction.destination, workingState.getLocal(instruction.source));
                        if (instruction.source == dexCFG.getDexMethod().getReturnReg())
                            setLocal(instruction.destination+1, workingState.getLocal(instruction.source));
                        else
                            setLocal(instruction.destination+1, workingState.getLocal(instruction.source+1));
                    } else {
                        int dest = instruction.destination;
                        int result = reuseOrCreateDef();
                        setLocal(dest, result);
                    	emitInstruction(insts.UnaryOpInstruction(getCurrentInstructionIndex(), instruction.getOperator(), result, val));
                    }
                }
            }

            // dex doesn't have indirect reads
//          private void doIndirectReads(int[] locals) {
//              for(int i = 0; i < locals.length; i++) {
//                  ssaIndirections.setUse(getCurrentInstructionIndex(), new ShrikeLocalName(locals[i]), workingState.getLocal(locals[i]));
//              }
//          }

            // dex doesn't have load or store, even with indirection
//          @Override
//          public void visitLoadIndirect(ILoadIndirectInstruction instruction) {
//              int addressVal = workingState.pop();
//              int result = reuseOrCreateDef();
//              doIndirectReads(bytecodeIndirections.indirectlyReadLocals(getCurrentInstructionIndex()));
//              TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getPushedType(null));
//              emitInstruction(insts.LoadIndirectInstruction(result, t, addressVal));
//              workingState.push(result);
//          }
//
//          private void doIndirectWrites(int[] locals, int rval) {
//              for(int i = 0; i < locals.length; i++) {
//                  ShrikeLocalName name = new ShrikeLocalName(locals[i]);
//                  int idx = getCurrentInstructionIndex();
//                  if (ssaIndirections.getDef(idx, name) == -1) {
//                      ssaIndirections.setDef(idx, name, rval==-1? symbolTable.newSymbol(): rval);
//                  }
//                  workingState.setLocal(locals[i], ssaIndirections.getDef(idx, name));
//              }
//          }
//
//          @Override
//          public void visitStoreIndirect(IStoreIndirectInstruction instruction) {
//              int val = workingState.pop();
//              int addressVal = workingState.pop();
//              doIndirectWrites(bytecodeIndirections.indirectlyWrittenLocals(getCurrentInstructionIndex()), val);
//              TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
//              emitInstruction(insts.StoreIndirectInstruction(addressVal, val, t));
//          }

        }

        /**
         * @param piCause
         * @param ref
         */
        private void reuseOrCreatePi(SSAInstruction piCause, int ref) {
            int n = getCurrentInstructionIndex();
            SSACFG.BasicBlock bb = cfg.getBlockForInstruction(n);

            BasicBlock path = getCurrentSuccessor();
            int outNum = dexCFG.getNumber(path);

            SSAPiInstruction pi = bb.getPiForRefAndPath(ref, path);
            if (pi == null) {
                pi = insts.PiInstruction(getCurrentInstructionIndex(), symbolTable.newSymbol(), ref, bb.getNumber(), outNum, piCause);
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
                Pair<Integer, SSAInstruction> pi = piNodePolicy.getPi(cond, getDef(cond.getUse(0)), getDef(cond.getUse(1)), symbolTable);
                if (pi != null) {
                    reuseOrCreatePi(pi.snd, pi.fst);
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

        class EdgeVisitor extends Visitor {

            @Override
            public void visitInvoke(Invoke instruction) {
                maybeInsertPi((SSAAbstractInvokeInstruction) getCurrentInstruction());
            }

            @Override
            public void visitBranch(Branch instruction) {
                maybeInsertPi((SSAConditionalBranchInstruction) getCurrentInstruction());
            }
        }

        @Override
        public Instruction[] getInstructions() {
            return dexCFG.getDexMethod().getDexInstructions();
        }
    }

    /**
     * Build the IR
     */
    public void build() {
        try {
           solve();
            if (localMap != null) {
                localMap.finishLocalMap(this);
            }
        } catch (AssertionError e) {
            System.err.println("When handling method " + method.getReference());
            e.printStackTrace();
            //throw e;
        }
    }

    public SSA2LocalMap getLocalMap() {
        return localMap;
    }

    public ShrikeIndirectionData getIndirectionData() {
        return shrikeIndirections;
    }

    /**
     * A logical mapping from &lt;pc, valueNumber&gt; -&gt; local number Note: make sure this class remains static: this persists as part of
     * the IR!!
     */
    private static class SSA2LocalMap implements com.ibm.wala.ssa.IR.SSA2LocalMap {

        private final DexCFG dexCFG;

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
         * maximum number of locals used at any program point
         */
        private final int maxLocals;

        /**
         * @param nInstructions number of instructions in the bytecode for this method
         * @param nBlocks number of basic blocks in the CFG
         */
        SSA2LocalMap(DexCFG dexCfg, int nInstructions, int nBlocks, int maxLocals) {
            dexCFG = dexCfg;
            localStoreMap = new IntPair[nInstructions];
            block2LocalState = new int[nBlocks][];
            this.maxLocals = maxLocals;
        }

        /**
         * Record the beginning of a new range, starting at the given program counter, in which a particular value number corresponds to
         * a particular local number
         */
        @SuppressWarnings("unused")
		void startRange(int pc, int localNumber, int valueNumber) {
            int max = ((DexIMethod)dexCFG.getMethod()).getMaxLocals();
            if (localNumber >= max) {
                assert false : "invalid local " + localNumber + ">" + max;
            }

            localStoreMap[pc] = new IntPair(valueNumber, localNumber);
        }

        /**
         * Finish populating the map of local variable information
         */
        private void finishLocalMap(DexSSABuilder builder) {
            for (BasicBlock bb : dexCFG) {
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
                if (!dexCFG.getMethod().hasLocalVariableTable()) {
                    return null;
                } else {
                    int[] localNumbers = findLocalsForValueNumber(index, vn);
                    if (localNumbers == null) {
                        return null;
                    } else {
                        DexIMethod m = dexCFG.getDexMethod();
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
         * @param pc a program counter (index into ShrikeBT instruction array)
         * @param vn a value number
         * @return if we know that immediately after the given program counter, v_vn corresponds to some set of locals, then return an
         *         array of the local numbers. else return null.
         */
        private int[] findLocalsForValueNumber(int pc, int vn) {

            IBasicBlock<Instruction> bb = dexCFG.getBlockForInstruction(pc);
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
        private static int[] extractIndices(int[] x, int y) {
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
