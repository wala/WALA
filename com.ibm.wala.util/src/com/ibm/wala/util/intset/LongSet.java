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
 * Set of longs; not necessary mutable TODO: extract a smaller interface?
 */
public interface LongSet {

  /**
   * @param i
   * @return true iff this set contains long i
   */
  public boolean contains(long i);

  /**
   * @return true iff this set contains integer i
   */
  public boolean containsAny(LongSet set);

  /**
   * This implementation must not despoil the original value of "this"
   * 
   * @return a new IntSet which is the intersection of this and that
   */
  public LongSet intersection(LongSet that);

  /**
   * @return true iff this set is empty
   */
  public boolean isEmpty();

  /**
   * @return the number of elements in this set
   */
  public int size();
  

  /**
   * @return maximum integer in this set.
   */
  public long max();

  /**
   * @return true iff <code>this</code> has the same value as
   *         <code>that</code>.
   */
  public boolean sameValue(LongSet that);

  /**
   * @return true iff <code>this</code> is a subset of <code>that</code>.
   */
  public boolean isSubset(LongSet that);

  /**
   * @return a perhaps more efficient iterator
   */
  public LongIterator longIterator();

  /**
   * Invoke an action on each element of the Set
   * 
   * @param action
   */
  public void foreach(LongSetAction action);

  /**
   * Invoke an action on each element of the Set, excluding elements of Set X
   * 
   * @param action
   */
  public void foreachExcluding(LongSet X, LongSetAction action);

}
