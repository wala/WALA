/*
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrike.shrikeBT;

import com.ibm.wala.shrike.shrikeCT.ClassConstants;

/**
 * This interface defines a bunch of constants from the JVM spec. It also defines some constants we
 * need for other purposes.
 *
 * <p>Here are the JVM constants:
 *
 * <ul>
 *   <li>The {@code OP_} constants define the JVM instruction opcodes.
 *   <li>The {@code ACC_} constants define the accessibility flags for classes, fields, and methods.
 *   <li>The {@code CONSTANT_} constants define the constant pool item types.
 *   <li>The {@code T_} constants define the types of arrays that can be created by {@link
 *       #OP_newarray}.
 *   <li>The {@code TYPE_} constants define the string representations of various JVM types. Two
 *       special non-JVM types are defined, {@link #TYPE_null} and {@link #TYPE_unknown}, as noted
 *       below.
 * </ul>
 *
 * Non-JVM constants:
 *
 * <ul>
 *   <li>The {@code OPR_} constants define the set of operators present in JVM instructions.
 *   <li>The {@code TYPE_..._index} constants define numeric representations of the JVM base types.
 *   <li>The {@link #indexedTypes} array maps those numeric representations to their official string
 *       representations.
 *   <li>The {@link #indexedTypes_T} array maps those numeric representations to the corresponding
 *       {@code T_} constant.
 * </ul>
 */
public interface Constants {

  short OP_nop = BytecodeConstants.JBC_nop;

  short OP_aconst_null = BytecodeConstants.JBC_aconst_null;

  short OP_iconst_m1 = BytecodeConstants.JBC_iconst_m1;

  short OP_iconst_0 = BytecodeConstants.JBC_iconst_0;

  short OP_iconst_1 = BytecodeConstants.JBC_iconst_1;

  short OP_iconst_2 = BytecodeConstants.JBC_iconst_2;

  short OP_iconst_3 = BytecodeConstants.JBC_iconst_3;

  short OP_iconst_4 = BytecodeConstants.JBC_iconst_4;

  short OP_iconst_5 = BytecodeConstants.JBC_iconst_5;

  short OP_lconst_0 = BytecodeConstants.JBC_lconst_0;

  short OP_lconst_1 = BytecodeConstants.JBC_lconst_1;

  short OP_fconst_0 = BytecodeConstants.JBC_fconst_0;

  short OP_fconst_1 = BytecodeConstants.JBC_fconst_1;

  short OP_fconst_2 = BytecodeConstants.JBC_fconst_2;

  short OP_dconst_0 = BytecodeConstants.JBC_dconst_0;

  short OP_dconst_1 = BytecodeConstants.JBC_dconst_1;

  short OP_bipush = BytecodeConstants.JBC_bipush;

  short OP_sipush = BytecodeConstants.JBC_sipush;

  short OP_ldc = BytecodeConstants.JBC_ldc;

  short OP_ldc_w = BytecodeConstants.JBC_ldc_w;

  short OP_ldc2_w = BytecodeConstants.JBC_ldc2_w;

  short OP_iload = BytecodeConstants.JBC_iload;

  short OP_lload = BytecodeConstants.JBC_lload;

  short OP_fload = BytecodeConstants.JBC_fload;

  short OP_dload = BytecodeConstants.JBC_dload;

  short OP_aload = BytecodeConstants.JBC_aload;

  short OP_iload_0 = BytecodeConstants.JBC_iload_0;

  short OP_iload_1 = BytecodeConstants.JBC_iload_1;

  short OP_iload_2 = BytecodeConstants.JBC_iload_2;

  short OP_iload_3 = BytecodeConstants.JBC_iload_3;

  short OP_lload_0 = BytecodeConstants.JBC_lload_0;

  short OP_lload_1 = BytecodeConstants.JBC_lload_1;

  short OP_lload_2 = BytecodeConstants.JBC_lload_2;

  short OP_lload_3 = BytecodeConstants.JBC_lload_3;

  short OP_fload_0 = BytecodeConstants.JBC_fload_0;

  short OP_fload_1 = BytecodeConstants.JBC_fload_1;

  short OP_fload_2 = BytecodeConstants.JBC_fload_2;

  short OP_fload_3 = BytecodeConstants.JBC_fload_3;

  short OP_dload_0 = BytecodeConstants.JBC_dload_0;

