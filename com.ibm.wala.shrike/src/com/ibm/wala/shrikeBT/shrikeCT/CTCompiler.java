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
package com.ibm.wala.shrikeBT.shrikeCT;

import java.util.Random;

import com.ibm.wala.shrikeBT.Compiler;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.ConstantPoolParser.ReferenceToken;

/**
 * This class lets you compile ShrikeBT intermediate code into real Java bytecodes using ShrikeCT.
 */
final public class CTCompiler extends Compiler {
  final private ClassWriter cw;

  final private Random random = new Random();

  /**
   * Compile 'md' into the class given by 'cw'.
   */
  private CTCompiler(ClassWriter cw, MethodData md) {
    super(md);
    this.cw = cw;
  }

  @Override
  protected int allocateConstantPoolInteger(int v) {
    return cw.addCPInt(v);
  }

  @Override
  protected int allocateConstantPoolFloat(float v) {
    return cw.addCPFloat(v);
  }

  @Override
  protected int allocateConstantPoolLong(long v) {
    return cw.addCPLong(v);
  }

  @Override
  protected int allocateConstantPoolDouble(double v) {
    return cw.addCPDouble(v);
  }

  @Override
  protected int allocateConstantPoolString(String v) {
    return cw.addCPString(v);
  }

  @Override
  protected int allocateConstantPoolClassType(String c) {
    return cw.addCPClass(convertTypeToClass(c));
  }

  @Override
  protected int allocateConstantPoolMethodType(String c) {
    return cw.addCPMethodType(c);
  }

  @Override
  protected int allocateConstantPoolMethodHandle(ReferenceToken c) {
    return cw.addCPMethodHandle(c);
  }

  @Override
  protected int allocateConstantPoolInvokeDynamic(BootstrapMethod b, String name, String type) {
    return cw.addCPInvokeDynamic(b, name, type);
  }

  /**
   * Convert a JVM type to the internal JVM class name (e.g., Ljava/lang/Object; to java/lang/Object)
   * 
   * @throws IllegalArgumentException if s is null
   */
  public static String convertTypeToClass(String s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    if (s.length() > 0 && s.charAt(0) == 'L') {
      return s.substring(1, s.length() - 1);
    } else {
      return s;
    }
  }

  public static CTCompiler make(ClassWriter cw, MethodData md) {
    if (md == null) {
      throw new IllegalArgumentException("md is null");
    }
    return new CTCompiler(cw, md);
  }

  @Override
  protected int allocateConstantPoolField(String c, String name, String type) {
    return cw.addCPFieldRef(convertTypeToClass(c), name, type);
  }

  @Override
  protected int allocateConstantPoolMethod(String c, String name, String sig) {
    return cw.addCPMethodRef(convertTypeToClass(c), name, sig);
  }

  @Override
  protected int allocateConstantPoolInterfaceMethod(String c, String name, String sig) {
    return cw.addCPInterfaceMethodRef(convertTypeToClass(c), name, sig);
  }

  @Override
  protected String createHelperMethod(boolean isStatic, String sig) {
    long r = Math.abs(random.nextLong());
    String name = "_helper_" + r;

    return name;
  }
}
