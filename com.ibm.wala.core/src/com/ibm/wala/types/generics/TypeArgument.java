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

/**
 * UNDER CONSTRUCTION
 * 
 * <pre> TypeArgument: WildcardIndicator? FieldTypeSignature *
 * 
 * WildcardIndicator: + -
 * 
 * 
 * </pre>
 * 
 * @author sjfink
 * 
 */
public class TypeArgument extends Signature {

  private final TypeSignature sig;

  private final WildcardIndicator w;

  private static enum WildcardIndicator {
    PLUS, MINUS
  }

  private final static TypeArgument WILDCARD = new TypeArgument("*") {
    @Override
    public boolean isWildcard() {
      return true;
    }

    @Override
    public String toString() {
      return "*";
    }
  };

  private TypeArgument(String s) {
    super(s);
    sig = null;
    w = null;
  }

  private TypeArgument(TypeSignature sig, WildcardIndicator w) {
    super(sig.rawString());
    this.sig = sig;
    this.w = w;
  }

  public boolean isWildcard() {
    return false;
  }

  public static TypeArgument[] make(String s) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
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
    case '+': {
      TypeSignature sig = TypeSignature.make(s.substring(1));
      return new TypeArgument(sig, WildcardIndicator.PLUS);
    }
    case '-': {
      TypeSignature sig = TypeSignature.make(s.substring(1));
      return new TypeArgument(sig, WildcardIndicator.MINUS);
    }
    default:
      TypeSignature sig = TypeSignature.make(s);
      return new TypeArgument(sig, null);
    }
  }

  /**
   * @param typeSigs TypeSignature*
   * @return tokenize it
   */
  static String[] parseForTypeArguments(String typeArgs) {
    ArrayList<String> args = new ArrayList<>(10);

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
      case (byte) '-':
      case (byte) '+':
      case (byte) 'T': { // type variable
        int off = i - 1;
        while (typeArgs.charAt(i++) != ';')
          ;
        args.add(typeArgs.substring(off, i));
        continue;
      }
      case (byte) '*': {
        // a wildcard
        args.add("*");
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
        assert false : "bad type argument list " + typeArgs;
      }
    }
  }

  public TypeSignature getFieldTypeSignature() {
    return sig;
  }

  @Override
  public String toString() {
    if (w == null) {
      return sig.toString();
    } else if (w.equals(WildcardIndicator.PLUS)) {
      return "+" + sig.toString();
    } else {
      return "-" + sig.toString();
    }
  }

}
