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
package com.ibm.wala.util.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * A debugging aid.  When HashSetFactory.DEBUG is set, this class creates ParanoidHashSets.  Otherwise,
 * it returns {@link LinkedHashSet}s
 */
public class HashSetFactory {

  /**
   * If true, this factory returns Paranoid versions of collections
   */
  final public static boolean DEBUG = false;

  /**
   * @return A {@link ParanoidHashSet} if DEBUG = true, a java.util.HashSet otherwise
   */
  public static <T> HashSet<T> make(int size) {
    if (DEBUG) {
      return new ParanoidHashSet<>(size);
    } else {
      return new LinkedHashSet<>(size);
    }
  }

  /**
   * @return A ParanoidHashSet if DEBUG = true, a java.util.HashSet otherwise
   */
  public static <T> HashSet<T> make() {
    if (DEBUG) {
      return new ParanoidHashSet<>();
    } else {
      return new LinkedHashSet<>();
    }
  }

  /**
   * @return A ParanoidHashSet if DEBUG = true, a java.util.HashSet otherwise
   */
  public static <T> HashSet<T> make(Collection<T> s) {
    if (s == null) {
      throw new IllegalArgumentException("null s");
    }
    if (DEBUG) {
      return new ParanoidHashSet<>(s);
    } else {
      return new LinkedHashSet<>(s);
    }
  }
}
