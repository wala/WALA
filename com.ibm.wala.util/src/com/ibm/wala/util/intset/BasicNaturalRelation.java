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
import java.util.Iterator;

import com.ibm.wala.util.collections.IVector;
import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.collections.TwoLevelVector;
import com.ibm.wala.util.debug.Assertions;

/**
 * A relation between non-negative integers
 * 
 * This implementation uses n IntVectors, to hold the first n y's associated with each x, and then 1 extra vector of SparseIntSet to
 * hold the remaining ys.
 */
public final class BasicNaturalRelation implements IBinaryNaturalRelation, Serializable {

  private static final long serialVersionUID = 4483720230344867621L;

  private final static boolean VERBOSE = false;

  private final static boolean DEBUG = false;

  /**
   * Tokens used as enumerated types to control the representation
   */
  public final static byte SIMPLE = 0;

  public final static byte TWO_LEVEL = 1;

  public final static byte SIMPLE_SPACE_STINGY = 2;

  private final static int EMPTY_CODE = -1;

  private final static int DELEGATE_CODE = -2;

  /**
   * smallStore[i][x] holds
   * <ul>
   * <li>if &gt;=0, the ith integer associated with x
   * <li>if -2, then use the delegateStore instead of the small store
   * <li>if -1, then R(x) is empty
   * </ul>
   * represented.
   */
  final IntVector[] smallStore;

  /**
   * delegateStore[x] holds an int set of the y's s.t. R(x,y)
   */
  final IVector<IntSet> delegateStore;

  /**
   * @param implementation a set of codes that represent how the first n IntVectors should be implemented.
   * @param vectorImpl a code that indicates how to represent the delegateStore.
   * 
   *          For example implementation = {SIMPLE_INT_VECTOR,TWO_LEVEL_INT_VECTOR,TWO_LEVEL_INT_VECTOR} will result in an
   *          implementation where the first 3 y's associated with each x are represented in IntVectors. The IntVector for the first
   *          y will be implemented with a SimpleIntVector, and the 2nd and 3rd are implemented with TwoLevelIntVector
   * 
   * @throws IllegalArgumentException if implementation is null
   * @throws IllegalArgumentException if implementation.length == 0
   */
  public BasicNaturalRelation(byte[] implementation, byte vectorImpl) throws IllegalArgumentException {

    if (implementation == null) {
      throw new IllegalArgumentException("implementation is null");
    }
    if (implementation.length == 0) {
      throw new IllegalArgumentException("implementation.length == 0");
    }
    smallStore = new IntVector[implementation.length];
    for (int i = 0; i < implementation.length; i++) {
      switch (implementation[i]) {
      case SIMPLE:
        smallStore[i] = new SimpleIntVector(EMPTY_CODE);
        break;
      case TWO_LEVEL:
        smallStore[i] = new TwoLevelIntVector(EMPTY_CODE);
        break;
      case SIMPLE_SPACE_STINGY:
        smallStore[i] = new TunedSimpleIntVector(EMPTY_CODE, 1, 1.1f);
        break;
      default:
        throw new IllegalArgumentException("unsupported implementation " + implementation[i]);
      }
    }
    switch (vectorImpl) {
    case SIMPLE:
      delegateStore = new SimpleVector<>();
      break;
    case TWO_LEVEL:
      delegateStore = new TwoLevelVector<>();
      break;
    default:
      throw new IllegalArgumentException("unsupported implementation " + vectorImpl);
    }
  }

  public BasicNaturalRelation() {
    this(new byte[] { SIMPLE }, TWO_LEVEL);
  }

  /**
   * maximum x for any pair in this relation.
   */
  private int maxX = -1;

