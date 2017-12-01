/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT.tools;

import java.util.Arrays;
import java.util.BitSet;

import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.PopInstruction;
import com.ibm.wala.shrikeBT.StoreInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.info.LocalAllocator;

@Deprecated
public final class MethodOptimizer {
  final private MethodData data;

  private IInstruction[] instructions;

  private ExceptionHandler[][] handlers;

  private final MethodEditor editor;

  // The value at index [i][N] is the index of the only instruction which pushes
  // a value onto
  // the stack which is #N popped by instruction i, or -2 if there is no such
  // instruction
  // or -1 if there is more than one such instruction.
  private int[][] uniqueStackDefLocations;

  // The value at index i[N] is the index of the only instruction which pops a
  // value off
  // the stack which is pushed by instruction i, or -2 if there is no such
  // instruction
  // or -1 if there is more than one such instruction.
  private int[] uniqueStackUseLocations;

  private int[] stackSizes;

  private int[][] backEdges;

  // The value at index i is the index of the only instruction which stores a
  // value onto
  // the stack which is popped by instruction i, or -2 if there is no such
  // instruction
  // or -1 if there is more than one such instruction.

  final static int[] noEdges = new int[0];

  public MethodOptimizer(MethodData d, MethodEditor e) {
    if (d == null) {
      throw new IllegalArgumentException("null d");
    }
    this.data = d;
    this.editor = e;
  }

  public MethodOptimizer(MethodData d) {
    this(d, new MethodEditor(d));
  }

  public static class UnoptimizableCodeException extends Exception {
    private static final long serialVersionUID = 2543170335674010642L;

    public UnoptimizableCodeException(String s) {
      super(s);
    }
  }

  public int findUniqueStackDef(final int instr, final int stack) throws UnoptimizableCodeException {
    instructions = editor.getInstructions();
    handlers = editor.getHandlers();
    checkConsistentStackSizes();
    buildBackEdges();
    buildStackDefMap();

    return uniqueStackDefLocations[instr][stack];
  }

  public void optimize() throws UnoptimizableCodeException {
    boolean changed;
    do {
      instructions = editor.getInstructions();
      handlers = editor.getHandlers();
      checkConsistentStackSizes();
      buildBackEdges();

      editor.beginPass();
      buildStackDefMap();
      pushBackLocalStores();
      forwardDups();
      changed = editor.applyPatches();
      editor.endPass();
    } while (changed);
  }

  private void buildBackEdges() {
    int[] backEdgeCount = new int[instructions.length];

    for (int i = 0; i < instructions.length; i++) {
      int[] targets = instructions[i].getBranchTargets();
      for (int j = 0; j < targets.length; j++) {
        backEdgeCount[targets[j]]++;
      }
      ExceptionHandler[] hs = handlers[i];
      for (int j = 0; j < hs.length; j++) {
        backEdgeCount[hs[j].getHandler()]++;
      }
    }

    backEdges = new int[instructions.length][];
    for (int i = 0; i < backEdges.length; i++) {
      if (backEdgeCount[i] > 0) {
        backEdges[i] = new int[backEdgeCount[i]];
      } else {
        backEdges[i] = noEdges;
      }
    }
    Arrays.fill(backEdgeCount, 0);

    for (int i = 0; i < instructions.length; i++) {
      int[] targets = instructions[i].getBranchTargets();
      for (int target2 : targets) {
        int target = target2;
        backEdges[target][backEdgeCount[target]] = i;
        backEdgeCount[target]++;
      }
      ExceptionHandler[] hs = handlers[i];
      for (ExceptionHandler element : hs) {
        int target = element.getHandler();
        backEdges[target][backEdgeCount[target]] = i;
        backEdgeCount[target]++;
      }
    }
  }

  private int checkConsistentStackSizes() throws UnoptimizableCodeException {
    stackSizes = new int[instructions.length];
    Arrays.fill(stackSizes, -1);

    checkStackSizesAt(0, 0);

    int result = 0;
    for (int stackSize : stackSizes) {
      result = Math.max(result, stackSize);
    }
    return result;
  }

