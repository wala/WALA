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

package com.ibm.wala.ipa.callgraph;

import java.util.Objects;

/** A placeholder for strong typing. */
public interface ContextItem {

  class Value<T> implements ContextItem {
    private final T v;

    public Value(T v) {
      this.v = v;
    }

    public T getValue() {
      return v;
    }

    public static <T> Value<T> make(T v) {
      return new Value<>(v);
    }

    @Override
    public int hashCode() {
      return 31 + ((v == null) ? 0 : v.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      if (!Objects.equals(v, ((Value<?>) obj).v)) return false;
      return true;
    }
  }
}
