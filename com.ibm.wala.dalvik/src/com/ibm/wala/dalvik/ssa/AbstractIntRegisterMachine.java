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

import com.ibm.wala.dalvik.classLoader.DexCFG;
import com.ibm.wala.dalvik.classLoader.DexCFG.BasicBlock;
import com.ibm.wala.dalvik.classLoader.DexConstants;
import com.ibm.wala.dalvik.dex.instructions.ArrayGet;
import com.ibm.wala.dalvik.dex.instructions.ArrayLength;
import com.ibm.wala.dalvik.dex.instructions.ArrayPut;
import com.ibm.wala.dalvik.dex.instructions.BinaryOperation;
import com.ibm.wala.dalvik.dex.instructions.Branch;
import com.ibm.wala.dalvik.dex.instructions.Constant;
import com.ibm.wala.dalvik.dex.instructions.GetField;
import com.ibm.wala.dalvik.dex.instructions.InstanceOf;
import com.ibm.wala.dalvik.dex.instructions.Instruction;
import com.ibm.wala.dalvik.dex.instructions.Instruction.Visitor;
import com.ibm.wala.dalvik.dex.instructions.Invoke;
import com.ibm.wala.dalvik.dex.instructions.Monitor;
import com.ibm.wala.dalvik.dex.instructions.New;
import com.ibm.wala.dalvik.dex.instructions.PutField;
import com.ibm.wala.dalvik.dex.instructions.Switch;
import com.ibm.wala.dalvik.dex.instructions.Throw;
import com.ibm.wala.dalvik.dex.instructions.UnaryOperation;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BasicFramework;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.AbstractVariable;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IGetInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.IPutInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.INodeWithNumber;

