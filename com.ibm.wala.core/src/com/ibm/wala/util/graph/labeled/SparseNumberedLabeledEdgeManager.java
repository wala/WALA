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
 * Refinement Analysis Tools is Copyright ©2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient’s reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents’ employees.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.demandpa.genericutil.ArraySetMultiMap;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager;

/**
 * @author manu
 * @author Stephen Fink
 */
public class SparseNumberedLabeledEdgeManager<T, U> implements LabeledEdgeManager<T, U> {

  /**
   * the label to be attached to an edge when no label is specified
   */
  private final U defaultLabel;

  private final NumberedNodeManager<T> nodeManager;

  /**
   * maps each edge label to its own {@link SparseNumberedEdgeManager}
   */
  private final Map<U, SparseNumberedEdgeManager<T>> edgeLabelToManager = HashMapFactory.make();

  private final ArraySetMultiMap<T, U> nodeToPredLabels = new ArraySetMultiMap<T, U>();

  private final ArraySetMultiMap<T, U> nodeToSuccLabels = new ArraySetMultiMap<T, U>();

  private SparseNumberedEdgeManager<T> getManagerForLabel(U label) {
    SparseNumberedEdgeManager<T> ret = edgeLabelToManager.get(label);
    if (ret == null) {
      ret = new SparseNumberedEdgeManager<T>(nodeManager);
      edgeLabelToManager.put(label, ret);
    }
    return ret;
  }

  /*
   * @see util.LabelledEdgeManager#addEdge(java.lang.Object, java.lang.Object,
   *      java.lang.Object)
   */
  public void addEdge(T src, T dst, U label) {
    nodeToSuccLabels.put(src, label);
    nodeToPredLabels.put(dst, label);
    getManagerForLabel(label).addEdge(src, dst);
  }

  /*
   * @see util.LabelledEdgeManager#getPredNodeCount(java.lang.Object,
   *      java.lang.Object)
   */
  public int getPredNodeCount(T N, U label) {
    return getManagerForLabel(label).getPredNodeCount(N);
  }

  /*
   * @see util.LabelledEdgeManager#getPredNodes(java.lang.Object,
   *      java.lang.Object)
   */
  public Iterator<? extends T> getPredNodes(T N, U label) {
    return getManagerForLabel(label).getPredNodes(N);
  }

  /*
   * @see util.LabelledEdgeManager#getSuccNodeCount(java.lang.Object,
   *      java.lang.Object)
   */
  public int getSuccNodeCount(T N, U label) {
    return getManagerForLabel(label).getSuccNodeCount(N);
  }

  /*
   * @see util.LabelledEdgeManager#getSuccNodes(java.lang.Object,
   *      java.lang.Object)
   */
  public Iterator<? extends T> getSuccNodes(T N, U label) {
    return getManagerForLabel(label).getSuccNodes(N);
  }

  /*
   * @see util.LabelledEdgeManager#hasEdge(java.lang.Object, java.lang.Object,
   *      java.lang.Object)
   */
  public boolean hasEdge(T src, T dst, U label) {
    return getManagerForLabel(label).hasEdge(src, dst);
  }

  /*
   * (non-Javadoc)
   * 
   * @see util.LabelledEdgeManager#removeAllIncidentEdges(java.lang.Object)
   */
  public void removeAllIncidentEdges(T node) {
    removeIncomingEdges(node);
    removeOutgoingEdges(node);
  }

  /*
   * @see util.LabelledEdgeManager#removeEdge(java.lang.Object,
   *      java.lang.Object, java.lang.Object)
   */
  public void removeEdge(T src, T dst, U label) throws IllegalArgumentException {
    getManagerForLabel(label).removeEdge(src, dst);
  }

  /*
   * @see util.LabelledEdgeManager#removeIncomingEdges(java.lang.Object)
   */
  public void removeIncomingEdges(T node) throws IllegalArgumentException {
    for (Iterator<U> inLabelIter = nodeToPredLabels.get(node).iterator(); inLabelIter.hasNext();) {
      U label = inLabelIter.next();
      getManagerForLabel(label).removeIncomingEdges(node);
    }

  }

  /*
   * @see util.LabelledEdgeManager#removeOutgoingEdges(java.lang.Object)
   */
  public void removeOutgoingEdges(T node) throws IllegalArgumentException {
    for (Iterator<U> outLabelIter = nodeToSuccLabels.get(node).iterator(); outLabelIter.hasNext();) {
      U label = outLabelIter.next();
      getManagerForLabel(label).removeOutgoingEdges(node);
    }

  }

  public SparseNumberedLabeledEdgeManager(final NumberedNodeManager<T> nodeManager, U defaultLabel) {
    super();
    this.defaultLabel = defaultLabel;
    this.nodeManager = nodeManager;
    if (defaultLabel == null) {
      throw new IllegalArgumentException("null default label");
    }
  }

  public Iterator<? extends U> getPredLabels(T N) {
    return nodeToPredLabels.get(N).iterator();
  }

  public Iterator<? extends U> getSuccLabels(T N) {
    return nodeToSuccLabels.get(N).iterator();
  }

  public Set<? extends U> getEdgeLabels(T src, T dst) {
    Set<U> labels = HashSetFactory.make();

    for (U key : edgeLabelToManager.keySet()) {
      if (edgeLabelToManager.get(key).hasEdge(src, dst)) {
        labels.add(key);
      }
    }

    return labels;
  }

  public void addEdge(T src, T dst) {
    addEdge(src, dst, defaultLabel);
  }

  public int getPredNodeCount(T N) {
    int count = 0;
    for (U label : nodeToPredLabels.get(N)) {
      count += getPredNodeCount(N, label);
    }
    return count;
  }

  public Iterator<? extends T> getPredNodes(T N) {
    Collection<T> preds = HashSetFactory.make();
    for (U label : nodeToPredLabels.get(N)) {
      preds.addAll(Iterator2Collection.toCollection(getPredNodes(N, label)));
    }
    return preds.iterator();
  }

  public int getSuccNodeCount(T N) {
    int count = 0;
    for (U label : nodeToSuccLabels.get(N)) {
      count += getSuccNodeCount(N, label);
    }
    return count;
  }

  public Iterator<? extends T> getSuccNodes(T N) {
    Collection<T> succs = HashSetFactory.make();
    for (U label : nodeToSuccLabels.get(N)) {
      succs.addAll(Iterator2Collection.toCollection(getSuccNodes(N, label)));
    }
    return succs.iterator();
  }

  public boolean hasEdge(T src, T dst) {
    return hasEdge(src, dst, defaultLabel);
  }

  public void removeEdge(T src, T dst) throws UnsupportedOperationException {
    removeEdge(src, dst, defaultLabel);
  }

  @Override
  public U getDefaultLabel() {
    return defaultLabel;
  }

}
