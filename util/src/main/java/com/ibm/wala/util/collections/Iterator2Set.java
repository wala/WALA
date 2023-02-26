/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class Iterator2Set<T> extends Iterator2Collection<T> implements Serializable, Set<T> {

  private static final long serialVersionUID = 3771468677527694694L;

  private final Set<T> delegate;

  protected Iterator2Set(Iterator<? extends T> i, Set<T> delegate) {
    this.delegate = delegate;
    while (i.hasNext()) {
      delegate.add(i.next());
    }
  }

  @Override
  protected Collection<T> getDelegate() {
    return delegate;
  }
}
