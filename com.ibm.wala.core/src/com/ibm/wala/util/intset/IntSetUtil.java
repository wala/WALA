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

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * Utilities for dealing with IntSets
 */
public class IntSetUtil {

  public static final String INT_SET_FACTORY_CONFIG_PROPERTY_NAME = "com.ibm.wala.mutableIntSetFactory";

  private static MutableIntSetFactory defaultIntSetFactory;

  static {
    MutableIntSetFactory defaultFactory = new MutableSharedBitVectorIntSetFactory();
    if (System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME) != null) {
      try {
        Class intSetFactoryClass = Class.forName(System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME));
        MutableIntSetFactory intSetFactory = (MutableIntSetFactory) intSetFactoryClass.newInstance();
        setDefaultIntSetFactory(intSetFactory);
      } catch (Exception e) {
        Trace.println("Cannot use int set factory " + System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME));
        setDefaultIntSetFactory(defaultFactory);
      }
    } else {
      setDefaultIntSetFactory(defaultFactory);
    }
    assert defaultIntSetFactory != null;
  }

  public static MutableIntSet make() {
    return defaultIntSetFactory.make();
  }

  private final static boolean DEBUG = false;

  /**
   * This method constructs an appropriate mutable copy of set.
   * 
   * @param set
   * @return a new MutableIntSet object with the same value as set
   * @throws UnimplementedError if (not ( set instanceof com.ibm.wala.util.intset.SparseIntSet ) ) and (not ( set instanceof
   *           com.ibm.wala.util.intset.BitVectorIntSet ) ) and (not ( set instanceof com.ibm.wala.util.intset.BimodalMutableIntSet
   *           ) ) and (not ( set instanceof com.ibm.wala.util.intset.DebuggingMutableIntSet ) ) and (not ( set instanceof
   *           com.ibm.wala.util.intset.SemiSparseMutableIntSet ) ) and (not ( set instanceof
   *           com.ibm.wala.util.intset.MutableSharedBitVectorIntSet ) )
   * @throws IllegalArgumentException if set == null
   */
  public static MutableIntSet makeMutableCopy(IntSet set) throws IllegalArgumentException, UnimplementedError {
    if (set == null) {
      throw new IllegalArgumentException("set == null");
    }
    if (set instanceof SparseIntSet) {
      return MutableSparseIntSet.make(set);
    } else if (set instanceof BitVectorIntSet) {
      return new BitVectorIntSet(set);
    } else if (set instanceof BimodalMutableIntSet) {
      return BimodalMutableIntSet.makeCopy(set);
    } else if (set instanceof MutableSharedBitVectorIntSet) {
      return new MutableSharedBitVectorIntSet((MutableSharedBitVectorIntSet) set);
    } else if (set instanceof SemiSparseMutableIntSet) {
      return new SemiSparseMutableIntSet((SemiSparseMutableIntSet) set);
    } else if (set instanceof DebuggingMutableIntSet) {
      MutableIntSet pCopy = makeMutableCopy(((DebuggingMutableIntSet) set).primaryImpl);
      MutableIntSet sCopy = makeMutableCopy(((DebuggingMutableIntSet) set).secondaryImpl);
      return new DebuggingMutableIntSet(pCopy, sCopy);
    } else {
      Assertions.UNREACHABLE(set.getClass().toString());
      return null;
    }
  }

  /**
   * Compute the asymmetric difference of two sets, a \ b.
   */
  public static IntSet diff(IntSet A, IntSet B) {
    if (A == null) {
      throw new IllegalArgumentException("null A");
    }
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
    return diff(A, B, IntSetUtil.getDefaultIntSetFactory());
  }

  private static IntSet defaultSlowDiff(IntSet A, IntSet B, MutableIntSetFactory factory) {
    // TODO: this is slow ... optimize please.
    MutableIntSet result = factory.makeCopy(A);
    if (DEBUG) {
      Trace.println("initial result " + result + " " + result.getClass());
    }
    for (IntIterator it = B.intIterator(); it.hasNext();) {
      int I = it.next();
      result.remove(I);
      if (DEBUG) {
        Trace.println("removed " + I + " now is " + result);
      }
    }
    if (DEBUG) {
      Trace.println("return " + result);
    }
    return result;
  }

  /**
   * Compute the asymmetric difference of two sets, a \ b.
   */
  public static IntSet diff(IntSet A, IntSet B, MutableIntSetFactory factory) {
    if (factory == null) {
      throw new IllegalArgumentException("null factory");
    }
    if (A == null) {
      throw new IllegalArgumentException("null A");
    }
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
    if (A instanceof SparseIntSet && B instanceof SparseIntSet) {
      return SparseIntSet.diff((SparseIntSet) A, (SparseIntSet) B);
    } else if (A instanceof SemiSparseMutableIntSet && B instanceof SemiSparseMutableIntSet) {
      IntSet d = SemiSparseMutableIntSet.diff((SemiSparseMutableIntSet) A, (SemiSparseMutableIntSet) B);
      return d;
    } else {
      return defaultSlowDiff(A, B, factory);
    }
  }

  /**
   * Subtract two sets, i.e. a = a \ b.
   * 
   * @throws IllegalArgumentException if B == null
   */
  public static MutableIntSet removeAll(MutableIntSet A, IntSet B) throws IllegalArgumentException {
    if (B == null) {
      throw new IllegalArgumentException("B == null");
    }
    if (A instanceof SemiSparseMutableIntSet && B instanceof SemiSparseMutableIntSet) {
      if (DEBUG) {
        Trace.println("call SemiSparseMutableIntSet.removeAll");
      }
      return ((SemiSparseMutableIntSet) A).removeAll((SemiSparseMutableIntSet) B);
    } else {
      for (IntIterator it = B.intIterator(); it.hasNext();) {
        int I = it.next();
        A.remove(I);
        if (DEBUG) {
          Trace.println("removed " + I + " now is " + A);
        }
      }
      if (DEBUG) {
        Trace.println("return " + A);
      }
      return A;
    }
  }

  /**
   * @return index \in [low,high] s.t. data[index] = key, or -1 if not found
   */
  public static int binarySearch(int[] data, int key, int low, int high) throws IllegalArgumentException {
    if (data == null) {
      throw new IllegalArgumentException("null array");
    }
    if (data.length == 0) {
      return -1;
    }
    if (low <= high && (low < 0 || high < 0)) {
      throw new IllegalArgumentException("can't search negative indices " + low + " " + high);
    }
    if (high > data.length - 1) {
      high = data.length - 1;
    }
    if (low <= high) {
      int mid = (low + high) / 2;
      int midValue = data[mid];
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

  /**
   * @return Returns the defaultIntSetFactory.
   */
  public static MutableIntSetFactory getDefaultIntSetFactory() {
    return defaultIntSetFactory;
  }

  /**
   * @param defaultIntSetFactory The defaultIntSetFactory to set.
   */
  public static void setDefaultIntSetFactory(MutableIntSetFactory defaultIntSetFactory) {
    if (defaultIntSetFactory == null) {
      throw new IllegalArgumentException("null defaultIntSetFactory");
    }
    IntSetUtil.defaultIntSetFactory = defaultIntSetFactory;
  }

  /**
   * @return a new sparse int set which adds j to s
   * @throws IllegalArgumentException if s == null
   */
  public static IntSet add(IntSet s, int j) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s == null");
    }
    if (s instanceof SparseIntSet) {
      SparseIntSet sis = (SparseIntSet) s;
      return SparseIntSet.add(sis, j);
    } else {
      // really slow. optimize as needed.
      MutableSparseIntSet result = MutableSparseIntSet.make(s);
      result.add(j);
      return result;
    }
  }
}
