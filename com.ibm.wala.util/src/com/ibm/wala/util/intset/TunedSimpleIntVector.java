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

/** a simple implementation of int vector that can be tuned to control space usage */
public class TunedSimpleIntVector extends SimpleIntVector {

  private static final long serialVersionUID = -1380867351543398351L;

  private final int initialSize;

  private final float expansion;

  TunedSimpleIntVector(int defaultValue, int initialSize, float expansion) {
    super(defaultValue, initialSize);
    this.initialSize = initialSize;
    this.expansion = expansion;
  }

  @Override
  float getGrowthFactor() {
    return expansion;
  }

  @Override
  int getInitialSize() {
    return initialSize;
  }
}
