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
package com.ibm.wala.util.collections;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.math.Logs;

/**
 * An {@link IVector} implementation which delegates to pages of int vectors.
 */
public class TwoLevelVector<T> implements IVector<T>, Serializable {

  private static final int PAGE_SIZE = 4096;

  private static final int LOG_PAGE_SIZE = Logs.log2(PAGE_SIZE);

  /**
   * Array of IVector: data.get(i) holds data[i*PAGE_SIZE] ...
   * data[(i+1)*PAGESIZE - 1]
   */
  final private Vector<SparseVector<T>> data = new Vector<>(0);

  private int maxPage = -1;

  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
  @Override
  public T get(int x) {
    if (x < 0) {
      throw new IllegalArgumentException("invalid x: " + x);
    }
    int page = getPageNumber(x);
    if (page >= data.size()) {
      return null;
    }
    IVector<T> v = data.get(page);
    if (v == null) {
      return null;
    }
    int localX = x - getFirstIndexOnPage(page);
    return v.get(localX);
  }

  private static int getFirstIndexOnPage(int page) {
    return page << LOG_PAGE_SIZE;
  }

  private static int getPageNumber(int x) {
    return x >> LOG_PAGE_SIZE;
  }

  /*
   * TODO: this can be optimized
   * 
   * @see com.ibm.wala.util.intset.IntVector#set(int, int)
   */
  @Override
  public void set(int x, T value) {
    if (x < 0) {
      throw new IllegalArgumentException("illegal x: " + x);
    }
    int page = getPageNumber(x);
    IVector<T> v = findOrCreatePage(page);
    int localX = toLocalIndex(x, page);
    v.set(localX, value);
  }

  private static int toLocalIndex(int x, int page) {
    return x - getFirstIndexOnPage(page);
  }

  private IVector<T> findOrCreatePage(int page) {
    if (page >= data.size()) {
      SparseVector<T> v = new SparseVector<>();
      data.setSize(page + 1);
      data.add(page, v);
      maxPage = Math.max(page, maxPage);
      return v;
    } else {
      SparseVector<T> v = data.get(page);
      if (v == null) {
        v = new SparseVector<>();
        data.set(page, v);
        maxPage = Math.max(page, maxPage);
      }
      return v;
    }
  }

  /*
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  @Override
  public void performVerboseAction() {
    // do nothing;
  }

  /*
   * @see com.ibm.wala.util.intset.IVector#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      final Iterator<SparseVector<T>> outer = data.iterator();

      Iterator<T> inner;
      {
        while (outer.hasNext()) {
          IVector<T> v = outer.next();
          if (v != null) {
            Iterator<T> it = v.iterator();
            if (it.hasNext()) {
              inner = it;
              break;
            }
          }
        }
      }

      @Override
      public boolean hasNext() {
        return inner != null;
      }

      @Override
      public T next() {
        T result = inner.next();
        if (!inner.hasNext()) {
          inner = null;
          while (outer.hasNext()) {
            IVector<T> v = outer.next();
            if (v != null) {
              Iterator<T> it = v.iterator();
              if (it.hasNext()) {
                inner = it;
                break;
              }
            }
          }
        }
        return result;
      }

      @Override
      public void remove() {
        // TODO Auto-generated method stub
        Assertions.UNREACHABLE();
      }
    };
  }

  @Override
  public int getMaxIndex() {
    if (maxPage == -1) {
      return -1;
    } else {
      IVector<T> v = data.get(maxPage);
      int localMax = v.getMaxIndex();
      return maxPage * PAGE_SIZE + localMax;
    }

  }

}
