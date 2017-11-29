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

import com.ibm.wala.shrikeBT.IBinaryOpInstruction.Operator;

/**
 * A Decoder translates a method's Java bytecode into shrikeBT code, i.e. an array of Instruction objects and an array of lists of
 * ExceptionHandlers.
 * 
 * This class implements basic decoding functionality. A toolkit for reading class files must specialize this class with particular
 * constant pool reader implementation.
 * 
 * Normal usage of this class looks like this:
 * 
 * <pre>
 * 
 *    Decoder d = new MyToolkitDecoder(...);
 *    try {
 *      d.decode();
 *    } catch (Decoder.InvalidBytecodeException ex) {
 *      ...
 *    }
 *    Instruction[] myInstructions = d.getInstructions();
 *    ExceptionHandler[][] exnHandlers = d.getHandlers();
 * 
 * </pre>
 */
public abstract class Decoder implements Constants {
  private static final int UNSEEN = -1;

  private static final int INSIDE_INSTRUCTION = -2;

  private static int skip(int a, int b) {
    return a < b ? a : a + 1;
  }

  private final static ExceptionHandler[] noHandlers = new ExceptionHandler[0];

  /** This holds predecoded instructions for the single-byte instructions. */
  private final static Instruction[] simpleInstructions = makeSimpleInstructions();

  private static Instruction[] makeSimpleInstructions() {
    Instruction[] table = new Instruction[256];

    table[OP_aconst_null] = ConstantInstruction.make(TYPE_null, null);
    for (int i = OP_iconst_m1; i <= OP_iconst_5; i++) {
      table[i] = ConstantInstruction.make(TYPE_int, new Integer(i - OP_iconst_m1 - 1));
    }
    for (int i = OP_lconst_0; i <= OP_lconst_1; i++) {
      table[i] = ConstantInstruction.make(TYPE_long, new Long(i - OP_lconst_0));
    }
    for (int i = OP_fconst_0; i <= OP_fconst_2; i++) {
      table[i] = ConstantInstruction.make(TYPE_float, new Float(i - OP_fconst_0));
    }
    for (int i = OP_dconst_0; i <= OP_dconst_1; i++) {
      table[i] = ConstantInstruction.make(TYPE_double, new Double(i - OP_dconst_0));
    }

    for (int i = OP_iload_0; i <= OP_aload_3; i++) {
      table[i] = LoadInstruction.make(indexedTypes[(i - OP_iload_0) / 4], (i - OP_iload_0) % 4);
    }
    for (int i = OP_iaload; i <= OP_saload; i++) {
      table[i] = ArrayLoadInstruction.make(indexedTypes[i - OP_iaload]);
    }
    for (int i = OP_istore_0; i <= OP_astore_3; i++) {
      table[i] = StoreInstruction.make(indexedTypes[(i - OP_istore_0) / 4], (i - OP_istore_0) % 4);
    }
    for (int i = OP_iastore; i <= OP_sastore; i++) {
      table[i] = ArrayStoreInstruction.make(indexedTypes[i - OP_iastore]);
    }

    table[OP_pop] = PopInstruction.make(1);
    table[OP_dup] = DupInstruction.make(1, 0);
    table[OP_dup_x1] = DupInstruction.make(1, 1);
    table[OP_swap] = SwapInstruction.make();

    for (int i = OP_iadd; i <= OP_drem; i++) {
      table[i] = BinaryOpInstruction
          .make(indexedTypes[(i - OP_iadd) % 4], BinaryOpInstruction.Operator.values()[(i - OP_iadd) / 4]);
    }
    for (int i = OP_ineg; i <= OP_dneg; i++) {
      table[i] = UnaryOpInstruction.make(indexedTypes[i - OP_ineg]);
    }
    for (int i = OP_ishl; i <= OP_lushr; i++) {
      table[i] = ShiftInstruction.make(indexedTypes[(i - OP_ishl) % 2], ShiftInstruction.Operator.values()[(i - OP_ishl) / 2]);
    }
    for (int i = OP_iand; i <= OP_lxor; i++) {
      table[i] = BinaryOpInstruction.make(indexedTypes[(i - OP_iand) % 2],
          BinaryOpInstruction.Operator.values()[BinaryOpInstruction.Operator.AND.ordinal() + (i - OP_iand) / 2]);
    }

    for (int i = OP_i2l; i <= OP_d2f; i++) {
      table[i] = ConversionInstruction.make(indexedTypes[(i - OP_i2l) / 3], indexedTypes[skip((i - OP_i2l) % 3, (i - OP_i2l) / 3)]);
    }
    for (int i = OP_i2b; i <= OP_i2s; i++) {
      table[i] = ConversionInstruction.make(TYPE_int, indexedTypes[5 + (i - OP_i2b)]);
    }

    table[OP_lcmp] = ComparisonInstruction.make(TYPE_long, ComparisonInstruction.Operator.CMP);
    for (int i = OP_fcmpl; i <= OP_dcmpg; i++) {
      table[i] = ComparisonInstruction.make(indexedTypes[2 + (i - OP_fcmpl) / 2],
          ComparisonInstruction.Operator.values()[ComparisonInstruction.Operator.CMPL.ordinal() + (i - OP_fcmpl) % 2]);
    }

    for (int i = OP_ireturn; i <= OP_areturn; i++) {
      table[i] = ReturnInstruction.make(indexedTypes[i - OP_ireturn]);
    }
    table[OP_return] = ReturnInstruction.make(TYPE_void);

    table[OP_athrow] = ThrowInstruction.make(false);

    table[OP_monitorenter] = MonitorInstruction.make(true);
    table[OP_monitorexit] = MonitorInstruction.make(false);

    table[OP_arraylength] = ArrayLengthInstruction.make();

    return table;
  }

