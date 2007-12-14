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

import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class wraps two concrete {@link MutableIntSet}s behind the standard interface, carrying
 * out all operations on both of them and performing consistency checks at
 * every step. The purpose of this is debugging bitset implementations.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 *  
 */
class DebuggingMutableIntSet implements MutableIntSet {

  final MutableIntSet primaryImpl;
  final MutableIntSet secondaryImpl;

  DebuggingMutableIntSet(MutableIntSet p, MutableIntSet s) {
    primaryImpl = p;
    secondaryImpl = s;
  }

  private void assertEquiv() {
    Assertions._assert(primaryImpl.sameValue(secondaryImpl));
  }

  /**
   * @param i
   * @return true iff this set contains integer i
   */
  public boolean contains(int i) {
    Assertions._assert(primaryImpl.contains(i) == secondaryImpl.contains(i));
    return primaryImpl.contains(i);
  }

  /**
   */
  public boolean isEmpty() {
    if (primaryImpl.isEmpty() != secondaryImpl.isEmpty()) {
      System.err.println(primaryImpl + ".isEmpty() = " + primaryImpl.isEmpty() + " and " + secondaryImpl + ".isEmpty() = "
          + secondaryImpl.isEmpty());
      Assertions.UNREACHABLE();
    }

    return primaryImpl.isEmpty();
  }

  /**
   */
  public int size() {
    if (primaryImpl.size() != secondaryImpl.size()) {
      Assertions._assert(
        primaryImpl.size() == secondaryImpl.size(),
        "size " + primaryImpl.size() + " of " + primaryImpl +
	" differs from " + 
	"size " + secondaryImpl.size() + " of " + secondaryImpl);
    }

    return primaryImpl.size();
  }

  public int max() {
    Assertions._assert(primaryImpl.max() == secondaryImpl.max());
    return primaryImpl.max();
  }

  /**
   * Add an integer value to this set.
   * 
   * @param i
   * @return true iff the value of this changes.
   */
  public boolean add(int i) {
    boolean pr = primaryImpl.add(i);
    boolean sr = secondaryImpl.add(i);

    if (pr != sr) {
      Assertions._assert(pr == sr, 
        "adding " + i + " to " + primaryImpl + " returns " + pr + 
	", but adding " + i + " to " + secondaryImpl + " returns " + sr);
    }

    return pr;
  }

  /**
   * Remove an integer from this set.
   * 
   * @param i
   */
  public boolean remove(int i) {
    boolean result = primaryImpl.remove(i);
    secondaryImpl.remove(i);
    assertEquiv();
    return result;
  }

