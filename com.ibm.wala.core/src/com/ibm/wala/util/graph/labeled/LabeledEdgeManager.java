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

import org.w3c.dom.Node;

/**
 * On object which tracks labelled edges in a graph.
 * 
 * @author Manu Sridharan
 * 
 * @param <T>
 * @param <U>
 */
public interface LabeledEdgeManager<T, U> {

  /**
   * Return an Iterator over the immediate predecessor {@link Node nodes} of
   * this Node in the Graph on edges with some label.
   * 
   * This method never returns <code>null</code>.
   * 
   * @return an Iterator over the immediate predecessor nodes of this Node.
   */
  public Iterator<? extends T> getPredNodes(T N, U label);

  /**
   * 
   * @param N
   * @return the labels on edges whose destination is N
   */
  public Iterator<? extends U> getPredLabels(T N);

  /**
   * Return the number of {@link #getPredNodes immediate predecessor}
   * {@link Node nodes} of this Node in the Graph on edges with some label.
   * 
   * @return the number of immediate predecessor Nodes of this Node in the
   *         Graph.
   */
  public int getPredNodeCount(T N, U label);

  /**
   * Return an Iterator over the immediate successor {@link Node nodes} of this
   * Node in the Graph on edges with some label.
   * <p>
   * This method never returns <code>null</code>.
   * 
   * @return an Iterator over the immediate successor Nodes of this Node.
   */
  public Iterator<? extends T> getSuccNodes(T N, U label);

  /**
   * 
   * @param N
   * @return the labels on edges whose source is N
   */
  public Iterator<? extends U> getSuccLabels(T N);

  /**
   * Return the number of {@link #getSuccNodes immediate successor}
   * {@link Node nodes} of this Node in the Graph
   * 
   * @return the number of immediate successor Nodes of this Node in the Graph.
   */
  public int getSuccNodeCount(T N, U label);

  /**
   * adds an edge with some label
   * 
   * @param src
   * @param dst
   * @param label
   */
  public void addEdge(T src, T dst, U label);

  public void removeEdge(T src, T dst, U label);

  public void removeAllIncidentEdges(T node);

  public void removeIncomingEdges(T node);

  public void removeOutgoingEdges(T node);

  public boolean hasEdge(T src, T dst, U label);

}
