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
package com.ibm.wala.util.intset;

/**
 * A {@link MutableSparseIntSet} that allows for tuning of its initial size and expansion factor.
 *
 * @see #getInitialNonEmptySize()
 * @see #getExpansionFactor()
 */
public class TunedMutableSparseIntSet extends MutableSparseIntSet {

  private static final long serialVersionUID = -1559172158241923881L;

  private final int initialSize;

  private final float expansion;

  public TunedMutableSparseIntSet(int initialSize, float expansion)
      throws IllegalArgumentException {
    super();
    if (initialSize <= 0) {
      throw new IllegalArgumentException("invalid initial size " + initialSize);
    }
    this.initialSize = initialSize;
    this.expansion = expansion;
  }

  @Override
  public float getExpansionFactor() {
    return expansion;
  }

  @Override
  public int getInitialNonEmptySize() {
    return initialSize;
  }
}
