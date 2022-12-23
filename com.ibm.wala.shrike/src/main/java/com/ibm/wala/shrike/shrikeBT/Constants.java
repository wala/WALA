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

/**
 * This interface defines a bunch of constants from the JVM spec. It also defines some constants we
 * need for other purposes.
 *
 * <p>Here are the JVM constants:
 *
 * <ul>
 *   <li>The OP_ constants define the JVM instruction opcodes.
 *   <li>The ACC_ constants define the accessibility flags for classes, fields and methods.
 *   <li>The CONSTANT_ constants define the constant pool item types.
 *   <li>The T_ constants define the types of arrays that can be created by OP_newarray.
 *   <li>The TYPE_ constants define the string representations of various JVM types. Two special
 *       non-JVM types are defined, TYPE_null and TYPE_unknown, as noted below.
 * </ul>
 *
 * Non-JVM constants:
 *
 * <ul>
 *   <li>The OPR_ constants define the set of operators present in JVM instructions.
 *   <li>The operatorNames array gives the string names of those operators.
 *   <li>The TYPE_..._index constants define numeric representations of the JVM base types.
 *   <li>The indexedTypes array maps those numeric representations to their official string
 *       representations.
 *   <li>The indexedTypes_T array maps those numeric representations to the corresponding T_
 *       constant.
 * </ul>
 */
public interface Constants {

  short OP_nop = 0;

  short OP_aconst_null = 1;

  short OP_iconst_m1 = 2;

  short OP_iconst_0 = 3;

  short OP_iconst_1 = 4;

  short OP_iconst_2 = 5;

  short OP_iconst_3 = 6;

  short OP_iconst_4 = 7;

  short OP_iconst_5 = 8;

  short OP_lconst_0 = 9;

  short OP_lconst_1 = 10;

  short OP_fconst_0 = 11;

  short OP_fconst_1 = 12;

  short OP_fconst_2 = 13;

  short OP_dconst_0 = 14;

  short OP_dconst_1 = 15;

  short OP_bipush = 16;

  short OP_sipush = 17;

  short OP_ldc = 18;

  short OP_ldc_w = 19;

  short OP_ldc2_w = 20;

  short OP_iload = 21;

  short OP_lload = 22;

  short OP_fload = 23;

  short OP_dload = 24;

  short OP_aload = 25;

  short OP_iload_0 = 26;

  short OP_iload_1 = 27;

  short OP_iload_2 = 28;

  short OP_iload_3 = 29;

  short OP_lload_0 = 30;

  short OP_lload_1 = 31;

  short OP_lload_2 = 32;

  short OP_lload_3 = 33;

  short OP_fload_0 = 34;

  short OP_fload_1 = 35;

  short OP_fload_2 = 36;

  short OP_fload_3 = 37;

  short OP_dload_0 = 38;

  short OP_dload_1 = 39;

  short OP_dload_2 = 40;

  short OP_dload_3 = 41;

  short OP_aload_0 = 42;

  short OP_aload_1 = 43;

  short OP_aload_2 = 44;

  short OP_aload_3 = 45;

  short OP_iaload = 46;

  short OP_laload = 47;

  short OP_faload = 48;

  short OP_daload = 49;

  short OP_aaload = 50;

  short OP_baload = 51;

  short OP_caload = 52;

  short OP_saload = 53;

  short OP_istore = 54;

  short OP_lstore = 55;

  short OP_fstore = 56;

  short OP_dstore = 57;

  short OP_astore = 58;

  short OP_istore_0 = 59;

  short OP_istore_1 = 60;

  short OP_istore_2 = 61;

  short OP_istore_3 = 62;

  short OP_lstore_0 = 63;

  short OP_lstore_1 = 64;

  short OP_lstore_2 = 65;

  short OP_lstore_3 = 66;

  short OP_fstore_0 = 67;

  short OP_fstore_1 = 68;

  short OP_fstore_2 = 69;

  short OP_fstore_3 = 70;

  short OP_dstore_0 = 71;

  short OP_dstore_1 = 72;

  short OP_dstore_2 = 73;

  short OP_dstore_3 = 74;

  short OP_astore_0 = 75;

  short OP_astore_1 = 76;

  short OP_astore_2 = 77;

  short OP_astore_3 = 78;

  short OP_iastore = 79;

  short OP_lastore = 80;

  short OP_fastore = 81;

  short OP_dastore = 82;

  short OP_aastore = 83;

  short OP_bastore = 84;

  short OP_castore = 85;

  short OP_sastore = 86;

  short OP_pop = 87;

  short OP_pop2 = 88;

  short OP_dup = 89;

  short OP_dup_x1 = 90;

  short OP_dup_x2 = 91;

  short OP_dup2 = 92;

  short OP_dup2_x1 = 93;

  short OP_dup2_x2 = 94;

  short OP_swap = 95;

  short OP_iadd = 96;

  short OP_ladd = 97;

  short OP_fadd = 98;

  short OP_dadd = 99;

  short OP_isub = 100;

  short OP_lsub = 101;

