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

import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** An object that implements a bijection between whole numbers and objects. */
public interface OrdinalSetMapping<T extends @Nullable Object> extends Iterable<T> {
  /**
   * @return the object numbered n.
   */
  T getMappedObject(int n) throws NoSuchElementException;

  /**
   * @return the number of a given object, or -1 if the object is not currently in the range.
   */
  int getMappedIndex(Object o);

  /**
   * @return whether the given object is mapped by this mapping
   */
  boolean hasMappedIndex(T o);

  /**
   * @return the maximum integer mapped to an object
   */
  int getMaximumIndex();

  /**
   * @return the current size of the bijection
   */
  int getSize();

  /**
   * Add an Object to the set of mapped objects.
   *
   * @return the integer to which the object is mapped.
   */
  int add(T o);

  /**
   * Stream over mapped objects.
   *
   * @return a stream over the mapped objects
   */
  Stream<T> stream();
}