  private static final Instruction makeZero = ConstantInstruction.make(0);

  // Holds the result of decoding
  private IInstruction[] instructions;

  private ExceptionHandler[][] handlers;

  private int[] instructionsToBytecodes;

  final private ConstantPoolReader constantPool;

  // Holds the input to decode
  private final byte[] code;

  private final int[] rawHandlers;

  // Temporary working data
  private int[] decodedOffset;

  private byte[] decodedSize;

  private ArrayList<Instruction> decoded;

  private int[] belongsToSub;

  private int[] JSRs;

  private RetInfo[] retInfo;

  /**
   * This constructor is only supposed to be used by subclasses.
   * 
   * @param code the bytecodes for a method as per JVM spec
   * @param rawHandlers flattened array of (startPC, endPC, targetPC, classIndex) tuples defined as per the JVM spec
   */
  protected Decoder(byte[] code, int[] rawHandlers, ConstantPoolReader cp) {
    this.code = code;
    this.rawHandlers = rawHandlers;
    this.constantPool = cp;
  }

  public ConstantPoolReader getConstantPool() {
    return constantPool;
  }

  private int decodeShort(int index) {
    return (code[index] << 8) | (code[index + 1] & 0xFF);
  }

  private int decodeUShort(int index) {
    return ((code[index] & 0xFF) << 8) | (code[index + 1] & 0xFF);
  }

  private int decodeInt(int index) {
    return (code[index] << 24) | ((code[index + 1] & 0xFF) << 16) | ((code[index + 2] & 0xFF) << 8) | (code[index + 3] & 0xFF);
  }

  private Instruction makeConstantPoolLoad(int index) throws InvalidBytecodeException {
    ConstantInstruction ci = ConstantInstruction.make(constantPool, index);
    if (ci == null) {
      throw new InvalidBytecodeException("Constant pool item at index " + index + " (type "
          + constantPool.getConstantPoolItemType(index) + ") cannot be loaded");
    }
    return ci;
  }

  private static int elemCount(byte[] stack, int stackPtr) throws InvalidBytecodeException {
    if (stackPtr < 0) {
      throw new InvalidBytecodeException("Stack underflow");
    }

    if (stack[stackPtr] == 2) {
      return 1;
    } else {
      if (stackPtr < 1) {
        throw new InvalidBytecodeException("Stack underflow");
      }

      if (stack[stackPtr - 1] != 1) {
        throw new InvalidBytecodeException("Trying to manipulate a pair of " + "one-word items but one of them is two words");
      }

      return 2;
    }
  }

  private static String getPrimitiveType(int t) throws InvalidBytecodeException {
    switch (t) {
    case T_BOOLEAN:
      return TYPE_boolean;
    case T_CHAR:
      return TYPE_char;
    case T_FLOAT:
      return TYPE_float;
    case T_DOUBLE:
      return TYPE_double;
    case T_BYTE:
      return TYPE_byte;
    case T_SHORT:
      return TYPE_short;
    case T_INT:
      return TYPE_int;
    case T_LONG:
      return TYPE_long;
    default:
      throw new InvalidBytecodeException("Unknown primitive type " + t);
    }
  }

  private static class RetInfo {
    int sub;

