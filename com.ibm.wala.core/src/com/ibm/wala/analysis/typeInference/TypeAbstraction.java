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
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Abstraction of a Java type. These are immutable.
 * 
 * @author sfink
 */
public abstract class TypeAbstraction implements ContextItem {

  /**
   * Canonical element representing TOP for a dataflow lattice
   */
  public final static TypeAbstraction TOP = new TypeAbstraction() {
    @Override
    public TypeAbstraction meet(TypeAbstraction rhs) {
      return rhs;
    }

    @Override
    public String toString() {
      return "JavaTypeAbstraction.TOP";
    }

    @Override
    public int hashCode() {
      return 17;
    }

    @Override
    public boolean equals(Object other) {
      return this == other;
    }

    @Override
    public IClass getType() {
      return null;
    }

    @Override
    public TypeReference getTypeReference() {
      return null;
    }
  };

  public abstract TypeAbstraction meet(TypeAbstraction rhs);

  /**
   * @see java.lang.Object#equals(Object)
   */
  @Override
  public abstract boolean equals(Object obj);

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public abstract int hashCode();

  /**
   * A TypeReference representing the types of this abstraction
   */
  public abstract TypeReference getTypeReference();

  /**
   * This is here for convenience; it makes sense for Point and Cone Dispatch.
   * TODO: probably should get rid of it.
   */
  public IClass getType() {
    Assertions.UNREACHABLE("getType not implemented for " + getClass());
    return null;
  }

}
