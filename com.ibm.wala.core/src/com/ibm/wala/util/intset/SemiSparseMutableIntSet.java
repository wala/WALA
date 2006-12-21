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

import com.ibm.wala.util.collections.*;

public class SemiSparseMutableIntSet implements MutableIntSet {
  private static final int SPARSE_INSERT_THRESHOLD = 10;

  private MutableSparseIntSet sparsePart = new MutableSparseIntSet();
  private OffsetBitVector densePart = null;
  private int sparseInsertCount = 0;

  private void fixAfterSparseInsert() {
    if (sparseInsertCount++ > SPARSE_INSERT_THRESHOLD) {
      sparseInsertCount = 0;
      IntIterator sparseBits = sparsePart.intIterator();
      int thisBit = sparseBits.next();
      if (densePart == null) {
	int maxOffset = -1;
	int maxCount = -1;
	int maxMax = -1;
	int maxBit = -1;

	int offset = thisBit;
	int bits = 32;
	int count = 1;
	while (sparseBits.hasNext()) {
	  int nextBit = sparseBits.next();

	  int newBits = bits + (nextBit - thisBit);
	  int newCount = count + 1;
	
	  if (newBits > (32*newCount)) {
	    count = newCount;
	    bits = newBits;
	  } else if (bits > 32*count) {
	    if (count > maxCount) {
	      maxOffset = offset;
	      maxMax = thisBit;
	      maxCount = count;
	    }
	    offset = nextBit;
	    count = 1;
	    bits = 32;
	  }
	  thisBit = nextBit;
	}

	if (maxOffset != -1) {
	  densePart = new OffsetBitVector(maxOffset, maxMax);
	  sparseBits = sparsePart.intIterator();
	  int bit;
	  while ((bit = sparseBits.next()) != maxOffset);
	  for(int i = 0; i < maxCount; i++) {
	    densePart.set(sparseBits.next());
	  }
	  for(int bit1 = densePart.nextSetBit(0); 
	      bit1 != -1;
	      bit1 = densePart.nextSetBit(bit1+1))
	  { 
	    sparsePart.remove(bit1);
	  }
	}

      } else {
	int moveCount = 0;
	int newOffset = -1;
	int newLength = -1;
	
	// push stuff just below dense part into it, if it saves space
	if (thisBit < densePart.getOffset()) {
	  newOffset = thisBit;
	  int bits = 32;
	  int count = 1;
	  while (sparseBits.hasNext()) {
	    int nextBit = sparseBits.next();
	    if (nextBit >= densePart.getOffset()) {
	      if (bits > (32*count)) {
		moveCount += count;
		break;
	      } else {
		newOffset = -1;
	      }
	    } else {
	      bits += (nextBit - thisBit);
	      count++;
	
	      if (bits > (32*count)) {
		newOffset = nextBit;
		count = 1;
		bits = 32;
	      }
	    }
	  }
	}

	// push stuff just above dense part into it, if it saves space
	if (thisBit >= densePart.length()) {
	  int count = 1;
	  int bits = (thisBit + 1 - densePart.length());
	  if (32*count > bits) {
	    newLength = thisBit;
	  }
	  while (sparseBits.hasNext()) {
	    thisBit = sparseBits.next();
	    count++;
	    bits = (thisBit + 1 - densePart.length());
	    newLength = (32*count > bits)? thisBit: newLength;
	  }
	  if (newLength > -1) {
	    moveCount += count;
	  }
	}
	
	// actually move bits from sparse to dense
	if (newOffset != -1 || newLength != -1) {
	  int index = 0;
	  int[] bits = new int[ moveCount ];
	  for(sparseBits = sparsePart.intIterator(); sparseBits.hasNext(); ) {
	    int bit = sparseBits.next();
	    if (newOffset!=-1 && bit>=newOffset && bit<=densePart.getOffset()) {
	      bits[index++] = bit;
	    }
	    if (newLength!=-1 && bit>=densePart.length() && bit<=newLength) {
	      bits[index++] = bit;
	    }
	  }
	  
	  for(int i = 0; i < moveCount; i++) {
	    sparsePart.remove(bits[i]);
	    densePart.set(bits[i]);
	  }
	}
      }  
    }
  }

