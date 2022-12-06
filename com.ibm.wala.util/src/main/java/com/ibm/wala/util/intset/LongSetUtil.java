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

/** Utilities for dealing with LongSets */
public class LongSetUtil {

  public static final String INT_SET_FACTORY_CONFIG_PROPERTY_NAME =
      "com.ibm.wala.mutableLongSetFactory";

  private static MutableLongSetFactory defaultLongSetFactory;

  static {
    MutableLongSetFactory defaultFactory = new MutableSparseLongSetFactory();
    if (System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME) != null) {
      try {
        Class<?> intSetFactoryClass =
            Class.forName(System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME));
        MutableLongSetFactory intSetFactory =
            (MutableLongSetFactory) intSetFactoryClass.getDeclaredConstructor().newInstance();
        setDefaultLongSetFactory(intSetFactory);
      } catch (Exception e) {
        System.err.println(
            ("Cannot use int set factory "
                + System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME)));
        setDefaultLongSetFactory(defaultFactory);
      }
    } else {
      setDefaultLongSetFactory(defaultFactory);
    }
    assert defaultLongSetFactory != null;
  }

  public static MutableLongSet make() {
    return defaultLongSetFactory.make();
  }

  private static final boolean DEBUG = false;

  /**
   * This method constructs an appropriate mutable copy of set.
   *
   * @return a new MutableLongSet object with the same value as set
   * @throws UnimplementedError if (not ( set instanceof com.ibm.wala.util.intset.SparseLongSet ) )
   *     and (not ( set instanceof com.ibm.wala.util.intset.BitVectorLongSet ) ) and (not ( set
   *     instanceof com.ibm.wala.util.intset.BimodalMutableLongSet ) ) and (not ( set instanceof
   *     com.ibm.wala.util.intset.DebuggingMutableLongSet ) ) and (not ( set instanceof
   *     com.ibm.wala.util.intset.SemiSparseMutableLongSet ) ) and (not ( set instanceof
   *     com.ibm.wala.util.intset.MutableSharedBitVectorLongSet ) )
   * @throws IllegalArgumentException if set == null
   */
  public static MutableLongSet makeMutableCopy(LongSet set)
      throws IllegalArgumentException, UnimplementedError {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    if (set instanceof SparseLongSet) {
      return MutableSparseLongSet.make(set);
    } else {
      Assertions.UNREACHABLE(set.getClass().toString());
      return null;
    }
  }

  /** Compute the asymmetric difference of two sets, a \ b. */
  public static LongSet diff(LongSet A, LongSet B) {
    if (A == null) {
      throw new IllegalArgumentException("null A");
    }
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
    return diff(A, B, LongSetUtil.getDefaultLongSetFactory());
  }

  private static LongSet defaultSlowDiff(LongSet A, LongSet B, MutableLongSetFactory factory) {
    // TODO: this is slow ... optimize please.
    MutableLongSet result = factory.makeCopy(A);
    if (DEBUG) {
      System.err.println(("initial result " + result + ' ' + result.getClass()));
    }
    for (LongIterator it = B.longIterator(); it.hasNext(); ) {
      long I = it.next();
      result.remove(I);
      if (DEBUG) {
        System.err.println(("removed " + I + " now is " + result));
      }
    }
    if (DEBUG) {
      System.err.println(("return " + result));
    }
    return result;
  }

  /** Compute the asymmetric difference of two sets, a \ b. */
  public static LongSet diff(LongSet A, LongSet B, MutableLongSetFactory factory) {
    if (A == null) {
      throw new IllegalArgumentException("null A");
    }
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
    if (A instanceof SparseLongSet && B instanceof SparseLongSet) {
      return SparseLongSet.diff((SparseLongSet) A, (SparseLongSet) B);
    } else {
      return defaultSlowDiff(A, B, factory);
    }
  }

  /**
   * Subtract two sets, i.e. a = a \ b.
   *
   * @throws IllegalArgumentException if A == null || B == null
   */
  public static MutableLongSet removeAll(MutableLongSet A, LongSet B)
      throws IllegalArgumentException {
    if (A == null) {
      throw new IllegalArgumentException("A == null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B == null");
    }
    for (LongIterator it = B.longIterator(); it.hasNext(); ) {
      long I = it.next();
      A.remove(I);
      if (DEBUG) {
        System.err.println(("removed " + I + " now is " + A));
      }
    }
    if (DEBUG) {
      System.err.println(("return " + A));
    }
    return A;
  }

  /** @return index \in [low,high] s.t. data[index] = key, or -1 if not found */
  public static int binarySearch(long[] data, long key, int low, int high)
      throws IllegalArgumentException {
    if (data == null) {
      throw new IllegalArgumentException("null array");
    }
    if (data.length == 0) {
      return -1;
    }
    if (low <= high && (low < 0 || high < 0)) {
      throw new IllegalArgumentException("can't search negative indices");
    }
    if (high > data.length - 1) {
      high = data.length - 1;
    }
    if (low <= high) {
      int mid = (low + high) / 2;
      long midValue = data[mid];
      if (midValue == key) {
        return mid;
      } else if (midValue > key) {
        return binarySearch(data, key, low, mid - 1);
      } else {
        return binarySearch(data, key, mid + 1, high);
      }
    } else {
      return -1;
    }
  }

  public static MutableLongSetFactory getDefaultLongSetFactory() {
    return defaultLongSetFactory;
  }

  public static void setDefaultLongSetFactory(MutableLongSetFactory defaultLongSetFactory) {
    if (defaultLongSetFactory == null) {
      throw new IllegalArgumentException("null defaultLongSetFactory");
    }
    LongSetUtil.defaultLongSetFactory = defaultLongSetFactory;
  }

  /**
   * @return a new sparse int set which adds j to s
   * @throws IllegalArgumentException if s == null
   */
  public static LongSet add(LongSet s, int j) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s == null");
    }
    if (s instanceof SparseLongSet) {
      SparseLongSet sis = (SparseLongSet) s;
      return SparseLongSet.add(sis, j);
    } else {
      // really slow. optimize as needed.
      MutableSparseLongSet result = MutableSparseLongSet.make(s);
      result.add(j);
      return result;
    }
  }
}