  /**
   * @return true iff this set contains integer i
   */
  public boolean containsAny(IntSet set) {
    if (set instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) set;
      boolean ppr = primaryImpl.containsAny(db.primaryImpl);
      boolean ssr = secondaryImpl.containsAny(db.secondaryImpl);

      if (ppr != ssr) {
        Assertions._assert(ppr == ssr, "containsAny " + this + " " + set + " " + ppr + " " + ssr);
      }
      return ppr;
    } else {
      Assertions.UNREACHABLE();
      return false;
    }
  }

  /**
   * This implementation must not despoil the original value of "this"
   * 
   * @return a new IntSet which is the intersection of this and that
   */
  public IntSet intersection(IntSet that) {
    if (that instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) that;
      IntSet ppr = primaryImpl.intersection(db.primaryImpl);
      IntSet ssr = secondaryImpl.intersection(db.secondaryImpl);

      Assertions._assert(ppr.sameValue(ssr));

      return ppr;
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#union(com.ibm.wala.util.intset.IntSet)
   */
  public IntSet union(IntSet that) {
    MutableSparseIntSet temp = new MutableSparseIntSet();
    temp.addAll(this);
    temp.addAll(that);

    return temp;
  }

  /**
   * @return true iff <code>this</code> has the same value as
   *         <code>that</code>.
   */
  public boolean sameValue(IntSet that) {
    if (that instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) that;
      boolean ppr = primaryImpl.sameValue(db.primaryImpl);
      boolean ssr = secondaryImpl.sameValue(db.secondaryImpl);

      Assertions._assert(ppr == ssr);

      return ppr;
    } else {
      Assertions.UNREACHABLE();
      return false;
    }
  }

  /**
   * @return true iff <code>this</code> is a subset of <code>that</code>.
   */
  public boolean isSubset(IntSet that) {
    if (that instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) that;
      boolean ppr = primaryImpl.isSubset(db.primaryImpl);
      boolean ssr = secondaryImpl.isSubset(db.secondaryImpl);

      Assertions._assert(ppr == ssr);

      return ppr;
    } else {
      Assertions.UNREACHABLE();
      return false;
    }
  }

  /**
   * Set the value of this to be the same as the value of set
   * 
   * @param set
   */
  public void copySet(IntSet set) {
    if (set instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) set;
      primaryImpl.copySet(db.primaryImpl);
      secondaryImpl.copySet(db.secondaryImpl);

      Assertions._assert(primaryImpl.sameValue(secondaryImpl));
    } else {
      Assertions.UNREACHABLE();
    }
  }

  /**
   * Add all members of set to this.
   * 
   * @param set
   * @return true iff the value of this changes.
   */
  public boolean addAll(IntSet set) {
    if (set instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) set;
      int ps = primaryImpl.size();
      int ss = secondaryImpl.size();
      boolean ppr = primaryImpl.addAll(db.primaryImpl);
      boolean ssr = secondaryImpl.addAll(db.secondaryImpl);

      if (ppr != ssr) {
        System.err.println("ppr was " + ppr + " (should be " + (ps != primaryImpl.size()) + ") but ssr was " + ssr + " (should be "
            + (ss != secondaryImpl.size()) + ")");
        System.err.println("adding " + set + " to " + this + " failed");
        Assertions.UNREACHABLE();
      }

      return ppr;
    } else {
      Assertions.UNREACHABLE();
      return false;
    }
  }

  /**
   * Interset this with another set.
   * 
   * @param set
   */
  public void intersectWith(IntSet set) {
    if (set instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) set;
      primaryImpl.intersectWith(db.primaryImpl);
      secondaryImpl.intersectWith(db.secondaryImpl);

      if (!primaryImpl.sameValue(secondaryImpl))
        Assertions._assert(false, this + " (" + primaryImpl.size() + ", " + secondaryImpl.size()
            + ") inconsistent after intersecting with " + set);
    } else {
      Assertions.UNREACHABLE();
    }
  }

  /**
   * @param other
   * @param filter
   */
  public boolean addAllInIntersection(IntSet other, IntSet filter) {
    if (other instanceof DebuggingMutableIntSet && filter instanceof DebuggingMutableIntSet) {
      DebuggingMutableIntSet db = (DebuggingMutableIntSet) other;
      DebuggingMutableIntSet df = (DebuggingMutableIntSet) filter;
      boolean pr = primaryImpl.addAllInIntersection(db.primaryImpl, df.primaryImpl);
      boolean sr = secondaryImpl.addAllInIntersection(db.secondaryImpl, df.secondaryImpl);

      Assertions._assert(pr == sr);

      return pr;
    } else {
      Assertions.UNREACHABLE();
      return false;
    }
  }

  /*
   * @see com.ibm.wala.util.intset.IntSet#intIterator()
   */
  public IntIterator intIterator() {
    MutableSparseIntSet bits = MutableSparseIntSet.makeEmpty();
    for (IntIterator pi = primaryImpl.intIterator(); pi.hasNext();) {
      int x = pi.next();
      Assertions._assert(!bits.contains(x));
      bits.add(x);
    }
    for (IntIterator si = secondaryImpl.intIterator(); si.hasNext();) {
      int x = si.next();
      Assertions._assert(bits.contains(x));
      bits.remove(x);
    }
    Assertions._assert(bits.isEmpty());

    return primaryImpl.intIterator();
  }

  /**
   * Invoke an action on each element of the Set
   * 
   * @param action
   */
  public void foreach(IntSetAction action) {
    final Set<Integer> bits = HashSetFactory.make();
    primaryImpl.foreach(new IntSetAction() {
      public void act(int x) {
        Assertions._assert(!bits.contains(new Integer(x)));
        bits.add(new Integer(x));
      }
    });
    secondaryImpl.foreach(new IntSetAction() {
      public void act(int x) {
        Assertions._assert(bits.contains(new Integer(x)));
        bits.remove(new Integer(x));
      }
    });
    Assertions._assert(bits.isEmpty());

    primaryImpl.foreach(action);
  }

  /**
   * Invoke an action on each element of the Set, excluding elements of Set X
   * 
   * @param action
   */
  public void foreachExcluding(IntSet X, IntSetAction action) {
    final Set<Integer> bits = HashSetFactory.make();
    primaryImpl.foreachExcluding(X, new IntSetAction() {
      public void act(int x) {
        Assertions._assert(!bits.contains(new Integer(x)));
        bits.add(new Integer(x));
      }
    });
    secondaryImpl.foreachExcluding(X, new IntSetAction() {
      public void act(int x) {
        Assertions._assert(bits.contains(new Integer(x)));
        bits.remove(new Integer(x));
      }
    });
    Assertions._assert(bits.isEmpty());

    primaryImpl.foreachExcluding(X, action);
  }

  @Override
  public String toString() {
    return "[[P " + primaryImpl.toString() + ", S " + secondaryImpl.toString() + " ]]";
  }

}
