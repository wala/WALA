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
 * A map from an edge in a supergraph to a flow function
 * 
 * @param T type of node in the supergraph
 * 
 * @author sfink
 */
public interface IFlowFunctionMap<T> {

  /**
   * @param src
   * @param dest
   * @return the flow function for a "normal" edge in the supergraph from
   *         src->dest
   */
  public IUnaryFlowFunction getNormalFlowFunction(T src, T dest);

  /**
   * @param src
   * @param dest
   * @return the flow function for a "call" edge in the supergraph from
   *         src->dest
   */
  public IUnaryFlowFunction getCallFlowFunction(T src, T dest);

  /**
   * @param call
   *          supergraph node of the call instruction for this return edge.
   * @param src
   * @param dest
   * @return the flow function for a "return" edge in the supergraph from
   *         src->dest
   */
  public IFlowFunction getReturnFlowFunction(T call, T src, T dest);


  /**
   * @param src
   * @param dest
   * @return the flow function for a "call-to-return" edge in the supergraph
   *         from src->dest
   */
  public IUnaryFlowFunction getCallToReturnFlowFunction(T src, T dest);

  /**
   * @param src
   * @param dest
   * @return the flow function for a "call-to-return" edge in the supergraph
   *         from src->dest, when the supergraph does not contain any callees of
   *         src. This happens via, e.g., slicing.
   */
  public IUnaryFlowFunction getCallNoneToReturnFlowFunction(T src, T dest);
}
