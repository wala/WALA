/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.collections;

import com.ibm.wala.util.intset.IntIterator;
import java.util.NoSuchElementException;

/**
 * A singleton instance of an empty iterator; this is better than Collections.EMPTY_SET.iterator(),
 * which allocates an iterator object;
 */
public final class EmptyIntIterator implements IntIterator {

  private static final EmptyIntIterator EMPTY = new EmptyIntIterator();

  public static EmptyIntIterator instance() {
    return EMPTY;
  }

  /** prevent instantiation */
  private EmptyIntIterator() {}

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public int next() throws NoSuchElementException {
    throw new NoSuchElementException();
  }
}
