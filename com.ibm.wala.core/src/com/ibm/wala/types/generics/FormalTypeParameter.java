/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.types.generics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.TypeReference;

/**
 * Under construction.
 * 
 * FormalTypeParameter: Identifier ClassBound InterfaceBound*
 * 
 * ClassBound: : FieldTypeSignature?
 * 
 * InterfaceBound : FieldTypeSignature
 * 
 * FieldTypeSignature: ClassTypeSignature ArrayTypeSignature TypeVariableSignature
 */
public class FormalTypeParameter extends Signature {

  private final String id;

  private final TypeSignature classBound;

  private final TypeSignature[] interfaceBounds;

  private FormalTypeParameter(String s) throws IllegalArgumentException {
    super(s);
    id = parseForId(s);
    classBound = parseForClassBound(s);
    interfaceBounds = parseForInterfaceBounds(s);
  }

  private static TypeSignature parseForClassBound(String s) {
    int start = s.indexOf(':');
    if (start == s.length() - 1) {
      return null;
    }
    int end = s.indexOf(':', start + 1);
    if (end == start + 1) {
      return null;
    }
    if (end == -1) {
      return TypeSignature.make(s.substring(start + 1));
    } else {
      return TypeSignature.make(s.substring(start + 1, end));
    }
  }

  private static TypeSignature[] parseForInterfaceBounds(String s) {
    List<TypeSignature> list = new LinkedList<>();

    int start = s.indexOf(':');
    if (start == s.length() - 1) {
      return null;
    }
    start = s.indexOf(':', start + 1);
    while (start != -1) {
      int end = s.indexOf(':', start + 1);
      if (end == -1) {
        list.add(TypeSignature.make(s.substring(start + 1)));
      } else {
        list.add(TypeSignature.make(s.substring(start + 1, end)));
      }
      start = s.indexOf(':', start + 1);
    }
    TypeSignature[] result = new TypeSignature[list.size()];
    return list.toArray(result);
  }

  private static String parseForId(String s) throws IllegalArgumentException {
    if (s.indexOf(':') == -1) {
      throw new IllegalArgumentException(s);
    }
    return s.substring(0, s.indexOf(':'));
  }

  public static FormalTypeParameter make(String string) throws IllegalArgumentException {
    if (string == null) {
      throw new IllegalArgumentException("string is null");
    }
    return new FormalTypeParameter(string);
  }

  public TypeSignature getClassBound() {
    return classBound;
  }

  public String getIdentifier() {
    return id;
  }

  /**
   * @param s a string that holds a sequence of formal type parameters beginning at index begin
   * @return the index where the next formal type parameter ends (actually, end +1)
   */
  static int formalTypeParameterEnds(String s, int begin) {
    int result = begin;
    while (s.charAt(result) != ':') {
      result++;
    }
    do {
      assert (s.charAt(result) == ':');
      switch (s.charAt(++result)) {
      case TypeReference.ClassTypeCode: {
        int depth = 0;
        while (s.charAt(result) != ';' || depth > 0) {
          if (s.charAt(result) == '<') {
            depth++;
          }
          if (s.charAt(result) == '>') {
            depth--;
          }
          result++;
        }
        result++;
        break;
      }
      case ':':
        break;
      default:
        assert false : "bad type signature list " + s + " " + (result - 1);
      }
    } while (s.charAt(result) == ':');
    return result;
  }

  static String[] parseForFormalTypeParameters(String s) {
    ArrayList<String> sigs = new ArrayList<>(10);

    int beginToken = 1;
    while (s.charAt(beginToken) != '>') {
      int endToken = FormalTypeParameter.formalTypeParameterEnds(s, beginToken);
      sigs.add(s.substring(beginToken, endToken));
      beginToken = endToken;
    }
    Iterator<String> it = sigs.iterator();
    String[] result = new String[sigs.size()];
    for (int j = 0; j < result.length; j++) {
      result[j] = it.next();
    }
    return result;
  }

  public TypeSignature[] getInterfaceBounds() {
    return interfaceBounds;
  }

  /**
   * @param klass
   * @return the formal type parameters, or null if none
   * @throws InvalidClassFileException
   */
  public static FormalTypeParameter[] getTypeParameters(IClass klass) throws InvalidClassFileException {
    if (klass instanceof ShrikeClass) {
      ShrikeClass sc = (ShrikeClass) klass;
      if (sc.getClassSignature() == null) {
        return null;
      } else {
        return sc.getClassSignature().getFormalTypeParameters();
      }
    } else {
      return null;
    }
  }

  public static FormalTypeParameter[] getTypeParameters(IMethod method) throws InvalidClassFileException {
    if (method instanceof ShrikeCTMethod) {
      ShrikeCTMethod sm = (ShrikeCTMethod) method;
      if (sm.getMethodTypeSignature() == null) {
        return null;
      } else {
        return sm.getMethodTypeSignature().getFormalTypeParameters();
      }
    } else {
      return null;
    }
  }
}
