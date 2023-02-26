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

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;

/**
 * UNDER CONSTRUCTION.
 *
 * <p>MethodTypeSignature: FormalTypeParameters? (TypeSignature*) ReturnType ThrowsSignature*
 *
 * <p>ReturnType: TypeSignature
 *
 * @author sjfink
 */
public class MethodTypeSignature extends Signature {

  private MethodTypeSignature(String s) {
    super(s);
  }

  public static MethodTypeSignature make(String genericsSignature) throws IllegalArgumentException {
    if (genericsSignature == null) {
      throw new IllegalArgumentException("genericsSignature is null");
    }
    if (genericsSignature.length() == 0) {
      throw new IllegalArgumentException();
    }
    return new MethodTypeSignature(genericsSignature);
  }

  /** @return null if no arguments */
  public TypeSignature[] getArguments() {
    String typeSig = rawString().replaceAll(".*\\(", "\\(").replaceAll("\\).*", "\\)");
    String[] args = TypeSignature.parseForTypeSignatures(typeSig);
    if (args == null) {
      return null;
    }
    TypeSignature[] result = new TypeSignature[args.length];
    for (int i = 0; i < args.length; i++) {
      result[i] = TypeSignature.make(args[i]);
    }
    return result;
  }

  public TypeSignature getReturnType() {
    String rtString = rawString().substring(rawString().indexOf(')') + 1);
    return TypeSignature.make(rtString);
  }

  public FormalTypeParameter[] getFormalTypeParameters() {
    if (rawString().charAt(0) != '<') {
      // no formal type parameters
      return null;
    }
    int index = endOfFormalTypeParameters();
    String[] args =
        FormalTypeParameter.parseForFormalTypeParameters(rawString().substring(0, index));
    FormalTypeParameter[] result = new FormalTypeParameter[args.length];
    for (int i = 0; i < args.length; i++) {
      result[i] = FormalTypeParameter.make(args[i]);
    }
    return result;
  }

  private int endOfFormalTypeParameters() {
    if (rawString().charAt(0) != '<') {
      return 0;
    }
    int i = 1;
    int depth = 1;
    while (depth > 0) {
      if (rawString().charAt(i) == '>') {
        depth--;
      }
      if (rawString().charAt(i) == '<') {
        depth++;
      }
      i++;
    }
    return i;
  }

  /** @return {@link TypeSignature} for arguments, which includes information about generic types */
  public static TypeSignature[] getArguments(IMethod method) throws InvalidClassFileException {
    if (method instanceof ShrikeCTMethod) {
      ShrikeCTMethod sm = (ShrikeCTMethod) method;
      if (sm.getMethodTypeSignature() == null) {
        return null;
      } else {
        return sm.getMethodTypeSignature().getArguments();
      }
    } else {
      return null;
    }
  }

  public static MethodTypeSignature getMethodTypeSignature(IMethod method)
      throws InvalidClassFileException {
    if (method instanceof ShrikeCTMethod) {
      ShrikeCTMethod sm = (ShrikeCTMethod) method;
      return sm.getMethodTypeSignature();
    } else {
      return null;
    }
  }
}
