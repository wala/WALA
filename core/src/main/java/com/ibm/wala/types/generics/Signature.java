/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.types.generics;

import java.util.Objects;

/**
 * Base class for wrappers around Strings that represent Signature annotations according to Java 5.0
 * JVM spec enhancements.
 *
 * @author sjfink
 */
public abstract class Signature {

  private final String s;

  public Signature(final String s) {
    if (s == null) {
      throw new IllegalArgumentException("s cannot be null");
    }
    this.s = s;
  }

  @Override
  public String toString() {
    return s;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((s == null) ? 0 : s.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    if (!Objects.equals(s, ((Signature) obj).s)) return false;
    return true;
  }

  protected String rawString() {
    return s;
  }
}
