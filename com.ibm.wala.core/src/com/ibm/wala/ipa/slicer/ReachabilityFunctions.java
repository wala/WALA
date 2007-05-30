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
 * 
 * @author sjfink
 * 
 */
class ReachabilityFunctions implements IFlowFunctionMap {

  private static final ReachabilityFunctions instance = new ReachabilityFunctions();

  public static ReachabilityFunctions singleton() {
    return instance;
  }

  private final VectorGenFlowFunction f = VectorGenFlowFunction.make(SparseIntSet.singleton(0));

  protected final static IUnaryFlowFunction killReachability = new IUnaryFlowFunction() {
    public SparseIntSet getTargets(int d1) {
      // kill even the reachability predicate 0.
      return new SparseIntSet();
    }
    @Override
    public String toString() {
      return "killFlow";
    }
  };

  private ReachabilityFunctions() {
  }

  public IUnaryFlowFunction getCallFlowFunction(Object src, Object dest) {
    return f;
  }

  /* 
   * @see com.ibm.wala.dataflow.IFDS.IFlowFunctionMap#getCallNoneToReturnFlowFunction(java.lang.Object, java.lang.Object)
   */
  public IUnaryFlowFunction getCallNoneToReturnFlowFunction(Object src, Object dest) {
    return f;
  }

  public IUnaryFlowFunction getCallToReturnFlowFunction(Object src, Object dest) {
    // force flow into callee and back.
    return killReachability;
  }

  public IUnaryFlowFunction getNormalFlowFunction(Object src, Object dest) {
    return f;
  }

  public IFlowFunction getReturnFlowFunction(Object call, Object src, Object dest) {
    return f;
  }

}