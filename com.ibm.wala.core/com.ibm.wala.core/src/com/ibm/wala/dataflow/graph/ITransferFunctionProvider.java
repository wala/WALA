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

import com.ibm.wala.fixedpoint.impl.UnaryOperator;

/**
 * 
 * The DataflowSolver builds system over graphs, with dataflow transfer
 * functions on the nodes, the edges or both. In any case, it takes an
 * ITransferFunctionProvider to tell it what functions to use.
 * 
 * @author sfink
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 */
public interface ITransferFunctionProvider<T> {

  /**
   * @param node
   * @return the transfer function from IN_node -> OUT_node
   */
  public UnaryOperator getNodeTransferFunction(T node);

  /**
   * @return true if this provider provides node transfer functions
   */
  public boolean hasNodeTransferFunctions();

  /**
   * @param src
   * @param dst
   * @return the transfer function from OUT_src -> EDGE_<src,dst>
   */
  public UnaryOperator getEdgeTransferFunction(T src, T dst);

  /**
   * @return true if this provider provides edge transfer functions
   */
  public boolean hasEdgeTransferFunctions();

  /**
   * TODO: perhaps this should go with a Lattice object instead. TODO: provide
   * an API to allow composition of the meet operator with the flow operator for
   * a given block, as an optimization?
   */
  public AbstractMeetOperator getMeetOperator();
}