  private void checkStackSizesAt(int instruction, int stackSize) throws UnoptimizableCodeException {
    while (true) {
      if (instruction < 0 || instruction >= instructions.length) {
        throw new UnoptimizableCodeException("Code exits in an illegal way");
      }
      if (stackSizes[instruction] != -1) {
        if (stackSizes[instruction] != stackSize) {
          throw new UnoptimizableCodeException("Mismatched stack sizes at " + instruction + ": " + stackSize + " and "
              + stackSizes[instruction]);
        } else {
          return;
        }
      }
      stackSizes[instruction] = stackSize;

      IInstruction instr = instructions[instruction];
      stackSize -= instr.getPoppedCount();
      if (stackSize < 0) {
        throw new UnoptimizableCodeException("Stack underflow at " + instruction);
      }
      if (instr instanceof DupInstruction) {
        DupInstruction d = (DupInstruction) instr;
        stackSize += d.getSize() + d.getPoppedCount();
      } else if (instr.getPushedType(null) != null) {
        stackSize++;
      }

      int[] targets = instr.getBranchTargets();
      for (int target : targets) {
        checkStackSizesAt(target, stackSize);
      }

      ExceptionHandler[] hs = handlers[instruction];
      for (ExceptionHandler element : hs) {
        checkStackSizesAt(element.getHandler(), 1);
      }

      if (!instr.isFallThrough()) {
        return;
      }

      instruction++;
    }
  }

  private static boolean instructionKillsVar(IInstruction instr, int v) {
    if (instr instanceof StoreInstruction) {
      StoreInstruction st = (StoreInstruction) instr;
      return st.getVarIndex() == v || (Util.getWordSize(st.getType()) == 2 && st.getVarIndex() + 1 == v);
    } else {
      return false;
    }
  }

  private void forwardDups() {
    for (int i = 0; i < instructions.length; i++) {
      IInstruction instr = instructions[i];
      if (instr instanceof DupInstruction && ((DupInstruction) instr).getDelta() == 0 && uniqueStackDefLocations[i][0] >= 0
          && instructions[uniqueStackDefLocations[i][0]] instanceof LoadInstruction) {
        int source = uniqueStackDefLocations[i][0];
        final LoadInstruction li = (LoadInstruction) instructions[source];

        for (int j = 0; j < instructions.length; j++) {
          int[] locs = uniqueStackDefLocations[j];
          if (locs[0] == i) {
            // check to see if the variable is killed along any path from the
            // dup
            // to its use
            BitSet path = getInstructionsOnPath(source, j);
            boolean killed = false;
            int v = li.getVarIndex();
            for (int k = 0; j < instructions.length && !killed; k++) {
              if (path.get(k)) {
                if (instructionKillsVar(instructions[k], v)) {
                  killed = true;
                }
              }
            }

            if (!killed) {
              editor.insertBefore(j, new MethodEditor.Patch() {
                @Override
                public void emitTo(Output w) {
                  w.emit(PopInstruction.make(1));
                  w.emit(li);
                }
              });
            }
          }
        }
      }
    }
  }

  private void pushBackLocalStores() {
    for (int i = 0; i < instructions.length; i++) {
      IInstruction instr = instructions[i];
      if (instr instanceof StoreInstruction && uniqueStackDefLocations[i][0] >= 0 && uniqueStackDefLocations[i][0] != i - 1
          && uniqueStackUseLocations[uniqueStackDefLocations[i][0]] == i) {
        final StoreInstruction s = (StoreInstruction) instr;
        int source = uniqueStackDefLocations[i][0];

        // Check if the path from source to i contains anything killing the
        // variable
        BitSet path = getInstructionsOnPath(source, i);
        boolean killed = false;
        int v = s.getVarIndex();
        for (int j = 0; j < instructions.length && !killed; j++) {
          if (path.get(j)) {
            if (instructionKillsVar(instructions[j], v)) {
              killed = true;
            }
          }
        }

        if (killed) {
          final String type = s.getType();
          final int newVar = LocalAllocator.allocate(data, type);
          // put a store to the newVar right after the source
          editor.insertAfter(source, new MethodEditor.Patch() {
            @Override
            public void emitTo(Output w) {
              w.emit(StoreInstruction.make(type, newVar));
            }
          });
          // load newVar before storing to correct variable
          editor.insertBefore(i, new MethodEditor.Patch() {
            @Override
            public void emitTo(Output w) {
              w.emit(LoadInstruction.make(type, newVar));
            }
          });
        } else {
          // remove store instruction
          editor.replaceWith(i, new MethodEditor.Patch() {
            @Override
            public void emitTo(Output w) {
            }
          });
          // replace it right after the source
          editor.insertAfter(source, new MethodEditor.Patch() {
            @Override
            public void emitTo(Output w) {
              w.emit(s);
            }
          });
        }
      }
    }
  }

