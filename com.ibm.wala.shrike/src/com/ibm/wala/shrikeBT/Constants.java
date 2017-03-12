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

/**
 * This interface defines a bunch of constants from the JVM spec. It also defines some constants we need for other purposes.
 * 
 * Here are the JVM constants:
 * <ul>
 * <li>The OP_ constants define the JVM instruction opcodes.
 * <li>The ACC_ constants define the accessibility flags for classes, fields and methods.
 * <li>The CONSTANT_ constants define the constant pool item types.
 * <li>The T_ constants define the types of arrays that can be created by OP_newarray.
 * <li>The TYPE_ constants define the string representations of various JVM types. Two special non-JVM types are defined, TYPE_null
 * and TYPE_unknown, as noted below.
 * </ul>
 * 
 * Non-JVM constants:
 * <ul>
 * <li>The OPR_ constants define the set of operators present in JVM instructions.
 * <li>The operatorNames array gives the string names of those operators.
 * <li>The TYPE_..._index constants define numeric representations of the JVM base types.
 * <li>The indexedTypes array maps those numeric representations to their official string representations.
 * <li>The indexedTypes_T array maps those numeric representations to the corresponding T_ constant.
 * </ul>
 */
public interface Constants {

  public static final short OP_nop = 0;

  public static final short OP_aconst_null = 1;

  public static final short OP_iconst_m1 = 2;

  public static final short OP_iconst_0 = 3;

  public static final short OP_iconst_1 = 4;

  public static final short OP_iconst_2 = 5;

  public static final short OP_iconst_3 = 6;

  public static final short OP_iconst_4 = 7;

  public static final short OP_iconst_5 = 8;

  public static final short OP_lconst_0 = 9;

  public static final short OP_lconst_1 = 10;

  public static final short OP_fconst_0 = 11;

  public static final short OP_fconst_1 = 12;

  public static final short OP_fconst_2 = 13;

  public static final short OP_dconst_0 = 14;

  public static final short OP_dconst_1 = 15;

  public static final short OP_bipush = 16;

  public static final short OP_sipush = 17;

  public static final short OP_ldc = 18;

  public static final short OP_ldc_w = 19;

  public static final short OP_ldc2_w = 20;

  public static final short OP_iload = 21;

  public static final short OP_lload = 22;

  public static final short OP_fload = 23;

  public static final short OP_dload = 24;

  public static final short OP_aload = 25;

  public static final short OP_iload_0 = 26;

  public static final short OP_iload_1 = 27;

  public static final short OP_iload_2 = 28;

  public static final short OP_iload_3 = 29;

  public static final short OP_lload_0 = 30;

  public static final short OP_lload_1 = 31;

  public static final short OP_lload_2 = 32;

  public static final short OP_lload_3 = 33;

  public static final short OP_fload_0 = 34;

  public static final short OP_fload_1 = 35;

  public static final short OP_fload_2 = 36;

  public static final short OP_fload_3 = 37;

  public static final short OP_dload_0 = 38;

  public static final short OP_dload_1 = 39;

  public static final short OP_dload_2 = 40;

  public static final short OP_dload_3 = 41;

  public static final short OP_aload_0 = 42;

  public static final short OP_aload_1 = 43;

  public static final short OP_aload_2 = 44;

  public static final short OP_aload_3 = 45;

  public static final short OP_iaload = 46;

  public static final short OP_laload = 47;

  public static final short OP_faload = 48;

  public static final short OP_daload = 49;

  public static final short OP_aaload = 50;

  public static final short OP_baload = 51;

  public static final short OP_caload = 52;

  public static final short OP_saload = 53;

  public static final short OP_istore = 54;

  public static final short OP_lstore = 55;

  public static final short OP_fstore = 56;

  public static final short OP_dstore = 57;

  public static final short OP_astore = 58;

  public static final short OP_istore_0 = 59;

  public static final short OP_istore_1 = 60;

  public static final short OP_istore_2 = 61;

  public static final short OP_istore_3 = 62;

  public static final short OP_lstore_0 = 63;

  public static final short OP_lstore_1 = 64;

  public static final short OP_lstore_2 = 65;

  public static final short OP_lstore_3 = 66;

  public static final short OP_fstore_0 = 67;

  public static final short OP_fstore_1 = 68;

  public static final short OP_fstore_2 = 69;

  public static final short OP_fstore_3 = 70;

  public static final short OP_dstore_0 = 71;

  public static final short OP_dstore_1 = 72;

  public static final short OP_dstore_2 = 73;

  public static final short OP_dstore_3 = 74;

  public static final short OP_astore_0 = 75;

  public static final short OP_astore_1 = 76;

  public static final short OP_astore_2 = 77;

