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

/**
 *
 * A singleton instance of an empty iterator; this is better than
 * Collections.EMPTY_SET.iterator(), which allocates an iterator object;
 * 
 * @author sfink
 */
public final class EmptyIterator<T> implements Iterator<T> {

  private static final EmptyIterator EMPTY = new EmptyIterator();

  @SuppressWarnings("unchecked")
  public static <T> EmptyIterator<T> instance() {
    return EMPTY;
  }

  /**
   * prevent instantiation
   */
  private EmptyIterator() {
  }

  public boolean hasNext() {
    return false;
  }

  public T next() {
    return null;
  }

  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

}
