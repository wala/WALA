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

import java.util.Set;

import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.BitVector;

public class ExceptionFlowSolver extends BitVectorSolver<CGNode> {

  private IntraproceduralResult intraResult;
  private ExceptionToBitvectorTransformer transformer;

  public ExceptionFlowSolver(IKilldallFramework<CGNode, BitVectorVariable> problem, IntraproceduralResult intraResult,
      ExceptionToBitvectorTransformer transformer) {
    super(problem);
    this.intraResult = intraResult;
    this.transformer = transformer;
  }

  @Override
  protected BitVectorVariable makeNodeVariable(CGNode n, boolean IN) {
    BitVectorVariable result = new BitVectorVariable();
    Set<TypeReference> exceptions = intraResult.getIntraproceduralExceptions(n);
    BitVector bitVector = transformer.computeBitVector(exceptions);
    result.addAll(bitVector);
    return result;
  }

  @Override
  protected BitVectorVariable makeEdgeVariable(CGNode src, CGNode dst) {

    if (src.equals(dst)) {
      /*
       * Set edge variables of self loops to the value of the node, otherwise
       * leafs will lose their information.
       */
      return makeNodeVariable(src, true);
    } else {
      /*
       * If we do not initialize BitVectorVariable, with a BitVector, it
       * contains null, which may crash in combination with {@link
       * BitVectorMinusVector} used in {@link ExceptionTransferFunction}
       */
      BitVectorVariable result = new BitVectorVariable();
      result.addAll(new BitVector());
      return result;
    }
  }
}
