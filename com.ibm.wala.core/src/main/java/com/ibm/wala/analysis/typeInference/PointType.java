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
import com.ibm.wala.util.debug.Assertions;

/** Represents a single concrete type. */
public class PointType extends TypeAbstraction {

  private final IClass type;

  /** @throws IllegalArgumentException if type is null */
  public PointType(IClass type) {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    this.type = type;
    assert type.getReference().isReferenceType();
  }

  @Override
  public TypeAbstraction meet(TypeAbstraction rhs) {
    if (rhs == TOP) {
      return this;
    } else {
      if (rhs instanceof PointType) {
        PointType other = (PointType) rhs;
        if (type.equals(other.type)) {
          return this;
        } else if (type.isArrayClass() || other.type.isArrayClass()) {
          // give up on arrays. We don't care anyway.
          return new ConeType(type.getClassHierarchy().getRootClass());
        } else {
          return new ConeType(
              type.getClassHierarchy().getLeastCommonSuperclass(this.type, other.type));
        }
      } else if (rhs instanceof ConeType) {
        ConeType other = (ConeType) rhs;
        if (type.equals(other.getType())) {
          // "this" and the cone type have the same underlying type, return the cone type
          return other;
        }
        TypeReference T = other.getType().getReference();
        if (type.isArrayClass() || T.isArrayType()) {
          // give up on arrays. We don't care anyway.
          return new ConeType(type.getClassHierarchy().getRootClass());
        }
        IClass typeKlass = type;
        if (type.getClassHierarchy().isSubclassOf(typeKlass, other.getType())) {
          return other;
        } else if (other.isInterface()) {
          if (type.getClassHierarchy().implementsInterface(typeKlass, other.getType())) {
            return other;
          }
        }
        // if we get here, we need to do cha-based superclass and return a cone.
        // TODO: avoid the allocation
        return other.meet(new ConeType(this.getType()));
      } else {
        Assertions.UNREACHABLE("Unexpected type: " + rhs.getClass());
        return null;
      }
    }
  }

  @Override
  public String toString() {
    return "point: " + type.toString();
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
    if (!(obj instanceof PointType)) {
      return false;
    }
    PointType other = (PointType) obj;
    if (!type.getClassHierarchy().equals(other.type.getClassHierarchy())) {
      Assertions.UNREACHABLE("different chas " + this + ' ' + other);
    }
    return type.equals(other.type);
  }

  @Override
  public int hashCode() {
    return 37 * type.hashCode();
  }

  public boolean isArrayType() {
    return getType().isArrayClass();
  }

  public IClass getIClass() {
    return type;
  }
}