  private void buildStackDefMap() {
    int[][] abstractStacks = new int[instructions.length][];

    for (int i = 0; i < instructions.length; i++) {
      abstractStacks[i] = new int[stackSizes[i]];
      Arrays.fill(abstractStacks[i], -2);
    }

    for (int i = 0; i < instructions.length; i++) {
      if (instructions[i] instanceof DupInstruction) {
        DupInstruction d = (DupInstruction) instructions[i];
        for (int j = 0; j < 2 * d.getSize() + d.getDelta(); j++) {
          followStackDef(abstractStacks, i, i + 1, stackSizes[i + 1] - 1 - j);
        }
      } else if (instructions[i].getPushedType(null) != null) {
        followStackDef(abstractStacks, i, i + 1, stackSizes[i + 1] - 1);
      }
    }

    uniqueStackDefLocations = new int[instructions.length][];
    for (int i = 0; i < instructions.length; i++) {
      uniqueStackDefLocations[i] = new int[instructions[i].getPoppedCount()];
      int popped = instructions[i].getPoppedCount();
      System.arraycopy(abstractStacks[i], stackSizes[i] - popped, uniqueStackDefLocations[i], 0, popped);
    }

    uniqueStackUseLocations = new int[instructions.length];
    Arrays.fill(uniqueStackUseLocations, -2);

    for (int i = 0; i < instructions.length; i++) {
      abstractStacks[i] = new int[stackSizes[i]];
      Arrays.fill(abstractStacks[i], -2);
    }

    for (int i = 0; i < instructions.length; i++) {
      int count = instructions[i].getPoppedCount();
      if (count == 1) {
        followStackUse(abstractStacks, i, i, stackSizes[i] - 1);
      } else if (count > 1) {
        for (int j = 0; j < count; j++) {
          followStackUse(abstractStacks, -1, i, stackSizes[i] - 1 - j);
        }
      }
    }

    for (int i = 0; i < instructions.length; i++) {
      if (instructions[i].getPushedType(null) != null) {
        uniqueStackUseLocations[i] = abstractStacks[i + 1][stackSizes[i + 1] - 1];
      }
    }
  }

  private void followStackDef(int[][] abstractDefStacks, int def, int instruction, int stackPointer) {
    while (true) {
      int[] stack = abstractDefStacks[instruction];
      if (stackPointer >= stack.length) {
        // the value must have been popped off by the last instruction
        return;
      }

      if (stack[stackPointer] == -2) {
        stack[stackPointer] = def;
      } else if (stack[stackPointer] == def) {
        return;
      } else if (stack[stackPointer] == -1) {
        return;
      } else {
        stack[stackPointer] = -1;
        def = -1;
      }

      int[] targets = instructions[instruction].getBranchTargets();
      for (int target : targets) {
        followStackDef(abstractDefStacks, def, target, stackPointer);
      }

      ExceptionHandler[] hs = handlers[instruction];
      for (ExceptionHandler element : hs) {
        followStackDef(abstractDefStacks, -1, element.getHandler(), 0);
      }

      if (!instructions[instruction].isFallThrough()) {
        return;
      }
      instruction++;
    }
  }

  private void followStackUse(int[][] abstractUseStacks, int use, int instruction, int stackPointer) {
    while (true) {
      int[] stack = abstractUseStacks[instruction];
      if (stackPointer >= stack.length) {
        // the value must have been pushed by this instruction
        return;
      }

      if (stack[stackPointer] == -2) {
        stack[stackPointer] = use;
      } else if (stack[stackPointer] == use || stack[stackPointer] == -1) {
        return;
      } else {
        stack[stackPointer] = -1;
        use = -1;
      }

      int[] back = backEdges[instruction];
      for (int element : back) {
        followStackUse(abstractUseStacks, use, element, stackPointer);
      }

      if (instruction == 0 || !instructions[instruction - 1].isFallThrough()) {
        return;
      }
      instruction--;
    }
  }

  private BitSet getInstructionsOnPath(int from, int to) {
    BitSet reachable = new BitSet();
    getReachableInstructions(reachable, from, to);
    BitSet reaching = new BitSet();
    getReachingInstructions(reaching, from, to);
    reachable.and(reaching);
    return reachable;
  }

  private void getReachableInstructions(BitSet bits, int from, int to) {
    while (true) {
      if (from == to) {
        return;
      }

      bits.set(from);

      int[] targets = instructions[from].getBranchTargets();
      for (int target : targets) {
        getReachableInstructions(bits, target, to);
      }

      if (!instructions[from].isFallThrough()) {
        return;
      }
      from++;
    }
  }

  private void getReachingInstructions(BitSet bits, int from, int to) {
    while (true) {
      if (to == from) {
        return;
      }

      bits.set(to);

      int[] targets = backEdges[to];
      for (int target : targets) {
        getReachingInstructions(bits, from, target);
      }

      if (to == 0 || !instructions[to - 1].isFallThrough()) {
        return;
      }
      to--;
    }
  }
}
