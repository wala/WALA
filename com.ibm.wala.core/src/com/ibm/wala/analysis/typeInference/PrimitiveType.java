/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.typeInference;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;

public class PrimitiveType extends TypeAbstraction {

  private static Map<TypeReference, PrimitiveType> refernceToType = new HashMap<TypeReference, PrimitiveType>();

  public static final PrimitiveType BOOLEAN = makePrimitive(TypeReference.Boolean, 1);

  public static final PrimitiveType CHAR = makePrimitive(TypeReference.Char, 16);

  public static final PrimitiveType BYTE = makePrimitive(TypeReference.Byte, 8);

  public static final PrimitiveType SHORT = makePrimitive(TypeReference.Short, 16);

  public static final PrimitiveType INT = makePrimitive(TypeReference.Int, 32);

  public static final PrimitiveType LONG = makePrimitive(TypeReference.Long, 64);

  public static final PrimitiveType FLOAT = makePrimitive(TypeReference.Float, 32);

  public static final PrimitiveType DOUBLE = makePrimitive(TypeReference.Double, 64);

  private static HashMap<String, String> primitiveNameMap;
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

  private final TypeReference reference;

  private final int size;

  private PrimitiveType(TypeReference reference, int size) {
    this.reference = reference;
    this.size = size;
  }

  @Override
  public TypeAbstraction meet(TypeAbstraction rhs) {
    if (rhs == TOP) {
      return this;
    } else if (rhs == this) {
      return this;
    } else if (rhs instanceof PrimitiveType) {
      if (size() > ((PrimitiveType) rhs).size()) {
        return this;
      } else {
        return rhs;
      }
    } else {
      return TOP;
    }
  }

  public int size() {
    return size;
  }

  @Override
  public int hashCode() {
    return reference.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return this == other;
  }

  @Override
  public IClass getType() {
    return null;
  }

  @Override
  public TypeReference getTypeReference() {
    return reference;
  }

  public static PrimitiveType getPrimitive(TypeReference reference) {
    return refernceToType.get(reference);
  }

  private static PrimitiveType makePrimitive(TypeReference reference, int size) {
    PrimitiveType newType = new PrimitiveType(reference, size);
    refernceToType.put(reference, newType);
    return newType;
  }

  @Override
  public String toString() {
    String result = primitiveNameMap.get(reference.getName().toString());
    return (result != null) ? result : "PrimitiveType";
  }

}
