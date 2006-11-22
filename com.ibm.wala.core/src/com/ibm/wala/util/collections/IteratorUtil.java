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
 * 
 * @author sfink
 * 
 */
public class IteratorUtil {

  /**
   * @param it
   * @param o
   * @return true iff the Iterator returns some elements which equals() the
   *         object o
   */
  public static <T> boolean contains(Iterator<? extends T> it, T o) {
    while (it.hasNext()) {
      if (o.equals(it.next())) {
        return true;
      }
    }
    return false;
  }

  public final static <T> int count(Iterator<T> it) {
    int count = 0;
    while (it.hasNext()) {
      it.next();
      count++;
    }
    return count;
  }
}
