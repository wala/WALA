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

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * A debugging factory that creates debugging bitsets that are implemented as two bitsets that
 * perform consistency checks for every operation.
 */
public class DebuggingMutableIntSetFactory implements MutableIntSetFactory<DebuggingMutableIntSet> {

  private MutableIntSetFactory<?> primary;

  private MutableIntSetFactory<?> secondary;

  public DebuggingMutableIntSetFactory(MutableIntSetFactory<?> p, MutableIntSetFactory<?> s) {
    primary = p;
    secondary = s;
    if (p == null) {
      throw new IllegalArgumentException("null p");
    }
    if (s == null) {
      throw new IllegalArgumentException("null s");
    }
  }

  public DebuggingMutableIntSetFactory() {
    this(new MutableSparseIntSetFactory(), new MutableSharedBitVectorIntSetFactory());
  }

  @Override
  public DebuggingMutableIntSet make(int[] set) {
    if (set == null) {
      throw new IllegalArgumentException("null set");
    }
    return new DebuggingMutableIntSet(primary.make(set), secondary.make(set));
  }

  @Override
  public DebuggingMutableIntSet parse(String string) {
    int[] backingStore = SparseIntSet.parseIntArray(string);
    return make(backingStore);
  }

  @Override
  public DebuggingMutableIntSet makeCopy(IntSet x) throws UnimplementedError {
    if (x == null) {
      throw new IllegalArgumentException("null x");
    }
    if (x instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) x;
      MutableIntSet pr = primary.makeCopy(db.primaryImpl);
      MutableIntSet sr = secondary.makeCopy(db.secondaryImpl);

      assert pr.sameValue(db.primaryImpl);
      assert sr.sameValue(db.secondaryImpl);
      assert pr.sameValue(sr);

      return new DebuggingMutableIntSet(pr, sr);
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  @Override
  public DebuggingMutableIntSet make() {
    return new DebuggingMutableIntSet(primary.make(), secondary.make());
  }

  public void setPrimaryFactory(MutableIntSetFactory<?> x) {
    if (x == null) {
      throw new IllegalArgumentException("null x");
    }
    if (x == this) {
      throw new IllegalArgumentException("bad recursion");
    }
    primary = x;
  }

  public void setSecondaryFactory(MutableIntSetFactory<?> x) {
    if (x == null) {
      throw new IllegalArgumentException("null x");
    }
    if (x == this) {
      throw new IllegalArgumentException("bad recursion");
    }
    secondary = x;
  }
}
