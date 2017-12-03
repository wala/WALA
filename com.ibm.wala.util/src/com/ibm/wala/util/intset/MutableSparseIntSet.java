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

/**
 * A sparse ordered, mutable duplicate-free, fully-encapsulated set of integers.
 * Instances are not canonical, except for EMPTY.
 * 
 * This implementation will be inefficient if these sets get large.
 * 
 * TODO: even for small sets, we probably want to work on this to reduce the
 * allocation activity.
 */
public class MutableSparseIntSet extends SparseIntSet implements MutableIntSet {

	private static final long serialVersionUID = 1479453398189400698L;

	/**
	 * If forced to grow the backing array .. then by how much
	 */
	private final static float EXPANSION_FACTOR = 1.5f;

	/**
	 * Default initial size for a backing array with one element
	 */
	private final static int INITIAL_NONEMPTY_SIZE = 2;

	/**
	 * a debug flag, used to trap when a set gets large
	 */
	private final static boolean DEBUG_LARGE = false;

	private final static int TRAP_SIZE = 1000;

	protected MutableSparseIntSet(IntSet set) {
		super();
		copySet(set);
	}

	protected MutableSparseIntSet(int[] backingStore) {
		super(backingStore);
	}

	/**
	 * Create an empty set with a non-zero capacity
	 */
	private MutableSparseIntSet(int initialCapacity)
			throws IllegalArgumentException {
		super(new int[initialCapacity]);
		size = 0;
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException(
					"initialCapacity must be positive");
		}
	}

	protected MutableSparseIntSet() {
		super();
	}

	/*
	 * @see com.ibm.wala.util.intset.MutableIntSet#clear()
	 */
	@Override
  public void clear() {
		size = 0;
	}

	/**
   */
	@Override
  public boolean remove(int value) {
		if (elements != null) {
			int remove;
			for (remove = 0; remove < size; remove++) {
				if (elements[remove] >= value) {
					break;
				}
			}
			if (remove == size) {
				return false;
			}
			if (elements[remove] == value) {
				if (size == 1) {
					elements = null;
					size = 0;
				} else {
					if (remove < size) {
						System.arraycopy(elements, remove + 1, elements,
								remove, size - remove - 1);
					}
					size--;
				}
				return true;
			}
		}
		return false;
	}

	/**
   */
	public int getInitialNonEmptySize() {
		return INITIAL_NONEMPTY_SIZE;
	}

	public float getExpansionFactor() {
		return EXPANSION_FACTOR;
	}

	/**
	 * @param value
	 * @return true iff this value changes
	 */
	@Override
  @SuppressWarnings("unused")
	public boolean add(int value) {
		if (elements == null) {
			elements = new int[getInitialNonEmptySize()];
			size = 1;
			elements[0] = value;
			return true;
		} else {
			int insert;
			if (size == 0 || value > max()) {
				insert = size;
			} else if (value == max()) {
				return false;
			} else {
				for (insert = 0; insert < size; insert++) {
					if (elements[insert] >= value) {
						break;
					}
				}
			}
			if (insert < size && elements[insert] == value) {
				return false;
			}
			if (size < elements.length - 1) {
				// there's space in the backing elements array. Use it.
				if (size != insert) {
					System.arraycopy(elements, insert, elements, insert + 1,
							size - insert);
				}
				size++;
				elements[insert] = value;
				if (DEBUG_LARGE && size() > TRAP_SIZE) {
					Assertions.UNREACHABLE();
				}
				return true;
			} else {
				// no space left. expand the backing array.
				float newExtent = elements.length * getExpansionFactor() + 1;
				int[] tmp = new int[(int) newExtent];
				System.arraycopy(elements, 0, tmp, 0, insert);
				if (size != insert) {
					System.arraycopy(elements, insert, tmp, insert + 1, size
							- insert);
				}
				tmp[insert] = value;
				size++;
				elements = tmp;
				if (DEBUG_LARGE && size() > TRAP_SIZE) {
					Assertions.UNREACHABLE();
				}
				return true;
			}
		}
	}

	/**
	 * @throws IllegalArgumentException
	 *             if that == null
	 */
	@Override
  @SuppressWarnings("unused")
	public void copySet(IntSet that) throws IllegalArgumentException {
		if (that == null) {
			throw new IllegalArgumentException("that == null");
		}
		if (that instanceof SparseIntSet) {
			SparseIntSet set = (SparseIntSet) that;
			if (set.elements != null) {
				// SJF: clone is performance problem. don't use it.
				// elements = set.elements.clone();
				elements = new int[set.elements.length];
				for (int i = 0; i < set.size; i++) {
					elements[i] = set.elements[i];
				}
				size = set.size;
			} else {
				elements = null;
				size = 0;
			}
		} else {
			elements = new int[that.size()];
			size = that.size();
			that.foreach(new IntSetAction() {
				private int index = 0;

				@Override
        public void act(int i) {
					elements[index++] = i;
				}
			});
		}
		if (DEBUG_LARGE && size() > TRAP_SIZE) {
			Assertions.UNREACHABLE();
		}
	}

	@Override
  public void intersectWith(IntSet set) {
		if (set == null) {
			throw new IllegalArgumentException("null set");
		}
		if (set instanceof SparseIntSet) {
			intersectWith((SparseIntSet) set);
		} else {
			int j = 0;
			for (int i = 0; i < size; i++)
				if (set.contains(elements[i])) {
					elements[j++] = elements[i];
				}
			size = j;
		}
	}

	public void intersectWith(SparseIntSet set) {
		if (set == null) {
			throw new IllegalArgumentException("null set");
		}
		SparseIntSet that = set;
		if (this.isEmpty()) {
			return;
		} else if (that.isEmpty()) {
			elements = null;
			size = 0;
			return;
		} else if (this.equals(that)) {
			return;
		}

		// some simple optimizations
		if (size == 1) {
			if (that.contains(elements[0])) {
				return;
			} else {
				elements = null;
				size = 0;
				return;
			}
		}
		if (that.size == 1) {
			if (contains(that.elements[0])) {
				if (size > getInitialNonEmptySize()) {
					elements = new int[getInitialNonEmptySize()];
				}
				size = 1;
				elements[0] = that.elements[0];
				return;
			} else {
				elements = null;
				size = 0;
				return;
			}
		}

		int[] ar = this.elements;
		int ai = 0;
		int al = size;
		int[] br = that.elements;
		int bi = 0;
		int bl = that.size;
		int[] cr = null; // allocate on demand
		int ci = 0;

		while (ai < al && bi < bl) {
			int cmp = (ar[ai] - br[bi]);

			// (accept element only on a match)
			if (cmp > 0) { // a greater
				bi++;
			} else if (cmp < 0) { // b greater
				ai++;
			} else {
				if (cr == null) {
					cr = new int[al]; // allocate enough (i.e. too much)
				}
				cr[ci++] = ar[ai];
				ai++;
				bi++;
			}
		}

		// now compact cr to 'just enough'
		size = ci;
		elements = cr;
		return;
	}

	/**
	 * Add all elements from another int set.
	 * 
	 * @return true iff this set changes
	 * @throws IllegalArgumentException
	 *             if set == null
	 */
	@Override
  @SuppressWarnings("unused")
	public boolean addAll(IntSet set) throws IllegalArgumentException {
		if (set == null) {
			throw new IllegalArgumentException("set == null");
		}
		if (set instanceof SparseIntSet) {
			return addAll((SparseIntSet) set);
		} else {
			int oldSize = size;
			set.foreach(i -> {
      	if (!contains(i))
      		add(i);
      });

			if (DEBUG_LARGE && size() > TRAP_SIZE) {
				Assertions.UNREACHABLE();
			}
			return size != oldSize;

		}
	}

	/**
	 * Add all elements from another int set.
	 * 
	 * @param that
	 * @return true iff this set changes
	 */
	public boolean addAll(SparseIntSet that) {
		if (that == null) {
			throw new IllegalArgumentException("null that");
		}
		if (this.isEmpty()) {
			copySet(that);
			return !that.isEmpty();
		} else if (that.isEmpty()) {
			return false;
		} else if (this.equals(that)) {
			return false;
		}

		// common-case optimization
		if (that.size == 1) {
			boolean result = add(that.elements[0]);
			return result;
		}

		int[] br = that.elements;
		int bl = that.size();

		return addAll(br, bl);
	}

	@SuppressWarnings("unused")
	private boolean addAll(int[] that, int thatSize) {
		int[] ar = this.elements;
		int ai = 0;
		final int al = size();
		int bi = 0;

		// invariant: assume cr has same value as ar until cr is allocated.
		// we allocate cr lazily when we discover cr != ar.
		int[] cr = null;
		int ci = 0;

		while (ai < al && bi < thatSize) {
			int cmp = (ar[ai] - that[bi]);

			// (always accept element)
			if (cmp > 0) { // a greater
				if (cr == null) {
					cr = new int[al + thatSize];
					System.arraycopy(ar, 0, cr, 0, ci);
				}
				cr[ci++] = that[bi++];
			} else if (cmp < 0) { // b greater
				if (cr != null) {
					cr[ci] = ar[ai];
				}
				ci++;
				ai++;
			} else {
				if (cr != null) {
					cr[ci] = ar[ai]; // (same: use a)
				}
				ci++;
				ai++;
				bi++;
			}
		}

		// append tail if any (at most one of a or b has tail)
		if (ai < al) {
			int tail = al - ai;
			if (cr != null) {
				System.arraycopy(ar, ai, cr, ci, tail);
			}
			ci += tail;
		} else if (bi < thatSize) {
			int tail = thatSize - bi;
			if (cr == null) {
				cr = new int[al + thatSize];
				System.arraycopy(ar, 0, cr, 0, ci);
			}
			System.arraycopy(that, bi, cr, ci, tail);
			ci += tail;
		}

		assert ci > 0;

		elements = (cr == null) ? ar : cr;
		size = ci;
		if (DEBUG_LARGE && size() > TRAP_SIZE) {
			Assertions.UNREACHABLE();
		}
		return (al != size);
	}

	public void removeAll(BitVectorIntSet v) {
		if (v == null) {
			throw new IllegalArgumentException("null v");
		}
		int ai = 0;
		for (int i = 0; i < size; i++) {
			if (!v.contains(elements[i])) {
				elements[ai++] = elements[i];
			}
		}
		size = ai;
	}

	public <T extends BitVectorBase<T>> void removeAll(T v) {
		if (v == null) {
			throw new IllegalArgumentException("null v");
		}
		int ai = 0;
		for (int i = 0; i < size; i++) {
			if (!v.get(elements[i])) {
				elements[ai++] = elements[i];
			}
		}
		size = ai;
	}

	/**
	 * TODO optimize
	 * 
	 * @param set
	 * @throws IllegalArgumentException
	 *             if set is null
	 */
	public void removeAll(MutableSparseIntSet set) {
		if (set == null) {
			throw new IllegalArgumentException("set is null");
		}
		for (IntIterator it = set.intIterator(); it.hasNext();) {
			remove(it.next());
		}
	}

	/*
	 * @see
	 * com.ibm.wala.util.intset.MutableIntSet#addAllInIntersection(com.ibm.wala
	 * .util.intset.IntSet, com.ibm.wala.util.intset.IntSet)
	 */
	@Override
  public boolean addAllInIntersection(IntSet other, IntSet filter) {
		if (other == null) {
			throw new IllegalArgumentException("other is null");
		}
		if (filter == null) {
			throw new IllegalArgumentException("invalid filter");
		}
		// a hack. TODO: better algorithm
		if (other.size() < 5) {
			boolean result = false;
			for (IntIterator it = other.intIterator(); it.hasNext();) {
				int i = it.next();
				if (filter.contains(i)) {
					result |= add(i);
				}
			}
			return result;
		} else if (filter.size() < 5) {
			boolean result = false;
			for (IntIterator it = filter.intIterator(); it.hasNext();) {
				int i = it.next();
				if (other.contains(i)) {
					result |= add(i);
				}
			}
			return result;
		} else {
			BitVectorIntSet o = new BitVectorIntSet(other);
			o.intersectWith(filter);
			return addAll(o);
		}
	}

	public static MutableSparseIntSet diff(MutableSparseIntSet A,
			MutableSparseIntSet B) {
		return new MutableSparseIntSet(diffInternal(A, B));
	}

	public static MutableSparseIntSet make(IntSet set) {
		return new MutableSparseIntSet(set);
	}

	public static MutableSparseIntSet makeEmpty() {
		return new MutableSparseIntSet();
	}

	public static MutableSparseIntSet createMutableSparseIntSet(
			int initialCapacity) throws IllegalArgumentException {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("illegal initialCapacity: "
					+ initialCapacity);
		}
		return new MutableSparseIntSet(initialCapacity);
	}

}