    final int retVar;

    final int stackLen;

    final byte[] stackWords;

    RetInfo(int sub, int retVar, int stackLen, byte[] stackWords) {
      this.sub = sub;
      this.retVar = retVar;
      this.stackLen = stackLen;
      this.stackWords = stackWords;
    }
  }

  private boolean doesSubroutineReturn(int sub) {
    for (RetInfo element : retInfo) {
      if (element != null && element.sub == sub) {
        return true;
      }
    }
    return false;
  }

  private int findReturnToVar(int v, int addr, boolean[] visited) throws InvalidBytecodeException {
    while (true) {
      if (visited[addr]) {
        return 0;
      } else if (retInfo[addr] != null && retInfo[addr].retVar == v) {
        return addr;
      } else {
        int offset = decodedOffset[addr];

        if (offset == UNSEEN) {
          return 0;
        }

        int size = decodedSize[addr];
        Instruction instr = null;

        visited[addr] = true;

        for (int j = 0; j < rawHandlers.length; j += 4) {
          if (rawHandlers[j] <= addr && addr < rawHandlers[j + 1]) {
            int handlerAddr = rawHandlers[j + 2];

            if (decodedOffset[handlerAddr] < 0) {
              byte[] stackWords = new byte[code.length * 2];
              // the bottom element on the stack must be a return address.
              stackWords[0] = 1;
              decodeAt(handlerAddr, 1, stackWords);
            }

            int r = findReturnToVar(v, handlerAddr, visited);
            if (r != 0) {
              return r;
            }
          }
        }

        // If there's a JSR here, see if it ever returns. If it does not
        // then this instruction does not fall through and so we should
        // stop searching.
        if (JSRs[addr] != 0) {
          if (!doesSubroutineReturn(JSRs[addr])) {
            return 0;
          }
        } else {
          for (int j = 0; j < size; j++) {
            instr = decoded.get(offset + j);
            if (instr instanceof StoreInstruction && ((StoreInstruction) instr).getVarIndex() == v) {
              return 0;
            }

            int[] targets = instr.getBranchTargets();
            for (int target : targets) {
              if (target >= 0) {
                int r = findReturnToVar(v, target, visited);
                if (r != 0) {
                  return r;
                }
              }
            }
          }

          if (instr != null && !instr.isFallThrough()) {
            return 0;
          }
        }

        do {
          addr++;
        } while (decodedOffset[addr] == INSIDE_INSTRUCTION);
      }
    }
  }

  /**
   * Locate an instruction that returns from this subroutine; return 0 if one cannot be found.
   */
  private int findReturn(int subAddr) throws InvalidBytecodeException {
    if (decodedSize[subAddr] < 1) {
      throw new InvalidBytecodeException("Subroutine at " + subAddr + " does not start with an astore or pop instruction");
    }
    Instruction instr = decoded.get(decodedOffset[subAddr]);

    if (instr instanceof PopInstruction) {
      // this subroutine can't return
      return 0;
    }

    if (!(instr instanceof StoreInstruction)) {
      throw new InvalidBytecodeException("Subroutine at " + subAddr + " does not start with an astore or pop instruction");
    }

    int localIndex = ((StoreInstruction) instr).getVarIndex();

    do {
      subAddr++;
    } while (decodedOffset[subAddr] == INSIDE_INSTRUCTION);

    return findReturnToVar(localIndex, subAddr, new boolean[code.length]);
  }

  private void decodeSubroutine(int jsrAddr, int retToAddr, int subAddr, int stackLen, byte[] stackWords)
      throws InvalidBytecodeException {
    if (JSRs == null) {
      JSRs = new int[code.length];
      retInfo = new RetInfo[code.length];
    }

    JSRs[jsrAddr] = subAddr;

    if (decodedOffset[subAddr] < 0) {
      stackWords[stackLen] = 1;
      stackLen++;

      decodeAt(subAddr, stackLen, stackWords);
    }

    int retAddr = findReturn(subAddr);
    if (retAddr > 0) {
      RetInfo r = retInfo[retAddr];
      r.sub = subAddr;
      byte[] cloneStackWords = new byte[r.stackWords.length];
      System.arraycopy(r.stackWords, 0, cloneStackWords, 0, cloneStackWords.length);
      decodeAt(retToAddr, r.stackLen, cloneStackWords);
    }
  }

