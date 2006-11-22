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

package com.ibm.wala.util.intset;

/**
 * @author sfink
 *
 */
public interface MutableIntSet extends IntSet {

  /**
   * Set the value of this to be the same as the value of set
   * 
   * @param set
   */
  void copySet(IntSet set);

  /**
   * Add all members of set to this.
   * 
   * @param set
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

  /**
   * Interset this with another set.
   * 
   * @param set
   */
  void intersectWith(IntSet set);

  /**
   * @param other
   * @param filter
   */
  boolean addAllInIntersection(IntSet other, IntSet filter);

}