  short OP_dload_1 = BytecodeConstants.JBC_dload_1;

  short OP_dload_2 = BytecodeConstants.JBC_dload_2;

  short OP_dload_3 = BytecodeConstants.JBC_dload_3;

  short OP_aload_0 = BytecodeConstants.JBC_aload_0;

  short OP_aload_1 = BytecodeConstants.JBC_aload_1;

  short OP_aload_2 = BytecodeConstants.JBC_aload_2;

  short OP_aload_3 = BytecodeConstants.JBC_aload_3;

  short OP_iaload = BytecodeConstants.JBC_iaload;

  short OP_laload = BytecodeConstants.JBC_laload;

  short OP_faload = BytecodeConstants.JBC_faload;

  short OP_daload = BytecodeConstants.JBC_daload;

  short OP_aaload = BytecodeConstants.JBC_aaload;

  short OP_baload = BytecodeConstants.JBC_baload;

  short OP_caload = BytecodeConstants.JBC_caload;

  short OP_saload = BytecodeConstants.JBC_saload;

  short OP_istore = BytecodeConstants.JBC_istore;

  short OP_lstore = BytecodeConstants.JBC_lstore;

  short OP_fstore = BytecodeConstants.JBC_fstore;

  short OP_dstore = BytecodeConstants.JBC_dstore;

  short OP_astore = BytecodeConstants.JBC_astore;

  short OP_istore_0 = BytecodeConstants.JBC_istore_0;

  short OP_istore_1 = BytecodeConstants.JBC_istore_1;

  short OP_istore_2 = BytecodeConstants.JBC_istore_2;

  short OP_istore_3 = BytecodeConstants.JBC_istore_3;

  short OP_lstore_0 = BytecodeConstants.JBC_lstore_0;

  short OP_lstore_1 = BytecodeConstants.JBC_lstore_1;

  short OP_lstore_2 = BytecodeConstants.JBC_lstore_2;

  short OP_lstore_3 = BytecodeConstants.JBC_lstore_3;

  short OP_fstore_0 = BytecodeConstants.JBC_fstore_0;

  short OP_fstore_1 = BytecodeConstants.JBC_fstore_1;

  short OP_fstore_2 = BytecodeConstants.JBC_fstore_2;

  short OP_fstore_3 = BytecodeConstants.JBC_fstore_3;

  short OP_dstore_0 = BytecodeConstants.JBC_dstore_0;

  short OP_dstore_1 = BytecodeConstants.JBC_dstore_1;

  short OP_dstore_2 = BytecodeConstants.JBC_dstore_2;

  short OP_dstore_3 = BytecodeConstants.JBC_dstore_3;

  short OP_astore_0 = BytecodeConstants.JBC_astore_0;

  short OP_astore_1 = BytecodeConstants.JBC_astore_1;

  short OP_astore_2 = BytecodeConstants.JBC_astore_2;

  short OP_astore_3 = BytecodeConstants.JBC_astore_3;

  short OP_iastore = BytecodeConstants.JBC_iastore;

  short OP_lastore = BytecodeConstants.JBC_lastore;

  short OP_fastore = BytecodeConstants.JBC_fastore;

  short OP_dastore = BytecodeConstants.JBC_dastore;

  short OP_aastore = BytecodeConstants.JBC_aastore;

  short OP_bastore = BytecodeConstants.JBC_bastore;

  short OP_castore = BytecodeConstants.JBC_castore;

  short OP_sastore = BytecodeConstants.JBC_sastore;

  short OP_pop = BytecodeConstants.JBC_pop;

  short OP_pop2 = BytecodeConstants.JBC_pop2;

  short OP_dup = BytecodeConstants.JBC_dup;

  short OP_dup_x1 = BytecodeConstants.JBC_dup_x1;

  short OP_dup_x2 = BytecodeConstants.JBC_dup_x2;

  short OP_dup2 = BytecodeConstants.JBC_dup2;

  short OP_dup2_x1 = BytecodeConstants.JBC_dup2_x1;

  short OP_dup2_x2 = BytecodeConstants.JBC_dup2_x2;

  short OP_swap = BytecodeConstants.JBC_swap;

  short OP_iadd = BytecodeConstants.JBC_iadd;

  short OP_ladd = BytecodeConstants.JBC_ladd;