  /**
   * @param i
   * @return true iff this set contains integer i
   */
  public boolean contains(int i) {
    return 
      sparsePart.contains(i) || (densePart != null && densePart.contains(i));
  }

  /**
   * @return true iff this set contains integer i
   */
  public boolean containsAny(IntSet set) {
    if (!sparsePart.isEmpty() && sparsePart.containsAny(set)) {
      return true;
    } else if (densePart != null) {
      int lower = densePart.getOffset();
      for(IntIterator is = set.intIterator(); is.hasNext(); ) {
	int i = is.next();
	if (i < lower) continue;
	if (densePart.get(i)) {
	  return true;
	}
      }
    }

    return false;
  }

  /**
   * This implementation must not despoil the original value of "this"
   * 
   * @return a new IntSet which is the intersection of this and that
   */
  public IntSet intersection(IntSet that) {
    SemiSparseMutableIntSet newThis = new SemiSparseMutableIntSet();
    for(IntIterator bits = intIterator(); bits.hasNext(); ) {
      int bit = bits.next();
      if (that.contains(bit)) {
	newThis.add(bit);
      }
    }
    return newThis;
  }
    
  /**
   * @return true iff this set is empty
   */
  public boolean isEmpty() {
    return sparsePart.isEmpty() && (densePart == null || densePart.isZero());
  }

  /**
   * @return the number of elements in this set
   */
  public int size() {
    return sparsePart.size() + (densePart==null? 0: densePart.populationCount());
  }

  /**
   * @return a perhaps more efficient iterator
   */
  public IntIterator intIterator() {
    class DensePartIterator implements IntIterator {
      private int i = -1;

      public boolean hasNext() {
	return densePart.nextSetBit(i+1) != -1;
      }
 
      public int next() {
	int next = densePart.nextSetBit(i+1);
	i = next+1;
	return next;
      }
    };

    if (sparsePart.isEmpty()) {
      if (densePart == null || densePart.isZero()) {
	return EmptyIntIterator.instance();
      } else {
	return new DensePartIterator();
      }
    } else {
      if (densePart == null || densePart.isZero()) {
	return sparsePart.intIterator();
      } else {
	return 
	  new CompoundIntIterator(
	    sparsePart.intIterator(),
	    new DensePartIterator());
      }
    }
  }

  /**
   * Invoke an action on each element of the Set
   * 
   * @param action
   */
  public void foreach(IntSetAction action) {
    sparsePart.foreach(action);
    if (densePart != null) {
      for(int b = densePart.nextSetBit(0); 
	      b != -1;
	      b = densePart.nextSetBit(b+1))
      { 
	action.act(b);
      }
    }
  }

  /**
   * Invoke an action on each element of the Set, excluding elements of Set X
   * 
   * @param action
   */
  public void foreachExcluding(IntSet X, IntSetAction action) {
    sparsePart.foreachExcluding(X, action);
    if (densePart != null) {
      for(int b = densePart.nextSetBit(0); 
	      b != -1;
	      b = densePart.nextSetBit(b+1))
      { 
	if (! X.contains(b)) {
	  action.act(b);
	}
      }
    }
  }

  /**
   * @return maximum integer in this set.
   */
  public int max() {
    if (densePart == null) {
      return sparsePart.max();
    } else {
      return Math.max(sparsePart.max(), densePart.max());
    }
  }

  /**
   * @return true iff <code>this</code> has the same value as
   *         <code>that</code>.
   */
  public boolean sameValue(IntSet that) {
    if (size() != that.size()) {
      return false;
    }
    if (densePart != null) {
      for(int bit = densePart.nextSetBit(0); 
	  bit != -1;
	  bit = densePart.nextSetBit(bit+1))
      { 
	if (! that.contains(bit)) {
	  return false;
	}
      }
    }
    for(IntIterator bits = sparsePart.intIterator(); bits.hasNext(); ) {
      if (! that.contains(bits.next())) {
	return false;
      }
    }
    return true;
  }

  /**
   * @return true iff <code>this</code> is a subset of <code>that</code>.
   */
  public boolean isSubset(IntSet that) {
    if (size() > that.size()) {
      return false;
    }

    for(IntIterator bits = sparsePart.intIterator(); bits.hasNext(); ) {
      if (! that.contains(bits.next())) {
	return false;
      }
    }

    if (densePart != null) {
      for(int b = densePart.nextSetBit(0); 
	      b != -1;
	      b = densePart.nextSetBit(b+1))
      { 
	if (! that.contains(b)) {
	  return false;
	}
      }
    }

    return true;
  }

