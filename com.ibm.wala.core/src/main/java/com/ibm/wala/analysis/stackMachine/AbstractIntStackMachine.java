/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.analysis.stackMachine;

import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.cfg.ShrikeCFG.BasicBlock;
import com.ibm.wala.core.util.CancelRuntimeException;
import com.ibm.wala.core.util.shrike.ShrikeUtil;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BasicFramework;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.AbstractVariable;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.shrike.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrike.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrike.shrikeBT.Constants;
import com.ibm.wala.shrike.shrikeBT.DupInstruction;
import com.ibm.wala.shrike.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrike.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrike.shrikeBT.IConversionInstruction;
import com.ibm.wala.shrike.shrikeBT.IGetInstruction;
import com.ibm.wala.shrike.shrikeBT.IInstanceofInstruction;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrike.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrike.shrikeBT.IPutInstruction;
import com.ibm.wala.shrike.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrike.shrikeBT.IStoreInstruction;
import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrike.shrikeBT.NewInstruction;
import com.ibm.wala.shrike.shrikeBT.PopInstruction;
import com.ibm.wala.shrike.shrikeBT.SwapInstruction;
import com.ibm.wala.shrike.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrike.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrike.shrikeBT.Util;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.INodeWithNumber;
import java.util.Arrays;

/**
 * Skeleton of functionality to propagate information through the Java bytecode stack machine using
 * ShrikeBT.
 *
 * <p>This class computes properties the Java operand stack and of the local variables at the
 * beginning of each basic block.
 *
 * <p>In this implementation, each dataflow variable value is an integer, and the "meeter" object
 * provides the meets
 */
public abstract class AbstractIntStackMachine implements FixedPointConstants {

  private static final boolean DEBUG = false;

  public static final int TOP = -1;

  public static final int BOTTOM = -2;

  public static final int UNANALYZED = -3;

  public static final int IGNORE = -4;

  /** The solver */
  private DataflowSolver<BasicBlock, MachineState> solver;

  /** The control flow graph to analyze */
  private final ShrikeCFG cfg;

  /** Should uninitialized variables be considered TOP (optimistic) or BOTTOM (pessimistic); */
  public static final boolean OPTIMISTIC = true;

  protected AbstractIntStackMachine(final ShrikeCFG G) {
    if (G == null) {
      throw new IllegalArgumentException("G is null");
    }
    this.cfg = G;
  }

