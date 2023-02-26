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

import java.io.Serializable;

/** Set of integers; not necessary mutable TODO: extract a smaller interface? */
public interface IntSet extends Serializable {

  /** @return true iff this set contains integer i */
  boolean contains(int i);

  /** @return true iff this set contains integer i */
  boolean containsAny(IntSet set);

  /**
   * This implementation must not despoil the original value of "this"
   *
   * @return a new IntSet which is the intersection of this and that
   */
  IntSet intersection(IntSet that);

  /**
   * This implementation must not despoil the original value of "this"
   *
   * @return a new IntSet containing all elements of this and that
   */
  IntSet union(IntSet that);

  /** @return true iff this set is empty */
  boolean isEmpty();

  /** @return the number of elements in this set */
  int size();

  /** @return a perhaps more efficient iterator */
  IntIterator intIterator();

  /** Invoke an action on each element of the Set */
  void foreach(IntSetAction action);

  /** Invoke an action on each element of the Set, excluding elements of Set X */
  void foreachExcluding(IntSet X, IntSetAction action);

  /** @return maximum integer in this set. */
  int max();

  /** @return true iff {@code this} has the same value as {@code that}. */
  boolean sameValue(IntSet that);

  /** @return true iff {@code this} is a subset of {@code that}. */
  boolean isSubset(IntSet that);
}