  short OP_fadd = BytecodeConstants.JBC_fadd;

  short OP_dadd = BytecodeConstants.JBC_dadd;

  short OP_isub = BytecodeConstants.JBC_isub;

  short OP_lsub = BytecodeConstants.JBC_lsub;

  short OP_fsub = BytecodeConstants.JBC_fsub;

  short OP_dsub = BytecodeConstants.JBC_dsub;

  short OP_imul = BytecodeConstants.JBC_imul;

  short OP_lmul = BytecodeConstants.JBC_lmul;

  short OP_fmul = BytecodeConstants.JBC_fmul;

  short OP_dmul = BytecodeConstants.JBC_dmul;

  short OP_idiv = BytecodeConstants.JBC_idiv;

  short OP_ldiv = BytecodeConstants.JBC_ldiv;

  short OP_fdiv = BytecodeConstants.JBC_fdiv;

  short OP_ddiv = BytecodeConstants.JBC_ddiv;

  short OP_irem = BytecodeConstants.JBC_irem;

  short OP_lrem = BytecodeConstants.JBC_lrem;

  short OP_frem = BytecodeConstants.JBC_frem;

  short OP_drem = BytecodeConstants.JBC_drem;

  short OP_ineg = BytecodeConstants.JBC_ineg;

  short OP_lneg = BytecodeConstants.JBC_lneg;

  short OP_fneg = BytecodeConstants.JBC_fneg;

  short OP_dneg = BytecodeConstants.JBC_dneg;

  short OP_ishl = BytecodeConstants.JBC_ishl;

  short OP_lshl = BytecodeConstants.JBC_lshl;

  short OP_ishr = BytecodeConstants.JBC_ishr;

  short OP_lshr = BytecodeConstants.JBC_lshr;

  short OP_iushr = BytecodeConstants.JBC_iushr;

  short OP_lushr = BytecodeConstants.JBC_lushr;

  short OP_iand = BytecodeConstants.JBC_iand;

  short OP_land = BytecodeConstants.JBC_land;

  short OP_ior = BytecodeConstants.JBC_ior;

  short OP_lor = BytecodeConstants.JBC_lor;

  short OP_ixor = BytecodeConstants.JBC_ixor;

  short OP_lxor = BytecodeConstants.JBC_lxor;

  short OP_iinc = BytecodeConstants.JBC_iinc;

  short OP_i2l = BytecodeConstants.JBC_i2l;

  short OP_i2f = BytecodeConstants.JBC_i2f;

  short OP_i2d = BytecodeConstants.JBC_i2d;

  short OP_l2i = BytecodeConstants.JBC_l2i;

  short OP_l2f = BytecodeConstants.JBC_l2f;

  short OP_l2d = BytecodeConstants.JBC_l2d;

  short OP_f2i = BytecodeConstants.JBC_f2i;

  short OP_f2l = BytecodeConstants.JBC_f2l;

  short OP_f2d = BytecodeConstants.JBC_f2d;

  short OP_d2i = BytecodeConstants.JBC_d2i;

  short OP_d2l = BytecodeConstants.JBC_d2l;

  short OP_d2f = BytecodeConstants.JBC_d2f;

  short OP_i2b = BytecodeConstants.JBC_int2byte;

  short OP_i2c = BytecodeConstants.JBC_int2char;

  short OP_i2s = BytecodeConstants.JBC_int2short;

  short OP_lcmp = BytecodeConstants.JBC_lcmp;

  short OP_fcmpl = BytecodeConstants.JBC_fcmpl;

  short OP_fcmpg = BytecodeConstants.JBC_fcmpg;

  short OP_dcmpl = BytecodeConstants.JBC_dcmpl;

  short OP_dcmpg = BytecodeConstants.JBC_dcmpg;

  short OP_ifeq = BytecodeConstants.JBC_ifeq;

  short OP_ifne = BytecodeConstants.JBC_ifne;

  short OP_iflt = BytecodeConstants.JBC_iflt;

  short OP_ifge = BytecodeConstants.JBC_ifge;

  short OP_ifgt = BytecodeConstants.JBC_ifgt;

  short OP_ifle = BytecodeConstants.JBC_ifle;

  short OP_if_icmpeq = BytecodeConstants.JBC_if_icmpeq;

  short OP_if_icmpne = BytecodeConstants.JBC_if_icmpne;

