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
public interface MutableLongSet extends LongSet {

  /**
   * Set the value of this to be the same as the value of set
   * 
   * @param set
   */
  void copySet(LongSet set);

  /**
   * Add all members of set to this.
   * 
   * @param set
   * @return true iff the value of this changes.
   */
  boolean addAll(LongSet set);

  /**
   * Add an integer value to this set.
   * 
   * @param i
   * @return true iff the value of this changes.
   */
  boolean add(long i);

  /**
   * Remove an integer from this set.
   * 
   * @param i
   */
  void remove(long i);

  /**
   * Interset this with another set.
   * 
   * @param set
   */
  void intersectWith(LongSet set);

}
