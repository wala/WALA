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

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * Abstraction of a set of {@link PointType}. These are immutable. TODO: fix for efficiency if needed.
 */
public class SetType extends TypeAbstraction {

  private final HashSet<TypeReference> types;

  private final int hashCode;

  public SetType(PointType[] points) {
    if (points == null) {
      throw new IllegalArgumentException("points is null");
    }
    if (points.length == 0) {
      throw new IllegalArgumentException("points.length == 0");
    }
    types = HashSetFactory.make(points.length);
    int h = 0;
    for (int i = 0; i < points.length; i++) {
      if (points[i] == null) {
        throw new IllegalArgumentException("points[" + i + "] is null");
      }
      TypeReference T = points[i].getType().getReference();
      h ^= T.hashCode();
      types.add(T);
    }
    hashCode = h;
  }

  @Override
  public TypeReference getTypeReference() {
    Iterator ti = types.iterator();
    TypeAbstraction T = (TypeAbstraction) ti.next();
    while (ti.hasNext()) {
      T = T.meet((TypeAbstraction) ti.next());
    }
    return T.getTypeReference();
  }

  /*
   * @see com.ibm.wala.analysis.typeInference.TypeAbstraction#meet(com.ibm.wala.analysis.typeInference.TypeAbstrac )
   */
  @Override
  public TypeAbstraction meet(TypeAbstraction rhs) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    // TODO: make SetTypes of size 1 equal PointTypes.
    // need a factory facade for this.

    // TODO: canonicalize?? How to improve this?
    if (obj == this) {
      return true;
    }
    if (obj instanceof SetType) {
      if (hashCode() != obj.hashCode()) {
        return false;
      } else {
        SetType other = (SetType) obj;
        return (types.equals(other.types));
      }
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * @return Iterator of the TypeReferences which compose this Set.
   */
  public Iterator<TypeReference> iteratePoints() {
    return types.iterator();
  }
}
