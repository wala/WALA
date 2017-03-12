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

import com.ibm.wala.util.Predicate;

/**
 * A filter "A or B"
 */
public class OrFilter<T> extends Predicate<T> {

  public static <T> OrFilter<T> createOrFilter(Predicate<T> a, Predicate<T> b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException("cannot compose null filter");
    }
    return new OrFilter<>(a, b);
  }

  private final Predicate<T> a;
  private final Predicate<T> b;
  
  private OrFilter(Predicate<T> a, Predicate<T> b) {
    this.a = a;
    this.b = b;
  }
  
  @Override public boolean test(T o) {
    return a.test(o) || b.test(o);
  }

}