  private void assignReachablesToSubroutine(int addr, int sub) {

    while (belongsToSub[addr] < 0) {
      int size = decodedSize[addr];

      belongsToSub[addr] = sub;

      for (int j = 0; j < rawHandlers.length; j += 4) {
        if (rawHandlers[j] <= addr && addr < rawHandlers[j + 1]) {
          assignReachablesToSubroutine(rawHandlers[j + 2], sub);
        }
      }

      Instruction instr = null;
      if (size > 0 && JSRs[addr] == 0) {
        int offset = decodedOffset[addr];
        instr = decoded.get(offset + size - 1);
        int[] targets = instr.getBranchTargets();
        for (int target : targets) {
          if (target >= 0) {
            // only chase real gotos; ignore rets
            assignReachablesToSubroutine(target, sub);
          }
        }
      }

      if (instr != null && !instr.isFallThrough()) {
        return;
      }
      if (JSRs[addr] != 0 && !doesSubroutineReturn(JSRs[addr])) {
        return;
      }

      do {
        addr++;
      } while (decodedOffset[addr] < 0);
    }
  }

  private void assignSubroutine(int sub) {
    assignReachablesToSubroutine(sub, sub);

    for (int i = 0; i < belongsToSub.length; i++) {
      if (JSRs[i] > 0 && belongsToSub[i] == sub && belongsToSub[JSRs[i]] < 0) {
        assignSubroutine(JSRs[i]);
      }
    }
  }

  private void computeSubroutineMap() {
    belongsToSub = new int[code.length];

    for (int i = 0; i < belongsToSub.length; i++) {
      belongsToSub[i] = -1;
    }

    assignSubroutine(0);
  }