  /**
   * Set the value of this to be the same as the value of set
   * 
   * @param set
   */
  public void copySet(IntSet set) {
    if (set instanceof SemiSparseMutableIntSet) {
      SemiSparseMutableIntSet that = (SemiSparseMutableIntSet) set;
      sparsePart = new MutableSparseIntSet(that.sparsePart);
      if (that.densePart == null) {
	densePart = null;
      } else {  
	densePart = new OffsetBitVector(that.densePart);
      }
    } else {
      densePart = null;
      sparsePart = new MutableSparseIntSet();
      for(IntIterator bits = set.intIterator(); bits.hasNext(); ) {
	add( bits.next() );
      }
    }
  }

  /**
   * Add all members of set to this.
   * 
   * @param set
   * @return true iff the value of this changes.
   */
  public boolean addAll(IntSet set) {
    boolean change = false;
    if (set instanceof SemiSparseMutableIntSet) {
      SemiSparseMutableIntSet that = (SemiSparseMutableIntSet) set;
    
      if (densePart == null) {

	// that dense part only
	if (that.densePart != null) {
	  densePart = new OffsetBitVector(that.densePart);
	  for(int b = densePart.nextSetBit(0); 
	      b != -1;
	      b = densePart.nextSetBit(b+1))
	   { 
	     if (sparsePart.contains(b)) {
	       sparsePart.remove(b);
	     } else {
	       change = true;
	     }
	  }
	  for(IntIterator bits = that.sparsePart.intIterator(); 
	      bits.hasNext(); )
	  {
	    change |= sparsePart.add( bits.next() );
	  }

	// no dense part
	} else {
	  for(IntIterator bs = that.sparsePart.intIterator(); bs.hasNext(); ) {
	    change |= add( bs.next() );
	  }
	}

      } else {
	int oldSize = size();

	// both dense parts
	if (that.densePart != null) {
	  densePart.or(that.densePart);

	  for(IntIterator bs = that.sparsePart.intIterator(); bs.hasNext(); ) {
	    add( bs.next() );
	  }
	  for(IntIterator bs = sparsePart.intIterator(); bs.hasNext(); ) {
	    int b = bs.next();
	    if (densePart.get(b)) {
	      sparsePart.remove(b);
	    }
	  }

	  change = (size() != oldSize);

	// this dense part only
	} else {
	  for(IntIterator bs = that.sparsePart.intIterator(); bs.hasNext(); ) {
	    change |= add(bs.next());
	  }
	}
      }
    } else {
      for(IntIterator bs = set.intIterator(); bs.hasNext(); ) {
	change |= add(bs.next());
      }
    }

    return change;
  }

  /**
   * Add an integer value to this set.
   * 
   * @param i integer to add
   * @return true iff the value of this changes.
   */
  public boolean add(int i) {
    if (! contains(i)) {
      if (densePart!=null && densePart.getOffset()<=i && densePart.length()>i){
	densePart.set(i);
      } else {
	sparsePart.add(i);
	fixAfterSparseInsert();
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Remove an integer from this set.
   * 
   * @param i integer to remove
   * @return true iff the value of this changes.
   */
  public boolean remove(int i) {
    if (densePart != null && densePart.get(i)) {
      densePart.clear(i);
      return true;
    } else if (sparsePart.contains(i)) {
      sparsePart.remove(i);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Interset this with another set.
   * 
   * @param set
   */
  public void intersectWith(IntSet set) {
    sparsePart.intersectWith(set);
    if (densePart != null) {
      for(int b = densePart.nextSetBit(0); 
	      b != -1;
	      b = densePart.nextSetBit(b+1))
      { 
	if (! set.contains(b)) {
	  densePart.clear(b);
	}
      }
    }
  }
    
  /**
   * @param other
   * @param filter
   */
  public boolean addAllInIntersection(IntSet other, IntSet filter) {
    boolean change = false;
    for(IntIterator bits = other.intIterator(); bits.hasNext(); ) {
      int bit = bits.next();
      if (filter.contains(bit)) {
	change |= add( bit );
      }
    }

    return change;
  }
}
