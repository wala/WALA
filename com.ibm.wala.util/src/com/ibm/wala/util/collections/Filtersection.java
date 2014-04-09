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


/**
 * intersection of two filters
 */
public class Filtersection<T> implements Filter<T> {
  
  final private Filter<T> a;
  final private Filter<T> b;
  
  public Filtersection(Filter<T> a, Filter<T> b) {
    this.a = a;
    this.b = b;
    if (a == null) {
      throw new IllegalArgumentException("null a");
    }
    if (b == null) {
      throw new IllegalArgumentException("null b");
    }
  }

  public boolean accepts(T o) {
    return a.accepts(o) && b.accepts(o);
  }

}