  short OP_if_icmplt = BytecodeConstants.JBC_if_icmplt;

  short OP_if_icmpge = BytecodeConstants.JBC_if_icmpge;

  short OP_if_icmpgt = BytecodeConstants.JBC_if_icmpgt;

  short OP_if_icmple = BytecodeConstants.JBC_if_icmple;

  short OP_if_acmpeq = BytecodeConstants.JBC_if_acmpeq;

  short OP_if_acmpne = BytecodeConstants.JBC_if_acmpne;

  short OP_goto = BytecodeConstants.JBC_goto;

  short OP_jsr = BytecodeConstants.JBC_jsr;

  short OP_ret = BytecodeConstants.JBC_ret;

  short OP_tableswitch = BytecodeConstants.JBC_tableswitch;

  short OP_lookupswitch = BytecodeConstants.JBC_lookupswitch;

  short OP_ireturn = BytecodeConstants.JBC_ireturn;

  short OP_lreturn = BytecodeConstants.JBC_lreturn;

  short OP_freturn = BytecodeConstants.JBC_freturn;

  short OP_dreturn = BytecodeConstants.JBC_dreturn;

  short OP_areturn = BytecodeConstants.JBC_areturn;

  short OP_return = BytecodeConstants.JBC_return;

  short OP_getstatic = BytecodeConstants.JBC_getstatic;

  short OP_putstatic = BytecodeConstants.JBC_putstatic;

  short OP_getfield = BytecodeConstants.JBC_getfield;

  short OP_putfield = BytecodeConstants.JBC_putfield;

  short OP_invokevirtual = BytecodeConstants.JBC_invokevirtual;

  short OP_invokespecial = BytecodeConstants.JBC_invokespecial;

  short OP_invokestatic = BytecodeConstants.JBC_invokestatic;

  short OP_invokeinterface = BytecodeConstants.JBC_invokeinterface;

  short OP_invokedynamic = BytecodeConstants.JBC_xxxunusedxxx;

  short OP_new = BytecodeConstants.JBC_new;

  short OP_newarray = BytecodeConstants.JBC_newarray;

  short OP_anewarray = BytecodeConstants.JBC_anewarray;

  short OP_arraylength = BytecodeConstants.JBC_arraylength;

  short OP_athrow = BytecodeConstants.JBC_athrow;

  short OP_checkcast = BytecodeConstants.JBC_checkcast;

  short OP_instanceof = BytecodeConstants.JBC_instanceof;

  short OP_monitorenter = BytecodeConstants.JBC_monitorenter;

  short OP_monitorexit = BytecodeConstants.JBC_monitorexit;

  short OP_wide = BytecodeConstants.JBC_wide;

  short OP_multianewarray = BytecodeConstants.JBC_multianewarray;

  short OP_ifnull = BytecodeConstants.JBC_ifnull;

  short OP_ifnonnull = BytecodeConstants.JBC_ifnonnull;

  short OP_goto_w = BytecodeConstants.JBC_goto_w;

  short OP_jsr_w = BytecodeConstants.JBC_jsr_w;

  char ACC_PUBLIC = ClassConstants.ACC_PUBLIC;

  char ACC_PRIVATE = ClassConstants.ACC_PRIVATE;

  char ACC_PROTECTED = ClassConstants.ACC_PROTECTED;

  char ACC_STATIC = ClassConstants.ACC_STATIC;

  char ACC_FINAL = ClassConstants.ACC_FINAL;

  char ACC_SYNCHRONIZED = ClassConstants.ACC_SYNCHRONIZED;

  char ACC_SUPER = ClassConstants.ACC_SUPER;

  char ACC_VOLATILE = ClassConstants.ACC_VOLATILE;

  char ACC_TRANSIENT = ClassConstants.ACC_TRANSIENT;

  char ACC_NATIVE = ClassConstants.ACC_NATIVE;

  char ACC_INTERFACE = ClassConstants.ACC_INTERFACE;

  char ACC_ABSTRACT = ClassConstants.ACC_ABSTRACT;

  char ACC_STRICT = ClassConstants.ACC_STRICT;

  char ACC_SYNTHETIC = 0x1000;

  char ACC_ANNOTATION = 0x2000;

  char ACC_ENUM = 0x4000;

  char ACC_MODULE = 0x8000;

