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
 * <verbatim>
 * TypeSignature: 
 *    FieldTypeSignature 
 *    BaseType (code for a primitive)
 * 
 * FieldTypeSignature: 
 *    ClassTypeSignature 
 *    ArrayTypeSignature
 *    TypeVariableSignature
 * 
 * TypeVariableSignature: 
 *    T identifier ;
 * </verbatim>
 * 
 * @author sjfink
 * 
 */
public abstract class TypeSignature extends Signature {

  TypeSignature(String s) {
    super(s);
  }

  public static TypeSignature make(String s) {
    assert (s.length() > 0);
    if (s.charAt(0) == 'L') {
      return ClassTypeSignature.makeClassTypeSig(s);
    } else if (s.charAt(0) == 'T') {
      return TypeVariableSignature.make(s);
    } else {
      Assertions.UNREACHABLE(s);
      return null;
    }
  }

  public abstract boolean isTypeVariable();

  public abstract boolean isClassTypeSignature();

  /**
   * @param typeSigs
   *          Strin TypeSignature*
   * @return tokenize it
   */
  static String[] parseForTypeSignatures(String typeSigs) {
    ArrayList<String> sigs = new ArrayList<String>(10);

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
          if (typeSigs.charAt(i-1) == '<') {
            depth++;
          }
          if (typeSigs.charAt(i-1) == '>') {
            depth--;
          }
        }
        sigs.add(typeSigs.substring(off, i));
        continue;
      }
      case TypeReference.ArrayTypeCode: {
        int off = i - 1;
        while (typeSigs.charAt(i) == TypeReference.ArrayTypeCode) {
          ++i;
        }
        if (typeSigs.charAt(i++) == TypeReference.ClassTypeCode) {
          while (typeSigs.charAt(i++) != ';')
            ;
          sigs.add(typeSigs.substring(off, i - off - 1));
        } else {
          sigs.add(typeSigs.substring(off, i - off));
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
        if (Assertions.verifyAssertions) {
          Assertions._assert(false, "bad type signature list " + typeSigs);
        }
      }
    }
  }
}
