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

/**
 * Utilities for dealing with IntSets
 * 
 * @author sfink
 */
public class IntSetUtil {

  public static final String INT_SET_FACTORY_CONFIG_PROPERTY_NAME = "com.ibm.wala.mutableIntSetFactory";
  
  private static MutableIntSetFactory defaultIntSetFactory;
  
  static {
	  MutableIntSetFactory defaultFactory = new MutableSharedBitVectorIntSetFactory();
	  if (System.getProperties().containsKey(INT_SET_FACTORY_CONFIG_PROPERTY_NAME)) {
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
   */
  public static MutableIntSet makeMutableCopy(IntSet set) {
    if (set instanceof SparseIntSet) {
      return new MutableSparseIntSet(set);
    } else if (set instanceof BitVectorIntSet) {
      return new BitVectorIntSet(set);
    } else if (set instanceof BimodalMutableIntSet) {
      return BimodalMutableIntSet.makeCopy(set);
    } else if (set instanceof MutableSharedBitVectorIntSet) {
      return new MutableSharedBitVectorIntSet((MutableSharedBitVectorIntSet) set);
    } else if (set instanceof SemiSparseMutableIntSet) {
      return new SemiSparseMutableIntSet((SemiSparseMutableIntSet) set);
    } else if (set instanceof DebuggingMutableIntSet) {
    	MutableIntSet pCopy = makeMutableCopy(((DebuggingMutableIntSet)set).primaryImpl);
    	MutableIntSet sCopy = makeMutableCopy(((DebuggingMutableIntSet)set).secondaryImpl);
    	return new DebuggingMutableIntSet(pCopy, sCopy);
    } else {
      Assertions.UNREACHABLE(set.getClass().toString());
      return null;
    }
  }

  /**
   * Compute the asymmetric difference of two sets, a \ b.
   * 
   * @param A
   * @param B
   */
  public static IntSet diff(IntSet A, IntSet B) {
    return diff(A, B, IntSetUtil.getDefaultIntSetFactory());
  }
  /**
   * Compute the asymmetric difference of two sets, a \ b.
   * 
   * @param A
   * @param B
   */
  public static IntSet diff(IntSet A, IntSet B, MutableIntSetFactory factory) {
    if (DEBUG) {
      Trace.println("diff " + A + " " + B);
    }
    if (A instanceof SparseIntSet && B instanceof SparseIntSet) {
      if (DEBUG) {
        Trace.println("call SparseIntSet.diff");
      }
      return SparseIntSet.diff((SparseIntSet) A, (SparseIntSet) B);
    } else {
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
  }

  /**
   * @param data
   * @param key
   * @param low
   * @param high
   * @return index \in [low,high] s.t. data[index] = key, or -1 if not found
   */
  public static int binarySearch(int[] data, int key, int low, int high) {
    if (low <= high) {
      int mid = (low + high) / 2;
      int midValue = data[mid];
      if (midValue == key)
        return mid;
      else if (midValue > key)
        return binarySearch(data, key, low, mid - 1);
      else
        return binarySearch(data, key, mid + 1, high);
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
    IntSetUtil.defaultIntSetFactory = defaultIntSetFactory;
  }
  /**
   * @param s
   * @param j
   * @return a new sparse int set which adds j to s
   */
  public static IntSet add(IntSet s, int j) {
    if (s instanceof SparseIntSet) {
      SparseIntSet sis = (SparseIntSet)s;
      return SparseIntSet.add(sis,j);
    } else {
      Assertions.UNREACHABLE("implement me");
      return null;
    }
  }
}