  byte CONSTANT_Utf8 = ClassConstants.CONSTANT_Utf8;

  byte CONSTANT_Integer = ClassConstants.CONSTANT_Integer;

  byte CONSTANT_Float = ClassConstants.CONSTANT_Float;

  byte CONSTANT_Long = ClassConstants.CONSTANT_Long;

  byte CONSTANT_Double = ClassConstants.CONSTANT_Double;

  byte CONSTANT_Class = ClassConstants.CONSTANT_Class;

  byte CONSTANT_String = ClassConstants.CONSTANT_String;

  byte CONSTANT_FieldRef = ClassConstants.CONSTANT_FieldRef;

  byte CONSTANT_MethodRef = ClassConstants.CONSTANT_MethodRef;

  byte CONSTANT_InterfaceMethodRef = ClassConstants.CONSTANT_InterfaceMethodRef;

  byte CONSTANT_NameAndType = ClassConstants.CONSTANT_NameAndType;

  byte CONSTANT_MethodHandle = ClassConstants.CONSTANT_MethodHandle;

  byte CONSTANT_MethodType = ClassConstants.CONSTANT_MethodType;

  byte CONSTANT_Dynamic = ClassConstants.CONSTANT_Dynamic;

  byte CONSTANT_InvokeDynamic = ClassConstants.CONSTANT_InvokeDynamic;

  byte T_BOOLEAN = 4;

  byte T_CHAR = 5;

  byte T_FLOAT = 6;

  byte T_DOUBLE = 7;

  byte T_BYTE = 8;

  byte T_SHORT = 9;

  byte T_INT = 10;

  byte T_LONG = 11;

  String TYPE_boolean = "Z";

  String TYPE_byte = "B";

  String TYPE_int = "I";

  String TYPE_short = "S";

  String TYPE_long = "J";

  String TYPE_float = "F";

  String TYPE_double = "D";

  String TYPE_char = "C";

  String TYPE_void = "V";

  String TYPE_String = "Ljava/lang/String;";

  String TYPE_MethodHandle = "Ljava/lang/invoke/MethodHandle;";

  String TYPE_MethodType = "Ljava/lang/invoke/MethodType;";

  String TYPE_Object = "Ljava/lang/Object;";

  String TYPE_Throwable = "Ljava/lang/Throwable;";

  String TYPE_Class = "Ljava/lang/Class;";

  String TYPE_Exception = "Ljava/lang/Exception;";

  String TYPE_RuntimeException = "Ljava/lang/RuntimeException;";

  String TYPE_Error = "Ljava/lang/Error;";

  /**
   * This represents the type of "null", which can be any object. It is not defined by the JVM spec.
   */
  String TYPE_null = "L;";

  /** This represents a type which is unknown. It is not defined by the JVM spec. */
  String TYPE_unknown = "L?;";

  byte TYPE_int_index = 0;

  byte TYPE_long_index = 1;

  byte TYPE_float_index = 2;

  byte TYPE_double_index = 3;

  byte TYPE_Object_index = 4;

  byte TYPE_byte_index = 5;

  byte TYPE_char_index = 6;

  byte TYPE_short_index = 7;

  byte TYPE_boolean_index = 8;

  String[] indexedTypes = {
    TYPE_int,
    TYPE_long,
    TYPE_float,
    TYPE_double,
    TYPE_Object,
    TYPE_byte,
    TYPE_char,
    TYPE_short,
    TYPE_boolean
  };

  byte[] indexedTypes_T = {T_INT, T_LONG, T_FLOAT, T_DOUBLE, 0, T_BYTE, T_CHAR, T_SHORT, T_BOOLEAN};

  // analyses use these constants to report results

  /**
   * @deprecated use {@code AnalysisResult.NO.ordinal() + 1}
   * @see AnalysisResult#NO
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int NO = AnalysisResult.NO.ordinal() + 1;

  /**
   * @deprecated use {@code AnalysisResult.YES.ordinal() + 1}
   * @see AnalysisResult#YES
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int YES = AnalysisResult.YES.ordinal() + 1;

  /**
   * @deprecated use {@code AnalysisResult.MAYBE.ordinal() + 1}
   * @see AnalysisResult#MAYBE
   */
  @Deprecated(forRemoval = true, since = "1.7.2")
  int MAYBE = AnalysisResult.MAYBE.ordinal() + 1;
}
