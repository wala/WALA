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
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.types.TypeReference;

/**
 * Abstraction of a Java type. These are immutable.
 *
 * @see TypeInference
 */
public abstract class TypeAbstraction implements ContextItem {

  /** Canonical element representing TOP for a dataflow lattice */
  public static final TypeAbstraction TOP =
      new TypeAbstraction() {
        @Override
        public TypeAbstraction meet(TypeAbstraction rhs) {
          return rhs;
        }

        @Override
        public String toString() {
          return "WalaTypeAbstraction.TOP";
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

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  /** A TypeReference representing the types of this abstraction */
  public abstract TypeReference getTypeReference();

  /**
   * This is here for convenience; it makes sense for Point and Cone Dispatch. TODO: probably should
   * get rid of it.
   *
   * @throws UnsupportedOperationException unconditionally
   */
  public IClass getType() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("getType not implemented for " + getClass());
  }
}