  private int decodeBytecodeInstruction(int index, int stackLen, byte[] stackWords) throws InvalidBytecodeException {
    int opcode = code[index] & 0xFF;

    Instruction i = simpleInstructions[opcode];
    if (i != null) {
      decoded.add(i);
      return index + 1;
    }

    boolean wide = false;

    while (true) {
      index++;

      switch (opcode) {
      case OP_nop:
        break;
      case OP_bipush:
        i = ConstantInstruction.make(code[index]);
        index++;
        break;
      case OP_sipush:
        i = ConstantInstruction.make(decodeShort(index));
        index += 2;
        break;
      case OP_ldc:
        i = makeConstantPoolLoad(code[index] & 0xFF);
        index++;
        break;
      case OP_ldc_w:
        i = makeConstantPoolLoad(decodeShort(index));
        index += 2;
        break;
      case OP_ldc2_w:
        i = makeConstantPoolLoad(decodeShort(index));
        index += 2;
        break;
      case OP_iload:
      case OP_lload:
      case OP_fload:
      case OP_dload:
      case OP_aload:
        i = LoadInstruction.make(indexedTypes[opcode - OP_iload], wide ? decodeUShort(index) : (code[index] & 0xFF));
        index += wide ? 2 : 1;
        break;
      case OP_istore:
      case OP_lstore:
      case OP_fstore:
      case OP_dstore:
      case OP_astore:
        i = StoreInstruction.make(indexedTypes[opcode - OP_istore], wide ? decodeUShort(index) : (code[index] & 0xFF));
        index += wide ? 2 : 1;
        break;
      case OP_pop2:
        i = PopInstruction.make(elemCount(stackWords, stackLen - 1));
        break;
      case OP_dup_x2:
        i = DupInstruction.make(1, elemCount(stackWords, stackLen - 2));
        break;
      case OP_dup2:
        i = DupInstruction.make(elemCount(stackWords, stackLen - 1), 0);
        break;
      case OP_dup2_x1:
        i = DupInstruction.make(elemCount(stackWords, stackLen - 1), 1);
        break;
      case OP_dup2_x2:
        i = DupInstruction.make(elemCount(stackWords, stackLen - 1), elemCount(stackWords, stackLen - 2));
        break;
      case OP_iinc: {
        int v = wide ? decodeUShort(index) : (code[index] & 0xFF);
        int c = wide ? decodeShort(index + 2) : code[index + 1];

        decoded.add(LoadInstruction.make(TYPE_int, v));
        decoded.add(ConstantInstruction.make(c));
        decoded.add(BinaryOpInstruction.make(TYPE_int, Operator.ADD));
        i = StoreInstruction.make(TYPE_int, v);
        index += wide ? 4 : 2;
        break;
      }
      case OP_ifeq:
      case OP_ifne:
      case OP_iflt:
      case OP_ifle:
      case OP_ifgt:
      case OP_ifge:
        decoded.add(makeZero);
        i = ConditionalBranchInstruction.make(TYPE_int, ConditionalBranchInstruction.Operator.values()[opcode - OP_ifeq],
            (index - 1) + decodeShort(index));
        index += 2;
        break;
      case OP_if_icmpeq:
      case OP_if_icmpne:
      case OP_if_icmplt:
      case OP_if_icmple:
      case OP_if_icmpgt:
      case OP_if_icmpge:
        i = ConditionalBranchInstruction.make((short) opcode, (index - 1) + decodeShort(index));
        index += 2;
        break;
      case OP_if_acmpeq:
      case OP_if_acmpne:
        i = ConditionalBranchInstruction.make(TYPE_Object, ConditionalBranchInstruction.Operator.values()[opcode - OP_if_acmpeq],
            (index - 1) + decodeShort(index));
        index += 2;
        break;
      case OP_goto:
        i = GotoInstruction.make((index - 1) + decodeShort(index));
        index += 2;
        break;
      case OP_jsr: {
        index += 2;
        break;
      }
      case OP_jsr_w: {
        index += 4;
        break;
      }
      case OP_ret:
        int v = wide ? decodeUShort(index) : (code[index] & 0xFF);
        i = GotoInstruction.make(-1 - v);

        if (retInfo == null) {
          throw new InvalidBytecodeException("'ret' outside of subroutine");
        }
        retInfo[index - (wide ? 2 : 1)] = new RetInfo(-1, v, stackLen, stackWords);

        index += wide ? 2 : 1;
        break;
      case OP_tableswitch: {
        int start = index - 1;
        while ((index & 3) != 0) {
          index++;
        }
        int def = start + decodeInt(index);
        int low = decodeInt(index + 4);
        int high = decodeInt(index + 8);
        int[] t = new int[(high - low + 1) * 2];

        for (int j = 0; j < t.length; j += 2) {
          t[j] = j / 2 + low;
          t[j + 1] = start + decodeInt(index + 12 + j * 2);
        }
        i = SwitchInstruction.make(t, def);
        index += 12 + (high - low + 1) * 4;
        break;
      }
      case OP_lookupswitch: {
        int start = index - 1;
        while ((index & 3) != 0) {
          index++;
        }
        int def = start + decodeInt(index);
        int n = decodeInt(index + 4);
        int[] t = new int[n * 2];

        for (int j = 0; j < t.length; j += 2) {
          t[j] = decodeInt(index + 8 + j * 4);
          t[j + 1] = start + decodeInt(index + 12 + j * 4);
        }
        i = SwitchInstruction.make(t, def);
        index += 8 + n * 8;
        break;
      }
      case OP_getstatic:
      case OP_getfield: {
        int f = decodeUShort(index);
        i = GetInstruction.make(constantPool, f, opcode == OP_getstatic);
        index += 2;
        break;
      }
      case OP_putstatic:
      case OP_putfield: {
        int f = decodeUShort(index);
        i = PutInstruction.make(constantPool, f, opcode == OP_putstatic);
        index += 2;
        break;
      }
      case OP_invokevirtual:
      case OP_invokespecial:
      case OP_invokestatic: {
        int m = decodeUShort(index);
        i = InvokeInstruction.make(constantPool, m, opcode);
        index += 2;
        break;
      }
      case OP_invokeinterface: {
        int m = decodeUShort(index);
        i = InvokeInstruction.make(constantPool, m, opcode);
        index += 4;
        break;
      }
      case OP_invokedynamic: {
        int m = decodeUShort(index);
        i = InvokeDynamicInstruction.make(constantPool, m, opcode);
        index += 4;
        break;
      }
      case OP_new:
        i = NewInstruction.make(constantPool.getConstantPoolClassType(decodeUShort(index)), 0);
        index += 2;
        break;
      case OP_newarray:
        i = NewInstruction.make(Util.makeArray(getPrimitiveType(code[index])), 1);
        index++;
        break;
      case OP_anewarray:
        i = NewInstruction.make(Util.makeArray(constantPool.getConstantPoolClassType(decodeUShort(index))), 1);
        index += 2;
        break;
      case OP_checkcast:
        i = CheckCastInstruction.make(constantPool.getConstantPoolClassType(decodeUShort(index)));
        index += 2;
        break;
      case OP_instanceof:
        i = InstanceofInstruction.make(constantPool.getConstantPoolClassType(decodeUShort(index)));
        index += 2;
        break;
      case OP_wide:
        wide = true;
        opcode = code[index] & 0xFF;
        continue;
      case OP_multianewarray:
        i = NewInstruction.make(constantPool.getConstantPoolClassType(decodeUShort(index)), code[index + 2] & 0xFF);
        index += 3;
        break;
      case OP_ifnull:
      case OP_ifnonnull:
        decoded.add(ConstantInstruction.make(TYPE_Object, null));
        i = ConditionalBranchInstruction.make(TYPE_Object, ConditionalBranchInstruction.Operator.values()[opcode - OP_ifnull],
            (index - 1) + decodeShort(index));
        index += 2;
        break;
      case OP_goto_w:
        i = GotoInstruction.make((index - 1) + decodeInt(index));
        index += 4;
        break;
      default:
        throw new InvalidBytecodeException("Unknown opcode " + opcode);
      }

      break;
    }

    if (i != null) {
      decoded.add(i);
    }

    return index;
  }

