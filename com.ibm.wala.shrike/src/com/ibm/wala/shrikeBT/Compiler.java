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
package com.ibm.wala.shrikeBT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import com.ibm.wala.shrikeBT.ConstantInstruction.ClassToken;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.Operator;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyProvider;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.ConstantPoolParser.ReferenceToken;

/**
 * This class generates Java bytecode from ShrikeBT Instructions.
 * 
 * If there are too many instructions to fit into 64K bytecodes, then we break the method up, generating auxiliary methods called by
 * the main method.
 * 
 * This class is abstract; there are subclasses for specific class file access toolkits. These toolkits are responsible for
 * providing ways to allocate constant pool entries.
 */
public abstract class Compiler implements Constants {
  // input
  final private boolean isConstructor;
  
  final private boolean isStatic;

  final private String classType;

  final private String signature;

  private final IInstruction[] instructions;

  final private ExceptionHandler[][] handlers;

  final private int[] instructionsToBytecodes;

  private static final int[] noRawHandlers = new int[0];

  private ClassHierarchyProvider hierarchy;

  private ConstantPoolReader presetConstants;

  // working
  private int[] instructionsToOffsets;

  private BitSet branchTargets;

  private byte[][] stackWords;

  private byte[] code;

  // working on breaking up overlarge methods
  private int allocatedLocals;

  private BitSet[] liveLocals;

  private int[][] backEdges;

  private String[][] localTypes;

  private String[][] stackTypes;

  // output
  private int maxLocals;

  private int maxStack;

  private Output mainMethod;

  private ArrayList<Output> auxMethods;

  /**
   * Initialize a Compiler for the given method data.
   * 
   * @param isStatic true iff the method is static
   * @param classType the JVM type of the class the method belongs to
   * @param signature the JVM signature of the method
   * @param instructions the ShrikeBT instructions
   * @param handlers the ShrikeBT exception handlers
   * @param instructionsToBytecodes the map from instructions to original bytecode offsets
   * @throws IllegalArgumentException if handlers is null
   * @throws IllegalArgumentException if instructions is null
   * @throws IllegalArgumentException if instructionsToBytecodes is null
   */
  public Compiler(boolean isConstructor, boolean isStatic, String classType, String signature, IInstruction[] instructions, ExceptionHandler[][] handlers,
      int[] instructionsToBytecodes) {
    if (instructionsToBytecodes == null) {
      throw new IllegalArgumentException("instructionsToBytecodes is null");
    }
    if (instructions == null) {
      throw new IllegalArgumentException("instructions is null");
    }
    if (handlers == null) {
      throw new IllegalArgumentException("handlers is null");
    }
    if (instructions.length != handlers.length) {
      throw new IllegalArgumentException("Instructions/handlers length mismatch");
    }
    if (instructions.length != instructionsToBytecodes.length) {
      throw new IllegalArgumentException("Instructions/handlers length mismatch");
    }

    this.isConstructor = isConstructor;
    this.isStatic = isStatic;
    this.classType = classType;
    this.signature = signature;
    this.instructions = instructions;
    this.handlers = handlers;
    this.instructionsToBytecodes = instructionsToBytecodes;
  }

  /**
   * Extract the data for the method to be compiled from the MethodData container.
   */
  protected Compiler(MethodData info) {
    this(info.getName().equals("<init>"), info.getIsStatic(), info.getClassType(), info.getSignature(), info.getInstructions(), info.getHandlers(), info
        .getInstructionsToBytecodes());
  }

  /**
   * @return the JVM type for the class this method belongs to
   */
  final public String getClassType() {
    return classType;
  }

  /**
   * Notify the compiler that the constants appearing in the ConstantPoolReader cp will appear in the final class file.
   * 
   * Instructions which were extracted from a class file with the same ConstantPoolReader can be written back much more efficiently
   * if the same constant pool indices are valid in the new class file.
   */
  final public void setPresetConstants(ConstantPoolReader cp) {
    presetConstants = cp;
  }

  final public void setClassHierarchy(ClassHierarchyProvider h) {
    this.hierarchy = h;
  }

  protected abstract int allocateConstantPoolInteger(int v);

  protected abstract int allocateConstantPoolFloat(float v);

  protected abstract int allocateConstantPoolLong(long v);

  protected abstract int allocateConstantPoolDouble(double v);

  protected abstract int allocateConstantPoolString(String v);

  protected abstract int allocateConstantPoolClassType(String c);

  protected abstract int allocateConstantPoolMethodType(String c);

  protected abstract int allocateConstantPoolMethodHandle(ReferenceToken c);

  protected abstract int allocateConstantPoolField(String c, String name, String type);

  protected abstract int allocateConstantPoolMethod(String c, String name, String sig);

  protected abstract int allocateConstantPoolInterfaceMethod(String c, String name, String sig);

  protected abstract int allocateConstantPoolInvokeDynamic(BootstrapMethod b, String name, String type);

  protected abstract String createHelperMethod(boolean isStatic, String sig);
  
  private void collectInstructionInfo() {
    final BitSet s = new BitSet(instructions.length);
    final BitSet localsUsed = new BitSet(32);
    final BitSet localsWide = new BitSet(32);

    IInstruction.Visitor visitor = new IInstruction.Visitor() {
      private void visitTargets(IInstruction instr) {
        int[] ts = instr.getBranchTargets();
        for (int element : ts) {
          s.set(element);
        }
      }

      @Override
      public void visitGoto(GotoInstruction instruction) {
        visitTargets(instruction);
      }

      @Override
      public void visitLocalStore(IStoreInstruction instruction) {
        localsUsed.set(instruction.getVarIndex());
        String t = instruction.getType();
        if (t.equals(TYPE_long) || t.equals(TYPE_double)) {
          localsWide.set(instruction.getVarIndex());
        }
      }

      @Override
      public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
        visitTargets(instruction);
      }

      @Override
      public void visitSwitch(SwitchInstruction instruction) {
        visitTargets(instruction);
      }
    };

    for (IInstruction instruction : instructions) {
      instruction.visit(visitor);
    }

    String[] paramTypes = Util.getParamsTypes(isStatic ? null : TYPE_Object, signature);
    int index = 0;
    for (String t : paramTypes) {
      localsUsed.set(index);
      if (t.equals(TYPE_long) || t.equals(TYPE_double)) {
        localsWide.set(index);
        index += 2;
      } else {
        index++;
      }
    }

    ExceptionHandler[] lastHS = null;
    for (ExceptionHandler[] hs : handlers) {
      if (hs != lastHS) {
        for (ExceptionHandler element : hs) {
          s.set(element.handler);
        }
        lastHS = hs;
      }
    }

    this.branchTargets = s;

