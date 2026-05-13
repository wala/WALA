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
 * Information about java byte codes that appear in the "code" attribute of a .class file.
 *
 * @author Bowen Alpern
 * @author Derek Lieber
 * @author Stephen Fink
 */
public interface BytecodeConstants {

  // The following mnemonics are defined in Chapter 10 of The Java Virtual Machine Specification.
  //
  int JBC_nop = 0;

  int JBC_aconst_null = 1;

  int JBC_iconst_m1 = 2;

  int JBC_iconst_0 = 3;

  int JBC_iconst_1 = 4;

  int JBC_iconst_2 = 5;

  int JBC_iconst_3 = 6;

  int JBC_iconst_4 = 7;

  int JBC_iconst_5 = 8;

  int JBC_lconst_0 = 9;

  int JBC_lconst_1 = 10;

  int JBC_fconst_0 = 11;

  int JBC_fconst_1 = 12;

  int JBC_fconst_2 = 13;

  int JBC_dconst_0 = 14;

  int JBC_dconst_1 = 15;

  int JBC_bipush = 16;

  int JBC_sipush = 17;

  int JBC_ldc = 18;

  int JBC_ldc_w = 19;

  int JBC_ldc2_w = 20;

  int JBC_iload = 21;

  int JBC_lload = 22;

  int JBC_fload = 23;

  int JBC_dload = 24;

  int JBC_aload = 25;

  int JBC_iload_0 = 26;

  int JBC_iload_1 = 27;

  int JBC_iload_2 = 28;

  int JBC_iload_3 = 29;

  int JBC_lload_0 = 30;

  int JBC_lload_1 = 31;

  int JBC_lload_2 = 32;

  int JBC_lload_3 = 33;

  int JBC_fload_0 = 34;

  int JBC_fload_1 = 35;

  int JBC_fload_2 = 36;

  int JBC_fload_3 = 37;

  int JBC_dload_0 = 38;

  int JBC_dload_1 = 39;

  int JBC_dload_2 = 40;

  int JBC_dload_3 = 41;

  int JBC_aload_0 = 42;

  int JBC_aload_1 = 43;

  int JBC_aload_2 = 44;

  int JBC_aload_3 = 45;

  int JBC_iaload = 46;

  int JBC_laload = 47;

  int JBC_faload = 48;

  int JBC_daload = 49;

  int JBC_aaload = 50;

  int JBC_baload = 51;

  int JBC_caload = 52;

  int JBC_saload = 53;

  int JBC_istore = 54;

  int JBC_lstore = 55;

  int JBC_fstore = 56;

  int JBC_dstore = 57;

  int JBC_astore = 58;

  int JBC_istore_0 = 59;

  int JBC_istore_1 = 60;

  int JBC_istore_2 = 61;

  int JBC_istore_3 = 62;

  int JBC_lstore_0 = 63;

  int JBC_lstore_1 = 64;

  int JBC_lstore_2 = 65;

  int JBC_lstore_3 = 66;

  int JBC_fstore_0 = 67;

  int JBC_fstore_1 = 68;

  int JBC_fstore_2 = 69;

  int JBC_fstore_3 = 70;

  int JBC_dstore_0 = 71;

  int JBC_dstore_1 = 72;

  int JBC_dstore_2 = 73;

  int JBC_dstore_3 = 74;

  int JBC_astore_0 = 75;

  int JBC_astore_1 = 76;

  int JBC_astore_2 = 77;

  int JBC_astore_3 = 78;

  int JBC_iastore = 79;

  int JBC_lastore = 80;

  int JBC_fastore = 81;

  int JBC_dastore = 82;

  int JBC_aastore = 83;

  int JBC_bastore = 84;

  int JBC_castore = 85;

  int JBC_sastore = 86;

  int JBC_pop = 87;

  int JBC_pop2 = 88;

  int JBC_dup = 89;

  int JBC_dup_x1 = 90;

  int JBC_dup_x2 = 91;

  int JBC_dup2 = 92;

  int JBC_dup2_x1 = 93;

  int JBC_dup2_x2 = 94;

  int JBC_swap = 95;

  int JBC_iadd = 96;

  int JBC_ladd = 97;

  int JBC_fadd = 98;

