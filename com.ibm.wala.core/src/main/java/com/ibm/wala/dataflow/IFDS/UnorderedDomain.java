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
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.MutableMapping;

/** A {@link TabulationDomain} with no build-in partial order defining priority. */
public class UnorderedDomain<T, U> extends MutableMapping<T> implements TabulationDomain<T, U> {

  private static final long serialVersionUID = -988075488958891635L;

  @Override
  public boolean hasPriorityOver(PathEdge<U> p1, PathEdge<U> p2) {
    return false;
  }
}
