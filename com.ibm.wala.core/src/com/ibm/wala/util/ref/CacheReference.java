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
package com.ibm.wala.util.ref;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import com.ibm.wala.util.debug.Assertions;

/**
 * A factory for References ... useful for debugging.
 */
public final class CacheReference {

  private final static byte SOFT = 0;

  private final static byte WEAK = 1;

  private final static byte HARD = 2;

  // should be SOFT except during debugging.
  private final static byte choice = SOFT;

  public final static Object make(final Object referent) {

    switch (choice) {
    case SOFT:
      return new SoftReference<>(referent);
    case WEAK:
      return new WeakReference<>(referent);
    case HARD:
      return referent;
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public final static Object get(final Object reference) throws IllegalArgumentException {

    if (reference == null) {
      return null;
    }
    switch (choice) {
    case SOFT:
      if (!(reference instanceof java.lang.ref.SoftReference)) {
        throw new IllegalArgumentException("not ( reference instanceof java.lang.ref.SoftReference ) ");
      }
      return ((SoftReference) reference).get();
    case WEAK:
      return ((WeakReference) reference).get();
    case HARD:
      return reference;
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

}