/**
 * Skeleton of functionality to propagate information through the Java bytecode stack machine using ShrikeBT.
 * <p>
 * This class computes properties the Java operand stack and of the local variables at the beginning of each basic block.
 * <p>
 * In this implementation, each dataflow variable value is an integer, and the "meeter" object provides the meets
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractIntRegisterMachine implements FixedPointConstants {
    private static final boolean DEBUG = false;

    public static final int TOP = -1;

    public static final int BOTTOM = -2;

    public static final int UNANALYZED = -3;

    public static final int IGNORE = -4;

    /**
     * The solver
     */
    private DataflowSolver solver;

    /**
     * The control flow graph to analyze
     */
    final private DexCFG cfg;

    /**
     * The max height of the stack.
     */
    //  final private int maxStackHeight;

    /**
     * the max number of locals in play
     */
    final protected int maxLocals;

    /**
     * Should uninitialized variables be considered TOP (optimistic) or BOTTOM (pessimistic);
     */
    final public static boolean OPTIMISTIC = true;

    protected AbstractIntRegisterMachine(final DexCFG G) {
        if (G == null) {
            throw new IllegalArgumentException("G is null");
        }
        //    maxStackHeight = G.getMaxStackHeight();
        maxLocals = G.getDexMethod().getMaxLocals();
        this.cfg = G;
    }

    protected void init(Meeter meeter, final FlowProvider flow) {
        final MeetOperator meet = new MeetOperator(meeter);
        ITransferFunctionProvider<BasicBlock, MachineState> xferFunctions = new ITransferFunctionProvider<BasicBlock, MachineState>() {
            @Override
            public boolean hasNodeTransferFunctions() {
                return flow.needsNodeFlow();
            }

            @Override
            public boolean hasEdgeTransferFunctions() {
                return flow.needsEdgeFlow();
            }

            @Override
            public UnaryOperator<MachineState> getNodeTransferFunction(final BasicBlock node) {
                return new UnaryOperator<MachineState>() {
                    @Override
                    public byte evaluate(MachineState lhs, MachineState rhs) {

                        MachineState exit = lhs;
                        MachineState entry = rhs;

                        MachineState newExit = flow.flow(entry, node);
                        if (newExit.stateEquals(exit)) {
                            return NOT_CHANGED;
                        } else {
                            exit.copyState(newExit);
                            return CHANGED;
                        }
                    }

                    @Override
                    public String toString() {
                        return "NODE-FLOW";
                    }

                    @Override
                    public int hashCode() {
                        return 9973 * node.hashCode();
                    }

                    @Override
                    public boolean equals(Object o) {
                        return this == o;
                    }
                };
            }

            @Override
            public UnaryOperator<MachineState> getEdgeTransferFunction(final BasicBlock from, final BasicBlock to) {
                return new UnaryOperator<MachineState>() {
                    @Override
                    public byte evaluate(MachineState lhs, MachineState rhs) {

                        MachineState exit = lhs;
                        MachineState entry = rhs;

                        MachineState newExit = flow.flow(entry, from, to);
                        if (newExit.stateEquals(exit)) {
                            return NOT_CHANGED;
                        } else {
                            exit.copyState(newExit);
                            return CHANGED;
                        }
                    }

                    @Override
                    public String toString() {
                        return "EDGE-FLOW";
                    }

                    @Override
                    public int hashCode() {
                        return 9973 * (from.hashCode() ^ to.hashCode());
                    }

                    @Override
                    public boolean equals(Object o) {
                        return this == o;
                    }
                };
            }

            @Override
            public AbstractMeetOperator<MachineState> getMeetOperator() {
                return meet;
            }
        };

        IKilldallFramework<BasicBlock, MachineState> problem = new BasicFramework<>(cfg, xferFunctions);
        solver = new DataflowSolver<BasicBlock, MachineState>(problem) {
            private MachineState entry;

            @Override
            protected MachineState makeNodeVariable(BasicBlock n, boolean IN) {
                assert n != null;
                MachineState result = new MachineState(n);
                if (IN && n.equals(cfg.entry())) {
                    entry = result;
                }
                return result;
            }

            @Override
            protected MachineState makeEdgeVariable(BasicBlock from, BasicBlock to) {
                assert from != null;
                assert to != null;
                MachineState result = new MachineState(from);

                return result;
            }

            @Override
            protected void initializeWorkList() {
                super.buildEquations(false, false);
                /*
                 * Add only the entry variable to the work list.
                 */
                 for (INodeWithNumber inn : Iterator2Iterable.make(getFixedPointSystem().getStatementsThatUse(entry))) {
                     AbstractStatement s = (AbstractStatement) inn;
                     addToWorkList(s);
                 }
            }

            @Override
            protected void initializeVariables() {
                super.initializeVariables();
                AbstractIntRegisterMachine.this.initializeVariables();
            }

            @Override
            protected MachineState[] makeStmtRHS(int size) {
                return new MachineState[size];
            }
        };

    }

    public boolean solve() {
        try {
            return solver.solve(null);
        } catch (CancelException e) {
            throw new CancelRuntimeException(e);
        }
    }

    /**
     * Convenience method ... a little ugly .. perhaps delete later.
     */
    protected void initializeVariables() {
    }

    public MachineState getEntryState() {
        return (MachineState) solver.getIn(cfg.entry());
    }

    /**
     * @return the state at the entry to a given block
     */
    public MachineState getIn(BasicBlock bb) {
        return (MachineState) solver.getIn(bb);
    }

    private class MeetOperator extends AbstractMeetOperator<MachineState> {

        private final Meeter meeter;

        MeetOperator(Meeter meeter) {
            this.meeter = meeter;
        }

        @Override
        public boolean isUnaryNoOp() {
            return false;
        }

        @Override
        public byte evaluate(MachineState lhs, MachineState[] rhs) {
            BasicBlock bb = lhs.getBasicBlock();
            // TODO
//          Exception e = new Exception("evaluating a MeetOperator");
//          e.printStackTrace();
//          return NOT_CHANGED;
            
             if (!bb.isCatchBlock()) {
                return meet(lhs, rhs, bb, meeter) ? CHANGED : NOT_CHANGED;
            } else {            	
                return meetForCatchBlock(lhs, rhs, bb, meeter) ? CHANGED : NOT_CHANGED;
            }
        }

        @Override
        public int hashCode() {
            return 72223 * meeter.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MeetOperator) {
                MeetOperator other = (MeetOperator) o;
                return meeter.equals(other.meeter);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "MEETER";
        }
        
        
    }

    /**
     * A Meeter object provides the dataflow logic needed to meet the abstract machine state for a dataflow meet.
     */
    protected interface Meeter {

        /**
         * Return the integer that represents the meet of a particular local at the entry to a basic block.
         *
         * @param n The number of the local
         * @param rhs The values to meet
         * @param bb The basic block at whose entry this meet occurs
         * @return The value of local n after the meet.
         */
        int meetLocal(int n, int[] rhs, BasicBlock bb);
    }

    /**
     * Evaluate a meet of machine states.
     *
     * TODO: add some efficiency shortcuts. TODO: clean up and refactor.
     *
     * @param bb the basic block at whose entry the meet occurs
     * @return true if the lhs value changes. false otherwise.
     */
    private static boolean meet(IVariable lhs, IVariable[] rhs, BasicBlock bb, Meeter meeter) {

//      boolean changed = meetStacks(lhs, rhs, bb, meeter);

        return meetLocals(lhs, rhs, bb, meeter);
//      return changed;
    }

    /**
     * Evaluate a meet of machine states at a catch block.
     *
     * TODO: add some efficiency shortcuts. TODO: clean up and refactor.
     *
     * @param bb the basic block at whose entry the meet occurs
     * @return true if the lhs value changes. false otherwise.
     */
    private static boolean meetForCatchBlock(IVariable lhs, IVariable[] rhs, BasicBlock bb, Meeter meeter) {

        boolean changed = meetLocals(lhs, rhs, bb, meeter);

//      int meet = meeter.meetStackAtCatchBlock(bb);
//      boolean changed = meetLocals(lhs, rhs, bb, meeter);
        return changed;
    }

    /**
     * Evaluate a meet of the stacks of machine states at the entry of a catch block.
     *
     * TODO: add some efficiency shortcuts. TODO: clean up and refactor.
     *
     * @param bb the basic block at whose entry the meet occurs
     * @return true if the lhs value changes. false otherwise.
     */