  public static final short OP_astore_3 = 78;

  public static final short OP_iastore = 79;

  public static final short OP_lastore = 80;

  public static final short OP_fastore = 81;

  public static final short OP_dastore = 82;

  public static final short OP_aastore = 83;

  public static final short OP_bastore = 84;

  public static final short OP_castore = 85;

  public static final short OP_sastore = 86;

  public static final short OP_pop = 87;

  public static final short OP_pop2 = 88;

  public static final short OP_dup = 89;

  public static final short OP_dup_x1 = 90;

  public static final short OP_dup_x2 = 91;

  public static final short OP_dup2 = 92;

  public static final short OP_dup2_x1 = 93;

  public static final short OP_dup2_x2 = 94;

  public static final short OP_swap = 95;

  public static final short OP_iadd = 96;

  public static final short OP_ladd = 97;

  public static final short OP_fadd = 98;

  public static final short OP_dadd = 99;

  public static final short OP_isub = 100;

  public static final short OP_lsub = 101;

  public static final short OP_fsub = 102;

  public static final short OP_dsub = 103;

  public static final short OP_imul = 104;

  public static final short OP_lmul = 105;

  public static final short OP_fmul = 106;

  public static final short OP_dmul = 107;

  public static final short OP_idiv = 108;

  public static final short OP_ldiv = 109;

  public static final short OP_fdiv = 110;

  public static final short OP_ddiv = 111;

  public static final short OP_irem = 112;

  public static final short OP_lrem = 113;

  public static final short OP_frem = 114;

  public static final short OP_drem = 115;

  public static final short OP_ineg = 116;

  public static final short OP_lneg = 117;

  public static final short OP_fneg = 118;

  public static final short OP_dneg = 119;

  public static final short OP_ishl = 120;

  public static final short OP_lshl = 121;

  public static final short OP_ishr = 122;

  public static final short OP_lshr = 123;

  public static final short OP_iushr = 124;

  public static final short OP_lushr = 125;

  public static final short OP_iand = 126;

  public static final short OP_land = 127;

  public static final short OP_ior = 128;

  public static final short OP_lor = 129;

  public static final short OP_ixor = 130;

  public static final short OP_lxor = 131;

  public static final short OP_iinc = 132;

  public static final short OP_i2l = 133;

  public static final short OP_i2f = 134;

  public static final short OP_i2d = 135;

  public static final short OP_l2i = 136;

  public static final short OP_l2f = 137;

  public static final short OP_l2d = 138;

  public static final short OP_f2i = 139;

  public static final short OP_f2l = 140;

  public static final short OP_f2d = 141;

  public static final short OP_d2i = 142;

  public static final short OP_d2l = 143;

  public static final short OP_d2f = 144;

  public static final short OP_i2b = 145;

  public static final short OP_i2c = 146;

  public static final short OP_i2s = 147;

  public static final short OP_lcmp = 148;

  public static final short OP_fcmpl = 149;

  public static final short OP_fcmpg = 150;

  public static final short OP_dcmpl = 151;

  public static final short OP_dcmpg = 152;

  public static final short OP_ifeq = 153;

  public static final short OP_ifne = 154;

  public static final short OP_iflt = 155;

  public static final short OP_ifge = 156;

  public static final short OP_ifgt = 157;

  public static final short OP_ifle = 158;

  public static final short OP_if_icmpeq = 159;

  public static final short OP_if_icmpne = 160;

  public static final short OP_if_icmplt = 161;

  public static final short OP_if_icmpge = 162;

  public static final short OP_if_icmpgt = 163;

  public static final short OP_if_icmple = 164;

  public static final short OP_if_acmpeq = 165;

  public static final short OP_if_acmpne = 166;

  public static final short OP_goto = 167;

  public static final short OP_jsr = 168;

  public static final short OP_ret = 169;

  public static final short OP_tableswitch = 170;

  public static final short OP_lookupswitch = 171;

  public static final short OP_ireturn = 172;

  public static final short OP_lreturn = 173;

  public static final short OP_freturn = 174;

  public static final short OP_dreturn = 175;

  public static final short OP_areturn = 176;

  public static final short OP_return = 177;

  public static final short OP_getstatic = 178;

  public static final short OP_putstatic = 179;

  public static final short OP_getfield = 180;

  public static final short OP_putfield = 181;

  public static final short OP_invokevirtual = 182;

  public static final short OP_invokespecial = 183;

  public static final short OP_invokestatic = 184;

  public static final short OP_invokeinterface = 185;

  public static final short OP_invokedynamic = 186;

  public static final short OP_new = 187;

  public static final short OP_newarray = 188;

  public static final short OP_anewarray = 189;

