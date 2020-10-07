/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.analysis.typeInference;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.Map;

/**
 * Abstraction of a primitive type. Subclasses will define the primitive type abstractions for a
 * particular language.
 *
 * @see JavaPrimitiveType
 */
public abstract class PrimitiveType extends TypeAbstraction {

  protected static final Map<TypeReference, PrimitiveType> referenceToType = HashMapFactory.make();

  public static PrimitiveType getPrimitive(TypeReference reference) {
    return referenceToType.get(reference);
  }

  protected final TypeReference reference;

  protected final int size;

  protected PrimitiveType(TypeReference reference, int size) {
    this.reference = reference;
    this.size = size;
    referenceToType.put(reference, this);
  }

  @Override
  public TypeAbstraction meet(TypeAbstraction rhs) {
    if (rhs == TOP) {
      return this;
    } else if (rhs == this) {
      return this;
    } else if (rhs instanceof PrimitiveType) {
      // the meet of two primitives is the smaller of the two types.
      // in particular integer meet boolean == boolean
      if (size() < ((PrimitiveType) rhs).size()) {
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

  @Override
  public String toString() {
    return reference.getName().toString();
  }
}
