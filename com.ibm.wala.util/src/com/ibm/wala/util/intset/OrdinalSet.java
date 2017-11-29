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

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;

/**
 * A Set backed by a set of integers.
 */
public class OrdinalSet<T> implements Iterable<T> {

  private final IntSet S;

  private final OrdinalSetMapping<T> mapping;

  @SuppressWarnings("rawtypes")
  private final static OrdinalSet EMPTY = new OrdinalSet();

  public static <T> OrdinalSet<T> empty() {
    return EMPTY;
  }

  private OrdinalSet() {
    S = null;
    mapping = null;
  }

  public OrdinalSet(IntSet S, OrdinalSetMapping<T> mapping) {
    this.S = S;
    this.mapping = mapping;
  }

  public boolean containsAny(OrdinalSet<T> that) {
    if (that == null) {
      throw new IllegalArgumentException("null that");
    }
    if (S == null || that.S == null) {
      return false;
    }
    return S.containsAny(that.S);
  }

  public int size() {
    return (S == null) ? 0 : S.size();
  }

  @Override
  public Iterator<T> iterator() {
    if (S == null) {
      return EmptyIterator.instance();
    } else {

      return new Iterator<T>() {
        IntIterator it = S.intIterator();

        @Override
        public boolean hasNext() {
          return it.hasNext();
        }

        @Override
        public T next() {
          return mapping.getMappedObject(it.next());
        }

        @Override
        public void remove() {
          Assertions.UNREACHABLE();
        }
      };
    }
  }

  /**
   * @return a new OrdinalSet instances
   * @throws IllegalArgumentException if A is null
   */
  public static <T> OrdinalSet<T> intersect(OrdinalSet<T> A, OrdinalSet<T> B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (A.size() != 0 && B.size() != 0) {
      assert A.mapping.equals(B.mapping);
    }
    if (A.S == null || B.S == null) {
      return new OrdinalSet<>(null, A.mapping);
    }
    IntSet isect = A.S.intersection(B.S);
    return new OrdinalSet<>(isect, A.mapping);
  }

  /**
   * @return true if the contents of two sets are equal
   */
  public static <T> boolean equals(OrdinalSet<T> a, OrdinalSet<T> b) {
    if ((a == null && b == null) || a == b || (a.mapping == b.mapping && a.S == b.S)) {
      return true;
    }

    assert a != null && b != null;
    if (a.size() == b.size()) {
      if (a.mapping == b.mapping || (a.mapping != null && b.mapping != null && a.mapping.equals(b.mapping))) {
        return a.S == b.S || (a.S != null && b.S != null && a.S.sameValue(b.S));
      }
    }
    
    return false;
  }
  
  /**
   * Creates the union of two ordinal sets.
   * 
   * @param A ordinal set a
   * @param B ordinal set b
   * @return union of a and b
   * @throws IllegalArgumentException iff A or B is null
   */
  public static <T> OrdinalSet<T> unify(OrdinalSet<T> A, OrdinalSet<T> B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    if (A.size() != 0 && B.size() != 0) {
      assert A.mapping.equals(B.mapping);
    }

    if (A.S == null) {
      return (B.S == null) ? OrdinalSet.<T> empty() : new OrdinalSet<>(B.S, B.mapping);
    } else if (B.S == null) {
      return new OrdinalSet<>(A.S, A.mapping);
    }

    IntSet union = A.S.union(B.S);
    return new OrdinalSet<>(union, A.mapping);
  }

  @Override
  public String toString() {
    return Iterator2Collection.toSet(iterator()).toString();
  }

  /**
   */
  public SparseIntSet makeSparseCopy() {
    return (S == null) ? new SparseIntSet() : new SparseIntSet(S);
  }

  /**
   * Dangerous. Added for performance reasons. Use this only if you really know what you are doing.
   */
  public IntSet getBackingSet() {
    return S;
  }

  /**
   * @return true iff this set contains object
   */
  public boolean contains(T object) {
    if (this == EMPTY || S == null || object == null) {
      return false;
    }
    int index = mapping.getMappedIndex(object);
    return (index == -1) ? false : S.contains(index);
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * @throws NullPointerException if instances is null
   */
  public static <T> Collection<T> toCollection(OrdinalSet<T> instances) {
    return Iterator2Collection.toSet(instances.iterator());
  }

  /**
   * Precondition: the ordinal set mapping has an index for every element of c Convert a "normal" collection to an OrdinalSet, based
   * on the given mapping.
   * 
   * @throws IllegalArgumentException if c is null
   */
  public static <T> OrdinalSet<T> toOrdinalSet(Collection<T> c, OrdinalSetMapping<T> m) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    MutableSparseIntSet s = MutableSparseIntSet.makeEmpty();
    for (T t : c) {
      int index = m.getMappedIndex(t);
      assert index >= 0;
      s.add(index);
    }
    return new OrdinalSet<>(s, m);
  }

  public OrdinalSetMapping<T> getMapping() {
    return mapping;
  }

}
