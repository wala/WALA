// Licensed Materials - Property of IBM
// 5724-D15
// (C) Copyright IBM Corporation 2004. All Rights Reserved. 
// Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                             
// --------------------------------------------------------------------------- 

package com.ibm.wala.j2ee.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BooleanIdentity;
import com.ibm.wala.dataflow.graph.BooleanSolver;
import com.ibm.wala.dataflow.graph.BooleanUnion;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.BooleanVariable;
import com.ibm.wala.fixpoint.TrueOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * 
 * This class will prune a call graph to include only nodes from which some
 * class which matches a certain filter is reachable.
 * 
 * For example, we can (and do) use this class to prune a call graph to include
 * only nodes for which some method A is reachable where A.getDeclaringClass()
 * is loaded by the application loader. This pruned call graph should hold all
 * the information needed to analyze transaction boundaries.
 * 
 * @author sfink
 * 
 */
public class PruneCallGraph {

  private final static boolean DEBUG = false;

  /**
   * @param F
   *          a filter which may accept some nodes in a call graph
   * @return the Set of nodes N s.t. there exists some path from the fake root
   *         node of G to some node M that contains N, where F accepts node M.
   */
  public static Set<CGNode> computeNodesOnPathToAccept(CallGraph G, Filter F) {
    OnPathSystem S = new OnPathSystem(G, F);
    S.solve();
    Set<CGNode> reachable = DFS.getReachableNodes(G, Collections.singleton(G.getFakeRootNode()));
    HashSet<CGNode> result = HashSetFactory.make();
    for (Iterator<CGNode> it = reachable.iterator(); it.hasNext();) {
      CGNode N = it.next();
      BooleanVariable B = S.getVariable(N);
      if (B.getValue())
        result.add(N);
      if (DEBUG) {
        Trace.println("On path " + N + " " + B.getValue());
      }
    }
    return result;
  }

  /**
   * A dataflow system which computes a boolean B(N) for every node in the call
   * graph, where B(N) = true if there exists some path from the fake root node
   * of G to some node M that contains N, where F accepts node M, and B(N) =
   * false otherwise.
   */
  private static class OnPathSystem {

    private CallGraph CG;

    private Filter filter;

    private Collection<CGNode> nodes;

    private DataflowSolver<CGNode> solver;

    /**
     * @param CG
     *          governing call graph
     */
    public OnPathSystem(CallGraph CG, Filter F) {
      this.CG = CG;
      this.filter = F;
      this.nodes = new Iterator2Collection<CGNode>(CG.iterator());
    }

    public BooleanVariable getVariable(CGNode n) {
      return (BooleanVariable) solver.getOut(n);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.dataflow.fixpoint.Solvable#solve()
     */
    public boolean solve() {
      final OrdinalSetMapping<CGNode> values = new MutableMapping<CGNode>(nodes.toArray());
      ITransferFunctionProvider<CGNode> functions = new ITransferFunctionProvider<CGNode>() {

        public UnaryOperator getNodeTransferFunction(CGNode node) {
          CGNode n = (CGNode) node;
          if (filter.accepts(n)) {
            return TrueOperator.instance();
          } else {
            return BooleanIdentity.instance();
          }
        }

        public boolean hasNodeTransferFunctions() {
          return true;
        }

        public UnaryOperator getEdgeTransferFunction(CGNode from, CGNode to) {
          Assertions.UNREACHABLE();
          return null;
        }

        public boolean hasEdgeTransferFunctions() {
          return false;
        }

        public AbstractMeetOperator getMeetOperator() {
          return BooleanUnion.instance();
        }

      };

      BitVectorFramework<CGNode,CGNode> F = new BitVectorFramework<CGNode,CGNode>(GraphInverter.invert(CG), functions, values);
      solver = new BooleanSolver<CGNode>(F);
      return solver.solve();
    }
  }
}