  short OP_fsub = 102;

  short OP_dsub = 103;

  short OP_imul = 104;

  short OP_lmul = 105;

  short OP_fmul = 106;

  short OP_dmul = 107;

  short OP_idiv = 108;

  short OP_ldiv = 109;

  short OP_fdiv = 110;

  short OP_ddiv = 111;

  short OP_irem = 112;

  short OP_lrem = 113;

  short OP_frem = 114;

  short OP_drem = 115;

  short OP_ineg = 116;

  short OP_lneg = 117;

  short OP_fneg = 118;

  short OP_dneg = 119;

  short OP_ishl = 120;

  short OP_lshl = 121;

  short OP_ishr = 122;

  short OP_lshr = 123;

  short OP_iushr = 124;

  short OP_lushr = 125;

  short OP_iand = 126;

  short OP_land = 127;

  short OP_ior = 128;

  short OP_lor = 129;

  short OP_ixor = 130;

  short OP_lxor = 131;

  short OP_iinc = 132;

  short OP_i2l = 133;

  short OP_i2f = 134;

  short OP_i2d = 135;

  short OP_l2i = 136;

  short OP_l2f = 137;

  short OP_l2d = 138;

  short OP_f2i = 139;

  short OP_f2l = 140;

  short OP_f2d = 141;

  short OP_d2i = 142;

  short OP_d2l = 143;

  short OP_d2f = 144;

  short OP_i2b = 145;

  short OP_i2c = 146;

  short OP_i2s = 147;

  short OP_lcmp = 148;

  short OP_fcmpl = 149;

  short OP_fcmpg = 150;

  short OP_dcmpl = 151;

  short OP_dcmpg = 152;

  short OP_ifeq = 153;

  short OP_ifne = 154;

  short OP_iflt = 155;

  short OP_ifge = 156;

  short OP_ifgt = 157;

  short OP_ifle = 158;

  short OP_if_icmpeq = 159;

  short OP_if_icmpne = 160;

  short OP_if_icmplt = 161;

  short OP_if_icmpge = 162;

  short OP_if_icmpgt = 163;

  short OP_if_icmple = 164;

  short OP_if_acmpeq = 165;

  short OP_if_acmpne = 166;

  short OP_goto = 167;

  short OP_jsr = 168;

  short OP_ret = 169;

  short OP_tableswitch = 170;

  short OP_lookupswitch = 171;

  short OP_ireturn = 172;

  short OP_lreturn = 173;

  short OP_freturn = 174;

  short OP_dreturn = 175;

  short OP_areturn = 176;

  short OP_return = 177;

  short OP_getstatic = 178;

  short OP_putstatic = 179;

  short OP_getfield = 180;

  short OP_putfield = 181;

  short OP_invokevirtual = 182;

  short OP_invokespecial = 183;

  short OP_invokestatic = 184;

  short OP_invokeinterface = 185;

  short OP_invokedynamic = 186;

  short OP_new = 187;

  short OP_newarray = 188;

  short OP_anewarray = 189;

  short OP_arraylength = 190;

  short OP_athrow = 191;

  short OP_checkcast = 192;

  short OP_instanceof = 193;

  short OP_monitorenter = 194;

  short OP_monitorexit = 195;

  short OP_wide = 196;

  short OP_multianewarray = 197;

  short OP_ifnull = 198;

  short OP_ifnonnull = 199;

  short OP_goto_w = 200;

  short OP_jsr_w = 201;

  char ACC_PUBLIC = 0x1;

  char ACC_PRIVATE = 0x2;

  char ACC_PROTECTED = 0x4;

  char ACC_STATIC = 0x8;

  char ACC_FINAL = 0x10;

  char ACC_SYNCHRONIZED = 0x20;

  char ACC_SUPER = 0x20;

  char ACC_VOLATILE = 0x40;

  char ACC_TRANSIENT = 0x80;

  char ACC_NATIVE = 0x100;

  char ACC_INTERFACE = 0x200;

  char ACC_ABSTRACT = 0x400;

  char ACC_STRICT = 0x800;

  char ACC_SYNTHETIC = 0x1000;

  char ACC_ANNOTATION = 0x2000;

  char ACC_ENUM = 0x4000;

  char ACC_MODULE = 0x8000;

  byte CONSTANT_Utf8 = 1;

  byte CONSTANT_Integer = 3;

  byte CONSTANT_Float = 4;

  byte CONSTANT_Long = 5;

  byte CONSTANT_Double = 6;

  byte CONSTANT_Class = 7;

  byte CONSTANT_String = 8;

  byte CONSTANT_FieldRef = 9;

  byte CONSTANT_MethodRef = 10;

  byte CONSTANT_InterfaceMethodRef = 11;

  byte CONSTANT_NameAndType = 12;

  byte CONSTANT_MethodHandle = 15;

  byte CONSTANT_MethodType = 16;

  byte CONSTANT_InvokeDynamic = 18;

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

  // these constants are used by analyses to report results
  int NO = 1;

  int YES = 2;

  int MAYBE = 3;
}
