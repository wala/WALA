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

import com.ibm.wala.analysis.typeInference.*;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Implement a Context which corresponds to a given type abstraction. Thus, this
 * maps the name "TYPE" to a JavaTypeAbstraction.
 * 
 * @author sfink
 */
public class JavaTypeContext implements Context {

  private final TypeAbstraction type;

  public JavaTypeContext(TypeAbstraction type) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(type != null);
    }
    this.type = type;
  }

  public ContextItem get(ContextKey name) {
    if (name == ContextKey.RECEIVER) {
      return type;
    } else if (name == ContextKey.FILTER) {
      if (type instanceof PointType) {
        IClass cls = ((PointType) type).getIClass();
        return new FilteredPointerKey.SingleClassFilter(cls);
      } else {
        return null;
      }
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "JavaTypeContext<" + type + ">";
  }

  @Override
  public int hashCode() {
    return 6367 * type.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

}
