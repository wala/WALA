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
 * A comparator based on lexicographical ordering of toString()
 */
public class ToStringComparator<T> implements Comparator<T> {

  @SuppressWarnings("rawtypes")
  private static final ToStringComparator INSTANCE = new ToStringComparator();
  
  private ToStringComparator() {}
  
  /*
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(T o1, T o2) throws NullPointerException {
    // by convention, null is the least element
    if (o1 == null) {
      return o2 == null ? 0 : -1;
    } else if (o2 == null) {
      return 1;
    }
    return o1.toString().compareTo(o2.toString());
  }
  
  public static <T> ToStringComparator<T> instance() {
    return INSTANCE;
  }

}
