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

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

/**
 * 
 * A relation between non-negative integers
 * 
 * This implementation uses n IntVectors, to hold the first n y's associated
 * with each x, and then 1 extra vector of SparseIntSet to hold the remaining
 * ys.
 * 
 * @author sfink
 */
public final class BasicNonNegativeIntRelation implements IBinaryNonNegativeIntRelation {

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
   * <li> if >=0, the ith integer associated with x
   * <li> if -2, then use the delegateStore instead of the small store
   * <li> if -1, then R(x) is empty
   * </ul>
   * represented.
   */
  final IntVector[] smallStore;

  /**
   * delegateStore[x] holds an int set of the y's s.t. R(x,y)
   */
  final IVector<IntSet> delegateStore;

  /**
   * @param implementation
   *          a set of codes that represent how the first n IntVectors should be
   *          implemented.
   * @param vectorImpl
   *          a code that indicates how to represent the delegateStore.
   * 
   * For example implementation =
   * {SIMPLE_INT_VECTOR,TWO_LEVEL_INT_VECTOR,TWO_LEVEL_INT_VECTOR} will result
   * in an implementation where the first 3 y's associated with each x are
   * represented in IntVectors. The IntVector for the first y will be
   * implemented with a SimpleIntVector, and the 2nd and 3rd are implemented
   * with TwoLevelIntVector
   */
  public BasicNonNegativeIntRelation(byte[] implementation, byte vectorImpl) {
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
        Assertions.UNREACHABLE();
        break;
      }
    }
    switch (vectorImpl) {
    case SIMPLE:
      delegateStore = new SimpleVector<IntSet>();
      break;
    case TWO_LEVEL:
      delegateStore = new TwoLevelVector<IntSet>();
      break;
    default:
      Assertions.UNREACHABLE();
      delegateStore = null;
      break;
    }
  }

  /**
   * a Default contructor
   */
  public BasicNonNegativeIntRelation() {
    this(new byte[] { SIMPLE }, TWO_LEVEL);
  }

  /**
   * maximum x for any pair in this relation.
   */
  private int maxX = -1;

  /**
   * Add (x,y) to the relation.
   * 
   * This is performance-critical, so the implementation looks a little ugly in
   * order to help out the compiler with redundancy elimination.
   * 
   * @param x
   * @param y
   * @return true iff the relation changes as a result of this call.
   */
  public boolean add(int x, int y) throws IllegalArgumentException {
    if (Assertions.verifyAssertions) {
      if (x < 0) {
        throw new IllegalArgumentException("illegal x: " + x);
      }
      if (y < 0) {
        throw new IllegalArgumentException("illegal y: " + y);
      }
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
          MutableIntSet s = new BimodalMutableIntSet(ssLength+1, 1.1f);
          delegateStore.set(x, s);
          for (int j = 0; j < smallStore.length; j++) {
            IntVector vv = smallStore[j];
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

  /**
   * @return Iterator of IntPair
   */
  public Iterator<IntPair> iterator() {
    return new TotalIterator();
  }

  private class TotalIterator implements Iterator<IntPair> {

    /**
     * the next x that will be returned in a pair (x,y), or -1 if not hasNext()
     */
    private int nextX = -1;

    /**
     * the source of the next y ... an integer between 0 and smallStore.length ..
     * nextIndex == smallStore.length means use the delegateIterator
     */
    private int nextIndex = -1;

    private IntIterator delegateIterator = null;

    TotalIterator() {
      advanceX();
    }

    private void advanceX() {
      delegateIterator = null;
      for (int i = nextX + 1; i <= maxX; i++) {
        if (anyRelationWithX(i)) {
          nextX = i;
          nextIndex = getFirstIndex(i);
          if (nextIndex == smallStore.length) {
            IntSet s = (IntSet) delegateStore.get(i);
            if (Assertions.verifyAssertions) {
              Assertions._assert(s.size() > 0);
            }
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

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return nextX != -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      Assertions.UNREACHABLE();
    }

  }

  private IntSet getDelegate(int x) {
    return (IntSet) delegateStore.get(x);
  }

  /**
   * @param x
   * @return true iff there exists pair (x,y) for some y
   */
  private boolean anyRelationWithX(int x) {
    return smallStore[0].get(x) != EMPTY_CODE;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation#getRelated(int)
   */
  public IntSet getRelated(int x) {
    if (DEBUG) {
      Assertions._assert(x >= 0);
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
            MutableSparseIntSet result = new MutableSparseIntSet(ssLength);
            for (int i = 0; i < smallStore.length; i++) {
              if (smallStore[i].get(x) == EMPTY_CODE) {
                break;
              }
              result.add(smallStore[i].get(x));
            }
            return result;
          }
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation#getRelatedCount(int)
   */
  public int getRelatedCount(int x) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(x >= 0);
    }
    if (!anyRelationWithX(x)) {
      return 0;
    } else {
      if (usingDelegate(x)) {
        return getDelegate(x).size();
      } else {
        int result = 0;
        for (int i = 0; i < smallStore.length; i++) {
          if (smallStore[i].get(x) == EMPTY_CODE) {
            break;
          }
          result++;
        }
        return result;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation#remove(int,
   *      int)
   */
  public void remove(int x, int y) {
    if (usingDelegate(x)) {
      // TODO: switch representation back to small store?
      MutableIntSet s = (MutableIntSet) delegateStore.get(x);
      s.remove(y);
      if (s.size() == 0) {
        delegateStore.set(x,null);
        for (int i = 0; i < smallStore.length; i++) {
          smallStore[i].set(x, EMPTY_CODE);
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

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation#removeAll(int)
   */
  public void removeAll(int x) {
    for (int i = 0; i < smallStore.length; i++) {
      smallStore[i].set(x, EMPTY_CODE);
    }
    delegateStore.set(x, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  public void performVerboseAction() {
    if (VERBOSE) {
      Trace.println(getClass() + " stats:");
      Trace.println("count: " + countPairs());
      Trace.println("delegate: " + delegateStore.getClass());
      delegateStore.performVerboseAction();
      for (int i = 0; i < smallStore.length; i++) {
        Trace.println("smallStore[" + i + "]: " + smallStore[i].getClass());
        smallStore[i].performVerboseAction();
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
    for (Iterator it = iterator(); it.hasNext();) {
      it.next();
      result++;
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IBinaryNonNegativeIntRelation#contains(int,
   *      int)
   */
  public boolean contains(int x, int y) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(x >= 0);
      Assertions._assert(y >= 0);
    }
    if (usingDelegate(x)) {
      return getDelegate(x).contains(y);
    } else {
      for (int i = 0; i < smallStore.length; i++) {
        if (smallStore[i].get(x) == y) {
          return true;
        }
      }
      return false;
    }
  }

  public int maxKeyValue() {
    return maxX;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
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
