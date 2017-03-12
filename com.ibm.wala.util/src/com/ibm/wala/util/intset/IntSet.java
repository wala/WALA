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

import java.io.Serializable;

/**
 * Set of integers; not necessary mutable TODO: extract a smaller interface?
 */
public interface IntSet extends Serializable {

  /**
   * @return true iff this set contains integer i
   */
  public boolean contains(int i);

  /**
   * @return true iff this set contains integer i
   */
  public boolean containsAny(IntSet set);

  /**
   * This implementation must not despoil the original value of "this"
   * 
   * @return a new IntSet which is the intersection of this and that
   */
  public IntSet intersection(IntSet that);

  /**
   * This implementation must not despoil the original value of "this"
   * 
   * @return a new IntSet containing all elements of this and that
   */
  public IntSet union(IntSet that);

  /**
   * @return true iff this set is empty
   */
  public boolean isEmpty();

  /**
   * @return the number of elements in this set
   */
  public int size();

  /**
   * @return a perhaps more efficient iterator
   */
  public IntIterator intIterator();

  /**
   * Invoke an action on each element of the Set
   */
  public void foreach(IntSetAction action);

  /**
   * Invoke an action on each element of the Set, excluding elements of Set X
   */
  public void foreachExcluding(IntSet X, IntSetAction action);

  /**
   * @return maximum integer in this set.
   */
  public int max();

  /**
   * @return true iff <code>this</code> has the same value as <code>that</code>.
   */
  public boolean sameValue(IntSet that);

  /**
   * @return true iff <code>this</code> is a subset of <code>that</code>.
   */
  public boolean isSubset(IntSet that);

}
