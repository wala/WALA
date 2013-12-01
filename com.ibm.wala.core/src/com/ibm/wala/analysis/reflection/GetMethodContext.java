/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
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
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;

/**
 * @brief
 *  A context which may only be used if the following is true:
 *  - The method to be interpreted is either
 *    {@link java.lang.Class#getMethod(String, Class...)} or
 *    {@link java.lang.Class#getDeclaredMethod(String, Class...)}.
 *  - The type of the "this" argument is known.
 *  - The value of the first argument (the method name) is a constant.
 * @author
 *  Michael Heilmann
 * @see
 *  com.ibm.wala.analysis.reflection.GetMethodContextInterpreter
 * @see
 *  com.ibm.wala.analysis.reflection.GetMethodContextSelector
 */
public class GetMethodContext implements Context {
  /**
   * @brief
   *  The type abstraction.
   */
  private final TypeAbstraction type;

  /**
   * @brief
   *  The method name.
   */
  private final ConstantKey name;
  
  /**
   * @brief
   *  Construct this GetMethodContext.
   * @param type
   *  The type.
   * @param name
   *  The name of the method.
   */
  public GetMethodContext(TypeAbstraction type,ConstantKey name) {
    if (type == null) {
      throw new IllegalArgumentException("null == type");
    }
    this.type = type;
    if (name == null) {
      throw new IllegalArgumentException("null == name");
    }
    this.name = name;
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
    } else if (name == ContextKey.PARAMETERS[1]) {
       return new FilteredPointerKey.SingleClassFilter(this.name.getConcreteType());
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "GetMethodContext<" + type + ", " + name + ">";
  }

  @Override
  public int hashCode() {
    return 6367 * type.hashCode() * name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      GetMethodContext other = (GetMethodContext) obj;
      return type.equals(other.type) && name.equals(other.name);
    } else {
      return false;
    }
  }

  /**
   * @brief
   *  Get the type.
   * @return
   *  The type.
   */
  public TypeAbstraction getType() {
    return type;
  }
  
  /**
   * @brief
   *  Get the name.
   * @return
   *  The name.
   */
  public String getName() {
    return (String)name.getValue();
  }
}
