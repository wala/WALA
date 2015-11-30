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

import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorMinusVector;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.intset.BitVector;

public class ExceptionTransferFunctionProvider implements ITransferFunctionProvider<CGNode, BitVectorVariable> {
  private ExceptionToBitvectorTransformer transformer;
  private CallGraph cg;
  private IntraproceduralResult intraResult;

  public ExceptionTransferFunctionProvider(IntraproceduralResult intraResult, CallGraph cg,
      ExceptionToBitvectorTransformer transformer) {
    this.cg = cg;
    this.transformer = transformer;
    this.intraResult = intraResult;
  }

  @Override
  public boolean hasNodeTransferFunctions() {
    return false;
  }

  @Override
  public boolean hasEdgeTransferFunctions() {
    return true;
  }

  @Override
  public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
    return BitVectorUnion.instance();
  }

  @Override
  public UnaryOperator<BitVectorVariable> getNodeTransferFunction(CGNode node) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(CGNode src, CGNode dst) {
//    if (src.equals(dst)) {
//      /*
//       * To make use of start values for all nodes, self-loops were introduced
//       * into the graph (Otherwise these values would get eliminated during
//       * short circuit optimization.)
//       * 
//       * If the method does not call itself, we would have no analysis result
//       * for the self-loop edge. We can just return the identity function for
//       * this cases.
//       * 
//       * If the method does call itself, we could produce a more precise
//       * transfer function as the method is catching its own exceptions, maybe.
//       * However, the less precise transfer function we actually use, does not
//       * result in a less precise result: There is an execution, that produces
//       * the exceptions so they need to be included.
//       */
//      return BitVectorIdentity.instance();
//    } else 
    {
      CGNode tmp = src;
      src = dst;
      dst = tmp;
      
      
      Iterator<CallSiteReference> callsites = cg.getPossibleSites(src, dst);
      BitVector filtered = new BitVector(transformer.getValues().getSize());
      
      if (callsites.hasNext()) {      
        CallSiteReference callsite = callsites.next();
        filtered = transformer.computeBitVector(intraResult.getCaughtExceptions(src, callsite));              
        while (callsites.hasNext()) {
          callsite = callsites.next();
          BitVector bv = transformer.computeBitVector(intraResult.getCaughtExceptions(src, callsite));
          filtered.and(bv);        
        }
        
        return new BitVectorMinusVector(filtered);
      } else {
        return BitVectorIdentity.instance();
      }
    }
  }

}
