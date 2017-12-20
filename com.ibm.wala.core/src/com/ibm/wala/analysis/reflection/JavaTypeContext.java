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
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;

/**
 *  Implements a Context which corresponds to a given type abstraction.
 *  Thus, this maps the name "TYPE" to a JavaTypeAbstraction.
 * TODO
 *  This context maps
 *  {@link com.ibm.wala.ipa.callgraph.ContextKey#RECEIVER} to a {@link TypeAbstraction}.
 */
public class JavaTypeContext implements Context {

  private final TypeAbstraction type;

  public JavaTypeContext(TypeAbstraction type) {
    if (type == null) {
      throw new IllegalArgumentException("null type");
    }
    this.type = type;
  }

  @Override
  public ContextItem get(ContextKey name) {
    if (name == ContextKey.RECEIVER) {
      return type;
    } else if (name == ContextKey.PARAMETERS[0]) {
      if (type instanceof PointType) {
        IClass cls = ((PointType) type).getIClass();
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
    return "JavaTypeContext<" + type + ">";
  }

  @Override
  public int hashCode() {
    return 6367 * type.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      JavaTypeContext other = (JavaTypeContext) obj;
      return type.equals(other.type);
    } else {
      return false;
    }
  }

  public TypeAbstraction getType() {
    return type;
  }

}
