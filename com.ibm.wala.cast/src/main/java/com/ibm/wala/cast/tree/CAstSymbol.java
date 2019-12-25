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

  public static Object NULL_DEFAULT_VALUE =
      new Object() {
        @Override
        public String toString() {
          return "NULL DEFAULT VALUE";
        }
      };

  public String name();

  /** like final in Java; can only be declared / assigned once */
  public boolean isFinal();

  public boolean isCaseInsensitive();

  public Object defaultInitValue();

  public boolean isInternalName();

  public CAstType type();
}
