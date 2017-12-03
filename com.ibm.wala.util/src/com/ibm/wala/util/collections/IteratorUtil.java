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

import java.util.Iterator;
/**
 * utilities dealing with Iterators
 */
public class IteratorUtil {

  /**
   * @return true iff the Iterator returns some elements which equals() the object o
   */
  public static <T> boolean contains(Iterator<? extends T> it, T o) {
    if (it == null) {
      throw new IllegalArgumentException("null it");
    }
    while (it.hasNext()) {
      if (o.equals(it.next())) {
        return true;
      }
    }
    return false;
  }

  public final static <T> int count(Iterator<T> it) throws IllegalArgumentException {
    if (it == null) {
      throw new IllegalArgumentException("it == null");
    }
    int count = 0;
    while (it.hasNext()) {
      it.next();
      count++;
    }
    return count;
  }

  public static <T, S extends T> Iterator<S> filter(Iterator<T> iterator, final Class<S> cls) {
    return new MapIterator<>(new FilterIterator<>(iterator, cls::isInstance), cls::cast);
  }
}
