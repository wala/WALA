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

import com.ibm.wala.util.collections.Pair;

/**
 * An explicit representation of an edge in a call graph.
 * 
 * NB: We do NOT enforce that any particular call graph implementation actually
 * contains CGEdge objects. In fact, the CGEdge is currently not mentioned
 * anywhere else in the callgraph API. This class is just provided since some
 * clients might find it a useful utility.
 * 
 * This abstraction does not include a call site reference, so this edge might
 * actually represent several distinct call sites.
 */
public class CGEdge extends Pair<CGNode,CGNode>{

  public CGEdge(CGNode src, CGNode dest) {
    super(src,dest);
    if (src == null) {
      throw new IllegalArgumentException("null src");
    }
    if (dest == null) {
      throw new IllegalArgumentException("null dest");
    }
  }


  /**
   * @return the node at the tail of this edge
   */
  public CGNode getDest() {
    return snd;
  }

  /**
   * @return the node at the head of this edge.
   */
  public CGNode getSrc() {
    return fst;
  }
}