  /**
   * Add (x,y) to the relation.
   * 
   * This is performance-critical, so the implementation looks a little ugly in order to help out the compiler with redundancy
   * elimination.
   * 
   * @return true iff the relation changes as a result of this call.
   */
  @Override
  public boolean add(int x, int y) throws IllegalArgumentException {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x: " + x);
    }
    if (y < 0) {
      throw new IllegalArgumentException("illegal y: " + y);
    }
    maxX = Math.max(maxX, x);
    MutableIntSet delegated = (MutableIntSet) delegateStore.get(x);
    if (delegated != null) {
      return delegated.add(y);
    } else {
      IntVector smallStore0 = smallStore[0];
      if (smallStore0.get(x) != EMPTY_CODE) {
        int i = 0;
        IntVector v = null;
        int ssLength = smallStore.length;
        for (; i < ssLength; i++) {
          v = smallStore[i];
          int val = v.get(x);
          if (val == y) {
            return false;
          } else if (val == EMPTY_CODE) {
            break;
          }
        }
        if (i == ssLength) {
          MutableIntSet s = new BimodalMutableIntSet(ssLength + 1, 1.1f);
          delegateStore.set(x, s);
          for (IntVector vv : smallStore) {
            s.add(vv.get(x));
            vv.set(x, DELEGATE_CODE);
          }
          s.add(y);
        } else {
          v.set(x, y);
        }
        return true;
      } else {
        // smallStore[0].get(x) == EMPTY_CODE : just add
        smallStore0.set(x, y);
        return true;
      }
    }
  }

  private boolean usingDelegate(int x) {
    return smallStore[0].get(x) == DELEGATE_CODE;
  }

  @Override
  public Iterator<IntPair> iterator() {
    return new TotalIterator();
  }

  private class TotalIterator implements Iterator<IntPair> {

    /**
     * the next x that will be returned in a pair (x,y), or -1 if not hasNext()
     */
    private int nextX = -1;

    /**
     * the source of the next y ... an integer between 0 and smallStore.length .. nextIndex == smallStore.length means use the
     * delegateIterator
     */
    private int nextIndex = -1;

    private IntIterator delegateIterator = null;

    TotalIterator() {
      advanceX();
    }

    private void advanceX() {
      delegateIterator = null;
      for (int i = nextX + 1; i <= maxX; i++) {
        if (anyRelated(i)) {
          nextX = i;
          nextIndex = getFirstIndex(i);
          if (nextIndex == smallStore.length) {
            IntSet s = delegateStore.get(i);
            assert s.size() > 0;
            delegateIterator = s.intIterator();
          }
          return;
        }
      }
      nextX = -1;
    }

    private int getFirstIndex(int x) {
      if (smallStore[0].get(x) >= 0) {
        // will get first y for x from smallStore[0][x]
        return 0;
      } else {
        // will get first y for x from delegateStore[x]
        return smallStore.length;
      }
    }

    @Override
    public boolean hasNext() {
      return nextX != -1;
    }

    @Override
    public IntPair next() {
      IntPair result = null;
      if (nextIndex == smallStore.length) {
        int y = delegateIterator.next();
        result = new IntPair(nextX, y);
        if (!delegateIterator.hasNext()) {
          advanceX();
        }
      } else {
        result = new IntPair(nextX, smallStore[nextIndex].get(nextX));
        if (nextIndex == (smallStore.length - 1) || smallStore[nextIndex + 1].get(nextX) == EMPTY_CODE) {
          advanceX();
        } else {
          nextIndex++;
        }
      }
      return result;
    }

    @Override
    public void remove() {
      Assertions.UNREACHABLE();
    }

  }

  private IntSet getDelegate(int x) {
    return delegateStore.get(x);
  }

  /**
   * @param x
   * @return true iff there exists pair (x,y) for some y
   */
  @Override
  public boolean anyRelated(int x) {
    return smallStore[0].get(x) != EMPTY_CODE;
  }

  /*
   * @see com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation#getRelated(int)
   */
  @Override
  public IntSet getRelated(int x) {
    if (DEBUG) {
      assert x >= 0;
    }
    int ss0 = smallStore[0].get(x);
    if (ss0 == EMPTY_CODE) {
      return null;
    } else {
      if (ss0 == DELEGATE_CODE) {
        return getDelegate(x);
      } else {
        int ssLength = smallStore.length;
        if (ssLength == 2) {
          int ss1 = smallStore[1].get(x);
          if (ss1 == EMPTY_CODE) {
            return SparseIntSet.singleton(ss0);
          } else {
            return SparseIntSet.pair(ss0, ss1);
          }
        } else if (ssLength == 1) {
          return SparseIntSet.singleton(ss0);
        } else {
          int ss1 = smallStore[1].get(x);
          if (ss1 == EMPTY_CODE) {
            return SparseIntSet.singleton(ss0);
          } else {
            MutableSparseIntSet result = MutableSparseIntSet.createMutableSparseIntSet(ssLength);
            for (IntVector element : smallStore) {
              if (element.get(x) == EMPTY_CODE) {
                break;
              }
              result.add(element.get(x));
            }
            return result;
          }
        }
      }
    }
  }

  /*
   * @see com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation#getRelatedCount(int)
   */
  @Override
  public int getRelatedCount(int x) throws IllegalArgumentException {
    if (x < 0) {
      throw new IllegalArgumentException("x must be greater than zero");
    }
    if (!anyRelated(x)) {
      return 0;
    } else {
      if (usingDelegate(x)) {
        return getDelegate(x).size();
      } else {
        int result = 0;
        for (IntVector element : smallStore) {
          if (element.get(x) == EMPTY_CODE) {
            break;
          }
          result++;
        }
        return result;
      }
    }
  }

  @Override
  public void remove(int x, int y) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x: " + x);
    }
    if (y < 0) {
      throw new IllegalArgumentException("illegal y: " + y);
    }
    if (usingDelegate(x)) {
      // TODO: switch representation back to small store?
      MutableIntSet s = (MutableIntSet) delegateStore.get(x);
      s.remove(y);
      if (s.size() == 0) {
        delegateStore.set(x, null);
        for (IntVector element : smallStore) {
          element.set(x, EMPTY_CODE);
        }
      }
    } else {
      for (int i = 0; i < smallStore.length; i++) {
        if (smallStore[i].get(x) == y) {
          for (int j = i; j < smallStore.length; j++) {
            if (j < (smallStore.length - 1)) {
              smallStore[j].set(x, smallStore[j + 1].get(x));
            } else {
              smallStore[j].set(x, EMPTY_CODE);
            }
          }
          return;
        }
      }
    }
  }

  @Override
  public void removeAll(int x) {
    for (IntVector element : smallStore) {
      element.set(x, EMPTY_CODE);
    }
    delegateStore.set(x, null);
  }

  /*
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  @Override
  public void performVerboseAction() {
    if (VERBOSE) {
      System.err.println((getClass() + " stats:"));
      System.err.println(("count: " + countPairs()));
      System.err.println(("delegate: " + delegateStore.getClass()));
      delegateStore.performVerboseAction();
      for (int i = 0; i < smallStore.length; i++) {
        System.err.println(("smallStore[" + i + "]: " + smallStore[i].getClass()));
      }
    }
  }

  /**
   * This is slow.
   * 
   * @return the size of this relation, in the number of pairs
   */
  private int countPairs() {
    int result = 0;
    for (@SuppressWarnings("unused") Object name : this) {
      result++;
    }
    return result;
  }

  @Override
  public boolean contains(int x, int y) {
    if (x < 0) {
      throw new IllegalArgumentException("invalid x: " + x);
    }
    if (y < 0) {
      throw new IllegalArgumentException("invalid y: " + y);
    }
    if (usingDelegate(x)) {
      return getDelegate(x).contains(y);
    } else {
      for (IntVector element : smallStore) {
        if (element.get(x) == y) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public int maxKeyValue() {
    return maxX;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i <= maxX; i++) {
      result.append(i).append(":");
      result.append(getRelated(i));
      result.append("\n");
    }
    return result.toString();
  }
}
