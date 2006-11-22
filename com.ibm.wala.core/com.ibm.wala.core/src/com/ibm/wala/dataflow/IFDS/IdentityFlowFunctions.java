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
package com.ibm.wala.dataflow.IFDS;

/**
 * 
 * A silly debugging aid that always returns the identity flow function 
 *
 * @author sfink
 */
public class IdentityFlowFunctions implements IFlowFunctionMap {

  private final static IdentityFlowFunctions SINGLETON = new IdentityFlowFunctions();

  public static IdentityFlowFunctions singleton() {
    return SINGLETON;
  }

  /**
   * 
   */
  private IdentityFlowFunctions() {
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.IFlowFunctionMap#getNormalFlowFunction(java.lang.Object, java.lang.Object)
   */
  public IUnaryFlowFunction getNormalFlowFunction(Object src, Object dest) {
    return IdentityFlowFunction.identity();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.IFlowFunctionMap#getCallFlowFunction(java.lang.Object, java.lang.Object)
   */
  public IUnaryFlowFunction getCallFlowFunction(Object src, Object dest) {
    return IdentityFlowFunction.identity();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.IFlowFunctionMap#getReturnFlowFunction(java.lang.Object, java.lang.Object, java.lang.Object)
   */
  public IFlowFunction getReturnFlowFunction(Object call, Object src, Object dest) {
    return IdentityFlowFunction.identity();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.IFlowFunctionMap#getCallToReturnFlowFunction(java.lang.Object, java.lang.Object)
   */
  public IUnaryFlowFunction getCallToReturnFlowFunction(Object src, Object dest) {
    return IdentityFlowFunction.identity();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.IFDS.IFlowFunctionMap#getCallNoneToReturnFlowFunction(java.lang.Object, java.lang.Object)
   */
  public IUnaryFlowFunction getCallNoneToReturnFlowFunction(Object src, Object dest) {
    return IdentityFlowFunction.identity();
  }

}
