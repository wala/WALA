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
package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.BitVectorVariable;

/** A {@link DataflowSolver} specialized for {@link BitVectorVariable}s */
public class BitVectorSolver<T> extends DataflowSolver<T, BitVectorVariable> {

  public BitVectorSolver(IKilldallFramework<T, BitVectorVariable> problem) {
    super(problem);
  }

  @Override
  protected BitVectorVariable makeNodeVariable(T n, boolean IN) {
    return new BitVectorVariable();
  }

  @Override
  protected BitVectorVariable makeEdgeVariable(T src, T dst) {
    return new BitVectorVariable();
  }

  @Override
  protected BitVectorVariable[] makeStmtRHS(int size) {
    return new BitVectorVariable[size];
  }
}
