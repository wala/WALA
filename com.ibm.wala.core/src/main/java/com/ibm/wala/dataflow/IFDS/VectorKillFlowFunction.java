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
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

/** A function which kills a vector of incoming dataflow facts */
public class VectorKillFlowFunction implements IReversibleFlowFunction {

  private final IntSet kill;

  /** @param kill the intset of facts which are killed by this flow function */
  private VectorKillFlowFunction(IntSet kill) {
    if (kill == null) {
      throw new IllegalArgumentException("null kill");
    }
    this.kill = kill;
  }

  @Override
  public IntSet getTargets(int i) {
    return kill.contains(i) ? null : SparseIntSet.singleton(i);
  }

  @Override
  public IntSet getSources(int i) {
    return kill.contains(i) ? null : SparseIntSet.singleton(i);
  }

  /**
   * @param kill the intset of facts which should be killed by a function
   * @return an instance of a flow function which kills these facts
   */
  public static VectorKillFlowFunction make(IntSet kill) {
    return new VectorKillFlowFunction(kill);
  }

  @Override
  public String toString() {
    return "VectorKill: " + kill;
  }
}