  private static int applyInstructionToStack(Instruction i, int stackLen, byte[] stackWords) throws InvalidBytecodeException {
    stackLen -= i.getPoppedCount();

    if (stackLen < 0) {
      throw new InvalidBytecodeException("Stack underflow");
    }

    if (i instanceof DupInstruction) {
      DupInstruction d = (DupInstruction) i;
      int delta = d.getDelta();
      int size = d.getSize();

      System.arraycopy(stackWords, stackLen + delta, stackWords, stackLen + size + delta, size);
      System.arraycopy(stackWords, stackLen, stackWords, stackLen + size, delta);
      System.arraycopy(stackWords, stackLen + size + delta, stackWords, stackLen, size);
      stackLen += size * 2 + delta;
    } else if (i instanceof SwapInstruction) {
      if (stackWords[stackLen] != stackWords[stackLen + 1]) {
        throw new Error("OP_swap must always be swapping the same size, 1");
      }
      stackLen += 2;
    } else {
      byte pushedWords = i.getPushedWordSize();
      if (pushedWords > 0) {
        stackWords[stackLen] = pushedWords;
        stackLen++;
      }
    }

    return stackLen;
  }

  private void decodeAt(int index, int stackLen, byte[] stackWords) throws InvalidBytecodeException {
    if (index < 0 || index >= decodedOffset.length) {
      throw new InvalidBytecodeException(index, "Branch index " + index + " out of range");
    }

    while (decodedOffset[index] < 0) {
      int s = decoded.size();

      decodedOffset[index] = s;

      int newIndex;

      try {
        newIndex = decodeBytecodeInstruction(index, stackLen, stackWords);
        int instrCount = decoded.size() - s;

        decodedSize[index] = (byte) instrCount;

        // mark invalid offsets
        for (int i = index + 1; i < newIndex; i++) {
          decodedOffset[i] = INSIDE_INSTRUCTION;
        }

        if (instrCount > 0) {
          for (int i = s; i < s + instrCount; i++) {
            stackLen = applyInstructionToStack(decoded.get(i), stackLen, stackWords);
          }

          Instruction instr = decoded.get(s + instrCount - 1);
          int[] targets = instr.getBranchTargets();

          for (int t : targets) {
            if (t >= 0) {
              decodeAt(t, stackLen, stackWords.clone());
            }
          }

          if (!instr.isFallThrough()) {
            return;
          }
        } else { // possibly the jsr case
          int jIndex = index;
          int opcode = code[jIndex] & 0xFF;
          if (opcode == OP_wide) {
            jIndex++;
            opcode = code[jIndex] & 0xFF;
          }

          if (opcode == OP_jsr || opcode == OP_jsr_w) {
            jIndex++;
            int offset = opcode == OP_jsr_w ? decodeInt(jIndex) : decodeShort(jIndex);

            decoded.add(GotoInstruction.make(0));
            decodedSize[index] = 1;

            decodeSubroutine(index, newIndex, index + offset, stackLen, stackWords);
            return;
          }
        }
      } catch (InvalidBytecodeException ex) {
        ex.setIndex(index);
        throw ex;
      } catch (Error ex) {
        System.err.println("Fatal error at index " + index);
        throw ex;
      } catch (RuntimeException ex) {
        System.err.println("Fatal error at index " + index);
        throw ex;
      }

      index = newIndex;

      if (index >= decodedOffset.length) {
        throw new InvalidBytecodeException(index, "Fell off end of bytecode array");
      }
    }
  }

  /**
   * This exception is thrown when the Decoder detects invalid incoming bytecode (code that would not pass the Java verifier). We
   * don't guarantee to perform full verification in the Decoder, however.
   */
  public static class InvalidBytecodeException extends Exception {

