/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.analysis.typeInference;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.HashMap;

/** Abstraction of a primitive type in Java. */
public class JavaPrimitiveType extends PrimitiveType {

  public static final PrimitiveType BOOLEAN = makePrimitive(TypeReference.Boolean, 1);

  public static final PrimitiveType CHAR = makePrimitive(TypeReference.Char, 16);

  public static final PrimitiveType BYTE = makePrimitive(TypeReference.Byte, 8);

  public static final PrimitiveType SHORT = makePrimitive(TypeReference.Short, 16);

  public static final PrimitiveType INT = makePrimitive(TypeReference.Int, 32);

  public static final PrimitiveType LONG = makePrimitive(TypeReference.Long, 64);

  public static final PrimitiveType FLOAT = makePrimitive(TypeReference.Float, 32);

  public static final PrimitiveType DOUBLE = makePrimitive(TypeReference.Double, 64);

  public static final PrimitiveType VOID = makePrimitive(TypeReference.Void, 0);

  public static void init() {}

  private JavaPrimitiveType(TypeReference reference, int size) {
    super(reference, size);
  }

  private static PrimitiveType makePrimitive(TypeReference reference, int size) {
    return new JavaPrimitiveType(reference, size);
  }

  private static final HashMap<String, String> primitiveNameMap;

  static {
    primitiveNameMap = HashMapFactory.make(9);
    primitiveNameMap.put("I", "int");
    primitiveNameMap.put("J", "long");
    primitiveNameMap.put("S", "short");
    primitiveNameMap.put("B", "byte");
    primitiveNameMap.put("C", "char");
    primitiveNameMap.put("D", "double");
    primitiveNameMap.put("F", "float");
    primitiveNameMap.put("Z", "boolean");
    primitiveNameMap.put("V", "void");
  }

  @Override
  public String toString() {
    String result = primitiveNameMap.get(reference.getName().toString());
    return (result != null) ? result : reference.getName().toString();
  }
}
