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


/**
 *
 * Abstraction of a set of PointTypes.  These are immutable.
 * TODO: fix for efficiency if needed.
 * 
 * @author sfink
 * 
 */
public class SetType extends TypeAbstraction {

  private final HashSet<TypeReference> types;
  private final int hashCode;
  
  public SetType(PointType[] points) {
    if (points == null) {
      throw new IllegalArgumentException("points is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(points.length > 0);
    }
    types = HashSetFactory.make(points.length);
    int h = 0;
    for (int i = 0; i< points.length; i++) {
      TypeReference T = points[i].getType().getReference();
      h ^= T.hashCode();
      types.add(T);
    }
    hashCode = h;
  }


  /* (non-Javadoc)
   * @see com.ibm.wala.analysis.typeInference.TypeAbstraction#meet(com.ibm.wala.analysis.typeInference.TypeAbstraction)
   */
  @Override
  public TypeAbstraction meet(TypeAbstraction rhs) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.analysis.typeInference.TypeAbstraction#equals(java.lang.Object)
   */
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
        SetType other = (SetType)obj;
        return (types.equals(other.types));
      }
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.analysis.typeInference.TypeAbstraction#hashCode()
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * @return Iterator of the TypeReferences which compose this Set.
   */
  public Iterator iteratePoints() {
    return types.iterator();
  }
}
