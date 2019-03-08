/*
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

package com.ibm.wala.demandpa.alg;

import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * Abstract super class for demand points-to analysis. Implements basic methods for tracking how
 * much traversal has been done.
 *
 * @author Manu Sridharan
 */
public abstract class AbstractDemandPointsTo implements IDemandPointerAnalysis {

  protected final CallGraph cg;

  protected final HeapModel heapModel;

  protected final MemoryAccessMap mam;

  protected final IClassHierarchy cha;

  protected final AnalysisOptions options;

  protected int numNodesTraversed;

  private int traversalBudget = Integer.MAX_VALUE;

  public int getTraversalBudget() {
    return traversalBudget;
  }

  protected void setTraversalBudget(int traversalBudget) {
    this.traversalBudget = traversalBudget;
  }

  public AbstractDemandPointsTo(
      CallGraph cg,
      HeapModel model,
      MemoryAccessMap mam,
      IClassHierarchy cha,
      AnalysisOptions options) {
    this.cg = cg;
    this.heapModel = model;
    this.mam = mam;
    this.cha = cha;
    this.options = options;
  }

  @Override
  public HeapModel getHeapModel() {
    return heapModel;
  }

  /** */
  protected void incrementNumNodesTraversed() {
    if (numNodesTraversed > traversalBudget) {
      throw new BudgetExceededException();
    }
    numNodesTraversed++;
  }

  protected void setNumNodesTraversed(int traversed) {
    numNodesTraversed = traversed;
  }

  public int getNumNodesTraversed() {
    return numNodesTraversed;
  }

  @Override
  public CallGraph getBaseCallGraph() {
    return cg;
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return cha;
  }
}
