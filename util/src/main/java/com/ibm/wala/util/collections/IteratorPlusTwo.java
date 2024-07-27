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

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

public class IteratorPlusTwo<T> implements Iterator<T> {
  private final Iterator<T> it;

  // the following fields will be nulled out after visiting xtra.
  private @Nullable T xtra1;
  private @Nullable T xtra2;

  public IteratorPlusTwo(Iterator<T> it, T xtra1, T xtra2) {
    if (it == null) {
      throw new IllegalArgumentException("it null");
    }
    this.it = it;
    this.xtra1 = xtra1;
    this.xtra2 = xtra2;
  }

  @Override
  public boolean hasNext() {
    return it.hasNext() || (xtra1 != null) || (xtra2 != null);
  }

  @Override
  public T next() {
    if (it.hasNext()) {
      return it.next();
    } else if (xtra1 != null) {
      T result = xtra1;
      xtra1 = null;
      return result;
    } else {
      T result = xtra2;
      if (result != null) {
        xtra2 = null;
        return result;
      } else {
        throw new NoSuchElementException();
      }
    }
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