    private static final long serialVersionUID = -8807125136613458111L;

    private int index;

    InvalidBytecodeException(String s) {
      super(s);
      index = -1;
    }

    InvalidBytecodeException(int i, String s) {
      super(s);
      index = i;
    }

    void setIndex(int i) {
      if (index < 0) {
        index = i;
      }
    }

    /**
     * @return the offset of the bytecode instruction deemed to be invalid
     */
    public int getIndex() {
      return index;
    }
  }

  private ExceptionHandler[] makeHandlers(int i, int[] addrMap) {
    int numHandlers = 0;
    for (int j = 0; j < rawHandlers.length; j += 4) {
      if (rawHandlers[j] <= i && i < rawHandlers[j + 1]) {
        numHandlers++;
      }
    }

    return makeHandlers(i, numHandlers, addrMap);
  }

  private ExceptionHandler[] makeHandlers(int i, int numHandlers, int[] addrMap) {
    if (numHandlers == 0) {
      return noHandlers;
    } else {
      ExceptionHandler[] hs = new ExceptionHandler[numHandlers];
      numHandlers = 0;
      for (int j = 0; j < rawHandlers.length; j += 4) {
        if (rawHandlers[j] <= i && i < rawHandlers[j + 1]) {
          int classIndex = rawHandlers[j + 3];
          String catchClass = classIndex == 0 ? null : constantPool.getConstantPoolClassType(classIndex);

          hs[numHandlers] = new ExceptionHandler(addrMap[rawHandlers[j + 2]], catchClass);
          numHandlers++;
        }
      }
      return hs;
    }
  }

  private int computeSubroutineLength(int sub) {
    int len = 1; // extra instruction for "push null"

    for (int i = sub; i < belongsToSub.length; i++) {
      if (belongsToSub[i] == sub) {
        len += decodedSize[i];
        if (JSRs[i] > 0) {
          len += computeSubroutineLength(JSRs[i]);
        }
      }
    }

    return len;
  }

  private int appendSubroutineCode(int callSite, int newCodeIndex, int[] callerMap) {
    instructions[callerMap[callSite]] = GotoInstruction.make(newCodeIndex);

    instructions[newCodeIndex] = ConstantInstruction.make(TYPE_Object, null);
    newCodeIndex++;

    int subStart = newCodeIndex;
    int[] map = callerMap.clone();
    int sub = JSRs[callSite];

    // emit the subroutine code
    for (int i = sub; i < belongsToSub.length; i++) {
      if (belongsToSub[i] == sub) {
        int s = decodedSize[i];
        int offset = decodedOffset[i];

        map[i] = newCodeIndex;

        for (int j = 0; j < s; j++) {
          Instruction instr = decoded.get(offset + j);
          instructions[newCodeIndex] = instr;
          instructionsToBytecodes[newCodeIndex] = i;
          newCodeIndex++;
        }
      }
    }

    // fix up branch targets within emitted subroutine
    for (int i = subStart; i < newCodeIndex; i++) {
      IInstruction instr = instructions[i];
      if (instr instanceof GotoInstruction && ((GotoInstruction) instr).getLabel() < 0) {
        // fix up 'ret' instruction to branch back to return address
        instructions[i] = GotoInstruction.make(callerMap[callSite] + 1);
      } else {
        instructions[i] = instr.redirectTargets(map);
      }
      handlers[i] = makeHandlers(instructionsToBytecodes[i], map);
    }

    // extend handlers to cover the fake "push null"
    handlers[subStart - 1] = handlers[subStart];

    // resolve callee subroutines
    for (int i = sub; i < belongsToSub.length; i++) {
      if (belongsToSub[i] == sub && JSRs[i] > 0) {
        newCodeIndex = appendSubroutineCode(i, newCodeIndex, map);
      }
    }

    return newCodeIndex;
  }

