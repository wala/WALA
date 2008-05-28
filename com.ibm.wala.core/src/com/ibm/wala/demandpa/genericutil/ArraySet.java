/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient’s reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents’ employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.demandpa.genericutil;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A set implementation backed by an array. This implementation is
 * space-efficient for small sets, but several operations like
 * {@link #contains(Object)} are linear time.
 * 
 * @author Manu Sridharan
 * 
 * @param <T>
 */

public class ArraySet<T> extends AbstractSet<T> {

  private static final ArraySet EMPTY = new ArraySet<Object>(0, true) {
    @Override
    public boolean add(Object obj_) {
      throw new RuntimeException();
    }
  };

  @SuppressWarnings("all")
  public static final <T> ArraySet<T> empty() {
    return (ArraySet<T>) EMPTY;
  }

  private T[] _elems;

  private int _curIndex = 0;

  private final boolean checkDupes;

  @SuppressWarnings("all")
  public ArraySet(int numElems_, boolean checkDupes) {
    _elems = (T[]) new Object[numElems_];
    this.checkDupes = checkDupes;
  }

  public ArraySet() {
    this(1, true);
  }

  @SuppressWarnings("all")
  public ArraySet(ArraySet<T> other) throws IllegalArgumentException {
    if (other == null) {
      throw new IllegalArgumentException("other == null");
    }
    int size = other._curIndex;
    this._elems = (T[]) new Object[size];
    this.checkDupes = other.checkDupes;
    this._curIndex = size;
    System.arraycopy(other._elems, 0, _elems, 0, size);
  }

  private ArraySet(Collection<T> other) {
    this(other.size(), true);
    addAll(other);
  }

  /*
   * (non-Javadoc)
   * 
   * @see AAA.util.AAASet#add(java.lang.Object)
   */
  @SuppressWarnings("all")
  public boolean add(T obj_) {
    assert obj_ != null;
    if (checkDupes && this.contains(obj_))
      return false;
    if (_curIndex == _elems.length) {
      // lengthen array
      Object[] tmp = _elems;
      _elems = (T[]) new Object[tmp.length * 2];
      System.arraycopy(tmp, 0, _elems, 0, tmp.length);
    }
    _elems[_curIndex] = obj_;
    _curIndex++;
    return true;
  }

  public boolean addAll(ArraySet<T> other) throws IllegalArgumentException {
    if (other == null) {
      throw new IllegalArgumentException("other == null");
    }
    boolean ret = false;
    for (int i = 0; i < other.size(); i++) {
      boolean added = add(other.get(i));
      ret = ret || added;
    }
    return ret;
  }

  /*
   * (non-Javadoc)
   * 
   * @see AAA.util.AAASet#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Object obj_) {
    for (int i = 0; i < _curIndex; i++) {
      if (_elems[i].equals(obj_))
        return true;
    }
    return false;
  }

  public boolean intersects(ArraySet<T> other) throws IllegalArgumentException {
    if (other == null) {
      throw new IllegalArgumentException("other == null");
    }
    for (int i = 0; i < other.size(); i++) {
      if (contains(other.get(i)))
        return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see AAA.util.AAASet#forall(AAA.util.ObjectVisitor)
   */
  public void forall(ObjectVisitor<T> visitor_) {
    for (int i = 0; i < _curIndex; i++) {
      visitor_.visit(_elems[i]);
    }
  }

  @Override
  public int size() {
    return _curIndex;
  }

  public T get(int i) {
    return _elems[i];
  }

  @Override
  public boolean remove(Object obj_) {
    int ind;
    for (ind = 0; ind < _curIndex && !_elems[ind].equals(obj_); ind++) {
    }
    // check if object was never there
    if (ind == _curIndex)
      return false;
    return remove(ind);
  }

  /**
   * @param ind
   * @return <code>true</code>
   */
  public boolean remove(int ind) {
    // hope i got this right...
    System.arraycopy(_elems, ind + 1, _elems, ind, _curIndex - (ind + 1));
    _curIndex--;
    return true;
  }

  @Override
  public void clear() {
    _curIndex = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Set#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return new ArraySetIterator();
  }

  /**
   * @author manu
   */
  public class ArraySetIterator implements Iterator<T> {

    int ind = 0;

    final int setSize = size();

    /**
     * 
     */
    public ArraySetIterator() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove() {
      throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
      return ind < setSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    public T next() {
      if (ind >= setSize) {
        throw new NoSuchElementException();
      }
      return get(ind++);
    }

  }

  public static <T> ArraySet<T> make() {
    return new ArraySet<T>();
  }
  
  public static <T> ArraySet<T> make(Collection<T> other) throws IllegalArgumentException {
    if (other == null) {
      throw new IllegalArgumentException("other == null");
    }
    return new ArraySet<T>(other);
  }

}
