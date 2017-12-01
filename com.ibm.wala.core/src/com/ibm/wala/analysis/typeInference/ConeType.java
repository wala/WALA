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

import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * Represents a type and its subtypes.
 */
public class ConeType extends TypeAbstraction {

  private final IClass type;

  /**
   * @throws IllegalArgumentException if type is null
   */
  public ConeType(IClass type) {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    assert type.getReference().isReferenceType() : type;
    this.type = type;
  }

  @Override
  public TypeAbstraction meet(TypeAbstraction rhs) {
    if (rhs == TOP) {
      return this;
    } else if (rhs instanceof ConeType) {
      ConeType other = (ConeType) rhs;
      if (type.equals(other.type)) {
        return this;
      } else if (type.isArrayClass() || other.type.isArrayClass()) {
        // give up on arrays. We don't care anyway.
        return new ConeType(type.getClassHierarchy().getRootClass());
      } else {
        return new ConeType(type.getClassHierarchy().getLeastCommonSuperclass(this.type, other.type));
      }
    } else if (rhs instanceof PointType) {
      return rhs.meet(this);
    } else if (rhs instanceof PrimitiveType) {
      return TOP;
    } else {
      Assertions.UNREACHABLE("unexpected type " + rhs.getClass());
      return null;
    }
  }


  @Override
  public String toString() {
    return "cone:" + type.toString();
  }
  
  @Override
  public IClass getType() {
    return type;
  }

  @Override
  public TypeReference getTypeReference() {
    return type.getReference();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ConeType)) {
      return false;
    }
    ConeType other = (ConeType) obj;
    if (other == TOP) {
      return false;
    }
    if (!type.getClassHierarchy().equals(other.type.getClassHierarchy())) {
      Assertions.UNREACHABLE("different chas " + this + " " + other);
    }
    return type.equals(other.type);
  }

  @Override
  public int hashCode() {
    return 39 * type.hashCode();
  }

  public boolean isArrayType() {
    return getType().isArrayClass();
  }

  public boolean isInterface() {
    return getType().isInterface();
  }

  /**
   * @return an Iterator of IClass that implement this interface
   */
  public Iterator<IClass> iterateImplementors() {
    return type.getClassHierarchy().getImplementors(getType().getReference()).iterator();
  }

  public IClassHierarchy getClassHierarchy() {
    return type.getClassHierarchy();
  }
}
