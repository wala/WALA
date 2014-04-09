/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.collections;

/**
 * A filter "A or B"
 */
public class OrFilter<T> implements Filter<T> {

  public static <T> OrFilter<T> createOrFilter(Filter<T> a, Filter<T> b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException("cannot compose null filter");
    }
    return new OrFilter<T>(a, b);
  }

  private final Filter<T> a;
  private final Filter<T> b;
  
  private OrFilter(Filter<T> a, Filter<T> b) {
    this.a = a;
    this.b = b;
  }
  
  public boolean accepts(T o) {
    return a.accepts(o) || b.accepts(o);
  }

}