  protected void init(Meeter meeter, final FlowProvider flow) {
    final MeetOperator meet = new MeetOperator(meeter);
    ITransferFunctionProvider<BasicBlock, MachineState> xferFunctions =
        new ITransferFunctionProvider<BasicBlock, MachineState>() {
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
          public UnaryOperator<MachineState> getEdgeTransferFunction(
              final BasicBlock from, final BasicBlock to) {
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
    solver =
        new DataflowSolver<BasicBlock, MachineState>(problem) {
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
            for (INodeWithNumber s :
                Iterator2Iterable.make(getFixedPointSystem().getStatementsThatUse(entry))) {
              addToWorkList((AbstractStatement<?, ?>) s);
            }
          }

          @Override
          protected void initializeVariables() {
            super.initializeVariables();
            AbstractIntStackMachine.this.initializeVariables();
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

  /** Convenience method ... a little ugly .. perhaps delete later. */
  protected void initializeVariables() {}

  public MachineState getEntryState() {
    return solver.getIn(cfg.entry());
  }

  /** @return the state at the entry to a given block */
  public MachineState getIn(ShrikeCFG.BasicBlock bb) {
    return solver.getIn(bb);
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
   * A Meeter object provides the dataflow logic needed to meet the abstract machine state for a
   * dataflow meet.
   */
  protected interface Meeter {

    /**
     * Return the integer that represents the meet of a particular stack slot at the entry to a
     * basic block.
     *
     * @param slot The stack slot to meet
     * @param rhs The values to meet
     * @param bb The basic block at whose entry this meet occurs
     * @return The value result of the meet
     */
    int meetStack(int slot, int[] rhs, BasicBlock bb);

    /**
     * Return the integer that represents stack slot 0 after a meet at the entry to a catch block.
     *
     * @param bb The basic block at whose entry this meet occurs
     * @return The value of stack slot 0 after the meet
     */
    int meetStackAtCatchBlock(BasicBlock bb);

    /**
     * Return the integer that represents the meet of a particular local at the entry to a basic
     * block.
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
   * <p>TODO: add some efficiency shortcuts. TODO: clean up and refactor.
   *
   * @param bb the basic block at whose entry the meet occurs
   * @return true if the lhs value changes. false otherwise.
   */
  private static boolean meet(MachineState lhs, MachineState[] rhs, BasicBlock bb, Meeter meeter) {

    boolean changed = meetStacks(lhs, rhs, bb, meeter);

    changed |= meetLocals(lhs, rhs, bb, meeter);
    return changed;
  }

  /**
   * Evaluate a meet of machine states at a catch block.
   *
   * <p>TODO: add some efficiency shortcuts. TODO: clean up and refactor.
   *
   * @param bb the basic block at whose entry the meet occurs
   * @return true if the lhs value changes. false otherwise.
   */
  private static boolean meetForCatchBlock(
      MachineState lhs, MachineState[] rhs, BasicBlock bb, Meeter meeter) {

    boolean changed = meetStacksAtCatchBlock(lhs, bb, meeter);
    changed |= meetLocals(lhs, rhs, bb, meeter);
    return changed;
  }

  /**
   * Evaluate a meet of the stacks of machine states at the entry of a catch block.
   *
   * <p>TODO: add some efficiency shortcuts. TODO: clean up and refactor.
   *
   * @param bb the basic block at whose entry the meet occurs
   * @return true if the lhs value changes. false otherwise.
   */
  private static boolean meetStacksAtCatchBlock(MachineState lhs, BasicBlock bb, Meeter meeter) {
    boolean changed = false;

    // evaluate the meet of the stack of height 1, which holds the exception
    // object.

    // allocate lhs.stack if it's
    // not already allocated.
    if (lhs.stack == null) {
      lhs.allocateStack(1);
      lhs.stackHeight = 1;
    }

    int meet = meeter.meetStackAtCatchBlock(bb);
    if (lhs.stack[0] == TOP) {
      if (meet != TOP) {
        changed = true;
        lhs.stack[0] = meet;
      }
    } else if (meet != lhs.stack[0]) {
      changed = true;
      lhs.stack[0] = meet;
    }
    return changed;
  }

  /**
   * Evaluate a meet of the stacks of machine states at the entry of a basic block.
   *
   * <p>TODO: add some efficiency shortcuts. TODO: clean up and refactor.
   *
   * @param bb the basic block at whose entry the meet occurs
   * @return true if the lhs value changes. false otherwise.
   */
  private static boolean meetStacks(
      MachineState lhs, MachineState[] rhs, BasicBlock bb, Meeter meeter) {
    boolean changed = false;

    // evaluate the element-wise meet over the stacks

    // first ... how high are the stacks?
    int height = computeMeetStackHeight(rhs);

    // if there's any stack height to meet, allocate lhs.stack if it's
    // not already allocated.
    if (height > -1 && (lhs.stack == null || lhs.stack.length < height)) {
      lhs.allocateStack(height);
      lhs.stackHeight = height;
      changed = true;
    }

    // now do the element-wise meet.
    for (int i = 0; i < height; i++) {
      int[] R = new int[rhs.length];
      for (int j = 0; j < R.length; j++) {
        MachineState m = rhs[j];
        if (m.stack == null || m.stack.length < i + 1) {
          R[j] = TOP;
        } else {
          R[j] = m.stack[i];
          if (R[j] == 0) {
            R[j] = TOP;
          }
        }
      }
      int meet = meeter.meetStack(i, R, bb);
      if (lhs.stack[i] == TOP) {
        if (meet != TOP) {
          changed = true;
          lhs.stack[i] = meet;
        }
      } else if (meet != lhs.stack[i]) {
        changed = true;
        lhs.stack[i] = meet;
      }
    }
    return changed;
  }

  /**
   * Evaluate a meet of locals of machine states at the entry to a basic block.
   *
   * <p>TODO: add some efficiency shortcuts. TODO: clean up and refactor.
   *
   * @param bb the basic block at whose entry the meet occurs
   * @return true if the lhs value changes. false otherwise.
   */
  private static boolean meetLocals(
      MachineState lhs, MachineState[] rhs, BasicBlock bb, Meeter meeter) {

    boolean changed = false;
    // need we allocate lhs.locals?
    int nLocals = computeMeetNLocals(rhs);
    if (nLocals > -1 && (lhs.locals == null || lhs.locals.length < nLocals)) {
      lhs.allocateLocals(nLocals);
    }

    // evaluate the element-wise meet over the locals.
    for (int i = 0; i < nLocals; i++) {
      int[] R = new int[rhs.length];
      for (int j = 0; j < rhs.length; j++) {
        R[j] = rhs[j].getLocal(i);
      }
      int meet = meeter.meetLocal(i, R, bb);
      if (lhs.locals[i] == TOP) {
        if (meet != TOP) {
          changed = true;
          lhs.locals[i] = meet;
        }
      } else if (meet != lhs.locals[i]) {
        changed = true;
        lhs.locals[i] = meet;
      }
    }
    return changed;
  }

  /**
   * @return the number of locals to meet. Return -1 if there is no local meet necessary.
   * @param operands The operands for this operator. operands[0] is the left-hand side.
   */
  private static int computeMeetNLocals(MachineState[] operands) {
    MachineState lhs = operands[0];
    int nLocals = -1;
    if (lhs.locals != null) {
      nLocals = lhs.locals.length;
    } else {
      for (int i = 1; i < operands.length; i++) {
        MachineState rhs = operands[i];
        if (rhs.locals != null) {
          nLocals = rhs.locals.length;
          break;
        }
      }
    }
    return nLocals;
  }

  /**
   * @return the height of stacks that are being meeted. Return -1 if there is no stack meet
   *     necessary.
   * @param operands The operands for this operator. operands[0] is the left-hand side.
   */
  private static int computeMeetStackHeight(MachineState[] operands) {
    MachineState lhs = operands[0];
    int height = -1;
    if (lhs.stack != null) {
      height = lhs.stackHeight;
    } else {
      for (int i = 1; i < operands.length; i++) {
        MachineState rhs = operands[i];
        if (rhs.stack != null) {
          height = rhs.stackHeight;
          break;
        }
      }
    }
    return height;
  }

  /** Representation of the state of the JVM stack machine at some program point. */
  public class MachineState extends AbstractVariable<MachineState> {
    private int[] stack;

    private int[] locals;

    // NOTE: stackHeight == -1 is a special code meaning "this variable is TOP"
    private int stackHeight;

    private final BasicBlock bb;

    /**
     * I'm not using clone because I don't want to necessarily inherit the AbstractVariable state
     * from the superclass
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
      stack = null;
    }

    boolean isTOP() {
      return stackHeight == -1;
    }

    public void push(int i) {
      if (stack == null || stackHeight >= stack.length) allocateStack(stackHeight + 1);
      stack[stackHeight++] = i;
    }

    public int pop() {
      if (stackHeight <= 0) {
        assert stackHeight > 0 : "can't pop stack of height " + stackHeight;
      }
      stackHeight -= 1;
      return stack[stackHeight];
    }

    public int peek() {
      return stack[stackHeight - 1];
    }

    public void swap() {
      int temp = stack[stackHeight - 1];
      stack[stackHeight - 1] = stack[stackHeight - 2];
      stack[stackHeight - 2] = temp;
    }

    private void allocateStack(int stackHeight) {
      if (stack == null) {
        stack = new int[stackHeight + 1];
        this.stackHeight = 0;
      } else {
        stack = Arrays.copyOf(stack, Math.max(stack.length, stackHeight) * 2 + 1);
      }
    }

    private void allocateLocals(int maxLocals) {
      int[] result = new int[maxLocals];
      int start = 0;
      if (locals != null) {
        System.arraycopy(locals, 0, result, 0, locals.length);
        start = locals.length;
      }

      for (int i = start; i < maxLocals; i++) {
        result[i] = OPTIMISTIC ? TOP : BOTTOM;
      }

      locals = result;
    }

    public void clearStack() {
      stackHeight = 0;
    }

    /** set the value of local i to symbol j */
    public void setLocal(int i, int j) {
      if (locals == null || locals.length < i + 1) {
        if (OPTIMISTIC && (j == TOP)) {
          return;
        } else {
          allocateLocals(i + 1);
        }
      }
      locals[i] = j;
    }

    /** @return the number of the symbol corresponding to local i */
    public int getLocal(int i) {
      if (locals == null || locals.length < i + 1) {
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
      if (stack != null) for (int i = 0; i < stackHeight; i++) if (stack[i] == from) stack[i] = to;

      if (locals != null)
        for (int i = 0; i < locals.length; i++) if (locals[i] == from) locals[i] = to;
    }

    public boolean hasValue(int val) {
      if (stack != null) for (int i = 0; i < stackHeight; i++) if (stack[i] == val) return true;

      if (locals != null) for (int local : locals) if (local == val) return true;

      return false;
    }

    @Override
    public String toString() {
      if (isTOP()) {
        return "<TOP>@" + System.identityHashCode(this);
      }
      StringBuilder result = new StringBuilder("<");
      result.append('S');
      if (stackHeight == 0) {
        result.append("[empty]");
      } else {
        result.append(array2StringBuffer(stack, stackHeight));
      }
      result.append('L');
      result.append(array2StringBuffer(locals, locals == null ? 0 : locals.length));
      result.append('>');
      return result.toString();
    }

    private StringBuilder array2StringBuffer(int[] array, int n) {
      StringBuilder result = new StringBuilder("[");
      if (array == null) {
        result.append(OPTIMISTIC ? "TOP" : "BOTTOM");
      } else {
        for (int i = 0; i < n - 1; i++) {
          result.append(array[i]).append(',');
        }
        result.append(array[n - 1]);
      }
      result.append(']');
      return result;
    }

    @Override
    public void copyState(MachineState other) {
      stack = other.stack == null ? null : other.stack.clone();
      locals = other.locals == null ? null : other.locals.clone();
      stackHeight = other.stackHeight;
    }

    boolean stateEquals(MachineState exit) {
      if (stackHeight != exit.stackHeight) return false;
      if (locals == null) {
        if (exit.locals != null) return false;
      } else {
        if (exit.locals == null) return false;
        else if (locals.length != exit.locals.length) return false;
      }

      for (int i = 0; i < stackHeight; i++) {
        if (stack[i] != exit.stack[i]) return false;
      }
      if (locals != null) {
        for (int i = 0; i < locals.length; i++) {
          if (locals[i] == TOP) {
            if (exit.locals[i] != TOP) return false;
          }
          if (locals[i] != exit.locals[i]) return false;
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

    /** Use with care. */
    public int[] getLocals() {
      return locals;
    }
  }

  /** Interface which defines a flow function for a basic block */
  public interface FlowProvider {

    public boolean needsNodeFlow();

    public boolean needsEdgeFlow();

    /**
     * Compute the MachineState at the exit of a basic block, given a MachineState at the block's
     * entry.
     */
    public MachineState flow(MachineState entry, BasicBlock basicBlock);

    /**
     * Compute the MachineState at the end of an edge, given a MachineState at the edges's entry.
     */
    public MachineState flow(MachineState entry, BasicBlock from, BasicBlock to);
  }

  /**
   * This gives some basic facilities for shoving things around on the stack. Client analyses should
   * subclass this as needed.
   */
  protected abstract static class BasicStackFlowProvider implements FlowProvider, Constants {
    private final ShrikeCFG cfg;

    protected MachineState workingState;

    private BasicStackMachineVisitor visitor;

    private com.ibm.wala.shrike.shrikeBT.IInstruction.Visitor edgeVisitor;

    private int currentInstructionIndex = 0;

    private BasicBlock currentBlock;

    private BasicBlock currentSuccessorBlock;

    /** Only subclasses can instantiate */
    protected BasicStackFlowProvider(ShrikeCFG cfg) {
      this.cfg = cfg;
    }

    /** Initialize the visitors used to perform the flow functions */
    protected void init(
        BasicStackMachineVisitor v, com.ibm.wala.shrike.shrikeBT.IInstruction.Visitor ev) {
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
      IInstruction[] instructions = getInstructions();
      if (DEBUG) {
        System.err.println(("Entry to BB" + cfg.getNumber(basicBlock) + ' ' + workingState));
      }
      for (int i = basicBlock.getFirstInstructionIndex();
          i <= basicBlock.getLastInstructionIndex();
          i++) {
        currentInstructionIndex = i;
        instructions[i].visit(visitor);

        if (DEBUG) {
          System.err.println(("After " + instructions[i] + ' ' + workingState));
        }
      }
      return workingState;
    }

    @Override
    public MachineState flow(MachineState entry, BasicBlock from, BasicBlock to) {
      workingState = entry.duplicate();
      currentBlock = from;
      currentSuccessorBlock = to;
      IInstruction[] instructions = getInstructions();
      if (DEBUG) {
        System.err.println(("Entry to BB" + cfg.getNumber(from) + ' ' + workingState));
      }
      for (int i = from.getFirstInstructionIndex(); i <= from.getLastInstructionIndex(); i++) {
        currentInstructionIndex = i;
        instructions[i].visit(edgeVisitor);
        if (DEBUG) {
          System.err.println(("After " + instructions[i] + ' ' + workingState));
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

    public abstract IInstruction[] getInstructions();

    /** Update the machine state to account for an instruction */
    protected class BasicStackMachineVisitor extends IInstruction.Visitor {

      @Override
      public void visitArrayLength(ArrayLengthInstruction instruction) {

        workingState.pop();
        workingState.push(UNANALYZED);
      }

      @Override
      public void visitArrayLoad(IArrayLoadInstruction instruction) {
        workingState.pop();
        workingState.pop();
        workingState.push(UNANALYZED);
      }

      @Override
      public void visitArrayStore(IArrayStoreInstruction instruction) {
        workingState.pop();
        workingState.pop();
        workingState.pop();
      }

      @Override
      public void visitBinaryOp(IBinaryOpInstruction instruction) {
        workingState.pop();
      }

      @Override
      public void visitComparison(IComparisonInstruction instruction) {
        workingState.pop();
        workingState.pop();
        workingState.push(UNANALYZED);
      }

      @Override
      public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
        workingState.pop();
        workingState.pop();
      }

      @Override
      public void visitConstant(ConstantInstruction instruction) {
        workingState.push(UNANALYZED);
      }

      @Override
      public void visitConversion(IConversionInstruction instruction) {
        workingState.pop();
        workingState.push(UNANALYZED);
      }

      @Override
      public void visitDup(DupInstruction instruction) {

        int size = instruction.getSize();
        int delta = instruction.getDelta();
        assert size == 1 || size == 2;
        assert delta == 0 || delta == 1 || delta == 2;
        int toPop = size + delta;
        int v1 = workingState.pop();
        int v2 = (toPop > 1) ? workingState.pop() : IGNORE;
        int v3 = (toPop > 2) ? workingState.pop() : IGNORE;
        int v4 = (toPop > 3) ? workingState.pop() : IGNORE;

        if (size > 1) {
          workingState.push(v2);
        }
        workingState.push(v1);
        if (v4 != IGNORE) {
          workingState.push(v4);
        }
        if (v3 != IGNORE) {
          workingState.push(v3);
        }
        if (v2 != IGNORE) {
          workingState.push(v2);
        }
        workingState.push(v1);
      }

      @Override
      public void visitGet(IGetInstruction instruction) {
        popN(instruction);
        workingState.push(UNANALYZED);
      }

      protected void popN(IInstruction instruction) {
        for (int i = 0; i < instruction.getPoppedCount(); i++) {
          workingState.pop();
        }
      }

      @Override
      public void visitInstanceof(IInstanceofInstruction instruction) {
        workingState.pop();
        workingState.push(UNANALYZED);
      }

      @Override
      public void visitInvoke(IInvokeInstruction instruction) {
        popN(instruction);
        ClassLoaderReference loader =
            cfg.getMethod().getDeclaringClass().getClassLoader().getReference();
        TypeReference returnType =
            ShrikeUtil.makeTypeReference(
                loader, Util.getReturnType(instruction.getMethodSignature()));
        if (!returnType.equals(TypeReference.Void)) {
          workingState.push(UNANALYZED);
        }
      }

      @Override
      public void visitMonitor(MonitorInstruction instruction) {
        workingState.pop();
      }

      @Override
      public void visitLocalLoad(ILoadInstruction instruction) {
        int t = workingState.getLocal(instruction.getVarIndex());
        workingState.push(t);
      }

      @Override
      public void visitLocalStore(IStoreInstruction instruction) {
        int index = instruction.getVarIndex();
        workingState.setLocal(index, workingState.pop());
      }

      @Override
      public void visitNew(NewInstruction instruction) {
        popN(instruction);
        workingState.push(UNANALYZED);
      }

      @Override
      public void visitPop(PopInstruction instruction) {
        if (instruction.getPoppedCount() > 0) {
          workingState.pop();
        }
      }

      @Override
      public void visitPut(IPutInstruction instruction) {
        popN(instruction);
      }

      @Override
      public void visitShift(IShiftInstruction instruction) {
        workingState.pop();
      }

      @Override
      public void visitSwap(SwapInstruction instruction) {
        workingState.swap();
      }

      @Override
      public void visitSwitch(SwitchInstruction instruction) {
        workingState.pop();
      }

      @Override
      public void visitThrow(ThrowInstruction instruction) {
        int exceptionType = workingState.pop();
        workingState.clearStack();
        workingState.push(exceptionType);
      }

      @Override
      public void visitUnaryOp(IUnaryOpInstruction instruction) {
        // treated as a no-op in basic scheme
      }
    }
  }
}
