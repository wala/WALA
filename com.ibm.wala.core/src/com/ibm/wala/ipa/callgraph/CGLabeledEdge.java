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

import com.ibm.wala.classLoader.CallSiteReference;

/**
 * An explicit representation of an edge in a call graph, which additionally carries a label.
 * 
 * NB: We do NOT enforce that any particular call graph implementation actually contains {@link CGEdge} objects. In fact, the CGEdge is
 * currently not mentioned anywhere else in the callgraph API. This class is just provided since some clients might find it a useful
 * utility.
 * 
 * This abstraction does not include a call site reference, so this edge might actually represent several distinct call sites.
 */
public class CGLabeledEdge extends CGEdge {

  private final CallSiteReference label;

  public CGLabeledEdge(CGNode src, CallSiteReference label, CGNode dst) {
    super(src, dst);
    this.label = label;
  }

  @Override
  public int hashCode() {
    return super.hashCode() ^ label.hashCode();
  }

  @Override
  public boolean equals(Object t) {
    return super.equals(t) && label.equals(((CGLabeledEdge)t).label);
  }

  public CallSiteReference getLabel() {
    return label;
  }

}
