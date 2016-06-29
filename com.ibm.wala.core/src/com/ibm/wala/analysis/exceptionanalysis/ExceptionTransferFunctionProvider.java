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
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorMinusVector;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionVector;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.BitVector;

public class ExceptionTransferFunctionProvider implements ITransferFunctionProvider<CGNode, BitVectorVariable> {
  private Exception2BitvectorTransformer transformer;
  private CallGraph cg;
  private CGIntraproceduralExceptionAnalysis intraResult;

  public ExceptionTransferFunctionProvider(CGIntraproceduralExceptionAnalysis intraResult, CallGraph cg,
      Exception2BitvectorTransformer transformer) {
    this.cg = cg;
    this.transformer = transformer;
    this.intraResult = intraResult;
  }

  @Override
  public boolean hasNodeTransferFunctions() {
    return true;
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
    Set<TypeReference> exceptions = intraResult.getAnalysis(node).getExceptions();
    BitVector bitVector = transformer.computeBitVector(exceptions);
    return new BitVectorUnionVector(bitVector);
  }

  @Override
  public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(CGNode dst, CGNode src) {
    /*
     * Note, that dst and src are swapped. For the data-flow-analysis we use
     * called -> caller, but for the call graph we need caller -> called.
     */

    Iterator<CallSiteReference> callsites = cg.getPossibleSites(src, dst);
    BitVector filtered = new BitVector(transformer.getValues().getSize());

    if (callsites.hasNext()) {

      CallSiteReference callsite = callsites.next();

      Set<TypeReference> caught = new LinkedHashSet<>();
      caught.addAll(intraResult.getAnalysis(src).getCaughtExceptions(callsite));
      while (callsites.hasNext()) {
        callsite = callsites.next();
        caught.retainAll(intraResult.getAnalysis(src).getCaughtExceptions(callsite));
      }

      filtered = transformer.computeBitVector(caught);
      return new BitVectorMinusVector(filtered);
    } else {
      // This case should not happen, as we should only get src, dst pairs,
      // which represent an edge in the call graph. For each edge in the call
      // graph should be at least one call site.
      throw new RuntimeException("Internal Error: Got call graph edge without call site.");
    }
  }
}
