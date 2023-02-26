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
package com.ibm.wala.cast.tree;

public interface CAstSymbol {

  Object NULL_DEFAULT_VALUE =
      new Object() {
        @Override
        public String toString() {
          return "NULL DEFAULT VALUE";
        }
      };

  String name();

  /** like final in Java; can only be declared / assigned once */
  boolean isFinal();

  boolean isCaseInsensitive();

  Object defaultInitValue();

  boolean isInternalName();

  CAstType type();
}
