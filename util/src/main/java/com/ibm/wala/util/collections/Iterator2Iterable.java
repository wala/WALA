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

import java.util.Iterator;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/** Converts an {@link Iterator} to an {@link Iterable}. */
public class Iterator2Iterable<T> implements Iterable<T> {

  private final @Nullable Iterator<T> iter;

  public static <T> Iterator2Iterable<T> make(@Nullable Iterator<T> iter) {
    return new Iterator2Iterable<>(iter);
  }

  public Iterator2Iterable(@Nullable Iterator<T> iter) {
    this.iter = iter;
  }

  @Override
  public @Nullable Iterator<T> iterator() {
    return iter;
  }

  public static <T> Iterable<T> of(Supplier<Iterator<T>> iteratorFactory) {
    return iteratorFactory::get;
  }
}
