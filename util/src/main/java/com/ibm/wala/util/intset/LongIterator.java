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
package com.ibm.wala.util.intset;

/** a more efficient iterator for sets of longs */
public interface LongIterator {

  /**
   * @return true iff this iterator has a next element
   */
  boolean hasNext();

  /**
   * @return next integer in the iteration
   */
  long next();
}
