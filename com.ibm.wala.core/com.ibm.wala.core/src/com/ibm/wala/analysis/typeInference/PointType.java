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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * Abstraction of a Java type. These are immutable.
 * @author sfink
 */
public class PointType extends TypeAbstraction {

  private final IClass type;
  private final ClassHierarchy cha;

  /**
   * Private constructor ... only for internal use.
   */
  public PointType(IClass type, ClassHierarchy cha) {
    this.type = type;
    if (Assertions.verifyAssertions) {
      Assertions._assert(type != null);
      Assertions._assert(type.getReference().isReferenceType());
    }
    this.cha = cha;
  }

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
          return new ConeType(cha.getRootClass(), cha);
        } else {
          return new ConeType(cha.getLeastCommonSuperclass(this.type, other.type), cha);
        }
      } else if (rhs instanceof ConeType) {
        ConeType other = (ConeType) rhs;
        TypeReference T = other.getType().getReference();
        if (type.isArrayClass() || T.isArrayType()) {
          // give up on arrays. We don't care anyway.
          return new ConeType(cha.getRootClass(), cha);
        }
        IClass typeKlass = type;
        if (cha.isSubclassOf(typeKlass, other.getType())) {
          return other;
        } else if (other.isInterface()) {
          if (cha.implementsInterface(typeKlass, T)) {
            return other;
          }
        }
        // if we get here, we need to do cha-based superclass and return a cone.
        // TODO: avoid the allocation
        return other.meet(new ConeType(other.getType(), cha));
      } else {
        Assertions.UNREACHABLE("Unexpected type: " + rhs.getClass());
        return null;
      }
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "point: " + type.toString();
  }

  /**
   * Method getType.
   * 
   * @return TypeReference
   */
  public IClass getType() {
    return type;
  }

  /**
   * @see java.lang.Object#equals(Object)
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof PointType)) {
      return false;
    }
    PointType other = (PointType) obj;
    if (Assertions.verifyAssertions) {
      if (!cha.equals(other.cha)) {
        Assertions._assert(cha.equals(other.cha), "different chas " + this + " " + other);
      }
    }
    return type.equals(other.type);
  }

  /**
   * @see java.lang.Object#hashCode()
   */
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
