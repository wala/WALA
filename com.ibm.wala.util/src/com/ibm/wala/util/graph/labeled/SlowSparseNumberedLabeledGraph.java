/*******************************************************************************
 * Copyright (c) 2007 Manu Sridharan and Juergen Graf
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manu Sridharan
 *     Juergen Graf
 *******************************************************************************/
/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.util.graph.labeled;

import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.SlowNumberedNodeManager;

/**
 * A labeled graph implementation suitable for sparse graphs.
 */
public class SlowSparseNumberedLabeledGraph<T, U> extends AbstractNumberedLabeledGraph<T, U> {

  /**
   * @return a graph with the same nodes and edges as g
   */
  public static <T,U> SlowSparseNumberedLabeledGraph<T,U> duplicate(LabeledGraph<T,U> g) {
    SlowSparseNumberedLabeledGraph<T,U> result = new SlowSparseNumberedLabeledGraph<>(g.getDefaultLabel());
    copyInto(g, result);
    return result;
  }

  public static <T,U> void copyInto(LabeledGraph<T,U> g, LabeledGraph<T,U> into) {
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    for (T name : g) {
      into.addNode(name);
    }
    for (T n : g) {
      for (T s : Iterator2Iterable.make(g.getSuccNodes(n))) {
        for(U l : g.getEdgeLabels(n, s)) {
          into.addEdge(n, s, l);
        }
      }
    }
  }

  private final SlowNumberedNodeManager<T> nodeManager;

  private final SparseNumberedLabeledEdgeManager<T, U> edgeManager;

  public SlowSparseNumberedLabeledGraph(U defaultLabel) {
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
    nodeManager = new SlowNumberedNodeManager<>();
    edgeManager = new SparseNumberedLabeledEdgeManager<>(nodeManager, defaultLabel);
  }

  @Override
  protected NumberedLabeledEdgeManager<T, U> getEdgeManager() {
    return edgeManager;
  }

  @Override
  protected NumberedNodeManager<T> getNodeManager() {
    return nodeManager;
  }

}
