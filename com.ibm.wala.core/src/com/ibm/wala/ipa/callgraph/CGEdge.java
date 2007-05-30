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

package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * An explicit representation of an edge in a call graph.
 * 
 * NB: We do NOT enforce that any particular call graph implementation actually
 * contains CGEdge objects. In fact, the CGEdge is currently not mentioned
 * anywhere else in the callgraph API. This class is just provided since some
 * clients might find it a useful utility.
 * 
 * This abstraction does not include a call site reference, so this edge might
 * actually represent several distinct call sites.
 * 
 * @author sfink
 */
public class CGEdge {
  private final CGNode src;

  private final CGNode dest;

  public CGEdge(CGNode src, CGNode dest) {
    this.src = src;
    this.dest = dest;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(this.getClass().equals(obj.getClass()));
    }
    CGEdge other = (CGEdge) obj;
    return src.equals(other.src) && dest.equals(other.dest);
  }

  @Override
  public int hashCode() {
    return 4027 * src.hashCode() + dest.hashCode();
  }

  @Override
  public String toString() {
    return "[" + src.toString() + "," + dest.toString() + "]";
  }

  /**
   * @return the node at the tail of this edge
   */
  public CGNode getDest() {
    return dest;
  }

  /**
   * @return the node at the head of this edge.
   */
  public CGNode getSrc() {
    return src;
  }
}
