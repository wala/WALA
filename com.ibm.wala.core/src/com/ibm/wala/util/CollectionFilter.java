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

import java.util.Collection;

import com.ibm.wala.annotations.NonNull;
import com.ibm.wala.util.collections.Filter;

/**
 * 
 * A filter defined by set membership
 * 
 * @author sfink
 */
public class CollectionFilter<T> implements Filter<T> {

  @NonNull
  private final Collection<? extends T> S;

  public CollectionFilter(Collection<? extends T> S) {
    this.S = S;
  }

  /*
   * @see com.ibm.wala.util.Filter#accepts(java.lang.Object)
   */
  public boolean accepts(T o) {
    return S.contains(o);
  }

}
