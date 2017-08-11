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
 * A dataflow framework in the style of Kildall, POPL 73
 * This represents a dataflow problem induced over a graph.
 * 
 * @param <T> type of nodes in the graph
 */
public interface IKilldallFramework<T,V extends IVariable<V>> {

  /**
   * @return the flow graph which induces this dataflow problem
   */
  public Graph<T> getFlowGraph();
  
  /**
   * @return an object which provides the flow function for each node in the graph
   */
  public ITransferFunctionProvider<T,V> getTransferFunctionProvider();
}