//  private boolean meetStacksAtCatchBlock(IVariable lhs, IBasicBlock<Instruction> bb, Meeter meeter) {
//      boolean changed = false;
//      MachineState L = (MachineState) lhs;
//
//      // evaluate the meet of the stack of height 1, which holds the exception
//      // object.
//
//      // allocate lhs.stack if it's
//      // not already allocated.
//      if (L.stack == null) {
//          L.allocateStack();
//          L.stackHeight = 1;
//      }
//
//      int meet = meeter.meetStackAtCatchBlock(bb);
//      if (L.stack[0] == TOP) {
//          if (meet != TOP) {
//              changed = true;
//              L.stack[0] = meet;
//          }
//      } else if (meet != L.stack[0]) {
//          changed = true;
//          L.stack[0] = meet;
//      }
//      return changed;
//  }

    /**
     * Evaluate a meet of the stacks of machine states at the entry of a basic block.
     *
     * TODO: add some efficiency shortcuts. TODO: clean up and refactor.
     *
     * @param bb the basic block at whose entry the meet occurs
     * @return true if the lhs value changes. false otherwise.
     */
//  private boolean meetStacks(IVariable lhs, IVariable[] rhs, IBasicBlock<Instruction> bb, Meeter meeter) {
//      boolean changed = false;
//      MachineState L = (MachineState) lhs;
//
//      // evaluate the element-wise meet over the stacks
//
//      // first ... how high are the stacks?
//      int height = computeMeetStackHeight(rhs);
//
//      // if there's any stack height to meet, allocate lhs.stack if it's
//      // not already allocated.
//      if (height > -1 && L.stack == null) {
//          L.allocateStack();
//          L.stackHeight = height;
//          changed = true;
//      }
//
//      // now do the element-wise meet.
//      for (int i = 0; i < height; i++) {
//          int[] R = new int[rhs.length];
//          for (int j = 0; j < R.length; j++) {
//              MachineState m = (MachineState) rhs[j];
//              if (m.stack == null) {
//                  R[j] = TOP;
//              } else {
//                  R[j] = m.stack[i];
//                  if (R[j] == 0) {
//                      R[j] = TOP;
//                  }
//              }
//          }
//          int meet = meeter.meetStack(i, R, bb);
//          if (L.stack[i] == TOP) {
//              if (meet != TOP) {
//                  changed = true;
//                  L.stack[i] = meet;
//              }
//          } else if (meet != L.stack[i]) {
//              changed = true;
//              L.stack[i] = meet;
//          }
//      }
//      return changed;
//  }

    /**
     * Evaluate a meet of locals of machine states at the entry to a basic block.
     *
     * TODO: add some efficiency shortcuts. TODO: clean up and refactor.
     *
     * @param bb the basic block at whose entry the meet occurs
     * @return true if the lhs value changes. false otherwise.
     */
    private static boolean meetLocals(IVariable lhs, IVariable[] rhs, BasicBlock bb, Meeter meeter) {

        boolean changed = false;
        MachineState L = (MachineState) lhs;
        // need we allocate lhs.locals?
        int nLocals = computeMeetNLocals(rhs);
        if (nLocals > -1 && L.locals == null) {
            L.allocateLocals();
            changed = true;
        }

        // evaluate the element-wise meet over the locals.
        for (int i = 0; i < nLocals; i++) {
            int[] R = new int[rhs.length];
            for (int j = 0; j < rhs.length; j++) {
                R[j] = ((MachineState) rhs[j]).getLocal(i);
            }
            int meet = meeter.meetLocal(i, R, bb);
            if (L.locals[i] == TOP) {
                if (meet != TOP) {
                    changed = true;
                    L.locals[i] = meet;
                }
            } else if (meet != L.locals[i]) {
                changed = true;
                L.locals[i] = meet;
            }
        }
        return changed;
    }

    /**
     * @return the number of locals to meet. Return -1 if there is no local meet necessary.
     * @param operands The operands for this operator. operands[0] is the left-hand side.
     */
    private static int computeMeetNLocals(IVariable[] operands) {
        MachineState lhs = (MachineState) operands[0];
        int nLocals = -1;
        if (lhs.locals != null) {
            nLocals = lhs.locals.length;
        } else {
            for (int i = 1; i < operands.length; i++) {
                MachineState rhs = (MachineState) operands[i];
                if (rhs.locals != null) {
                    nLocals = rhs.locals.length;
                    break;
                }
            }
        }
        return nLocals;
    }

    /**
     * @return the height of stacks that are being meeted. Return -1 if there is no stack meet necessary.
     * @param operands The operands for this operator. operands[0] is the left-hand side.
     */
    @SuppressWarnings("unused")
	private static int computeMeetStackHeight(IVariable[] operands) {
        MachineState lhs = (MachineState) operands[0];
        int height = -1;
        if (lhs.stack != null) {
            height = lhs.stackHeight;
        } else {
            for (int i = 1; i < operands.length; i++) {
                MachineState rhs = (MachineState) operands[i];
                if (rhs.stack != null) {
                    height = rhs.stackHeight;
                    break;
                }
            }
        }
        return height;
    }

    /**
     * Representation of the state of the JVM stack machine at some program point.
     */
    public class MachineState extends AbstractVariable<MachineState> {
        private int[] stack;

        private int[] locals;

        // NOTE: stackHeight == -1 is a special code meaning "this variable is TOP"
        private int stackHeight;

        private final BasicBlock bb;

        /**
         * I'm not using clone because I don't want to necessarily inherit the AbstractVariable state from the superclass
         */
        public MachineState duplicate() {
            MachineState result = new MachineState(bb);
            result.copyState(this);
            return result;
        }

        public MachineState(BasicBlock bb) {
            setTOP();
            this.bb = bb;
        }

        public BasicBlock getBasicBlock() {
            return bb;
        }

        void setTOP() {
            stackHeight = -1;
        }

 //      public void push(int i) {
//          if (stack == null)
//              allocateStack();
//          stack[stackHeight++] = i;
//      }

//      public int pop() {
//          if (stackHeight <= 0) {
//              assert stackHeight > 0 : "can't pop stack of height " + stackHeight;
//          }
//          stackHeight -= 1;
//          return stack[stackHeight];
//      }

//      public int peek() {
//          return stack[stackHeight - 1];
//      }

//      public void swap() {
//          int temp = stack[stackHeight - 1];
//          stack[stackHeight - 1] = stack[stackHeight - 2];
//          stack[stackHeight - 2] = temp;
//      }

//      private void allocateStack() {
//          stack = new int[maxStackHeight + 1];
//          stackHeight = 0;
//      }

        public void allocateLocals() {
            locals = allocateNewLocalsArray();
        }

//      public void clearStack() {
//          stackHeight = 0;
//      }

        /**
         * set the value of local i to symbol j
         *
         * @param i
         * @param j
         */
        public void setLocal(int i, int j) {
            if (locals == null) {
                if (OPTIMISTIC && (j == TOP)) {
                    return;
                } else {
                    allocateLocals();
                }
            }
            locals[i] = j;
        }

        /**
         * @param i
         * @return the number of the symbol corresponding to local i
         */
        public int getLocal(int i) {
            if (locals == null) {
                if (OPTIMISTIC) {
                    return TOP;
                } else {
                    return BOTTOM;
                }
            } else {
                return locals[i];
            }
        }

        public void replaceValue(int from, int to) {
            if (stack != null)
                for (int i = 0; i < stackHeight; i++)
                    if (stack[i] == from)
                        stack[i] = to;

            if (locals != null)
                for (int i = 0; i < maxLocals; i++)
                    if (locals[i] == from)
                        locals[i] = to;
        }

        public boolean hasValue(int val) {
            if (stack != null)
                for (int i = 0; i < stackHeight; i++)
                    if (stack[i] == val)
                        return true;

            if (locals != null)
                for (int i = 0; i < maxLocals; i++)
                    if (locals[i] == val)
                        return true;

            return false;
        }

        @Override
        public String toString() {
            // TODO
            return "Some machine state...";
//          if (isTOP()) {
//              return "<TOP>@" + System.identityHashCode(this);
//          }
//          StringBuffer result = new StringBuffer("<");
//          result.append("S");
//          if (stackHeight == 0) {
//              result.append("[empty]");
//          } else {
//              result.append(array2StringBuffer(stack, stackHeight));
//          }
//          result.append("L");
//          result.append(array2StringBuffer(locals, maxLocals));
//          result.append(">");
//          return result.toString();
        }

//      private StringBuffer array2StringBuffer(int[] array, int n) {
//          StringBuffer result = new StringBuffer("[");
//          if (array == null) {
//              result.append(OPTIMISTIC ? "TOP" : "BOTTOM");
//          } else {
//              for (int i = 0; i < n - 1; i++) {
//                  result.append(array[i]).append(",");
//              }
//              result.append(array[n - 1]);
//          }
//          result.append("]");
//          return result;
//      }

        @Override
        public void copyState(MachineState other) {
            if (other.stack == null) {
                stack = null;
            } else {
                stack = new int[other.stack.length];
                System.arraycopy(other.stack, 0, stack, 0, other.stack.length);
            }
            if (other.locals == null) {
                locals = null;
            } else {
                locals = new int[other.locals.length];
                System.arraycopy(other.locals, 0, locals, 0, other.locals.length);
            }
            stackHeight = other.stackHeight;
        }

        boolean stateEquals(MachineState exit) {
            if (stackHeight != exit.stackHeight)
                return false;
            if (locals == null) {
                if (exit.locals != null)
                    return false;
            } else {
                if (exit.locals == null)
                    return false;
                else if (locals.length != exit.locals.length)
                    return false;
            }

            for (int i = 0; i < stackHeight; i++) {
                if (stack[i] != exit.stack[i])
                    return false;
            }
            if (locals != null) {
                for (int i = 0; i < locals.length; i++) {
                    if (locals[i] == TOP) {
                        if (exit.locals[i] != TOP)
                            return false;
                    }
                    if (locals[i] != exit.locals[i])
                        return false;
                }
            }
            return true;
        }

        /**
         * Returns the stackHeight.
         *
         * @return int
         */
         public int getStackHeight() {
             return stackHeight;
         }

         /**
          * Use with care.
          */
         public int[] getLocals() {
             return locals;
         }

    }

    public int[] allocateNewLocalsArray() {
        int[] result = new int[maxLocals];
        for (int i = 0; i < maxLocals; i++) {
            result[i] = OPTIMISTIC ? TOP : BOTTOM;
        }
        return result;
    }

    /**
     * Interface which defines a flow function for a basic block
     */
    public interface FlowProvider {

        public boolean needsNodeFlow();

        public boolean needsEdgeFlow();

        /**
         * Compute the MachineState at the exit of a basic block, given a MachineState at the block's entry.
         */
        public MachineState flow(MachineState entry, BasicBlock basicBlock);

        /**
         * Compute the MachineState at the end of an edge, given a MachineState at the edges's entry.
         */
        public MachineState flow(MachineState entry, BasicBlock from, BasicBlock to);
    }

    /**
     * This gives some basic facilities for shoving things around on the stack. Client analyses should subclass this as needed.
     */
    protected static abstract class BasicRegisterFlowProvider implements FlowProvider, DexConstants {
        private final DexCFG cfg;

        protected MachineState workingState;

        private BasicRegisterMachineVisitor visitor;

        private Visitor edgeVisitor;

        private int currentInstructionIndex = 0;

        private BasicBlock currentBlock;

        private BasicBlock currentSuccessorBlock;

        /**
         * Only subclasses can instantiate
         */
        protected BasicRegisterFlowProvider(DexCFG cfg) {
            this.cfg = cfg;
        }

        /**
         * Initialize the visitors used to perform the flow functions
         */
        protected void init(BasicRegisterMachineVisitor v, Visitor ev) {
            this.visitor = v;
            this.edgeVisitor = ev;
        }

        @Override
        public boolean needsNodeFlow() {
            return true;
        }

        @Override
        public boolean needsEdgeFlow() {
            return false;
        }

        @Override
        public MachineState flow(MachineState entry, BasicBlock basicBlock) {
            workingState = entry.duplicate();
            currentBlock = basicBlock;
            currentSuccessorBlock = null;
            Instruction[] instructions = getInstructions();
            if (DEBUG) {
                System.err.println("Entry to BB" + cfg.getNumber(basicBlock) + " " + workingState);
            }
            for (int i = basicBlock.getFirstInstructionIndex(); i <= basicBlock.getLastInstructionIndex(); i++) {
                currentInstructionIndex = i;
                instructions[i].visit(visitor);

                if (DEBUG) {
                    
                }
            }
            return workingState;
        }

        @Override
        public MachineState flow(MachineState entry, BasicBlock from, BasicBlock to) {
            workingState = entry.duplicate();
            currentBlock = from;
            currentSuccessorBlock = to;
            Instruction[] instructions = getInstructions();
            if (DEBUG) {
                System.err.println("Entry to BB" + cfg.getNumber(from) + " " + workingState);
            }
            for (int i = from.getFirstInstructionIndex(); i <= from.getLastInstructionIndex(); i++) {
                currentInstructionIndex = i;
                instructions[i].visit(edgeVisitor);
                if (DEBUG) {
                    
                }
            }
            return workingState;
        }

        protected int getCurrentInstructionIndex() {
            return currentInstructionIndex;
        }

        protected int getCurrentProgramCounter() {
            return cfg.getProgramCounter(currentInstructionIndex);
        }

        protected BasicBlock getCurrentBlock() {
            return currentBlock;
        }

        protected BasicBlock getCurrentSuccessor() {
            return currentSuccessorBlock;
        }

        public abstract Instruction[] getInstructions();

        /**
         * Update the machine state to account for an instruction
         */
        protected class BasicRegisterMachineVisitor extends Visitor {

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayLength(ArrayLengthInstruction)
             */
            @Override
            public void visitArrayLength(ArrayLength instruction) {

                // TODO
                //        workingState.pop();
                //        workingState.push(UNANALYZED);
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayLoad(IArrayLoadInstruction)
             */
            @Override
            public void visitArrayGet(ArrayGet instruction) {

                // TODO
                //        workingState.pop();
                //        workingState.pop();
                //        workingState.push(UNANALYZED);
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitArrayStore(IArrayStoreInstruction)
             */
            @Override
            public void visitArrayPut(ArrayPut instruction) {
                // TODO
                //        workingState.pop();
                //        workingState.pop();
                //        workingState.pop();
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitBinaryOp(IBinaryOpInstruction)
             */
            @Override
            public void visitBinaryOperation(BinaryOperation instruction) {
                // TODO
//              workingState.pop();
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitComparison(IComparisonInstruction)
             */
//          @Override
//          public void visitComparison(IComparisonInstruction instruction) {
//              workingState.pop();
//              workingState.pop();
//              workingState.push(UNANALYZED);
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConditionalBranch(IConditionalBranchInstruction)
             */
            @Override
            public void visitBranch(Branch instruction) {
                // TODO
//              workingState.pop();
//              workingState.pop();
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConstant(ConstantInstruction)
             */
            @Override
            public void visitConstant(Constant instruction) {
                // TODO
//              workingState.push(UNANALYZED);
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitConversion(IConversionInstruction)
             */
//          @Override
//          public void visitConversion(IConversionInstruction instruction) {
//              workingState.pop();
//              workingState.push(UNANALYZED);
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitDup(DupInstruction)
             */
//          @Override
//          public void visitDup(DupInstruction instruction) {
//
//              int size = instruction.getSize();
//              int delta = instruction.getDelta();
//              assert size == 1 || size == 2;
//              assert delta == 0 || delta == 1 || delta == 2;
//              int toPop = size + delta;
//              int v1 = workingState.pop();
//              int v2 = (toPop > 1) ? workingState.pop() : IGNORE;
//              int v3 = (toPop > 2) ? workingState.pop() : IGNORE;
//              int v4 = (toPop > 3) ? workingState.pop() : IGNORE;
//
//              if (size > 1) {
//                  workingState.push(v2);
//              }
//              workingState.push(v1);
//              if (v4 != IGNORE) {
//                  workingState.push(v4);
//              }
//              if (v3 != IGNORE) {
//                  workingState.push(v3);
//              }
//              if (v2 != IGNORE) {
//                  workingState.push(v2);
//              }
//              workingState.push(v1);
//
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitGet(IGetInstruction)
             */
            @Override
            public void visitGetField(GetField instruction) {
                // TODO
//              popN(instruction);
//              workingState.push(UNANALYZED);
                throw new UnimplementedError();
            }

//          protected void popN(IInstruction instruction) {
//              for (int i = 0; i < instruction.getPoppedCount(); i++) {
//                  workingState.pop();
//              }
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitInstanceof
             */
            @Override
            public void visitInstanceof(InstanceOf instruction) {
                // TODO
//              workingState.pop();
//              workingState.push(UNANALYZED);
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitInvoke(IInvokeInstruction)
             */
            @Override
            public void visitInvoke(Invoke instruction) {
                // TODO
                throw new UnimplementedError();
//              popN(instruction);
//              ClassLoaderReference loader = cfg.getMethod().getDeclaringClass().getClassLoader().getReference();
//              TypeReference returnType = ShrikeUtil.makeTypeReference(loader, Util.getReturnType(instruction.getMethodSignature()));
//              if (!returnType.equals(TypeReference.Void)) {
//                  workingState.push(UNANALYZED);
//              }
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitMonitor(MonitorInstruction)
             */
            @Override
            public void visitMonitor(Monitor instruction) {
                // TODO
//              workingState.pop();
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitLocalLoad(ILoadInstruction)
             */
//          @Override
//          public void visitLocalLoad(ILoadInstruction instruction) {
//              int t = workingState.getLocal(instruction.getVarIndex());
//              workingState.push(t);
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitLocalStore(IStoreInstruction)
             */
//          @Override
//          public void visitLocalStore(IStoreInstruction instruction) {
//              int index = instruction.getVarIndex();
//              workingState.setLocal(index, workingState.pop());
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitNew(NewInstruction)
             */
            @Override
            public void visitNew(New instruction) {
                // TODO
//              popN(instruction);
//              workingState.push(UNANALYZED);
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitPop(PopInstruction)
             */
//          @Override
//          public void visitPop(PopInstruction instruction) {
//              if (instruction.getPoppedCount() > 0) {
//                  workingState.pop();
//              }
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitPut(IPutInstruction)
             */
            @Override
            public void visitPutField(PutField instruction) {
                // TODO
//              popN(instruction);
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitShift(IShiftInstruction)
             */
//          @Override
//          public void visitShift(IShiftInstruction instruction) {
//              workingState.pop();
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitSwap(SwapInstruction)
             */
//          @Override
//          public void visitSwap(SwapInstruction instruction) {
//              workingState.swap();
//          }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitSwitch(SwitchInstruction)
             */
            @Override
            public void visitSwitch(Switch instruction) {
                // TODO
//              workingState.pop();
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitThrow(ThrowInstruction)
             */
            @Override
            public void visitThrow(Throw instruction) {
                // TODO
//              int exceptionType = workingState.pop();
//              workingState.clearStack();
//              workingState.push(exceptionType);
                throw new UnimplementedError();
            }

            /**
             * @see com.ibm.wala.shrikeBT.IInstruction.Visitor#visitUnaryOp(IUnaryOpInstruction)
             */
            @Override
            public void visitUnaryOperation(UnaryOperation instruction) {
                // TODO
                // treated as a no-op in basic scheme
                throw new UnimplementedError();
            }
        }
    }
}
