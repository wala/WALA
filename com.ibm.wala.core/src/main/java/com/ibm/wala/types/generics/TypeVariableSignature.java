/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.types.generics;

import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.debug.Assertions;

/**
 * TypeVariableSignature: T identifier ;
 *
 * @author sjfink
 */
public class TypeVariableSignature extends TypeSignature {

  private TypeVariableSignature(String s) throws IllegalArgumentException {
    super(s);
    if (s.length() == 0) {
      throw new IllegalArgumentException();
    }
    if (s.charAt(s.length() - 1) != ';') {
      throw new IllegalArgumentException(s);
    }
  }

  public static TypeVariableSignature make(String s) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    return new TypeVariableSignature(s);
  }

  @Override
  public boolean isClassTypeSignature() {
    return false;
  }

  @Override
  public boolean isTypeVariable() {
    return true;
  }

  @Override
  public boolean isArrayTypeSignature() {
    return false;
  }

  public String getIdentifier() {
    return rawString().substring(1, rawString().length() - 1);
  }

  @Override
  public boolean isBaseType() {
    return false;
  }

  /** @return -1 if there is no match */
  public static int getTypeVariablePosition(TypeVariableSignature v, ShrikeClass klass)
      throws IllegalArgumentException {
    if (klass == null) {
      throw new IllegalArgumentException("klass cannot be null");
    }

    try {
      ClassSignature sig = klass.getClassSignature();
      if (sig == null) {
        return -1;
      }
      FormalTypeParameter[] fp = sig.getFormalTypeParameters();
      if (fp == null) {
        return -1;
      }
      for (int i = 0; i < fp.length; i++) {
        FormalTypeParameter f = fp[i];
        if (f.getIdentifier().equals(v.getIdentifier())) {
          return i;
        }
      }
      //      System.err.println("sig : " + sig);
      //      System.err.println("fp : " + fp.length);
      //      for (FormalTypeParameter f : fp) {
      //        System.err.println(f);
      //      }
      //      Assertions.UNREACHABLE("did not find " + v + " in " + klass );
      return -1;
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return -1;
    }
  }
}