  public static final short OP_arraylength = 190;

  public static final short OP_athrow = 191;

  public static final short OP_checkcast = 192;

  public static final short OP_instanceof = 193;

  public static final short OP_monitorenter = 194;

  public static final short OP_monitorexit = 195;

  public static final short OP_wide = 196;

  public static final short OP_multianewarray = 197;

  public static final short OP_ifnull = 198;

  public static final short OP_ifnonnull = 199;

  public static final short OP_goto_w = 200;

  public static final short OP_jsr_w = 201;

  public static final short ACC_PUBLIC = 0x1;

  public static final short ACC_PRIVATE = 0x2;

  public static final short ACC_PROTECTED = 0x4;

  public static final short ACC_STATIC = 0x8;

  public static final short ACC_FINAL = 0x10;

  public static final short ACC_SYNCHRONIZED = 0x20;

  public static final short ACC_SUPER = 0x20;

  public static final short ACC_VOLATILE = 0x40;

  public static final short ACC_TRANSIENT = 0x80;

  public static final short ACC_NATIVE = 0x100;

  public static final short ACC_INTERFACE = 0x200;

  public static final short ACC_ABSTRACT = 0x400;

  public static final short ACC_STRICT = 0x800;

  public static final byte CONSTANT_Utf8 = 1;

  public static final byte CONSTANT_Integer = 3;

  public static final byte CONSTANT_Float = 4;

  public static final byte CONSTANT_Long = 5;

  public static final byte CONSTANT_Double = 6;

  public static final byte CONSTANT_Class = 7;

  public static final byte CONSTANT_String = 8;

  public static final byte CONSTANT_FieldRef = 9;

  public static final byte CONSTANT_MethodRef = 10;

  public static final byte CONSTANT_InterfaceMethodRef = 11;

  public static final byte CONSTANT_NameAndType = 12;
  
  public static final byte CONSTANT_MethodHandle = 15;
  
  public static final byte CONSTANT_MethodType = 16;

  public static final byte CONSTANT_InvokeDynamic = 18;

  public static final byte T_BOOLEAN = 4;

  public static final byte T_CHAR = 5;

  public static final byte T_FLOAT = 6;

  public static final byte T_DOUBLE = 7;

  public static final byte T_BYTE = 8;

  public static final byte T_SHORT = 9;

  public static final byte T_INT = 10;

  public static final byte T_LONG = 11;

  public static final String TYPE_boolean = "Z";

  public static final String TYPE_byte = "B";

  public static final String TYPE_int = "I";

  public static final String TYPE_short = "S";

  public static final String TYPE_long = "J";

  public static final String TYPE_float = "F";

  public static final String TYPE_double = "D";

  public static final String TYPE_char = "C";

  public static final String TYPE_void = "V";

  public static final String TYPE_String = "Ljava/lang/String;";

  public static final String TYPE_MethodHandle = "Ljava/lang/invoke/MethodHandle;";

  public static final String TYPE_MethodType = "Ljava/lang/invoke/MethodType;";

  public static final String TYPE_Object = "Ljava/lang/Object;";

  public static final String TYPE_Throwable = "Ljava/lang/Throwable;";

  public static final String TYPE_Class = "Ljava/lang/Class;";

  public static final String TYPE_Exception = "Ljava/lang/Exception;";

  public static final String TYPE_RuntimeException = "Ljava/lang/RuntimeException;";

  public static final String TYPE_Error = "Ljava/lang/Error;";

  /**
   * This represents the type of "null", which can be any object. It is not defined by the JVM spec.
   */
  public static final String TYPE_null = "L;";

  /** This represents a type which is unknown. It is not defined by the JVM spec. */
  public static final String TYPE_unknown = "L?;";

  public static final byte TYPE_int_index = 0;

  public static final byte TYPE_long_index = 1;

  public static final byte TYPE_float_index = 2;

  public static final byte TYPE_double_index = 3;

  public static final byte TYPE_Object_index = 4;

  public static final byte TYPE_byte_index = 5;

  public static final byte TYPE_char_index = 6;

  public static final byte TYPE_short_index = 7;

  public static final byte TYPE_boolean_index = 8;

  public static final String[] indexedTypes = { TYPE_int, TYPE_long, TYPE_float, TYPE_double, TYPE_Object, TYPE_byte, TYPE_char,
      TYPE_short, TYPE_boolean };

  public static final byte[] indexedTypes_T = { T_INT, T_LONG, T_FLOAT, T_DOUBLE, 0, T_BYTE, T_CHAR, T_SHORT, T_BOOLEAN };

  // these constants are used by analyses to report results
  public static final int NO = 1;

  public static final int YES = 2;

  public static final int MAYBE = 3;
}
