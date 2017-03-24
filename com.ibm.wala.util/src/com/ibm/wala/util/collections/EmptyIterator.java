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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A singleton instance of an empty iterator; this is better than
 * Collections.EMPTY_SET.iterator(), which allocates an iterator object;
 */
public final class EmptyIterator<T> implements Iterator<T> {

  @SuppressWarnings("rawtypes")
  private static final EmptyIterator EMPTY = new EmptyIterator();

  public static <T> EmptyIterator<T> instance() {
    return EMPTY;
  }

  /**
   * prevent instantiation
   */
  private EmptyIterator() {
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public T next() {
    throw new NoSuchElementException();
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

}
