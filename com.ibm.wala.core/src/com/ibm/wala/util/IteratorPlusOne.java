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
package com.ibm.wala.util;

import java.util.Iterator;

/**
 *
 * A utility to efficiently compose an iterator and a singleton
 * 
 * @author sfink
 */
public class IteratorPlusOne<T> implements Iterator<T> {
  private final Iterator<? extends T> it;

  // the following field will be nulled out after visiting xtra.
  private T xtra;

  public IteratorPlusOne(Iterator<? extends T> it, T xtra) {
    this.it = it;
    this.xtra = xtra;
  }
    
  public boolean hasNext() {
    return it.hasNext() || (xtra != null);
  }

  public T next() {
    if (it.hasNext()) {
      return it.next();
    } else {
      T result = xtra;
      xtra = null;
      return result;
    }
  }
  
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
