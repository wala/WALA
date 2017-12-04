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
package com.ibm.wala.demandpa.alg;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.demandpa.flowgraph.SimpleDemandPointerFlowGraph;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.traverse.SlowDFSDiscoverTimeIterator;

/**
 * Purely field-based, context-insensitive demand-driven points-to analysis with
 * very simple implementation.
 * 
 * @author Manu Sridharan
 * 
 */
public class SimpleDemandPointsTo extends AbstractDemandPointsTo {

  private static final boolean VERBOSE = false;

  public SimpleDemandPointsTo(CallGraph cg, HeapModel model, MemoryAccessMap fam, IClassHierarchy cha, AnalysisOptions options) {
    super(cg, model, fam, cha, options);
  }

  @Override
  public Collection<InstanceKey> getPointsTo(PointerKey pk) throws IllegalArgumentException, UnimplementedError {

    if (pk == null) {
      throw new IllegalArgumentException("pk == null");
    }
    assert pk instanceof LocalPointerKey : "we only handle locals";
    LocalPointerKey lpk = (LocalPointerKey) pk;
    // Create an (initially empty) dependence graph
    SimpleDemandPointerFlowGraph g = new SimpleDemandPointerFlowGraph(cg, heapModel, mam, cha);

    // initialize the graph with the subgraph of x's method
    CGNode node = lpk.getNode();
    g.addSubgraphForNode(node);

    if (!g.containsNode(pk)) {
      return Collections.emptySet();
    }

    if (VERBOSE) {
      System.err.println(g.toString());
    }

    SlowDFSDiscoverTimeIterator<Object> dfs = new SlowDFSDiscoverTimeIterator<>(g, pk);
    Collection<InstanceKey> keys = HashSetFactory.make();
    while (dfs.hasNext()) {
      Object o = dfs.next();
      if (o instanceof InstanceKey) {
        keys.add((InstanceKey) o);
      }
    }
    
    return keys;
  }

}
