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

import java.util.function.Predicate;

/** A filter that accepts everything. */
public class IndiscriminateFilter<T> implements Predicate<T> {

  public static <T> IndiscriminateFilter<T> singleton() {
    return new IndiscriminateFilter<>();
  }

  /** @see FilterIterator */
  @Override
  public boolean test(Object o) {
    return true;
  }
}
