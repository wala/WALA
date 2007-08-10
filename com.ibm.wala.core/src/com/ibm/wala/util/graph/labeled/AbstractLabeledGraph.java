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

import java.util.Iterator;

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

public abstract class AbstractLabeledGraph<T, U> implements LabeledGraph<T, U> {

  /**
   * @return the object which manages nodes in the graph
   */
  protected abstract NodeManager<T> getNodeManager();

  /**
   * @return the object which manages edges in the graph
   */
  protected abstract LabeledEdgeManager<T, U> getEdgeManager();

  public void removeNodeAndEdges(T N) {
    getEdgeManager().removeAllIncidentEdges(N);
    getNodeManager().removeNode(N);
  }

  public void addNode(T n) {
    getNodeManager().addNode(n);
  }

  public boolean containsNode(T N) {
    return getNodeManager().containsNode(N);
  }

  public int getNumberOfNodes() {
    return getNodeManager().getNumberOfNodes();
  }

  public Iterator<T> iterator() {
    return getNodeManager().iterator();
  }

  public void removeNode(T n) {
    getNodeManager().removeNode(n);
  }

  public void addEdge(T src, T dst, U label) {
    getEdgeManager().addEdge(src, dst, label);
  }

  public Iterator<? extends U> getPredLabels(T N) {
    return getEdgeManager().getPredLabels(N);
  }

  public int getPredNodeCount(T N, U label) {
    return getEdgeManager().getPredNodeCount(N, label);
  }

  public Iterator<? extends T> getPredNodes(T N, U label) {
    return getEdgeManager().getPredNodes(N, label);
  }

  public Iterator<? extends U> getSuccLabels(T N) {
    return getEdgeManager().getSuccLabels(N);
  }

  public int getSuccNodeCount(T N, U label) {
    return getEdgeManager().getSuccNodeCount(N, label);
  }

  public Iterator<? extends T> getSuccNodes(T N, U label) {
    return getEdgeManager().getSuccNodes(N, label);
  }

  public boolean hasEdge(T src, T dst, U label) {
    return getEdgeManager().hasEdge(src, dst, label);
  }

  public void removeAllIncidentEdges(T node) {
    getEdgeManager().removeAllIncidentEdges(node);
  }

  public void removeEdge(T src, T dst, U label) {
    getEdgeManager().removeEdge(src, dst, label);
  }

  public void removeIncomingEdges(T node) {
    getEdgeManager().removeIncomingEdges(node);
  }

  public void removeOutgoingEdges(T node) {
    getEdgeManager().removeOutgoingEdges(node);
  }

  public Graph<T> convertToGraph() {
    Graph<T> ret = new SlowSparseNumberedGraph<T>();
    for (Iterator<? extends T> iter = iterator(); iter.hasNext();) {
      T node = iter.next();
      ret.addNode(node);
      for (Iterator<? extends U> succLabelIter = getSuccLabels(node); succLabelIter.hasNext();) {
        final U label = succLabelIter.next();
        for (Iterator<? extends T> succNodeIter = getSuccNodes(node, label); succNodeIter.hasNext();) {
          T succ = succNodeIter.next();
          ret.addNode(succ);
          ret.addEdge(node, succ);
        }
      }
    }
    return ret;

  }
}
