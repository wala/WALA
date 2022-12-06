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

package com.ibm.wala.util.intset;

/** An {@link IntSet} that can be changed. */
public interface MutableIntSet extends IntSet {

  /** Set the value of this to be the same as the value of set */
  void copySet(IntSet set);

  /**
   * Add all members of set to this.
   *
   * @return true iff the value of this changes.
   */
  boolean addAll(IntSet set);

  /**
   * Add an integer value to this set.
   *
   * @param i integer to add
   * @return true iff the value of this changes.
   */
  boolean add(int i);

  /**
   * Remove an integer from this set.
   *
   * @param i integer to remove
   * @return true iff the value of this changes.
   */
  boolean remove(int i);

  /** remove all elements from this set */
  void clear();

  /** Intersect this with another set. */
  void intersectWith(IntSet set);

  /** */
  boolean addAllInIntersection(IntSet other, IntSet filter);
}
