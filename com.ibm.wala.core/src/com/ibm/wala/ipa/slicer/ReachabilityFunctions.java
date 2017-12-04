/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IFlowFunctionMap;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.dataflow.IFDS.VectorGenFlowFunction;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * Trivial flow functions to represent simple reachability. All functions simply
 * return "0"
 */
public class ReachabilityFunctions<T> implements IFlowFunctionMap<T> {


  public final static VectorGenFlowFunction FLOW_REACHES = VectorGenFlowFunction.make(SparseIntSet.singleton(0));

  public final static IUnaryFlowFunction KILL_FLOW = new IUnaryFlowFunction() {
    @Override
    public SparseIntSet getTargets(int d1) {
      // kill even the reachability predicate 0.
      return new SparseIntSet();
    }
    @Override
    public String toString() {
      return "killFlow";
    }
  };

  public static <T> ReachabilityFunctions<T> createReachabilityFunctions() {
    return new ReachabilityFunctions<>();
  }

  private ReachabilityFunctions() {
  }

  /* 
   * @see com.ibm.wala.dataflow.IFDS.IFlowFunctionMap#getCallNoneToReturnFlowFunction(java.lang.Object, java.lang.Object)
   */
  @Override
  public IUnaryFlowFunction getCallNoneToReturnFlowFunction(T src, T dest) {
    return FLOW_REACHES;
  }

  @Override
  public IUnaryFlowFunction getCallToReturnFlowFunction(T src, T dest) {
    // force flow into callee and back.
    return KILL_FLOW;
  }

  @Override
  public IUnaryFlowFunction getNormalFlowFunction(T src, T dest) {
    return FLOW_REACHES;
  }

  @Override
  public IFlowFunction getReturnFlowFunction(T call, T src, T dest) {
    return FLOW_REACHES;
  }
  
  @SuppressWarnings("unused")
  public IFlowFunction getReturnFlowFunction(T src, T dest) {
    return FLOW_REACHES;
  }

  @Override
  public IUnaryFlowFunction getCallFlowFunction(T src, T dest, T ret) {
    return FLOW_REACHES;
  }

}
