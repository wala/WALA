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

import java.util.Comparator;

/**
 * A comparator based on hash codes
 */
public class HashCodeComparator<T> implements Comparator<T> {

  @SuppressWarnings("rawtypes")
  private static final HashCodeComparator INSTANCE = new HashCodeComparator();

  /*
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  public int compare(T o1, T o2) throws NullPointerException {
    // by convention null is less than non-null
    if (o1 == null) {
      return o2 == null ? 0 : -1;
    } else if (o2 == null) {
      return 1;
    }
    return o1.hashCode() - o2.hashCode();
  }

  @SuppressWarnings("unchecked")
  public static <T> HashCodeComparator<T> instance() {
    return INSTANCE;
  }

}