  int JBC_dadd = 99;

  int JBC_isub = 100;

  int JBC_lsub = 101;

  int JBC_fsub = 102;

  int JBC_dsub = 103;

  int JBC_imul = 104;

  int JBC_lmul = 105;

  int JBC_fmul = 106;

  int JBC_dmul = 107;

  int JBC_idiv = 108;

  int JBC_ldiv = 109;

  int JBC_fdiv = 110;

  int JBC_ddiv = 111;

  int JBC_irem = 112;

  int JBC_lrem = 113;

  int JBC_frem = 114;

  int JBC_drem = 115;

  int JBC_ineg = 116;

  int JBC_lneg = 117;

  int JBC_fneg = 118;

  int JBC_dneg = 119;

  int JBC_ishl = 120;

  int JBC_lshl = 121;

  int JBC_ishr = 122;

  int JBC_lshr = 123;

  int JBC_iushr = 124;

  int JBC_lushr = 125;

  int JBC_iand = 126;

  int JBC_land = 127;

  int JBC_ior = 128;

  int JBC_lor = 129;

  int JBC_ixor = 130;

  int JBC_lxor = 131;

  int JBC_iinc = 132;

  int JBC_i2l = 133;

  int JBC_i2f = 134;

  int JBC_i2d = 135;

  int JBC_l2i = 136;

  int JBC_l2f = 137;

  int JBC_l2d = 138;

  int JBC_f2i = 139;

  int JBC_f2l = 140;

  int JBC_f2d = 141;

  int JBC_d2i = 142;

  int JBC_d2l = 143;

  int JBC_d2f = 144;

  int JBC_int2byte = 145;

  int JBC_int2char = 146;

  int JBC_int2short = 147;

  int JBC_lcmp = 148;

  int JBC_fcmpl = 149;

  int JBC_fcmpg = 150;

  int JBC_dcmpl = 151;

  int JBC_dcmpg = 152;

  int JBC_ifeq = 153;

  int JBC_ifne = 154;

  int JBC_iflt = 155;

  int JBC_ifge = 156;

  int JBC_ifgt = 157;

  int JBC_ifle = 158;

  int JBC_if_icmpeq = 159;

  int JBC_if_icmpne = 160;

  int JBC_if_icmplt = 161;

  int JBC_if_icmpge = 162;

  int JBC_if_icmpgt = 163;

  int JBC_if_icmple = 164;

  int JBC_if_acmpeq = 165;

  int JBC_if_acmpne = 166;

  int JBC_goto = 167;

  int JBC_jsr = 168;

  int JBC_ret = 169;

  int JBC_tableswitch = 170;

  int JBC_lookupswitch = 171;

  int JBC_ireturn = 172;

  int JBC_lreturn = 173;

  int JBC_freturn = 174;

  int JBC_dreturn = 175;

  int JBC_areturn = 176;

  int JBC_return = 177;

  int JBC_getstatic = 178;

  int JBC_putstatic = 179;

  int JBC_getfield = 180;

  int JBC_putfield = 181;

  int JBC_invokevirtual = 182;

  int JBC_invokespecial = 183;

  int JBC_invokestatic = 184;

  int JBC_invokeinterface = 185;

  int JBC_xxxunusedxxx = 186;

  int JBC_new = 187;

  int JBC_newarray = 188;

  int JBC_anewarray = 189;

  int JBC_arraylength = 190;

  int JBC_athrow = 191;

  int JBC_checkcast = 192;

  int JBC_instanceof = 193;

  int JBC_monitorenter = 194;

  int JBC_monitorexit = 195;

  int JBC_wide = 196;

  int JBC_multianewarray = 197;

  int JBC_ifnull = 198;

  int JBC_ifnonnull = 199;

  int JBC_goto_w = 200;

  int JBC_jsr_w = 201;

  int JBC_impdep1 = 254;

  int JBC_impdep2 = 255;

