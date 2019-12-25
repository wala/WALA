/*
 * Copyright (c) 2013 IBM Corporation.
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
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;

/**
 * A context which may be used if
 *
 * <ul>
 *   <li>the method to be interpreted is either {@link java.lang.Class#getMethod(String, Class...)}
 *       or {@link java.lang.Class#getDeclaredMethod(String, Class...)},
 *   <li>the type of the "this" argument is known and
 *   <li>the value of the first argument (the method name) is a constant.
 * </ul>
 *
 * In the special case described above, {@link GetMethodContextInterpreter} and {@link
 * GetMethodContextSelector} should be preferred over {@link JavaLangClassContextInterpreter} and
 * {@link JavaLangClassContextSelector}, as {@link GetMethodContextInterpreter} and {@link
 * GetMethodContextSelector} drastically reduce the number of methods returned increasing the
 * precision of the analysis. Thus, {@link GetMethodContextInterpreter} and {@link
 * GetMethodContextSelector} should be placed in be placed in front of {@link
 * JavaLangClassContextInterpreter} and {@link JavaLangClassContextSelector} .
 *
 * @author Michael Heilmann
 * @see com.ibm.wala.analysis.reflection.GetMethodContextInterpreter
 * @see com.ibm.wala.analysis.reflection.GetMethodContextSelector TODO Do the same for {@link
 *     Class#getField(String)} and {@link Class#getDeclaredField(String)}.
 */
public class GetMethodContext implements Context {
  /** The type abstraction. */
  private final TypeAbstraction type;

  /** The method name. */
  private final ConstantKey<String> name;

  /**
   * Construct this GetMethodContext.
   *
   * @param type the type
   * @param name the name of the method
   */
  public GetMethodContext(TypeAbstraction type, ConstantKey<String> name) {
    if (type == null) {
      throw new IllegalArgumentException("null == type");
    }
    this.type = type;
    if (name == null) {
      throw new IllegalArgumentException("null == name");
    }
    this.name = name;
  }

  class NameItem implements ContextItem {
    String name() {
      return getName();
    }
  };

  @Override
  public ContextItem get(ContextKey name) {

    if (name == ContextKey.RECEIVER) {
      return type;
    } else if (name == ContextKey.NAME) {
      return new NameItem();
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
    return "GetMethodContext<" + type + ", " + name + '>';
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
   * Get the type.
   *
   * @return the type
   */
  public TypeAbstraction getType() {
    return type;
  }

  /**
   * Get the name.
   *
   * @return the name
   */
  public String getName() {
    return name.getValue();
  }
}
