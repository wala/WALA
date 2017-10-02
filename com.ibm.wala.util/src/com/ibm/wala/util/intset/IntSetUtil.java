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

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * Utilities for dealing with {@link IntSet}s
 */
public class IntSetUtil {

  public static final String INT_SET_FACTORY_CONFIG_PROPERTY_NAME = "com.ibm.wala.mutableIntSetFactory";

  private static MutableIntSetFactory<?> defaultIntSetFactory;

  static {
    MutableIntSetFactory<?> defaultFactory = new MutableSharedBitVectorIntSetFactory();
    if (System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME) != null) {
      try {
        @SuppressWarnings("unchecked")
        Class<? extends MutableIntSetFactory<?>> intSetFactoryClass = (Class<? extends MutableIntSetFactory<?>>) Class.forName(System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME));
        MutableIntSetFactory<?> intSetFactory = intSetFactoryClass.newInstance();
        setDefaultIntSetFactory(intSetFactory);
      } catch (Exception e) {
        System.err.println(("Cannot use int set factory " + System.getProperty(INT_SET_FACTORY_CONFIG_PROPERTY_NAME)));
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

  public static MutableIntSet make(int[] initial) {
	    return defaultIntSetFactory.make(initial);
  }

  public static IntSet make(Set<Integer> x) {
    int[] vals = new int[ x.size() ];
    Iterator<Integer> vs = x.iterator();
    for(int i = 0; i < vals.length; i++) {
      vals[i] = vs.next();
    }
    return make(vals);
  }

  private final static boolean DEBUG = false;

  // there's no reason to instantiate this class
  private IntSetUtil() {
  }
  
  /**
   * This method constructs an appropriate mutable copy of set.
   * 
   * @return a new {@link MutableIntSet} object with the same value as set
   * @throws UnimplementedError if we haven't supported the set type yet.
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
    } else if (set instanceof EmptyIntSet) {
      return IntSetUtil.make();
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

  private static IntSet defaultSlowDiff(IntSet A, IntSet B, MutableIntSetFactory<?> factory) {
    // TODO: this is slow ... optimize please.
    MutableIntSet result = factory.makeCopy(A);
    if (DEBUG) {
      System.err.println(("initial result " + result + " " + result.getClass()));
    }
    for (IntIterator it = B.intIterator(); it.hasNext();) {
      int I = it.next();
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

  /**
   * Compute the asymmetric difference of two sets, a \ b.
   */
  public static IntSet diff(IntSet A, IntSet B, MutableIntSetFactory<?> factory) {
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
    if (A == null) {
      throw new IllegalArgumentException("A == null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B == null");
    }
    if (A instanceof SemiSparseMutableIntSet && B instanceof SemiSparseMutableIntSet) {
      if (DEBUG) {
        System.err.println("call SemiSparseMutableIntSet.removeAll");
      }
      return ((SemiSparseMutableIntSet) A).removeAll((SemiSparseMutableIntSet) B);
    } else {
      for (IntIterator it = B.intIterator(); it.hasNext();) {
        int I = it.next();
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
  public static MutableIntSetFactory<?> getDefaultIntSetFactory() {
    return defaultIntSetFactory;
  }

  /**
   * @param defaultIntSetFactory The defaultIntSetFactory to set.
   */
  public static void setDefaultIntSetFactory(MutableIntSetFactory<?> defaultIntSetFactory) {
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
  
  public static int[] toArray(IntSet s) {
    int i = 0;
    int[] result = new int[ s.size() ];
    IntIterator x = s.intIterator();
    while (x.hasNext()) {
      result[i++] = x.next();
    }
    assert ! x.hasNext();
    return result;
  }

}
