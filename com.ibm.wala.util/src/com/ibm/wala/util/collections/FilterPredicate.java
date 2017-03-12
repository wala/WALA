/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
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
 * A migration aid, to move from Filter to Predicate
 */
@Deprecated
public class FilterPredicate<T> extends Predicate<T> {
  
  public static <T> FilterPredicate<T> toPredicate(Predicate<T> f) {
    return new FilterPredicate<>(f);
  }
  
  private final Predicate<T> f;
  
  private FilterPredicate(Predicate<T> f) {
    this.f = f;
  }

  @Override
  public boolean test(T t) {
    return f.test(t);
  }
 
}
