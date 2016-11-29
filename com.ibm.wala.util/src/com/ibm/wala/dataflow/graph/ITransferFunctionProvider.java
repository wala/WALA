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
import com.ibm.wala.fixpoint.UnaryOperator;

/**
 * The {@link DataflowSolver} builds system over graphs, with dataflow transfer
 * functions on the nodes, the edges or both. In any case, it takes an
 * {@link ITransferFunctionProvider} to tell it what functions to use.
 * 
 * @param <T> type of node in the graph 
 * @param <V> type of abstract states computed 
 */
@SuppressWarnings("rawtypes")
public interface ITransferFunctionProvider<T, V extends IVariable> {

  /**
   * @return the transfer function from IN_node -&gt; OUT_node
   */
  public UnaryOperator<V> getNodeTransferFunction(T node);

  /**
   * @return true if this provider provides node transfer functions
   */
  public boolean hasNodeTransferFunctions();

  /**
   * @return the transfer function from OUT_src -&gt; EDGE_&lt;src,dst&gt;
   */
  public UnaryOperator<V> getEdgeTransferFunction(T src, T dst);

  /**
   * @return true if this provider provides edge transfer functions
   */
  public boolean hasEdgeTransferFunctions();

  /**
   * TODO: perhaps this should go with a Lattice object instead. TODO: provide
   * an API to allow composition of the meet operator with the flow operator for
   * a given block, as an optimization?
   */
  public AbstractMeetOperator<V> getMeetOperator();
}