  // Length of each instruction introduced by the above bytecodes.
  // -1 indicates a variable length instruction.
  // -2 indicates an unused instruction.
  //
  byte[] JBC_length = {
    1, // nop
    1, // aconst_null
    1, // iconst_m1
    1, // iconst_0
    1, // iconst_1
    1, // iconst_2
    1, // iconst_3
    1, // iconst_4
    1, // iconst_5
    1, // lconst_0
    1, // lconst_1
    1, // fconst_0
    1, // fconst_1
    1, // fconst_2
    1, // dconst_0
    1, // dconst_1
    2, // bipush
    3, // sipush
    2, // ldc
    3, // ldc_w
    3, // ldc2_w
    2, // iload
    2, // lload
    2, // fload
    2, // dload
    2, // aload
    1, // iload_0
    1, // iload_1
    1, // iload_2
    1, // iload_3
    1, // lload_0
    1, // lload_1
    1, // lload_2
    1, // lload_3
    1, // fload_0
    1, // fload_1
    1, // fload_2
    1, // fload_3
    1, // dload_0
    1, // dload_1
    1, // dload_2
    1, // dload_3
    1, // aload_0
    1, // aload_1
    1, // aload_2
    1, // aload_3
    1, // iaload
    1, // laload
    1, // faload
    1, // daload
    1, // aaload
    1, // baload
    1, // caload
    1, // saload
    2, // istore
    2, // lstore
    2, // fstore
    2, // dstore
    2, // astore
    1, // istore_0
    1, // istore_1
    1, // istore_2
    1, // istore_3
    1, // lstore_0
    1, // lstore_1
    1, // lstore_2
    1, // lstore_3
    1, // fstore_0
    1, // fstore_1
    1, // fstore_2
    1, // fstore_3
    1, // dstore_0
    1, // dstore_1
    1, // dstore_2
    1, // dstore_3
    1, // astore_0
    1, // astore_1
    1, // astore_2
    1, // astore_3
    1, // iastore
    1, // lastore
    1, // fastore
    1, // dastore
    1, // aastore
    1, // bastore
    1, // castore
    1, // sastore
    1, // pop
    1, // pop2
    1, // dup
    1, // dup_x1
    1, // dup_x2
    1, // dup2
    1, // dup2_x1
    1, // dup2_x2
    1, // swap
    1, // iadd
    1, // ladd
    1, // fadd
    1, // dadd
    1, // isub
    1, // lsub
    1, // fsub
    1, // dsub
    1, // imul
    1, // lmul
    1, // fmul
    1, // dmul
    1, // idiv
    1, // ldiv
    1, // fdiv
    1, // ddiv
    1, // irem
    1, // lrem
    1, // frem
    1, // drem
    1, // ineg
    1, // lneg
    1, // fneg
    1, // dneg
    1, // ishl
    1, // lshl
    1, // ishr
    1, // lshr
    1, // iushr
    1, // lushr
    1, // iand
    1, // land
    1, // ior
    1, // lor
    1, // ixor
    1, // lxor
    3, // iinc
    1, // i2l
    1, // i2f
    1, // i2d
    1, // l2i
    1, // l2f
    1, // l2d
    1, // f2i
    1, // f2l
    1, // f2d
    1, // d2i
    1, // d2l
    1, // d2f
    1, // int2byte
    1, // int2char
    1, // int2short
    1, // lcmp
    1, // fcmpl
    1, // fcmpg
    1, // dcmpl
    1, // dcmpg
    3, // ifeq
    3, // ifne
    3, // iflt
    3, // ifge
    3, // ifgt
    3, // ifle
    3, // if_icmpeq
    3, // if_icmpne
    3, // if_icmplt
    3, // if_icmpge
    3, // if_icmpgt
    3, // if_icmple
    3, // if_acmpeq
    3, // if_acmpne
    3, // goto
    3, // jsr
    2, // ret
    -1, // tableswitch
    -1, // lookupswitch
    1, // ireturn
    1, // lreturn
    1, // freturn
    1, // dreturn
    1, // areturn
    1, // return
    3, // getstatic
    3, // putstatic
    3, // getfield
    3, // putfield
    3, // invokevirtual
    3, // invokenonvirtual
    3, // invokestatic
    5, // invokeinterface
    -2, // xxxunusedxxx
    3, // new
    2, // newarray
    3, // anewarray
    1, // arraylength
    1, // athrow
    3, // checkcast
    3, // instanceof
    1, // monitorenter
    1, // monitorexit
    -1, // wide
    4, // multianewarray
    3, // ifnull
    3, // ifnonnull
    5, // goto_w
    5, // jsr_w
  };

