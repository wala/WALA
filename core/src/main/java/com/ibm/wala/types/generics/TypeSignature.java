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
    return switch (s.charAt(0)) {
      case TypeReference.VoidTypeCode -> BaseType.VOID;
      case TypeReference.BooleanTypeCode -> BaseType.BOOLEAN;
      case TypeReference.ByteTypeCode -> BaseType.BYTE;
      case TypeReference.ShortTypeCode -> BaseType.SHORT;
      case TypeReference.IntTypeCode -> BaseType.INT;
      case TypeReference.LongTypeCode -> BaseType.LONG;
      case TypeReference.FloatTypeCode -> BaseType.FLOAT;
      case TypeReference.DoubleTypeCode -> BaseType.DOUBLE;
      case TypeReference.CharTypeCode -> BaseType.CHAR;
      case 'L' -> ClassTypeSignature.makeClassTypeSig(s);
      case 'T' -> TypeVariableSignature.make(s);
      case TypeReference.ArrayTypeCode -> ArrayTypeSignature.make(s);
      default -> throw new IllegalArgumentException("malformed TypeSignature string:" + s);
    };
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
        case TypeReference.VoidTypeCode -> sigs.add(TypeReference.VoidName.toString());
        case TypeReference.BooleanTypeCode -> sigs.add(TypeReference.BooleanName.toString());
        case TypeReference.ByteTypeCode -> sigs.add(TypeReference.ByteName.toString());
        case TypeReference.ShortTypeCode -> sigs.add(TypeReference.ShortName.toString());
        case TypeReference.IntTypeCode -> sigs.add(TypeReference.IntName.toString());
        case TypeReference.LongTypeCode -> sigs.add(TypeReference.LongName.toString());
        case TypeReference.FloatTypeCode -> sigs.add(TypeReference.FloatName.toString());
        case TypeReference.DoubleTypeCode -> sigs.add(TypeReference.DoubleName.toString());
        case TypeReference.CharTypeCode -> sigs.add(TypeReference.CharName.toString());
        case TypeReference.ClassTypeCode -> {
          int off = i - 1;
          i = getEndIndexOfClassType(typeSigs, i);
          sigs.add(typeSigs.substring(off, i));
        }
        case TypeReference.ArrayTypeCode -> {
          int arrayStart = i - 1;
          while (typeSigs.charAt(i) == TypeReference.ArrayTypeCode) {
            i++;
          }
          switch (typeSigs.charAt(i)) {
            case TypeReference.BooleanTypeCode,
                TypeReference.ByteTypeCode,
                TypeReference.ShortTypeCode,
                TypeReference.IntTypeCode,
                TypeReference.LongTypeCode,
                TypeReference.FloatTypeCode,
                TypeReference.DoubleTypeCode,
                TypeReference.CharTypeCode -> {
              sigs.add(typeSigs.substring(arrayStart, i + 1));
              i++;
            }
            case 'T', TypeReference.ClassTypeCode -> {
              i++; // to skip 'L' or 'T'
              i = getEndIndexOfClassType(typeSigs, i);
              sigs.add(typeSigs.substring(arrayStart, i));
            }
            default -> Assertions.UNREACHABLE("BANG " + typeSigs.charAt(i));
          }
        }
        case (byte) 'T' -> { // type variable
          int off = i - 1;
          while (typeSigs.charAt(i++) != ';')
            ;
          sigs.add(typeSigs.substring(off, i));
        }
        case (byte) '*' -> // unbounded wildcard
            sigs.add("*");
        // bounded wildcard
        case (byte) '-', (byte) '+' -> {
          int boundedStart = i - 1;
          i++; // to skip 'L'
          i = getEndIndexOfClassType(typeSigs, i);
          sigs.add(typeSigs.substring(boundedStart, i));
        } // end of parameter list
        case (byte) ')', (byte) '>' -> {
          return sigs.toArray(new String[0]); // end of type argument list
        }
        default -> throw new IllegalArgumentException("bad type signature list " + typeSigs);
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
