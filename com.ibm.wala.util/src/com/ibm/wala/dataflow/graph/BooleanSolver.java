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

import com.ibm.wala.fixpoint.BooleanVariable;

/** A {@link DataflowSolver} specialized for {@link BooleanVariable}s */
public class BooleanSolver<T> extends DataflowSolver<T, BooleanVariable> {

  public BooleanSolver(IKilldallFramework<T, BooleanVariable> problem) {
    super(problem);
  }

  @Override
  protected BooleanVariable makeNodeVariable(T n, boolean IN) {
    return new BooleanVariable();
  }

  @Override
  protected BooleanVariable makeEdgeVariable(T src, T dst) {
    return new BooleanVariable();
  }

  @Override
  protected BooleanVariable[] makeStmtRHS(int size) {
    return new BooleanVariable[size];
  }
}
