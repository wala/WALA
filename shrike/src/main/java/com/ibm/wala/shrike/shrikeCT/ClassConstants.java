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
package com.ibm.wala.shrike.shrikeCT;

/**
 * This interface defines class file constants used by ShrikeCT. The names and values are taken
 * directly from the JVM spec.
 */
public interface ClassConstants {
  int MAGIC = 0xCAFEBABE;

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

  byte CONSTANT_Module = 19;

  byte CONSTANT_Package = 20;

  short ACC_PUBLIC = 0x1;

  short ACC_PRIVATE = 0x2;

  short ACC_PROTECTED = 0x4;

  short ACC_STATIC = 0x8;

  short ACC_FINAL = 0x10;

  short ACC_SYNCHRONIZED = 0x20;

  short ACC_SUPER = 0x20;

  short ACC_VOLATILE = 0x40;

  short ACC_TRANSIENT = 0x80;

  short ACC_NATIVE = 0x100;

  short ACC_INTERFACE = 0x200;

  short ACC_ABSTRACT = 0x400;

  short ACC_STRICT = 0x800;

  byte REF_getField = 1;

  byte REF_getStatic = 2;

  byte REF_putField = 3;

  byte REF_putStatic = 4;

  byte REF_invokeVirtual = 5;

  byte REF_invokeStatic = 6;

  byte REF_invokeSpecial = 7;

  byte REF_newInvokeSpecial = 8;

  byte REF_invokeInterface = 9;
}
