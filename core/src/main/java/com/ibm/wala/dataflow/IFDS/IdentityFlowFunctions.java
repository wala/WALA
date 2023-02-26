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
package com.ibm.wala.dataflow.IFDS;

/** A silly debugging aid that always returns the identity flow function */
public class IdentityFlowFunctions<T> implements IFlowFunctionMap<T> {

  private static final IdentityFlowFunctions<?> SINGLETON = new IdentityFlowFunctions<>();

  @SuppressWarnings("unchecked")
  public static <T> IdentityFlowFunctions<T> singleton() {
    return (IdentityFlowFunctions<T>) SINGLETON;
  }

  private IdentityFlowFunctions() {}

  @Override
  public IUnaryFlowFunction getNormalFlowFunction(T src, T dest) {
    return IdentityFlowFunction.identity();
  }

  @Override
  public IFlowFunction getReturnFlowFunction(T call, T src, T dest) {
    return IdentityFlowFunction.identity();
  }

  @Override
  public IUnaryFlowFunction getCallToReturnFlowFunction(T src, T dest) {
    return IdentityFlowFunction.identity();
  }

  @Override
  public IUnaryFlowFunction getCallNoneToReturnFlowFunction(T src, T dest) {
    return IdentityFlowFunction.identity();
  }

  @Override
  public IUnaryFlowFunction getCallFlowFunction(T src, T dest, T ret) {
    return IdentityFlowFunction.identity();
  }
}