  /** Bytecode names (for debugging/printing) */
  String[] JBC_name = {
    "nop",
    "aconst_null",
    "iconst_m1",
    "iconst_0",
    "iconst_1",
    "iconst_2",
    "iconst_3",
    "iconst_4",
    "iconst_5",
    "lconst_0",
    "lconst_1",
    "fconst_0",
    "fconst_1",
    "fconst_2",
    "dconst_0",
    "dconst_1",
    "bipush",
    "sipush",
    "ldc",
    "ldc_w",
    "ldc2_w",
    "iload",
    "lload",
    "fload",
    "dload",
    "aload",
    "iload_0",
    "iload_1",
    "iload_2",
    "iload_3",
    "lload_0",
    "lload_1",
    "lload_2",
    "lload_3",
    "fload_0",
    "fload_1",
    "fload_2",
    " fload_3",
    " dload_0",
    " dload_1",
    " dload_2",
    " dload_3",
    " aload_0",
    " aload_1",
    " aload_2",
    " aload_3",
    " iaload",
    " laload",
    " faload",
    " daload",
    " aaload",
    " baload",
    " caload",
    " saload",
    " istore",
    " lstore",
    " fstore",
    " dstore",
    " astore",
    " istore_0",
    " istore_1",
    " istore_2",
    " istore_3",
    " lstore_0",
    " lstore_1",
    " lstore_2",
    " lstore_3",
    " fstore_0",
    " fstore_1",
    " fstore_2",
    " fstore_3",
    " dstore_0",
    " dstore_1",
    " dstore_2",
    " dstore_3",
    " astore_0",
    " astore_1",
    " astore_2",
    " astore_3",
    "iastore",
    "lastore",
    "fastore",
    "dastore",
    "aastore",
    "bastore",
    "castore",
    "sastore",
    "pop",
    "pop2",
    "dup",
    "dup_x1",
    "dup_x2",
    "dup2",
    "dup2_x1",
    "dup2_x2",
    "swap",
    "iadd",
    "ladd",
    "fadd",
    "dadd",
    "isub",
    "lsub",
    "fsub",
    "dsub",
    "imul",
    "lmul",
    "fmul",
    "dmul",
    "idiv",
    "ldiv",
    "fdiv",
    "ddiv",
    "irem",
    "lrem",
    "frem",
    "drem",
    "ineg",
    "lneg",
    "fneg",
    "dneg",
    "ishl",
    "lshl",
    "ishr",
    "lshr",
    "iushr",
    "lushr",
    "iand",
    "land",
    "ior",
    "lor",
    "ixor",
    "lxor",
    "iinc",
    "i2l",
    "i2f",
    "i2d",
    "l2i",
    "l2f",
    "l2d",
    "f2i",
    "f2l",
    "f2d",
    "d2i",
    "d2l",
    "d2f",
    "int2byte",
    "int2char",
    "int2short",
    "lcmp",
    "fcmpl",
    "fcmpg",
    "dcmpl",
    "dcmpg",
    "ifeq",
    "ifne",
    "iflt",
    "ifge",
    "ifgt",
    "ifle",
    "if_icmpeq",
    "if_icmpne",
    "if_icmplt",
    "if_icmpge",
    "if_icmpgt",
    "if_icmple",
    "if_acmpeq",
    "if_acmpne",
    "goto",
    "jsr",
    "ret",
    " tableswitch",
    " lookupswitch",
    "ireturn",
    "lreturn",
    "freturn",
    "dreturn",
    "areturn",
    "return",
    "getstatic",
    "putstatic",
    "getfield",
    "putfield",
    "invokevirtual",
    "invokenonvirtual",
    "invokestatic",
    "invokeinterface",
    " xxxunusedxxx",
    "new",
    "newarray",
    "anewarray",
    "arraylength",
    "athrow",
    "checkcast",
    "instanceof",
    "monitorenter",
    "monitorexit",
    " wide",
    "multianewarray",
    "ifnull",
    "ifnonnull",
    "goto_w",
    "jsr_w",
  };
}
