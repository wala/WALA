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

  public static final PrimitiveType BOOLEAN = makePrimitive(TypeReference.Boolean);

  public static final PrimitiveType CHAR = makePrimitive(TypeReference.Char);

  public static final PrimitiveType BYTE = makePrimitive(TypeReference.Byte);

  public static final PrimitiveType SHORT = makePrimitive(TypeReference.Short);

  public static final PrimitiveType INT = makePrimitive(TypeReference.Int);

  public static final PrimitiveType LONG = makePrimitive(TypeReference.Long);

  public static final PrimitiveType FLOAT = makePrimitive(TypeReference.Float);

  public static final PrimitiveType DOUBLE = makePrimitive(TypeReference.Double);

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

  private PrimitiveType(TypeReference reference) {
    this.reference = reference;
  }

  public TypeAbstraction meet(TypeAbstraction rhs) {
    if (rhs == TOP) {
      return this;
    } else if (rhs == this) {
      return this;
    } else if (this == BOOLEAN) {
      return rhs;
    } else if (rhs == BOOLEAN) {
      return this;
    }
    return TOP;
  }

  public int hashCode() {
    return reference.hashCode();
  }

  public boolean equals(Object other) {
    return this == other;
  }

  public IClass getType() {
    return null;
  }

  public static PrimitiveType getPrimitive(TypeReference reference) {
    return refernceToType.get(reference);
  }

  private static PrimitiveType makePrimitive(TypeReference reference) {
    PrimitiveType newType = new PrimitiveType(reference);
    refernceToType.put(reference, newType);
    return newType;
  }

  public String toString() {
    String result = primitiveNameMap.get(reference.getName().toString());
    return (result != null) ? result : "PrimitiveType";
  }

}
