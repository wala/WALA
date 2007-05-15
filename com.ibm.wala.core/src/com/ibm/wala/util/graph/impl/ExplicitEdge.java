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
package com.ibm.wala.util.graph.impl;

/**
 *
 * A utility class for use by clients.  Use with care ... this will be slow and a space hog.
 * 
 * @author sfink
 */
public class ExplicitEdge {
  
  private Object src;
  private Object dest;
  
  public ExplicitEdge(Object src, Object dest) {
    this.src = src;
    this.dest = dest;
  }
  public String toString() {
    return "<" + src + "->" + dest + ">";
  }
  public int hashCode() {
    return src.hashCode() * 947 + dest.hashCode();
  }
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass().equals(obj.getClass())) {
      ExplicitEdge other = (ExplicitEdge)obj;
      return src.equals(other.src) && dest.equals(other.dest);
    } else {
      return false;
    }
  }
}
