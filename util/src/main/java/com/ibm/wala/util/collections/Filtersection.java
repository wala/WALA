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
package com.ibm.wala.util.collections;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.function.Predicate;

/** intersection of two filters */
public class Filtersection<T> implements Predicate<T> {

  private final Predicate<T> a;
  private final Predicate<T> b;

  public Filtersection(Predicate<T> a, Predicate<T> b) {
    this.a = a;
    this.b = b;
    checkArgument(a != null, "null a");
    checkArgument(b != null, "null b");
  }

  @Override
  public boolean test(T o) {
    return a.test(o) && b.test(o);
  }
}