  /**
   * Perform the decoding.
   * 
   * @throws InvalidBytecodeException the incoming code is invalid and would fail Java bytecode verification
   */
  final public void decode() throws InvalidBytecodeException {
    byte[] stackWords = new byte[code.length * 2];

    decoded = new ArrayList<>();
    decodedOffset = new int[code.length];
    for (int i = 0; i < decodedOffset.length; i++) {
      decodedOffset[i] = UNSEEN;
    }
    decodedSize = new byte[code.length];

    decodeAt(0, 0, stackWords);
    // Decode code that's only reachable through exception handlers
    for (int i = 0; i < rawHandlers.length; i += 4) {
      stackWords[0] = 1;
      decodeAt(rawHandlers[i + 2], 1, stackWords);
    }

    if (retInfo != null) {
      computeSubroutineMap();
      retInfo = null;
    }

    int instructionsLen = decoded.size();

    if (belongsToSub != null) {
      for (int i = 0; i < belongsToSub.length; i++) {
        if (belongsToSub[i] == 0) {
          if (JSRs[i] > 0) {
            instructionsLen += computeSubroutineLength(JSRs[i]);
          }
        } else if (belongsToSub[i] > 0) {
          instructionsLen -= decodedSize[i];
        }
      }
    }

    instructions = new Instruction[instructionsLen];
    instructionsToBytecodes = new int[instructionsLen];
    handlers = new ExceptionHandler[instructionsLen][];

    // shuffle decoded instructions into method order
    int p = 0;
    for (int i = 0; i < decodedOffset.length; i++) {
      int offset = decodedOffset[i];

      if (offset >= 0 && (belongsToSub == null || belongsToSub[i] == 0)) {
        decodedOffset[i] = p;

        int s = decodedSize[i];
        for (int j = 0; j < s; j++) {
          instructions[p] = decoded.get(offset + j);
          instructionsToBytecodes[p] = i;
          p++;
        }
      }
    }

    // fix up instructions to refer to the instruction vector instead of
    // bytecode offsets
    for (int i = 0; i < p; i++) {
      instructions[i] = instructions[i].redirectTargets(decodedOffset);
    }

    // emit subroutines
    if (JSRs != null) {
      for (int i = 0; i < JSRs.length; i++) {
        if (JSRs[i] > 0 && belongsToSub[i] == 0) {
          p = appendSubroutineCode(i, p, decodedOffset);
        }
      }
    }

    // generate exception handlers
    if (rawHandlers.length > 0) {
      ExceptionHandler[] hs = null;
      int handlersValidBefore = -1;

      p = 0;
      for (int i = 0; i < decodedOffset.length; i++) {
        if (decodedOffset[i] >= 0 && (belongsToSub == null || belongsToSub[i] == 0)) {
          if (i >= handlersValidBefore) {
            // We just crossed a handler range boundary
            // compute new exception handler array
            int numHandlers = 0;
            handlersValidBefore = Integer.MAX_VALUE;
            for (int j = 0; j < rawHandlers.length; j += 4) {
              if (rawHandlers[j] <= i) {
                if (i < rawHandlers[j + 1]) {
                  numHandlers++;
                  handlersValidBefore = Math.min(handlersValidBefore, rawHandlers[j + 1]);
                }
              } else {
                handlersValidBefore = Math.min(handlersValidBefore, rawHandlers[j]);
              }
            }

            hs = makeHandlers(i, numHandlers, decodedOffset);
          }

          int s = decodedSize[i];
          for (int j = 0; j < s; j++) {
            handlers[p] = hs;
            p++;
          }
        }
      }
    } else {
      for (int i = 0; i < handlers.length; i++) {
        handlers[i] = noHandlers;
      }
    }

    decoded = null;
    decodedOffset = null;
    decodedSize = null;
    belongsToSub = null;
    JSRs = null;
  }

  /**
   * Get the decoded instructions.
   * 
   * @return array of decoded instructions
   */
  final public IInstruction[] getInstructions() {
    if (instructions == null) {
      throw new Error("Call decode() before calling getInstructions()");
    }
    return instructions;
  }

  /**
   * Get the decoded exception handlers.
   * 
   * @return array of exception handler lists
   */
  final public ExceptionHandler[][] getHandlers() {
    if (handlers == null) {
      throw new Error("Call decode() before calling getHandlers()");
    }
    return handlers;
  }

  /**
   * Get the mapping between instructions and input bytecodes.
   * 
   * @return an array m such that m[i] is the offset of the bytecode instruction which gave rise to the Instruction referenced in
   *         the instructions array at offset i
   */
  final public int[] getInstructionsToBytecodes() {
    if (instructionsToBytecodes == null) {
      throw new Error("Call decode() before calling getInstructionsToBytecodes()");
    }
    return instructionsToBytecodes;
  }

  /**
   * @return true iff the method decoded by this Decoder contains subroutines (JSRs)
   */
  final public boolean containsSubroutines() {
    if (instructions == null) {
      throw new Error("Call decode() before calling containsSubroutines()");
    }
    return JSRs != null;
  }
}
