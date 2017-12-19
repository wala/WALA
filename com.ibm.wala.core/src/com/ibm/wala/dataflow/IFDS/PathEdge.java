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
package com.ibm.wala.dataflow.IFDS;


/**
 * an individual edge &lt;entry, d1&gt; -&gt; &lt;target, d2&gt;
 * 
 * @param <T> node type in the supergraph
 */
public final class PathEdge<T> {

  final T entry;
  final int d1;
  final T target;
  final int d2;

  public static <T> PathEdge<T> createPathEdge(T s_p, int d1, T n, int d2) {
    if (s_p == null) {
      throw new IllegalArgumentException("null s_p");
    }
    if (n == null) {
      throw new IllegalArgumentException("null n");
    }
    return new PathEdge<>(s_p, d1, n, d2);
  }
  
  private PathEdge(T s_p, int d1, T n, int d2) {
    this.entry = s_p;
    this.d1 = d1;
    this.target = n;
    this.d2 = d2;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("<");
    result.append(entry.toString());
    result.append(",");
    result.append(d1);
    result.append("> -> <");
    result.append(target.toString());
    result.append(",");
    result.append(d2);
    result.append(">");
    return result.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + d1;
    result = prime * result + d2;
    result = prime * result + ((target == null) ? 0 : target.hashCode());
    result = prime * result + ((entry == null) ? 0 : entry.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final PathEdge other = (PathEdge) obj;
    if (d1 != other.d1)
      return false;
    if (d2 != other.d2)
      return false;
    if (target == null) {
      if (other.target != null)
        return false;
    } else if (!target.equals(other.target))
      return false;
    if (entry == null) {
      if (other.entry != null)
        return false;
    } else if (!entry.equals(other.entry))
      return false;
    return true;
  }

  public int getD1() {
    return d1;
  }

  public int getD2() {
    return d2;
  }

  public T getEntry() {
    return entry;
  }

  public T getTarget() {
    return target;
  }
}
