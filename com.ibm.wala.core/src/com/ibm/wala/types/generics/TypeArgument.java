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
 * UNDER CONSTRUCTION
 * 
 * <verbatim> 
 * TypeArgument: 
 *    WildcardIndicator? FieldTypeSignature 
 *    *
 * 
 * WildcardIndicator: 
 *    + 
 *    - 
 * </verbatim>
 * 
 * @author sjfink
 * 
 */
public class TypeArgument extends Signature {

  private final TypeSignature sig;
  
  private final static TypeArgument WILDCARD = new TypeArgument("*") {
    public boolean isWildcard() {
      return true;
    }
  };

  private TypeArgument(String s) {
    super(s);
    sig = null;
  }

  private TypeArgument(TypeSignature sig) {
    super(sig.rawString());
    this.sig = sig;
  }
  
  public boolean isWildcard() {
    return false;
  }

  public static TypeArgument[] make(String s) throws IllegalArgumentException {
    if (s.length() == 0 || s.charAt(0) != '<') {
      throw new IllegalArgumentException(s);
    }
    if (s.charAt(s.length() - 1) != '>') {
      throw new IllegalArgumentException(s);
    }
    String[] args = parseForTypeArguments(s);
    TypeArgument[] result = new TypeArgument[args.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = makeTypeArgument(args[i]);
    }
    return result;
  }

  private static TypeArgument makeTypeArgument(String s) {
    switch (s.charAt(0)) {
    case '*':
      return WILDCARD;
    case '+':
    case '-':
      Assertions.UNREACHABLE();
      return null;
    default:
      TypeSignature sig = TypeSignature.make(s);
      return new TypeArgument(sig);
    }
  }

  /**
   * @param typeSigs
   *          Strin TypeSignature*
   * @return tokenize it
   */
  static String[] parseForTypeArguments(String typeArgs) {
    ArrayList<String> args = new ArrayList<String>(10);

    int i = 1;
    while (true) {
      switch (typeArgs.charAt(i++)) {
      case TypeReference.ClassTypeCode: {
        int off = i - 1;
        int depth = 0;
        while (typeArgs.charAt(i++) != ';' || depth > 0) {
          if (typeArgs.charAt(i - 1) == '<') {
            depth++;
          }
          if (typeArgs.charAt(i - 1) == '>') {
            depth--;
          }
        }
        args.add(typeArgs.substring(off, i));
        continue;
      }
      case TypeReference.ArrayTypeCode: {
        int off = i - 1;
        while (typeArgs.charAt(i) == TypeReference.ArrayTypeCode) {
          ++i;
        }
        if (typeArgs.charAt(i++) == TypeReference.ClassTypeCode) {
          while (typeArgs.charAt(i++) != ';')
            ;
          args.add(typeArgs.substring(off, i - off - 1));
        } else {
          args.add(typeArgs.substring(off, i - off));
        }
        continue;
      }
      case (byte) 'T': { // type variable
        int off = i - 1;
        while (typeArgs.charAt(i++) != ';')
          ;
        args.add(typeArgs.substring(off, i));
        continue;
      }
      case (byte) '>': // end of argument list
        int size = args.size();
        if (size == 0) {
          return null;
        }
        Iterator<String> it = args.iterator();
        String[] result = new String[size];
        for (int j = 0; j < size; j++) {
          result[j] = it.next();
        }
        return result;
      default:
        if (Assertions.verifyAssertions) {
          Assertions._assert(false, "bad type argument list " + typeArgs);
        }
      }
    }
  }

  public TypeSignature getFieldTypeSignature() {
    return sig;
  }

}
