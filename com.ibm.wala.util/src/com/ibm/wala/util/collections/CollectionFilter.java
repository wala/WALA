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

package com.ibm.wala.util.collections;

import java.util.Collection;

import com.ibm.wala.util.Predicate;

/**
 * A filter defined by set membership
 */
public class CollectionFilter<T> extends Predicate<T> {

  private final Collection<? extends T> S;

  public CollectionFilter(Collection<? extends T> S) {
    if (S == null) {
      throw new IllegalArgumentException("null S");
    }
    this.S = S;
  }

  /*
   * @see com.ibm.wala.util.Filter#accepts(java.lang.Object)
   */
  @Override public boolean test(T o) {
    return S.contains(o);
  }

}
