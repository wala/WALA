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

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

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
 * 
 */
public abstract class TypeSignature extends Signature {

  TypeSignature(String s) {
    super(s);
  }

  public static TypeSignature make(String s) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    if (s.length() == 0) {
      throw new IllegalArgumentException("illegal empty string s");
    }
    assert (s.length() > 0);
    switch (s.charAt(0)) {
    case TypeReference.VoidTypeCode:
      Assertions.UNREACHABLE();
      return null;
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
   * @param typeSigs TypeSignature*
   * @return tokenize it
   */
  static String[] parseForTypeSignatures(String typeSigs) throws IllegalArgumentException {
    ArrayList<String> sigs = new ArrayList<>(10);
    if (typeSigs.length() < 2) {
      // TODO: check this?
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
      case TypeReference.ClassTypeCode: {
        int off = i - 1;
        int depth = 0;
        while (typeSigs.charAt(i++) != ';' || depth > 0) {
          if (typeSigs.charAt(i - 1) == '<') {
            depth++;
          }
          if (typeSigs.charAt(i - 1) == '>') {
            depth--;
          }
        }
        sigs.add(typeSigs.substring(off, i));
        continue;
      }
      case TypeReference.ArrayTypeCode: {
        switch (typeSigs.charAt(i)) {
        case TypeReference.BooleanTypeCode:
        case TypeReference.ByteTypeCode:
        case TypeReference.IntTypeCode:
          sigs.add(typeSigs.substring(i - 1, i + 1));
          break;
        case 'T':
        case TypeReference.ClassTypeCode:
          int off = i - 1;
          i++;
          int depth = 0;
          while (typeSigs.charAt(i++) != ';' || depth > 0) {
            if (typeSigs.charAt(i - 1) == '<') {
              depth++;
            }
            if (typeSigs.charAt(i - 1) == '>') {
              depth--;
            }
          }
          sigs.add(typeSigs.substring(off, i));
          break;
        default:
          Assertions.UNREACHABLE("BANG " + typeSigs.charAt(i));
        }
        continue;
      }
      case (byte) 'T': { // type variable
        int off = i - 1;
        while (typeSigs.charAt(i++) != ';')
          ;
        sigs.add(typeSigs.substring(off, i));
        continue;
      }
      case (byte) ')': // end of parameter list
        int size = sigs.size();
        if (size == 0) {
          return null;
        }
        Iterator<String> it = sigs.iterator();
        String[] result = new String[size];
        for (int j = 0; j < size; j++) {
          result[j] = it.next();
        }
        return result;
      default:
        assert false : "bad type signature list " + typeSigs;
      }
    }
  }
}
