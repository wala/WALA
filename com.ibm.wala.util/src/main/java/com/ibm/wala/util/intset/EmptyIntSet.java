/*
 * Copyright (c) 2011 - IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.intset;

import java.util.NoSuchElementException;

public class EmptyIntSet implements IntSet {

  private static final long serialVersionUID = 5116475799916663164L;
  public static EmptyIntSet instance = new EmptyIntSet();

  @Override
  public boolean contains(int i) {
    return false;
  }

  @Override
  public boolean containsAny(IntSet set) {
    return false;
  }

  @Override
  public IntSet intersection(IntSet that) {
    return this;
  }

  @Override
  public IntSet union(IntSet that) {
    return that;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public int size() {
    return 0;
  }

  private static final IntIterator emptyIter =
      new IntIterator() {

        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public int next() {
          throw new NoSuchElementException();
        }
      };

  @Override
  public IntIterator intIterator() {
    return emptyIter;
  }

  @Override
  public void foreach(IntSetAction action) {}

  @Override
  public void foreachExcluding(IntSet X, IntSetAction action) {}

  @Override
  public int max() {
    throw new NoSuchElementException();
  }

  @Override
  public boolean sameValue(IntSet that) {
    return that.isEmpty();
  }

  @Override
  public boolean isSubset(IntSet that) {
    return true;
  }
}
