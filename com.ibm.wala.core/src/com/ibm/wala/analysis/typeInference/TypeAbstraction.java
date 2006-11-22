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
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * Absraction of a Java type.  These are immutable.
 * 
 * @author sfink
 */
public abstract class TypeAbstraction implements ContextItem {

  /**
   * Canonical element representing TOP for a dataflow lattice
   */
  public final static TypeAbstraction TOP = new TypeAbstraction() {
    public TypeAbstraction meet(TypeAbstraction rhs) {
      return rhs;
    }
    public String toString() {
      return "JavaTypeAbstraction.TOP";
    }
    public int hashCode() {
      return 17;
    }
    public boolean equals(Object other) {
      return this == other;
    }
    public IClass getType() { 
      return null;
    }
  };

  public abstract TypeAbstraction meet(TypeAbstraction rhs);

  /**
   * @see java.lang.Object#equals(Object)
   */
  public abstract boolean equals(Object obj);

  /**
   * @see java.lang.Object#hashCode()
   */
  public abstract int hashCode();

  /**
   * This is here for convenience; it makes sense for Point and Cone Dispatch.
   * TODO: probably should get rid of it.
   */
  public IClass getType() {
    Assertions.UNREACHABLE("getType not implemented for " + getClass());
    return null;
  }

}
