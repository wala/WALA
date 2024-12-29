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

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * UNDER CONSTRUCTION.
 *
 * <pre> TypeSignature: FieldTypeSignature BaseType (code for a primitive)
 *
 * FieldTypeSignature: ClassTypeSignature ArrayTypeSignature TypeVariableSignature
 *
 * TypeVariableSignature: T identifier ;
 *
 * </pre>
 *
 * @author sjfink
 */
public abstract class TypeSignature extends Signature {

  TypeSignature(String s) {
    super(s);
  }

  public static TypeSignature make(String s) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    if (s.isEmpty()) {
      throw new IllegalArgumentException("illegal empty string s");
    }
    assert !s.isEmpty();
    switch (s.charAt(0)) {
      case TypeReference.VoidTypeCode:
        return BaseType.VOID;
      case TypeReference.BooleanTypeCode:
        return BaseType.BOOLEAN;
      case TypeReference.ByteTypeCode:
        return BaseType.BYTE;
      case TypeReference.ShortTypeCode:
        return BaseType.SHORT;
      case TypeReference.IntTypeCode:
        return BaseType.INT;
      case TypeReference.LongTypeCode:
        return BaseType.LONG;
      case TypeReference.FloatTypeCode:
        return BaseType.FLOAT;
      case TypeReference.DoubleTypeCode:
        return BaseType.DOUBLE;
      case TypeReference.CharTypeCode:
        return BaseType.CHAR;
      case 'L':
        return ClassTypeSignature.makeClassTypeSig(s);
      case 'T':
        return TypeVariableSignature.make(s);
      case TypeReference.ArrayTypeCode:
        return ArrayTypeSignature.make(s);
      default:
        throw new IllegalArgumentException("malformed TypeSignature string:" + s);
    }
  }

  public abstract boolean isTypeVariable();

  public abstract boolean isClassTypeSignature();

  public abstract boolean isArrayTypeSignature();

  public abstract boolean isBaseType();

  /**
   * Split a string of consecutive type signatures (TypeSignature*) into its top-level type
   * signatures. The string should start with either {@code (} or {@code <} and have a respective
   * matching {@code )} or {@code >}.
   *
   * <p>TODO handle wildcards
   *
   * <p>TODO test on all methods in JDK
   *
   * @param typeSigs a string of consecutive type signatures
   * @return an array of top-level type signatures
   */
  public static String[] parseForTypeSignatures(String typeSigs) throws IllegalArgumentException {
    ArrayList<String> sigs = new ArrayList<>(10);
    char start = typeSigs.charAt(0);
    if (start != '(' && start != '<') {
      throw new IllegalArgumentException(
          "illegal start of TypeSignature " + typeSigs + ", must be '(' or '<'");
    }
    if (typeSigs.length() < 2) {
      throw new IllegalArgumentException("illegal string of TypeSignature " + typeSigs);
    }

    int i = 1;
    while (true) {
      switch (typeSigs.charAt(i++)) {
        case TypeReference.VoidTypeCode:
          sigs.add(TypeReference.VoidName.toString());
          continue;
        case TypeReference.BooleanTypeCode:
          sigs.add(TypeReference.BooleanName.toString());
          continue;
        case TypeReference.ByteTypeCode:
          sigs.add(TypeReference.ByteName.toString());
          continue;
        case TypeReference.ShortTypeCode:
          sigs.add(TypeReference.ShortName.toString());
          continue;
        case TypeReference.IntTypeCode:
          sigs.add(TypeReference.IntName.toString());
          continue;
        case TypeReference.LongTypeCode:
          sigs.add(TypeReference.LongName.toString());
          continue;
        case TypeReference.FloatTypeCode:
          sigs.add(TypeReference.FloatName.toString());
          continue;
        case TypeReference.DoubleTypeCode:
          sigs.add(TypeReference.DoubleName.toString());
          continue;
        case TypeReference.CharTypeCode:
          sigs.add(TypeReference.CharName.toString());
          continue;
        case TypeReference.ClassTypeCode:
          {
            int off = i - 1;
            i = getEndIndexOfClassType(typeSigs, i);
            sigs.add(typeSigs.substring(off, i));
            continue;
          }
        case TypeReference.ArrayTypeCode:
          {
            int arrayStart = i - 1;
            while (typeSigs.charAt(i) == TypeReference.ArrayTypeCode) {
              i++;
            }
            switch (typeSigs.charAt(i)) {
              case TypeReference.BooleanTypeCode:
              case TypeReference.ByteTypeCode:
              case TypeReference.ShortTypeCode:
              case TypeReference.IntTypeCode:
              case TypeReference.LongTypeCode:
              case TypeReference.FloatTypeCode:
              case TypeReference.DoubleTypeCode:
              case TypeReference.CharTypeCode:
                sigs.add(typeSigs.substring(arrayStart, i + 1));
                i++;
                break;
              case 'T':
              case TypeReference.ClassTypeCode:
                i++; // to skip 'L' or 'T'
                i = getEndIndexOfClassType(typeSigs, i);
                sigs.add(typeSigs.substring(arrayStart, i));
                break;
              default:
                Assertions.UNREACHABLE("BANG " + typeSigs.charAt(i));
            }
            continue;
          }
        case (byte) 'T':
          { // type variable
            int off = i - 1;
            while (typeSigs.charAt(i++) != ';')
              ;
            sigs.add(typeSigs.substring(off, i));
            continue;
          }
        case (byte) ')': // end of parameter list
        case (byte) '>': // end of type argument list
          int size = sigs.size();
          if (size == 0) {
            return new String[0];
          }
          Iterator<String> it = sigs.iterator();
          String[] result = new String[size];
          for (int j = 0; j < size; j++) {
            result[j] = it.next();
          }
          return result;
        default:
          throw new IllegalArgumentException("bad type signature list " + typeSigs);
      }
    }
  }

  private static int getEndIndexOfClassType(String typeSigs, int i) {
    int depth = 0;
    while (typeSigs.charAt(i++) != ';' || depth > 0) {
      if (typeSigs.charAt(i - 1) == '<') {
        depth++;
      }
      if (typeSigs.charAt(i - 1) == '>') {
        depth--;
      }
    }
    return i;
  }
}
