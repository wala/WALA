/*******************************************************************************
 * Copyright (c) 2007 Juergen Graf
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Juergen Graf
 *******************************************************************************/
package com.ibm.wala.util.graph.labeled;

import java.util.Iterator;

/**
 * This is an Adapter for a LabeledEdgeManager to be used as a normal 
 * EdgeManager in a sane way. Every operation where no specific edge label is
 * provided, the default label is used.
 * 
 * @author grafj
 * 
 */
public abstract class AbstractLabeledEdgeManager<T, U> implements LabeledEdgeManager<T, U> {

  private U defaultLabel;
  
  public AbstractLabeledEdgeManager(U defaultLabel) {
    this.defaultLabel = defaultLabel;
  }

  public void setDefaultLabel(U label) {
    this.defaultLabel = label;
  }

  public void addEdge(T src, T dst) {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    addEdge(src, dst, defaultLabel);
  }

  public int getPredNodeCount(T N) {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    return getPredNodeCount(N, defaultLabel);
  }

  public Iterator<? extends T> getPredNodes(T N) {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    return getPredNodes(N, defaultLabel);
  }

  public int getSuccNodeCount(T N) {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    return getSuccNodeCount(N, defaultLabel);
  }

  public Iterator<? extends T> getSuccNodes(T N) {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    return getSuccNodes(N, defaultLabel);
  }

  public boolean hasEdge(T src, T dst) {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    return hasEdge(src, dst, defaultLabel);
  }

  public void removeEdge(T src, T dst) throws UnsupportedOperationException {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    removeEdge(src, dst, defaultLabel);
  }

}
