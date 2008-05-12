package com.ibm.wala.dataflow.graph;

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
import com.ibm.wala.fixpoint.BooleanVariable;

/**
 * @author sfink
 */
public class BooleanSolver<T> extends DataflowSolver<T, BooleanVariable> {

  public BooleanSolver(IKilldallFramework<T, BooleanVariable> problem) {
    super(problem);
  }

  @Override
  protected BooleanVariable makeNodeVariable(Object n, boolean IN) {
    return new BooleanVariable();

  }

  @Override
  protected BooleanVariable makeEdgeVariable(Object src, Object dst) {
    return new BooleanVariable();

  }

}
