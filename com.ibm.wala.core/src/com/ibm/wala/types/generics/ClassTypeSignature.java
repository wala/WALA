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
 * @author sjfink
 * 
 */
public class ClassTypeSignature extends TypeSignature {

  ClassTypeSignature(String s) {
    super(s);
  }

  public static ClassTypeSignature makeClassTypeSig(String s) {
    return new ClassTypeSignature(s);
  }

  public TypeArgument[] getTypeArguments() {
    String argString = rawString().replaceAll(".*<", "<").replaceAll(">.*", ">");
    String[] args = parseForTypeArguments(argString);
    TypeArgument[] result = new TypeArgument[args.length];
    for (int i = 0; i < args.length; i++) {
      result[i] = TypeArgument.make(args[i]);
    }
    return result;
  }

  static String[] parseForTypeArguments(String argsString) {
    ArrayList<String> args = new ArrayList<String>(10);

    int i = 1;
    while (true) {
      switch (argsString.charAt(i++)) {
      case '*':
        args.add(String.valueOf('*'));
        continue;
      case TypeReference.VoidTypeCode:
        args.add(TypeReference.VoidName.toString());
        continue;
      case TypeReference.BooleanTypeCode:
        args.add(TypeReference.BooleanName.toString());
        continue;
      case TypeReference.ByteTypeCode:
        args.add(TypeReference.ByteName.toString());
        continue;
      case TypeReference.ShortTypeCode:
        args.add(TypeReference.ShortName.toString());
        continue;
      case TypeReference.IntTypeCode:
        args.add(TypeReference.IntName.toString());
        continue;
      case TypeReference.LongTypeCode:
        args.add(TypeReference.LongName.toString());
        continue;
      case TypeReference.FloatTypeCode:
        args.add(TypeReference.FloatName.toString());
        continue;
      case TypeReference.DoubleTypeCode:
        args.add(TypeReference.DoubleName.toString());
        continue;
      case TypeReference.CharTypeCode:
        args.add(TypeReference.CharName.toString());
        continue;
      case TypeReference.ClassTypeCode: {
        int off = i - 1;
        while (argsString.charAt(i++) != ';')
          ;
        args.add(argsString.substring(off, i - off));

        continue;
      }
      case TypeReference.ArrayTypeCode: {
        int off = i - 1;
        while (argsString.charAt(i) == TypeReference.ArrayTypeCode) {
          ++i;
        }
        if (argsString.charAt(i++) == TypeReference.ClassTypeCode) {
          while (argsString.charAt(i++) != ';')
            ;
          args.add(argsString.substring(off, i - off - 1));
        } else {
          args.add(argsString.substring(off, i - off));
        }
        continue;
      }
      case (byte) '>': // end of parameter list
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
          Assertions._assert(false, "bad type argument list " + argsString + " " + argsString.charAt(i-1));
        }
      }
    }
  }

  @Override
  public boolean isTypeVariable() {
    // TODO Auto-generated method stub
    return false;
  }

}
