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
package com.ibm.wala.shrikeCT;

/**
 * This interface defines class file constants used by ShrikeCT. The names and values are taken directly from the JVM spec.
 */
public interface ClassConstants {
  public static final int MAGIC = 0xCAFEBABE;

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
  
  public static final byte REF_getField = 1;
  
  public static final byte REF_getStatic = 2;
  
  public static final byte REF_putField = 3;
  
  public static final byte REF_putStatic = 4;
  
  public static final byte REF_invokeVirtual = 5;
  
  public static final byte REF_invokeStatic = 6;
  
  public static final byte REF_invokeSpecial = 7;
  
  public static final byte REF_newInvokeSpecial = 8;
  
  public static final byte REF_invokeInterface = 9;
  
}
