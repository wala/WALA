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
package com.ibm.wala.ipa.callgraph;

/** A context that first checks with A, then defaults to B. */
public class DelegatingContext implements Context {

  private final Context A;

  private final Context B;

  public DelegatingContext(Context A, Context B) {
    this.A = A;
    this.B = B;
    if (A == null) {
      throw new IllegalArgumentException("null A");
    }
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
  }

  @Override
  public ContextItem get(ContextKey name) {
    ContextItem result = A.get(name);
    if (result == null) {
      result = B.get(name);
    }
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + A.hashCode();
    result = prime * result + B.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DelegatingContext other = (DelegatingContext) obj;
    if (!A.equals(other.A)) return false;
    if (!B.equals(other.B)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "DelegatingContext [A=" + A + ", B=" + B + ']';
  }

  @Override
  public boolean isA(Class<? extends Context> type) {
    return A.isA(type) || B.isA(type);
  }
}