    int maxUsed = localsUsed.length();
    if (maxUsed > 0 && localsWide.get(maxUsed - 1)) {
      maxUsed++;
    }
    this.maxLocals = maxUsed;
  }

  private void writeInt(int offset, int v) {
    code[offset] = (byte) (v >> 24);
    code[offset + 1] = (byte) (v >> 16);
    code[offset + 2] = (byte) (v >> 8);
    code[offset + 3] = (byte) v;
  }

  private void writeShort(int offset, int v) {
    code[offset] = (byte) (v >> 8);
    code[offset + 1] = (byte) v;
  }

  private void writeByte(int offset, int v) {
    code[offset] = (byte) v;
  }

  private boolean inBasicBlock(int i, int n) {
    if (i + n - 1 >= instructions.length) {
      return false;
    }

    for (int j = i + 1; j < i + n; j++) {
      if (branchTargets.get(j)) {
        return false;
      }

      if (!Arrays.equals(handlers[j], handlers[i])) {
        return false;
      }

      if (instructionsToBytecodes[j] != instructionsToBytecodes[i]) {
        return false;
      }
    }

    return true;
  }

  private void checkStackWordSize(byte[] stackWords, int stackLen) {
    if (stackLen * 2 > maxStack) {
      int words = 0;
      for (int i = 0; i < stackLen; i++) {
        words += stackWords[i];
      }
      if (words > maxStack) {
        maxStack = words;
      }
    }
  }

  // private static int getStackDelta(Instruction instr) {
  // if (instr instanceof DupInstruction) {
  // return ((DupInstruction) instr).getSize();
  // } else {
  // return (instr.getPushedWordSize() > 0 ? 1 : 0) - instr.getPoppedCount();
  // }
  // }

  private void computeStackWordsAt(int i, int stackLen, byte[] stackWords, boolean[] visited) {
    while (!visited[i]) {
      IInstruction instr = instructions[i];

      if (i > 0 && !instructions[i - 1].isFallThrough()) {
        byte[] newWords = new byte[stackLen];
        System.arraycopy(stackWords, 0, newWords, 0, stackLen);
        this.stackWords[i] = newWords;
      }

      visited[i] = true;

      if (stackLen < instr.getPoppedCount()) {
        throw new IllegalArgumentException("Stack underflow in intermediate code, at offset " + i);
      }

      if (instr instanceof DupInstruction) {
        DupInstruction d = (DupInstruction) instr;
        int size = d.getSize();
        int delta = d.getDelta();

        System.arraycopy(stackWords, stackLen - size - delta, stackWords, stackLen - delta, delta + size);
        System.arraycopy(stackWords, stackLen, stackWords, stackLen - size - delta, size);
        stackLen += size;
        checkStackWordSize(stackWords, stackLen);
      } else if (instr instanceof SwapInstruction) {
        // we may have to emulate this using a dup[2]_x[1,2]
        // followed by a pop[2]. So update the maxStack to account for the
        // temporarily larger stack size
        int words = stackWords[stackLen - 1];
        for (int j = 0; j < stackLen; j++) {
          words += stackWords[j];
        }
        if (words > maxStack) {
          maxStack = words;
        }

        byte b = stackWords[stackLen - 2];
        stackWords[stackLen - 2] = stackWords[stackLen - 1];
        stackWords[stackLen - 1] = b;
      } else {
        stackLen -= instr.getPoppedCount();

        byte w = instr.getPushedWordSize();
        if (w > 0) {
          stackWords[stackLen] = w;
          stackLen++;
          checkStackWordSize(stackWords, stackLen);
        }
      }

      int[] bt = instr.getBranchTargets();
      for (int element : bt) {
        int t = element;
        if (t < 0 || t >= visited.length) {
          throw new IllegalArgumentException("Branch target at offset " + i + " is out of bounds: " + t + " (max " + visited.length
              + ")");
        }
        if (!visited[t]) {
          computeStackWordsAt(element, stackLen, stackWords.clone(), visited);
        }
      }

      ExceptionHandler[] hs = handlers[i];
      for (ExceptionHandler element : hs) {
        int t = element.handler;
        if (!visited[t]) {
          byte[] newWords = stackWords.clone();
          newWords[0] = 1;
          computeStackWordsAt(t, 1, newWords, visited);
        }
      }

      if (!instr.isFallThrough()) {
        return;
      }

      i++;
    }
  }

  private void computeStackWords() {
    stackWords = new byte[instructions.length][];
    maxStack = 0;

    computeStackWordsAt(0, 0, new byte[instructions.length * 2], new boolean[instructions.length]);
  }

  abstract class Patch {
    final int instrStart;

    final int instrOffset;

    final int targetLabel;

    Patch(int instrStart, int instrOffset, int targetLabel) {
      this.instrStart = instrStart;
      this.instrOffset = instrOffset;
      this.targetLabel = targetLabel;
    }

    abstract boolean apply();
  }

  class ShortPatch extends Patch {
    ShortPatch(int instrStart, int instrOffset, int targetLabel) {
      super(instrStart, instrOffset, targetLabel);
    }

    @Override
    boolean apply() {
      int delta = instructionsToOffsets[targetLabel] - instrStart;
      if ((short) delta == delta) {
        writeShort(instrOffset, delta);
        return true;
      } else {
        return false;
      }
    }
  }

  class IntPatch extends Patch {
    IntPatch(int instrStart, int instrOffset, int targetLabel) {
      super(instrStart, instrOffset, targetLabel);
    }

    @Override
    boolean apply() {
      writeInt(instrOffset, instructionsToOffsets[targetLabel] - instrStart);
      return true;
    }
  }

  private void insertBranchOffsetInt(ArrayList<Patch> patches, int instrStart, int instrOffset, int targetLabel) {
    if (instructionsToOffsets[targetLabel] > 0 || targetLabel == 0) {
      writeInt(instrOffset, instructionsToOffsets[targetLabel] - instrStart);
    } else {
      patches.add(new IntPatch(instrStart, instrOffset, targetLabel));
    }
  }

  private static boolean applyPatches(ArrayList<Patch> patches) {
    for (Patch p : patches) {
      if (!p.apply()) {
        return false;
      }
    }
    return true;
  }

  private static byte[] cachedBuf;

  private static synchronized byte[] makeCodeBuf() {
    if (cachedBuf != null) {
      byte[] result = cachedBuf;
      cachedBuf = null;
      return result;
    } else {
      return new byte[65535];
    }
  }

  private static synchronized void releaseCodeBuf(byte[] buf) {
    cachedBuf = buf;
  }

  private boolean outputInstructions(int startInstruction, int endInstruction, int startOffset, boolean farBranches,
      byte[] initialStack) {
    instructionsToOffsets = new int[instructions.length];
    code = makeCodeBuf();

    ArrayList<Patch> patches = new ArrayList<>();

    int curOffset = startOffset;
    final int[] curOffsetRef = new int[1];
    int stackLen = initialStack == null ? 0 : initialStack.length;
    final int[] stackLenRef = new int[1];
    final byte[] stackWords = new byte[maxStack];
    if (stackLen > 0) {
      System.arraycopy(initialStack, 0, stackWords, 0, stackLen);
    }
    final int[] instrRef = new int[1];

    IInstruction.Visitor noOpcodeHandler = new IInstruction.Visitor() {
      @Override
      public void visitPop(PopInstruction instruction) {
        int count = instruction.getPoppedCount();
        int offset = curOffsetRef[0];
        int stackLen = stackLenRef[0];

        while (count > 0) {
          code[offset] = (byte) (stackWords[stackLen - 1] == 1 ? OP_pop : OP_pop2);
          count--;
          stackLen--;
          offset++;
        }

        curOffsetRef[0] = offset;
      }

      @Override
      public void visitDup(DupInstruction instruction) {
        int size = instruction.getSize();
        int delta = instruction.getDelta();
        int offset = curOffsetRef[0];
        int stackLen = stackLenRef[0];

        int sizeWords = stackWords[stackLen - 1];
        if (size == 2) {
          sizeWords += stackWords[stackLen - 2];
        }
        int deltaWords = delta == 0 ? 0 : stackWords[stackLen - 1 - size];
        if (delta == 2) {
          deltaWords += stackWords[stackLen - 1 - size - 1];
        }
        if (sizeWords > 2 || deltaWords > 2) {
          throw new IllegalArgumentException("Invalid dup size");
        }

        code[offset] = (byte) (OP_dup + (3 * (sizeWords - 1)) + deltaWords);
        offset++;
        curOffsetRef[0] = offset;
      }

      @Override
      public void visitSwap(SwapInstruction instruction) {
        int offset = curOffsetRef[0];
        int stackLen = stackLenRef[0];
        int topSize = stackWords[stackLen - 1];
        int nextSize = stackWords[stackLen - 2];

        if (topSize == 1 && nextSize == 1) {
          code[offset] = (byte) OP_swap;
          offset++;
        } else {
          code[offset] = (byte) (OP_dup + (3 * (topSize - 1)) + nextSize);
          code[offset + 1] = (byte) (topSize == 1 ? OP_pop : OP_pop2);
          offset += 2;
        }

        curOffsetRef[0] = offset;
      }
    };

    for (int i = startInstruction; i < endInstruction; i++) {
      Instruction instr = (Instruction) instructions[i];
      int opcode = instr.getOpcode();
      int startI = i;

      instructionsToOffsets[i] = curOffset;

      if (opcode != -1) {
        boolean fallToConditional = false;

        code[curOffset] = (byte) opcode;
        curOffset++;

        switch (opcode) {
        case OP_iconst_0:
          if (inBasicBlock(i, 2) && instructions[i + 1] instanceof ConditionalBranchInstruction) {
            ConditionalBranchInstruction cbr = (ConditionalBranchInstruction) instructions[i + 1];
            if (cbr.getType().equals(TYPE_int)) {
              code[curOffset - 1] = (byte) (cbr.getOperator().ordinal() + OP_ifeq);
              fallToConditional = true;
              i++;
              instr = (Instruction) instructions[i];
            }
          }
          if (!fallToConditional) {
            break;
          }
          //$FALL-THROUGH$
        case OP_aconst_null:
          if (!fallToConditional && inBasicBlock(i, 2) && instructions[i + 1] instanceof ConditionalBranchInstruction) {
            ConditionalBranchInstruction cbr = (ConditionalBranchInstruction) instructions[i + 1];
            if (cbr.getType().equals(TYPE_Object)) {
              code[curOffset - 1] = (byte) (cbr.getOperator().ordinal() + OP_ifnull);
              fallToConditional = true;
              i++;
              instr = (Instruction) instructions[i];
            }
          }
          if (!fallToConditional) {
            break;
          }
          // by Xiangyu
          //$FALL-THROUGH$
        case OP_ifeq:
        case OP_ifge:
        case OP_ifgt:
        case OP_ifle:
        case OP_iflt:
        case OP_ifne: {
          int targetI = instr.getBranchTargets()[0];
          boolean invert = false;
          int iStart = curOffset - 1;

          if (inBasicBlock(i, 2) && instr.getBranchTargets()[0] == i + 2 && instructions[i + 1] instanceof GotoInstruction) {
            invert = true;
            targetI = instructions[i + 1].getBranchTargets()[0];
            i++;
          }

          if (targetI <= i) {
            int delta = instructionsToOffsets[targetI] - iStart;
            if ((short) delta != delta) {
              // emit "if_!XX TMP; goto_w L; TMP:"
              invert = !invert;
              writeShort(curOffset, 8);
              code[curOffset + 2] = (byte) OP_goto_w;
              writeInt(curOffset + 3, delta - 3);
              curOffset += 7;
            } else {
              writeShort(curOffset, (short) delta);
              curOffset += 2;
            }
          } else {
            Patch p;
            if (farBranches) {
              // emit "if_!XX TMP; goto_w L; TMP:"
              invert = !invert;
              writeShort(curOffset, 8);
              code[curOffset + 2] = (byte) OP_goto_w;
              p = new IntPatch(curOffset + 2, curOffset + 3, targetI);
              curOffset += 7;
            } else {
              p = new ShortPatch(iStart, curOffset, targetI);
              curOffset += 2;
            }
            patches.add(p);
          }

          if (invert) {
            code[iStart] = (byte) (((code[iStart] - OP_ifeq) ^ 1) + OP_ifeq);
          }
          break;
        }
          // by Xiangyu

        case OP_if_icmpeq:
        case OP_if_icmpge:
        case OP_if_icmpgt:
        case OP_if_icmple:
        case OP_if_icmplt:
        case OP_if_icmpne:
        case OP_if_acmpeq:
        case OP_if_acmpne: {
          int targetI = instr.getBranchTargets()[0];
          boolean invert = false;
          int iStart = curOffset - 1;

          if (inBasicBlock(i, 2) && instr.getBranchTargets()[0] == i + 2 && instructions[i + 1] instanceof GotoInstruction) {
            invert = true;
            targetI = instructions[i + 1].getBranchTargets()[0];
            i++;
          }

          if (targetI <= i) {
            int delta = instructionsToOffsets[targetI] - iStart;
            if ((short) delta != delta) {
              // emit "if_!XX TMP; goto_w L; TMP:"
              invert = !invert;
              writeShort(curOffset, 8);
              code[curOffset + 2] = (byte) OP_goto_w;
              writeInt(curOffset + 3, delta - 3);
              curOffset += 7;
            } else {
              writeShort(curOffset, (short) delta);
              curOffset += 2;
            }
          } else {
            Patch p;
            if (farBranches) {
              // emit "if_!XX TMP; goto_w L; TMP:"
              invert = !invert;
              writeShort(curOffset, 8);
              code[curOffset + 2] = (byte) OP_goto_w;
              p = new IntPatch(curOffset + 2, curOffset + 3, targetI);
              curOffset += 7;
            } else {
              p = new ShortPatch(iStart, curOffset, targetI);
              curOffset += 2;
            }
            patches.add(p);
          }

          if (invert) {
            code[iStart] = (byte) (((code[iStart] - OP_if_icmpeq) ^ 1) + OP_if_icmpeq);
          }
          break;
        }
        case OP_bipush:
          writeByte(curOffset, ((ConstantInstruction.ConstInt) instr).getIntValue());
          curOffset++;
          break;
        case OP_sipush:
          writeShort(curOffset, ((ConstantInstruction.ConstInt) instr).getIntValue());
          curOffset += 2;
          break;
        case OP_ldc_w: {
          int cpIndex;
          ConstantInstruction ci = (ConstantInstruction) instr;
          if (presetConstants != null && ci.getLazyConstantPool() == presetConstants) {
            cpIndex = ci.getCPIndex();
          } else {
            String t = instr.getPushedType(null);
            if (t.equals(TYPE_int)) {
              cpIndex = allocateConstantPoolInteger(((ConstantInstruction.ConstInt) instr).getIntValue());
            } else if (t.equals(TYPE_String)) {
              cpIndex = allocateConstantPoolString((String) ((ConstantInstruction.ConstString) instr).getValue());
            } else if (t.equals(TYPE_Class)) {
              cpIndex = allocateConstantPoolClassType(((ClassToken) ((ConstantInstruction.ConstClass) instr).getValue()).getTypeName());
            } else if (t.equals(TYPE_MethodType)) {
              cpIndex = allocateConstantPoolMethodType(((String) ((ConstantInstruction.ConstMethodType) instr).getValue()));
            } else if (t.equals(TYPE_MethodHandle)) {
              cpIndex = allocateConstantPoolMethodHandle(((ReferenceToken) ((ConstantInstruction.ConstMethodHandle) instr).getValue()));
            } else {
              cpIndex = allocateConstantPoolFloat(((ConstantInstruction.ConstFloat) instr).getFloatValue());
            }
          }

          if (cpIndex < 256) {
            code[curOffset - 1] = (byte) OP_ldc;
            code[curOffset] = (byte) cpIndex;
            curOffset++;
          } else {
            writeShort(curOffset, cpIndex);
            curOffset += 2;
          }
          break;
        }
        case OP_ldc2_w: {
          int cpIndex;
          ConstantInstruction ci = (ConstantInstruction) instr;
          if (presetConstants != null && ci.getLazyConstantPool() == presetConstants) {
            cpIndex = ci.getCPIndex();
          } else {
            String t = instr.getPushedType(null);
            if (t.equals(TYPE_long)) {
              cpIndex = allocateConstantPoolLong(((ConstantInstruction.ConstLong) instr).getLongValue());
            } else {
              cpIndex = allocateConstantPoolDouble(((ConstantInstruction.ConstDouble) instr).getDoubleValue());
            }
          }

          writeShort(curOffset, cpIndex);
          curOffset += 2;
          break;
        }
        case OP_iload_0:
        case OP_iload_1:
        case OP_iload_2:
        case OP_iload_3:
        case OP_iload: {
          if (inBasicBlock(i, 4)) {
            // try to generate an OP_iinc
            if (instructions[i + 1] instanceof ConstantInstruction.ConstInt && instructions[i + 2] instanceof BinaryOpInstruction
                && instructions[i + 3] instanceof StoreInstruction) {
              LoadInstruction i0 = (LoadInstruction) instr;
              ConstantInstruction.ConstInt i1 = (ConstantInstruction.ConstInt) instructions[i + 1];
              BinaryOpInstruction i2 = (BinaryOpInstruction) instructions[i + 2];
              StoreInstruction i3 = (StoreInstruction) instructions[i + 3];

              int c = i1.getIntValue();
              int v = i0.getVarIndex();
              BinaryOpInstruction.Operator op = i2.getOperator();
              if ((short) c == c && i3.getVarIndex() == v && (op == Operator.ADD || op == Operator.SUB)
                  && i2.getType().equals(TYPE_int) && i3.getType().equals(TYPE_int)) {
                if (v < 256 && (byte) c == c) {
                  code[curOffset - 1] = (byte) OP_iinc;
                  writeByte(curOffset, v);
                  writeByte(curOffset + 1, c);
                  curOffset += 2;
                } else {
                  code[curOffset - 1] = (byte) OP_wide;
                  code[curOffset] = (byte) OP_iinc;
                  writeShort(curOffset + 1, v);
                  writeShort(curOffset + 3, c);
                  curOffset += 5;
                }
                instructionsToOffsets[i + 1] = -1;
                instructionsToOffsets[i + 2] = -1;
                instructionsToOffsets[i + 3] = -1;
                i += 3;
                break;
              }
            }
          }
          if (opcode != OP_iload) {
            break;
          }
        }
          //$FALL-THROUGH$
        case OP_lload:
        case OP_fload:
        case OP_dload:
        case OP_aload: {
          int v = ((LoadInstruction) instr).getVarIndex();

          if (v < 256) {
            writeByte(curOffset, v);
            curOffset++;
          } else {
            code[curOffset - 1] = (byte) OP_wide;
            code[curOffset] = (byte) opcode;
            writeShort(curOffset + 1, v);
            curOffset += 3;
          }
          break;
        }
        case OP_istore:
        case OP_lstore:
        case OP_fstore:
        case OP_dstore:
        case OP_astore: {
          int v = ((StoreInstruction) instr).getVarIndex();

          if (v < 256) {
            writeByte(curOffset, v);
            curOffset++;
          } else {
            code[curOffset - 1] = (byte) OP_wide;
            code[curOffset] = (byte) opcode;
            writeShort(curOffset + 1, v);
            curOffset += 3;
          }
          break;
        }
        case OP_goto: {
          int targetI = instr.getBranchTargets()[0];
          if (targetI <= i) {
            int delta = instructionsToOffsets[targetI] - (curOffset - 1);
            if ((short) delta != delta) {
              code[curOffset - 1] = (byte) OP_goto_w;
              writeInt(curOffset, delta);
              curOffset += 4;
            } else {
              writeShort(curOffset, (short) delta);
              curOffset += 2;
            }
          } else if (targetI == i + 1) {
            // ignore noop gotos
            curOffset--;
          } else {
            Patch p;
            if (farBranches) {
              code[curOffset - 1] = (byte) OP_goto_w;
              p = new IntPatch(curOffset - 1, curOffset, instr.getBranchTargets()[0]);
              curOffset += 4;
            } else {
              p = new ShortPatch(curOffset - 1, curOffset, instr.getBranchTargets()[0]);
              curOffset += 2;
            }
            patches.add(p);
          }
          break;
        }
        case OP_lookupswitch: {
          int start = curOffset - 1;
          SwitchInstruction sw = (SwitchInstruction) instr;
          int[] casesAndLabels = sw.getCasesAndLabels();

          while ((curOffset & 3) != 0) {
            writeByte(curOffset, 0);
            curOffset++;
          }

          if (curOffset + 4 * casesAndLabels.length + 8 > code.length) {
            return false;
          }
          insertBranchOffsetInt(patches, start, curOffset, sw.getDefaultLabel());
          writeInt(curOffset + 4, casesAndLabels.length / 2);
          curOffset += 8;
          for (int j = 0; j < casesAndLabels.length; j += 2) {
            writeInt(curOffset, casesAndLabels[j]);
            insertBranchOffsetInt(patches, start, curOffset + 4, casesAndLabels[j + 1]);
            curOffset += 8;
          }
          break;
        }
        case OP_tableswitch: {
          int start = curOffset - 1;
          SwitchInstruction sw = (SwitchInstruction) instr;
          int[] casesAndLabels = sw.getCasesAndLabels();

          while ((curOffset & 3) != 0) {
            writeByte(curOffset, 0);
            curOffset++;
          }
          if (curOffset + 2 * casesAndLabels.length + 12 > code.length) {
            return false;
          }
          insertBranchOffsetInt(patches, start, curOffset, sw.getDefaultLabel());
          writeInt(curOffset + 4, casesAndLabels[0]);
          writeInt(curOffset + 8, casesAndLabels[casesAndLabels.length - 2]);
          curOffset += 12;
          for (int j = 0; j < casesAndLabels.length; j += 2) {
            insertBranchOffsetInt(patches, start, curOffset, casesAndLabels[j + 1]);
            curOffset += 4;
          }
          break;
        }
        case OP_getfield:
        case OP_getstatic: {
          GetInstruction g = (GetInstruction) instr;
          int cpIndex;

          if (presetConstants != null && presetConstants == g.getLazyConstantPool()) {
            cpIndex = ((GetInstruction.Lazy) g).getCPIndex();
          } else {
            cpIndex = allocateConstantPoolField(g.getClassType(), g.getFieldName(), g.getFieldType());
          }
          writeShort(curOffset, cpIndex);
          curOffset += 2;
          break;
        }
        case OP_putfield:
        case OP_putstatic: {
          PutInstruction p = (PutInstruction) instr;
          int cpIndex;

          if (presetConstants != null && presetConstants == p.getLazyConstantPool()) {
            cpIndex = ((PutInstruction.Lazy) p).getCPIndex();
          } else {
            cpIndex = allocateConstantPoolField(p.getClassType(), p.getFieldName(), p.getFieldType());
          }
          writeShort(curOffset, cpIndex);
          curOffset += 2;
          break;
        }
        case OP_invokespecial:
        case OP_invokestatic:
        case OP_invokevirtual: {
          InvokeInstruction inv = (InvokeInstruction) instr;
          int cpIndex;

          if (presetConstants != null && presetConstants == inv.getLazyConstantPool()) {
            cpIndex = ((InvokeInstruction.Lazy) inv).getCPIndex();
          } else {
            cpIndex = allocateConstantPoolMethod(inv.getClassType(), inv.getMethodName(), inv.getMethodSignature());
          }
          writeShort(curOffset, cpIndex);
          curOffset += 2;
          break;
        }
        case OP_invokedynamic: {
          InvokeDynamicInstruction inv = (InvokeDynamicInstruction) instr;
          
          int cpIndex;
          if (presetConstants != null && presetConstants == inv.getLazyConstantPool()) {
            cpIndex = ((InvokeDynamicInstruction.Lazy) inv).getCPIndex();
          } else {
            cpIndex = allocateConstantPoolInvokeDynamic(inv.getBootstrap(), inv.getMethodName(), inv.getMethodSignature());
          }

          writeShort(curOffset, cpIndex);
          code[curOffset + 2] = 0;
          code[curOffset + 3] = 0;
          curOffset += 4;
          break;
        }
        case OP_invokeinterface: {
          InvokeInstruction inv = (InvokeInstruction) instr;
          String sig = inv.getMethodSignature();
          int cpIndex;

          if (presetConstants != null && presetConstants == inv.getLazyConstantPool()) {
            cpIndex = ((InvokeInstruction.Lazy) inv).getCPIndex();
          } else {
            cpIndex = allocateConstantPoolInterfaceMethod(inv.getClassType(), inv.getMethodName(), sig);
          }
          writeShort(curOffset, cpIndex);
          code[curOffset + 2] = (byte) (Util.getParamsWordSize(sig) + 1);
          code[curOffset + 3] = 0;
          curOffset += 4;
          break;
        }
       case OP_new:
          writeShort(curOffset, allocateConstantPoolClassType(((NewInstruction) instr).getType()));
          curOffset += 2;
          break;
        case OP_newarray:
          code[curOffset] = indexedTypes_T[Util.getTypeIndex(((NewInstruction) instr).getType().substring(1))];
          curOffset++;
          break;
        case OP_anewarray:
          writeShort(curOffset, allocateConstantPoolClassType(((NewInstruction) instr).getType().substring(1)));
          curOffset += 2;
          break;
        case OP_multianewarray: {
          NewInstruction n = (NewInstruction) instr;
          writeShort(curOffset, allocateConstantPoolClassType(n.getType()));
          code[curOffset + 2] = (byte) n.getArrayBoundsCount();
          curOffset += 3;
          break;
        }
        case OP_checkcast:
          writeShort(curOffset, allocateConstantPoolClassType(((CheckCastInstruction) instr).getTypes()[0]));
          curOffset += 2;
          break;
        case OP_instanceof:
          writeShort(curOffset, allocateConstantPoolClassType(((InstanceofInstruction) instr).getType()));
          curOffset += 2;
          break;
        default:
          // do nothing
        }
      } else {
        stackLenRef[0] = stackLen;
        curOffsetRef[0] = curOffset;
        instrRef[0] = i;
        instr.visit(noOpcodeHandler);
        curOffset = curOffsetRef[0];
        i = instrRef[0];
      }

      boolean haveStack = true;

      while (startI <= i) {
        instr = (Instruction) instructions[startI];
        if (instr.isFallThrough() && haveStack) {
          if (stackLen < instr.getPoppedCount()) {
            throw new IllegalArgumentException("Stack underflow in intermediate code, at offset " + startI);
          }

          if (instr instanceof DupInstruction) {
            DupInstruction d = (DupInstruction) instr;
            int size = d.getSize();
            int delta = d.getDelta();

            System.arraycopy(stackWords, stackLen - size - delta, stackWords, stackLen - delta, delta + size);
            System.arraycopy(stackWords, stackLen, stackWords, stackLen - size - delta, size);
            stackLen += size;
          } else if (instr instanceof SwapInstruction) {
            byte b = stackWords[stackLen - 1];
            stackWords[stackLen - 1] = stackWords[stackLen - 2];
            stackWords[stackLen - 2] = b;
          } else {
            stackLen -= instr.getPoppedCount();

            byte w = instr.getPushedWordSize();
            if (w > 0) {
              stackWords[stackLen] = w;
              stackLen++;
            }
          }
        } else {
          // No stack, or the instruction doesn't fall through
          // try to grab the stack state at the start of the next instruction
          if (startI + 1 < endInstruction) {
            byte[] s = this.stackWords[startI + 1];
            // if the next instruction doesn't have stack info (it's not
            // reachable), then just ignore it and remember that we don't have
            // stack info
            if (s == null) {
              haveStack = false;
            } else {
              stackLen = s.length;
              System.arraycopy(s, 0, stackWords, 0, stackLen);
            }
          }
        }

        startI++;
      }

      if (curOffset > code.length - 8) {
        return false;
      }

      if (!haveStack) {
        // skip forward through the unreachable instructions until we find
        // an instruction for which we know the stack state
        while (i + 1 < endInstruction) {
          byte[] s = this.stackWords[i + 1];
          if (s != null) {
            stackLen = s.length;
            System.arraycopy(s, 0, stackWords, 0, stackLen);
            break;
          }
          i++;
        }
      }
    }

    if (applyPatches(patches)) {
      byte[] newCode = new byte[curOffset];
      System.arraycopy(code, 0, newCode, 0, curOffset);
      releaseCodeBuf(code);
      code = newCode;
    } else {
      if (farBranches) {
        throw new Error("Failed to apply patches even with farBranches on");
      } else {
        return outputInstructions(startInstruction, endInstruction, startOffset, true, initialStack);
      }
    }

    return true;
  }

  private int[] buildRawHandlers(int start, int end) {
    int[] handlerCounts = new int[end - start];
    int maxCount = 0;

    for (int i = start; i < end; i++) {
      int len = handlers[i].length;
      handlerCounts[i - start] = len;
      if (len > maxCount) {
        maxCount = len;
      }
    }

    if (maxCount == 0) {
      return noRawHandlers;
    } else {
      ArrayList<int[]> rawHandlerList = new ArrayList<>();

      for (int i = maxCount; i > 0; i--) {
        for (int j = start; j < end; j++) {
          if (handlerCounts[j - start] == i) {
            int first = j;
            ExceptionHandler h = handlers[j][handlers[j].length - i];

            do {
              handlerCounts[j - start]--;
              j++;
            } while (j < end && handlerCounts[j - start] == i && handlers[j][handlers[j].length - i].equals(h));

            if (h.handler >= start && h.handler < end) {
              rawHandlerList.add(new int[] { instructionsToOffsets[first], j < end ? instructionsToOffsets[j] : code.length,
                  instructionsToOffsets[h.handler], h.catchClass == null ? 0 : allocateConstantPoolClassType(h.catchClass) });
            }

            j--;
          }
        }
      }

      int[] rawHandlers = new int[4 * rawHandlerList.size()];
      int count = 0;
      for (int[] element : rawHandlerList) {
        System.arraycopy(element, 0, rawHandlers, count, 4);
        count += 4;
      }
      return rawHandlers;
    }
  }

  private int[] buildBytecodeMap(int start, int end) {
    int[] r = new int[code.length];

    for (int i = 0; i < r.length; i++) {
      r[i] = -1;
    }

    for (int i = start; i < end; i++) {
      int off = instructionsToOffsets[i];
      if (off >= 0) {
        r[off] = instructionsToBytecodes[i];
      }
    }

    return r;
  }

  static class HelperPatch {
    final int start;

    final int length;

    final Instruction[] code;

    final ExceptionHandler[] handlers;

    HelperPatch(int start, int length, Instruction[] code, ExceptionHandler[] handlers) {
      this.start = start;
      this.length = length;
      this.code = code;
      this.handlers = handlers;
    }
  }

  private void addBackEdge(int from, int to) {
    int[] oldEdges = backEdges[from];
    if (oldEdges == null) {
      backEdges[from] = new int[] { to };
    } else if (oldEdges[oldEdges.length - 1] < 0) {
      int left = 1;
      int right = oldEdges.length - 1;
      while (true) {
        if (right - left < 2) {
          if (oldEdges[left] < 0) {
            break;
          } else {
            if (oldEdges[right] >= 0)
              throw new Error("Failed binary search");
            left = right;
            break;
          }
        } else {
          int mid = (left + right) / 2;
          if (oldEdges[mid] < 0) {
            right = mid;
          } else {
            left = mid + 1;
          }
        }
      }
      oldEdges[left] = to;
    } else {
      int[] newEdges = new int[oldEdges.length * 2];
      System.arraycopy(oldEdges, 0, newEdges, 0, oldEdges.length);
      newEdges[oldEdges.length] = to;
      for (int i = oldEdges.length + 1; i < newEdges.length; i++) {
        newEdges[i] = -1;
      }
      backEdges[from] = newEdges;
    }
  }

  private void addLiveVar(int instruction, int index) {
    while (true) {
      if (liveLocals[instruction].get(index)) {
        break;
      }

      IInstruction instr = instructions[instruction];
      if (instr instanceof StoreInstruction && ((StoreInstruction) instr).getVarIndex() == index) {
        break;
      }

      liveLocals[instruction].set(index);
      int[] back = backEdges[instruction];
      if (back != null) {
        for (int element : back) {
          addLiveVar(element, index);
        }
      }

      if (instruction > 0 && instructions[instruction - 1].isFallThrough()) {
        instruction--;
      } else {
        break;
      }
    }
  }

  private void makeLiveLocals() {
    liveLocals = new BitSet[instructions.length];
    backEdges = new int[instructions.length][];

    for (int i = 0; i < instructions.length; i++) {
      IInstruction instr = instructions[i];
      int[] targets = instr.getBranchTargets();
      for (int target : targets) {
        addBackEdge(target, i);
      }
      ExceptionHandler[] hs = handlers[i];
      for (ExceptionHandler element : hs) {
        addBackEdge(element.handler, i);
      }
      liveLocals[i] = new BitSet();
    }

    for (int i = 0; i < backEdges.length; i++) {
      int[] back = backEdges[i];
      if (back != null && back[back.length - 1] < 0) {
        int j = back.length;
        while (back[j - 1] < 0) {
          j--;
        }
        int[] newBack = new int[j];
        System.arraycopy(back, 0, newBack, 0, newBack.length);
        backEdges[i] = newBack;
      }
    }

    for (int i = 0; i < instructions.length; i++) {
      IInstruction instr = instructions[i];
      if (instr instanceof LoadInstruction) {
        addLiveVar(i, ((LoadInstruction) instr).getVarIndex());
      }
    }
  }

  private String getAndCheckLocalType(int i, int l) {
    String[] lts = localTypes[i];
    String t = TYPE_unknown;
    if (l < lts.length) {
      t = lts[l];
    }
    if (t.equals(TYPE_null) || t.equals(TYPE_unknown)) {
      throw new IllegalArgumentException("Cannot split oversized method because local " + l + " is undefined at " + i);
    }
    return t;
  }

  private void allocateLocals(int count) {
    if (maxLocals < allocatedLocals + count * 2) {
      maxLocals = allocatedLocals + count * 2;
    }
  }

  private HelperPatch makeHelperPatch(int start, int len, int retVar, int unreadStack, int untouchedStack) {
    String retType = retVar >= 0 ? getAndCheckLocalType(start + len, retVar) : "V";

    ArrayList<Instruction> callWrapper = new ArrayList<>();
    int curStackLen = stackTypes[start].length;

    StringBuffer sigBuf = new StringBuffer();
    sigBuf.append("(");
    // spill needed stack variables to allocated locals;
    allocateLocals(curStackLen - unreadStack);
    for (int i = curStackLen - 1; i >= unreadStack; i--) {
      if (i < untouchedStack) {
        callWrapper.add(DupInstruction.make(0));
      }
      callWrapper.add(StoreInstruction.make(stackTypes[start][i], allocatedLocals + 2 * (i - unreadStack)));
    }
    // push needed locals
    BitSet liveVars = liveLocals[start];
    for (int i = 0; i < liveVars.length(); i++) {
      if (liveVars.get(i)) {
        String t = getAndCheckLocalType(start, i);
        sigBuf.append(t);
        callWrapper.add(LoadInstruction.make(t, i));
        if (Util.getWordSize(t) > 1) {
          i++;
        }
      } else {
        // dummy
        sigBuf.append("I");
        callWrapper.add(ConstantInstruction.make(0));
      }
    }
    // push stack variables
    for (int i = unreadStack; i < curStackLen; i++) {
      callWrapper.add(LoadInstruction.make(stackTypes[start][i], allocatedLocals + 2 * (i - unreadStack)));
      sigBuf.append(stackTypes[start][i]);
      if (Util.getWordSize(stackTypes[start][i]) == 2) {
        sigBuf.append("I");
        callWrapper.add(ConstantInstruction.make(0));
      }
    }
    sigBuf.append(")");
    sigBuf.append(retType);
    String sig = sigBuf.toString();

    String name = createHelperMethod(true, sig);

    callWrapper.add(InvokeInstruction.make(sig, classType, name, IInvokeInstruction.Dispatch.STATIC));

    int savedMaxStack = maxStack;
    maxStack += curStackLen - unreadStack;

    int prefixLength = 4 * (curStackLen - unreadStack);
    byte[] initialStack = new byte[curStackLen - unreadStack];
    for (int i = 0; i < initialStack.length; i++) {
      initialStack[i] = Util.getWordSize(stackTypes[start][unreadStack + i]);
    }
    if (!outputInstructions(start, start + len, prefixLength, false, initialStack)) {
      throw new Error("Helper function is overlarge");
    }
    byte[] newCode = new byte[code.length + (retVar >= 0 ? 5 : 1)];
    for (int i = 0; i < curStackLen - unreadStack; i++) {
      int local = allocatedLocals + i * 2;
      newCode[i * 4] = (byte) OP_wide;
      newCode[i * 4 + 1] = (byte) LoadInstruction.make(stackTypes[start][i + unreadStack], 500).getOpcode();
      newCode[i * 4 + 2] = (byte) (local >> 8);
      newCode[i * 4 + 3] = (byte) local;
    }
    System.arraycopy(code, prefixLength, newCode, prefixLength, code.length - prefixLength);
    int suffixOffset = code.length;
    if (retVar >= 0) {
      newCode[suffixOffset] = (byte) OP_wide;
      newCode[suffixOffset + 1] = (byte) LoadInstruction.make(retType, 500).getOpcode();
      newCode[suffixOffset + 2] = (byte) (retVar >> 8);
      newCode[suffixOffset + 3] = (byte) retVar;
      newCode[suffixOffset + 4] = (byte) ReturnInstruction.make(retType).getOpcode();
      callWrapper.add(StoreInstruction.make(retType, retVar));
    } else {
      newCode[suffixOffset] = (byte) ReturnInstruction.make(TYPE_void).getOpcode();
    }

    if (callWrapper.size() > len) {
      return null;
    }

    int[] rawHandlers = buildRawHandlers(start, start + len);
    int[] bytecodeMap = buildBytecodeMap(start, start + len);

    auxMethods.add(new Output(name, sig, newCode, rawHandlers, bytecodeMap, maxLocals, maxStack, true, null));

    maxStack = savedMaxStack;

    Instruction[] patch = new Instruction[callWrapper.size()];
    callWrapper.toArray(patch);

    ExceptionHandler[] startHS = handlers[start];
    ArrayList<ExceptionHandler> newHS = new ArrayList<>();
    for (ExceptionHandler element : startHS) {
      int t = element.handler;
      if (t < start || t >= start + len) {
        newHS.add(element);
      }
    }
    ExceptionHandler[] patchHS = new ExceptionHandler[newHS.size()];
    newHS.toArray(patchHS);

    return new HelperPatch(start, len, patch, patchHS);
  }

  private HelperPatch findBlock(int start, int len) {
    while (len > 100) {
      // make sure there is at most one entry
      int lastInvalid = start - 1;
      for (int i = start + 1; i < start + len; i++) {
        int[] back = backEdges[i];
        boolean outsideBranch = false;
        for (int j = 0; back != null && j < back.length; j++) {
          if (back[j] < start || back[j] >= start + len) {
            outsideBranch = true;
          }
        }
        if (outsideBranch) {
          HelperPatch p = findBlock(lastInvalid + 1, i - lastInvalid - 1);
          if (p != null) {
            return p;
          }
          lastInvalid = i;
        }
      }
      if (lastInvalid >= start) {
        return null;
      }

      // make sure there is at most one exit (fall through at the end)
      if (!instructions[start + len - 1].isFallThrough()) {
        len--;
        continue;
      }
      lastInvalid = start - 1;
      for (int i = start; i < start + len; i++) {
        int[] targets = instructions[i].getBranchTargets();
        boolean outsideBranch = false;
        if (instructions[i] instanceof ReturnInstruction) {
          outsideBranch = true;
        }
        for (int target : targets) {
          if (target < start || target >= start + len) {
            outsideBranch = true;
          }
        }
        if (outsideBranch) {
          HelperPatch p = findBlock(lastInvalid + 1, i - lastInvalid - 1);
          if (p != null) {
            return p;
          }
          lastInvalid = i;
        }
      }
      if (lastInvalid >= start) {
        return null;
      }

      lastInvalid = start - 1;
      for (int i = start; i < start + len; i++) {
        boolean out = false;
        ExceptionHandler[] hs = handlers[i];
        for (ExceptionHandler element : hs) {
          int h = element.handler;
          if (h < start || h >= start + len) {
            out = true;
          }
        }
        int[] targets = instructions[i].getBranchTargets();
        for (int t : targets) {
          if (t < start || t >= start + len) {
            out = true;
          }
        }
        if (out) {
          HelperPatch p = findBlock(lastInvalid + 1, i - lastInvalid - 1);
          if (p != null) {
            return p;
          }
          lastInvalid = i;
        }
      }

      if (lastInvalid >= start) {
        return null;
      }

      if (stackTypes[start] == null) {
        while (stackTypes[start] == null && len > 0) {
          start++;
          len--;
        }
        continue;
      }

      // See how many stack elements at entry are still there, unchanged, at
      // exit
      int untouchedStack = Integer.MAX_VALUE;
      // See how many elements of that part of the stack are never even read
      int unreadStack = Integer.MAX_VALUE;
      for (int i = start; i < start + len; i++) {
        if (stackTypes[i] == null) {
          untouchedStack = 0;
          unreadStack = 0;
          break;
        }
        int lowWaterMark = stackTypes[i].length - instructions[i].getPoppedCount();
        unreadStack = Math.min(unreadStack, lowWaterMark);
        if (instructions[i] instanceof DupInstruction) {
          // dup instructions don't actually pop off/change the element they
          // duplicate
          lowWaterMark += instructions[i].getPoppedCount();
        }
        untouchedStack = Math.min(untouchedStack, lowWaterMark);
      }

      if (untouchedStack > unreadStack + 1 || (untouchedStack == unreadStack + 1 && untouchedStack < stackTypes[start].length)) {
        // we can only handle 1 read-but-untouched element
        start++;
        len--;
        continue;
      }

      // make sure we know the type of all the stack values that must be passed
      // in
      boolean unknownType = false;
      for (int i = unreadStack; i < untouchedStack; i++) {
        String t = stackTypes[start][i];
        if (t == null || t.equals(TYPE_unknown) || t.equals(TYPE_null)) {
          unknownType = true;
          break;
        }
      }
      if (unknownType) {
        start++;
        len--;
        continue;
      }

      // make sure outgoing stack size is no more than the untouched stack size.
      if (stackTypes[start + len] == null || stackTypes[start + len].length > untouchedStack) {
        // This is a little conservative. We might be able to stop sooner with a
        // valid
        // extractable method, because changing 'len' might mean we have more
        // untouched stack elements. But we'll do this in a dumb way to avoid
        // being caught in some N^2 loop looking for extractable code.
        while (len > 0 && (stackTypes[start + len] == null || stackTypes[start + len].length > untouchedStack)) {
          len--;
        }
        continue;
      }

      // make sure at most one local is defined and live on exit
      BitSet liveAtEnd = liveLocals[start + len];
      boolean multipleDefs = false;
      int localDefed = -1;
      int firstDef = -1;
      int secondDef = -1;
      for (int i = start; i < start + len; i++) {
        IInstruction instr = instructions[i];
        if (instr instanceof StoreInstruction) {
          int l = ((StoreInstruction) instr).getVarIndex();
          if (liveAtEnd.get(l) && l != localDefed) {
            if (localDefed < 0) {
              localDefed = l;
              firstDef = i;
            } else {
              multipleDefs = true;
              secondDef = i;
              break;
            }
          }
        }
      }

      if (multipleDefs) {
        HelperPatch p = findBlock(start, secondDef - start);
        if (p != null) {
          return p;
        }
        len = (start + len) - (firstDef + 1);
        start = firstDef + 1;
        continue;
      }

      // make sure that the same external handlers are used all the way through
      ExceptionHandler[] startHS = handlers[start];
      int numOuts = 0;
      for (ExceptionHandler element : startHS) {
        int t = element.handler;
        if (t < start || t >= start + len) {
          numOuts++;
        }
      }
      boolean mismatchedHandlers = false;
      int firstMismatch = -1;
      for (int i = start + 1; i < start + len; i++) {
        ExceptionHandler[] hs = handlers[i];
        int matchingOuts = 0;
        for (ExceptionHandler element : hs) {
          int t = element.handler;
          if (t < start || t >= start + len) {
            boolean match = false;
            for (ExceptionHandler element2 : startHS) {
              if (element2.equals(element)) {
                match = true;
                break;
              }
            }
            if (match) {
              matchingOuts++;
            }
          }
        }
        if (matchingOuts != numOuts) {
          firstMismatch = i;
          mismatchedHandlers = true;
          break;
        }
      }
      if (mismatchedHandlers) {
        HelperPatch p = findBlock(start, firstMismatch - start);
        if (p != null) {
          return p;
        }
        start = firstMismatch;
        continue;
      }

      // all conditions satisfied, extract the code
      try {
        HelperPatch p = makeHelperPatch(start, len, localDefed, unreadStack, untouchedStack);
        if (p == null) {
          // something went wrong. Probably the code to call the helper ended up
          // being
          // bigger than the original code we extracted!
          return null;
        } else {
          return p;
        }
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }

    return null;
  }

  private void makeHelpers() {
    int offset = 0;
    ArrayList<HelperPatch> patches = new ArrayList<>();

    while (offset + 5000 < instructions.length) {
      HelperPatch p = findBlock(offset, 5000);
      if (p != null) {
        patches.add(p);
        offset = p.start + p.length;
      } else {
        offset += 500;
      }
    }

    for (HelperPatch p : patches) {
      System.arraycopy(p.code, 0, instructions, p.start, p.code.length);
      for (int j = 0; j < p.length; j++) {
        int index = j + p.start;
        if (j < p.code.length) {
          instructions[index] = p.code[j];
        } else {
          instructions[index] = PopInstruction.make(0); // nop
        }
        handlers[index] = p.handlers;
        instructionsToBytecodes[index] = -1;
      }
    }
  }

  private void makeTypes() {
    Verifier v = new Verifier(isConstructor, isStatic, classType, signature, instructions, handlers, instructionsToBytecodes, null);
    if (hierarchy != null) {
      v.setClassHierarchy(hierarchy);
    }
    try {
      v.computeTypes();
    } catch (Verifier.FailureException ex) {
      throw new IllegalArgumentException("Cannot split oversized method because verification failed: " + ex.getMessage());
    }
    localTypes = v.getLocalTypes();
    stackTypes = v.getStackTypes();
  }

  /**
   * Do the work of generating new bytecodes.
   * 
   * In pathological cases this could throw an Error, when the code you passed in is too large to fit into a single JVM method and
   * Compiler can't find a way to break it up into helper methods. You probably won't encounter this unless you try to make it
   * happen :-).
   */
  final public void compile() {
    collectInstructionInfo();

    computeStackWords();

    if (!outputInstructions(0, instructions.length, 0, false, null)) {
      allocatedLocals = maxLocals;
      makeLiveLocals();
      makeTypes();
      auxMethods = new ArrayList<>();
      makeHelpers();

      computeStackWords();
      if (!outputInstructions(0, instructions.length, 0, false, null)) {
        throw new Error("Input code too large; consider breaking up your code");
      }
    }
    mainMethod = new Output(null, null, code, buildRawHandlers(0, instructions.length), buildBytecodeMap(0, instructions.length),
        maxLocals, maxStack, isStatic, instructionsToOffsets);

    instructionsToOffsets = null;
    branchTargets = null;
    stackWords = null;
    code = null;
  }

  /**
   * Get the output bytecodes and other information for the method.
   */
  final public Output getOutput() {
    return mainMethod;
  }

  /**
   * Get bytecodes and other information for any helper methods that are required to implement the main method. These helpers
   * represent code that could not be fit into the main method because of JVM method size constraints.
   */
  final public Output[] getAuxiliaryMethods() {
    if (auxMethods == null) {
      return null;
    } else {
      Output[] r = new Output[auxMethods.size()];
      auxMethods.toArray(r);
      return r;
    }
  }

  /**
   * This class represents a method generated by a Compiler. One input method to the Compiler can generate multiple Outputs (if the
   * input method is too big to be represented by a single method in the JVM, say if it requires more than 64K bytecodes).
   */
  public final static class Output {
    final private byte[] code;

    final private int[] rawHandlers;

    final private int[] newBytecodesToOldBytecodes;

    final private String name;

    final private String signature;

    final private boolean isStatic;

    final private int maxLocals;

    final private int maxStack;

    final private int[] instructionsToOffsets;
    
    Output(String name, String signature, byte[] code, int[] rawHandlers, int[] newBytecodesToOldBytecodes, int maxLocals,
        int maxStack, boolean isStatic, int[] instructionsToOffsets) {
      this.code = code;
      this.name = name;
      this.signature = signature;
      this.rawHandlers = rawHandlers;
      this.newBytecodesToOldBytecodes = newBytecodesToOldBytecodes;
      this.isStatic = isStatic;
      this.maxLocals = maxLocals;
      this.maxStack = maxStack;
      this.instructionsToOffsets = instructionsToOffsets;
    }

    /**
     * @return the actual bytecodes
     */
    public byte[] getCode() {
      return code;
    }

    public int[] getInstructionOffsets() {
      return instructionsToOffsets;
    }
    
    /**
     * @return the name of the method; either "null", if this code takes the place of the original method, or some string
     *         representing the name of a helper method
     */
    public String getMethodName() {
      return name;
    }

    /**
     * @return the method signature in JVM format
     */
    public String getMethodSignature() {
      return signature;
    }

    /**
     * @return the access flags that should be used for this method, or 0 if this is the code for the original method
     */
    public int getAccessFlags() {
      return name != null ? (ACC_PRIVATE | (isStatic ? ACC_STATIC : 0)) : 0;
    }

    /**
     * @return the raw exception handler table in JVM format
     */
    public int[] getRawHandlers() {
      return rawHandlers;
    }

    /**
     * @return whether the method is static
     */
    public boolean isStatic() {
      return isStatic;
    }

    /**
     * @return a map m such that the new bytecode instruction at offset i corresponds to the bytecode instruction at m[i] in the
     *         original method
     */
    public int[] getNewBytecodesToOldBytecodes() {
      return newBytecodesToOldBytecodes;
    }

    /**
     * @return the maximum stack size in words as required by the JVM
     */
    public int getMaxStack() {
      return maxStack;
    }

    /**
     * @return the maximum local variable size in words as required by the JVM
     */
    public int getMaxLocals() {
      return maxLocals;
    }
  }
}
