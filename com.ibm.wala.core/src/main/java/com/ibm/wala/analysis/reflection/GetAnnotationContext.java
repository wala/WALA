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
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMember;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.types.MemberReference;

/**
 * Implements a Context which corresponds to a given type abstraction. Thus, this maps the name
 * "TYPE" to a JavaTypeAbstraction. TODO This context maps {@link
 * com.ibm.wala.ipa.callgraph.ContextKey#RECEIVER} to a {@link TypeAbstraction}.
 */
public class GetAnnotationContext implements Context {

  private final TypeAbstraction type;
  private final IMember member;
  private final TypeAbstraction annotationType;

  public GetAnnotationContext(TypeAbstraction type, TypeAbstraction annotationType) {
    if (type == null) {
      throw new IllegalArgumentException("null type");
    }
    if (annotationType == null) {
      throw new IllegalArgumentException("null annotation type");
    }
    this.type = type;
    this.member = null;
    this.annotationType = annotationType;
  }

  public GetAnnotationContext(IMember mr, TypeAbstraction annotationType) {
    if (mr == null) {
      throw new IllegalArgumentException("null type");
    }
    if (annotationType == null) {
      throw new IllegalArgumentException("null annotation type");
    }
    this.type = null;
    this.member = mr;
    this.annotationType = annotationType;
  }

  @Override
  public ContextItem get(ContextKey name) {
    if (name == ContextKey.RECEIVER) {
      return type;
    } else if (name == ContextKey.PARAMETERS[0]) {
      if (type instanceof PointType) {
        IClass cls = ((PointType) type).getIClass(); //Class.getAnnotation
        return new FilteredPointerKey.SingleClassFilter(cls);
      } else if (member instanceof IMember){
        return new ContextItem.Value<IMember>(member);
      } else {
        assert false;
        return null;
      }
    } else if (name == ContextKey.PARAMETERS[1]) {
      if (annotationType instanceof PointType) {
        IClass cls = ((PointType) annotationType).getIClass();
        return new FilteredPointerKey.SingleClassFilter(cls);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "GetAnnotationContext<" + type + " : " + annotationType + '>';
  }

  @Override
  public int hashCode() {
    if (type != null) {
      return 6367 * type.hashCode();
    } else {
      return 6367 * member.hashCode();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      GetAnnotationContext other = (GetAnnotationContext) obj;
      if (type != null) {
        return type.equals(other.type);
      } else {
        return member.equals(other.member);
      }
    } else {
      return false;
    }
  }

  public TypeAbstraction getType() {
    return type;
  }
}

