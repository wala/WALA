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
package com.ibm.wala.demandpa.flowgraph;

import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.util.graph.labeled.LabeledGraph;

public interface IFlowLabelGraph extends LabeledGraph<Object, IFlowLabel> {

  /**
   * Apply a visitor to the successors of some node.
   * @param node
   * @param v
   */
  public abstract void visitSuccs(Object node, IFlowLabelVisitor v);

  /**
   * Apply a visitor to the predecessors of some node.
   * @param node
   * @param v
   */
  public abstract void visitPreds(Object node, IFlowLabelVisitor v);

}