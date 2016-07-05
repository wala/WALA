/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.exceptionanalysis;

import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.intset.BitVector;

public class InitializedBitVectorSolver extends BitVectorSolver<CGNode> {
  public InitializedBitVectorSolver(IKilldallFramework<CGNode, BitVectorVariable> problem) {
    super(problem);
  }

  @Override
  protected BitVectorVariable makeNodeVariable(CGNode n, boolean IN) {
    return newBV();
  }

  @Override
  protected BitVectorVariable makeEdgeVariable(CGNode src, CGNode dst) {
    return newBV();
  }

  private BitVectorVariable newBV() {
    /*
     * If we do not initialize BitVectorVariable, with a BitVector, it contains
     * null, which may crash in combination with {@link BitVectorMinusVector}
     * used in {@link ExceptionTransferFunction}
     */
    BitVectorVariable result = new BitVectorVariable();
    result.addAll(new BitVector());
    return result;
  }
}
