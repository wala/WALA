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
package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.graph.Graph;

/**
 * a basic implementation of the dataflow framework
 */
public class BasicFramework<T, V extends IVariable<V>> implements IKilldallFramework<T, V> {

  private final Graph<T> flowGraph;
  private final ITransferFunctionProvider<T, V> transferFunctionProvider;

  public BasicFramework(Graph<T> flowGraph, ITransferFunctionProvider<T, V> transferFunctionProvider) {
    this.flowGraph = flowGraph;
    this.transferFunctionProvider = transferFunctionProvider;
  } 

  /* 
   * @see com.ibm.wala.dataflow.graph.IKilldallFramework#getFlowGraph()
   */
  @Override
  public Graph<T> getFlowGraph() {
    return flowGraph;
  }

  /*
   * @see com.ibm.wala.dataflow.graph.IKilldallFramework#getTransferFunctionMap()
   */
  @Override
  public ITransferFunctionProvider<T, V> getTransferFunctionProvider() {
    return transferFunctionProvider;
  }
}
