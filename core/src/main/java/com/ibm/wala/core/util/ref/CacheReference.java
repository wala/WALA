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
package com.ibm.wala.core.util.ref;

import com.ibm.wala.util.debug.Assertions;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/** A factory for References ... useful for debugging. */
public final class CacheReference {

  private static final byte SOFT = 0;

  private static final byte WEAK = 1;

  private static final byte HARD = 2;

  // should be SOFT except during debugging.
  private static final byte choice = SOFT;

  public static Object make(final Object referent) {

    return switch (choice) {
      case SOFT -> new SoftReference<>(referent);
      case WEAK -> new WeakReference<>(referent);
      case HARD -> referent;
      default -> {
        Assertions.UNREACHABLE();
        yield null;
      }
    };
  }

  public static Object get(final Object reference) throws IllegalArgumentException {

    if (reference == null) {
      return null;
    }
    return switch (choice) {
      case SOFT -> {
        if (!(reference instanceof SoftReference)) {
          throw new IllegalArgumentException(
              "not ( reference instanceof java.lang.ref.SoftReference ) ");
        }
        yield ((SoftReference<?>) reference).get();
      }
      case WEAK -> ((WeakReference<?>) reference).get();
      case HARD -> reference;
      default -> {
        Assertions.UNREACHABLE();
        yield null;
      }
    };
  }
}
